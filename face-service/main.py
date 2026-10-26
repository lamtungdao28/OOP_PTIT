"""Stateless AI service. The Spring Boot backend owns identity and attendance data."""

from __future__ import annotations

import json
import os
import secrets
from functools import lru_cache
from pathlib import Path

import cv2
from fastapi import Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator

from recognition import FaceEngine, FaceError, MODEL_NAME, recognize

app = FastAPI(title="Classroom Face AI Service", version="1.0.0")
MODEL_DIR = Path(os.getenv("FACE_MODEL_DIR", Path(__file__).resolve().parent / "models"))
ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"}
# Khởi tạo ứng dụng FastAPI.
# MODEL_DIR: Đường dẫn đến thư mục chứa 2 file mô hình AI ONNX (mặc định là thư mục models/, có thể ghi đè qua biến môi trường FACE_MODEL_DIR).
# ALLOWED_CONTENT_TYPES: Danh sách trắng các định dạng ảnh hợp lệ (JPEG, PNG, WebP) để ngăn chặn việc tải lên các tệp độc hại hoặc không tương thích.

class Candidate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    studentId: int = Field(gt=0)
    modelName: str
    embedding: list[float] = Field(min_length=128, max_length=128)

    @field_validator("embedding")
    @classmethod
    def finite_embedding(cls, value: list[float]) -> list[float]:
        import math

        if not all(math.isfinite(number) for number in value):
            raise ValueError("Embedding must contain finite numbers")
        return value
# Lớp này sử dụng Pydantic để kiểm tra tính toàn vẹn của từng hồ sơ sinh viên gửi từ Spring Boot sang:
# extra="forbid": Nghiêm cấm chứa bất kỳ trường dữ liệu thừa thãi nào ngoài các trường quy định.
# studentId: int = Field(gt=0): Mã sinh viên bắt buộc là số nguyên dương $> 0$.
# embedding: list[float] = Field(min_length=128, max_length=128): Vector bắt buộc phải có độ dài đúng 128 số thực.
# @field_validator("embedding"): Đảm bảo 128 số này đều là số hữu hạn (math.isfinite), không bị dính lỗi NaN (Not a Number) hay vô cực Infinity.

@lru_cache(maxsize=1)
def get_engine() -> FaceEngine:
    return FaceEngine(MODEL_DIR)
# Quá trình đọc 2 file mô hình ONNX từ ổ cứng vào RAM rất nặng. Bộ nhớ đệm cache đảm bảo đối tượng FaceEngine chỉ được khởi tạo đúng 1 lần duy nhất trong suốt vòng đời của server.

async def require_backend(x_ai_service_token: str | None = Header(default=None)) -> None:
    expected = os.getenv("AI_SERVICE_TOKEN")
    if not expected:
        raise HTTPException(503, "AI_SERVICE_TOKEN is not configured")
    if not x_ai_service_token or not secrets.compare_digest(x_ai_service_token, expected):
        raise HTTPException(401, "Invalid AI service token")
# Ngăn chặn người ngoài truy cập trực tiếp vào AI Service: Service này chỉ phục vụ riêng cho Spring Boot backend.
# Phía Spring Boot bắt buộc phải gửi kèm header X-AI-Service-Token.
# secrets.compare_digest: Phương thức so sánh chuỗi bảo mật của Python nhằm chống tấn công đo thời gian (Timing Attack). Nếu token không khớp $\rightarrow$ báo lỗi 401 Unauthorized.

async def ready_engine() -> FaceEngine:
    try:
        return get_engine()
    except (FileNotFoundError, cv2.error) as exc:
        raise HTTPException(503, "Face models are unavailable") from exc
# Đảm bảo engine đã sẵn sàng. Nếu chưa tải mô hình hoặc OpenCV bị lỗi, hàm lập tức trả về mã lỗi HTTP 503 Service Unavailable.

async def read_image(image: UploadFile) -> bytes:
    if image.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(415, "Supported image types: JPEG, PNG, WebP")
    data = await image.read(5 * 1024 * 1024 + 1)
    if not data or len(data) > 5 * 1024 * 1024:
        raise HTTPException(413, "Image must be between 1 byte and 5 MB")
    return data
# Kiểm tra định dạng tệp (nếu không phải JPEG/PNG/WebP $\rightarrow$ lỗi 415 Unsupported Media Type).
# Chỉ đọc tối đa $5\text{MB} + 1\text{byte}$. Nếu kích thước vượt quá 5MB hoặc file rỗng $\rightarrow$ báo lỗi 413 Payload Too Large. Cơ chế này giúp chống tấn công tràn bộ nhớ (DoS attack).

@app.get("/health")
async def health() -> dict:
    await ready_engine()
    return {"status": "ok", "modelName": MODEL_NAME}
# Dùng cho Docker / Kubernetes hoặc hệ thống giám sát kiểm tra xem dịch vụ và mô hình AI đã nạp vào bộ nhớ xong hay chưa.

@app.post("/face/register", dependencies=[Depends(require_backend)])
async def register(image: UploadFile = File(...), engine: FaceEngine = Depends(ready_engine)) -> dict:
    try:
        embedding = engine.embedding(await read_image(image))
    except FaceError as exc:
        raise HTTPException(exc.status_code, str(exc)) from exc
    return {"modelName": MODEL_NAME, "embedding": embedding}


@app.post("/face/recognize", dependencies=[Depends(require_backend)])
async def recognize_face(image: UploadFile = File(...), candidates: str = Form(...),
                         engine: FaceEngine = Depends(ready_engine)) -> dict:
    try:
        parsed = json.loads(candidates)
        if not isinstance(parsed, list) or len(parsed) > 500:
            raise ValueError("candidates must be an array of at most 500 students")
        validated = [Candidate.model_validate(item).model_dump() for item in parsed]
        ids = [item["studentId"] for item in validated]
        if len(ids) != len(set(ids)):
            raise ValueError("Duplicate studentId in candidates")
    except (json.JSONDecodeError, ValidationError, ValueError, TypeError) as exc:
        raise HTTPException(422, "Invalid candidates list") from exc
    try:
        embedding = engine.embedding(await read_image(image))
    except FaceError as exc:
        raise HTTPException(exc.status_code, str(exc)) from exc
    return recognize(embedding, validated)
