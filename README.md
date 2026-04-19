# 🎬 Cinema Booking System - Enterprise Grade Movie Platform

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Test Coverage](https://img.shields.io/badge/Coverage-95%2B%25-brightgreen.svg)](#-kiểm-thử--chất-lượng-code)
[![Deployment](https://img.shields.io/badge/Deployment-Railway-blueviolet.svg)](https://railway.app/)
[![Database](https://img.shields.io/badge/Database-Aiven%20MySQL-blue.svg)](https://aiven.io/)

Hệ thống đặt vé xem phim trực tuyến thời gian thực, tích hợp **AI Agent**, thanh toán **VNPay**, và hệ thống giám sát **ELK Stack**.

---

## 🔗 Quick Links

- 🌐 **Live Demo (Swagger UI):** [https://booking-cinema-production.up.railway.app/swagger-ui/index.html](https://booking-cinema-production.up.railway.app/swagger-ui/index.html)
- 🤖 **AI Chatbot:** `/api/v1/chatbot/chat`
- 📔 **API Docs:** `/v3/api-docs`

---

## ✨ Tính Năng Nổi Bật

### 🤖 AI Agent (Trợ Lý Ảo Gemini)
Sử dụng **Spring AI** kết hợp với **Gemini 2.0 Flash**, chatbot có khả năng gọi 15 công cụ (Function Calling) để tra cứu lịch chiếu, giá vé, kiểm tra ghế trống và gợi ý phim thông minh.

### 💳 Thanh Toán & Đặt Vé
- **Real-time Locking:** Giữ ghế trong 5-10 phút khi người dùng đang thực hiện giao dịch.
- **VNPay Integration:** Xử lý thanh toán an toàn, tự động hoàn trả ghế nếu thanh toán thất bại.
- **Mã QR & Email:** Tự động tạo mã QR và gửi vé qua email tích hợp mã hóa an toàn.

---

## 🚀 Hướng Dẫn Cài Đặt (Local Development)

### 1. Yêu cầu hệ thống
- JDK 21+
- Docker & Docker Compose
- Maven 3.8+

### 2. Thiết lập biến môi trường
Sao chép tệp mẫu và điền thông tin thực tế của bạn:
```bash
cp .env.example .env
```
> [!TIP]
> Bạn cần chuẩn bị các API Key từ: [Google AI Studio](https://aistudio.google.com/apikey) (AI), [VNPay Sandbox](https://sandbox.vnpayment.vn/) (Thanh toán), và [Cloudinary](https://cloudinary.com/) (Lưu trữ ảnh).

### 3. Khởi chạy hệ thống
```bash
./mvnw clean package -DskipTests
docker compose up -d
```

---

## 📖 Hướng Dẫn Sử Dụng & Test API

### 🔐 Tài khoản mặc định
Hệ thống tự động khởi tạo:
- **Username:** `admin` | **Password:** `admin`

### 🛠 Cách sử dụng Swagger UI (Authorize)
1. Truy cập: `https://booking-cinema-production.up.railway.app/swagger-ui/index.html`
2. Tìm **Authentication** -> `POST /api/v1/auth/login`
3. Thực hiện đăng nhập với tài khoản admin để lấy chuỗi **token**.
4. Nhấn nút **Authorize** ở phía trên cùng trang web.
5. Nhập: `Bearer <token_của_bạn>` rồi nhấn Authorize.
6. Bây giờ bạn có thể gọi mọi API bảo mật.

---

## 🏗 Kiến Trúc Hệ Thống & Rate Limit

Hệ thống sử dụng mô hình **Layered Architecture** giúp dễ dàng mở rộng và bảo trì.

| Tầng | API Targets | Hạn mức |
|---|---|---|
| **Strict** | Auth, Booking, Payment | 3–5 req/phút |
| **Moderate** | Statistics, Chatbot | 10–20 req/phút |
| **Global** | Toàn bộ hệ thống | 100 req/phút |

---

## 🧪 Kiểm Thử & Chất Lượng Code

- **Unit Testing:** >95% coverage cho các Service quan trọng (`ReviewService`, `CinemaService`, v.v.).
- **Monitoring:** Log được tập trung qua ELK Stack và giám sát qua Kibana.

---

## 📝 License & Contact

- **Author:** [Hiến - hien1172004](https://github.com/hien1172004)
- **Support:** Nếu có bất kỳ câu hỏi nào, vui lòng liên hệ qua GitHub Issue của dự án.

---
*Dự án hướng tới việc xây dựng một hệ thống đặt vé xem phim chuẩn quy mô doanh nghiệp.*
