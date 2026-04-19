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
Sử dụng **Spring AI** kết hợp với **Gemini 2.0 Flash**, chatbot có khả năng gọi 15 công cụ (Function Calling) để:
- Tra cứu lịch chiếu, giá vé, kiểm tra ghế trống theo suất chiếu.
- Gợi ý phim thông minh và kiểm tra tình trạng đơn hàng qua mã Booking.
- Lưu lịch sử hội thoại (Chat Memory) bền vững trong Database.

### 💳 Thanh Toán & Đặt Vé
- **Real-time Locking:** Giữ ghế trong 5-10 phút khi người dùng đang thực hiện giao dịch.
- **VNPay Integration:** Xử lý thanh toán an toàn, tự động hoàn trả ghế nếu thanh toán thất bại.
- **Mã QR & Email:** Tự động tạo mã QR và gửi vé qua email tích hợp mã hóa an toàn.

### 🚦 Ops & Security
- **Rate Limit:** Bảo vệ chống DDoS đa tầng (Global, Auth, Payment).
- **Monitoring:** Tổng hợp log qua ELK Stack và giám sát sức khỏe qua Actuator.
- **Cloud Native:** Tối ưu JVM cho môi trường Container (Memory-constrained environments).

---

## 🏗 Kiến Trúc Hệ Thống

Hệ thống được thiết kế theo mô hình **Layered Architecture** với sự tách biệt rõ ràng giữa Controller, Service, và Repository.

<p align="center">
  <img src="docs/architecture.png" width="800" alt="Architecture Diagram"/>
</p>

---

## 🚦 Chiến Lược Rate Limit (Tiered Strategy)

| Tầng | API Targets | Hạn mức |
|---|---|---|
| **Strict** | Auth (Login/Register), Booking, Payment | 3–5 req/phút |
| **Moderate** | Statistics, Chatbot, Promotion Apply | 10–20 req/phút |
| **Global** | Toàn bộ hệ thống (Security Filter) | 100 req/phút |

---

## 🧪 Kiểm Thử & Chất Lượng Code

Dự án chú trọng đặc biệt vào chất lượng mã nguồn với:
- **Unit Testing:** >95% coverage cho các Service quan trọng (`CinemaService`, `UserService`, `ReviewService`).
- **Standard:** Tuân thủ naming convention `given_when_then` và sử dụng `AssertJ` cho các câu khẳng định rõ ràng.

---

## ☁️ Cloud Deployment

Dự án được triển khai trên hạ tầng hiện đại:
- **Application Server:** [Railway.app](https://railway.app/) (Tự động CI/CD qua GitHub).
- **Database Service:** [Aiven](https://aiven.io/) (Cung cấp MySQL và Valkey/Redis với SSL).

---

## 📝 License & Contact

- **License:** Dự án này được phát hành dưới giấy phép [MIT](https://opensource.org/licenses/MIT).
- **Author:** [Hiền - hien1172004](https://github.com/hien1172004)
- **Support:** Nếu có bất kỳ câu hỏi nào, vui lòng liên hệ qua GitHub Issue của dự án.

---
*Dự án hướng tới việc xây dựng một hệ thống đặt vé xem phim chuẩn quy mô doanh nghiệp.*
