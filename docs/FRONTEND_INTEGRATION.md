# React + TypeScript integration

## Environment and base URL

Đặt URL backend qua biến môi trường frontend, ví dụ `VITE_API_BASE_URL=http://localhost:8080/api`. Không ghi JWT secret, SQL password hoặc thông tin ngân hàng vào frontend.

## Login and requests

```ts
const response = await fetch(`${API}/auth/login`, {
  method: 'POST', headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({ identifier, password })
});
const auth = await response.json();
sessionStorage.setItem('access_token', auth.token);
```

Gửi token bằng `Authorization: Bearer ${token}`. Khi nhận `401`, xóa token và đưa người dùng về đăng nhập. Khi nhận `403`, hiển thị không có quyền; không thử đổi role ở client. Không lưu password.

## Role and ownership

`user.role` trong login/me chỉ dùng điều hướng giao diện. Backend mới quyết định quyền. TENANT không truyền userId để xem dữ liệu người khác; dùng endpoint `/tenant/...` và để backend lấy danh tính từ JWT.

## CORS

Backend cho phép origin từ `CORS_ALLOWED_ORIGINS`, mặc định local là `http://localhost:5173`. Nhiều origin phân tách bằng dấu phẩy. Không dùng `*` cùng credentials. Production phải dùng domain frontend HTTPS cụ thể.

## Dates, money and pagination

- Ngày request dùng `yyyy-MM-dd`; giao diện có thể hiển thị `dd/MM/yyyy`.
- Tiền dùng số decimal; tránh phép toán floating-point cho số tiền.
- `PageResponse.page` bắt đầu từ 0; không giả định hệ thống chỉ có 30 phòng.
- Report revenue theo múi giờ `Asia/Ho_Chi_Minh`, dựa trên `ConfirmedAt` thành công.

## Scheduler and payment safety

Frontend không bật Scheduler hợp đồng, không coi QR là thanh toán thành công và không giả lập callback. Payment chỉ thành công khi backend ghi nhận trạng thái hợp lệ.
