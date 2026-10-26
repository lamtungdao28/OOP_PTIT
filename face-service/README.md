# Face AI Service

Three FastAPI endpoints for face embedding and matching. The service does not access the database or create attendance records. Spring Boot must authenticate users, limit candidates to active enrollments in the class, and decide whether to save an attendance record.

## Setup (Python 3.11+)

```powershell
cd face-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe download_models.py
$env:AI_SERVICE_TOKEN = "choose-a-long-random-secret"
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Spring Boot should use the same `AI_SERVICE_TOKEN` and call the two face endpoints with `X-AI-Service-Token`. Keep port 8000 private. The downloaded ONNX models are excluded from Git; run `download_models.py` on each installation. They come from [OpenCV Zoo](https://github.com/opencv/opencv_zoo). Set `FACE_MODEL_DIR` to override the model directory.

## API contract

`GET /health` returns `{"status":"ok","modelName":"SFace-2021dec"}` when both models load; otherwise it returns 503.

`POST /face/register` accepts `multipart/form-data` with one `image` (JPEG, PNG or WebP, at most 5 MB). It requires exactly one clear face. Response:

```json
{"modelName":"SFace-2021dec","embedding":[0.01,0.02]}
```

The example embedding is shortened. The actual array has 128 numbers. Spring Boot stores it as a face profile and does not return it to the browser.

`POST /face/recognize` accepts `multipart/form-data` with `image` and `candidates` (a JSON array encoded as a form field):

```json
[{"studentId":101,"modelName":"SFace-2021dec","embedding":[0.01,0.02]}]
```

The actual candidate embedding has 128 numbers. Send only active enrolled students with active face profiles, at most 500. Response:

```json
{"studentId":101,"confidence":0.8241,"modelName":"SFace-2021dec"}
```

`studentId` is `null` for no match or an ambiguous match. The score is cosine similarity, not a calibrated probability. The initial match threshold is 0.363 with a 0.05 lead over the next candidate; tune on your own dataset before production use. Spring Boot must recheck the student, enrollment, open session, lecturer ownership, and duplicate attendance before recording PRESENT or LATE.

This basic detector does not provide liveness or anti-spoofing checks. Do not use face matching as the only protection against attendance fraud.

## Tests

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m pytest -q
```

The tests use a fake model engine and do not need downloaded ONNX files. For a manual model check, start the service and open `/health`.
