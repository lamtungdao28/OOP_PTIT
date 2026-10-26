"""Download the two official OpenCV Zoo ONNX models used by this service."""

from pathlib import Path
from urllib.request import urlretrieve

ROOT = Path(__file__).resolve().parent
MODELS = {
    "face_detection_yunet_2023mar.onnx":
        "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx",
    "face_recognition_sface_2021dec.onnx":
        "https://media.githubusercontent.com/media/opencv/opencv_zoo/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx",
}
# tải 2 mô hình Deeplearning để xử lý nhận dạng khuôn mặt

def main() -> None:
    model_dir = ROOT / "models"
    model_dir.mkdir(exist_ok=True)
    # chuẩn bị thư mục lưu trữ nếu thư mục đã tồn tại thì bỏ qua

    for name, url in MODELS.items():
        target = model_dir / name
        if target.exists() and target.stat().st_size > 100_000:
            print(f"Present: {target}")
            continue
        # kiểm tra nếu mô hình vượt quá 100MB => báo lỗi

        temporary = target.with_suffix(".download")
        try:
            urlretrieve(url, temporary)
            if temporary.stat().st_size <= 100_000:
                raise RuntimeError(f"Downloaded model is unexpectedly small: {name}")
            temporary.replace(target)
            print(f"Downloaded: {target}")
        # Tải về dạng download để tránh bị ngắt mạng làm hỏng rồi mới tải về thật kết hợp kiểm tra trên 100KB

        finally:
            temporary.unlink(missing_ok=True)
        # xóa file temporary nếu có


if __name__ == "__main__":
    main()
