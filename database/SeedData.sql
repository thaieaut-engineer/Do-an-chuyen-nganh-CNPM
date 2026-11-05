/*
    Du lieu mau cho schema QuanLyPhongKhamThuCuc (SQL Server).
    Script chi INSERT, khong thay doi schema va co the chay lai.
    Tai khoan mau dung PBKDF2-HMAC-SHA256 theo auth.PasswordHasher.
    Marker cu chi duoc thay the cho cac tai khoan seed con giu marker nay.
*/
IF DB_ID(N'QuanLyPhongKhamThuCuc') IS NULL
BEGIN
    THROW 51000, N'Chua ton tai database QuanLyPhongKhamThuCuc. Hay chay schema.sql truoc.', 1;
END;
GO

USE QuanLyPhongKhamThuCuc;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @Today DATE = CONVERT(DATE, GETDATE());
DECLARE @Now DATETIME2(0) = SYSDATETIME();
DECLARE @ReviewPeriod NVARCHAR(30) =
    CONCAT(YEAR(CONVERT(DATE, GETDATE())), N'-Q', DATEPART(QUARTER, GETDATE()));
DECLARE @InvalidPasswordMarker VARCHAR(255) = 'NOT-A-REAL-HASH-AUTHENTICATION-NOT-IMPLEMENTED';

DECLARE @Employees TABLE (
    MaNhanVien VARCHAR(20) PRIMARY KEY,
    HoTen NVARCHAR(150),
    NgaySinh DATE,
    GioiTinh NVARCHAR(10),
    SoDienThoai VARCHAR(20),
    Email VARCHAR(120),
    DiaChi NVARCHAR(255),
    NgayBatDau DATE,
    TrangThaiCongTac NVARCHAR(20),
    MaBoPhan VARCHAR(20),
    MaChucDanh VARCHAR(20)
);

DECLARE @Patients TABLE (
    MaBenhNhan VARCHAR(20) PRIMARY KEY,
    HoTen NVARCHAR(150),
    NgaySinh DATE,
    GioiTinh NVARCHAR(10),
    SoGiayTo VARCHAR(40),
    SoDienThoai VARCHAR(20),
    Email VARCHAR(120),
    DiaChi NVARCHAR(255)
);

DECLARE @AppointmentSeed TABLE (
    MaBenhNhan VARCHAR(20) PRIMARY KEY,
    MaNhanVien VARCHAR(20),
    NgayOffset INT,
    GioHen TIME(0),
    TrangThai NVARCHAR(20),
    LyDoKham NVARCHAR(500),
    GhiChu NVARCHAR(255)
);

DECLARE @AppointmentMap TABLE (
    MaLichHen BIGINT PRIMARY KEY,
    MaBenhNhan VARCHAR(20),
    MaKhungGio BIGINT,
    MaNhanVien VARCHAR(20),
    ThoiGianHen DATETIME2(0),
    TrangThai NVARCHAR(20)
);

DECLARE @VisitMap TABLE (
    MaLuotKham BIGINT PRIMARY KEY,
    MaLichHen BIGINT,
    MaBenhNhan VARCHAR(20),
    MaNhanVien VARCHAR(20),
    ThoiGianTiepNhan DATETIME2(0),
    TrangThai NVARCHAR(20)
);

DECLARE @PurchaseItems TABLE (
    LoaiHang VARCHAR(10),
    MaHang VARCHAR(20),
    SoLo VARCHAR(80),
    SoLuong DECIMAL(14,2),
    DonGia DECIMAL(14,2),
    PRIMARY KEY (LoaiHang, MaHang, SoLo)
);

DECLARE @ServicePrices TABLE (
    MaDichVu VARCHAR(20) PRIMARY KEY,
    DonGia DECIMAL(14,2)
);

DECLARE @AccountSeed TABLE (
    TenDangNhap VARCHAR(80) PRIMARY KEY,
    MaNhanVien VARCHAR(20),
    EmailKhoiPhuc VARCHAR(120),
    MatKhauHash VARCHAR(255),
    MaVaiTro VARCHAR(20)
);

BEGIN TRY
    BEGIN TRANSACTION;

    INSERT INTO @Employees VALUES
    ('NV001', N'Nguyễn Hoàng Minh', '1985-03-12', N'Nam', '0901000001', 'admin@thucuc-demo.vn', N'Ba Đình, Hà Nội', '2018-01-08', N'DangLam', 'BP-ADMIN', 'CD-QUANLY'),
    ('NV002', N'Trần Thu Hương', '1990-07-24', N'Nữ', '0901000002', 'huong.le-tan@thucuc-demo.vn', N'Cầu Giấy, Hà Nội', '2020-05-04', N'DangLam', 'BP-TIEPNHAN', 'CD-LETAN'),
    ('NV003', N'Phạm Đức Thành', '1978-11-06', N'Nam', '0901000003', 'thanh.pham@thucuc-demo.vn', N'Đống Đa, Hà Nội', '2017-03-01', N'DangLam', 'BP-KHAM', 'CD-BACSI'),
    ('NV004', N'Lê Ngọc Mai', '1982-02-19', N'Nữ', '0901000004', 'mai.le@thucuc-demo.vn', N'Hai Bà Trưng, Hà Nội', '2018-08-15', N'DangLam', 'BP-KHAM', 'CD-BACSI'),
    ('NV005', N'Vũ Quang Huy', '1986-09-10', N'Nam', '0901000005', 'huy.vu@thucuc-demo.vn', N'Thanh Xuân, Hà Nội', '2019-02-11', N'DangLam', 'BP-KHAM', 'CD-BACSI'),
    ('NV006', N'Đặng Thị Lan', '1984-05-03', N'Nữ', '0901000006', 'lan.dang@thucuc-demo.vn', N'Long Biên, Hà Nội', '2019-10-01', N'DangLam', 'BP-KHAM', 'CD-BACSI'),
    ('NV007', N'Ngô Minh Quân', '1980-12-27', N'Nam', '0901000007', 'quan.ngo@thucuc-demo.vn', N'Tây Hồ, Hà Nội', '2016-06-20', N'DangLam', 'BP-KHAM', 'CD-BACSI'),
    ('NV008', N'Hoàng Thu Trang', '1988-04-14', N'Nữ', '0901000008', 'trang.hoang@thucuc-demo.vn', N'Hoàng Mai, Hà Nội', '2021-01-12', N'DangLam', 'BP-KHAM', 'CD-BACSI'),
    ('NV009', N'Bùi Văn Phúc', '1987-08-31', N'Nam', '0901000009', 'phuc.kho@thucuc-demo.vn', N'Nam Từ Liêm, Hà Nội', '2020-09-07', N'DangLam', 'BP-KHO', 'CD-THUKHO'),
    ('NV010', N'Nguyễn Thị Vân', '1992-01-18', N'Nữ', '0901000010', 'van.thungan@thucuc-demo.vn', N'Hoàn Kiếm, Hà Nội', '2022-04-18', N'DangLam', 'BP-TAICHINH', 'CD-THUNGAN'),
    ('NV011', N'Đỗ Thành Nam', '1983-06-22', N'Nam', '0901000011', 'nam.quanly@thucuc-demo.vn', N'Hà Đông, Hà Nội', '2019-12-02', N'DangLam', 'BP-ADMIN', 'CD-QUANLY');

    INSERT INTO BoPhan (MaBoPhan, TenBoPhan, MoTa, TrangThai)
    SELECT v.MaBoPhan, v.TenBoPhan, v.MoTa, N'HoatDong'
    FROM (VALUES
        ('BP-ADMIN', N'Ban quản lý và hành chính', N'Quản trị vận hành và hệ thống'),
        ('BP-TIEPNHAN', N'Tiếp đón và chăm sóc khách hàng', N'Tiếp nhận bệnh nhân, đặt lịch'),
        ('BP-KHAM', N'Khối khám chuyên khoa', N'Đội ngũ bác sĩ và chuyên môn'),
        ('BP-KHO', N'Kho dược và vật tư y tế', N'Quản lý thuốc, hóa chất và vật tư'),
        ('BP-TAICHINH', N'Tài chính và viện phí', N'Thu ngân, hóa đơn và thanh toán')
    ) v(MaBoPhan, TenBoPhan, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM BoPhan b WHERE b.MaBoPhan = v.MaBoPhan);

    INSERT INTO ChucDanh (MaChucDanh, TenChucDanh, MoTa)
    SELECT v.MaChucDanh, v.TenChucDanh, v.MoTa
    FROM (VALUES
        ('CD-QUANLY', N'Quản lý phòng khám', N'Quản lý vận hành và nhân sự'),
        ('CD-LETAN', N'Nhân viên lễ tân', N'Tiếp đón và điều phối lịch hẹn'),
        ('CD-BACSI', N'Bác sĩ', N'Thăm khám, chẩn đoán và điều trị'),
        ('CD-THUKHO', N'Nhân viên phụ trách kho', N'Quản lý thuốc và vật tư'),
        ('CD-THUNGAN', N'Thu ngân', N'Tính phí và tiếp nhận thanh toán')
    ) v(MaChucDanh, TenChucDanh, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM ChucDanh c WHERE c.MaChucDanh = v.MaChucDanh);

    INSERT INTO ChuyenKhoa (MaChuyenKhoa, TenChuyenKhoa, MoTa, TrangThai)
    SELECT v.MaChuyenKhoa, v.TenChuyenKhoa, v.MoTa, N'HoatDong'
    FROM (VALUES
        ('CK-NOI', N'Nội tổng quát', N'Khám và điều trị bệnh lý nội khoa'),
        ('CK-TIM', N'Tim mạch', N'Tầm soát và điều trị bệnh tim mạch'),
        ('CK-NHI', N'Nhi khoa', N'Chăm sóc sức khỏe trẻ em'),
        ('CK-DA', N'Da liễu', N'Khám và điều trị bệnh da'),
        ('CK-TAIMUIHONG', N'Tai mũi họng', N'Khám chuyên khoa tai mũi họng'),
        ('CK-CHINHHINH', N'Cơ xương khớp', N'Khám và điều trị cơ xương khớp')
    ) v(MaChuyenKhoa, TenChuyenKhoa, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM ChuyenKhoa c WHERE c.MaChuyenKhoa = v.MaChuyenKhoa);

    INSERT INTO NhanVien (
        MaNhanVien, HoTen, NgaySinh, GioiTinh, SoDienThoai, Email, DiaChi,
        NgayBatDau, TrangThaiCongTac, MaBoPhan, MaChucDanh)
    SELECT e.MaNhanVien, e.HoTen, e.NgaySinh, e.GioiTinh, e.SoDienThoai,
           e.Email, e.DiaChi, e.NgayBatDau, e.TrangThaiCongTac, e.MaBoPhan, e.MaChucDanh
    FROM @Employees e
    WHERE NOT EXISTS (SELECT 1 FROM NhanVien n WHERE n.MaNhanVien = e.MaNhanVien);

    INSERT INTO BacSi (MaNhanVien, HocVi, ChuyenMon, MaChuyenKhoa, ChungChiHanhNghe)
    SELECT v.MaNhanVien, v.HocVi, v.ChuyenMon, v.MaChuyenKhoa, v.ChungChiHanhNghe
    FROM (VALUES
        ('NV003', N'Thạc sĩ, Bác sĩ', N'Nội khoa tổng quát', 'CK-NOI', N'CCHN-DEMO-0003'),
        ('NV004', N'Tiến sĩ, Bác sĩ', N'Tim mạch', 'CK-TIM', N'CCHN-DEMO-0004'),
        ('NV005', N'Thạc sĩ, Bác sĩ', N'Nhi khoa', 'CK-NHI', N'CCHN-DEMO-0005'),
        ('NV006', N'Bác sĩ chuyên khoa I', N'Da liễu', 'CK-DA', N'CCHN-DEMO-0006'),
        ('NV007', N'Thạc sĩ, Bác sĩ', N'Tai mũi họng', 'CK-TAIMUIHONG', N'CCHN-DEMO-0007'),
        ('NV008', N'Bác sĩ chuyên khoa I', N'Cơ xương khớp', 'CK-CHINHHINH', N'CCHN-DEMO-0008')
    ) v(MaNhanVien, HocVi, ChuyenMon, MaChuyenKhoa, ChungChiHanhNghe)
    WHERE NOT EXISTS (SELECT 1 FROM BacSi b WHERE b.MaNhanVien = v.MaNhanVien);

    INSERT INTO VaiTro (MaVaiTro, TenVaiTro, MoTa, TrangThai)
    SELECT v.MaVaiTro, v.TenVaiTro, v.MoTa, N'HoatDong'
    FROM (VALUES
        ('VT-ADMIN', N'Quản trị viên', N'Quản trị tài khoản, cấu hình và toàn bộ phân hệ'),
        ('VT-LETAN', N'Lễ tân', N'Tiếp nhận bệnh nhân và quản lý lịch hẹn'),
        ('VT-BACSI', N'Bác sĩ', N'Truy cập lịch khám, hồ sơ và chỉ định chuyên môn'),
        ('VT-KHO', N'Nhân viên kho', N'Quản lý thuốc, hóa chất và chứng từ kho'),
        ('VT-THUNGAN', N'Thu ngân', N'Tính phí, hóa đơn và giao dịch'),
        ('VT-QUANLY', N'Quản lý phòng khám', N'Xem báo cáo, nhân sự và điều phối hoạt động')
    ) v(MaVaiTro, TenVaiTro, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM VaiTro r WHERE r.MaVaiTro = v.MaVaiTro);

    INSERT INTO Quyen (MaQuyen, TenQuyen, NhomChucNang, MoTa)
    SELECT v.MaQuyen, v.TenQuyen, v.NhomChucNang, v.MoTa
    FROM (VALUES
        ('BN_XEM', N'Xem hồ sơ bệnh nhân', N'Bệnh nhân', N'Tra cứu hồ sơ bệnh nhân'),
        ('BN_SUA', N'Tạo và cập nhật hồ sơ', N'Bệnh nhân', N'Tiếp nhận và cập nhật thông tin bệnh nhân'),
        ('LH_QUANLY', N'Quản lý lịch hẹn', N'Lịch hẹn', N'Tạo, xác nhận và cập nhật lịch hẹn'),
        ('KHAM_HOSO', N'Quản lý hồ sơ khám', N'Khám bệnh', N'Lập và cập nhật phiếu khám'),
        ('KHAM_CHIDINH', N'Tạo chỉ định dịch vụ', N'Khám bệnh', N'Chỉ định dịch vụ cận lâm sàng'),
        ('DONTHUOC_KE', N'Kê đơn thuốc', N'Dược', N'Tạo đơn thuốc cho lượt khám'),
        ('KHO_XEM', N'Xem tồn kho', N'Kho', N'Tra cứu lô và số lượng tồn'),
        ('KHO_NHAP', N'Ghi nhận nhập kho', N'Kho', N'Lập phiếu nhập thuốc và hóa chất'),
        ('KHO_XUAT', N'Ghi nhận xuất kho', N'Kho', N'Lập phiếu xuất và cấp phát thuốc'),
        ('VIENPHI_TINH', N'Tính viện phí', N'Viện phí', N'Lập phiếu tính phí'),
        ('HOADON_QUANLY', N'Quản lý hóa đơn', N'Viện phí', N'Tạo và tra cứu hóa đơn'),
        ('THANHTOAN_THU', N'Tiếp nhận thanh toán', N'Viện phí', N'Ghi nhận giao dịch thanh toán'),
        ('NHANSU_XEM', N'Xem danh sách nhân sự', N'Nhân sự', N'Tra cứu hồ sơ nhân viên'),
        ('NHANSU_SUA', N'Quản lý nhân sự', N'Nhân sự', N'Tạo và cập nhật hồ sơ nhân viên'),
        ('THIETBI_QUANLY', N'Quản lý thiết bị', N'Thiết bị', N'Theo dõi thiết bị và bảo trì'),
        ('BAOCAO_XEM', N'Xem báo cáo', N'Báo cáo', N'Tra cứu báo cáo vận hành'),
        ('TAIKHOAN_QUANLY', N'Quản lý tài khoản', N'Quản trị', N'Tạo tài khoản và phân quyền'),
        ('CAUHINH_QUANLY', N'Quản lý cấu hình', N'Quản trị', N'Cập nhật cấu hình hệ thống'),
        ('NHATKY_XEM', N'Xem nhật ký hệ thống', N'Quản trị', N'Tra cứu nhật ký hoạt động')
    ) v(MaQuyen, TenQuyen, NhomChucNang, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM Quyen q WHERE q.MaQuyen = v.MaQuyen);

    INSERT INTO VaiTroQuyen (MaVaiTro, MaQuyen)
    SELECT v.MaVaiTro, v.MaQuyen
    FROM (VALUES
        ('VT-ADMIN', 'BN_XEM'), ('VT-ADMIN', 'BN_SUA'), ('VT-ADMIN', 'LH_QUANLY'),
        ('VT-ADMIN', 'KHAM_HOSO'), ('VT-ADMIN', 'KHAM_CHIDINH'), ('VT-ADMIN', 'DONTHUOC_KE'),
        ('VT-ADMIN', 'KHO_XEM'), ('VT-ADMIN', 'KHO_NHAP'), ('VT-ADMIN', 'KHO_XUAT'),
        ('VT-ADMIN', 'VIENPHI_TINH'), ('VT-ADMIN', 'HOADON_QUANLY'), ('VT-ADMIN', 'THANHTOAN_THU'),
        ('VT-ADMIN', 'NHANSU_XEM'), ('VT-ADMIN', 'NHANSU_SUA'), ('VT-ADMIN', 'THIETBI_QUANLY'),
        ('VT-ADMIN', 'BAOCAO_XEM'), ('VT-ADMIN', 'TAIKHOAN_QUANLY'), ('VT-ADMIN', 'CAUHINH_QUANLY'),
        ('VT-ADMIN', 'NHATKY_XEM'),
        ('VT-LETAN', 'BN_XEM'), ('VT-LETAN', 'BN_SUA'), ('VT-LETAN', 'LH_QUANLY'),
        ('VT-LETAN', 'NHANSU_XEM'),
        ('VT-BACSI', 'BN_XEM'), ('VT-BACSI', 'LH_QUANLY'), ('VT-BACSI', 'KHAM_HOSO'),
        ('VT-BACSI', 'KHAM_CHIDINH'), ('VT-BACSI', 'DONTHUOC_KE'),
        ('VT-KHO', 'KHO_XEM'), ('VT-KHO', 'KHO_NHAP'), ('VT-KHO', 'KHO_XUAT'),
        ('VT-KHO', 'DONTHUOC_KE'),
        ('VT-THUNGAN', 'BN_XEM'), ('VT-THUNGAN', 'VIENPHI_TINH'),
        ('VT-THUNGAN', 'HOADON_QUANLY'), ('VT-THUNGAN', 'THANHTOAN_THU'),
        ('VT-QUANLY', 'BN_XEM'), ('VT-QUANLY', 'LH_QUANLY'), ('VT-QUANLY', 'NHANSU_XEM'),
        ('VT-QUANLY', 'THIETBI_QUANLY'), ('VT-QUANLY', 'BAOCAO_XEM')
    ) v(MaVaiTro, MaQuyen)
    WHERE NOT EXISTS (
        SELECT 1 FROM VaiTroQuyen x
        WHERE x.MaVaiTro = v.MaVaiTro AND x.MaQuyen = v.MaQuyen
    );

    INSERT INTO @AccountSeed VALUES
    ('admin', 'NV001', 'admin@thucuc-demo.vn', 'pbkdf2-sha256$240000$XYNlAEL7hyn5jXb9L3iN_g$IJbZBuncxRhGIpo_PfIsdsBgIJL1GiS8egyVBx7sJzM', 'VT-ADMIN'),
    ('letan.huong', 'NV002', 'huong.le-tan@thucuc-demo.vn', 'pbkdf2-sha256$240000$p5QRSGx5HGxP6gshga37Bg$9nvNhkNVJHXdxjxHpAauDkb5VZKlAyWSilP8r4ZawDc', 'VT-LETAN'),
    ('bacsi.thanh', 'NV003', 'thanh.pham@thucuc-demo.vn', 'pbkdf2-sha256$240000$M-oa7_m3V1idu5ZSFEro8g$ytOPU4ANQ5xjTluwQWSY22sJT-qJC35yRqV7BgqTvI0', 'VT-BACSI'),
    ('kho.phuc', 'NV009', 'phuc.kho@thucuc-demo.vn', 'pbkdf2-sha256$240000$WZ6KNQRHdP3ea2Gq0UD5xQ$U_5BmIOhI9RH4DnfYaxzn3WfMVzGGMqs-Gs7SUotUnM', 'VT-KHO'),
    ('thungan.van', 'NV010', 'van.thungan@thucuc-demo.vn', 'pbkdf2-sha256$240000$cwMz-vanBEXxEz17EJzCGg$cx8S9Vu5eK5AjhuOeXSvBdTqQgBsrtTWXO_nV-vuhO4', 'VT-THUNGAN'),
    ('quanly.nam', 'NV011', 'nam.quanly@thucuc-demo.vn', 'pbkdf2-sha256$240000$XLr7LdWqqVu-CVqqt6_C9Q$sG3j4KfIBshuXvBtAaEMCo4b_DElhr7ZchB8omPunQo', 'VT-QUANLY');

    UPDATE a
    SET MatKhauHash = s.MatKhauHash
    FROM TaiKhoan a
    JOIN @AccountSeed s ON s.TenDangNhap = a.TenDangNhap
    WHERE a.MatKhauHash = @InvalidPasswordMarker;

    INSERT INTO TaiKhoan (TenDangNhap, MatKhauHash, MaNhanVien, EmailKhoiPhuc, TrangThai)
    SELECT s.TenDangNhap, s.MatKhauHash, s.MaNhanVien, s.EmailKhoiPhuc, N'HoatDong'
    FROM @AccountSeed s
    WHERE NOT EXISTS (SELECT 1 FROM TaiKhoan a WHERE a.TenDangNhap = s.TenDangNhap);

    INSERT INTO TaiKhoanVaiTro (MaTaiKhoan, MaVaiTro, NguoiGan)
    SELECT a.MaTaiKhoan, v.MaVaiTro, adminAccount.MaTaiKhoan
    FROM @AccountSeed v
    JOIN TaiKhoan a ON a.TenDangNhap = v.TenDangNhap
    CROSS JOIN (SELECT MaTaiKhoan FROM TaiKhoan WHERE TenDangNhap = 'admin') adminAccount
    WHERE NOT EXISTS (
        SELECT 1 FROM TaiKhoanVaiTro x
        WHERE x.MaTaiKhoan = a.MaTaiKhoan AND x.MaVaiTro = v.MaVaiTro
    );

    INSERT INTO CauHinhHeThong (MaCauHinh, GiaTri, KieuDuLieu, MoTa, CapNhatBoi)
    SELECT v.MaCauHinh, v.GiaTri, v.KieuDuLieu, v.MoTa, a.MaTaiKhoan
    FROM (VALUES
        ('PHONGKHAM_TEN', N'Phòng khám Thu Cúc', 'STRING', N'Tên hiển thị của cơ sở khám'),
        ('LICHHEN_THOILUONG_PHUT', N'30', 'INTEGER', N'Thời lượng một khung khám (phút)'),
        ('LICHHEN_DAT_TRUOC_NGAY', N'30', 'INTEGER', N'Số ngày tối đa có thể đặt lịch trước'),
        ('BHYT_TYLE_MACDINH', N'80', 'DECIMAL', N'Tỷ lệ tham khảo; mức thực tế tùy quyền lợi thẻ'),
        ('KHO_CANHBAO_HAN_NGAY', N'90', 'INTEGER', N'Cảnh báo thuốc sắp hết hạn')
    ) v(MaCauHinh, GiaTri, KieuDuLieu, MoTa)
    CROSS JOIN (SELECT MaTaiKhoan FROM TaiKhoan WHERE TenDangNhap = 'admin') a
    WHERE NOT EXISTS (SELECT 1 FROM CauHinhHeThong c WHERE c.MaCauHinh = v.MaCauHinh);

    INSERT INTO @Patients VALUES
    ('BN001', N'Nguyễn Minh Anh', '1992-04-12', N'Nữ', 'DEMO00000001', '0912000001', 'minhanh.demo@example.invalid', N'Ba Đình, Hà Nội'),
    ('BN002', N'Trần Hoàng Nam', '1987-09-23', N'Nam', 'DEMO00000002', '0912000002', 'hoangnam.demo@example.invalid', N'Cầu Giấy, Hà Nội'),
    ('BN003', N'Phạm Ngọc Lan', '2001-02-08', N'Nữ', 'DEMO00000003', '0912000003', 'ngoclan.demo@example.invalid', N'Đống Đa, Hà Nội'),
    ('BN004', N'Đỗ Gia Bảo', '2017-06-19', N'Nam', 'DEMO00000004', '0912000004', 'giabao.demo@example.invalid', N'Hai Bà Trưng, Hà Nội'),
    ('BN005', N'Lê Khánh Vy', '1995-11-01', N'Nữ', 'DEMO00000005', '0912000005', 'khanhvy.demo@example.invalid', N'Thanh Xuân, Hà Nội'),
    ('BN006', N'Bùi Thanh Tùng', '1979-03-15', N'Nam', 'DEMO00000006', '0912000006', 'thanhtung.demo@example.invalid', N'Long Biên, Hà Nội'),
    ('BN007', N'Hoàng Thu Hà', '1968-12-30', N'Nữ', 'DEMO00000007', '0912000007', 'thuha.demo@example.invalid', N'Tây Hồ, Hà Nội'),
    ('BN008', N'Vũ Đức Long', '1990-08-07', N'Nam', 'DEMO00000008', '0912000008', 'duclong.demo@example.invalid', N'Nam Từ Liêm, Hà Nội'),
    ('BN009', N'Nguyễn Thị Hạnh', '1983-01-26', N'Nữ', 'DEMO00000009', '0912000009', 'thihanh.demo@example.invalid', N'Hoàng Mai, Hà Nội'),
    ('BN010', N'Phan Quang Minh', '2008-05-11', N'Nam', 'DEMO00000010', '0912000010', 'quangminh.demo@example.invalid', N'Hà Đông, Hà Nội'),
    ('BN011', N'Đặng Mỹ Linh', '1998-10-04', N'Nữ', 'DEMO00000011', '0912000011', 'mylinh.demo@example.invalid', N'Ba Đình, Hà Nội'),
    ('BN012', N'Đinh Văn Hùng', '1972-07-17', N'Nam', 'DEMO00000012', '0912000012', 'vanhung.demo@example.invalid', N'Cầu Giấy, Hà Nội'),
    ('BN013', N'Tạ Bảo Châu', '2012-02-21', N'Nữ', 'DEMO00000013', '0912000013', 'baochau.demo@example.invalid', N'Đống Đa, Hà Nội'),
    ('BN014', N'Ngô Đức Anh', '1989-06-02', N'Nam', 'DEMO00000014', '0912000014', 'ducanh.demo@example.invalid', N'Hai Bà Trưng, Hà Nội'),
    ('BN015', N'Võ Thị Mai', '1959-09-14', N'Nữ', 'DEMO00000015', '0912000015', 'thimai.demo@example.invalid', N'Thanh Xuân, Hà Nội'),
    ('BN016', N'Đoàn Minh Khang', '1994-12-09', N'Nam', 'DEMO00000016', '0912000016', 'minhkhang.demo@example.invalid', N'Long Biên, Hà Nội'),
    ('BN017', N'Lý Thu Phương', '2004-03-28', N'Nữ', 'DEMO00000017', '0912000017', 'thuphuong.demo@example.invalid', N'Tây Hồ, Hà Nội'),
    ('BN018', N'Nguyễn Hải Đăng', '2019-08-16', N'Nam', 'DEMO00000018', '0912000018', 'haidang.demo@example.invalid', N'Nam Từ Liêm, Hà Nội'),
    ('BN019', N'Trương Thị Thanh', '1976-05-25', N'Nữ', 'DEMO00000019', '0912000019', 'thanhtruong.demo@example.invalid', N'Hoàng Mai, Hà Nội'),
    ('BN020', N'Phạm Tuấn Kiệt', '1999-01-05', N'Nam', 'DEMO00000020', '0912000020', 'tuankiet.demo@example.invalid', N'Hà Đông, Hà Nội');

    INSERT INTO BenhNhan (MaBenhNhan, HoTen, NgaySinh, GioiTinh, SoGiayTo, SoDienThoai, Email, DiaChi)
    SELECT p.MaBenhNhan, p.HoTen, p.NgaySinh, p.GioiTinh, p.SoGiayTo,
           p.SoDienThoai, p.Email, p.DiaChi
    FROM @Patients p
    WHERE NOT EXISTS (SELECT 1 FROM BenhNhan b WHERE b.MaBenhNhan = p.MaBenhNhan);

    INSERT INTO CaLamViec (MaCa, TenCa, GioBatDau, GioKetThuc, ThoiGianNghiPhut, TrangThai)
    SELECT v.MaCa, v.TenCa, v.GioBatDau, v.GioKetThuc, 30, N'HoatDong'
    FROM (VALUES
        ('CA-SANG', N'Ca sáng', CAST('08:00' AS TIME), CAST('12:00' AS TIME)),
        ('CA-CHIEU', N'Ca chiều', CAST('13:00' AS TIME), CAST('17:00' AS TIME))
    ) v(MaCa, TenCa, GioBatDau, GioKetThuc)
    WHERE NOT EXISTS (SELECT 1 FROM CaLamViec c WHERE c.MaCa = v.MaCa);

    INSERT INTO @AppointmentSeed VALUES
    ('BN001', 'NV003', -12, '08:00', N'HoanTat', N'Khám sức khỏe định kỳ', N'Dữ liệu minh họa'),
    ('BN002', 'NV004', -11, '08:30', N'HoanTat', N'Đánh giá huyết áp và tim mạch', N'Dữ liệu minh họa'),
    ('BN003', 'NV005', -10, '09:00', N'HoanTat', N'Tái khám theo lịch', N'Dữ liệu minh họa'),
    ('BN004', 'NV005', -9, '09:30', N'HoanTat', N'Sốt nhẹ và ho', N'Dữ liệu minh họa'),
    ('BN005', 'NV006', -8, '10:00', N'HoanTat', N'Khám phát ban ngoài da', N'Dữ liệu minh họa'),
    ('BN006', 'NV003', -7, '10:30', N'HoanTat', N'Đau họng và mệt mỏi', N'Dữ liệu minh họa'),
    ('BN007', 'NV004', -6, '11:00', N'HoanTat', N'Tái khám tim mạch', N'Dữ liệu minh họa'),
    ('BN008', 'NV007', -5, '08:00', N'HoanTat', N'Ù tai và nghẹt mũi', N'Dữ liệu minh họa'),
    ('BN009', 'NV006', -4, '08:30', N'HoanTat', N'Tư vấn chăm sóc da', N'Dữ liệu minh họa'),
    ('BN010', 'NV005', -3, '09:00', N'HoanTat', N'Khám nhi định kỳ', N'Dữ liệu minh họa'),
    ('BN011', 'NV008', -2, '09:30', N'HoanTat', N'Đau mỏi vai gáy', N'Dữ liệu minh họa'),
    ('BN012', 'NV003', -1, '10:00', N'HoanTat', N'Khám sức khỏe tổng quát', N'Dữ liệu minh họa'),
    ('BN013', 'NV005', 0, '08:00', N'DaDen', N'Tái khám nhi khoa', N'Đã đến phòng khám'),
    ('BN014', 'NV004', 0, '08:30', N'DangKham', N'Đánh giá đau ngực nhẹ', N'Đang trong lượt khám'),
    ('BN015', 'NV006', 0, '09:00', N'DaXacNhan', N'Tư vấn da liễu', N'Đã xác nhận'),
    ('BN016', 'NV008', 1, '09:30', N'DaXacNhan', N'Đau lưng kéo dài', N'Đã xác nhận'),
    ('BN017', 'NV007', 2, '10:00', N'DaXacNhan', N'Khám tai mũi họng', N'Đã xác nhận'),
    ('BN018', 'NV005', 3, '10:30', N'ChoXacNhan', N'Trẻ ho và sổ mũi', N'Chờ gọi xác nhận'),
    ('BN019', 'NV003', -2, '11:00', N'DaHuy', N'Khách đề nghị đổi lịch', N'Lịch hẹn đã hủy'),
    ('BN020', 'NV004', -1, '11:30', N'VangMat', N'Khám tim mạch', N'Bệnh nhân vắng mặt');

    INSERT INTO LichPhanCong (
        MaNhanVien, MaCa, NgayLam, DiaDiem, TrangThai, MaNguoiDuyet, ThoiDiemDuyet, GhiChu)
    SELECT DISTINCT s.MaNhanVien,
           CASE WHEN s.GioHen < CAST('12:00' AS TIME) THEN 'CA-SANG' ELSE 'CA-CHIEU' END,
           DATEADD(DAY, s.NgayOffset, @Today), N'Phòng khám Thu Cúc',
           N'DaXacNhan', 'NV011', DATEADD(DAY, s.NgayOffset, CAST(@Today AS DATETIME2)),
           N'Lịch phân công minh họa'
    FROM @AppointmentSeed s
    WHERE NOT EXISTS (
        SELECT 1 FROM LichPhanCong l
        WHERE l.MaNhanVien = s.MaNhanVien
          AND l.MaCa = CASE WHEN s.GioHen < CAST('12:00' AS TIME) THEN 'CA-SANG' ELSE 'CA-CHIEU' END
          AND l.NgayLam = DATEADD(DAY, s.NgayOffset, @Today)
    );

    INSERT INTO KhungGioKham (
        MaNhanVien, NgayKham, GioBatDau, GioKetThuc, SoLuongToiDa, TrangThai)
    SELECT s.MaNhanVien, DATEADD(DAY, s.NgayOffset, @Today), s.GioHen,
           DATEADD(MINUTE, 30, s.GioHen), 1,
           CASE WHEN s.TrangThai IN (N'DaHuy', N'VangMat') THEN N'Khoa' ELSE N'Mo' END
    FROM @AppointmentSeed s
    WHERE NOT EXISTS (
        SELECT 1 FROM KhungGioKham k
        WHERE k.MaNhanVien = s.MaNhanVien
          AND k.NgayKham = DATEADD(DAY, s.NgayOffset, @Today)
          AND k.GioBatDau = s.GioHen
    );

    INSERT INTO LichHen (
        MaBenhNhan, MaKhungGio, MaChuyenKhoa, MaNhanVien, ThoiGianHen,
        LyDoKham, NgayTao, TrangThai, NguoiTao, GhiChu)
    SELECT s.MaBenhNhan, k.MaKhungGio, bs.MaChuyenKhoa, s.MaNhanVien,
           DATEADD(SECOND, DATEDIFF(SECOND, CAST('00:00' AS TIME), s.GioHen),
                   CAST(DATEADD(DAY, s.NgayOffset, @Today) AS DATETIME2)),
           s.LyDoKham,
           DATEADD(DAY, -2, DATEADD(SECOND, DATEDIFF(SECOND, CAST('00:00' AS TIME), s.GioHen),
                   CAST(DATEADD(DAY, s.NgayOffset, @Today) AS DATETIME2))),
           s.TrangThai, 'NV002', s.GhiChu
    FROM @AppointmentSeed s
    JOIN BacSi bs ON bs.MaNhanVien = s.MaNhanVien
    JOIN KhungGioKham k
      ON k.MaNhanVien = s.MaNhanVien
     AND k.NgayKham = DATEADD(DAY, s.NgayOffset, @Today)
     AND k.GioBatDau = s.GioHen
    WHERE NOT EXISTS (
        SELECT 1 FROM LichHen h
        WHERE h.MaBenhNhan = s.MaBenhNhan
          AND h.ThoiGianHen = DATEADD(SECOND, DATEDIFF(SECOND, CAST('00:00' AS TIME), s.GioHen),
                   CAST(DATEADD(DAY, s.NgayOffset, @Today) AS DATETIME2))
    );

    INSERT INTO @AppointmentMap (MaLichHen, MaBenhNhan, MaKhungGio, MaNhanVien, ThoiGianHen, TrangThai)
    SELECT h.MaLichHen, h.MaBenhNhan, h.MaKhungGio, h.MaNhanVien, h.ThoiGianHen, h.TrangThai
    FROM LichHen h
    JOIN @AppointmentSeed s ON s.MaBenhNhan = h.MaBenhNhan
    WHERE h.ThoiGianHen = DATEADD(SECOND, DATEDIFF(SECOND, CAST('00:00' AS TIME), s.GioHen),
                   CAST(DATEADD(DAY, s.NgayOffset, @Today) AS DATETIME2));

    INSERT INTO LichSuLichHen (MaLichHen, NguoiThayDoi, ThoiDiem, HanhDong, GiaTriMoi, LyDo)
    SELECT a.MaLichHen, 'NV002', DATEADD(DAY, -2, a.ThoiGianHen),
           N'TaoDuLieuMau', a.TrangThai, N'Lịch sử minh họa để kiểm thử'
    FROM @AppointmentMap a
    WHERE NOT EXISTS (
        SELECT 1 FROM LichSuLichHen x
        WHERE x.MaLichHen = a.MaLichHen AND x.HanhDong = N'TaoDuLieuMau'
    );

    INSERT INTO LuotKham (MaBenhNhan, MaLichHen, MaNhanVien, ThoiGianTiepNhan, ThoiGianKetThuc, TrangThai, GhiChu)
    SELECT a.MaBenhNhan, a.MaLichHen, a.MaNhanVien,
           DATEADD(MINUTE, -15, a.ThoiGianHen),
           CASE WHEN a.TrangThai = N'HoanTat' THEN DATEADD(MINUTE, 25, a.ThoiGianHen) ELSE NULL END,
           CASE a.TrangThai
               WHEN N'HoanTat' THEN N'HoanTat'
               WHEN N'DangKham' THEN N'DangKham'
               ELSE N'ChoKham'
           END,
           N'Lượt khám minh họa'
    FROM @AppointmentMap a
    WHERE a.TrangThai IN (N'HoanTat', N'DaDen', N'DangKham')
      AND NOT EXISTS (SELECT 1 FROM LuotKham l WHERE l.MaLichHen = a.MaLichHen);

    INSERT INTO @VisitMap (MaLuotKham, MaLichHen, MaBenhNhan, MaNhanVien, ThoiGianTiepNhan, TrangThai)
    SELECT l.MaLuotKham, l.MaLichHen, l.MaBenhNhan, l.MaNhanVien, l.ThoiGianTiepNhan, l.TrangThai
    FROM LuotKham l
    JOIN @AppointmentMap a ON a.MaLichHen = l.MaLichHen;

    INSERT INTO PhieuKham (
        MaLuotKham, MaNhanVien, LyDoKham, TrieuChung, DauHieuSinhTon,
        KetQuaLamSang, ChanDoan, HuongDieuTri, TrangThai, ThoiGianLap, ThoiGianKy)
    SELECT v.MaLuotKham, v.MaNhanVien, a.LyDoKham,
           CASE WHEN v.MaBenhNhan IN ('BN004', 'BN018')
                THEN N'Ho nhẹ, thân nhiệt tăng nhẹ, ăn uống bình thường'
                ELSE N'Triệu chứng ổn định, không ghi nhận dấu hiệu cấp cứu'
           END,
           N'Mạch 78 lần/phút; huyết áp 120/80 mmHg; nhiệt độ 36,8°C',
           N'Tình trạng tổng quát ổn định; kết quả minh họa phục vụ kiểm thử',
           CASE WHEN v.MaNhanVien = 'NV004' THEN N'Theo dõi tim mạch, chưa ghi nhận dấu hiệu nguy cấp'
                WHEN v.MaNhanVien = 'NV005' THEN N'Nhiễm siêu vi đường hô hấp trên mức độ nhẹ'
                WHEN v.MaNhanVien = 'NV006' THEN N'Viêm da kích ứng mức độ nhẹ'
                WHEN v.MaNhanVien = 'NV008' THEN N'Đau cơ xương khớp, chưa ghi nhận dấu hiệu thần kinh'
                ELSE N'Khám tổng quát, tình trạng ổn định'
           END,
           N'Nghỉ ngơi, uống đủ nước, dùng thuốc theo đơn nếu có; tái khám khi triệu chứng tăng',
           CASE WHEN v.TrangThai = N'HoanTat' THEN N'DaKy' ELSE N'Nhap' END,
           DATEADD(MINUTE, 5, v.ThoiGianTiepNhan),
           CASE WHEN v.TrangThai = N'HoanTat' THEN DATEADD(MINUTE, 20, v.ThoiGianTiepNhan) ELSE NULL END
    FROM @VisitMap v
    JOIN @AppointmentSeed a ON a.MaBenhNhan = v.MaBenhNhan
    WHERE NOT EXISTS (SELECT 1 FROM PhieuKham p WHERE p.MaLuotKham = v.MaLuotKham);

    INSERT INTO DichVu (MaDichVu, TenDichVu, LoaiDichVu, DonViTinh, MoTa, TrangThai)
    SELECT v.MaDichVu, v.TenDichVu, v.LoaiDichVu, N'Lần', v.MoTa, N'DangApDung'
    FROM (VALUES
        ('DV-KHAM', N'Khám chuyên khoa', N'Khám bệnh', N'Khám ban đầu hoặc tái khám'),
        ('DV-XN-MAU', N'Xét nghiệm công thức máu', N'Xét nghiệm', N'Xét nghiệm huyết học cơ bản'),
        ('DV-SIEUAM', N'Siêu âm tổng quát', N'Chẩn đoán hình ảnh', N'Siêu âm ổ bụng tổng quát'),
        ('DV-DIENTIM', N'Điện tâm đồ', N'Thăm dò chức năng', N'Ghi điện tâm đồ nghỉ'),
        ('DV-XQUANG', N'X-quang ngực thẳng', N'Chẩn đoán hình ảnh', N'Chụp X-quang ngực tư thế thẳng')
    ) v(MaDichVu, TenDichVu, LoaiDichVu, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM DichVu d WHERE d.MaDichVu = v.MaDichVu);

    INSERT INTO ChiDinhDichVu (
        MaPhieuKham, MaDichVu, MaNhanVienThucHien, ThoiGianChiDinh,
        ThoiGianThucHien, KetQua, TrangThai, GhiChu)
    SELECT p.MaPhieuKham, svc.MaDichVu, v.MaNhanVien,
           DATEADD(MINUTE, 5, v.ThoiGianTiepNhan),
           CASE WHEN v.TrangThai = N'HoanTat' THEN DATEADD(MINUTE, 15, v.ThoiGianTiepNhan) ELSE NULL END,
           CASE WHEN v.TrangThai = N'HoanTat' THEN N'Kết quả minh họa; cần bác sĩ đối chiếu khi dùng thật' ELSE NULL END,
           CASE WHEN v.TrangThai = N'HoanTat' THEN N'HoanTat'
                WHEN v.TrangThai = N'DangKham' THEN N'DangThucHien'
                ELSE N'ChoThucHien'
           END,
           N'Chỉ định dữ liệu mẫu'
    FROM @VisitMap v
    JOIN PhieuKham p ON p.MaLuotKham = v.MaLuotKham
    CROSS APPLY (VALUES
        ('DV-KHAM'),
        (CASE ABS(CHECKSUM(v.MaBenhNhan)) % 4
            WHEN 0 THEN 'DV-XN-MAU'
            WHEN 1 THEN 'DV-SIEUAM'
            WHEN 2 THEN 'DV-DIENTIM'
            ELSE 'DV-XQUANG'
         END)
    ) svc(MaDichVu)
    WHERE NOT EXISTS (
        SELECT 1 FROM ChiDinhDichVu x
        WHERE x.MaPhieuKham = p.MaPhieuKham AND x.MaDichVu = svc.MaDichVu
    );

    INSERT INTO Thuoc (
        MaThuoc, TenThuoc, HoatChat, HamLuong, DangBaoChe, DonViTinh,
        DuongDungMacDinh, HangSanXuat, NuocSanXuat, MoTa, TrangThai)
    SELECT v.MaThuoc, v.TenThuoc, v.HoatChat, v.HamLuong, v.DangBaoChe, v.DonViTinh,
           v.DuongDung, v.HangSanXuat, N'Việt Nam', v.MoTa, N'DangSuDung'
    FROM (VALUES
        ('THUOC-PARA', N'Paracetamol', N'Paracetamol', N'500 mg', N'Viên nén', N'Viên', N'Uống', N'Dược Hậu Giang', N'Hạ sốt, giảm đau thông thường'),
        ('THUOC-AMOX', N'Amoxicillin', N'Amoxicillin', N'500 mg', N'Viên nang', N'Viên', N'Uống', N'Imexpharm', N'Kháng sinh kê đơn minh họa'),
        ('THUOC-ORS', N'Oresol', N'Glucose và điện giải', N'4,1 g', N'Bột pha uống', N'Gói', N'Uống', N'Vinphaco', N'Bù nước điện giải'),
        ('THUOC-CETI', N'Cetirizine', N'Cetirizine', N'10 mg', N'Viên nén', N'Viên', N'Uống', N'Domesc', N'Thuốc kháng histamine'),
        ('THUOC-OMEP', N'Omeprazole', N'Omeprazole', N'20 mg', N'Viên nang', N'Viên', N'Uống', N'Traphaco', N'Thuốc giảm tiết acid dạ dày'),
        ('THUOC-VITC', N'Vitamin C', N'Acid ascorbic', N'500 mg', N'Viên nén', N'Viên', N'Uống', N'Dược phẩm 3/2', N'Chế phẩm bổ sung vitamin')
    ) v(MaThuoc, TenThuoc, HoatChat, HamLuong, DangBaoChe, DonViTinh, DuongDung, HangSanXuat, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM Thuoc t WHERE t.MaThuoc = v.MaThuoc);

    INSERT INTO DonThuoc (MaLuotKham, MaNhanVien, NgayKe, LoiDan, TrangThai)
    SELECT v.MaLuotKham, v.MaNhanVien, DATEADD(MINUTE, 10, v.ThoiGianTiepNhan),
           N'Dữ liệu kê đơn minh họa. Không sử dụng thay cho chỉ định điều trị thực tế.',
           N'DaCapPhat'
    FROM @VisitMap v
    WHERE v.TrangThai = N'HoanTat'
      AND NOT EXISTS (SELECT 1 FROM DonThuoc d WHERE d.MaLuotKham = v.MaLuotKham);

    INSERT INTO ChiTietDonThuoc (
        MaDonThuoc, MaThuoc, LieuDung, DuongDung, TanSuat, ThoiGianDung,
        SoLuong, DonViTinh, HuongDan)
    SELECT d.MaDonThuoc, x.MaThuoc, x.LieuDung, N'Uống', x.TanSuat, x.ThoiGianDung,
           x.SoLuong, x.DonViTinh, x.HuongDan
    FROM DonThuoc d
    JOIN @VisitMap v ON v.MaLuotKham = d.MaLuotKham
    CROSS APPLY (VALUES
        ('THUOC-PARA', N'1 viên/lần', N'Khi sốt hoặc đau, tối đa 3 lần/ngày', N'2 ngày', CAST(10 AS DECIMAL(12,2)), N'Viên', N'Uống sau ăn; không vượt quá liều được bác sĩ hướng dẫn'),
        ('THUOC-AMOX', N'1 viên/lần', N'2 lần/ngày', N'5 ngày', CAST(15 AS DECIMAL(12,2)), N'Viên', N'Dùng đủ liệu trình theo đơn minh họa; kiểm tra dị ứng trước khi dùng')
    ) x(MaThuoc, LieuDung, TanSuat, ThoiGianDung, SoLuong, DonViTinh, HuongDan)
    WHERE v.TrangThai = N'HoanTat'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietDonThuoc c
          WHERE c.MaDonThuoc = d.MaDonThuoc AND c.MaThuoc = x.MaThuoc
      );

    INSERT INTO LoThuoc (
        MaThuoc, SoLo, NgaySanXuat, HanSuDung, DonGiaNhap, SoLuongTon, ViTriLuuTru, TrangThai)
    SELECT v.MaThuoc, v.SoLo, DATEADD(MONTH, -6, @Today), DATEADD(YEAR, 2, @Today),
           v.DonGia, v.SoLuongTon, v.ViTri, N'DangTon'
    FROM (VALUES
        ('THUOC-PARA', 'SEED-PARA-01', CAST(500 AS DECIMAL(14,2)), CAST(100 AS DECIMAL(14,2)), N'Kệ A-01'),
        ('THUOC-AMOX', 'SEED-AMOX-01', CAST(1000 AS DECIMAL(14,2)), CAST(100 AS DECIMAL(14,2)), N'Kệ A-02'),
        ('THUOC-ORS', 'SEED-ORS-01', CAST(2500 AS DECIMAL(14,2)), CAST(100 AS DECIMAL(14,2)), N'Kệ A-03'),
        ('THUOC-CETI', 'SEED-CETI-01', CAST(1500 AS DECIMAL(14,2)), CAST(100 AS DECIMAL(14,2)), N'Kệ A-04'),
        ('THUOC-OMEP', 'SEED-OMEP-01', CAST(2000 AS DECIMAL(14,2)), CAST(100 AS DECIMAL(14,2)), N'Kệ A-05'),
        ('THUOC-VITC', 'SEED-VITC-01', CAST(800 AS DECIMAL(14,2)), CAST(100 AS DECIMAL(14,2)), N'Kệ A-06')
    ) v(MaThuoc, SoLo, DonGia, SoLuongTon, ViTri)
    WHERE NOT EXISTS (
        SELECT 1 FROM LoThuoc l WHERE l.MaThuoc = v.MaThuoc AND l.SoLo = v.SoLo
    );

    INSERT INTO @PurchaseItems VALUES
    ('THUOC', 'THUOC-PARA', 'SEED-PARA-01', 220, 500),
    ('THUOC', 'THUOC-AMOX', 'SEED-AMOX-01', 280, 1000),
    ('THUOC', 'THUOC-ORS', 'SEED-ORS-01', 100, 2500),
    ('THUOC', 'THUOC-CETI', 'SEED-CETI-01', 100, 1500),
    ('THUOC', 'THUOC-OMEP', 'SEED-OMEP-01', 100, 2000),
    ('THUOC', 'THUOC-VITC', 'SEED-VITC-01', 100, 800);

    IF NOT EXISTS (SELECT 1 FROM PhieuNhap WHERE SoPhieu = 'PN-SEED-THUOC-001')
    BEGIN
        INSERT INTO PhieuNhap (
            SoPhieu, NgayNhap, NhaCungCap, MaNhanVienNhap, TongTien, TrangThai, GhiChu)
        SELECT 'PN-SEED-THUOC-001', DATEADD(DAY, -30, @Now), N'Nhà cung cấp dược phẩm minh họa',
               'NV009', SUM(SoLuong * DonGia), N'DaXacNhan',
               N'Phiếu nhập mẫu; số tồn lô thể hiện tồn sau cấp phát'
        FROM @PurchaseItems;
    END;

    INSERT INTO ChiTietPhieuNhap (MaPhieuNhap, MaLoThuoc, SoLuong, DonGia, ThanhTien)
    SELECT p.MaPhieuNhap, l.MaLoThuoc, x.SoLuong, x.DonGia, x.SoLuong * x.DonGia
    FROM @PurchaseItems x
    JOIN PhieuNhap p ON p.SoPhieu = 'PN-SEED-THUOC-001'
    JOIN LoThuoc l ON l.MaThuoc = x.MaHang AND l.SoLo = x.SoLo
    WHERE x.LoaiHang = 'THUOC'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietPhieuNhap c
          WHERE c.MaPhieuNhap = p.MaPhieuNhap AND c.MaLoThuoc = l.MaLoThuoc
      );

    INSERT INTO PhieuXuat (
        SoPhieu, NgayXuat, LoaiXuat, MaDonThuoc, MaNhanVienXuat, NguoiNhan, TrangThai, GhiChu)
    SELECT CONCAT('PX-SEED-RX-', CONVERT(VARCHAR(20), d.MaDonThuoc)),
           DATEADD(MINUTE, 15, d.NgayKe), N'CapPhatDonThuoc', d.MaDonThuoc,
           'NV009', v.MaBenhNhan, N'DaXacNhan', N'Cấp phát minh họa theo đơn thuốc'
    FROM DonThuoc d
    JOIN @VisitMap v ON v.MaLuotKham = d.MaLuotKham
    WHERE NOT EXISTS (
        SELECT 1 FROM PhieuXuat x
        WHERE x.SoPhieu = CONCAT('PX-SEED-RX-', CONVERT(VARCHAR(20), d.MaDonThuoc))
    );

    INSERT INTO ChiTietPhieuXuat (MaPhieuXuat, MaLoThuoc, MaChiTietDon, SoLuong, DonGia)
    SELECT x.MaPhieuXuat, l.MaLoThuoc, c.MaChiTietDon, c.SoLuong, l.DonGiaNhap
    FROM ChiTietDonThuoc c
    JOIN DonThuoc d ON d.MaDonThuoc = c.MaDonThuoc
    JOIN PhieuXuat x ON x.MaDonThuoc = d.MaDonThuoc
    JOIN LoThuoc l ON l.MaThuoc = c.MaThuoc
      AND l.SoLo = CASE c.MaThuoc
          WHEN 'THUOC-PARA' THEN 'SEED-PARA-01'
          WHEN 'THUOC-AMOX' THEN 'SEED-AMOX-01'
          ELSE ''
      END
    WHERE NOT EXISTS (
        SELECT 1 FROM ChiTietPhieuXuat y
        WHERE y.MaPhieuXuat = x.MaPhieuXuat AND y.MaChiTietDon = c.MaChiTietDon
    );

    INSERT INTO BangGiaDichVu (TenBangGia, TuNgay, DenNgay, TrangThai)
    SELECT N'Bảng giá tham khảo - dữ liệu mẫu', DATEADD(DAY, -30, @Today),
           DATEADD(YEAR, 1, @Today), N'DangApDung'
    WHERE NOT EXISTS (
        SELECT 1 FROM BangGiaDichVu WHERE TenBangGia = N'Bảng giá tham khảo - dữ liệu mẫu'
    );

    INSERT INTO @ServicePrices VALUES
    ('DV-KHAM', 150000), ('DV-XN-MAU', 120000), ('DV-SIEUAM', 250000),
    ('DV-DIENTIM', 180000), ('DV-XQUANG', 200000);

    INSERT INTO TheBHYT (
        MaBenhNhan, SoThe, HoTenTrenThe, NgaySinhTrenThe, NoiDangKyKCB,
        TuNgay, DenNgay, MucHuong, TrangThai)
    SELECT p.MaBenhNhan, CONCAT('DEMO-BHYT-', p.MaBenhNhan),
           p.HoTen, p.NgaySinh, N'Bệnh viện đa khoa Thu Cúc',
           DATEADD(YEAR, -1, @Today), DATEADD(YEAR, 1, @Today),
           CAST(80 AS DECIMAL(5,2)), N'HopLe'
    FROM @Patients p
    WHERE p.MaBenhNhan IN ('BN001','BN002','BN003','BN004','BN005','BN006','BN007','BN008')
      AND NOT EXISTS (
          SELECT 1 FROM TheBHYT t WHERE t.SoThe = CONCAT('DEMO-BHYT-', p.MaBenhNhan)
      );

    INSERT INTO ChiTietBangGia (MaBangGia, MaDichVu, DonGia)
    SELECT b.MaBangGia, p.MaDichVu, p.DonGia
    FROM BangGiaDichVu b
    CROSS JOIN @ServicePrices p
    WHERE b.TenBangGia = N'Bảng giá tham khảo - dữ liệu mẫu'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietBangGia c
          WHERE c.MaBangGia = b.MaBangGia AND c.MaDichVu = p.MaDichVu
      );

    INSERT INTO PhieuTinhPhi (
        MaLuotKham, LanTinh, ThoiDiemTinh, TongTruocGiam, TongGiamTru,
        BHYTChiTra, BenhNhanPhaiTra, TrangThai, MaNhanVienTinh)
    SELECT v.MaLuotKham, 1, DATEADD(MINUTE, 45, v.ThoiGianTiepNhan),
           totals.TongTien, 0, totals.BHYT,
           totals.TongTien - totals.BHYT, N'DaXacNhan', 'NV010'
    FROM @VisitMap v
    CROSS APPLY (
        SELECT CAST(
            COALESCE((SELECT SUM(sp.DonGia)
                      FROM PhieuKham pk
                      JOIN ChiDinhDichVu cd ON cd.MaPhieuKham = pk.MaPhieuKham
                      JOIN @ServicePrices sp ON sp.MaDichVu = cd.MaDichVu
                      WHERE pk.MaLuotKham = v.MaLuotKham), 0)
            + COALESCE((SELECT SUM(CASE c.MaThuoc WHEN 'THUOC-PARA' THEN 500 ELSE 1000 END * c.SoLuong)
                        FROM DonThuoc dt
                        JOIN ChiTietDonThuoc c ON c.MaDonThuoc = dt.MaDonThuoc
                        WHERE dt.MaLuotKham = v.MaLuotKham), 0)
            AS DECIMAL(16,2)) AS TongTien
    ) gross
    OUTER APPLY (
        SELECT TOP (1) t.MucHuong
        FROM TheBHYT t
        WHERE t.MaBenhNhan = v.MaBenhNhan AND t.TrangThai = N'HopLe'
        ORDER BY t.DenNgay DESC
    ) coverage
    CROSS APPLY (
        SELECT gross.TongTien,
               CAST(ROUND(gross.TongTien * COALESCE(coverage.MucHuong, 0) / 100.0, 0)
                    AS DECIMAL(16,2)) AS BHYT
    ) totals
    WHERE v.TrangThai = N'HoanTat'
      AND NOT EXISTS (
          SELECT 1 FROM PhieuTinhPhi p
          WHERE p.MaLuotKham = v.MaLuotKham AND p.LanTinh = 1
      );

    INSERT INTO ChiTietPhieuTinhPhi (
        MaPhieuTinhPhi, LoaiKhoan, MaChiDinh, MoTa, SoLuong, DonGia,
        ThanhTien, DuocBHYTChiTra, TyLeBHYT)
    SELECT f.MaPhieuTinhPhi, N'DichVu', cd.MaChiDinh, d.TenDichVu,
           CAST(1 AS DECIMAL(12,2)), sp.DonGia, sp.DonGia,
           CASE WHEN t.MaTheBHYT IS NULL THEN 0 ELSE 1 END,
           CASE WHEN t.MaTheBHYT IS NULL THEN 0 ELSE t.MucHuong END
    FROM PhieuTinhPhi f
    JOIN @VisitMap v ON v.MaLuotKham = f.MaLuotKham
    JOIN PhieuKham pk ON pk.MaLuotKham = v.MaLuotKham
    JOIN ChiDinhDichVu cd ON cd.MaPhieuKham = pk.MaPhieuKham
    JOIN DichVu d ON d.MaDichVu = cd.MaDichVu
    JOIN @ServicePrices sp ON sp.MaDichVu = cd.MaDichVu
    LEFT JOIN TheBHYT t ON t.MaBenhNhan = v.MaBenhNhan AND t.TrangThai = N'HopLe'
    WHERE f.LanTinh = 1 AND v.TrangThai = N'HoanTat'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietPhieuTinhPhi x
          WHERE x.MaPhieuTinhPhi = f.MaPhieuTinhPhi AND x.MaChiDinh = cd.MaChiDinh
      );

    INSERT INTO ChiTietPhieuTinhPhi (
        MaPhieuTinhPhi, LoaiKhoan, MaChiTietDon, MoTa, SoLuong, DonGia,
        ThanhTien, DuocBHYTChiTra, TyLeBHYT)
    SELECT f.MaPhieuTinhPhi, N'Thuoc', c.MaChiTietDon, t.TenThuoc,
           c.SoLuong,
           CASE c.MaThuoc WHEN 'THUOC-PARA' THEN 500 ELSE 1000 END,
           c.SoLuong * CASE c.MaThuoc WHEN 'THUOC-PARA' THEN 500 ELSE 1000 END,
           CASE WHEN bhyt.MaTheBHYT IS NULL THEN 0 ELSE 1 END,
           CASE WHEN bhyt.MaTheBHYT IS NULL THEN 0 ELSE bhyt.MucHuong END
    FROM PhieuTinhPhi f
    JOIN @VisitMap v ON v.MaLuotKham = f.MaLuotKham
    JOIN DonThuoc d ON d.MaLuotKham = v.MaLuotKham
    JOIN ChiTietDonThuoc c ON c.MaDonThuoc = d.MaDonThuoc
    JOIN Thuoc t ON t.MaThuoc = c.MaThuoc
    LEFT JOIN TheBHYT bhyt ON bhyt.MaBenhNhan = v.MaBenhNhan AND bhyt.TrangThai = N'HopLe'
    WHERE f.LanTinh = 1 AND v.TrangThai = N'HoanTat'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietPhieuTinhPhi x
          WHERE x.MaPhieuTinhPhi = f.MaPhieuTinhPhi AND x.MaChiTietDon = c.MaChiTietDon
      );

    INSERT INTO HoaDon (
        SoHoaDon, MaPhieuTinhPhi, NgayLap, TongTien, SoTienBHYT, SoTienPhaiTra,
        TrangThai, MaThuNgan)
    SELECT CONCAT('HD-SEED-', CONVERT(VARCHAR(20), f.MaPhieuTinhPhi)),
           f.MaPhieuTinhPhi, DATEADD(MINUTE, 50, v.ThoiGianTiepNhan),
           f.TongTruocGiam - f.TongGiamTru, f.BHYTChiTra, f.BenhNhanPhaiTra,
           CASE WHEN v.MaBenhNhan IN ('BN001','BN002','BN003','BN004','BN005',
                                      'BN006','BN007','BN008','BN009','BN010')
                THEN N'DaThanhToan' ELSE N'ChuaThanhToan' END,
           'NV010'
    FROM PhieuTinhPhi f
    JOIN @VisitMap v ON v.MaLuotKham = f.MaLuotKham
    WHERE f.LanTinh = 1 AND v.TrangThai = N'HoanTat'
      AND NOT EXISTS (
          SELECT 1 FROM HoaDon h WHERE h.MaPhieuTinhPhi = f.MaPhieuTinhPhi
      );

    INSERT INTO PhuongThucThanhToan (MaPhuongThuc, TenPhuongThuc, TrangThai)
    SELECT v.MaPhuongThuc, v.TenPhuongThuc, N'HoatDong'
    FROM (VALUES
        ('PT-TIENMAT', N'Tiền mặt'),
        ('PT-CHUYENKHOAN', N'Chuyển khoản ngân hàng'),
        ('PT-THE', N'Thẻ ngân hàng')
    ) v(MaPhuongThuc, TenPhuongThuc)
    WHERE NOT EXISTS (SELECT 1 FROM PhuongThucThanhToan p WHERE p.MaPhuongThuc = v.MaPhuongThuc);

    INSERT INTO GiaoDichThanhToan (
        MaHoaDon, MaPhuongThuc, MaThamChieu, ThoiGianGiaoDich,
        SoTien, TrangThai, NguoiThucHien, GhiChu)
    SELECT h.MaHoaDon,
           CASE WHEN v.MaBenhNhan IN ('BN002','BN004','BN006','BN008','BN010')
                THEN 'PT-CHUYENKHOAN' ELSE 'PT-TIENMAT' END,
           CONCAT('SEED-GD-', CONVERT(VARCHAR(20), h.MaHoaDon)),
           DATEADD(MINUTE, 55, v.ThoiGianTiepNhan),
           h.SoTienPhaiTra, N'ThanhCong', 'NV010', N'Thanh toán đủ số tiền bệnh nhân phải trả'
    FROM HoaDon h
    JOIN PhieuTinhPhi f ON f.MaPhieuTinhPhi = h.MaPhieuTinhPhi
    JOIN @VisitMap v ON v.MaLuotKham = f.MaLuotKham
    WHERE h.TrangThai = N'DaThanhToan'
      AND NOT EXISTS (
          SELECT 1 FROM GiaoDichThanhToan g
          WHERE g.MaThamChieu = CONCAT('SEED-GD-', CONVERT(VARCHAR(20), h.MaHoaDon))
      );

    INSERT INTO LichSuXacThucBHYT (
        MaTheBHYT, MaLuotKham, ThoiDiem, NguoiThucHien,
        NguonTraCuu, MaThamChieuNguon, KetQua, NoiDungPhanHoi, GhiChu)
    SELECT t.MaTheBHYT, v.MaLuotKham, DATEADD(MINUTE, 5, v.ThoiGianTiepNhan),
           'NV002', N'Cổng tra cứu minh họa', CONCAT('SEED-BHYT-', t.SoThe),
           N'HopLe', N'{"ketQua":"HopLe","duLieu":"minh_hoa"}', N'Dữ liệu mô phỏng; không tra cứu cổng thật'
    FROM TheBHYT t
    JOIN @VisitMap v ON v.MaBenhNhan = t.MaBenhNhan
    WHERE t.SoThe LIKE 'DEMO-BHYT-%' AND v.TrangThai = N'HoanTat'
      AND NOT EXISTS (
          SELECT 1 FROM LichSuXacThucBHYT x
          WHERE x.MaTheBHYT = t.MaTheBHYT AND x.MaLuotKham = v.MaLuotKham
            AND x.MaThamChieuNguon = CONCAT('SEED-BHYT-', t.SoThe)
      );

    INSERT INTO LoaiThietBi (MaLoaiThietBi, TenLoaiThietBi, MoTa)
    SELECT v.MaLoaiThietBi, v.TenLoaiThietBi, v.MoTa
    FROM (VALUES
        ('TB-CHANDOAN', N'Thiết bị chẩn đoán', N'Thiết bị hỗ trợ khám và chẩn đoán'),
        ('TB-XETNGHIEM', N'Thiết bị xét nghiệm', N'Thiết bị xét nghiệm cận lâm sàng'),
        ('TB-THEODOI', N'Thiết bị theo dõi', N'Thiết bị theo dõi dấu hiệu sinh tồn')
    ) v(MaLoaiThietBi, TenLoaiThietBi, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM LoaiThietBi l WHERE l.MaLoaiThietBi = v.MaLoaiThietBi);

    INSERT INTO ThietBi (
        MaThietBi, TenThietBi, MaLoaiThietBi, Model, SoSerial, HangSanXuat,
        NgayMua, NgayDuaVaoSuDung, ViTri, TinhTrang)
    SELECT v.MaThietBi, v.TenThietBi, v.MaLoaiThietBi, v.Model, v.SoSerial,
           v.HangSanXuat, DATEADD(YEAR, -2, @Today), DATEADD(YEAR, -2, @Today),
           v.ViTri, v.TinhTrang
    FROM (VALUES
        ('TB-SEED-001', N'Máy siêu âm tổng quát', 'TB-CHANDOAN', 'US-300', 'SERIAL-DEMO-001', N'GE Healthcare', N'Phòng siêu âm 1', N'Tot'),
        ('TB-SEED-002', N'Máy xét nghiệm huyết học', 'TB-XETNGHIEM', 'XN-500', 'SERIAL-DEMO-002', N'Sysmex', N'Phòng xét nghiệm', N'DangSuDung'),
        ('TB-SEED-003', N'Máy điện tâm đồ 12 cần', 'TB-THEODOI', 'ECG-12', 'SERIAL-DEMO-003', N'Edan', N'Phòng khám tim mạch', N'CanBaoTri')
    ) v(MaThietBi, TenThietBi, MaLoaiThietBi, Model, SoSerial, HangSanXuat, ViTri, TinhTrang)
    WHERE NOT EXISTS (SELECT 1 FROM ThietBi t WHERE t.MaThietBi = v.MaThietBi);

    INSERT INTO LichBaoDuong (
        MaThietBi, NgayDuKien, NgayThucHien, NoiDung, DonViThucHien,
        MaNhanVienPhuTrach, ChiPhi, TrangThai)
    SELECT v.MaThietBi, DATEADD(DAY, v.NgayOffset, @Today),
           CASE WHEN v.TrangThai = N'DaThucHien' THEN DATEADD(DAY, v.NgayOffset, @Today) ELSE NULL END,
           v.NoiDung, N'Đơn vị bảo trì minh họa', 'NV011', v.ChiPhi, v.TrangThai
    FROM (VALUES
        ('TB-SEED-001', -30, N'Kiểm tra đầu dò và hiệu chuẩn', CAST(1200000 AS DECIMAL(14,2)), N'DaThucHien'),
        ('TB-SEED-002', 30, N'Bảo dưỡng định kỳ và kiểm chuẩn', CAST(1800000 AS DECIMAL(14,2)), N'LenLich'),
        ('TB-SEED-003', 7, N'Kiểm tra dây đo và điện cực', CAST(500000 AS DECIMAL(14,2)), N'LenLich')
    ) v(MaThietBi, NgayOffset, NoiDung, ChiPhi, TrangThai)
    WHERE NOT EXISTS (
        SELECT 1 FROM LichBaoDuong b
        WHERE b.MaThietBi = v.MaThietBi
          AND b.NgayDuKien = DATEADD(DAY, v.NgayOffset, @Today)
          AND b.NoiDung = v.NoiDung
    );

    INSERT INTO SuaChuaThietBi (
        MaThietBi, NgayBaoHong, MoTaSuCo, NgaySua, DonViSuaChua,
        ChiPhi, TrangThai, MaNhanVienTiepNhan, GhiChu)
    SELECT 'TB-SEED-003', DATEADD(DAY, -5, @Now),
           N'Dây điện cực có dấu hiệu tiếp xúc không ổn định', NULL,
           N'Đơn vị bảo trì minh họa', 0, N'DangXuLy', 'NV011',
           N'Không sử dụng thiết bị cho đến khi kiểm tra xong'
    WHERE NOT EXISTS (
        SELECT 1 FROM SuaChuaThietBi s
        WHERE s.MaThietBi = 'TB-SEED-003'
          AND s.MoTaSuCo = N'Dây điện cực có dấu hiệu tiếp xúc không ổn định'
    );

    INSERT INTO DanhGiaNhanVien (MaNhanVien, NguoiDanhGia, KyDanhGia, NgayDanhGia, Diem, NhanXet)
    SELECT v.MaNhanVien, 'NV011', @ReviewPeriod, DATEADD(DAY, -30, @Today),
           v.Diem, v.NhanXet
    FROM (VALUES
        ('NV002', CAST(4.70 AS DECIMAL(5,2)), N'Tiếp đón chu đáo, phối hợp lịch hẹn tốt'),
        ('NV003', CAST(4.80 AS DECIMAL(5,2)), N'Tư vấn rõ ràng, hồ sơ khám đầy đủ'),
        ('NV004', CAST(4.60 AS DECIMAL(5,2)), N'Tuân thủ quy trình chuyên môn'),
        ('NV009', CAST(4.50 AS DECIMAL(5,2)), N'Quản lý chứng từ và tồn kho cẩn thận'),
        ('NV010', CAST(4.75 AS DECIMAL(5,2)), N'Đối soát viện phí chính xác')
    ) v(MaNhanVien, Diem, NhanXet)
    WHERE NOT EXISTS (
        SELECT 1 FROM DanhGiaNhanVien d
        WHERE d.MaNhanVien = v.MaNhanVien AND d.KyDanhGia = @ReviewPeriod
    );

    INSERT INTO NhatKyHeThong (MaTaiKhoan, ThoiDiem, HanhDong, DoiTuong, MaDoiTuong, DiaChiIP, NoiDung)
    SELECT a.MaTaiKhoan, DATEADD(MINUTE, -i.SoPhut, @Now),
           i.HanhDong, i.DoiTuong, i.MaDoiTuong, '127.0.0.1', i.NoiDung
    FROM (VALUES
        ('admin', 120, N'DANG_NHAP', N'TaiKhoan', N'admin', N'Đăng nhập quản trị minh họa'),
        ('letan.huong', 90, N'TAO_LICH_HEN', N'LichHen', N'SEED', N'Tạo lịch hẹn dữ liệu mẫu'),
        ('bacsi.thanh', 60, N'HOAN_TAT_PHIEU_KHAM', N'PhieuKham', N'SEED', N'Hoàn tất phiếu khám dữ liệu mẫu'),
        ('kho.phuc', 45, N'XAC_NHAN_PHIEU_NHAP', N'PhieuNhap', N'PN-SEED-THUOC-001', N'Ghi nhận phiếu nhập minh họa'),
        ('thungan.van', 30, N'THU_TIEN', N'HoaDon', N'SEED', N'Ghi nhận thanh toán mẫu')
    ) i(TenDangNhap, SoPhut, HanhDong, DoiTuong, MaDoiTuong, NoiDung)
    JOIN TaiKhoan a ON a.TenDangNhap = i.TenDangNhap
    WHERE NOT EXISTS (
        SELECT 1 FROM NhatKyHeThong n
        WHERE n.MaTaiKhoan = a.MaTaiKhoan AND n.HanhDong = i.HanhDong
          AND n.DoiTuong = i.DoiTuong AND n.MaDoiTuong = i.MaDoiTuong
          AND n.NoiDung = i.NoiDung
    );

    INSERT INTO ThongBao (
        MaNhanVien, MaBenhNhan, TieuDe, NoiDung, LoaiThongBao,
        ThoiGianGui, TrangThai, KenhGui)
    SELECT v.MaNhanVien, v.MaBenhNhan, v.TieuDe, v.NoiDung, v.LoaiThongBao,
           DATEADD(MINUTE, -v.SoPhut, @Now), N'DaGui', N'Ứng dụng'
    FROM (VALUES
        ('NV002', NULL, N'Lịch hẹn cần xác nhận', N'Có lịch hẹn mới đang chờ lễ tân xác nhận.', N'LichHen', 20),
        ('NV003', NULL, N'Lịch khám trong ngày', N'Vui lòng kiểm tra danh sách lịch khám hôm nay.', N'NhacViec', 15),
        ('NV009', NULL, N'Kiểm tra hạn dùng thuốc', N'Vui lòng rà soát định kỳ các lô thuốc trong kho.', N'Kho', 10),
        ('NV010', NULL, N'Đối soát giao dịch', N'Có hóa đơn mẫu đã thanh toán cần đối soát.', N'ThanhToan', 5),
        (NULL, 'BN001', N'Nhắc lịch tái khám', N'Đây là thông báo minh họa; vui lòng liên hệ cơ sở để xác nhận lịch.', N'ChamSoc', 3)
    ) v(MaNhanVien, MaBenhNhan, TieuDe, NoiDung, LoaiThongBao, SoPhut)
    WHERE NOT EXISTS (
        SELECT 1 FROM ThongBao t
        WHERE ISNULL(t.MaNhanVien, '') = ISNULL(v.MaNhanVien, '')
          AND ISNULL(t.MaBenhNhan, '') = ISNULL(v.MaBenhNhan, '')
          AND t.TieuDe = v.TieuDe
    );

    INSERT INTO HoaChat (MaHoaChat, TenHoaChat, NongDo, DonViTinh, HangSanXuat, MoTa, TrangThai)
    SELECT v.MaHoaChat, v.TenHoaChat, v.NongDo, v.DonViTinh, v.HangSanXuat, v.MoTa, N'DangSuDung'
    FROM (VALUES
        ('HC-NACl', N'Natri clorid', N'0,9%', N'Chai', N'Bình Định Pharma', N'Dung dịch dùng cho xét nghiệm và vệ sinh'),
        ('HC-EDTA', N'Ống chống đông EDTA', N'K2EDTA', N'Ống', N'Greiner Bio-One', N'Vật tư lấy mẫu huyết học'),
        ('HC-THUOCNHUOM', N'Dung dịch nhuộm tiêu bản', N'Theo hướng dẫn NSX', N'Lít', N'Merck', N'Hóa chất xét nghiệm minh họa')
    ) v(MaHoaChat, TenHoaChat, NongDo, DonViTinh, HangSanXuat, MoTa)
    WHERE NOT EXISTS (SELECT 1 FROM HoaChat h WHERE h.MaHoaChat = v.MaHoaChat);

    INSERT INTO LoHoaChat (
        MaHoaChat, SoLo, NgaySanXuat, HanSuDung, SoLuongTon, ViTriLuuTru, TrangThai)
    SELECT v.MaHoaChat, v.SoLo, DATEADD(MONTH, -6, @Today), DATEADD(YEAR, 1, @Today),
           99, v.ViTri, N'DangTon'
    FROM (VALUES
        ('HC-NACl', 'SEED-NACL-01', N'Kho hóa chất B-01'),
        ('HC-EDTA', 'SEED-EDTA-01', N'Kho vật tư B-02'),
        ('HC-THUOCNHUOM', 'SEED-NHUOM-01', N'Tủ hóa chất B-03')
    ) v(MaHoaChat, SoLo, ViTri)
    WHERE NOT EXISTS (
        SELECT 1 FROM LoHoaChat l WHERE l.MaHoaChat = v.MaHoaChat AND l.SoLo = v.SoLo
    );

    INSERT INTO @PurchaseItems VALUES
    ('HOACHAT', 'HC-NACl', 'SEED-NACL-01', 100, 12000),
    ('HOACHAT', 'HC-EDTA', 'SEED-EDTA-01', 100, 8000),
    ('HOACHAT', 'HC-THUOCNHUOM', 'SEED-NHUOM-01', 100, 250000);

    UPDATE PhieuNhap
    SET TongTien = (
        SELECT SUM(SoLuong * DonGia) FROM @PurchaseItems
    )
    WHERE SoPhieu = 'PN-SEED-THUOC-001';

    INSERT INTO ChiTietNhapHoaChat (MaPhieuNhap, MaLoHoaChat, SoLuong, DonGia, ThanhTien)
    SELECT p.MaPhieuNhap, l.MaLoHoaChat, x.SoLuong, x.DonGia, x.SoLuong * x.DonGia
    FROM @PurchaseItems x
    JOIN PhieuNhap p ON p.SoPhieu = 'PN-SEED-THUOC-001'
    JOIN LoHoaChat l ON l.MaHoaChat = x.MaHang AND l.SoLo = x.SoLo
    WHERE x.LoaiHang = 'HOACHAT'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietNhapHoaChat c
          WHERE c.MaPhieuNhap = p.MaPhieuNhap AND c.MaLoHoaChat = l.MaLoHoaChat
      );

    INSERT INTO PhieuXuat (
        SoPhieu, NgayXuat, LoaiXuat, MaNhanVienXuat, NguoiNhan, TrangThai, GhiChu)
    SELECT 'PX-SEED-HOACHAT-001', DATEADD(DAY, -2, @Now), N'Khac', 'NV009',
           N'Phòng xét nghiệm', N'DaXacNhan', N'Xuất hóa chất minh họa; dùng chung cấu trúc PhieuXuat'
    WHERE NOT EXISTS (SELECT 1 FROM PhieuXuat WHERE SoPhieu = 'PX-SEED-HOACHAT-001');

    INSERT INTO ChiTietXuatHoaChat (MaPhieuXuat, MaLoHoaChat, SoLuong)
    SELECT p.MaPhieuXuat, l.MaLoHoaChat, 1
    FROM PhieuXuat p
    JOIN (VALUES
        ('HC-NACl', 'SEED-NACL-01'),
        ('HC-EDTA', 'SEED-EDTA-01'),
        ('HC-THUOCNHUOM', 'SEED-NHUOM-01')
    ) s(MaHoaChat, SoLo) ON 1 = 1
    JOIN LoHoaChat l ON l.MaHoaChat = s.MaHoaChat AND l.SoLo = s.SoLo
    WHERE p.SoPhieu = 'PX-SEED-HOACHAT-001'
      AND NOT EXISTS (
          SELECT 1 FROM ChiTietXuatHoaChat x
          WHERE x.MaPhieuXuat = p.MaPhieuXuat AND x.MaLoHoaChat = l.MaLoHoaChat
      );

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

SET NOCOUNT ON;

DECLARE @SchemaName SYSNAME;
DECLARE @TableName SYSNAME;
DECLARE @CountSql NVARCHAR(MAX);
DECLARE @QualifiedName NVARCHAR(260);
DECLARE table_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT s.name, t.name
    FROM sys.tables t
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    WHERE t.is_ms_shipped = 0
    ORDER BY s.name, t.name;

OPEN table_cursor;
FETCH NEXT FROM table_cursor INTO @SchemaName, @TableName;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @QualifiedName = QUOTENAME(@SchemaName) + N'.' + QUOTENAME(@TableName);
    SET @CountSql = COALESCE(@CountSql + N' UNION ALL ', N'')
                  + N'SELECT N''' + REPLACE(@QualifiedName, N'''', N'''''')
                  + N''' AS TenBang, COUNT_BIG(*) AS SoBanGhi FROM '
                  + @QualifiedName;
    FETCH NEXT FROM table_cursor INTO @SchemaName, @TableName;
END;
CLOSE table_cursor;
DEALLOCATE table_cursor;

EXEC sys.sp_executesql @CountSql;

SELECT a.TenDangNhap, n.HoTen, r.TenVaiTro, a.TrangThai,
       CASE WHEN a.MatKhauHash LIKE 'pbkdf2-sha256$%'
            THEN N'PBKDF2-HMAC-SHA256'
            ELSE N'Hash chưa được hỗ trợ/cấu hình'
       END AS TrangThaiMatKhau
FROM TaiKhoan a
LEFT JOIN NhanVien n ON n.MaNhanVien = a.MaNhanVien
LEFT JOIN TaiKhoanVaiTro ar ON ar.MaTaiKhoan = a.MaTaiKhoan
LEFT JOIN VaiTro r ON r.MaVaiTro = ar.MaVaiTro
WHERE a.TenDangNhap IN ('admin','letan.huong','bacsi.thanh','kho.phuc','thungan.van','quanly.nam')
ORDER BY a.TenDangNhap;

SELECT b.MaBenhNhan, b.HoTen, b.GioiTinh, b.NgaySinh
FROM BenhNhan b
WHERE b.MaBenhNhan LIKE 'BN0%'
ORDER BY b.MaBenhNhan;

SELECT h.MaLichHen, b.HoTen AS BenhNhan, n.HoTen AS BacSi,
       h.ThoiGianHen, h.TrangThai
FROM LichHen h
JOIN BenhNhan b ON b.MaBenhNhan = h.MaBenhNhan
LEFT JOIN NhanVien n ON n.MaNhanVien = h.MaNhanVien
WHERE b.MaBenhNhan LIKE 'BN0%'
ORDER BY h.ThoiGianHen;

SELECT
    (SELECT COUNT_BIG(*) FROM BacSi WHERE MaNhanVien LIKE 'NV00%') AS SoBacSiSeed,
    (SELECT COUNT_BIG(*) FROM LuotKham l JOIN BenhNhan b ON b.MaBenhNhan = l.MaBenhNhan WHERE b.MaBenhNhan LIKE 'BN0%') AS SoLuotKhamSeed,
    (SELECT COUNT_BIG(*) FROM DonThuoc d JOIN LuotKham l ON l.MaLuotKham = d.MaLuotKham JOIN BenhNhan b ON b.MaBenhNhan = l.MaBenhNhan WHERE b.MaBenhNhan LIKE 'BN0%') AS SoDonThuocSeed,
    (SELECT COUNT_BIG(*) FROM HoaDon h JOIN PhieuTinhPhi p ON p.MaPhieuTinhPhi = h.MaPhieuTinhPhi JOIN LuotKham l ON l.MaLuotKham = p.MaLuotKham JOIN BenhNhan b ON b.MaBenhNhan = l.MaBenhNhan WHERE b.MaBenhNhan LIKE 'BN0%') AS SoHoaDonSeed;

SELECT MaPhieuNhap, SoPhieu, TongTien,
       (SELECT SUM(ThanhTien) FROM ChiTietPhieuNhap c WHERE c.MaPhieuNhap = p.MaPhieuNhap) AS TongChiTiet
FROM PhieuNhap p WHERE SoPhieu = 'PN-SEED-THUOC-001';

SELECT h.SoHoaDon, h.TongTien, h.SoTienBHYT, h.SoTienPhaiTra,
       COALESCE(SUM(CASE WHEN g.TrangThai = N'ThanhCong' THEN g.SoTien ELSE 0 END), 0) AS DaThanhToan
FROM HoaDon h
LEFT JOIN GiaoDichThanhToan g ON g.MaHoaDon = h.MaHoaDon
WHERE h.SoHoaDon LIKE 'HD-SEED-%'
GROUP BY h.SoHoaDon, h.TongTien, h.SoTienBHYT, h.SoTienPhaiTra
ORDER BY h.SoHoaDon;

DBCC CHECKCONSTRAINTS WITH ALL_CONSTRAINTS;

SELECT t.name AS BangChuaCoDuLieu
FROM sys.tables t
WHERE t.is_ms_shipped = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys.partitions p
      WHERE p.object_id = t.object_id AND p.index_id IN (0,1) AND p.rows > 0
  )
ORDER BY t.name;
