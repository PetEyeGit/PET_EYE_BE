# Redis Setup — Docker (Local Development)

Hướng dẫn cài Redis bằng Docker để chạy luồng booking (PayOS pending, staff slot lock, cash deposit).

---

## Yêu cầu

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) đã cài và đang chạy
- Port `6379` chưa bị chiếm

---

## 1. Chạy Redis bằng Docker

### Cách nhanh (không cần password)

```bash
docker run -d \
  --name petreal-redis \
  -p 6379:6379 \
  redis:7-alpine
```

### Cách có password (khuyến nghị)

```bash
docker run -d --name peteye-redis -p 6379:6379 redis:7-alpine redis-server --requirepass 12345678
```

> Thay `yourpassword123` bằng password bạn muốn dùng.

---

## 2. Kiểm tra Redis đang chạy

```bash
# Xem container đang chạy
docker ps

# Ping thử Redis
docker exec -it petreal-redis redis-cli ping
# Kết quả mong đợi: PONG

# Nếu có password
docker exec -it petreal-redis redis-cli -a 12345678 ping
```

---

## 3. Cấu hình trong `.env`

Mở file `.env` ở thư mục gốc `PET_EYE_BE/` và thêm/sửa các dòng sau:

### Không có password

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### Có password

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=yourpassword123
```

---

## 4. Kiểm tra `application-local.yml`

File `src/main/resources/application-local.yml` đã được cấu hình sẵn để đọc từ `.env`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 5000ms
```

Không cần sửa file này, chỉ cần đảm bảo `.env` có đúng giá trị.

---

## 5. Khởi động lại Spring Boot

Sau khi Redis đang chạy và `.env` đã cập nhật, restart app:

```bash
./mvnw spring-boot:run
```

Hoặc chạy lại từ IDE (IntelliJ / VS Code).

---

## 6. Luồng booking sử dụng Redis

Redis được dùng cho 3 mục đích trong luồng booking:

| Key pattern | TTL | Mục đích |
|---|---|---|
| `pending_booking:{orderCode}` | 30 phút | Lưu thông tin booking PayOS chờ thanh toán |
| `cash_pending:{orderCode}` | 30 phút | Lưu thông tin booking cash chờ thanh toán cọc 10% |
| `staff_slot:{staffId}:{datetime}` | 30 phút | Lock slot staff tránh double-booking |

Nếu Redis không chạy, các API sau sẽ bị lỗi `RedisConnectionFailureException`:
- `POST /api/bookings/initiate-payment`
- `POST /api/bookings/confirm-payment`
- `POST /api/bookings/cash/initiate`
- `POST /api/bookings/cash/confirm`
- `GET /api/bookings/staff/{shopId}/availability`

---

## 7. Quản lý container

```bash
# Dừng Redis
docker stop petreal-redis

# Khởi động lại Redis
docker start petreal-redis

# Xem logs Redis
docker logs petreal-redis

# Xoá container (dữ liệu Redis sẽ mất)
docker rm -f petreal-redis
```

---

## 8. Tự động khởi động cùng Docker Desktop

Để Redis tự chạy mỗi khi mở Docker Desktop:

```bash
docker update --restart unless-stopped petreal-redis
```

---

## 9. Xem dữ liệu Redis (tuỳ chọn)

Dùng Redis CLI để debug:

```bash
docker exec -it petreal-redis redis-cli

# Xem tất cả keys
KEYS *

# Xem nội dung 1 key
GET pending_booking:12345678

# Xem TTL còn lại (giây)
TTL pending_booking:12345678

# Xoá 1 key thủ công
DEL pending_booking:12345678
```

Hoặc cài [RedisInsight](https://redis.io/redis-enterprise/redis-insight/) (GUI miễn phí) để xem trực quan hơn, kết nối tới `localhost:6379`.

---

## Troubleshooting

**Lỗi: `Connection refused: localhost:6379`**
→ Redis chưa chạy. Chạy lại lệnh `docker start petreal-redis` hoặc tạo mới container ở bước 1.

**Lỗi: `WRONGPASS invalid username-password pair`**
→ Password trong `.env` không khớp với password khi tạo container. Kiểm tra lại `REDIS_PASSWORD`.

**Lỗi: `port is already allocated`**
→ Port 6379 đang bị dùng bởi process khác. Đổi port: `-p 6380:6379` và cập nhật `REDIS_PORT=6380` trong `.env`.

**Docker Desktop chưa chạy**
→ Mở Docker Desktop trước, chờ icon ở taskbar chuyển sang trạng thái "Running" rồi mới chạy lệnh docker.
