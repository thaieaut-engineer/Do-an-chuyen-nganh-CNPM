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
| Hệ quản trị cơ sở dữ liệu | MySQL 8.0+ |
| Storage Engine | InnoDB |
| Character Set | `utf8mb4` |
| Collation | `utf8mb4_unicode_ci` |
| Công nghệ kết nối | Java Database Connectivity (JDBC) |
| JDBC Driver | `com.mysql.cj.jdbc.Driver` |
| Công cụ quản lý | MySQL Workbench, DBeaver, SQL Studio hoặc công cụ tương thích MySQL |

### Yêu cầu môi trường

- MySQL Server phiên bản 8.0 trở lên.
- Java Development Kit (JDK).
- MySQL Connector/J để kết nối ứng dụng Java với MySQL.
- Công cụ quản lý và truy vấn cơ sở dữ liệu.

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

### 4.1. Cài đặt bằng công cụ quản lý MySQL

**Bước 1:** Khởi động MySQL Server và kết nối đến máy chủ cơ sở dữ liệu.

**Bước 2:** Mở tệp SQL:

```text
QuanLyPhongKhamThuCuc_Database_ThietKeMoi.sql
```

**Bước 3:** Kiểm tra kết nối đang sử dụng đúng MySQL Server.

**Bước 4:** Thực thi toàn bộ nội dung SQL Script bằng chức năng **Run / Execute** của công cụ.

**Bước 5:** Sau khi thực thi thành công, làm mới danh sách cơ sở dữ liệu và kiểm tra:

```sql
SHOW DATABASES;
```

Kiểm tra các bảng trong cơ sở dữ liệu:

```sql
USE QuanLyPhongKhamThuCuc;

SHOW TABLES;
```

### 4.2. Cài đặt bằng Terminal / Command Line

Có thể sử dụng MySQL Command Line Client để thực thi trực tiếp tệp SQL:

```bash
mysql -u root -p < QuanLyPhongKhamThuCuc_Database_ThietKeMoi.sql
```

Sau đó nhập mật khẩu tài khoản MySQL khi được yêu cầu.

### 4.3. Kiểm tra sau khi cài đặt

Sử dụng câu lệnh sau để kiểm tra cơ sở dữ liệu:

```sql
USE QuanLyPhongKhamThuCuc;

SELECT COUNT(*) AS TongSoBang
FROM information_schema.tables
WHERE table_schema = 'QuanLyPhongKhamThuCuc';
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

Ứng dụng Java sử dụng **JDBC (Java Database Connectivity)** để kết nối và thao tác với cơ sở dữ liệu MySQL.

### 6.1. Cài đặt MySQL Connector/J

Nếu sử dụng Maven, thêm dependency sau vào tệp `pom.xml`:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.5.0</version>
</dependency>
```

Nếu không sử dụng Maven, có thể thêm tệp MySQL Connector/J `.jar` vào Libraries của dự án.

### 6.2. Tạo lớp kết nối DatabaseManager.java

Tạo lớp `DatabaseManager` trong package `com.thucuc.clinic.dal`:

```java
package com.thucuc.clinic.dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL =
        "jdbc:mysql://localhost:3306/QuanLyPhongKhamThuCuc"
        + "?useSSL=false"
        + "&allowPublicKeyRetrieval=true"
        + "&serverTimezone=Asia/Ho_Chi_Minh";

    private static final String USER = "root";
    private static final String PASSWORD = "your_password_here";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

**Lưu ý:**

- Thay `your_password_here` bằng mật khẩu MySQL của máy tính.
- Kiểm tra lại tên database và cổng kết nối MySQL.
- Không nên lưu mật khẩu thật trực tiếp trong mã nguồn khi đưa dự án lên GitHub.
- Với phiên bản MySQL Connector/J hiện đại, JDBC Driver được tự động đăng ký, không cần gọi `Class.forName()` trong trường hợp thông thường.

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

try (Connection conn = DatabaseManager.getConnection();
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
        conn = DatabaseManager.getConnection();
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

---

## 8. Thiết kế giao diện

Ứng dụng sử dụng Java Swing và FlatLaf. Điểm chạy `view.MainFrame` mở màn hình đăng nhập demo; sau khi nhập tên đăng nhập và mật khẩu bất kỳ (không để trống), ứng dụng mở màn hình chính. Chọn vai trò demo tại màn hình đăng nhập để xem menu tương ứng. Giai đoạn UI sử dụng Mock Data và không kết nối cơ sở dữ liệu.

Để chạy trong NetBeans, mở thư mục dự án hoặc tệp `pom.xml` dưới dạng Maven project, chờ Maven tải các dependencies, sau đó chọn **Run Project**. Action Maven của NetBeans trong `nbactions.xml` gọi `process-classes` và `exec:java` với runtime classpath; lớp khởi chạy là `view.MainFrame`. Có thể chạy trực tiếp lớp này bằng **Run File**. Dự án dùng JDK 17 trở lên. Lớp `com.vetcare.ql_phong_kham.QL_Phong_Kham` vẫn chuyển tiếp tới `MainFrame.main` để tương thích với cấu hình chạy cũ.

Giai đoạn 1 đã xây dựng:

- **Đăng nhập:** kiểm tra trường bắt buộc, hiện/ẩn mật khẩu, ghi nhớ đăng nhập, quên mật khẩu demo và chọn vai trò để xem menu.
- **Khung ứng dụng:** sidebar có thể thu gọn, điều hướng theo vai trò, header có tìm kiếm nhanh, thông báo mẫu, menu tài khoản và đăng xuất.
- **Tổng quan:** sáu thẻ thống kê, biểu đồ bệnh nhân và doanh thu theo tháng, trạng thái lịch hẹn, danh sách lịch hẹn và hoạt động gần đây bằng Mock Data.
- Các mục nghiệp vụ khác đang là khung điều hướng; form và thao tác nghiệp vụ sẽ được triển khai ở các giai đoạn tiếp theo.

Tệp `database/schema.sql`, cấu hình JDBC và các phần hướng dẫn cơ sở dữ liệu hiện có trong tài liệu vẫn theo MySQL. Chúng chưa được chuyển đổi sang SQL Server; cần thực hiện chuyển đổi schema và cấu hình kết nối riêng trước khi triển khai backend. UI hiện tại không gọi JDBC và không cần JDBC driver để chạy.

---

## 📌 Ghi chú

- Tên bảng và tên cột trong tài liệu được trình bày theo thiết kế cơ sở dữ liệu của dự án.
- Các câu lệnh SQL và mã Java trong tài liệu có mục đích minh họa cách kết nối, truy vấn và xử lý nghiệp vụ.
- Khi triển khai thực tế, cần đối chiếu các câu lệnh với cấu trúc bảng và kiểu dữ liệu trong SQL Script.
- Không đưa mật khẩu MySQL, thông tin tài khoản hoặc dữ liệu cá nhân thật lên GitHub.

---

**Project:** Quản lý phòng khám Thu Cúc  
**Database:** `QuanLyPhongKhamThuCuc`  
**Database Management System:** MySQL 8.0+  
**Backend Connectivity:** Java JDBC  
