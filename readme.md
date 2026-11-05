# Quản lý nhà trọ

Website quản lý nhà trọ cho hai vai trò **ADMIN** (chủ trọ) và **TENANT** (người thuê). Giao diện tiếng Việt, tiền VNĐ, ngày `dd/MM/yyyy`; mọi kiểm tra quyền thực hiện ở backend.

## Chức năng chính

- Phòng, người thuê, yêu cầu thuê, người ở cùng (giới hạn sức chứa, chặn trùng người).
- Hợp đồng: luồng nháp → chờ ký → hiệu lực → sắp hết hạn → hết hạn/kết thúc; tự đánh dấu sắp hết hạn bằng Scheduler.
- **Hợp đồng điện tử**: PDF tiếng Việt, người thuê ký tay trên điện thoại, file đã ký được khoá bằng SHA-256 và có thể kiểm tra toàn vẹn; người thuê tự gia hạn và ký phụ lục khi hợp đồng sắp hết hạn.
- Điện nước, dịch vụ, hoá đơn (nháp → phát hành), công nợ, thanh toán qua mã VietQR (admin xác nhận đã nhận tiền).
- Yêu cầu bảo trì kèm ảnh, thông báo, báo cáo/biểu đồ.
- **Trợ lý AI** (Gemini): hỏi đáp tự nhiên trên dữ liệu thật theo quyền của người dùng; không có API key thì tự chạy chế độ dò từ khoá.

## Công nghệ

| Phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.4 (Web, Security + JWT, Data JPA, Validation, Scheduler), OpenPDF |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, Recharts |
| Database | Microsoft SQL Server |

## Cấu trúc thư mục

```
quan_ly_nha_tro/
├── backend/            Spring Boot API (src/main/java/com/quanlynhatro/<module>)
│   ├── .env            cấu hình local (không commit) — mẫu: .env.example
│   └── storage/        file hợp đồng đã ký (cần sao lưu cùng database)
├── frontend/           React + TypeScript (src/pages, src/components, src/lib/api.ts)
├── database.sql        tạo database và toàn bộ schema (chạy được nhiều lần)
├── migrations/         script nâng cấp schema cho database đã có (V2, ...)
├── docs/               tài liệu API và tích hợp frontend
├── module_describe.md  đặc tả chức năng
└── AGENTS.md           quy tắc phát triển dự án
```

## Cài đặt và chạy trên máy

Yêu cầu: JDK 21, Maven 3.9+, Node.js + npm, SQL Server.

### 1. Database

- Cài mới: chạy `database.sql` (tạo database `QuanLyNhaTro` và mọi bảng).
- Database đã có từ trước: chạy các script trong `migrations/` theo thứ tự phiên bản, ví dụ:

```powershell
sqlcmd -S "ADMIN\BANG" -U sa -C -i migrations\V2__contract_documents.sql
```

Bảng `SchemaVersions` cho biết database đang ở phiên bản nào.

### 2. Backend

Sao chép `backend/.env.example` thành `backend/.env` rồi điền giá trị. Spring Boot tự đọc file này; biến môi trường Windows cùng tên sẽ được ưu tiên hơn `.env`.

| Biến | Bắt buộc | Ý nghĩa |
|---|---|---|
| `DB_HOST`, `DB_INSTANCE`, `DB_NAME`, `DB_USERNAME` | có | Kết nối SQL Server (mặc định `ADMIN\BANG`, `QuanLyNhaTro`, `sa`) |
| `DB_PASSWORD` | có | Mật khẩu SQL Server — không có giá trị mặc định |
| `JWT_SECRET` | có | Chuỗi Base64 tối thiểu 32 byte |
| `SERVER_PORT` | không | Cổng backend, mặc định `8080` |
| `CORS_ALLOWED_ORIGINS` | không | Origin frontend, mặc định `http://localhost:5173` |
| `VIETQR_BANK_CODE`, `VIETQR_BANK_ACCOUNT`, `VIETQR_ACCOUNT_NAME` | không | Tài khoản nhận tiền; để trống thì không tạo được mã QR |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | không | Bật trợ lý AI (lấy key tại https://aistudio.google.com/apikey) |
| `CONTRACT_EXPIRY_SCHEDULER_ENABLED` | không | Bật tác vụ quét hợp đồng sắp hết hạn hằng ngày |
| `CONTRACT_DOCUMENT_DIR` | không | Thư mục lưu PDF hợp đồng đã ký, mặc định `./storage/contract-documents` |

Tạo nhanh `JWT_SECRET` trong PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }) -as [byte[]])
```

Chạy (trong thư mục `backend`):

```powershell
mvn spring-boot:run
```

### 3. Frontend

Tạo `frontend/.env` trỏ tới backend (đổi cổng nếu `SERVER_PORT` khác 8080):

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Chạy (trong thư mục `frontend`):

```powershell
npm install
npm run dev
```

Mở http://localhost:5173.

> Không đặt mật khẩu SQL Server, JWT secret, API key hay thông tin ngân hàng trong `.env` của frontend.

## Kiểm thử

```powershell
# backend
mvn test

# frontend
npm run test
npm run lint
npm run build
```

Kiểm tra Maven đang dùng Java 21 (`mvn -v`), không chỉ `java -version`, vì `JAVA_HOME` có thể khiến Maven dùng JDK khác.

## Lưu ý vận hành

- Mã QR chỉ giúp người thuê chuyển khoản đúng số tiền và nội dung; thanh toán chỉ thành công khi admin xác nhận.
- Hoá đơn tạo ra ở trạng thái nháp; người thuê chỉ thấy và nhận thông báo sau khi admin bấm **Phát hành**.
- Sao lưu `backend/storage/` cùng database: thiếu file thì kiểm tra toàn vẹn hợp đồng đã ký sẽ báo không khớp.
- Khi bật trợ lý AI, câu hỏi và dữ liệu tra cứu được gửi tới Google Gemini.

Tài liệu thêm: [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md), [docs/FRONTEND_INTEGRATION.md](docs/FRONTEND_INTEGRATION.md).
