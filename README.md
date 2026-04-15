# 🎬 Cinema Booking System

Hệ thống đặt vé xem phim trực tuyến được xây dựng bằng **Spring Boot 3**, tích hợp **AI Chatbot** (Google Gemini), **ELK Stack** để quản lý log tập trung, **Redis** để caching, và **VNPay** để thanh toán.

---

## 📋 Mục Lục

- [Tính Năng](#-tính-năng)
- [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
- [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Cài Đặt & Chạy Dự Án](#-cài-đặt--chạy-dự-án)
- [Cấu Hình Môi Trường](#%EF%B8%8F-cấu-hình-môi-trường)
- [AI Chatbot](#-ai-chatbot)
- [API Endpoints](#-api-endpoints)
- [Giám Sát Log với ELK](#-giám-sát-log-với-elk)
- [Cấu Trúc Dự Án](#-cấu-trúc-dự-án)
- [Bảo Mật](#-bảo-mật)
- [Troubleshooting](#-troubleshooting-windows--wsl2)

---

## ✨ Tính Năng

### 👤 Người Dùng
- Đăng ký / Đăng nhập (JWT, OAuth2)
- Xem danh sách phim, suất chiếu, ghế ngồi
- Đặt vé, chọn ghế theo thời gian thực
- Thanh toán qua **VNPay** (MoMo nếu có)
- Nhận thông báo & vé qua email (kèm mã QR)
- Quản lý tài khoản, đổi mật khẩu

### 🤖 AI Chatbot (Gemini)
- Chatbot thông minh hỗ trợ khách hàng 24/7
- Tra cứu phim đang chiếu / sắp chiếu
- Gợi ý phim theo thể loại hoặc diễn viên
- Tra cứu lịch chiếu theo phim + ngày, theo rạp + ngày
- Kiểm tra ghế trống theo suất chiếu cụ thể
- Tra cứu bảng giá vé và phụ phí loại ghế
- Kiểm tra trạng thái đơn đặt vé theo mã booking
- Thông tin combo đồ ăn / bắp nước
- Thông tin khuyến mãi & mã giảm giá đang có hiệu lực
- Lưu lịch sử hội thoại theo từng user (JDBC Chat Memory)

### 🎞️ Quản Lý Nội Dung (Admin)
- Quản lý phim (thông tin, ảnh, diễn viên, đạo diễn, thể loại)
- Quản lý rạp chiếu, phòng chiếu, ghế ngồi
- Quản lý suất chiếu (showtime)
- Quản lý combo đồ ăn, sản phẩm bán kèm
- Quản lý mã khuyến mãi (promotion)
- Dashboard thống kê doanh thu

### 📊 Hệ Thống
- Log tập trung qua **ELK Stack** (Elasticsearch + Logstash + Kibana + Filebeat)
- Caching với **Redis**
- Upload ảnh lên **Cloudinary**
- **Rate Limit phân tầng** bảo vệ API (Global Filter & Annotation-based với Bucket4j)
- API documentation với **Swagger UI**
- Healthcheck cho tất cả services

---

## 🛠 Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.5 |
| **AI Chatbot** | Spring AI 1.1.2 + Google Gemini 2.0 Flash |
| **Database** | MySQL 8.0 |
| **Cache** | Redis 7 |
| **Logging** | ELK Stack 7.17.10 (Elasticsearch, Logstash, Kibana, Filebeat) |
| **Auth** | JWT (Nimbus JOSE), OAuth2 Resource Server |
| **Payment** | VNPay |
| **Storage** | Cloudinary |
| **Email** | Spring Mail (Gmail SMTP) |
| **ORM** | Spring Data JPA / Hibernate |
| **Mapping** | MapStruct 1.5.5 |
| **API Docs** | Springdoc OpenAPI (Swagger UI) |
| **Build Tool** | Maven |
| **Rate Limit** | Bucket4j 8.10.1 (Redis-backed) |
| **Container** | Docker + Docker Compose |

---

## 🏗 Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Mobile / Web)                     │
└─────────────────────────┬───────────────────────────────────┘
                           │ HTTP REST API
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot App (:8081)                       │
│  ┌───────────┐  ┌──────────┐  ┌───────────┐  ┌───────────┐ │
│  │Controller │→ │ Service  │→ │Repository │  │ AI Chatbot│ │
│  └───────────┘  └──────────┘  └───────────┘  └─────┬─────┘ │
└─────┬────────────────────┬──────────────────────────┼───────┘
      │                    │                          │
      ▼                    ▼                          ▼
┌──────────┐       ┌──────────────┐       ┌──────────────────┐
│  MySQL   │       │    Redis     │       │  Google Gemini   │
│  :3307   │       │   :6379      │       │  (Gemini 2.0     │
│ (Data)   │       │(Cache/Memory)│       │   Flash API)     │
└──────────┘       └──────────────┘       └──────────────────┘

ELK Logging Pipeline:
Spring App → Logstash(:5000) → Elasticsearch(:9200) → Kibana(:5601)
           ↘ File(/app/logs) → Filebeat(:5044) → Logstash

Cloud Storage:
Spring App → Cloudinary (Image Upload)
```

---

## 💻 Yêu Cầu Hệ Thống

| Phần mềm | Phiên bản tối thiểu |
|---|---|
| **Java JDK** | 21+ |
| **Maven** | 3.8+ |
| **Docker** | 24+ |
| **Docker Compose** | 2.0+ |
| **RAM** | Tối thiểu 4GB (ELK chiếm ~1.5GB) |

---

## 🚀 Cài Đặt & Chạy Dự Án

### Bước 1: Clone repository

```bash
git clone https://github.com/hien1172004/Booking-Cinema.git
cd Booking-Cinema
```

### Bước 2: Tạo file `.env`

Sao chép file mẫu và điền thông tin thực:

```bash
cp .env.example .env
```

Chỉnh sửa file `.env` theo hướng dẫn ở mục [Cấu Hình Môi Trường](#%EF%B8%8F-cấu-hình-môi-trường).

### Bước 3: Build ứng dụng

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows
.\mvnw.cmd clean package -DskipTests
```

> *File `pom.xml` đã cấu hình cờ `-parameters` để Spring Boot 3 nhận diện constructor chính xác.*

### Bước 4: Khởi chạy toàn bộ hệ thống

```bash
docker compose up -d
```

> **Khởi tạo dữ liệu**: Tệp `init.sql` tự động chạy khi MySQL khởi động lần đầu.  
> Để reset dữ liệu hoàn toàn: `docker compose down -v`

Kiểm tra trạng thái — tất cả services phải `healthy`:

```bash
docker compose ps
```

### Bước 5: Truy cập ứng dụng

| Service | URL |
|---|---|
| **API Backend** | http://localhost:8081 |
| **Swagger UI** | http://localhost:8081/swagger-ui.html |
| **Kibana** (log viewer) | http://localhost:5601 |
| **Elasticsearch** | http://localhost:9200 |

---

## 🔐 Tài Khoản Admin Mặc Định

Hệ thống tự động khởi tạo tài khoản quản trị viên trong lần chạy đầu tiên:

- **Username:** `admin`
- **Password:** `admin`

> **⚠️ Lưu ý:** Đổi mật khẩu ngay sau lần đăng nhập đầu tiên.

---

## 📖 Hướng Dẫn Sử Dụng Swagger UI

### 1. Truy cập
👉 **[http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)**

### 2. Lấy Token
1. Vào **Authentication** → `POST /api/v1/auth/login`
2. Nhấn **Try it out**, nhập:
   ```json
   {
     "username": "admin",
     "password": "admin"
   }
   ```
3. Nhấn **Execute** → Copy chuỗi `token` trong response.

### 3. Ủy Quyền
1. Nhấn nút **Authorize** (biểu tượng 🔓 trên đầu trang)
2. Nhập: `Bearer <chuỗi_token_của_bạn>`
3. Nhấn **Authorize** → **Close**

---

## ⚙️ Cấu Hình Môi Trường

Tạo file `.env` ở thư mục gốc với nội dung sau:

```env
# ========================
# App
# ========================
APP_PORT=8081

# ========================
# Database (MySQL)
# ========================
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/cinema_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_mysql_password

# ========================
# Redis
# ========================
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# ========================
# Cloudinary (Upload ảnh)
# Đăng ký tại: https://cloudinary.com
# ========================
CLOUD_NAME=your_cloud_name
CLOUD_API_KEY=your_api_key
CLOUD_API_SECRET=your_api_secret

# ========================
# Email (Gmail SMTP)
# Tạo App Password tại: https://myaccount.google.com/apppasswords
# ========================
EMAIL_USERNAME=your_gmail@gmail.com
EMAIL_PASSWORD=your_app_password_16_chars

# ========================
# JWT
# Tạo key ngẫu nhiên dài ≥ 64 ký tự (Base64)
# ========================
JWT_SIGNER_KEY=your_very_long_jwt_secret_key_at_least_64_chars

# ========================
# VNPay (Thanh toán)
# Đăng ký tại: https://sandbox.vnpayment.vn
# ========================
TMN_CODE=your_terminal_code
TMN_KEY=your_secret_key

# ========================
# Ngrok (Webhook URL cho VNPay IPN)
# Tải tại: https://ngrok.com | Chạy: ngrok http 8081
# ========================
NGROK_URL=https://your-subdomain.ngrok-free.app

# ========================
# Google Gemini AI (Chatbot)
# Lấy API key tại: https://aistudio.google.com/apikey
# ========================
GEMINI_API_KEY=your_gemini_api_key
```

### Giải Thích Từng Biến

| Biến | Mô tả | Cách lấy |
|---|---|---|
| `APP_PORT` | Port chạy Spring Boot | Mặc định `8081` |
| `SPRING_DATASOURCE_URL` | JDBC URL tới MySQL | Trong Docker: `mysql:3306`, local: `localhost:3307` |
| `CLOUD_NAME/KEY/SECRET` | Cloudinary credentials | Dashboard tại cloudinary.com |
| `EMAIL_PASSWORD` | Gmail App Password (không phải mật khẩu Google) | Google Account → Security → App Passwords |
| `JWT_SIGNER_KEY` | Khóa ký JWT (≥ 64 ký tự) | Tự generate ngẫu nhiên |
| `TMN_CODE` / `TMN_KEY` | Mã thanh toán VNPay | VNPay Sandbox portal |
| `NGROK_URL` | HTTPS tunnel để VNPay gọi IPN callback | `ngrok http 8081` |
| `GEMINI_API_KEY` | API key của Google Gemini | [Google AI Studio](https://aistudio.google.com/apikey) |

---

## 🤖 AI Chatbot

### Tổng Quan

Chatbot sử dụng **Spring AI 1.1.2** kết nối với **Google Gemini 2.0 Flash** qua OpenAI-compatible endpoint. Chatbot có khả năng gọi 15 **Tool** để lấy dữ liệu thực tế từ hệ thống.

### API Endpoint

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `POST` | `/api/v1/chatbot/chat` | Gửi tin nhắn tới chatbot | User / Admin |

**Request body:**
```json
{
  "message": "Hôm nay có phim gì hay?"
}
```

**Response:**
```json
{
  "reply": "Danh sách phim đang chiếu:\n• **Avengers: Doomsday** (150 phút) - Khởi chiếu: 25/04/2026\n..."
}
```

### Danh Sách 15 Tool của Chatbot

| # | Tool | Mô tả |
|---|---|---|
| 1 | `getNowShowingMovies` | Phim đang chiếu hiện tại |
| 2 | `getComingSoonMovies` | Phim sắp ra mắt |
| 3 | `getRecommendedMovies` | Gợi ý phim hay nhất |
| 4 | `getMoviesByGenre` | Tìm phim theo thể loại |
| 5 | `getMoviesByActor` | Tìm phim theo diễn viên / đạo diễn |
| 6 | `getShowtimesByMovieTitle` | Lịch chiếu phim hôm nay |
| 7 | `checkBookingStatus` | Kiểm tra trạng thái đơn vé theo mã BK... |
| 8 | `getCinemaContactInfo` | Địa chỉ & hotline các rạp |
| 9 | `getShowtimesByMovieAndDate` | Lịch chiếu phim theo ngày cụ thể |
| 10 | `getShowtimesByCinemaAndDate` | Lịch chiếu tại rạp theo ngày |
| 11 | `getTicketPriceInfo` | Bảng giá vé + phụ phí loại ghế |
| 12 | `checkAvailableSeats` | Ghế trống theo suất chiếu |
| 13 | `getMoviesByCinema` | Phim đang chiếu tại rạp cụ thể |
| 14 | `getFoodAndDrinkCombos` | Menu combo đồ ăn / bắp nước |
| 15 | `getActivePromotions` | Khuyến mãi & mã giảm giá đang có |

### Chat Memory

Lịch sử hội thoại được lưu trong **MySQL** thông qua `spring-ai-starter-model-chat-memory-repository-jdbc`. Mỗi user có một `conversationId` riêng (`chatbot:<username>`), duy trì ngữ cảnh trong **10 tin nhắn** gần nhất.

---

## Chạy Chỉ Dịch Vụ Hạ Tầng (Dev Mode)

Nếu muốn chạy app qua IDE/Maven mà không containerize app:

```bash
# Chỉ start MySQL, Redis, ELK
docker compose up -d mysql redis elasticsearch logstash kibana

# Chỉnh sửa SPRING_DATASOURCE_URL trong .env thành:
# SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/cinema_db

# Chạy Spring Boot
./mvnw spring-boot:run
```

---

## Dừng Hệ Thống

```bash
# Dừng tất cả containers (giữ data)
docker compose down

# Dừng và XÓA toàn bộ data (reset hoàn toàn)
docker compose down -v
```

---

## 📡 API Endpoints

Truy cập **Swagger UI** tại `http://localhost:8081/swagger-ui.html` để xem đầy đủ.

### Tổng Quan Các Module

| Module | Base Path | Mô tả |
|---|---|---|
| **Authentication** | `/api/v1/auth` | Đăng nhập, đăng ký, refresh token |
| **Users** | `/api/v1/users` | Quản lý người dùng |
| **Movies** | `/api/v1/movies` | Quản lý phim |
| **Categories** | `/api/v1/categories` | Thể loại phim |
| **Cinema** | `/api/v1/cinemas` | Quản lý rạp chiếu |
| **Rooms** | `/api/v1/rooms` | Quản lý phòng chiếu |
| **Seats** | `/api/v1/seats` | Quản lý ghế ngồi |
| **Seat Types** | `/api/v1/seat-types` | Loại ghế (Standard / VIP / Couple) |
| **Showtimes** | `/api/v1/showtimes` | Lịch chiếu phim |
| **Showtime Seats** | `/api/v1/showtime-seats` | Trạng thái ghế theo suất chiếu |
| **Bookings** | `/api/v1/bookings` | Đặt vé |
| **Tickets** | `/api/v1/tickets` | Quản lý vé |
| **Payments** | `/api/v1/payments` | Thanh toán VNPay |
| **Promotions** | `/api/v1/promotions` | Mã giảm giá |
| **Products** | `/api/v1/products` | Đồ ăn bán kèm |
| **Combos** | `/api/v1/combos` | Combo đồ ăn |
| **Notifications** | `/api/v1/notifications` | Thông báo |
| **Statistics** | `/api/v1/statistics` | Thống kê doanh thu |
| **Dashboard** | `/api/v1/dashboard` | Tổng quan admin |
| **Chatbot** | `/api/v1/chatbot` | AI Chatbot |
| **Cloudinary** | `/api/v1/upload` | Upload ảnh |

### Chiến Lược Rate Limit (Tiered Strategy)

| Tầng | API | Hạn mức |
|---|---|---|
| **Strict** | Auth, Booking, Payment, Chatbot | 3–5 req/phút |
| **Moderate** | Statistics, Search, Promotion | 10–30 req/phút |
| **Relaxed** | Movie Details, Recommend | 60 req/phút |
| **Global** | Tất cả API (Filter) | 100 req/phút |

> Response headers: `X-RateLimit-Remaining`, `Retry-After`

---

## 📊 Giám Sát Log với ELK

### Truy cập Kibana

Mở trình duyệt: **[http://localhost:5601](http://localhost:5601)**

### Tạo Index Pattern lần đầu

1. Vào **Stack Management** → **Index Patterns**
2. Click **Create index pattern**
3. Nhập pattern: `cinema-logs-*`
4. Chọn **Time field**: `@timestamp`
5. Click **Create index pattern**

### Xem Log

1. Vào menu **Discover**
2. Chọn index pattern `cinema-logs-*`
3. Filter theo các field:
   - `level` – mức log (INFO, WARN, ERROR)
   - `service` – tên service (`cinema-booking`)
   - `logger_name` – tên class ghi log
   - `message` – nội dung log

### Luồng Log

```
Spring App
  ├── Console (stdout Docker)
  ├── File: /app/logs/cinema.log  ←── Filebeat đọc ──→ Logstash(:5044)
  └── TCP:5000 ──────────────────────────────────────→ Logstash(:5000)
                                                            │
                                                     Elasticsearch(:9200)
                                                            │
                                                         Kibana(:5601) 📊
```

> Log file được rotate hàng ngày, giữ tối đa **7 ngày** và **500MB**.

---

## 📁 Cấu Trúc Dự Án

```
Cinema-Booking-System/
├── src/
│   └── main/
│       ├── java/org/example/cinemaBooking/
│       │   ├── Config/          # Security, Redis, Cloudinary, CORS, etc.
│       │   ├── Controller/      # REST Controllers (21 controllers)
│       │   │   └── ChatbotController.java
│       │   ├── DTO/             # Request/Response DTOs
│       │   ├── Entity/          # JPA Entities
│       │   ├── Exception/       # Global exception handling
│       │   ├── Mapper/          # MapStruct mappers
│       │   ├── Repository/      # Spring Data JPA repositories
│       │   ├── Service/         # Business logic
│       │   │   └── Chatbot/
│       │   │       ├── ChatbotService.java   # Chat orchestration + memory
│       │   │       └── ChatbotTools.java     # 15 tool methods cho AI
│       │   └── Shared/          # Enums, utilities
│       └── resources/
│           ├── application.yaml         # Cấu hình chính
│           ├── application-dev.yml      # Cấu hình môi trường Dev
│           ├── application-prod.yml     # Cấu hình môi trường Prod
│           └── logback-spring.xml       # Cấu hình logging (ELK + File)
├── logstash/
│   ├── pipeline/
│   │   └── logstash.conf        # Pipeline: input → filter → output
│   └── config/
│       └── logstash.yml
├── filebeat/
│   └── filebeat.yml             # Cấu hình Filebeat (filestream)
├── logs/                        # Log files (runtime)
├── docker-compose.yml           # Toàn bộ services (MySQL, Redis, ELK, App)
├── Dockerfile                   # Build image Spring Boot app
├── init.sql                     # Dữ liệu khởi tạo DB
├── pom.xml                      # Maven dependencies
├── .env                         # Biến môi trường (KHÔNG commit lên Git)
└── .env.example                 # Mẫu file .env (safe to commit)
```

---

## 🔒 Bảo Mật

- File `.env` đã được thêm vào `.gitignore` → **không bị commit lên Git**
- JWT dùng thuật toán **HS256** với key dài ≥ 64 ký tự
- Passwords trong DB được **hash bằng BCrypt**
- XSS / CSRF được Spring Security bảo vệ
- Rate Limit chống brute-force và DDoS
- `xpack.security.enabled=false` chỉ dùng cho môi trường **dev** — cần bật khi production

---

## 📝 Ghi Chú Quan Trọng

> **Ngrok URL**: VNPay cần một URL HTTPS công khai để gọi IPN callback. Mỗi lần restart ngrok, URL sẽ thay đổi → cần cập nhật `NGROK_URL` trong `.env`.

> **Gmail App Password**: Phải bật **2-Factor Authentication** trên tài khoản Google trước khi tạo App Password.

> **Gemini API Key**: Lấy miễn phí tại [Google AI Studio](https://aistudio.google.com/apikey). Giới hạn free tier là 15 RPM (request/phút) và 1 triệu token/ngày.

> **RAM**: ELK Stack tiêu thụ nhiều RAM. Nếu máy ít RAM, có thể giảm heap size trong `docker-compose.yml`:
> ```yaml
> ES_JAVA_OPTS=-Xms256m -Xmx256m   # Elasticsearch
> LS_JAVA_OPTS=-Xmx128m -Xms128m   # Logstash
> ```

---

## 🛠 Troubleshooting (Windows / WSL2)

| Lỗi | Nguyên nhân | Giải pháp |
|---|---|---|
| `Permission denied` (Logs) | Xung đột quyền host/container | Đã dùng **Named Volume** `cinema_logs` → không cần can thiệp |
| Filebeat `strict.perms` | Permission file yml trên Windows | Đã thêm `--strict.perms=false` trong `docker-compose.yml` |
| `init.sql` không chạy | Volume MySQL đã tồn tại | Chạy `docker compose down -v` để reset data |
| Chatbot trả về lỗi | Sai API key hoặc hết quota | Kiểm tra `GEMINI_API_KEY` trong `.env` và quota tại AI Studio |
| `Unable to find main class` | Build thiếu cấu hình | Đảm bảo chạy đúng lệnh `./mvnw clean package -DskipTests` |
