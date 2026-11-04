# Báo cáo dữ liệu mẫu — QuanLyPhongKhamThuCuc

## Phạm vi

`SeedData.sql` bám theo 49 bảng và các khóa ngoại trong `schema.sql`; không sửa schema, không xóa dữ liệu cũ, không bật `IDENTITY_INSERT`. Dữ liệu được thêm trong một transaction có `TRY/CATCH`, `XACT_ABORT` và rollback khi lỗi. Các khóa nghiệp vụ cố định cùng điều kiện `NOT EXISTS` giúp chạy lại mà không nhân đôi dữ liệu seed.

Ngày lịch hẹn, nhập/xuất kho, bảo trì và thanh toán được tính tương đối theo ngày chạy script. Hồ sơ bệnh nhân, nhân viên, số giấy tờ, số thẻ, chứng chỉ và kết quả khám đều là dữ liệu giả lập; không dùng để khám chữa bệnh hoặc làm giấy tờ thực tế.

## Nội dung và số lượng seed dự kiến trên database trống

| Nhóm | Bảng | Dữ liệu mẫu |
|---|---|---:|
| Nhân sự/tài khoản | `BoPhan`, `ChucDanh`, `ChuyenKhoa`, `NhanVien`, `BacSi` | 5 bộ phận, 5 chức danh, 6 chuyên khoa, 11 nhân viên, 6 bác sĩ |
| Phân quyền | `VaiTro`, `Quyen`, `TaiKhoan`, `TaiKhoanVaiTro`, `VaiTroQuyen`, `CauHinhHeThong` | 6 vai trò, 19 quyền, 6 tài khoản, 6 gán vai trò, 41 gán quyền, 5 cấu hình |
| Bệnh nhân/lịch khám | `BenhNhan`, `CaLamViec`, `LichPhanCong`, `KhungGioKham`, `LichHen`, `LichSuLichHen` | 20 bệnh nhân, 2 ca, phân công theo lịch, 20 khung giờ, 20 lịch hẹn, lịch sử tương ứng |
| Khám bệnh | `LuotKham`, `PhieuKham`, `DichVu`, `ChiDinhDichVu` | 14 lượt khám/phiếu khám, 5 dịch vụ, 28 chỉ định |
| Thuốc/kho | `Thuoc`, `DonThuoc`, `ChiTietDonThuoc`, `LoThuoc`, `PhieuNhap`, `ChiTietPhieuNhap`, `PhieuXuat`, `ChiTietPhieuXuat` | 6 thuốc, 12 đơn, 24 dòng đơn, 6 lô, 1 phiếu nhập, 6 dòng nhập thuốc, 12 phiếu cấp phát, 24 dòng xuất |
| Viện phí | `BangGiaDichVu`, `ChiTietBangGia`, `PhieuTinhPhi`, `ChiTietPhieuTinhPhi`, `HoaDon`, `PhuongThucThanhToan`, `GiaoDichThanhToan` | 1 bảng giá, 5 giá dịch vụ, 12 phiếu tính phí, dòng phí dịch vụ/thuốc, 12 hóa đơn, 3 phương thức, 10 giao dịch thành công |
| BHYT | `TheBHYT`, `LichSuXacThucBHYT` | 8 thẻ hợp lệ giả lập, lịch sử xác thực cho lượt khám phù hợp |
| Thiết bị/nhân viên | `LoaiThietBi`, `ThietBi`, `LichBaoDuong`, `SuaChuaThietBi`, `DanhGiaNhanVien` | 3 loại, 3 thiết bị, 3 lịch bảo trì, 1 phiếu sửa chữa, 5 đánh giá |
| Nhật ký/thông báo | `NhatKyHeThong`, `ThongBao` | 5 nhật ký, 5 thông báo |
| Hóa chất | `HoaChat`, `LoHoaChat`, `ChiTietNhapHoaChat`, `ChiTietXuatHoaChat` | 3 hóa chất, 3 lô, 3 dòng nhập, 3 dòng xuất |

`PhieuNhap` và `PhieuXuat` được dùng chung cho thuốc và hóa chất đúng theo quan hệ schema hiện có. Phiếu nhập chung có chi tiết ở cả `ChiTietPhieuNhap` và `ChiTietNhapHoaChat`; phiếu xuất hóa chất tham chiếu `PhieuXuat` qua `ChiTietXuatHoaChat`, không tạo bảng/chứng từ thay thế.

Script trả về số đếm thực tế cho mọi bảng trong database, danh sách bảng trống (nếu có), các tài khoản/vai trò, lịch hẹn, lượt khám/đơn/hóa đơn, đối chiếu phiếu nhập và giao dịch, cùng kết quả `DBCC CHECKCONSTRAINTS`. Database có dữ liệu từ trước thì số đếm là tổng hiện tại, không chỉ riêng phần seed.

## Kết quả kiểm thử thực thi

Đã chạy `schema.sql` và `SeedData.sql` trên một database SQL Server kiểm thử cô lập, sau đó chạy nguyên văn seed hai lần liên tiếp trên cùng kết nối. Database kiểm thử được dọn sau khi hoàn tất; không nạp dữ liệu vào database thật của dự án.

- Database có đủ 49 bảng; cả 49 bảng đều có dữ liệu sau seed.
- Có 20 bệnh nhân, 20 lịch hẹn, 14 lượt khám, 12 đơn thuốc, 12 hóa đơn và 10 giao dịch thanh toán.
- Có 8 thẻ BHYT và 3 chi tiết nhập hóa chất; các bảng hóa chất dùng chung phiếu nhập/xuất như schema quy định.
- `DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS` không báo lỗi khóa ngoại/ràng buộc.
- Sai lệch header/chi tiết phiếu tính phí, tiền hóa đơn, tiền thanh toán và tổng phiếu nhập: **0**.
- Sai lệch tồn lô thuốc/hóa chất sau nhập/xuất: **0**.
- Lặp script cùng phiên không sinh trùng dữ liệu và không lỗi do bảng tạm báo cáo.

## Tài khoản mẫu và mật khẩu

| Tên đăng nhập | Nhân viên | Vai trò | Mật khẩu |
|---|---|---|---|
| `admin` | Nguyễn Hoàng Minh | Quản trị viên | Chưa có mật khẩu database hợp lệ |
| `letan.huong` | Trần Thu Hương | Lễ tân | Chưa có mật khẩu database hợp lệ |
| `bacsi.thanh` | Phạm Đức Thành | Bác sĩ | Chưa có mật khẩu database hợp lệ |
| `kho.phuc` | Bùi Văn Phúc | Nhân viên kho | Chưa có mật khẩu database hợp lệ |
| `thungan.van` | Nguyễn Thị Vân | Thu ngân | Chưa có mật khẩu database hợp lệ |
| `quanly.nam` | Đỗ Thành Nam | Quản lý phòng khám | Chưa có mật khẩu database hợp lệ |

### Cảnh báo xác thực — cần xử lý trước khi dùng thật

Đã kiểm tra Java source: `view.auth.LoginFrame.signIn()` chỉ yêu cầu tên đăng nhập và mật khẩu không rỗng rồi mở giao diện demo; không đọc database, không xác minh tài khoản và không có thuật toán hash nào được triển khai. Vì vậy script **không tự tạo hash theo một thuật toán suy đoán**. Cột `TaiKhoan.MatKhauHash` nhận chuỗi đánh dấu `NOT-A-REAL-HASH-AUTHENTICATION-NOT-IMPLEMENTED`, không phải hash và không dùng được để xác thực.

Để thử riêng giao diện hiện tại, có thể nhập tên đăng nhập ở bảng trên, nhập `Demo123!` (hoặc chuỗi bất kỳ không rỗng) và chọn vai trò ở danh sách demo. Đây **không phải thông tin đăng nhập database**; ứng dụng hiện không kiểm tra hai giá trị này với `TaiKhoan`. Trước khi nối đăng nhập thật, cần thống nhất và triển khai password verifier trong Java, sau đó thay các giá trị đánh dấu bằng hash tạo từ đúng verifier đó.

## Cách chạy bằng SQL Server Management Studio

1. Sao lưu database đích nếu đã có dữ liệu; xác nhận instance SQL Server đang chạy.
2. Mở `schema.sql`, kết nối đúng SQL Server, chạy script một lần để tạo `QuanLyPhongKhamThuCuc` và các bảng.
3. Mở `SeedData.sql` trong cùng instance. Kiểm tra đầu script chọn đúng database `QuanLyPhongKhamThuCuc`.
4. Chạy toàn bộ file bằng **Execute**. Không bỏ các dòng `GO`.
5. Đọc các result set cuối: số dòng mỗi bảng, tài khoản/vai trò, lịch hẹn, dữ liệu khám/đơn/hóa đơn, đối chiếu tiền và `DBCC CHECKCONSTRAINTS`.
6. Chạy lại `SeedData.sql` để kiểm tra tính lặp an toàn; các bản ghi seed không được tăng thêm.

Script chỉ được chạy sau `schema.sql`. Nếu lỗi xảy ra trong phần nạp dữ liệu, transaction được rollback và lỗi SQL Server được ném lại; cần xử lý lỗi rồi chạy lại.

## Bất thường và giới hạn phát hiện

1. **Database và JDBC không khớp:** `schema.sql` tạo SQL Server `QuanLyPhongKhamThuCuc`, nhưng `config.DatabaseConnection` từ chối mọi URL không bắt đầu bằng `jdbc:mysql:`; `pom.xml` hiện có cả driver SQL Server và MySQL. Seed script là T-SQL cho SQL Server, nhưng ứng dụng hiện chưa thể kết nối schema này bằng cấu hình JDBC đang có.
2. **Đăng nhập chưa nối database:** màn hình đăng nhập chỉ là mock; lựa chọn vai trò do người dùng chọn trên UI, không lấy từ `TaiKhoanVaiTro`. Seed quyền không tạo ra kiểm soát truy cập thực tế trong ứng dụng.
3. **Không có quy tắc hash mật khẩu trong Java:** giữ marker không hợp lệ thay vì làm sai yêu cầu bằng cách tự chọn BCrypt/SHA hoặc thuật toán khác.
4. **Tỷ lệ BHYT trong dữ liệu mẫu:** thẻ ghi `MucHuong = 80`; phiếu tính phí lấy tỷ lệ này làm phần BHYT minh họa, bệnh nhân thanh toán phần còn lại. Đây là quy ước demo, không thay thế việc tính quyền lợi theo thẻ/quy định thực tế.
5. **Ràng buộc số học chưa có trong schema:** schema khai báo các cột tiền nhưng không có `CHECK` đảm bảo tổng chi tiết bằng header hoặc số tiền giao dịch bằng số phải trả. Seed tạo các giá trị khớp và in đối chiếu; quy tắc này hiện được đảm bảo bởi dữ liệu seed chứ không phải constraint database.
6. **Kết quả khám, thẻ BHYT, nhật ký và danh tính là giả lập:** thông tin xác thực BHYT không truy cập cổng thật; các chứng chỉ, số thẻ và định danh được đánh dấu `DEMO`.
