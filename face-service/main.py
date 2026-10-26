import os
import secrets
from functools import lru_cache

import cv2
import numpy as np

from fastapi import (
    FastAPI,
    File,
    Header,
    HTTPException,
    UploadFile
)

from insightface.app import FaceAnalysis


app = FastAPI(
    title="PTIT Face Recognition API"
)

MODEL_NAME = os.getenv(
    "FACE_MODEL_NAME",
    "buffalo_s"
)

MAX_IMAGE_SIZE = 5 * 1024 * 1024


# ==================================
# 1. Load pretrained model
# ==================================

@lru_cache(maxsize=1)
def get_model():

    model = FaceAnalysis(
        name=MODEL_NAME,
        providers=["CPUExecutionProvider"],
        allowed_modules=[
            "detection",
            "recognition"
        ]
    )

    model.prepare(
        ctx_id=-1,
        det_size=(640, 640)
    )

    return model


# ==================================
# 2. Face embedding extraction
# ==================================

def extract_embedding(image_bytes):

    image_array = np.frombuffer(
        image_bytes,
        dtype=np.uint8
    )

    image = cv2.imdecode(
        image_array,
        cv2.IMREAD_COLOR
    )

    if image is None:
        raise ValueError("INVALID_IMAGE")

    height, width = image.shape[:2]

    if height < 160 or width < 160:
        raise ValueError("IMAGE_TOO_SMALL")

    if height * width > 12_000_000:
        raise ValueError("IMAGE_TOO_LARGE")

    faces = get_model().get(image)

    if len(faces) == 0:
        raise ValueError("NO_FACE_DETECTED")

    if len(faces) > 1:
        raise ValueError("MULTIPLE_FACES_DETECTED")

    face = faces[0]

    embedding = np.asarray(
        face.embedding,
        dtype=np.float32
    )

    if not np.isfinite(embedding).all():
        raise ValueError("INVALID_EMBEDDING")

    norm = np.linalg.norm(embedding)

    if norm <= 1e-8:
        raise ValueError("INVALID_EMBEDDING")

    normalized_embedding = embedding / norm

    return normalized_embedding.tolist()


# ==================================
# 3. Health check
# ==================================

@app.get("/health")
def health():

    return {
        "status": "ok"
    }


# ==================================
# 4. Extract embedding API
# ==================================

@app.post("/extract")
def extract_face(
    image: UploadFile = File(...),
    x_service_key: str | None = Header(None)
):

    expected_key = os.getenv(
        "FACE_SERVICE_API_KEY",
        ""
    )

    if not expected_key:
        raise HTTPException(
            status_code=503,
            detail="SERVICE_KEY_NOT_CONFIGURED"
        )

    if not x_service_key or not secrets.compare_digest(
        x_service_key,
        expected_key
    ):
        raise HTTPException(
            status_code=401,
            detail="INVALID_SERVICE_KEY"
        )

    if image.content_type not in (
        "image/jpeg",
        "image/png"
    ):
        raise HTTPException(
            status_code=415,
            detail="UNSUPPORTED_IMAGE_TYPE"
        )

    image_bytes = image.file.read(
        MAX_IMAGE_SIZE + 1
    )

    if len(image_bytes) > MAX_IMAGE_SIZE:
        raise HTTPException(
            status_code=413,
            detail="IMAGE_TOO_LARGE"
        )

    try:

        embedding = extract_embedding(
            image_bytes
        )

    except ValueError as error:

        raise HTTPException(
            status_code=422,
            detail=str(error)
        )

    return {
        "modelName": MODEL_NAME,
        "embedding": embedding
    }