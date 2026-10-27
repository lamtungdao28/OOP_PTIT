# Smart Attendance Frontend

React + TypeScript frontend cho hệ thống Smart Attendance, tích hợp trực tiếp với Spring Boot API trong thư mục `backend`.

## Chạy trên macOS

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Backend mặc định tại `http://localhost:8080`. Cấu hình CORS backend cần cho phép `http://localhost:5173`.

## Kiểm tra

```bash
npm run typecheck
npm run lint
npm run build
```

JWT chỉ được giữ trong `sessionStorage`, ảnh khuôn mặt không được lưu ở local storage. Số liệu dashboard không có endpoint tổng hợp riêng sẽ hiển thị `—` hoặc được tính từ danh sách API hiện tại.
