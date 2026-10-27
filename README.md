# OOP PTIT — quản lý lớp và điểm danh khuôn mặt

Backend Spring Boot quản lý tài khoản, lớp học và điểm danh. Dịch vụ FastAPI dùng YuNet/SFace để tạo và so khớp đặc trưng khuôn mặt. FastAPI không truy cập database; Spring Boot quyết định có lưu điểm danh hay không.

## Yêu cầu

- Java 21. Maven Wrapper trong `backend/` tự tải Maven ở lần chạy đầu (cần Internet); IntelliJ IDEA cũng có thể chạy ứng dụng bằng Maven tích hợp.
- Python 3.11+.
- MySQL 8.4; có thể chạy bằng Docker Compose.
- Kết nối Internet một lần để tải thư viện và hai model ONNX. File model, môi trường ảo, database và các file chứa bí mật không được đưa lên Git.

## Cấu trúc repo

| Đường dẫn | Vai trò |
| --- | --- |
| `backend/` | Spring Boot API, JPA, JWT, MySQL và client gọi AI |
| `backend/compose.yaml` | MySQL phát triển trên cổng host `3307` |
| `backend/mock-data/demo-data.sql` | Dữ liệu demo đầy đủ, nạp thủ công |
| `backend/src/main/resources/db/face-profile-migration.sql` | Sửa cột embedding của database cũ |
| `face-service/` | FastAPI, YuNet và SFace trên cổng `8000` |

## Chạy trên Windows (PowerShell)

Các lệnh bên dưới được chạy từ thư mục tương ứng. Dùng hai terminal cho Spring Boot và FastAPI.

### 1. Khởi động MySQL

Tạo `backend/.env` (file này được Git bỏ qua):

```properties
MYSQL_DATABASE=java_sql
MYSQL_ROOT_PASSWORD=<mat-khau-mysql-cua-ban>
```

Tại `backend/`:

```powershell
docker compose up -d db
```

Compose công bố MySQL tại `localhost:3307`. Nếu dùng MySQL đã có sẵn, bỏ qua Compose và chỉnh `DB_URL` ở bước tiếp theo. Chỉ dùng tài khoản `root` cho phát triển cục bộ; môi trường triển khai nên có tài khoản DB riêng.

### 2. Cấu hình Spring Boot

Tạo `backend/.env.local` (cũng được Git bỏ qua):

```properties
DB_URL=jdbc:mysql://localhost:3307/java_sql
DB_USERNAME=root
DB_PASSWORD=<mat-khau-mysql-cua-ban>
JWT_SECRET=<khoa-ngau-nhien-32-byte-duoc-ma-hoa-Base64>
JWT_ISSUER=oop-ptit
JWT_AUDIENCE=oop-ptit-api
JWT_TTL=15m
AI_SERVICE_URL=http://127.0.0.1:8000
AI_SERVICE_TOKEN=<mot-token-noi-bo-dai-va-ngau-nhien>
JPA_DDL_AUTO=validate
```

Tạo `JWT_SECRET` bằng `openssl rand -base64 32` hoặc một công cụ tạo 32 byte ngẫu nhiên rồi mã hóa Base64. `AI_SERVICE_TOKEN` phải giống hệt biến môi trường của FastAPI ở bước 4. Xem [mẫu đầy đủ](backend/.env.example). Spring Boot đọc `.env` rồi `.env.local` **theo working directory `backend/`**; biến môi trường của hệ điều hành/IDE có ưu tiên cao hơn.

Nếu database **hoàn toàn mới và chưa có bảng**, chỉ trong môi trường phát triển: đặt `JPA_DDL_AUTO=update`, chạy Spring Boot một lần để Hibernate tạo schema, dừng ứng dụng, rồi đổi về `validate`. Nếu dùng schema cũ có `face_profiles.embedding` là `TINYBLOB`, chạy [migration](backend/src/main/resources/db/face-profile-migration.sql) trên đúng database để đổi thành `LONGBLOB`; embedding 128 chiều cần 512 byte.

### 3. Chuẩn bị FastAPI và model

Tại `face-service/`:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe download_models.py
$env:AI_SERVICE_TOKEN = "<cung-gia-tri-voi-backend>"
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Giữ terminal này mở. Model được lưu tại `face-service/models/` và không nằm trong Git. Kiểm tra:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Kết quả cần có `status: ok` và `modelName: SFace-2021dec`. Xem [tài liệu dịch vụ AI](face-service/README.md) cho hợp đồng `/face/register` và `/face/recognize`.

### 4. Chạy Spring Boot

Tại `backend/`, sau khi database đã có schema phù hợp:

```powershell
.\mvnw.cmd spring-boot:run
```

Hoặc chạy class `com.example.oopPTIT.OopPtitApplication` trong IntelliJ, với working directory là `backend/`. API mặc định ở `http://localhost:8080`. Kiểm tra `java -version` là Java 21; Maven Wrapper có thể cần tải bản phân phối và các thư viện ở lần chạy đầu.

## Dữ liệu demo và tài khoản

[Dữ liệu demo](backend/mock-data/demo-data.sql) chứa admin, giảng viên, sinh viên, lớp, buổi học và điểm danh. Nạp file **thủ công** vào một database phát triển đã có bảng. [README dữ liệu demo](backend/mock-data/README.md) mô tả số bản ghi, các tình huống test và cách tìm ID thật.

Các tài khoản demo mặc định `INACTIVE` và không có mật khẩu đăng nhập. Muốn test JWT, tạo BCrypt hash của mật khẩu tự chọn rồi đặt `@demo_password_hash` **trong cùng phiên MySQL trước lần nạp đầu**, theo hướng dẫn trong README dữ liệu demo; hoặc dùng tài khoản `ACTIVE` đã có. Nạp lại file sau khi đã tạo tài khoản `INACTIVE` sẽ không cập nhật mật khẩu/trạng thái. Bộ SQL không tạo embedding khuôn mặt giả: sinh viên cần gọi API đăng ký khuôn mặt bằng ảnh thật.

File [mock-data.sql cũ](backend/src/main/resources/db/mock-data.sql) chỉ tạo dữ liệu môn học, học kỳ, lớp và giảng viên; không cần nạp nếu chỉ muốn thử luồng demo đầy đủ.

## Thử nhanh API

1. `GET http://127.0.0.1:8000/health` → `200`.
2. `POST http://localhost:8080/api/auth/login` với JSON `{"username":"<student>","password":"<password>"}` → lấy `data.accessToken`.
3. `GET /api/students/me/face-profile` với `Authorization: Bearer <student-JWT>` → xem `registered`.
4. `POST /api/students/me/face-profile` với JWT sinh viên và `multipart/form-data`, trường File tên `image` → `201`. Gọi lại GET → `registered: true`. Nếu đã đăng ký, dùng `PUT` cùng URL để thay ảnh.
5. Đăng nhập giảng viên được phân công lớp. Mở phiên bằng `POST /api/sessions/{classSessionId}/attendance/open` với JSON `{"lateAfterMinutes":15}`. Sau đó gửi ảnh sinh viên đã đăng ký tới `POST /api/sessions/{classSessionId}/attendance/recognize` với JWT giảng viên → `201`, `method: 2`.

`{classSessionId}` là khóa chính của `class_sessions`, không phải `session_no` (ví dụ `940011`). Những câu `SELECT` cuối `demo-data.sql` trả ID thật.

Spring Boot gọi AI bằng `X-AI-Service-Token`, không chuyển JWT người dùng sang FastAPI. Ảnh hỗ trợ JPEG/PNG/WebP tối đa 5 MB và phải có đúng một khuôn mặt rõ.

## Kiểm tra và lỗi thường gặp

```powershell
# Trong backend/
.\mvnw.cmd test

# Trong face-service/; test dùng model giả, không cần tải ONNX
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m pytest -q
```

| Triệu chứng | Kiểm tra |
| --- | --- |
| Login `401` | Tài khoản `ACTIVE`, mật khẩu BCrypt đúng và Body JSON có `username`, `password` |
| Spring Boot gọi AI trả `503` | FastAPI đang chạy, `AI_SERVICE_URL` đúng, hai `AI_SERVICE_TOKEN` giống nhau |
| Đăng ký mặt `409` nhưng GET báo chưa có | Kiểm tra log SQL và kiểu cột `face_profiles.embedding`; schema cũ `TINYBLOB` cần migration |
| Nhận diện `422` | Ảnh không đạt yêu cầu hoặc không khớp rõ với sinh viên ghi danh hợp lệ |
| Điểm danh `409` | Phiên đã đóng/hết hạn hoặc sinh viên đã được điểm danh trong phiên |

Không commit `backend/.env`, `backend/.env.local`, mật khẩu, JWT, `face-service/.venv`, model ONNX hay dữ liệu database. Trước khi push, chạy `git status --short` và chỉ đưa các file nguồn, cấu hình mẫu và tài liệu cần thiết lên branch.
