# MODULE 1. XÁC THỰC VÀ PHÂN QUYỀN

## 1.1. Đăng nhập
- Người dùng đăng nhập bằng email hoặc tên đăng nhập và mật khẩu.
- Backend kiểm tra tài khoản có tồn tại, mật khẩu đúng và tài khoản còn hoạt động hay không.
- Nếu đăng nhập thành công, hệ thống tạo phiên đăng nhập hoặc token xác thực.
- Nếu sai thông tin, hiển thị thông báo dễ hiểu, không tiết lộ tài khoản có tồn tại hay không.
- Sau khi đăng nhập, Admin chuyển đến Dashboard quản trị; người thuê chuyển đến Dashboard cá nhân.
- Người chưa đăng nhập không được truy cập các trang yêu cầu xác thực.

## 1.2. Đăng xuất
- Cho phép người dùng chủ động đăng xuất.
- Hủy hoặc vô hiệu hóa phiên/token theo cơ chế xác thực được sử dụng.
- Xóa thông tin xác thực lưu ở phía client theo cách an toàn.
- Sau khi đăng xuất, chuyển về trang đăng nhập.

## 1.3. Đổi mật khẩu
- Người dùng nhập mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu mới.
- Backend xác minh mật khẩu hiện tại.
- Mật khẩu mới phải đáp ứng chính sách độ dài và độ phức tạp được cấu hình.
- Hai trường mật khẩu mới phải khớp nhau.
- Mật khẩu được băm an toàn trước khi lưu vào database, không lưu mật khẩu dạng văn bản thuần.
- Sau khi đổi mật khẩu thành công, thông báo kết quả và yêu cầu đăng nhập lại nếu chính sách bảo mật quy định.

## 1.4. Quên mật khẩu
- Người dùng nhập email đã đăng ký.
- Hệ thống gửi liên kết đặt lại mật khẩu có mã xác thực ngẫu nhiên, thời hạn ngắn và chỉ sử dụng một lần.
- Không tiết lộ email có tồn tại trong hệ thống hay không.
- Khi đặt lại thành công, mã xác thực cũ không được sử dụng lại.

## 1.5. Quản lý tài khoản
- Admin xem danh sách tài khoản, tìm kiếm theo tên, email, vai trò và trạng thái.
- Admin tạo tài khoản, cập nhật thông tin cơ bản, khóa hoặc kích hoạt tài khoản.
- Admin không được xem mật khẩu gốc của người dùng.
- Không cho phép tài khoản tự nâng quyền từ TENANT lên ADMIN.
- Không cho phép xóa hoặc vô hiệu hóa tài khoản Admin cuối cùng đang có quyền quản trị.
- Khi tài khoản bị khóa, các yêu cầu đăng nhập tiếp theo phải bị từ chối.

## 1.6. Phân quyền
- ADMIN được truy cập toàn bộ chức năng quản trị.
- TENANT chỉ được xem hồ sơ, hợp đồng, hóa đơn, thanh toán và yêu cầu sửa chữa của bản thân.
- TENANT không được tự sửa giá thuê, tiền cọc, trạng thái thanh toán hoặc trạng thái hợp đồng.
- Mọi API phải kiểm tra quyền ở backend.
- Không tin vào user ID, room ID hoặc role do frontend tự gửi lên mà chưa xác minh.
- Các thao tác nhạy cảm phải được ghi vào nhật ký hoạt động.

# MODULE 2. DASHBOARD

## 2.1. Dashboard Admin
- Hiển thị tổng số phòng đang được quản lý.
- Hiển thị số phòng trống, đang thuê, còn chỗ ở ghép, đã đủ sức chứa, đang bảo trì và ngừng khai thác.
- Hiển thị tỷ lệ lấp đầy.
- Hiển thị tổng tiền hóa đơn trong kỳ.
- Hiển thị số tiền đã thu, số tiền còn nợ và số hóa đơn quá hạn.
- Hiển thị số hợp đồng sắp hết hạn trong khoảng thời gian cấu hình, mặc định 30 ngày.
- Hiển thị số yêu cầu đăng ký thuê đang chờ duyệt.
- Hiển thị số yêu cầu sửa chữa chưa xử lý.
- Hiển thị biểu đồ doanh thu theo tháng.
- Hiển thị danh sách hoạt động gần đây như duyệt thuê, tạo hợp đồng, ghi nhận thanh toán.
- Các số liệu phải lấy từ database thông qua backend, không được nhập số liệu giả vào giao diện.

## 2.2. Dashboard người thuê
- Hiển thị thông tin phòng đang thuê.
- Hiển thị giá thuê, tiền cọc, ngày bắt đầu và ngày kết thúc hợp đồng.
- Hiển thị hóa đơn gần nhất và tổng số tiền còn phải trả.
- Hiển thị danh sách hóa đơn chưa thanh toán hoặc sắp đến hạn.
- Hiển thị trạng thái các giao dịch thanh toán gần đây.
- Hiển thị thông báo chưa đọc.
- Hiển thị trạng thái các yêu cầu sửa chữa do người thuê tạo.
- Nếu người dùng chưa có hợp đồng đang hoạt động, hiển thị hướng dẫn đăng ký thuê phòng.

## 2.3. Quy tắc tính toán
- Tỷ lệ lấp đầy được tính dựa trên tiêu chí sức chứa hoặc số phòng có người ở; phải chọn một cách tính thống nhất và ghi rõ trên giao diện.
- Doanh thu đã thu chỉ tính các khoản thanh toán đã được xác nhận hợp lệ.
- Công nợ được tính từ số tiền hóa đơn phải trả trừ số tiền đã thanh toán hợp lệ và các khoản điều chỉnh được phê duyệt.
- Dashboard phải phản ánh dữ liệu mới sau khi hóa đơn, hợp đồng hoặc thanh toán thay đổi.

# MODULE 3. QUẢN LÝ PHÒNG

## 3.1. Danh sách phòng
- Hiển thị các phòng dưới dạng bảng hoặc thẻ.
- Mỗi phòng có mã phòng, tên phòng, tầng, diện tích, giá thuê, sức chứa tối đa, số người đang ở và trạng thái.
- Cho phép tìm kiếm theo mã phòng.
- Cho phép lọc theo trạng thái, tầng, mức giá và khả năng ở ghép.
- Cho phép sắp xếp theo mã phòng, giá thuê hoặc trạng thái.
- Có trang chi tiết phòng để xem thông tin, người thuê, hợp đồng hiện tại và lịch sử thuê.

## 3.2. Thêm và sửa phòng
- Admin nhập mã phòng, tên phòng nếu cần, tầng, diện tích, giá thuê cơ bản, sức chứa và trạng thái khai thác.
- Mã phòng phải duy nhất.
- Diện tích, giá thuê và sức chứa phải là giá trị hợp lệ.
- Sức chứa tối đa phải lớn hơn hoặc bằng 1.
- Admin có thể cấu hình phòng có cho phép ở ghép hay không.
- Không cho phép thay đổi sức chứa xuống thấp hơn số người đang thuê hợp lệ.
- Không cho phép xóa phòng có hợp đồng đang hoạt động hoặc dữ liệu tài chính liên quan.
- Phòng không còn sử dụng được chuyển sang trạng thái ngừng khai thác.

## 3.3. Trạng thái phòng
- VACANT: Phòng trống, chưa có người thuê.
- OCCUPIED: Phòng đang có người thuê và chưa đạt sức chứa tối đa.
- FULL: Phòng đã đạt sức chứa tối đa.
- MAINTENANCE: Phòng đang bảo trì, không nhận đăng ký mới.
- INACTIVE: Phòng ngừng khai thác.

Các trạng thái có thể hiển thị bằng nhãn màu khác nhau để Admin dễ nhận biết.

## 3.4. Quy tắc ở ghép
- Một phòng có thể có nhiều người thuê nếu được phép ở ghép và chưa đạt sức chứa.
- Tổng số người thuê hợp lệ không được vượt quá sức chứa tối đa.
- Chỉ tính người đang có hợp đồng hoặc hồ sơ cư trú hợp lệ theo nghiệp vụ đã cấu hình.
- Khi một người kết thúc thuê, hệ thống cập nhật số người còn lại và trạng thái phòng.
- Khi có nhiều yêu cầu đăng ký cùng một phòng, backend phải kiểm tra sức chứa tại thời điểm duyệt để tránh vượt số người tối đa.
- Không tự động chuyển trạng thái phòng chỉ dựa trên dữ liệu frontend.

# MODULE 4. ĐĂNG KÝ THUÊ PHÒNG

## 4.1. Người thuê đăng ký
- Người dùng có thể xem danh sách phòng đang nhận đăng ký.
- Hiển thị giá thuê, sức chứa còn lại, điều kiện ở ghép và trạng thái phòng.
- Người dùng chọn phòng và nhập thông tin liên hệ, số người dự kiến ở, ngày dự kiến nhận phòng và ghi chú.
- Người dùng gửi yêu cầu đăng ký.
- Hệ thống tạo mã đăng ký duy nhất và lưu thời gian gửi.
- Người dùng chỉ được xem các yêu cầu do chính mình tạo.
- Không cho phép gửi yêu cầu cho phòng ngừng khai thác, đang bảo trì hoặc không còn chỗ.
- Việc gửi yêu cầu không đồng nghĩa với việc đã được giữ phòng hoặc đã có hợp đồng.

## 4.2. Admin xử lý đăng ký
- Hiển thị danh sách yêu cầu theo trạng thái và thời gian gửi.
- Cho phép tìm kiếm theo mã đăng ký, người đăng ký và phòng.
- Admin xem chi tiết thông tin, phòng đăng ký và lịch sử xử lý.
- Admin có thể duyệt, từ chối hoặc yêu cầu bổ sung thông tin.
- Khi từ chối, cần ghi lý do.
- Khi yêu cầu bổ sung thông tin, ghi nội dung cần bổ sung và thông báo cho người đăng ký.
- Mỗi lần đổi trạng thái phải lưu người thực hiện và thời gian.

## 4.3. Trạng thái đăng ký
- PENDING: Chờ xử lý.
- NEEDS_INFO: Chờ người đăng ký bổ sung thông tin.
- APPROVED: Đã được duyệt.
- REJECTED: Bị từ chối.
- CANCELLED: Đã hủy.
- EXPIRED: Đã hết hạn theo quy tắc được cấu hình.

## 4.4. Quy tắc nghiệp vụ
- Người đăng ký chỉ được hủy yêu cầu khi trạng thái cho phép.
- Admin không được duyệt yêu cầu nếu phòng không còn đủ sức chứa.
- Khi duyệt, hệ thống phải kiểm tra lại tình trạng phòng trong cùng giao dịch database với thao tác duyệt.
- Một yêu cầu đã bị từ chối hoặc hủy không được chuyển thẳng sang đã duyệt nếu không có quy trình mở lại rõ ràng.
- Duyệt đăng ký chưa tự động đồng nghĩa với hợp đồng đang hoạt động. Admin cần tạo hợp đồng hoặc thực hiện bước tiếp nhận theo quy trình.

# MODULE 5. QUẢN LÝ NGƯỜI THUÊ

## 5.1. Hồ sơ người thuê
- Admin xem danh sách người thuê.
- Thông tin gồm họ tên, số điện thoại, email, địa chỉ liên hệ, thông tin giấy tờ cần thiết theo quy định và ghi chú.
- Chỉ thu thập dữ liệu cá nhân thật sự cần thiết.
- Cho phép tìm kiếm theo tên, số điện thoại hoặc email.
- Cho phép cập nhật thông tin liên hệ.
- Ghi lại thời gian và người cập nhật dữ liệu quan trọng.

## 5.2. Chi tiết người thuê
- Hiển thị hồ sơ cá nhân.
- Hiển thị phòng đang thuê, hợp đồng hiện tại và các hợp đồng cũ.
- Hiển thị lịch sử hóa đơn, thanh toán và công nợ.
- Hiển thị lịch sử chuyển phòng.
- Hiển thị yêu cầu sửa chữa đã gửi.
- Hiển thị trạng thái tài khoản.

## 5.3. Chuyển phòng
- Admin chọn người thuê và phòng mới.
- Hệ thống kiểm tra phòng mới có hoạt động và còn sức chứa hay không.
- Xác định ngày chuyển phòng, giá thuê mới, cách xử lý tiền cọc và công nợ của phòng cũ.
- Lưu lịch sử chuyển phòng.
- Cập nhật hợp đồng theo quy trình đã chọn: kết thúc hợp đồng cũ và tạo hợp đồng mới, hoặc lập phụ lục điều chỉnh nếu nghiệp vụ cho phép.
- Không xóa lịch sử của phòng cũ.
- Các khoản phí phát sinh phải được ghi rõ và không tự ý ghi đè hóa đơn đã xác nhận.

## 5.4. Kết thúc thuê
- Admin ghi nhận ngày trả phòng thực tế.
- Kiểm tra hợp đồng, công nợ, tiền cọc và chi phí phát sinh.
- Ghi nhận phương án hoàn cọc hoặc khấu trừ cọc theo quy trình được phê duyệt.
- Cập nhật trạng thái hợp đồng và người thuê.
- Cập nhật số người và trạng thái phòng.
- Lưu lịch sử kết thúc thuê.
- Không xóa hồ sơ người thuê hoặc lịch sử thanh toán.

# MODULE 6. QUẢN LÝ HỢP ĐỒNG

## 6.1. Tạo hợp đồng
- Admin chọn người thuê và phòng.
- Hệ thống kiểm tra phòng hợp lệ và sức chứa.
- Nhập mã hợp đồng, ngày bắt đầu, ngày kết thúc, giá thuê, tiền cọc, chu kỳ thanh toán và ngày đến hạn.
- Có thể nhập các điều khoản hoặc ghi chú hợp đồng.
- Mã hợp đồng phải duy nhất.
- Hệ thống cho xem lại thông tin trước khi lưu.
- Khi hợp đồng được kích hoạt, cập nhật số người đang thuê và trạng thái phòng trong một giao dịch nghiệp vụ nhất quán.
- Không cho phép kích hoạt hợp đồng nếu phòng đã hết sức chứa.

## 6.2. Quản lý hợp đồng
- Admin xem danh sách hợp đồng và lọc theo phòng, người thuê, ngày hết hạn và trạng thái.
- Xem chi tiết tiền thuê, tiền cọc, thời hạn và lịch sử điều chỉnh.
- Có thể gia hạn hợp đồng bằng cách ghi nhận thời hạn mới và người phê duyệt.
- Có thể điều chỉnh giá thuê theo quy trình có hiệu lực từ ngày cụ thể.
- Không tự ý sửa dữ liệu lịch sử của hợp đồng đã kết thúc.
- Các thay đổi quan trọng cần có lịch sử trước và sau khi cập nhật.

## 6.3. Trạng thái hợp đồng
- DRAFT: Bản nháp.
- PENDING: Chờ xác nhận hoặc hoàn tất thủ tục.
- ACTIVE: Đang có hiệu lực.
- EXPIRING: Sắp hết hạn, là trạng thái cảnh báo có thể tính tự động từ ngày hết hạn.
- EXPIRED: Đã hết hạn.
- TERMINATED: Đã kết thúc trước hạn hoặc đã thanh lý theo quy trình.
- CANCELLED: Đã hủy trước khi có hiệu lực, nếu nghiệp vụ cho phép.

## 6.4. Gia hạn và thanh lý
- Hệ thống cảnh báo hợp đồng sắp hết hạn theo mốc cấu hình, mặc định 30 ngày.
- Admin có thể tạo đề nghị gia hạn hoặc cập nhật thời hạn theo quy trình được phê duyệt.
- Khi thanh lý, ghi nhận ngày kết thúc, công nợ cuối cùng, tiền cọc, khoản hoàn trả hoặc khấu trừ.
- Chỉ kết thúc hợp đồng khi người có quyền thực hiện và dữ liệu bắt buộc đã được kiểm tra.
- Khi hợp đồng hết hạn, hệ thống cập nhật trạng thái và thông báo; việc tiếp tục ở hoặc gia hạn phải theo quy trình được xác định, không tự động xóa quyền lợi hay dữ liệu của người thuê.

# MODULE 7. ĐIỆN, NƯỚC VÀ DỊCH VỤ

## 7.1. Cấu hình đơn giá
- Admin thiết lập đơn giá điện, nước và các khoản dịch vụ.
- Có thể cấu hình theo kỳ hiệu lực để giữ lịch sử giá cũ.
- Mỗi khoản phí có tên, đơn vị tính, đơn giá, trạng thái và ngày bắt đầu áp dụng.
- Chỉ cho phép đơn giá không âm.
- Khi đơn giá thay đổi, hóa đơn đã tạo không tự động thay đổi theo nếu chưa có quy trình điều chỉnh được phê duyệt.

## 7.2. Nhập chỉ số điện nước
- Admin chọn phòng và kỳ ghi chỉ số.
- Nhập chỉ số đầu kỳ và cuối kỳ của điện, nước.
- Số tiêu thụ bằng chỉ số cuối kỳ trừ chỉ số đầu kỳ.
- Không chấp nhận chỉ số cuối kỳ nhỏ hơn chỉ số đầu kỳ, trừ khi có quy trình xử lý đồng hồ thay mới hoặc điều chỉnh được ghi nhận.
- Mỗi phòng chỉ có một bộ ghi chỉ số hợp lệ cho cùng loại dịch vụ và kỳ, trừ trường hợp dữ liệu được điều chỉnh có lưu lịch sử.
- Cho phép nhập ghi chú và ngày ghi chỉ số.

## 7.3. Tính phí
- Tiền điện bằng số điện tiêu thụ nhân đơn giá điện áp dụng.
- Tiền nước bằng số nước tiêu thụ nhân đơn giá nước áp dụng.
- Tính thêm phí dịch vụ như Internet, vệ sinh hoặc gửi xe theo cấu hình.
- Hỗ trợ phí cố định theo phòng, theo người hoặc theo mức sử dụng nếu nghiệp vụ cần.
- Với phòng ở ghép, cho phép cấu hình cách chia phí: chia đều theo số người, theo định mức hoặc tính theo phòng.
- Quy tắc làm tròn tiền phải thống nhất.
- Hiển thị chi tiết cách tính để Admin kiểm tra trước khi tạo hóa đơn.

## 7.3.1. Quy tắc đơn giá đang áp dụng trong hệ thống
- Khi Admin nhập tay số điện/nước trên form hóa đơn, đơn giá là cố định: điện 3.500đ/kWh, nước 30.000đ/m³. Backend tự tính tiền từ số tiêu thụ và lưu snapshot đơn giá vào chi tiết hóa đơn, không dùng số tiền do frontend gửi.
- Khi dùng chỉ số đã chốt (UtilityReadings), đơn giá lấy từ cấu hình FeeConfigs đang hiệu lực trong kỳ.
- Phí nóng lạnh, vệ sinh và các dịch vụ khác được cấu hình trong danh mục dịch vụ (Services) với đơn giá và cách tính theo phòng/người/mức sử dụng; Admin chọn dịch vụ áp dụng khi tạo hóa đơn, hệ thống không tự cộng.

## 7.4. Điều chỉnh dữ liệu
- Cho phép sửa chỉ số chưa được chốt vào hóa đơn.
- Nếu chỉ số đã được sử dụng trong hóa đơn, phải có quyền phù hợp và lưu lịch sử điều chỉnh.
- Không âm thầm thay đổi hóa đơn đã thanh toán.
- Mọi khoản chênh lệch sau điều chỉnh phải được xử lý bằng nghiệp vụ điều chỉnh tài chính có dấu vết.

# MODULE 8. HÓA ĐƠN VÀ CÔNG NỢ

## 8.1. Tạo hóa đơn
- Admin có thể tạo hóa đơn thủ công hoặc sử dụng chức năng tạo hóa đơn định kỳ.
- Hóa đơn gắn với phòng, người chịu trách nhiệm thanh toán, hợp đồng và kỳ thanh toán.
- Hóa đơn gồm tiền thuê, điện, nước, dịch vụ, khoản điều chỉnh và tổng số tiền.
- Mỗi hóa đơn có mã duy nhất, ngày phát hành, hạn thanh toán và trạng thái.
- Khi tạo hóa đơn, lưu chi tiết các khoản phí và đơn giá đã áp dụng.
- Hóa đơn đã phát hành không tự thay đổi nếu bảng giá được cập nhật sau đó.
- Chống tạo trùng hóa đơn cho cùng hợp đồng và kỳ thanh toán, trừ khi có lý do nghiệp vụ được ghi nhận.

## 8.2. Trạng thái hóa đơn
- DRAFT: Bản nháp.
- UNPAID: Chưa thanh toán.
- PARTIALLY_PAID: Đã thanh toán một phần.
- PAID: Đã thanh toán đủ.
- OVERDUE: Đã quá hạn và còn số dư phải trả.
- CANCELLED: Đã hủy hợp lệ.

Trạng thái quá hạn phải được xác định dựa trên ngày đến hạn và số tiền còn phải trả. Hóa đơn đã thanh toán đủ không được chuyển thành quá hạn.

## 8.3. Theo dõi công nợ
- Hiển thị tổng tiền phải trả, tổng đã thanh toán và số dư còn nợ.
- Cho phép lọc theo người thuê, phòng, kỳ hóa đơn và trạng thái.
- Hiển thị danh sách hóa đơn quá hạn và số ngày quá hạn.
- Người thuê chỉ xem công nợ của chính mình hoặc các hóa đơn mình có trách nhiệm thanh toán.
- Admin có thể ghi nhận khoản điều chỉnh hoặc hủy hóa đơn theo quyền và quy trình.
- Không cho phép số tiền đã thanh toán vượt quá số tiền còn phải thu nếu chưa hỗ trợ nghiệp vụ trả thừa hoặc hoàn tiền rõ ràng.
- Khi nhận thanh toán hợp lệ, hệ thống cập nhật số tiền đã trả và số dư còn nợ trong một giao dịch database nhất quán.

## 8.4. Tạo hóa đơn định kỳ
- Admin cấu hình ngày tạo hóa đơn, chu kỳ thanh toán và ngày đến hạn.
- Backend chạy tác vụ định kỳ, không phụ thuộc trình duyệt.
- Chỉ tạo hóa đơn cho hợp đồng phù hợp với kỳ thanh toán.
- Xử lý trường hợp hợp đồng bắt đầu hoặc kết thúc giữa tháng theo quy tắc tính phí đã cấu hình.
- Đảm bảo tác vụ chạy lại không tạo hóa đơn trùng.
- Ghi log số hóa đơn tạo thành công, bỏ qua hoặc lỗi.

# MODULE 9. THANH TOÁN QR

## 9.1. Chọn phương thức thanh toán
- Người thuê mở hóa đơn và nhấn Thanh toán.
- Hiển thị số tiền còn phải trả, mã hóa đơn và hạn thanh toán.
- Cho phép chọn VietQR/chuyển khoản ngân hàng, MoMo hoặc VNPAY tùy theo cấu hình tích hợp đang hoạt động.
- Không hiển thị phương thức chưa được cấu hình hoặc chưa được bật ở môi trường hiện tại.

## 9.2. Tạo giao dịch
- Backend tạo mã giao dịch nội bộ duy nhất.
- Backend kiểm tra hóa đơn thuộc người thuê và còn số dư phải thanh toán.
- Số tiền giao dịch không được vượt số dư còn phải trả, trừ khi hệ thống hỗ trợ rõ ràng nghiệp vụ thanh toán thừa.
- Với VietQR, tạo QR chứa thông tin nhận tiền, số tiền và nội dung chuyển khoản theo dịch vụ đã tích hợp.
- Với MoMo hoặc VNPAY, backend gửi yêu cầu theo tài liệu chính thức của nhà cung cấp và nhận dữ liệu trả về.
- Lưu nhà cung cấp, mã giao dịch, hóa đơn liên quan, số tiền, thời điểm tạo và thời hạn giao dịch.
- Secret Key và thông tin xác thực chỉ được sử dụng ở backend.
- Không tạo giao dịch bằng cách để frontend tự quyết định số tiền hợp lệ.

## 9.3. Xác nhận thanh toán
- Không đánh dấu hóa đơn đã thanh toán chỉ vì người dùng quay lại trang website.
- Backend tiếp nhận callback, webhook hoặc IPN từ nguồn thanh toán đã tích hợp.
- Xác minh chữ ký hoặc cơ chế xác thực tương ứng với tài liệu của nhà cung cấp.
- Kiểm tra mã giao dịch, số tiền, đơn vị tiền tệ, hóa đơn và trạng thái giao dịch.
- Chống xử lý callback trùng lặp.
- Chỉ ghi nhận thanh toán thành công khi nhận được kết quả hợp lệ.
- Nếu giao dịch thất bại, bị hủy hoặc hết hạn, không tự động cộng tiền đã thu.
- Nếu cần xác nhận thủ công, chỉ Admin có quyền mới được xác nhận và phải ghi lại người thực hiện cùng bằng chứng đối chiếu.

## 9.4. Lịch sử thanh toán
- Hiển thị mã giao dịch, hóa đơn, số tiền, phương thức, thời gian và trạng thái.
- Người thuê chỉ xem giao dịch của mình.
- Admin xem và lọc giao dịch theo thời gian, nhà cung cấp và trạng thái.
- Lưu lịch sử callback hoặc kết quả xử lý cần thiết để đối soát.
- Hỗ trợ tra cứu các giao dịch đang chờ xác nhận.
- Có quy trình xử lý thanh toán trùng, sai số tiền, chuyển khoản thiếu hoặc giao dịch chưa ghép được hóa đơn.
- Không xóa giao dịch đã thành công. Nếu cần hoàn tiền hoặc điều chỉnh, tạo bản ghi nghiệp vụ mới.

## 9.5. Chế độ phát triển
- Có cấu hình riêng cho sandbox và production.
- Khi chưa có thông tin merchant, sử dụng mock provider chỉ trong môi trường phát triển.
- Mock provider phải được đánh dấu rõ là giả lập và không được xác nhận tiền thật.
- Không sử dụng dữ liệu giả để làm sai lệch công nợ trên môi trường production.
- Không lưu API secret trong source code hoặc repository.

# MODULE 10. THÔNG BÁO

## 10.1. Thông báo trong website
- Tạo thông báo khi đăng ký thuê được duyệt, từ chối hoặc yêu cầu bổ sung thông tin.
- Thông báo khi có hợp đồng mới, hợp đồng sắp hết hạn hoặc hợp đồng được gia hạn.
- Thông báo khi hóa đơn mới được phát hành.
- Nhắc hạn thanh toán trước ngày đến hạn theo cấu hình.
- Thông báo khi hóa đơn quá hạn.
- Thông báo khi thanh toán được xác nhận hoặc thất bại.
- Thông báo khi yêu cầu sửa chữa được tiếp nhận hoặc thay đổi trạng thái.
- Hiển thị số thông báo chưa đọc.
- Cho phép mở chi tiết và đánh dấu đã đọc.

## 10.2. Email
- Có thể gửi email khi tạo tài khoản, đặt lại mật khẩu, phát hành hóa đơn hoặc xác nhận thanh toán.
- Cấu hình SMTP hoặc dịch vụ email qua biến môi trường.
- Không gửi mật khẩu gốc qua email.
- Nếu gửi thất bại, ghi log và có cơ chế thử lại an toàn.
- Không gửi thông báo lặp vô hạn khi tác vụ được chạy lại.

## 10.3. Quy tắc
- Mỗi thông báo gắn với người nhận, nội dung, loại, thời gian và trạng thái đọc.
- Người thuê không được xem thông báo của người thuê khác.
- Tác vụ nhắc nợ phải tránh gửi lặp trong cùng một đợt nhắc.
- Nội dung thông báo phải dựa trên dữ liệu thực tế trong hệ thống.

# MODULE 11. YÊU CẦU SỬA CHỮA

## 11.1. Người thuê gửi yêu cầu
- Người thuê chọn loại sự cố như điện, nước, thiết bị trong phòng hoặc hư hỏng khác.
- Nhập tiêu đề, mô tả chi tiết và mức độ ưu tiên.
- Có thể đính kèm hình ảnh nếu chức năng tải tệp được bật.
- Hệ thống tự xác định phòng và người gửi dựa trên hợp đồng phù hợp, không tin room ID do frontend tự khai báo.
- Tạo mã yêu cầu duy nhất và ghi nhận thời điểm gửi.
- Người thuê chỉ xem yêu cầu do mình tạo hoặc được phép xem theo chính sách.

## 11.2. Admin xử lý
- Hiển thị danh sách yêu cầu và lọc theo phòng, loại sự cố, thời gian, mức ưu tiên và trạng thái.
- Admin xem chi tiết mô tả và hình ảnh.
- Có thể phân công người xử lý, ghi chú và cập nhật tiến độ.
- Ghi nhận chi phí dự kiến và chi phí thực tế nếu cần.
- Khi hoàn thành, ghi mô tả kết quả xử lý.
- Thông báo cho người thuê khi trạng thái thay đổi.

## 11.3. Trạng thái yêu cầu
- OPEN: Mới tiếp nhận.
- IN_PROGRESS: Đang xử lý.
- WAITING: Chờ vật tư hoặc thông tin.
- RESOLVED: Đã xử lý.
- CLOSED: Đã đóng yêu cầu.
- REJECTED: Từ chối xử lý, bắt buộc có lý do.

## 11.4. Bảo mật tệp đính kèm
- Kiểm tra loại tệp, kích thước và quyền truy cập.
- Không cho phép tải hoặc xem tệp của người khác nếu không có quyền.
- Không tin tên tệp hoặc phần mở rộng do người dùng gửi.
- Lưu đường dẫn và thông tin tệp an toàn, tránh thực thi tệp tải lên.

# MODULE 12. BÁO CÁO VÀ THỐNG KÊ

## 12.1. Báo cáo phòng
- Thống kê tổng số phòng, phòng trống, phòng đang thuê, phòng đầy, phòng bảo trì và phòng ngừng khai thác.
- Tính tỷ lệ lấp đầy theo phương pháp được cấu hình.
- Thống kê số người thuê và sức chứa còn lại.
- Cho phép lọc theo thời gian, tầng hoặc trạng thái.

## 12.2. Báo cáo doanh thu
- Thống kê số tiền đã thu theo ngày, tháng, quý hoặc năm.
- Phân biệt tiền đã thu thực tế với số tiền hóa đơn đã phát hành.
- Chỉ tính giao dịch thanh toán hợp lệ đã xác nhận.
- Hỗ trợ lọc theo phương thức thanh toán.
- Hiển thị biểu đồ doanh thu theo thời gian.

## 12.3. Báo cáo công nợ
- Tổng số tiền còn phải thu.
- Tổng số hóa đơn chưa thanh toán.
- Danh sách hóa đơn quá hạn.
- Phân loại theo số ngày quá hạn.
- Thống kê công nợ theo phòng và người chịu trách nhiệm thanh toán.
- Không cộng trùng một khoản nợ khi hóa đơn có nhiều giao dịch thanh toán.

## 12.4. Báo cáo hợp đồng
- Tổng số hợp đồng đang hoạt động.
- Danh sách hợp đồng sắp hết hạn.
- Danh sách hợp đồng đã hết hạn hoặc kết thúc.
- Thống kê hợp đồng mới theo thời gian.
- Lưu lịch sử để báo cáo không bị mất dữ liệu khi người thuê chuyển phòng.

## 12.5. Xuất dữ liệu
- Cho phép xuất các báo cáo được phân quyền thành CSV hoặc Excel.
- File xuất phải áp dụng cùng điều kiện lọc với màn hình.
- Không xuất thông tin cá nhân không cần thiết.
- Các giá trị xuất ra phải lấy từ backend, không chỉ lấy các dòng đang hiển thị trên một trang của frontend.
- Kiểm tra quyền trước khi tạo hoặc tải file.

# MODULE 13. QUẢN TRỊ HỆ THỐNG

## 13.1. Nhật ký hoạt động
- Ghi nhận các thao tác quan trọng: đăng nhập thất bại, tạo tài khoản, thay đổi giá thuê, duyệt đăng ký, tạo hoặc sửa hợp đồng, điều chỉnh hóa đơn, xác nhận thanh toán và thay đổi cấu hình.
- Mỗi bản ghi gồm người thực hiện, hành động, đối tượng bị tác động, thời gian, kết quả và thông tin cần thiết để kiểm tra.
- Với thay đổi quan trọng, lưu dữ liệu trước và sau theo chính sách bảo mật.
- Chỉ Admin có quyền phù hợp mới được xem nhật ký.
- Người dùng thông thường không được sửa hoặc xóa nhật ký.
- Không ghi mật khẩu, token, API secret hoặc thông tin nhạy cảm vào log.

## 13.2. Cấu hình hệ thống
- Cấu hình tên nhà trọ và thông tin liên hệ.
- Cấu hình tiền tệ, múi giờ và các quy tắc ngày tháng.
- Cấu hình chu kỳ tạo hóa đơn, hạn thanh toán và lịch nhắc nợ.
- Cấu hình ngưỡng cảnh báo hợp đồng sắp hết hạn.
- Cấu hình thông tin nhận tiền và phương thức thanh toán đang bật.
- Cấu hình sandbox hoặc production theo biến môi trường.
- Thông tin bí mật phải nằm trong biến môi trường hoặc hệ thống quản lý secret, không lưu trực tiếp vào frontend.

## 13.3. Tác vụ tự động
- Tạo hóa đơn định kỳ.
- Kiểm tra hóa đơn đến hạn hoặc quá hạn.
- Gửi thông báo nhắc thanh toán.
- Cảnh báo hợp đồng sắp hết hạn.
- Kiểm tra giao dịch thanh toán đang chờ xử lý nếu nhà cung cấp hỗ trợ tra cứu.
- Thử lại tác vụ thất bại theo chính sách giới hạn.
- Ghi log kết quả mỗi lần chạy.
- Đảm bảo các tác vụ có tính idempotent, tức là chạy lại không tạo hóa đơn hoặc khoản thanh toán trùng.

## 13.4. Sao lưu và khôi phục
- Có quy trình sao lưu database định kỳ.
- Sao lưu phải được bảo vệ bằng quyền truy cập phù hợp.
- Có hướng dẫn khôi phục dữ liệu và kiểm tra tính toàn vẹn.
- Không để bản sao lưu chứa thông tin bí mật ở nơi công khai.
- Kiểm thử khôi phục định kỳ, không chỉ kiểm tra việc tạo file sao lưu.

## 13.5. Xử lý lỗi và bảo mật
- Backend trả mã lỗi và thông báo nhất quán.
- Không trả stack trace hoặc thông tin nội bộ cho người dùng production.
- Kiểm tra dữ liệu đầu vào ở cả frontend và backend, trong đó backend là nơi quyết định dữ liệu có hợp lệ hay không.
- Có cơ chế phân trang cho danh sách lớn.
- Bảo vệ API trước truy cập trái phép, sửa ID để xem dữ liệu người khác và gửi yêu cầu lặp.
- Cấu hình CORS, HTTPS và giới hạn tần suất cho các endpoint phù hợp.
- Không đưa thông tin xác thực hoặc dữ liệu cá nhân nhạy cảm vào log.

# QUY TẮC NGHIỆP VỤ XUYÊN SUỐT

1. Một phòng không được có số người thuê vượt sức chứa.
2. Một người thuê chỉ được truy cập dữ liệu thuộc phạm vi được cấp quyền.
3. Hợp đồng chỉ có hiệu lực khi đáp ứng điều kiện nghiệp vụ và được kích hoạt hợp lệ.
4. Hóa đơn phải lưu chi tiết khoản phí và đơn giá tại thời điểm phát hành.
5. Công nợ được tính từ hóa đơn, khoản thanh toán đã xác nhận và điều chỉnh hợp lệ.
6. Không đánh dấu thanh toán thành công dựa trên dữ liệu do frontend tự gửi.
7. Giao dịch thanh toán phải chống ghi nhận trùng.
8. Các thao tác quan trọng phải có lịch sử để truy vết.
9. Tác vụ định kỳ phải chạy ở backend và an toàn khi chạy lại.
10. Không xóa cứng dữ liệu hợp đồng, hóa đơn hoặc giao dịch tài chính đã phát sinh.
11. Không cho phép chỉnh sửa dữ liệu thuộc người dùng khác bằng cách thay đổi ID trong request.
12. Các quy tắc tính tiền, chia phí ở ghép và xử lý tiền cọc phải được cấu hình hoặc xác định rõ trước khi triển khai.

# YÊU CẦU CHUNG VỀ GIAO DIỆN

- Giao diện tiếng Việt, dễ sử dụng trên desktop và tương thích màn hình điện thoại.
- Có sidebar điều hướng theo vai trò.
- Các trang danh sách cần có tìm kiếm, lọc, phân trang, trạng thái rỗng và trạng thái tải dữ liệu.
- Form phải có kiểm tra dữ liệu, hiển thị lỗi rõ ràng và không mất dữ liệu đã nhập khi gặp lỗi không nghiêm trọng.
- Các thao tác nhạy cảm như hủy hợp đồng, hủy hóa đơn hoặc điều chỉnh thanh toán cần hộp thoại xác nhận.
- Trạng thái phải hiển thị nhất quán bằng nhãn chữ, không chỉ dùng màu sắc.
- Không tạo dữ liệu giả để thay thế API thật khi backend đã được triển khai.
- Có thể dùng dữ liệu seed trong môi trường phát triển để tạo 30 phòng mẫu và các tài khoản thử nghiệm.
