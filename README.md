# 🎬 Cinema Booking System

Hệ thống đặt vé xem phim trực tuyến được xây dựng bằng **Spring Boot 3**, tích hợp **ELK Stack** để quản lý log tập trung, **Redis** để caching, và **VNPay** để thanh toán.

---

## 📋 Mục Lục

- [Tính Năng](#-tính-năng)
- [Công Nghệ Sử Dụng](#-công-nghệ-sử-dụng)
- [Kiến Trúc Hệ Thống](#-kiến-trúc-hệ-thống)
- [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
- [Cài Đặt & Chạy Dự Án](#-cài-đặt--chạy-dự-án)
- [Cấu Hình Môi Trường](#-cấu-hình-môi-trường)
- [API Endpoints](#-api-endpoints)
- [Giám Sát Log với ELK](#-giám-sát-log-với-elk)
- [Cấu Trúc Dự Án](#-cấu-trúc-dự-án)

---

## ✨ Tính Năng

### 👤 Người Dùng
- Đăng ký / Đăng nhập (JWT, OAuth2)
- Xem danh sách phim, suất chiếu, ghế ngồi
- Đặt vé, chọn ghế theo thời gian thực
- Thanh toán qua **VNPay**
- Nhận thông báo & vé qua email (kèm mã QR)
- Quản lý tài khoản, đổi mật khẩu

### 🎞️ Quản Lý Nội Dung
- Quản lý phim (thông tin, ảnh, diễn viên, đạo diễn)
- Quản lý rạp chiếu, phòng chiếu, ghế ngồi
- Quản lý suất chiếu (showtime)
- Quản lý combo đồ ăn, sản phẩm bán kèm
- Quản lý mã khuyến mãi (promotion)
- Dashboard thống kê doanh thu

### 📊 Hệ Thống
- Log tập trung qua **ELK Stack** (Elasticsearch + Logstash + Kibana)
- Caching với **Redis**
- Upload ảnh lên **Cloudinary**
- **Rate Limit phân tầng** bảo vệ API (Global & Annotation-based)
- API documentation với **Swagger UI**
- Healthcheck cho tất cả services

---

## 🛠 Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.5 |
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
| **Rate Limit** | Bucket4j 8.10.1 (Redis backup) |

---

## 🏗 Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────┐
│                   Client (Mobile/Web)                │
└─────────────────────┬───────────────────────────────┘
                      │ HTTP REST API
                      ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot App (:8081)                 │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────┐ │
│  │Controller│→ │ Service  │→ │    Repository      │ │
│  └──────────┘  └──────────┘  └────────────────────┘ │
└────┬──────────────────┬───────────────┬─────────────┘
     │                  │               │
     ▼                  ▼               ▼
┌─────────┐      ┌────────────┐  ┌───────────┐
│  MySQL  │      │   Redis    │  │Cloudinary │
│  :3307  │      │  :6379     │  │  (Cloud)  │
└─────────┘      └────────────┘  └───────────┘

ELK Logging Pipeline:
Spring App → Logstash(:5000) → Elasticsearch(:9200) → Kibana(:5601)
           ↘ File(/app/logs) → Filebeat → Logstash
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

Sao chép file mẫu và điền thông tin:

```bash
cp .env.example .env
```

Chỉnh sửa file `.env` với thông tin của bạn (xem [Cấu Hình Môi Trường](#-cấu-hình-môi-trường)).

### Bước 3: Build ứng dụng

```bash
./mvnw clean package -DskipTests
```

> Trên Windows: `.\mvnw.cmd clean package -DskipTests`
> 
> *Lưu ý: Tệp `pom.xml` đã được cấu hình cờ `-parameters` để đảm bảo Spring Boot 3 nhận diện constructor chính xác.*

### Bước 4: Khởi chạy toàn bộ hệ thống với Docker

```bash
docker compose up -d
```

> **Khởi tạo dữ liệu**: Tệp `init.sql` sẽ được thực thi khi bạn start lần đầu. Nếu bạn muốn chạy lại tệp này, hãy dùng lệnh `docker compose down -v` để reset dữ liệu hệ thống.

Lần đầu chạy sẽ mất vài phút để pull images về. Kiểm tra trạng thái:

```bash
docker compose ps
```

Tất cả services phải ở trạng thái `healthy`.

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

*   **Username:** `admin`
*   **Password:** `admin`

> **Lưu ý:** Bạn nên đổi mật khẩu ngay sau khi đăng nhập lần đầu để đảm bảo bảo mật.

---

## 📖 Hướng Dẫn Sử Dụng Swagger UI

Swagger UI cung cấp giao diện trực quan để thử nghiệm các API.

### 1. Truy cập
👉 **[http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)**

### 2. Cách Đăng Nhập & Lấy Token
Để gọi các API yêu cầu quyền (ví dụ: Admin, Staff), bạn cần thực hiện:

1.  Tìm đến **Authentication** > `POST /api/v1/auth/login`.
2.  Nhấn **Try it out** và nhập thông tin admin (hoặc user đã đăng ký):
    ```json
    {
      "username": "admin",
      "password": "admin"
    }
    ```
3.  Nhấn **Execute**. Nếu thành công, bạn sẽ nhận được một chuỗi `token` trong phần response.

### 3. Ủy Quyền (Authorize)
1.  Copy chuỗi `token` (chỉ copy phần text trong dấu ngoặc kép).
2.  Cuộn lên đầu trang Swagger, nhấn nút **Authorize** (biểu tượng chiếc khóa).
3.  Nhập vào ô Value theo định dạng: `Bearer <chuỗi_token_của_bạn>`.
4.  Nhấn **Authorize** rồi **Close**.

Bây giờ bạn đã có thể gọi các API quản trị như `/api/v1/movies`, `/api/v1/cinemas`, v.v.

---

## Chạy Chỉ Dịch Vụ Hạ Tầng (Không Build App)

Nếu muốn chạy app trực tiếp qua IDE/Maven (không containerize app):

```bash
# Chỉ start MySQL, Redis, ELK
docker compose up -d mysql redis elasticsearch logstash kibana

# Chạy Spring Boot
./mvnw spring-boot:run
```

> **Lưu ý**: Khi chạy local, set `spring.profiles.active=local` để tắt Logstash appender (tránh lỗi kết nối).

---

## Dừng Hệ Thống

```bash
# Dừng tất cả containers (giữ data)
docker compose down

# Dừng và XÓA toàn bộ data (reset hoàn toàn)
docker compose down -v
```

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
# Tải tại: https://ngrok.com
# ========================
NGROK_URL=https://your-subdomain.ngrok-free.app
```

### Giải Thích Từng Biến

| Biến | Mô tả | Cách lấy |
|---|---|---|
| `APP_PORT` | Port chạy Spring Boot | Mặc định `8081` |
| `SPRING_DATASOURCE_URL` | JDBC URL tới MySQL | Trong Docker dùng `mysql:3306`, local dùng `localhost:3307` |
| `CLOUD_NAME/KEY/SECRET` | Cloudinary credentials | Dashboard tại cloudinary.com |
| `EMAIL_PASSWORD` | Gmail App Password (không phải mật khẩu Google) | Google Account → Security → App Passwords |
| `JWT_SIGNER_KEY` | Khóa ký JWT (≥64 ký tự) | Tự generate ngẫu nhiên |
| `TMN_CODE` / `TMN_KEY` | Mã thanh toán VNPay | VNPay Sandbox portal |
| `NGROK_URL` | HTTPS tunnel để VNPay gọi IPN callback | `ngrok http 8081` |

---

## 📡 API Endpoints

Truy cập **Swagger UI** tại `http://localhost:8081/swagger-ui.html` để xem đầy đủ.

### Tổng Quan Các Module

| Module | Base Path | Mô tả |
|---|---|---|
| **Authentication** | `/api/v1/auth` | Đăng nhập, đăng ký, refresh token |
| **Users** | `/api/v1/users` | Quản lý người dùng |
| **Movies** | `/api/v1/movies` | Quản lý phim |
| **Cinema** | `/api/v1/cinemas` | Quản lý rạp chiếu |
| **Rooms** | `/api/v1/rooms` | Quản lý phòng chiếu |
| **Seats** | `/api/v1/seats` | Quản lý ghế ngồi |
| **Showtimes** | `/api/v1/showtimes` | Lịch chiếu phim |
| **Bookings** | `/api/v1/bookings` | Đặt vé |
| **Payments** | `/api/v1/payments` | Thanh toán VNPay |
| **Promotions** | `/api/v1/promotions` | Mã giảm giá |
| **Products** | `/api/v1/products` | Đồ ăn bán kèm |
| **Combos** | `/api/v1/combos` | Combo đồ ăn |
| **Notifications** | `/api/v1/notifications` | Thông báo |
| **Statistics** | `/api/v1/statistics` | Thống kê doanh thu |
| **Dashboard** | `/api/v1/dashboard` | Tổng quan admin |
| **Rate Limit Info** | Headers | `X-RateLimit-Remaining`, `Retry-After` |

### Chiến lược Rate Limit (Tiered Strategy)

| Tầng (Tier) | API | Hạn mức |
|---|---|---|
| **Strict** | Auth, Booking, Payment | 3-5 req/phút |
| **Moderate** | Statistics, Search | 10-30 req/phút |
| **Relaxed** | Movie Details, Recommend | 60 req/phút |
| **Global** | Tất cả API (Filter) | 100 req/phút |

---

## 📊 Giám Sát Log với ELK

### Truy cập Kibana

Mở trình duyệt: `http://localhost:5601`

### Tạo Index Pattern lần đầu

1. Vào **Stack Management** → **Index Patterns**
2. Click **Create index pattern**
3. Nhập pattern: `cinema-logs-*`
4. Chọn **Time field**: `@timestamp`
5. Click **Create index pattern**

### Xem Log

1. Vào menu **Discover**
2. Chọn index pattern `cinema-logs-*`
3. Có thể filter theo các field:
   - `level` – mức log (INFO, WARN, ERROR)
   - `service` – tên service (`cinema-booking`)
   - `logger_name` – tên class ghi log
   - `message` – nội dung log

### Luồng Log

```
Spring App
  ├── Console (stdout Docker)
  ├── File: /app/logs/cinema.log  ←── Filebeat đọc ──→ Logstash
  └── TCP:5000 ──────────────────────────────────────→ Logstash
                                                           │
                                                    Elasticsearch
                                                           │
                                                        Kibana 📊
```

> Log file được rotate hàng ngày, giữ tối đa **7 ngày** và **500MB**.

---

## 📁 Cấu Trúc Dự Án

```
Cinema-Booking-System/
├── src/
│   └── main/
│       ├── java/org/example/cinemaBooking/
│       │   ├── Config/          # Security, Redis, Cloudinary, etc.
│       │   ├── Controller/      # REST Controllers (19 controllers)
│       │   ├── DTO/             # Request/Response objects
│       │   ├── Entity/          # JPA Entities (24 entities)
│       │   ├── Exception/       # Global exception handling
│       │   ├── Mapper/          # MapStruct mappers
│       │   ├── Repository/      # Spring Data JPA repositories
│       │   ├── Service/         # Business logic
│       │   └── Shared/          # Common utilities, enums
│       └── resources/
│           ├── application.yaml         # Cấu hình chung
│           ├── application-dev.yml      # Cấu hình môi trường Dev
│           ├── application-prod.yml     # Cấu hình môi trường Prod
│           └── logback-spring.xml       # Cấu hình logging ELK
├── logstash/
│   ├── pipeline/
│   │   └── logstash.conf        # Pipeline: input→filter→output
│   └── config/
│       └── logstash.yml         # Cấu hình Logstash
├── filebeat/
│   └── filebeat.yml             # Cấu hình Filebeat
├── logs/                        # Log files (generated at runtime)
├── docker-compose.yml           # Định nghĩa toàn bộ services
├── dockerfile                   # Build image Spring Boot app
├── pom.xml                      # Maven dependencies
├── .env                         # Biến môi trường (KHÔNG commit)
└── .env.example                 # Mẫu file .env (safe to commit)
```

---

## 🔒 Bảo Mật

- File `.env` đã được thêm vào `.gitignore` → **không bị commit lên Git**
- JWT dùng thuật toán **HS256** với key dài ≥ 64 ký tự
- Passwords trong DB được **hash bằng BCrypt**
- XSS / CSRF được Spring Security bảo vệ
- `xpack.security.enabled=false` chỉ dùng cho môi trường **dev** – cần bật khi production

---

## 📝 Ghi Chú Quan Trọng

> **Ngrok URL**: VNPay cần một URL HTTPS công khai để gọi IPN callback. Mỗi lần restart ngrok, URL sẽ thay đổi → cần cập nhật `NGROK_URL` trong `.env`.

> **Gmail App Password**: Phải bật **2-Factor Authentication** trên tài khoản Google trước khi tạo App Password.

> **RAM**: ELK Stack tiêu thụ nhiều RAM. Nếu máy ít RAM, có thể giảm heap size trong `docker-compose.yml`:
> ```yaml
> ES_JAVA_OPTS=-Xms256m -Xmx256m   # Elasticsearch
> LS_JAVA_OPTS=-Xmx128m -Xms128m   # Logstash
> ```

---

## 🛠 Troubleshooting (Windows / WSL2)

Nếu bạn gặp lỗi khi chạy trên Windows, hãy kiểm tra các điểm sau:

1. **Lỗi `Permission denied` (Logs)**: 
   - Hệ thống đã chuyển sang sử dụng **Named Volume** (`cinema_logs`) trong `docker-compose.yml` để tránh xung đột quyền giữa Windows host và Linux container. Không cần can thiệp thủ công.
2. **Lỗi Filebeat `strict.perms`**: 
   - Đã cấu hình `--strict.perms=false` trong `docker-compose.yml`. Nếu bạn chỉnh sửa tệp `filebeat.yml`, hãy đảm bảo không dùng định dạng JSON nếu log đầu vào là Plain Text.
3. **Lỗi `init.sql` không chạy**: 
   - Chạy `docker compose down -v` để xóa volume dữ liệu cũ, MySQL sẽ thực thi lại tệp này khi khởi tạo lại.
