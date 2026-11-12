# 🏥 Hệ thống Quản lý Phòng khám Thu Cúc

> **Database Documentation & Installation Guide**  
> Tài liệu đặc tả thiết kế, hướng dẫn cài đặt và tích hợp cơ sở dữ liệu cho ứng dụng quản lý phòng khám Thu Cúc.

---

## 📑 Mục lục

- [1. Giới thiệu tổng quan](#1-giới-thiệu-tổng-quan)
- [2. Thông số kỹ thuật và yêu cầu](#2-thông-số-kỹ-thuật-và-yêu-cầu)
- [3. Cấu trúc phân hệ cơ sở dữ liệu](#3-cấu-trúc-phân-hệ-cơ-sở-dữ-liệu)
- [4. Hướng dẫn cài đặt cơ sở dữ liệu](#4-hướng-dẫn-cài-đặt-cơ-sở-dữ-liệu)
- [5. Đặc tả thiết kế quan trọng](#5-đặc-tả-thiết-kế-quan-trọng)
- [6. Tích hợp JDBC trong ứng dụng Java](#6-tích-hợp-jdbc-trong-ứng-dụng-java)
- [7. Quy tắc nghiệp vụ tại tầng Application Logic](#7-quy-tắc-nghiệp-vụ-tại-tầng-application-logic)
- [8. Thiết kế giao diện](#8-thiết-kế-giao-diện)

---

## 1. Giới thiệu tổng quan

Cơ sở dữ liệu **`QuanLyPhongKhamThuCuc`** được xây dựng nhằm phục vụ cho ứng dụng quản lý phòng khám Thu Cúc, phát triển bằng Java (JDBC Core / Java Swing).

Cơ sở dữ liệu được thiết kế dựa trên sơ đồ phân cấp chức năng (FHD) và các quy trình nghiệp vụ được mô tả trong tài liệu phân tích, thiết kế hệ thống.

Các phân hệ nghiệp vụ chính bao gồm:

- **Quản lý nhân sự:** Quản lý nhân viên, bác sĩ, chức danh, bộ phận và chuyên khoa.
- **Quản lý lịch khám:** Phân công ca làm việc, quản lý khung giờ và lịch hẹn khám.
- **Quản lý khám bệnh:** Tiếp nhận bệnh nhân, quản lý lượt khám, lập phiếu khám và chẩn đoán.
- **Quản lý thuốc:** Kê đơn thuốc, quản lý danh mục thuốc và chi tiết đơn thuốc.
- **Quản lý kho:** Theo dõi nhập, xuất, tồn kho thuốc và hóa chất xét nghiệm theo từng lô.
- **Quản lý viện phí:** Tính phí dịch vụ, lập hóa đơn và quản lý giao dịch thanh toán.
- **Quản lý bảo hiểm y tế:** Lưu trữ thông tin thẻ BHYT và lịch sử xác thực.
- **Quản lý thiết bị:** Theo dõi thiết bị y tế, lịch bảo dưỡng và sửa chữa.
- **Quản trị hệ thống:** Quản lý tài khoản, phân quyền RBAC, cấu hình và nhật ký hoạt động.

---

## 2. Thông số kỹ thuật và yêu cầu

| Thành phần | Đặc tả |
|---|---|
| Hệ quản trị cơ sở dữ liệu | Microsoft SQL Server |
| Cơ sở dữ liệu | `QuanLyPhongKhamThuCuc` |
| Công nghệ kết nối | Java Database Connectivity (JDBC) |
| JDBC Driver | `com.microsoft.sqlserver.jdbc.SQLServerDriver` |
| Công cụ quản lý | SQL Server Management Studio (SSMS) |

### Yêu cầu môi trường

- Microsoft SQL Server.
- Java Development Kit (JDK).
- Maven để tải Microsoft SQL Server JDBC Driver.
- SQL Server Management Studio để quản lý cơ sở dữ liệu.

---

## 3. Cấu trúc phân hệ cơ sở dữ liệu

Cơ sở dữ liệu được tổ chức thành **10 nhóm nghiệp vụ**, bao gồm hơn 40 bảng, phục vụ các chức năng chính của hệ thống.

```text
QuanLyPhongKhamThuCuc
│
├── 1. Nhân sự & Chuyên môn
│   ├── BoPhan
│   ├── ChucDanh
│   ├── ChuyenKhoa
│   ├── NhanVien
│   └── BacSi
│
├── 2. Lịch làm việc & Hẹn khám
│   ├── CaLamViec
│   ├── LichPhanCong
│   ├── KhungGioKham
│   ├── LichHen
│   └── LichSuLichHen
│
├── 3. Khám bệnh & Dịch vụ
│   ├── BenhNhan
│   ├── LuotKham
│   ├── PhieuKham
│   ├── DichVu
│   └── ChiDinhDichVu
│
├── 4. Thuốc & Đơn thuốc
│   ├── Thuoc
│   ├── DonThuoc
│   └── ChiTietDonThuoc
│
├── 5. Kho thuốc & Hóa chất
│   ├── LoThuoc
│   ├── PhieuNhap
│   ├── ChiTietPhieuNhap
│   ├── PhieuXuat
│   ├── ChiTietPhieuXuat
│   ├── HoaChat
│   ├── LoHoaChat
│   ├── ChiTietNhapHoaChat
│   └── ChiTietXuatHoaChat
│
├── 6. Viện phí & Thanh toán
│   ├── BangGiaDichVu
│   ├── ChiTietBangGia
│   ├── PhieuTinhPhi
│   ├── ChiTietPhieuTinhPhi
│   ├── HoaDon
│   ├── PhuongThucThanhToan
│   └── GiaoDichThanhToan
│
├── 7. Bảo hiểm y tế
│   ├── TheBHYT
│   └── LichSuXacThucBHYT
│
├── 8. Thiết bị y tế
│   ├── LoaiThietBi
│   ├── ThietBi
│   ├── LichBaoDuong
│   └── SuaChuaThietBi
│
├── 9. Đánh giá nhân viên
│   └── DanhGiaNhanVien
│
└── 10. Quản trị hệ thống
    ├── TaiKhoan
    ├── VaiTro
    ├── Quyen
    ├── TaiKhoanVaiTro
    ├── VaiTroQuyen
    ├── CauHinhHeThong
    ├── NhatKyHeThong
    └── ThongBao
```

---

## 4. Hướng dẫn cài đặt cơ sở dữ liệu

### 4.1. Cài đặt bằng SQL Server Management Studio

**Bước 1:** Khởi động SQL Server và kết nối đúng instance bằng SSMS.

**Bước 2:** Mở `database/schema.sql` và thực thi để tạo database cùng các bảng.

**Bước 3:** Mở `database/SeedData.sql`, chọn Execute để thêm dữ liệu mẫu và xem báo cáo ở cuối script.

Kiểm tra database và số bảng trong SSMS:

```sql
SELECT name FROM sys.databases WHERE name = N'QuanLyPhongKhamThuCuc';
```

Kiểm tra các bảng:

```sql
USE QuanLyPhongKhamThuCuc;
SELECT name FROM sys.tables ORDER BY name;
```

### 4.2. Cài đặt bằng sqlcmd

Có thể chạy schema và seed từ terminal Windows với Windows Authentication:

```powershell
sqlcmd -S localhost -E -C -i database\schema.sql
sqlcmd -S localhost -E -C -i database\SeedData.sql
```

### 4.3. Kiểm tra sau khi cài đặt

Sử dụng câu lệnh sau để kiểm tra số bảng:

```sql
USE QuanLyPhongKhamThuCuc;

SELECT COUNT(*) AS TongSoBang
FROM sys.tables;
```

---

## 5. Đặc tả thiết kế quan trọng

### 5.1. Mô hình kế thừa Bác sĩ và Nhân viên

Bảng `BacSi` được thiết kế theo mô hình **kế thừa dạng bảng con (Subtype)** từ bảng `NhanVien`.

- `NhanVien` lưu trữ thông tin chung của toàn bộ nhân viên.
- `BacSi` lưu trữ các thông tin chuyên môn riêng của bác sĩ.
- `BacSi.MaNhanVien` đồng thời là khóa chính (Primary Key) và khóa ngoại (Foreign Key) tham chiếu đến `NhanVien.MaNhanVien`.

Các bảng nghiệp vụ có liên quan đến bác sĩ sử dụng thống nhất cột `MaNhanVien` để tham chiếu đến bác sĩ tương ứng.

Các bảng liên quan:

- `KhungGioKham`
- `LichHen`
- `LuotKham`
- `PhieuKham`
- `DonThuoc`

### 5.2. Chuỗi quy trình tiếp nhận và khám bệnh

Quy trình nghiệp vụ khám bệnh được tổ chức theo trình tự:

```text
BenhNhan
    │
    ▼
LichHen (Không bắt buộc)
    │
    ▼
LuotKham
    │
    ▼
PhieuKham
    │
    ├───────────────┐
    ▼               ▼
ChiDinhDichVu    DonThuoc
```

**Các ràng buộc quan trọng:**

- `LuotKham.MaLichHen` cho phép giá trị `NULL`, hỗ trợ trường hợp bệnh nhân đến khám trực tiếp mà không đặt lịch trước.
- Mỗi lượt khám chỉ có một phiếu khám tương ứng.
- Ràng buộc một - một giữa `LuotKham` và `PhieuKham` được đảm bảo bằng `UNIQUE(MaLuotKham)` trong bảng `PhieuKham`.

### 5.3. Phân biệt kê đơn thuốc và quản lý xuất kho

Hệ thống tách biệt hai nghiệp vụ:

**Nghiệp vụ kê đơn thuốc:**

- Bác sĩ tạo đơn thuốc sau khi khám bệnh.
- Thông tin đơn thuốc được lưu tại `DonThuoc`.
- Các loại thuốc và số lượng được kê được lưu tại `ChiTietDonThuoc`.

**Nghiệp vụ xuất kho và cấp phát thuốc:**

- Thủ kho / Dược sĩ thực hiện xác nhận xuất thuốc.
- Phiếu xuất kho được lưu tại `PhieuXuat`.
- Chi tiết thuốc xuất theo từng lô được lưu tại `ChiTietPhieuXuat`.
- Số lượng tồn kho được cập nhật tương ứng sau khi xuất thuốc.

Việc tách biệt hai nghiệp vụ giúp phân định rõ trách nhiệm giữa bác sĩ và bộ phận quản lý kho.

---

## 6. Tích hợp JDBC trong ứng dụng Java

Ứng dụng Java sử dụng **JDBC (Java Database Connectivity)** cùng Microsoft SQL Server JDBC Driver. Cấu hình kết nối được đọc từ `db.properties` (không đưa thông tin xác thực vào Git).

### 6.1. Cấu hình SQL Server

Sao chép `db.properties.example` thành `db.properties`, sau đó cập nhật instance, tên database và thông tin đăng nhập SQL Server:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=QuanLyPhongKhamThuCuc;encrypt=true;trustServerCertificate=true
db.user=sa
db.password=YOUR_SQL_SERVER_PASSWORD
```

Maven đã khai báo `com.microsoft.sqlserver:mssql-jdbc`. Với SQL Server Express, thay `localhost:1433` bằng instance/port thực tế. `trustServerCertificate=true` phù hợp môi trường phát triển; triển khai thật nên cấu hình chứng chỉ tin cậy.

### 6.2. Lớp kết nối

`config.DatabaseConnection.getConnection()` nạp cấu hình và chỉ chấp nhận JDBC URL bắt đầu bằng `jdbc:sqlserver:`. Connection được đóng bằng try-with-resources tại DAO/service sử dụng.

Không commit `db.properties` hoặc thông tin mật khẩu thật.

### 6.3. Truy vấn dữ liệu bằng JDBC

**Ví dụ A: Truy vấn thông tin phiếu khám và bác sĩ đảm nhận**

```sql
SELECT
    pk.MaPhieuKham,
    pk.ChanDoan,
    pk.HuongDieuTri,
    nv.MaNhanVien,
    nv.HoTen AS TenBacSi,
    bs.HocVi,
    bs.ChuyenMon
FROM PhieuKham pk
JOIN BacSi bs
    ON pk.MaNhanVien = bs.MaNhanVien
JOIN NhanVien nv
    ON bs.MaNhanVien = nv.MaNhanVien
WHERE pk.MaLuotKham = ?;
```

Câu truy vấn sử dụng `JOIN` để kết hợp dữ liệu từ ba bảng:

- `PhieuKham`: Thông tin phiếu khám.
- `BacSi`: Thông tin chuyên môn của bác sĩ.
- `NhanVien`: Thông tin nhân viên và họ tên bác sĩ.

**Ví dụ triển khai bằng PreparedStatement:**

```java
String sql = """
    SELECT pk.MaPhieuKham, pk.ChanDoan,
           nv.HoTen AS TenBacSi, bs.HocVi
    FROM PhieuKham pk
    JOIN BacSi bs ON pk.MaNhanVien = bs.MaNhanVien
    JOIN NhanVien nv ON bs.MaNhanVien = nv.MaNhanVien
    WHERE pk.MaLuotKham = ?
    """;

try (Connection conn = config.DatabaseConnection.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {

    ps.setLong(1, maLuotKham);

    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            System.out.println("Phiếu khám: " + rs.getLong("MaPhieuKham"));
            System.out.println("Chẩn đoán: " + rs.getString("ChanDoan"));
            System.out.println("Bác sĩ: " + rs.getString("TenBacSi"));
        }
    }

} catch (SQLException e) {
    e.printStackTrace();
}
```

### 6.4. Quản lý Transaction khi cấp phát thuốc

Khi thực hiện xuất thuốc, hệ thống cần đảm bảo việc tạo phiếu xuất và trừ tồn kho được thực hiện trong cùng một giao dịch (Transaction).

Nếu một bước thất bại, toàn bộ giao dịch phải được hoàn tác bằng `rollback()` để tránh dữ liệu kho bị sai lệch.

**Ví dụ xử lý cấp phát thuốc:**

```java
public boolean processDrugIssue(
        long maDonThuoc,
        String maNhanVienXuat,
        List<OrderItemDTO> items) {

    String insertPhieuXuatSQL =
        "INSERT INTO PhieuXuat "
        + "(SoPhieu, LoaiXuat, MaDonThuoc, MaNhanVienXuat, TrangThai) "
        + "VALUES (?, 'CapPhatDonThuoc', ?, ?, 'DaXacNhan')";

    String updateStockSQL =
        "UPDATE LoThuoc "
        + "SET SoLuongTon = SoLuongTon - ? "
        + "WHERE MaLoThuoc = ? AND SoLuongTon >= ?";

    Connection conn = null;

    try {
        conn = config.DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        // 1. Tạo phiếu xuất kho
        try (PreparedStatement psXuat =
                 conn.prepareStatement(insertPhieuXuatSQL)) {

            psXuat.setString(1, "PX" + System.currentTimeMillis());
            psXuat.setLong(2, maDonThuoc);
            psXuat.setString(3, maNhanVienXuat);

            psXuat.executeUpdate();
        }

        // 2. Trừ tồn kho từng lô thuốc
        try (PreparedStatement psKho =
                 conn.prepareStatement(updateStockSQL)) {

            for (OrderItemDTO item : items) {

                psKho.setBigDecimal(1, item.getQuantity());
                psKho.setLong(2, item.getBatchId());
                psKho.setBigDecimal(3, item.getQuantity());

                int updatedRows = psKho.executeUpdate();

                if (updatedRows == 0) {
                    conn.rollback();
                    return false;
                }
            }
        }

        // 3. Xác nhận giao dịch
        conn.commit();
        return true;

    } catch (SQLException e) {

        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        e.printStackTrace();
        return false;

    } finally {

        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
```

> **Lưu ý:** Đây là ví dụ minh họa quản lý Transaction. Khi triển khai thực tế, cần bổ sung thao tác ghi chi tiết phiếu xuất vào `ChiTietPhieuXuat`, kiểm tra dữ liệu đầu vào và xử lý đồng thời khi nhiều người cùng xuất một lô thuốc.

---

## 7. Quy tắc nghiệp vụ tại tầng Application Logic

Một số quy tắc nghiệp vụ cần được xử lý tại tầng Service / Application Logic của ứng dụng Java nhằm đảm bảo tính chính xác và nhất quán của dữ liệu.

### 7.1. Kiểm tra trùng lịch làm việc

Trước khi thêm lịch phân công vào `LichPhanCong`, hệ thống cần kiểm tra khoảng thời gian làm việc của nhân viên.

Các trường cần kiểm tra:

- `GioBatDau`
- `GioKetThuc`
- Nhân viên được phân công
- Ngày làm việc

Không cho phép tạo lịch làm việc bị trùng với lịch đã tồn tại của cùng nhân viên.

### 7.2. Kiểm soát số lượng khung giờ khám

Khi bệnh nhân đăng ký lịch hẹn, hệ thống cần kiểm tra số lượng lịch hẹn đã được đăng ký trong khung giờ tương ứng.

- Nếu số lượng lịch hẹn chưa đạt giới hạn `SoLuongToiDa`, cho phép đăng ký.
- Nếu đã đạt giới hạn, cập nhật trạng thái `KhungGioKham` thành `Day`.
- Không cho phép đăng ký thêm khi khung giờ đã đầy.

### 7.3. Ưu tiên xuất thuốc theo FEFO

Hệ thống áp dụng nguyên tắc **FEFO (First Expired, First Out)**, tức là ưu tiên xuất các lô thuốc có hạn sử dụng gần nhất.

Câu truy vấn lựa chọn lô thuốc:

```sql
SELECT *
FROM LoThuoc
WHERE SoLuongTon > 0
  AND TrangThai = 'DangTon'
ORDER BY HanSuDung ASC;
```

Việc áp dụng FEFO giúp hạn chế tình trạng thuốc hết hạn trong quá trình lưu trữ và sử dụng.

### 7.4. Bảo toàn dữ liệu tài chính

Đối với các dữ liệu liên quan đến hóa đơn và giao dịch thanh toán:

- Không xóa trực tiếp các giao dịch tài chính đã phát sinh.
- Mọi thao tác hoàn tiền hoặc điều chỉnh cần được ghi nhận bằng giao dịch mới.
- Lưu lại lịch sử giao dịch để phục vụ việc tra cứu, đối soát và kiểm toán.

### 7.5. Kiểm tra quyền truy cập người dùng (RBAC)

Hệ thống sử dụng mô hình **RBAC (Role-Based Access Control)** để quản lý quyền truy cập.

Các bảng tham gia phân quyền:

```text
TaiKhoan
    │
    ▼
TaiKhoanVaiTro
    │
    ▼
VaiTro
    │
    ▼
VaiTroQuyen
    │
    ▼
Quyen
```

Trước khi cho phép người dùng truy cập chức năng, ứng dụng cần kiểm tra vai trò và quyền tương ứng của tài khoản.

Việc phân quyền được xử lý tại tầng ứng dụng Java, kết hợp với dữ liệu được lưu trữ trong các bảng quản trị hệ thống.

### 7.6. Theo dõi và chuyển trạng thái lượt khám

Màn hình **Lượt khám** đọc dữ liệu từ `LuotKham` và các quan hệ bệnh nhân, bác sĩ, lịch hẹn, phiếu khám. Danh sách được tìm kiếm, lọc và phân trang; màn hình không tạo hoặc xóa lượt khám.

Các trạng thái được khai báo bởi ràng buộc của `LuotKham` là `ChoKham`, `DangKham`, `ChoKetQua`, `HoanTat` và `DaHuy`. Ứng dụng chỉ cho phép tiến trạng thái theo luồng:

```text
ChoKham → DangKham → ChoKetQua → HoanTat
```

Chỉ tài khoản có quyền `KHAM_HOSO` mới được cập nhật. Bác sĩ chỉ cập nhật lượt khám của mình; tài khoản có quyền quản lý nhân sự hiện có (`NHANSU_SUA`) có thể điều phối lượt khám. Hoàn tất yêu cầu phiếu khám thuộc đúng bác sĩ phụ trách ở trạng thái `HoanTat`, `DaKy` hoặc `DaKhoa`, đồng thời không còn chỉ định dịch vụ ở trạng thái `ChoThucHien` hoặc `DangThucHien`. Khi hoàn tất, ứng dụng ghi `ThoiGianKetThuc`; schema hiện không có cột thời gian bắt đầu khám nên ứng dụng không tự tạo hoặc ghi trường đó. Mỗi lần chuyển trạng thái và ghi nhật ký `NhatKyHeThong` được thực hiện trong cùng transaction. Trạng thái `DaHuy` chỉ được hiển thị, không có thao tác hủy vì schema chưa xác định lý do hay quy trình hủy lượt khám.

### 7.7. Kê đơn thuốc

Màn hình **Kê đơn thuốc** đọc `DonThuoc`, `ChiTietDonThuoc` và danh mục `Thuoc`; người dùng có quyền `DONTHUOC_KE` mới truy cập được. Bác sĩ chỉ kê hoặc thay đổi đơn trên lượt khám do mình phụ trách; quyền quản lý hiện có `NHANSU_SUA` cho phép quản lý đơn của bác sĩ khác. Đơn mới chỉ được lập cho lượt khám `DangKham` hoặc `ChoKetQua`, với thuốc `DangSuDung`.

Chỉ đơn ở trạng thái `Nhap`, chưa liên kết phiếu xuất và còn trong lượt khám hợp lệ mới được sửa hoặc hủy. Thao tác hủy chuyển trạng thái sang `Huy`, không xóa đơn hay chi tiết. Các chi tiết đã lưu không bị xóa khi sửa. Kê đơn chỉ tạo/cập nhật dữ liệu đơn thuốc và nhật ký; không tạo `PhieuXuat`, không chọn lô và không thay đổi tồn kho. Việc xuất/cấp phát tiếp tục thuộc quy trình quản lý kho hiện có.

---

## 8. Đăng nhập, phân quyền và giao diện

Ứng dụng sử dụng Java Swing và FlatLaf. Điểm chạy `view.MainFrame` hiển thị màn hình đăng nhập; hệ thống chỉ mở giao diện chính sau khi xác thực tài khoản với SQL Server thành công. Không có chế độ chọn vai trò hay đăng nhập demo.

### 8.1. Thiết lập và chạy

1. Tạo database và dữ liệu mẫu theo mục 4.
2. Tạo `db.properties` từ `db.properties.example` trong thư mục gốc dự án và điền cấu hình đúng với SQL Server đang chạy.
3. Mở dự án Maven bằng NetBeans, chọn JDK 17 trở lên, chờ Maven nạp dependencies rồi chọn **Run Project**. Lớp khởi chạy Maven là `view.MainFrame`; cấu hình chạy NetBeans trong `nbactions.xml` gọi `exec:java`.

Không commit `db.properties` hoặc thông tin đăng nhập. Nếu ứng dụng báo không kết nối được database, xác nhận instance/port, database name, tài khoản SQL Server và trạng thái SQL Server trước khi chạy.

### 8.2. Tài khoản dữ liệu mẫu

Sau khi chạy `database/SeedData.sql`, có thể dùng các tài khoản demo sau:

| Tên đăng nhập | Mật khẩu | Vai trò mẫu |
|---|---|---|
| `admin` | `AdminDemo#2026` | Quản trị viên |
| `letan.huong` | `LeTanDemo#2026` | Lễ tân |
| `bacsi.thanh` | `BacSiDemo#2026` | Bác sĩ |
| `kho.phuc` | `KhoDemo#2026` | Nhân viên kho |
| `thungan.van` | `ThuNganDemo#2026` | Thu ngân |
| `quanly.nam` | `QuanLyDemo#2026` | Quản lý |

Đây là thông tin dành riêng cho môi trường phát triển/kiểm thử. Đổi mật khẩu hoặc vô hiệu hóa tài khoản demo trước khi triển khai thật.

### 8.3. Xác thực và phân quyền

- Mật khẩu được xác minh bằng PBKDF2-HMAC-SHA256 của JDK, theo định dạng hash do `bus.auth.PasswordHasher` quy định; mật khẩu rõ không được lưu trong database.
- Tài khoản chỉ đăng nhập khi trạng thái là `HoatDong` và nhân viên được gán còn trạng thái công tác hợp lệ.
- Vai trò và quyền được nạp từ `TaiKhoanVaiTro`, `VaiTro`, `VaiTroQuyen` và `Quyen`. Sidebar chỉ hiển thị trang người dùng có quyền; MainFrame kiểm tra lại quyền khi điều hướng.
- Menu tài khoản trên HeaderPanel cung cấp thông tin tài khoản, đổi mật khẩu và đăng xuất. Đổi mật khẩu yêu cầu mật khẩu hiện tại, mật khẩu mới tối thiểu 8 ký tự và xác nhận khớp; DAO cập nhật hash và ghi nhật ký trong cùng transaction.
- Đăng xuất cần xác nhận, ghi nhật ký, thu hồi session và xóa quyền đang giữ trước khi quay về LoginFrame. Các BUS nghiệp vụ cần gọi `AuthorizationBUS.requirePermission(...)` trước thao tác được bảo vệ.
- Dashboard, quản lý bệnh nhân và quản lý lịch hẹn hiện truy vấn dữ liệu SQL Server. Các module nghiệp vụ còn lại vẫn có thể là khung giao diện; cần kiểm tra riêng trước khi sử dụng.

### 8.4. Phân tầng chức năng đăng nhập

```text
view.auth.LoginFrame
    → bus.auth.AuthenticationBUS / AuthorizationBUS
        → dao.auth.AuthenticationDAO
            → config.DatabaseConnection → SQL Server
        ← model.auth.TaiKhoan, NhanVien, VaiTro, Quyen
    ← model.auth.AuthenticatedSession
```

`AuthenticationDAO` tự quản lý JDBC resource và transaction cho đăng nhập, đổi mật khẩu cùng nhật ký. `AuthenticationBUS` kiểm tra trạng thái/mật khẩu, đổi mật khẩu và đăng xuất; session không chứa mật khẩu hay hash, đồng thời bị thu hồi khi đăng xuất. `PasswordHasher` và `AuthenticationException` được đặt cùng tầng BUS để không còn package `auth` tách rời.

### 8.5. Dashboard dữ liệu thực

Dashboard tải dữ liệu bất đồng bộ theo luồng `view.DashboardPanel` → `bus.dashboard.DashboardBUS` → `dao.dashboard.DashboardDAO` → SQL Server. Các KPI và biểu đồ lấy từ `LichHen`, `LuotKham`, `BenhNhan`, `HoaDon` và `GiaoDichThanhToan`; lịch hẹn sắp tới nối `BenhNhan`, `NhanVien`, `BacSi` và `ChuyenKhoa`. Doanh thu chỉ cộng giao dịch `ThanhCong`, không cộng lại tổng hóa đơn. Số cần thu là số dư trên hóa đơn `ChuaThanhToan` hoặc `ThanhToanMotPhan`, trừ giao dịch thành công.

Dashboard không có mã quyền riêng trong schema hiện tại; quyền `BAOCAO_XEM` chỉ mô tả xem báo cáo. Vì vậy, Dashboard dùng quy tắc hiện có: yêu cầu phiên đăng nhập còn hiệu lực và không tự gán thêm quyền hoặc thay đổi database.

Có thể chạy các truy vấn chỉ đọc sau trong SSMS để đối chiếu tổng quan:

```sql
USE QuanLyPhongKhamThuCuc;

DECLARE @Today DATE = CONVERT(DATE, SYSDATETIME());
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);
DECLARE @MonthStart DATE = DATEFROMPARTS(YEAR(@Today), MONTH(@Today), 1);
DECLARE @NextMonth DATE = DATEADD(MONTH, 1, @MonthStart);

SELECT
    (SELECT COUNT_BIG(*) FROM dbo.LichHen
     WHERE ThoiGianHen >= @Today AND ThoiGianHen < @Tomorrow) AS LichHenHomNay,
    (SELECT COUNT_BIG(*) FROM dbo.LichHen
     WHERE ThoiGianHen >= @Today AND ThoiGianHen < @Tomorrow
       AND TrangThai = N'DaXacNhan') AS LichHenDaXacNhan,
    (SELECT COUNT_BIG(*) FROM dbo.LichHen
     WHERE ThoiGianHen >= @Today AND ThoiGianHen < @Tomorrow
       AND TrangThai = N'DaHuy') AS LichHenDaHuy,
    (SELECT COUNT_BIG(*) FROM dbo.LuotKham
     WHERE ThoiGianTiepNhan >= @Today AND ThoiGianTiepNhan < @Tomorrow)
        AS LuotKhamHomNay,
    (SELECT COUNT_BIG(*) FROM dbo.LuotKham
     WHERE ThoiGianTiepNhan >= @Today AND ThoiGianTiepNhan < @Tomorrow
       AND TrangThai = N'HoanTat') AS LuotKhamHoanTat,
    (SELECT COUNT_BIG(*) FROM dbo.LuotKham
     WHERE ThoiGianTiepNhan >= @Today AND ThoiGianTiepNhan < @Tomorrow
       AND TrangThai IN (N'ChoKham', N'DangKham', N'ChoKetQua'))
        AS LuotKhamDangXuLy,
    (SELECT COUNT_BIG(*) FROM dbo.BenhNhan) AS TongBenhNhan,
    (SELECT COUNT_BIG(*) FROM dbo.BenhNhan
     WHERE NgayTaoHoSo >= @MonthStart AND NgayTaoHoSo < @NextMonth)
        AS BenhNhanMoiTrongThang,
    (SELECT COALESCE(SUM(g.SoTien), 0)
     FROM dbo.GiaoDichThanhToan g
     JOIN dbo.HoaDon h ON h.MaHoaDon = g.MaHoaDon
     WHERE g.ThoiGianGiaoDich >= @Today AND g.ThoiGianGiaoDich < @Tomorrow
       AND g.TrangThai = N'ThanhCong'
       AND h.TrangThai NOT IN (N'DaHuy', N'DaHoanTien'))
        AS DoanhThuThucThuHomNay,
    (SELECT COALESCE(SUM(
         CASE WHEN h.SoTienPhaiTra > COALESCE(p.PaidAmount, 0)
              THEN h.SoTienPhaiTra - COALESCE(p.PaidAmount, 0)
              ELSE 0 END), 0)
     FROM dbo.HoaDon h
     OUTER APPLY (
         SELECT SUM(g.SoTien) AS PaidAmount
         FROM dbo.GiaoDichThanhToan g
         WHERE g.MaHoaDon = h.MaHoaDon AND g.TrangThai = N'ThanhCong'
     ) p
     WHERE h.TrangThai IN (N'ChuaThanhToan', N'ThanhToanMotPhan'))
        AS SoTienConPhaiThu;
```

### 8.6. Quản lý lịch hẹn

Màn hình lịch hẹn tuân theo luồng `view.AppointmentPanel/AppointmentDialog` →
`bus.appointment.LichHenBUS` → `dao.appointment.LichHenDAO` → SQL Server.
Quyền hiện có được dùng thống nhất là `LH_QUANLY`; BUS kiểm tra quyền cho cả
đọc và thay đổi dữ liệu. Bác sĩ lấy từ `BacSi` nối `NhanVien`; chuyên khoa
lấy theo `BacSi.MaChuyenKhoa`. Khung giờ chỉ được chọn khi đang mở, bác sĩ
được phân công ca đã xác nhận trong ngày, giờ nằm trong ca làm việc, nhân viên
đang làm việc và sức chứa còn chỗ. `SoLuongToiDa` trong schema có mặc định 1
nhưng cho phép `NULL`; ứng dụng dùng giá trị mặc định đó nếu dữ liệu hiện có là
`NULL`.

Đặt lịch, chỉnh sửa, xác nhận và hủy được ghi cùng lịch sử vào
`LichSuLichHen` trong transaction. Khi đặt/chỉnh sửa, DAO kiểm tra lại bệnh
nhân, phân công, trạng thái khung giờ, sức chứa và lịch trùng trong transaction
`SERIALIZABLE`; lịch đã phát sinh `LuotKham` không được chỉnh sửa hoặc hủy.
Trạng thái lịch hẹn lấy từ dữ liệu thực tế; các trạng thái được định nghĩa trong
schema gồm `ChoXacNhan`, `DaXacNhan`, `DaDen`, `DangKham`, `HoanTat`,
`DaHuy` và `VangMat`.

Có thể chạy các truy vấn chỉ đọc sau trong SSMS để đối chiếu danh sách và
khung giờ theo ngày/bác sĩ:

```sql
USE QuanLyPhongKhamThuCuc;

DECLARE @NgayKham DATE = CONVERT(DATE, SYSDATETIME());
DECLARE @MaNhanVien VARCHAR(20) = 'NV003';

SELECT lh.MaLichHen, lh.ThoiGianHen, lh.TrangThai,
        bn.MaBenhNhan, bn.HoTen AS TenBenhNhan, bn.SoDienThoai,
        nv.MaNhanVien, nv.HoTen AS TenBacSi,
        ck.TenChuyenKhoa, lh.MaKhungGio, lh.LyDoKham
FROM dbo.LichHen lh
JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
LEFT JOIN dbo.ChuyenKhoa ck
     ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
WHERE lh.ThoiGianHen >= @NgayKham
  AND lh.ThoiGianHen < DATEADD(DAY, 1, @NgayKham)
ORDER BY lh.ThoiGianHen, lh.MaLichHen;

SELECT kg.MaKhungGio, kg.NgayKham, kg.MaNhanVien,
        nv.HoTen AS TenBacSi, ck.TenChuyenKhoa,
        kg.GioBatDau, kg.GioKetThuc,
        COALESCE(kg.SoLuongToiDa, 1) AS SoLuongToiDa, kg.TrangThai,
        COUNT(lh.MaLichHen) AS SoLichDangChiemCho
FROM dbo.KhungGioKham kg
JOIN dbo.BacSi bs ON bs.MaNhanVien = kg.MaNhanVien
JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
LEFT JOIN dbo.LichHen lh
     ON lh.MaKhungGio = kg.MaKhungGio
   AND lh.TrangThai IN (N'ChoXacNhan', N'DaXacNhan', N'DaDen', N'DangKham')
WHERE kg.NgayKham = @NgayKham
  AND kg.MaNhanVien = @MaNhanVien
  AND kg.TrangThai = N'Mo'
  AND nv.TrangThaiCongTac IN (N'ThuViec', N'DangLam')
  AND ck.TrangThai = N'HoatDong'
  AND EXISTS (
      SELECT 1
      FROM dbo.LichPhanCong lp
      JOIN dbo.CaLamViec ca ON ca.MaCa = lp.MaCa
      WHERE lp.MaNhanVien = kg.MaNhanVien
        AND lp.NgayLam = kg.NgayKham
        AND lp.TrangThai = N'DaXacNhan'
        AND ca.TrangThai = N'HoatDong'
        AND kg.GioBatDau >= ca.GioBatDau
        AND kg.GioKetThuc <= ca.GioKetThuc)
GROUP BY kg.MaKhungGio, kg.NgayKham, kg.MaNhanVien, nv.HoTen,
          ck.TenChuyenKhoa, kg.GioBatDau, kg.GioKetThuc,
          kg.SoLuongToiDa, kg.TrangThai
HAVING COUNT(lh.MaLichHen) < COALESCE(kg.SoLuongToiDa, 1)
ORDER BY kg.GioBatDau, kg.MaKhungGio;
```

`@MaNhanVien` trong ví dụ cần thay bằng mã bác sĩ thực tế. Danh sách khung giờ
trên phản ánh lịch phân công và sức chứa; khi đặt lịch, ứng dụng còn kiểm tra
xung đột thời gian của bệnh nhân và bác sĩ trong transaction.

---

## 📌 Ghi chú

- Tên bảng và tên cột trong tài liệu được trình bày theo thiết kế cơ sở dữ liệu của dự án.
- Schema `database/schema.sql` và dữ liệu mẫu `database/SeedData.sql` dành cho Microsoft SQL Server.
- Các ví dụ truy vấn/cấp phát ở mục 6.3–6.4 chỉ minh họa; cần đối chiếu cấu trúc bảng và kiểu dữ liệu thực tế trước khi dùng cho nghiệp vụ.
- Khi triển khai thực tế, cần đối chiếu các câu lệnh với cấu trúc bảng và kiểu dữ liệu trong SQL Script.
- Không đưa mật khẩu SQL Server, thông tin tài khoản hoặc dữ liệu cá nhân thật lên GitHub.

---

**Project:** Quản lý phòng khám Thu Cúc  
**Database:** `QuanLyPhongKhamThuCuc`  
**Database Management System:** Microsoft SQL Server
**Backend Connectivity:** Java JDBC

### 8.8. Quản lý chỉ định dịch vụ

Màn hình **Chỉ định dịch vụ** dùng quyền sẵn có `KHAM_CHIDINH`. Bác sĩ chỉ xem và
thao tác trên lượt khám do mình phụ trách; tài khoản có quyền hiện có `NHANSU_SUA`
có thể xem/điều phối trên phạm vi các bác sĩ. Mục **Lượt khám** có nút xem toàn bộ
chỉ định của lượt đang chọn.

Theo schema, `ChiDinhDichVu.MaPhieuKham` tham chiếu `PhieuKham`; phiếu khám lại tham
chiếu duy nhất một lượt khám. Chỉ định mới chỉ được lập cho lượt `DangKham` hoặc
`ChoKetQua`, đã có phiếu khám khớp bác sĩ phụ trách, và chỉ chọn dịch vụ
`DangApDung`. Ghi chú giới hạn 255 ký tự. Chỉ định được lưu ở trạng thái
`ChoThucHien`; chỉ được sửa hoặc hủy mềm (`DaHuy`) khi còn `ChoThucHien`, lượt khám
còn hoạt động và chưa được đưa vào `ChiTietPhieuTinhPhi`. Chỉ định `DangThucHien`,
`HoanTat`, `DaHuy` hoặc đã tính phí không thể sửa/hủy từ màn hình này. Các thao tác
ghi và nhật ký hệ thống cùng nằm trong transaction.

`ChiDinhDichVu` không có cột số lượng, giá tại thời điểm chỉ định, người chỉ định
hay lý do hủy riêng. `MaNhanVienThucHien` là nhân viên thực hiện dịch vụ nên ứng dụng
không dùng trường này để giả danh bác sĩ chỉ định. Danh mục hiển thị giá tham khảo
từ `ChiTietBangGia` thuộc bảng giá `DangApDung` có hiệu lực theo ngày hiện tại; giá
không được lưu vào chỉ định và có thể thay đổi trước khi tính phí. Trường hợp có
nhiều giá đang hiệu lực cho cùng một dịch vụ, truy vấn dừng và báo lỗi dữ liệu thay
vì tự chọn một giá.

Truy vấn chỉ đọc để đối chiếu trong SSMS:

```sql
USE QuanLyPhongKhamThuCuc;

SELECT MaDichVu, TenDichVu, LoaiDichVu, DonViTinh, TrangThai
FROM dbo.DichVu
ORDER BY LoaiDichVu, TenDichVu;

SELECT cd.MaChiDinh, lv.MaLuotKham, pk.MaPhieuKham,
       bn.MaBenhNhan, bn.HoTen AS TenBenhNhan,
       dv.MaDichVu, dv.TenDichVu, pk.MaNhanVien AS MaBacSiPhuTrach,
       nv.HoTen AS TenBacSi, cd.ThoiGianChiDinh, cd.TrangThai, cd.GhiChu,
       CASE WHEN EXISTS (
           SELECT 1 FROM dbo.ChiTietPhieuTinhPhi ctp
           WHERE ctp.MaChiDinh = cd.MaChiDinh
       ) THEN 1 ELSE 0 END AS DaTinhPhi
FROM dbo.ChiDinhDichVu cd
JOIN dbo.PhieuKham pk ON pk.MaPhieuKham = cd.MaPhieuKham
JOIN dbo.LuotKham lv ON lv.MaLuotKham = pk.MaLuotKham
JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
JOIN dbo.BacSi bs ON bs.MaNhanVien = pk.MaNhanVien
JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
JOIN dbo.DichVu dv ON dv.MaDichVu = cd.MaDichVu
ORDER BY cd.ThoiGianChiDinh DESC, cd.MaChiDinh DESC;

DECLARE @MaLuotKham BIGINT = 1; -- Thay bằng mã lượt khám cần kiểm tra.
SELECT cd.MaChiDinh, cd.MaPhieuKham, cd.MaDichVu, dv.TenDichVu,
       cd.ThoiGianChiDinh, cd.TrangThai, cd.MaNhanVienThucHien, cd.GhiChu
FROM dbo.ChiDinhDichVu cd
JOIN dbo.PhieuKham pk ON pk.MaPhieuKham = cd.MaPhieuKham
JOIN dbo.DichVu dv ON dv.MaDichVu = cd.MaDichVu
WHERE pk.MaLuotKham = @MaLuotKham
ORDER BY cd.ThoiGianChiDinh, cd.MaChiDinh;

SELECT dv.MaDichVu, dv.TenDichVu
FROM dbo.DichVu dv
WHERE dv.TrangThai <> N'DangApDung';

SELECT bg.MaBangGia, bg.TenBangGia, bg.TuNgay, bg.DenNgay,
       ct.MaDichVu, dv.TenDichVu, ct.DonGia
FROM dbo.BangGiaDichVu bg
JOIN dbo.ChiTietBangGia ct ON ct.MaBangGia = bg.MaBangGia
JOIN dbo.DichVu dv ON dv.MaDichVu = ct.MaDichVu
WHERE bg.TrangThai = N'DangApDung'
  AND bg.TuNgay <= CONVERT(date, SYSDATETIME())
  AND (bg.DenNgay IS NULL OR bg.DenNgay >= CONVERT(date, SYSDATETIME()))
ORDER BY ct.MaDichVu, bg.TuNgay DESC;

SELECT ctp.MaPhieuTinhPhi, ctp.MaChiDinh, ctp.MoTa, ctp.SoLuong,
       ctp.DonGia, ctp.ThanhTien
FROM dbo.ChiTietPhieuTinhPhi ctp
WHERE ctp.MaChiDinh IS NOT NULL
ORDER BY ctp.MaChiDinh;
```

### 8.7. Quản lý phiếu khám

Màn hình **Phiếu khám** đọc danh sách, thống kê, bộ lọc và nội dung chi tiết từ SQL
Server; người dùng có thể tìm kiếm, lọc theo ngày lập/trạng thái/bác sĩ/chuyên khoa
và phân trang. Tài khoản cần quyền hiện có `KHAM_HOSO`. Bác sĩ chỉ xem và thao tác
với phiếu thuộc lượt khám mình phụ trách; tài khoản có quyền hiện có `NHANSU_SUA`
có thể quản lý phiếu của các bác sĩ.

Chỉ tạo phiếu nháp cho lượt khám đang `DangKham` hoặc `ChoKetQua` và chưa có phiếu
liên kết. Chỉ cập nhật phiếu `Nhap` khi lượt khám còn ở một trong hai trạng thái
trên; nội dung bắt buộc gồm triệu chứng, kết quả lâm sàng và chẩn đoán. Phiếu đã ký,
hoàn tất hoặc khóa chỉ được xem. Màn hình không ký phiếu, tạo y lệnh, chỉ định dịch
vụ hay tính viện phí; không thay đổi schema.
