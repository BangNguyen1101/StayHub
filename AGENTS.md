1. Bối cảnh và công nghệ cố định
Dự án: Website quản lý nhà trọ, ban đầu 30 phòng và có khả năng mở rộng.
    - Frontend: React + TypeScript + Tailwind CSS.
    - Backend: Java 21 + Spring Boot, Spring Security, REST API, Spring Scheduler.
    - Database: Microsoft SQL Server.
    - Biểu đồ: Recharts.
    - Vai trò: ADMIN và TENANT; mọi kiểm tra quyền phải được thực hiện ở backend.
    - Giao diện tiếng Việt, tiền VNĐ, ngày hiển thị dd/MM/yyyy.
    - Không có chức năng khóa/mở cửa hay quản lý khóa thông minh.
    - Không tự ý thay đổi stack, thêm framework lớn, chuyển kiến trúc microservices hoặc thêm vai trò mới.
2. Nguồn yêu cầu và thứ tự ưu tiên
    2.1. Yêu cầu mới nhất được người dùng xác nhận trong cuộc trò chuyện.
    2.2. Đặc tả chức năng của dự án được người dùng cung cấp.
    2.3. File AGENTS.md này.
    2.4. Quy ước đang tồn tại trong codebase.
    Nếu các nguồn mâu thuẫn, dừng tại phần bị ảnh hưởng, nêu rõ mâu thuẫn và hỏi người dùng. Không tự suy diễn quy tắc nghiệp vụ quan trọng.
3. Quy trình bắt buộc cho MỖI nhiệm vụ
    Bước A — Khảo sát (chưa sửa code)
    - Đọc cấu trúc dự án, file liên quan, cấu hình, migration và code có sẵn.
    - Xác định module, phụ thuộc, dữ liệu/API liên quan và nguy cơ ảnh hưởng tính năng cũ.
    - Không tạo lại những gì đã tồn tại nếu có thể tái sử dụng.
    Bước B — Lập kế hoạch
    - Trình bày mục tiêu, phạm vi, danh sách file dự kiến tạo/sửa, thứ tự các bước và cách kiểm thử.
    - Chỉ hỏi khi thiếu quyết định nghiệp vụ, có rủi ro mất dữ liệu, thay đổi kiến trúc/schema, hoặc yêu cầu mơ hồ.
    - Với thay đổi lớn (migration phá vỡ tương thích, sửa hợp đồng API, đổi cấu trúc dự án, thêm dependency lớn), phải chờ người dùng phê duyệt trước khi thực hiện.
    Bước C — Thực hiện tuần tự
    - Chỉ làm nhiệm vụ/module đang được giao; không tự chuyển sang module tiếp theo.
    - Trong module có thay đổi xuyên tầng, thực hiện theo thứ tự: quy tắc nghiệp vụ -> schema/migration (nếu cần và đã duyệt) -> entity/repository -> service -> controller/DTO/validation/security -> frontend API/types -> UI -> tests.
    - Thực hiện thay đổi nhỏ, tập trung, dễ kiểm tra; không refactor ngoài phạm vi.
    - Không viết code giả, API giả hoặc dữ liệu mock rồi báo là chức năng đã hoàn thiện. Mock chỉ được dùng khi người dùng yêu cầu rõ.
    Bước D — Kiểm thử và đối chiếu
    - Chạy các lệnh build/test/lint phù hợp với phần đã sửa nếu môi trường cho phép.
    - Kiểm tra các tình huống hợp lệ, không hợp lệ, phân quyền ADMIN/TENANT và dữ liệu thuộc người khác.
    - Không được báo 'đã kiểm thử thành công' nếu chưa thực sự chạy; ghi rõ lệnh, kết quả và hạn chế.
    - Nếu test thất bại, sửa lỗi trong phạm vi nhiệm vụ hoặc báo điểm chặn; không tự mở rộng phạm vi.
    Bước E — Báo cáo và dừng
    Sau mỗi nhiệm vụ, báo cáo ngắn theo mẫu:
    1. Đã hoàn thành.
    2. File đã tạo/sửa.
    3. Build/test đã chạy và kết quả.
    4. Điểm chưa hoàn thành, rủi ro hoặc quyết định cần người dùng xác nhận.
    5. Bước kế tiếp được đề xuất (CHƯA tự thực hiện).
    Dừng và chờ nhiệm vụ tiếp theo; không tự triển khai module kế tiếp.
4. Thứ tự các giai đoạn toàn dự án
    G0. Chốt đặc tả, quy tắc nghiệp vụ, use case, luồng trạng thái và tiêu chí nghiệm thu.
    G1. Thiết kế ERD, bảng/quan hệ/ràng buộc, chiến lược migration và dữ liệu mẫu; cần duyệt trước khi áp dụng schema.
    G2. Khởi tạo backend/frontend, cấu hình SQL Server, chuẩn lỗi API, bảo mật cơ bản.
    G3. Xác thực, phân quyền ADMIN/TENANT và kiểm tra quyền sở hữu dữ liệu.
    G4. Quản lý phòng, người thuê, đăng ký thuê, hợp đồng và người ở cùng.
    G5. Điện nước, dịch vụ, tiền cọc, hóa đơn và công nợ.
    G6. Thanh toán/đối soát, thông báo, tác vụ định kỳ, yêu cầu sửa chữa.
    G7. Dashboard, báo cáo, biểu đồ, quản trị hệ thống.
    G8. Kiểm thử tích hợp, kiểm thử bảo mật, hoàn thiện UI, tài liệu, triển khai.
    Thứ tự này là lộ trình mặc định, không phải giấy phép tự động làm toàn bộ. Chỉ bắt đầu giai đoạn/module khi người dùng giao việc. Nếu cần đổi thứ tự do phụ thuộc thực tế, giải thích và xin xác nhận.
5. Các ràng buộc nghiệp vụ không được vi phạm
    - Không hardcode giới hạn 30 phòng; 30 là dữ liệu khởi tạo, không phải giới hạn cố định.
    - TENANT chỉ được xem/thao tác dữ liệu của mình, phòng/hợp đồng liên quan hợp lệ và yêu cầu do mình tạo.
    - Không xóa cứng hợp đồng, hóa đơn, thanh toán đã phát sinh; giữ lịch sử và dấu vết thay đổi phù hợp.
    - Không sửa ngược giá trị lịch sử của hóa đơn khi bảng giá hiện tại thay đổi; lưu snapshot đơn giá tại thời điểm tính.
    - Không xác nhận thanh toán thành công chỉ vì QR đã được tạo/hiển thị hoặc người dùng báo đã chuyển tiền.
    - Tạo hóa đơn định kỳ, nhắc nợ phải chạy ở backend, độc lập việc người dùng mở website; tác vụ phải chống xử lý trùng.
    - Các thao tác tiền, cọc, hóa đơn, thanh toán, nhận/trả/chuyển phòng phải đảm bảo transaction và kiểm soát đồng thời khi cần.
    - Không tự quyết định quy tắc về ở ghép, người đại diện hợp đồng, tính tiền giữa tháng, chia chi phí, hoàn cọc hoặc chuyển phòng nếu đặc tả chưa chốt; hỏi trước.
    - Backend phải validate dữ liệu, phân quyền theo vai trò và theo quyền sở hữu tài nguyên; không chỉ ẩn nút ở frontend.
6. Quy tắc database và API
    - Dùng migration có phiên bản; không tự xóa database, DROP/TRUNCATE bảng hay xóa dữ liệu thật.
    - Thay đổi schema có thể gây mất dữ liệu hoặc không tương thích phải được duyệt trước.
    - Sử dụng khóa ngoại, ràng buộc duy nhất và chỉ mục phù hợp; tránh hóa đơn/giao dịch trùng.
    - Dùng kiểu số chính xác cho tiền (Java BigDecimal; SQL Server DECIMAL phù hợp), không dùng float/double cho tính tiền.
    - Không đưa entity JPA trực tiếp làm hợp đồng API nếu gây lộ dữ liệu hoặc vòng tham chiếu; dùng DTO và validation.
    - API phải có mã lỗi/trạng thái HTTP nhất quán, không tiết lộ mật khẩu, token hoặc thông tin nhạy cảm.
    - Không thay đổi endpoint/response đang được frontend sử dụng mà không rà soát tác động.
7. Quy tắc bảo mật và chất lượng
    - Không hardcode mật khẩu, token, khóa bí mật, thông tin kết nối thật; dùng biến môi trường/cấu hình an toàn.
    - Không commit file .env chứa bí mật hoặc log dữ liệu nhạy cảm.
    - Không bỏ qua Spring Security, không tắt xác thực chỉ để demo.
    - Code rõ ràng, đặt tên nhất quán, tránh trùng lặp, không tạo file/thư mục thừa.
    - Ưu tiên sửa tối thiểu để đáp ứng yêu cầu; giữ các chức năng đang chạy.
    - Không tự cài dependency mới nếu chưa chứng minh cần thiết; dependency lớn phải xin duyệt.
    - Nếu môi trường không cho chạy test hoặc kết nối SQL Server, nêu rõ và không bịa kết quả.
8. Những hành động cần xin phép rõ ràng
    - Xóa/ghi đè dữ liệu, reset database, chạy migration phá vỡ tương thích.
    - Đổi stack, cấu trúc kiến trúc, quy tắc nghiệp vụ hoặc phân quyền.
    - Refactor diện rộng, đổi tên hàng loạt file/API/bảng.
    - Cài đặt dịch vụ bên ngoài, tích hợp thanh toán thật, gửi email/SMS thật hoặc triển khai production.
    - Thực hiện công việc ngoài phạm vi yêu cầu hiện tại.
9. Quy tắc kết thúc
    Không đánh dấu nhiệm vụ hoàn thành khi chưa đối chiếu tiêu chí nghiệm thu. Không tự động bắt đầu bước tiếp theo. Khi thiếu thông tin quan trọng, nêu phương án và chờ người dùng quyết định thay vì tự đoán.