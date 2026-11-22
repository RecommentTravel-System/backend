# Wayvee — Project Overview & Feature Map

> **Quick reference & Comprehensive Guide** cho AI agent và nhà phát triển.  
> Đọc file này trước để nắm trọn bộ chức năng và kiến trúc toàn dự án mà **không cần quét lại toàn bộ source code**.

---

## 1. Wayvee là gì?

**Wayvee** là ứng dụng lập kế hoạch du lịch thông minh (Smart Travel Itinerary Planner & Discovery Platform) tối ưu cho thị trường Việt Nam. Hệ thống kết hợp dữ liệu bản đồ OpenStreetMap (Overpass & Nominatim), dịch vụ lưu trữ media Cloudinary, cổng thanh toán PayOS và giải thuật tối ưu lịch trình để mang lại trải nghiệm du lịch liền mạch.

---

## 2. Bản đồ chức năng toàn hệ thống (System Feature Map)

### 2.1. Khám phá & Bản đồ du lịch (Places & Map Discovery)
- **Tìm kiếm địa điểm**: Tìm theo tên, khu vực, khoảng cách xung quanh (nearby GPS), lọc theo bán kính.
- **Phân loại danh mục (Categories)**: Nhà hàng, Quán cafe, Khách sạn/Homestay, Điểm tham quan/Check-in, Mua sắm, Giải trí ban đêm.
- **Chi tiết địa điểm**: Tọa độ, địa chỉ chuẩn hóa (Nominatim), giờ mở cửa theo ngày (`LocationOpeningHour`), ảnh chất lượng cao.
- **Tương tác**: Lưu địa điểm yêu thích (`Favorites`), chia sẻ, dẫn đường.

### 2.2. Lập kế hoạch & Tối ưu lịch trình (Smart Itinerary & Replanning)
- **Tạo chuyến đi (Trip Planning)**: Chọn điểm đến, khoảng ngày đi - về, số lượng người, ngân sách dự kiến, phong cách du lịch.
- **Thuật toán sắp xếp lịch trình**: Phân bổ địa điểm theo ngày (`dayIndex`, `orderIndex`), tối ưu quãng đường di chuyển và thời gian di chuyển.
- **Replanning tự động**: Tự động gợi ý điều chỉnh lịch trình khi có thay đổi hoặc hủy địa điểm (Preview & Confirm).
- **Lưu & Xuất lịch trình**: Quản lý danh sách chuyến đi (DRAFT, ACTIVE, COMPLETED).

### 2.3. Hệ thống Đánh giá & Phản hồi (Reviews & Feedback System)
Hệ thống gồm 2 phân hệ độc lập:
1. **Đánh giá địa điểm (Place Reviews - OSM POI)**:
   - Người dùng đánh giá các địa điểm du lịch thực tế (1 - 5 sao, nội dung nhận xét, ẩn danh hoặc công khai).
   - Admin kiểm duyệt: Xác minh, ẩn hoặc xóa đánh giá vi phạm từ cộng đồng.
2. **Đánh giá & Góp ý trang web (Website Reviews / Feedback)**:
   - **Người dùng**:
     - Gửi đánh giá trải nghiệm hệ thống (1 - 5 sao, chọn chủ đề: UI/UX, lịch trình, tốc độ, địa điểm, dịch vụ...).
     - **Đính kèm đa phương tiện**: Người dùng có thể đăng tải nhiều **ảnh (JPG, PNG, WebP)** và **video (MP4, WebM)** minh họa lên Cloudinary.
     - Xem lại các feedback cá nhân đã gửi tại trang `Trung tâm đánh giá & Góp ý` (`/reviews`).
   - **Quản trị viên (Admin Portal - `/admin/reviews`)**:
     - Xem danh sách toàn bộ feedback từ người dùng kèm đầy đủ hình ảnh, video đính kèm.
     - **Quyền phản hồi (Rep lại)**: Admin có toàn quyền gửi phản hồi chính thức cho người dùng, sử dụng các mẫu câu trả lời nhanh (quick templates), chỉnh sửa hoặc xóa phản hồi.
     - **Không ràng buộc**: Việc trả lời là hoàn toàn tự nguyện theo nhu cầu chăm sóc khách hàng của Admin; hệ thống **không** gán nhãn hay ép buộc trạng thái "Chờ phản hồi". Chỉ khi Admin đã trả lời thì đánh giá mới hiển thị nhãn `✓ Đã phản hồi`.
     - Bộ lọc tiện ích: Lọc theo số sao, theo chủ đề hoặc theo trạng thái (`Đã phản hồi` / `Chưa phản hồi`).
   - **Hiển thị cho User**:
     - Khi Admin phản hồi, feedback của user sẽ lập tức hiển thị huy hiệu `✓ Đã có phản hồi từ Admin` cùng khối phản hồi chính thức mang nhận diện Ban Quản Trị WAYVEE.

### 2.4. Gói hội viên & Thanh toán trực tuyến (Premium & PayOS)
- **Các gói hội viên**: Gói dùng thử, Gói chuyến đi (lượt), Gói năm Premium không giới hạn.
- **Cổng thanh toán PayOS**: Tạo mã thanh toán VietQR động chuẩn NAPAS 247.
- **Webhook tự động**: Nhận thông báo IPN từ PayOS, xác thực chữ ký HMAC checksum, tự động kích hoạt gói subscription và mở khóa lịch trình cho người dùng.

### 2.5. Xác thực & Tài khoản (Authentication & Profile)
- **Đăng ký / Đăng nhập**: Email + Mật khẩu với JWT Bearer token; hỗ trợ đăng nhập mạng xã hội (Google OAuth2 / Firebase).
- **Kích hoạt tài khoản**: Gửi mã OTP xác thực qua Email (Resend / SMTP).
- **Bảo mật & Quên mật khẩu**: Đổi mật khẩu, quên mật khẩu với quy trình OTP an toàn, thu hồi JWT khi đăng xuất (`InvalidatedToken`).
- **Profile người dùng**: Cập nhật thông tin cá nhân, tải ảnh đại diện Avatar lên Cloudinary.

### 2.6. Bảng điều khiển Quản trị viên (Admin Portal - `/admin/*`)
- **Dashboard Tổng quan (`/admin/dashboard`)**: KPI người dùng, doanh thu, tổng số chuyến đi, biểu đồ hoạt động.
- **Quản lý người dùng (`/admin/users`)**: Xem danh sách thành viên, khóa/mở tài khoản, phân quyền `ROLE_USER` / `ROLE_ADMIN`.
- **Quản lý danh mục & POI (`/admin/categories`)**: Cấu hình nhóm địa điểm, công cụ import dữ liệu POI từ OpenStreetMap / CSV.
- **Quản lý doanh thu (`/admin/revenue`)**: Lịch sử giao dịch PayOS, thống kê gói cước được mua nhiều nhất.
- **Lịch sử & Lưu lượng truy cập (`/admin/access-logs`)**: Giám sát lưu lượng truy cập hàng ngày (lượt hôm nay, tổng lượt xem, khách duy nhất theo IP, thiết bị hàng đầu), biểu đồ trực quan biến động lưu lượng (7 ngày / 30 ngày), danh sách top 5 trang được xem nhiều nhất, cơ cấu thiết bị (Desktop / Mobile / Tablet) và bảng nhật ký chi tiết từng phiên truy cập có tìm kiếm, bộ lọc, xem chi tiết modal và xuất CSV. Tự động ghi nhận page view từ client router.
- **Kiểm duyệt & Phản hồi (`/admin/reviews`)**: Tab Đánh giá địa điểm và Tab Đánh giá website (xem ảnh/video, rep lại feedback).

---

## 3. Cấu trúc monorepo & Repositories

Dự án gồm 2 repository độc lập đặt cùng thư mục cha:

```
Wayvee/
├── BE/
│   └── WayveeSystem/          ← Git Repository: backend (nhánh: dev)
│       ├── doc/ARCHITECTURE.md
│       ├── pom.xml
│       └── src/main/java/org/example/wayveesystem/...
│
├── FE/
│   └── frontend/              ← Git Repository: frontend (nhánh: main)
│       ├── doc/ARCHITECTURE.md
│       ├── package.json
│       ├── vite.config.js
│       └── src/...
│
└── PROJECT.md                 ← File tổng hợp này
```

---

## 4. Tech Stack tóm tắt

| Layer | Công nghệ | Chi tiết |
|---|---|---|
| **Frontend** | React 19, Vite 8, Tailwind CSS v4, React Router v7 | Fast SPA, Context API, Lucide icons, responsive layout |
| **Backend** | Spring Boot 4.1.1, Java 21, PostgreSQL | JPA / Hibernate (`ddl-auto=update`), REST API |
| **Bảo mật** | Spring Security 6, JWT, OAuth2 Resource Server | `ROLE_USER`, `ROLE_ADMIN` |
| **Thanh toán** | PayOS SDK (`vn.payos`) | VietQR webhook auto-confirm |
| **Media** | Cloudinary HTTP5 SDK | Tự động phân loại ảnh và video (`resource_type=auto`) |
| **Bản đồ** | OpenStreetMap (Overpass API + Nominatim Reverse) | Tìm kiếm POI, địa chỉ |
| **Email** | Spring Mail, Resend SDK, Thymeleaf | Gửi mã OTP kích hoạt & khôi phục |
| **PWA & Desktop App** | W3C Web App Manifest, Service Worker, PWABuilder | Sẵn sàng đóng gói MSIX đưa lên Microsoft Store, hỗ trợ ngoại tuyến, bộ icon đa kích thước & screenshots |
| **Deploy FE** | Vercel | CI/CD từ nhánh `main` |
| **Deploy BE** | Docker / Render / VPS | CI/CD từ nhánh `dev` |

---

## 5. Danh mục API Endpoints cốt lõi

### Xác thực & Người dùng (`/api/auth`)
- `POST /api/auth/register`: Đăng ký tài khoản
- `POST /api/auth/login`: Đăng nhập, nhận JWT
- `POST /api/auth/logout`: Đăng xuất, hủy token
- `POST /api/auth/refresh`: Cấp mới token
- `POST /api/auth/verify-email`: Xác thực OTP
- `POST /api/auth/forgot-password` & `reset-password`: Quên mật khẩu
- `GET /api/auth/me`: Lấy thông tin tài khoản đang đăng nhập
- `POST /api/auth/me/avatar`: Upload avatar (Cloudinary)

### Đánh giá trang web & Phản hồi (`/api/v1/website-reviews`)
- `POST /api/v1/website-reviews`: Người dùng gửi feedback (sao, chủ đề, comment, `mediaUrls`)
- `POST /api/v1/website-reviews/upload`: Upload ảnh/video feedback lên Cloudinary (hỗ trợ JPG, PNG, WebP, MP4, WebM tối đa 30MB)
- `GET /api/v1/website-reviews`: Lấy danh sách feedback (Admin xem tất cả; User xem của chính mình)
- `GET /api/v1/website-reviews/stats`: Thống kê số lượng, điểm trung bình, phân bố sao
- `GET /api/v1/website-reviews/me`: Lấy danh sách feedback do chính người dùng hiện tại gửi
- `POST /api/v1/website-reviews/{id}/reply`: **Admin phản hồi** (hoặc cập nhật phản hồi) cho feedback
- `DELETE /api/v1/website-reviews/{id}/reply`: **Admin xóa phản hồi**

### Địa điểm & Bản đồ (`/api/v1/locations`)
- `POST /api/v1/locations/nearby`: Tìm kiếm địa điểm xung quanh tọa độ
- `GET /api/v1/locations/{osmId}`: Chi tiết địa điểm
- `POST /api/v1/locations/images`: Upload ảnh địa điểm (Cloudinary)

### Lịch trình chuyến đi (`/api/v1/trips`)
- `GET /api/v1/trips`: Danh sách chuyến đi của user
- `POST /api/v1/trips`: Tạo mới chuyến đi
- `GET /api/v1/trips/{id}`: Chi tiết chuyến đi và danh sách địa điểm
- `POST /api/v1/trips/replan/preview` & `confirm`: Thuật toán replanning tối ưu hóa

### Lịch sử truy cập & Lưu lượng hệ thống (`/api/access-logs` & `/api/admin/access-logs`)
- `POST /api/access-logs/record`: Ghi nhận lượt xem trang tự động (public, nhận diện IP, thiết bị, hệ điều hành, trình duyệt, người dùng nếu có)
- `GET /api/admin/access-logs`: Lấy danh sách nhật ký truy cập có phân trang, tìm kiếm từ khóa, lọc theo thiết bị và khoảng thời gian (Admin)
- `GET /api/admin/access-logs/stats`: Thống kê lưu lượng tổng hợp: lượt hôm nay, khách duy nhất, chuỗi ngày vẽ biểu đồ, top trang được xem, cơ cấu thiết bị (Admin)
- `DELETE /api/admin/access-logs/{id}`: Xóa bản ghi lịch sử truy cập (Admin)

### Thanh toán PayOS (`/api/payments`)
- `POST /api/payments/create`: Tạo payment link PayOS
- `POST /api/payments/payos-webhook`: Webhook nhận kết quả giao dịch tự động
- `GET /api/payments/{orderCode}/status`: Tra cứu trạng thái đơn hàng

---

## 6. Phân quyền người dùng (Roles & Permissions)

| Role | Quyền hạn |
|---|---|
| `ROLE_USER` | Tìm kiếm, xem địa điểm, tạo/lưu lịch trình, lưu yêu thích, gửi đánh giá địa điểm, gửi feedback website kèm ảnh/video, mua gói Premium PayOS |
| `ROLE_ADMIN` | Toàn quyền `ROLE_USER` + Toàn bộ khu vực `/api/admin/**` (quản trị người dùng, doanh thu, danh mục) + Kiểm duyệt đánh giá địa điểm + **Đọc toàn bộ feedback website, xem ảnh/video đính kèm và trả lời phản hồi trực tiếp cho người dùng** |
