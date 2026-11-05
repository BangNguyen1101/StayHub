# Backend API documentation

Base URL: `/api`. API bảo vệ dùng `Authorization: Bearer <JWT>`. API phân trang dùng `page` (bắt đầu từ 0), `size` và tham số lọc riêng. Response phân trang gồm `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

## Authentication

| Method | Endpoint | Quyền | Body/response chính |
|---|---|---|---|
| POST | `/auth/login` | Public | `{identifier,password}` → `{token,tokenType,user}` |
| POST | `/auth/register` | Public | `{username,email,password,fullName,phoneNumber}`; luôn tạo TENANT |
| GET | `/auth/me` | ADMIN/TENANT | Thông tin user, không có password hash |

## Rooms and rental requests

- `GET /admin/rooms`, `GET /admin/rooms/{id}`, `POST /admin/rooms`, `PUT /admin/rooms/{id}`, `PATCH /admin/rooms/{id}/status`: ADMIN.
- `GET /rooms/available`: ADMIN/TENANT.
- TENANT: `POST /rental-requests`, `GET /rental-requests/mine`, `GET /rental-requests/{id}`, `PATCH /rental-requests/{id}/cancel`, `PATCH /rental-requests/{id}/supplement`.
- ADMIN: `GET /admin/rental-requests`, `GET /admin/rental-requests/{id}`, `PATCH /admin/rental-requests/{id}/approve`, `/reject`, `/request-info`, `GET /admin/rental-requests/{id}/history`.

## Tenants and contracts

- ADMIN: `GET /admin/tenants`, `GET /admin/tenants/{userId}`, `PATCH /admin/tenants/{userId}/profile`.
- TENANT: `GET/PATCH /tenant/profile`; user identity lấy từ JWT.
- ADMIN contracts: `GET/POST /admin/contracts`, `GET/PATCH /admin/contracts/{id}`, `PATCH /admin/contracts/{id}/submit|activate|renew|terminate|cancel`, `GET /admin/contracts/{id}/history|occupants`, `POST /admin/contracts/{id}/occupants`, `PATCH /admin/contracts/occupants/{occupantId}`, `/move-out`.
- TENANT: `GET /tenant/contracts`, `GET /tenant/contracts/{id}`; chỉ hợp đồng của chính tenant.

## Utilities, invoices and payments

- ADMIN utility: `GET/POST /admin/fee-configs`, `PATCH /admin/fee-configs/{id}`; `GET/POST /admin/services`, `PATCH /admin/services/{id}`; `GET/POST /admin/utility-readings`, `PATCH /admin/utility-readings/{id}`, `PATCH /admin/utility-readings/{id}/lock`.
- ADMIN invoices: `GET/POST /admin/invoices`, `GET /admin/invoices/{id}`, `PATCH /admin/invoices/{id}/issue`, `GET /admin/debts`.
- Tạo hóa đơn (`POST /admin/invoices`), tiền điện nước có 2 cách:
  - Nhập tay số tiêu thụ (`electricityUsage`, `waterUsage`): backend tính theo đơn giá cố định **3.500đ/kWh** điện và **30.000đ/m³** nước. Các trường `electricityAmount`/`waterAmount` client gửi lên bị bỏ qua; gửi số tiền mà không gửi số tiêu thụ sẽ bị từ chối (409).
  - Không nhập tay: backend dùng chỉ số đã chốt (`UtilityReadings`) và đơn giá hiệu lực trong `FeeConfigs` theo `electricityFeeCode`/`waterFeeCode`.
  - Nóng lạnh, vệ sinh và các phí khác là dịch vụ trong bảng `Services`; Admin chọn trong `services[]`, backend không tự cộng.
- TENANT invoices: `GET /tenant/invoices`, `GET /tenant/invoices/{id}`, `GET /tenant/debts`.
- ADMIN payments: `POST /admin/invoices/{invoiceId}/payments/manual`, `GET /admin/payments`, `GET /admin/invoices/{invoiceId}/payments`.
- TENANT payments: `GET /tenant/payments`, `GET /tenant/invoices/{invoiceId}/payments`, `GET /tenant/invoices/{invoiceId}/payment-qr`. QR không xác nhận thanh toán.

## Notifications and maintenance

- ADMIN notifications: `POST/GET /admin/notifications`.
- TENANT notifications: `GET /tenant/notifications`, `GET /tenant/notifications/unread-count`, `GET /tenant/notifications/{id}`, `PATCH /tenant/notifications/{id}/read`.
- TENANT maintenance: `POST /maintenance-requests`, `GET /maintenance-requests`, `GET /maintenance-requests/{id}`.
- ADMIN maintenance: `GET /admin/maintenance-requests`, `GET /admin/maintenance-requests/{id}`, `PATCH /admin/maintenance-requests/{id}`, `GET /admin/maintenance-requests/{id}/history`.

## Reports (ADMIN only)

- `GET /admin/reports/dashboard?asOf=yyyy-MM-dd`
- `GET /admin/reports/revenue?from=yyyy-MM-dd&to=yyyy-MM-dd`
- `GET /admin/reports/invoiced?from=yyyy-MM-dd&to=yyyy-MM-dd`
- `GET /admin/reports/debts?asOf=yyyy-MM-dd`
- `GET /admin/reports/invoice-items?from=yyyy-MM-dd&to=yyyy-MM-dd`
- `GET /admin/reports/rooms`, `/contracts`, `/maintenance`
- `GET /admin/reports/utilities?fromPeriod=yyyy-MM&toPeriod=yyyy-MM`

`occupancyRate` tính theo phòng `OCCUPIED/FULL` trên tổng phòng không `INACTIVE`. `revenue` chỉ dùng `Payments.SUCCEEDED` và `ConfirmedAt`.

## Error response

```json
{"code":"VALIDATION_FAILED","message":"Invalid request"}
```

Thông thường: `400` validation/nghiệp vụ đầu vào, `401` thiếu hoặc JWT không hợp lệ, `403` sai role/quyền sở hữu, `404` không tồn tại, `409` xung đột. Server không trả stack trace, password hash hoặc secret.
