"""Face detection and embedding extraction with OpenCV YuNet and SFace."""

from __future__ import annotations

from pathlib import Path
from threading import Lock

import cv2
import numpy as np

MODEL_NAME = "SFace-2021dec"
COSINE_THRESHOLD = 0.363
MIN_MARGIN = 0.05
MAX_IMAGE_BYTES = 5 * 1024 * 1024
# tên mô hình, ngưỡng giống, ngưỡng đảm bảo phân tách 2 người, dung lượng ảnh tối đa 5MB

class FaceError(ValueError):
    def __init__(self, message: str, status_code: int = 422) -> None:
        super().__init__(message)
        self.status_code = status_code
        # Đóng gói lỗi xử lý ảnh kèm mã trạng thái HTTP (mặc định là 422 - Unprocessable Entity hoặc 413 - Payload Too Large) để FastAPI tự động trả mã lỗi phù hợp về cho client.

class FaceEngine:
    def __init__(self, model_dir: Path) -> None:
        detector_path = model_dir / "face_detection_yunet_2023mar.onnx"
        recognizer_path = model_dir / "face_recognition_sface_2021dec.onnx"
        if not detector_path.is_file() or not recognizer_path.is_file():
            raise FileNotFoundError("ONNX models missing; run python download_models.py")
        self.detector = cv2.FaceDetectorYN.create(str(detector_path), "", (320, 320), 0.9, 0.3, 5000)
        self.recognizer = cv2.FaceRecognizerSF.create(str(recognizer_path), "")
        self.lock = Lock()
        # cv2.FaceDetectorYN (YuNet): Mô hình phát hiện vị trí khuôn mặt và 5 điểm mốc (2 mắt, đỉnh mũi, 2 khóe miệng). Tham số 0.9 nghĩa là chỉ chấp nhận các khuôn mặt mà mô hình tin tưởng $\ge 90%$.
        # cv2.FaceRecognizerSF (SFace): Mô hình trích xuất đặc trưng khuôn mặt thành vector 128 chiều.
        # self.lock = Lock(): Khóa luồng (Thread Lock). Vì thư viện OpenCV C++ bên dưới không an toàn khi nhiều luồng cùng gọi hàm thay đổi kích cỡ ảnh (setInputSize) cùng lúc, khóa này giúp các request xử lý an toàn và tuần tự.

    def embedding(self, image_bytes: bytes) -> list[float]:
        if not image_bytes or len(image_bytes) > MAX_IMAGE_BYTES:
            raise FaceError("Image must be between 1 byte and 5 MB", 413)
        image = cv2.imdecode(np.frombuffer(image_bytes, dtype=np.uint8), cv2.IMREAD_COLOR)
        # Đảm bảo tệp không rỗng và sử dụng cv2.imdecode: Giải mã dữ liệu nhị phân (bytes) thành ma trận ảnh của OpenCV.

        if image is None:
            raise FaceError("Invalid image")
        height, width = image.shape[:2]
        if min(height, width) < 160 or max(height, width) > 4096:
            raise FaceError("Image dimensions must be between 160 and 4096 pixels")
        # Kiểm tra kích thước và độ phân giải của ảnh

        with self.lock:
            self.detector.setInputSize((width, height))
            _, faces = self.detector.detect(image)
            if faces is None or len(faces) == 0:
                raise FaceError("No face detected")
            if len(faces) != 1:
                raise FaceError("Exactly one face is required")
            # Phát hiện khuôn mặt và kiểm tra chỉ có 1 khuôn mặt

            face = faces[0]
            if min(float(face[2]), float(face[3])) < 80:
                raise FaceError("Face is too small")
            aligned = self.recognizer.alignCrop(image, face)
            # Mặt phải có kích thước tối thiểu 80x80 px và căn chỉnh khuôn mặt để cắt chuẩn về kích cỡ

            if cv2.Laplacian(aligned, cv2.CV_64F).var() < 40:
                raise FaceError("Face image is too blurry")
            # Nếu ảnh bị quá mờ => báo lỗi Sử dụng toán tử vi phân bậc 2 Laplacian để tính phương sai. Nếu phương sai $< 40$, tức ảnh chụp bị rung lắc hoặc mất nét nghiêm trọng $\rightarrow$ Từ chối xử lý.

            feature = np.asarray(self.recognizer.feature(aligned), dtype=np.float32).reshape(-1)
            # Tính toán vector đặc trưng (embedding) 128 chiều. Đây là "dấu vân tay" khuôn mặt.

        norm = float(np.linalg.norm(feature))
        if not np.isfinite(norm) or norm <= 0:
            raise FaceError("Could not create a valid embedding")
        return (feature / norm).astype(float).tolist()
        # Rút ra vector 128 số và chia cho độ dài Euclidean (L2-norm) để vector có độ dài đúng bằng 1: $||v||_2 = 1$. Việc này giúp bước tính độ tương đồng sau này trở nên cực kỳ nhanh chóng.


def recognize(embedding: list[float], candidates: list[dict]) -> dict:
    probe = np.asarray(embedding, dtype=np.float32)
    scores: list[tuple[float, int]] = []
    # probe nhận vector đặc trưng của khuông mặt, score lưu điểm tương đồng của khuôn mặt
    for candidate in candidates:
        if candidate["modelName"] != MODEL_NAME:
            continue
        # Nếu dùng mô hình cũ thì bỏ qua

        vector = np.asarray(candidate["embedding"], dtype=np.float32)
        if vector.shape != probe.shape or not np.all(np.isfinite(vector)):
            continue
        norm = float(np.linalg.norm(vector))
        if norm <= 0:
            continue
        # Vector phải đủ 128 chiều, không chứa NaN và Inf, độ dài vector phải lớn hơn 0

        score = float(np.dot(probe, vector / norm))
        scores.append((score, candidate["studentId"]))
        # Tính cosin và thêm vào danh sách embedding mặt + id 

    scores.sort(reverse=True)
    if not scores:
        return {"studentId": None, "confidence": None, "modelName": MODEL_NAME}
    best_score, best_id = scores[0]
    second_score = scores[1][0] if len(scores) > 1 else -1.0
    matched = best_score >= COSINE_THRESHOLD and best_score - second_score >= MIN_MARGIN
    # Sắp xếp giảm dần theo điểm và chọn ra người phù hợp nhất và phù hợp thứ 2
    # Điểm số của 2 người phải vượt qua ngưỡng nhất định
    return {
        "studentId": best_id if matched else None,
        "confidence": round(best_score, 4),
        "modelName": MODEL_NAME,
    }
