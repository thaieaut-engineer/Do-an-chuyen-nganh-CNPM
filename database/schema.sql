-- Create Database
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'QuanLyPhongKhamThuCuc')
BEGIN
    CREATE DATABASE QuanLyPhongKhamThuCuc;
END;
GO

USE QuanLyPhongKhamThuCuc;
GO

-- ============================================================================
-- 1. NHÂN SỰ & BÁC SĨ
-- ============================================================================
CREATE TABLE BoPhan (
    MaBoPhan VARCHAR(20) PRIMARY KEY,
    TenBoPhan NVARCHAR(100) NOT NULL,
    MoTa NVARCHAR(255),
    TrangThai NVARCHAR(20) DEFAULT 'HoatDong' CHECK (TrangThai IN ('HoatDong', 'NgungHoatDong'))
);

CREATE TABLE ChucDanh (
    MaChucDanh VARCHAR(20) PRIMARY KEY,
    TenChucDanh NVARCHAR(100) NOT NULL,
    MoTa NVARCHAR(255)
);

CREATE TABLE ChuyenKhoa (
    MaChuyenKhoa VARCHAR(20) PRIMARY KEY,
    TenChuyenKhoa NVARCHAR(120) NOT NULL,
    MoTa NVARCHAR(255),
    TrangThai NVARCHAR(20) DEFAULT 'HoatDong' CHECK (TrangThai IN ('HoatDong', 'NgungHoatDong'))
);

CREATE TABLE NhanVien (
    MaNhanVien VARCHAR(20) PRIMARY KEY,
    HoTen NVARCHAR(150) NOT NULL,
    NgaySinh DATE,
    GioiTinh NVARCHAR(10) CHECK (GioiTinh IN (N'Nam', N'Nữ', N'Khác')),
    SoDienThoai VARCHAR(20),
    Email VARCHAR(120) UNIQUE,
    DiaChi NVARCHAR(255),
    NgayBatDau DATE NOT NULL,
    NgayKetThuc DATE,
    TrangThaiCongTac NVARCHAR(20) DEFAULT 'DangLam' CHECK (TrangThaiCongTac IN ('ThuViec', 'DangLam', 'TamNghi', 'DaNghi')),
    MaBoPhan VARCHAR(20),
    MaChucDanh VARCHAR(20),
    FOREIGN KEY(MaBoPhan) REFERENCES BoPhan(MaBoPhan),
    FOREIGN KEY(MaChucDanh) REFERENCES ChucDanh(MaChucDanh)
);

-- BacSi la subtype tuy chon cua NhanVien: PK dong thoi la FK.
CREATE TABLE BacSi (
    MaNhanVien VARCHAR(20) PRIMARY KEY,
    HocVi NVARCHAR(80),
    ChuyenMon NVARCHAR(150),
    MaChuyenKhoa VARCHAR(20),
    ChungChiHanhNghe NVARCHAR(80),
    FOREIGN KEY(MaNhanVien) REFERENCES NhanVien(MaNhanVien),
    FOREIGN KEY(MaChuyenKhoa) REFERENCES ChuyenKhoa(MaChuyenKhoa)
);

-- ============================================================================
-- 2. BỆNH NHÂN / LỊCH HẸN / KHÁM
-- ============================================================================
CREATE TABLE BenhNhan (
    MaBenhNhan VARCHAR(20) PRIMARY KEY,
    HoTen NVARCHAR(150) NOT NULL,
    NgaySinh DATE,
    GioiTinh NVARCHAR(10) CHECK (GioiTinh IN (N'Nam', N'Nữ', N'Khác')),
    SoGiayTo VARCHAR(40) UNIQUE,
    SoDienThoai VARCHAR(20),
    Email VARCHAR(120),
    DiaChi NVARCHAR(255),
    NgayTaoHoSo DATETIME2 DEFAULT GETDATE(),
    TrangThai NVARCHAR(20) DEFAULT 'HoatDong' CHECK (TrangThai IN ('HoatDong', 'TamKhoa'))
);

CREATE INDEX IX_BenhNhan_HoTen ON BenhNhan(HoTen);
CREATE INDEX IX_BenhNhan_SoDienThoai ON BenhNhan(SoDienThoai);

CREATE TABLE CaLamViec (
    MaCa VARCHAR(20) PRIMARY KEY,
    TenCa NVARCHAR(80) NOT NULL,
    GioBatDau TIME NOT NULL,
    GioKetThuc TIME NOT NULL,
    ThoiGianNghiPhut INT DEFAULT 0,
    TrangThai NVARCHAR(20) DEFAULT 'HoatDong' CHECK (TrangThai IN ('HoatDong', 'NgungHoatDong'))
);

CREATE TABLE LichPhanCong (
    MaPhanCong BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaNhanVien VARCHAR(20) NOT NULL,
    MaCa VARCHAR(20) NOT NULL,
    NgayLam DATE NOT NULL,
    DiaDiem NVARCHAR(150),
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'ChoDuyet', 'DaXacNhan', 'DaHuy', 'DaKhoa')),
    MaNguoiDuyet VARCHAR(20),
    ThoiDiemDuyet DATETIME2,
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaNhanVien) REFERENCES NhanVien(MaNhanVien),
    FOREIGN KEY(MaCa) REFERENCES CaLamViec(MaCa),
    FOREIGN KEY(MaNguoiDuyet) REFERENCES NhanVien(MaNhanVien),
    CONSTRAINT UQ_LichPhanCong UNIQUE(MaNhanVien, MaCa, NgayLam)
);

CREATE TABLE KhungGioKham (
    MaKhungGio BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaNhanVien VARCHAR(20) NOT NULL, -- Dong nhat MaNhanVien (BacSi)
    NgayKham DATE NOT NULL,
    GioBatDau TIME NOT NULL,
    GioKetThuc TIME NOT NULL,
    SoLuongToiDa INT DEFAULT 1,
    TrangThai NVARCHAR(20) DEFAULT 'Mo' CHECK (TrangThai IN ('Mo', 'Day', 'Khoa')),
    FOREIGN KEY(MaNhanVien) REFERENCES BacSi(MaNhanVien)
);

CREATE INDEX IX_KhungGioKham_NgayKham_MaNhanVien ON KhungGioKham(NgayKham, MaNhanVien);

CREATE TABLE LichHen (
    MaLichHen BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaBenhNhan VARCHAR(20) NOT NULL,
    MaKhungGio BIGINT,
    MaChuyenKhoa VARCHAR(20),
    MaNhanVien VARCHAR(20), -- Dong nhat MaNhanVien (BacSi)
    ThoiGianHen DATETIME2 NOT NULL,
    LyDoKham NVARCHAR(500),
    NgayTao DATETIME2 DEFAULT GETDATE(),
    TrangThai NVARCHAR(20) DEFAULT 'ChoXacNhan' CHECK (TrangThai IN ('ChoXacNhan', 'DaXacNhan', 'DaDen', 'DangKham', 'HoanTat', 'DaHuy', 'VangMat')),
    NguoiTao VARCHAR(20),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaBenhNhan) REFERENCES BenhNhan(MaBenhNhan),
    FOREIGN KEY(MaKhungGio) REFERENCES KhungGioKham(MaKhungGio),
    FOREIGN KEY(MaChuyenKhoa) REFERENCES ChuyenKhoa(MaChuyenKhoa),
    FOREIGN KEY(MaNhanVien) REFERENCES BacSi(MaNhanVien),
    FOREIGN KEY(NguoiTao) REFERENCES NhanVien(MaNhanVien)
);

CREATE INDEX IX_LichHen_ThoiGianHen ON LichHen(ThoiGianHen);
CREATE INDEX IX_LichHen_TrangThai ON LichHen(TrangThai);

CREATE TABLE LichSuLichHen (
    MaLichSu BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaLichHen BIGINT NOT NULL,
    NguoiThayDoi VARCHAR(20),
    ThoiDiem DATETIME2 DEFAULT GETDATE(),
    HanhDong NVARCHAR(40) NOT NULL,
    GiaTriCu NVARCHAR(MAX), -- Dùng NVARCHAR(MAX) để lưu JSON
    GiaTriMoi NVARCHAR(MAX),
    LyDo NVARCHAR(255),
    FOREIGN KEY(MaLichHen) REFERENCES LichHen(MaLichHen),
    FOREIGN KEY(NguoiThayDoi) REFERENCES NhanVien(MaNhanVien)
);

CREATE TABLE LuotKham (
    MaLuotKham BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaBenhNhan VARCHAR(20) NOT NULL,
    MaLichHen BIGINT UNIQUE,
    MaNhanVien VARCHAR(20) NOT NULL, -- Dong nhat MaNhanVien (BacSi)
    ThoiGianTiepNhan DATETIME2 DEFAULT GETDATE(),
    ThoiGianKetThuc DATETIME2,
    TrangThai NVARCHAR(20) DEFAULT 'ChoKham' CHECK (TrangThai IN ('ChoKham', 'DangKham', 'ChoKetQua', 'HoanTat', 'DaHuy')),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaBenhNhan) REFERENCES BenhNhan(MaBenhNhan),
    FOREIGN KEY(MaLichHen) REFERENCES LichHen(MaLichHen),
    FOREIGN KEY(MaNhanVien) REFERENCES BacSi(MaNhanVien)
);

CREATE INDEX IX_LuotKham_MaBenhNhan_TiepNhan ON LuotKham(MaBenhNhan, ThoiGianTiepNhan);

CREATE TABLE PhieuKham (
    MaPhieuKham BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaLuotKham BIGINT NOT NULL UNIQUE,
    MaNhanVien VARCHAR(20) NOT NULL, -- Dong nhat MaNhanVien (BacSi)
    LyDoKham NVARCHAR(MAX),
    TrieuChung NVARCHAR(MAX),
    DauHieuSinhTon NVARCHAR(MAX),
    KetQuaLamSang NVARCHAR(MAX),
    ChanDoan NVARCHAR(MAX),
    HuongDieuTri NVARCHAR(MAX),
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'HoanTat', 'DaKy', 'DaKhoa')),
    ThoiGianLap DATETIME2 DEFAULT GETDATE(),
    ThoiGianKy DATETIME2,
    FOREIGN KEY(MaLuotKham) REFERENCES LuotKham(MaLuotKham),
    FOREIGN KEY(MaNhanVien) REFERENCES BacSi(MaNhanVien)
);

CREATE TABLE DichVu (
    MaDichVu VARCHAR(20) PRIMARY KEY,
    TenDichVu NVARCHAR(150) NOT NULL,
    LoaiDichVu NVARCHAR(80),
    DonViTinh NVARCHAR(30),
    MoTa NVARCHAR(255),
    TrangThai NVARCHAR(20) DEFAULT 'DangApDung' CHECK (TrangThai IN ('DangApDung', 'NgungApDung'))
);

CREATE TABLE ChiDinhDichVu (
    MaChiDinh BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuKham BIGINT NOT NULL,
    MaDichVu VARCHAR(20) NOT NULL,
    MaNhanVienThucHien VARCHAR(20),
    ThoiGianChiDinh DATETIME2 DEFAULT GETDATE(),
    ThoiGianThucHien DATETIME2,
    KetQua NVARCHAR(MAX),
    TrangThai NVARCHAR(20) DEFAULT 'ChoThucHien' CHECK (TrangThai IN ('ChoThucHien', 'DangThucHien', 'HoanTat', 'DaHuy')),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaPhieuKham) REFERENCES PhieuKham(MaPhieuKham),
    FOREIGN KEY(MaDichVu) REFERENCES DichVu(MaDichVu),
    FOREIGN KEY(MaNhanVienThucHien) REFERENCES NhanVien(MaNhanVien)
);

-- ============================================================================
-- 3. THUỐC / ĐƠN THUỐC / KHO THUỐC
-- ============================================================================
CREATE TABLE Thuoc (
    MaThuoc VARCHAR(20) PRIMARY KEY,
    TenThuoc NVARCHAR(180) NOT NULL,
    HoatChat NVARCHAR(180),
    HamLuong NVARCHAR(80),
    DangBaoChe NVARCHAR(80),
    DonViTinh NVARCHAR(30),
    DuongDungMacDinh NVARCHAR(80),
    HangSanXuat NVARCHAR(150),
    NuocSanXuat NVARCHAR(100),
    MoTa NVARCHAR(255),
    TrangThai NVARCHAR(20) DEFAULT 'DangSuDung' CHECK (TrangThai IN ('DangSuDung', 'NgungSuDung'))
);

CREATE INDEX IX_Thuoc_TenThuoc ON Thuoc(TenThuoc);

CREATE TABLE DonThuoc (
    MaDonThuoc BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaLuotKham BIGINT NOT NULL,
    MaNhanVien VARCHAR(20) NOT NULL, -- Dong nhat MaNhanVien (BacSi)
    NgayKe DATETIME2 DEFAULT GETDATE(),
    LoiDan NVARCHAR(MAX),
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'DaKy', 'DaGuiKho', 'DaCapPhat', 'Huy')),
    FOREIGN KEY(MaLuotKham) REFERENCES LuotKham(MaLuotKham),
    FOREIGN KEY(MaNhanVien) REFERENCES BacSi(MaNhanVien)
);

CREATE TABLE ChiTietDonThuoc (
    MaChiTietDon BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaDonThuoc BIGINT NOT NULL,
    MaThuoc VARCHAR(20) NOT NULL,
    LieuDung NVARCHAR(150) NOT NULL,
    DuongDung NVARCHAR(80),
    TanSuat NVARCHAR(100),
    ThoiGianDung NVARCHAR(100),
    SoLuong DECIMAL(12,2) NOT NULL,
    DonViTinh NVARCHAR(30),
    HuongDan NVARCHAR(MAX),
    FOREIGN KEY(MaDonThuoc) REFERENCES DonThuoc(MaDonThuoc),
    FOREIGN KEY(MaThuoc) REFERENCES Thuoc(MaThuoc)
);

CREATE TABLE LoThuoc (
    MaLoThuoc BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaThuoc VARCHAR(20) NOT NULL,
    SoLo VARCHAR(80) NOT NULL,
    NgaySanXuat DATE,
    HanSuDung DATE NOT NULL,
    DonGiaNhap DECIMAL(14,2) DEFAULT 0,
    SoLuongTon DECIMAL(14,2) DEFAULT 0,
    ViTriLuuTru NVARCHAR(100),
    TrangThai NVARCHAR(20) DEFAULT 'DangTon' CHECK (TrangThai IN ('DangTon', 'HetHang', 'HetHan', 'BiKhoa')),
    FOREIGN KEY(MaThuoc) REFERENCES Thuoc(MaThuoc),
    CONSTRAINT UQ_LoThuoc UNIQUE(MaThuoc, SoLo)
);

CREATE INDEX IX_LoThuoc_HanSuDung ON LoThuoc(HanSuDung);

CREATE TABLE PhieuNhap (
    MaPhieuNhap BIGINT IDENTITY(1,1) PRIMARY KEY,
    SoPhieu VARCHAR(30) NOT NULL UNIQUE,
    NgayNhap DATETIME2 DEFAULT GETDATE(),
    NhaCungCap NVARCHAR(180),
    MaNhanVienNhap VARCHAR(20) NOT NULL,
    TongTien DECIMAL(16,2) DEFAULT 0,
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'DaXacNhan', 'DaHuy')),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaNhanVienNhap) REFERENCES NhanVien(MaNhanVien)
);

CREATE TABLE ChiTietPhieuNhap (
    MaChiTietNhap BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuNhap BIGINT NOT NULL,
    MaLoThuoc BIGINT NOT NULL,
    SoLuong DECIMAL(14,2) NOT NULL,
    DonGia DECIMAL(14,2) NOT NULL,
    ThanhTien DECIMAL(16,2) NOT NULL,
    FOREIGN KEY(MaPhieuNhap) REFERENCES PhieuNhap(MaPhieuNhap),
    FOREIGN KEY(MaLoThuoc) REFERENCES LoThuoc(MaLoThuoc)
);

CREATE TABLE PhieuXuat (
    MaPhieuXuat BIGINT IDENTITY(1,1) PRIMARY KEY,
    SoPhieu VARCHAR(30) NOT NULL UNIQUE,
    NgayXuat DATETIME2 DEFAULT GETDATE(),
    LoaiXuat NVARCHAR(30) DEFAULT 'CapPhatDonThuoc' CHECK (LoaiXuat IN ('CapPhatDonThuoc', 'DieuChuyen', 'HuyHong', 'Khac')),
    MaDonThuoc BIGINT,
    MaNhanVienXuat VARCHAR(20) NOT NULL,
    NguoiNhan NVARCHAR(150),
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'DaXacNhan', 'DaHuy')),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaDonThuoc) REFERENCES DonThuoc(MaDonThuoc),
    FOREIGN KEY(MaNhanVienXuat) REFERENCES NhanVien(MaNhanVien)
);

CREATE TABLE ChiTietPhieuXuat (
    MaChiTietXuat BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuXuat BIGINT NOT NULL,
    MaLoThuoc BIGINT NOT NULL,
    MaChiTietDon BIGINT,
    SoLuong DECIMAL(14,2) NOT NULL,
    DonGia DECIMAL(14,2) DEFAULT 0,
    FOREIGN KEY(MaPhieuXuat) REFERENCES PhieuXuat(MaPhieuXuat),
    FOREIGN KEY(MaLoThuoc) REFERENCES LoThuoc(MaLoThuoc),
    FOREIGN KEY(MaChiTietDon) REFERENCES ChiTietDonThuoc(MaChiTietDon)
);

-- ============================================================================
-- 4. BẢNG GIÁ / VIỆN PHÍ / HÓA ĐƠN / GIAO DỊCH
-- ============================================================================
CREATE TABLE BangGiaDichVu (
    MaBangGia BIGINT IDENTITY(1,1) PRIMARY KEY,
    TenBangGia NVARCHAR(120) NOT NULL,
    TuNgay DATE NOT NULL,
    DenNgay DATE,
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'DangApDung', 'HetHieuLuc'))
);

CREATE TABLE ChiTietBangGia (
    MaChiTietBangGia BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaBangGia BIGINT NOT NULL,
    MaDichVu VARCHAR(20) NOT NULL,
    DonGia DECIMAL(14,2) NOT NULL,
    FOREIGN KEY(MaBangGia) REFERENCES BangGiaDichVu(MaBangGia),
    FOREIGN KEY(MaDichVu) REFERENCES DichVu(MaDichVu),
    CONSTRAINT UQ_ChiTietBangGia UNIQUE(MaBangGia, MaDichVu)
);

CREATE TABLE PhieuTinhPhi (
    MaPhieuTinhPhi BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaLuotKham BIGINT NOT NULL,
    LanTinh INT DEFAULT 1,
    ThoiDiemTinh DATETIME2 DEFAULT GETDATE(),
    TongTruocGiam DECIMAL(16,2) DEFAULT 0,
    TongGiamTru DECIMAL(16,2) DEFAULT 0,
    BHYTChiTra DECIMAL(16,2) DEFAULT 0,
    BenhNhanPhaiTra DECIMAL(16,2) DEFAULT 0,
    TrangThai NVARCHAR(20) DEFAULT 'TamTinh' CHECK (TrangThai IN ('TamTinh', 'DaXacNhan', 'Huy')),
    MaNhanVienTinh VARCHAR(20),
    FOREIGN KEY(MaLuotKham) REFERENCES LuotKham(MaLuotKham),
    FOREIGN KEY(MaNhanVienTinh) REFERENCES NhanVien(MaNhanVien),
    CONSTRAINT UQ_PhieuTinhPhi UNIQUE(MaLuotKham, LanTinh)
);

CREATE TABLE ChiTietPhieuTinhPhi (
    MaChiTietPhi BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuTinhPhi BIGINT NOT NULL,
    LoaiKhoan NVARCHAR(20) NOT NULL CHECK (LoaiKhoan IN ('DichVu', 'Thuoc', 'Khac')),
    MaChiDinh BIGINT,
    MaChiTietDon BIGINT,
    MoTa NVARCHAR(255) NOT NULL,
    SoLuong DECIMAL(12,2) DEFAULT 1,
    DonGia DECIMAL(14,2) NOT NULL,
    ThanhTien DECIMAL(16,2) NOT NULL,
    DuocBHYTChiTra BIT DEFAULT 0,
    TyLeBHYT DECIMAL(5,2) DEFAULT 0,
    FOREIGN KEY(MaPhieuTinhPhi) REFERENCES PhieuTinhPhi(MaPhieuTinhPhi),
    FOREIGN KEY(MaChiDinh) REFERENCES ChiDinhDichVu(MaChiDinh),
    FOREIGN KEY(MaChiTietDon) REFERENCES ChiTietDonThuoc(MaChiTietDon)
);

CREATE TABLE HoaDon (
    MaHoaDon BIGINT IDENTITY(1,1) PRIMARY KEY,
    SoHoaDon VARCHAR(40) NOT NULL UNIQUE,
    MaPhieuTinhPhi BIGINT NOT NULL,
    NgayLap DATETIME2 DEFAULT GETDATE(),
    TongTien DECIMAL(16,2) NOT NULL,
    SoTienBHYT DECIMAL(16,2) DEFAULT 0,
    SoTienPhaiTra DECIMAL(16,2) NOT NULL,
    TrangThai NVARCHAR(30) DEFAULT 'ChuaThanhToan' CHECK (TrangThai IN ('ChuaThanhToan', 'ThanhToanMotPhan', 'DaThanhToan', 'DaHuy', 'DaHoanTien')),
    MaThuNgan VARCHAR(20) NOT NULL,
    FOREIGN KEY(MaPhieuTinhPhi) REFERENCES PhieuTinhPhi(MaPhieuTinhPhi),
    FOREIGN KEY(MaThuNgan) REFERENCES NhanVien(MaNhanVien)
);

CREATE TABLE PhuongThucThanhToan (
    MaPhuongThuc VARCHAR(20) PRIMARY KEY,
    TenPhuongThuc NVARCHAR(80) NOT NULL,
    TrangThai NVARCHAR(20) DEFAULT 'HoatDong' CHECK (TrangThai IN ('HoatDong', 'NgungHoatDong'))
);

CREATE TABLE GiaoDichThanhToan (
    MaGiaoDich BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaHoaDon BIGINT NOT NULL,
    MaPhuongThuc VARCHAR(20) NOT NULL,
    MaThamChieu VARCHAR(100) UNIQUE,
    ThoiGianGiaoDich DATETIME2 DEFAULT GETDATE(),
    SoTien DECIMAL(16,2) NOT NULL,
    TrangThai NVARCHAR(20) DEFAULT 'DangXuLy' CHECK (TrangThai IN ('DangXuLy', 'ThanhCong', 'ThatBai', 'DaHoan', 'DaHuy')),
    NguoiThucHien VARCHAR(20),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaHoaDon) REFERENCES HoaDon(MaHoaDon),
    FOREIGN KEY(MaPhuongThuc) REFERENCES PhuongThucThanhToan(MaPhuongThuc),
    FOREIGN KEY(NguoiThucHien) REFERENCES NhanVien(MaNhanVien)
);

CREATE INDEX IX_GiaoDichThanhToan_ThoiGian ON GiaoDichThanhToan(ThoiGianGiaoDich);

-- ============================================================================
-- 5. BẢO HIỂM Y TẾ (BHYT)
-- ============================================================================
CREATE TABLE TheBHYT (
    MaTheBHYT BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaBenhNhan VARCHAR(20) NOT NULL,
    SoThe VARCHAR(50) NOT NULL UNIQUE,
    HoTenTrenThe NVARCHAR(150),
    NgaySinhTrenThe DATE,
    NoiDangKyKCB NVARCHAR(180),
    TuNgay DATE,
    DenNgay DATE,
    MucHuong DECIMAL(5,2),
    TrangThai NVARCHAR(20) DEFAULT 'ChuaXacThuc' CHECK (TrangThai IN ('ChuaXacThuc', 'HopLe', 'HetHan', 'KhongHopLe', 'ChoXacMinh')),
    FOREIGN KEY(MaBenhNhan) REFERENCES BenhNhan(MaBenhNhan)
);

CREATE TABLE LichSuXacThucBHYT (
    MaXacThuc BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaTheBHYT BIGINT NOT NULL,
    MaLuotKham BIGINT,
    ThoiDiem DATETIME2 DEFAULT GETDATE(),
    NguoiThucHien VARCHAR(20),
    NguonTraCuu NVARCHAR(120),
    MaThamChieuNguon VARCHAR(100),
    KetQua NVARCHAR(30) NOT NULL CHECK (KetQua IN ('HopLe', 'KhongHopLe', 'HetHan', 'KhongTimThay', 'ChoXacMinh', 'LoiKetNoi')),
    NoiDungPhanHoi NVARCHAR(MAX), -- Dùng NVARCHAR(MAX) để lưu JSON
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaTheBHYT) REFERENCES TheBHYT(MaTheBHYT),
    FOREIGN KEY(MaLuotKham) REFERENCES LuotKham(MaLuotKham),
    FOREIGN KEY(NguoiThucHien) REFERENCES NhanVien(MaNhanVien)
);

-- ============================================================================
-- 6. THIẾT BỊ & BẢO TRÌ
-- ============================================================================
CREATE TABLE LoaiThietBi (
    MaLoaiThietBi VARCHAR(20) PRIMARY KEY,
    TenLoaiThietBi NVARCHAR(120) NOT NULL,
    MoTa NVARCHAR(255)
);

CREATE TABLE ThietBi (
    MaThietBi VARCHAR(20) PRIMARY KEY,
    TenThietBi NVARCHAR(150) NOT NULL,
    MaLoaiThietBi VARCHAR(20),
    Model VARCHAR(100),
    SoSerial VARCHAR(100) UNIQUE,
    HangSanXuat NVARCHAR(120),
    NgayMua DATE,
    NgayDuaVaoSuDung DATE,
    ViTri NVARCHAR(150),
    TinhTrang NVARCHAR(20) DEFAULT 'Tot' CHECK (TinhTrang IN ('Tot', 'DangSuDung', 'CanBaoTri', 'DangSuaChua', 'NgungSuDung', 'ThanhLy')),
    FOREIGN KEY(MaLoaiThietBi) REFERENCES LoaiThietBi(MaLoaiThietBi)
);

CREATE TABLE LichBaoDuong (
    MaBaoDuong BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaThietBi VARCHAR(20) NOT NULL,
    NgayDuKien DATE NOT NULL,
    NgayThucHien DATE,
    NoiDung NVARCHAR(255),
    DonViThucHien NVARCHAR(150),
    MaNhanVienPhuTrach VARCHAR(20),
    ChiPhi DECIMAL(14,2) DEFAULT 0,
    TrangThai NVARCHAR(20) DEFAULT 'LenLich' CHECK (TrangThai IN ('LenLich', 'DaThucHien', 'QuaHan', 'Huy')),
    FOREIGN KEY(MaThietBi) REFERENCES ThietBi(MaThietBi),
    FOREIGN KEY(MaNhanVienPhuTrach) REFERENCES NhanVien(MaNhanVien)
);

CREATE TABLE SuaChuaThietBi (
    MaSuaChua BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaThietBi VARCHAR(20) NOT NULL,
    NgayBaoHong DATETIME2 DEFAULT GETDATE(),
    MoTaSuCo NVARCHAR(MAX) NOT NULL,
    NgaySua DATE,
    DonViSuaChua NVARCHAR(150),
    ChiPhi DECIMAL(14,2) DEFAULT 0,
    TrangThai NVARCHAR(20) DEFAULT 'MoiBao' CHECK (TrangThai IN ('MoiBao', 'DangXuLy', 'DaSua', 'KhongTheSua', 'Huy')),
    MaNhanVienTiepNhan VARCHAR(20),
    GhiChu NVARCHAR(255),
    FOREIGN KEY(MaThietBi) REFERENCES ThietBi(MaThietBi),
    FOREIGN KEY(MaNhanVienTiepNhan) REFERENCES NhanVien(MaNhanVien)
);

-- ============================================================================
-- 7. ĐÁNH GIÁ NHÂN VIÊN
-- ============================================================================
CREATE TABLE DanhGiaNhanVien (
    MaDanhGia BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaNhanVien VARCHAR(20) NOT NULL,
    NguoiDanhGia VARCHAR(20) NOT NULL,
    KyDanhGia NVARCHAR(30) NOT NULL,
    NgayDanhGia DATE NOT NULL,
    Diem DECIMAL(5,2),
    NhanXet NVARCHAR(MAX),
    FOREIGN KEY(MaNhanVien) REFERENCES NhanVien(MaNhanVien),
    FOREIGN KEY(NguoiDanhGia) REFERENCES NhanVien(MaNhanVien),
    CONSTRAINT UQ_DanhGiaNhanVien UNIQUE(MaNhanVien, KyDanhGia)
);

-- ============================================================================
-- 8. TÀI KHOẢN / PHÂN QUYỀN / CẤU HÌNH / NHẬT KÝ
-- ============================================================================
CREATE TABLE TaiKhoan (
    MaTaiKhoan BIGINT IDENTITY(1,1) PRIMARY KEY,
    TenDangNhap VARCHAR(80) NOT NULL UNIQUE,
    MatKhauHash VARCHAR(255) NOT NULL,
    MaNhanVien VARCHAR(20) UNIQUE,
    EmailKhoiPhuc VARCHAR(120),
    TrangThai NVARCHAR(20) DEFAULT 'ChoKichHoat' CHECK (TrangThai IN ('ChoKichHoat', 'HoatDong', 'Khoa', 'ThuHoi')),
    NgayTao DATETIME2 DEFAULT GETDATE(),
    LanDangNhapCuoi DATETIME2,
    FOREIGN KEY(MaNhanVien) REFERENCES NhanVien(MaNhanVien)
);

CREATE TABLE VaiTro (
    MaVaiTro VARCHAR(20) PRIMARY KEY,
    TenVaiTro NVARCHAR(100) NOT NULL,
    MoTa NVARCHAR(255),
    TrangThai NVARCHAR(20) DEFAULT 'HoatDong' CHECK (TrangThai IN ('HoatDong', 'NgungHoatDong'))
);

CREATE TABLE Quyen (
    MaQuyen VARCHAR(30) PRIMARY KEY,
    TenQuyen NVARCHAR(120) NOT NULL,
    NhomChucNang NVARCHAR(100),
    MoTa NVARCHAR(255)
);

CREATE TABLE TaiKhoanVaiTro (
    MaTaiKhoan BIGINT NOT NULL,
    MaVaiTro VARCHAR(20) NOT NULL,
    NgayGan DATETIME2 DEFAULT GETDATE(),
    NguoiGan BIGINT,
    PRIMARY KEY(MaTaiKhoan, MaVaiTro),
    FOREIGN KEY(MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan),
    FOREIGN KEY(MaVaiTro) REFERENCES VaiTro(MaVaiTro),
    FOREIGN KEY(NguoiGan) REFERENCES TaiKhoan(MaTaiKhoan)
);

CREATE TABLE VaiTroQuyen (
    MaVaiTro VARCHAR(20) NOT NULL,
    MaQuyen VARCHAR(30) NOT NULL,
    PRIMARY KEY(MaVaiTro, MaQuyen),
    FOREIGN KEY(MaVaiTro) REFERENCES VaiTro(MaVaiTro),
    FOREIGN KEY(MaQuyen) REFERENCES Quyen(MaQuyen)
);

CREATE TABLE CauHinhHeThong (
    MaCauHinh VARCHAR(80) PRIMARY KEY,
    GiaTri NVARCHAR(MAX),
    KieuDuLieu VARCHAR(30),
    MoTa NVARCHAR(255),
    CapNhatLuc DATETIME2 DEFAULT GETDATE(),
    CapNhatBoi BIGINT,
    FOREIGN KEY(CapNhatBoi) REFERENCES TaiKhoan(MaTaiKhoan)
);

CREATE TABLE NhatKyHeThong (
    MaNhatKy BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaTaiKhoan BIGINT,
    ThoiDiem DATETIME2 DEFAULT GETDATE(),
    HanhDong NVARCHAR(100) NOT NULL,
    DoiTuong NVARCHAR(100),
    MaDoiTuong VARCHAR(80),
    DiaChiIP VARCHAR(45),
    NoiDung NVARCHAR(MAX), -- Dùng NVARCHAR(MAX) để lưu JSON
    FOREIGN KEY(MaTaiKhoan) REFERENCES TaiKhoan(MaTaiKhoan)
);

CREATE INDEX IX_NhatKyHeThong_ThoiDiem ON NhatKyHeThong(ThoiDiem);
CREATE INDEX IX_NhatKyHeThong_DoiTuong ON NhatKyHeThong(DoiTuong, MaDoiTuong);

CREATE TABLE ThongBao (
    MaThongBao BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaNhanVien VARCHAR(20),
    MaBenhNhan VARCHAR(20),
    TieuDe NVARCHAR(180) NOT NULL,
    NoiDung NVARCHAR(MAX) NOT NULL,
    LoaiThongBao NVARCHAR(50),
    ThoiGianGui DATETIME2,
    TrangThai NVARCHAR(20) DEFAULT 'Nhap' CHECK (TrangThai IN ('Nhap', 'DaGui', 'ThatBai', 'DaDoc')),
    KenhGui NVARCHAR(40),
    FOREIGN KEY(MaNhanVien) REFERENCES NhanVien(MaNhanVien),
    FOREIGN KEY(MaBenhNhan) REFERENCES BenhNhan(MaBenhNhan)
);

-- ============================================================================
-- 9. QUẢN LÝ HÓA CHẤT
-- ============================================================================
CREATE TABLE HoaChat (
    MaHoaChat VARCHAR(20) PRIMARY KEY,
    TenHoaChat NVARCHAR(180) NOT NULL,
    NongDo NVARCHAR(80),
    DonViTinh NVARCHAR(30),
    HangSanXuat NVARCHAR(150),
    MoTa NVARCHAR(255),
    TrangThai NVARCHAR(20) DEFAULT 'DangSuDung' CHECK (TrangThai IN ('DangSuDung', 'NgungSuDung'))
);

CREATE TABLE LoHoaChat (
    MaLoHoaChat BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaHoaChat VARCHAR(20) NOT NULL,
    SoLo VARCHAR(80) NOT NULL,
    NgaySanXuat DATE,
    HanSuDung DATE,
    SoLuongTon DECIMAL(14,2) DEFAULT 0,
    ViTriLuuTru NVARCHAR(100),
    TrangThai NVARCHAR(20) DEFAULT 'DangTon' CHECK (TrangThai IN ('DangTon', 'HetHang', 'HetHan', 'BiKhoa')),
    FOREIGN KEY(MaHoaChat) REFERENCES HoaChat(MaHoaChat),
    CONSTRAINT UQ_LoHoaChat UNIQUE(MaHoaChat, SoLo)
);

CREATE INDEX IX_LoHoaChat_HanSuDung ON LoHoaChat(HanSuDung);

CREATE TABLE ChiTietNhapHoaChat (
    MaChiTietNhapHoaChat BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuNhap BIGINT NOT NULL,
    MaLoHoaChat BIGINT NOT NULL,
    SoLuong DECIMAL(14,2) NOT NULL,
    DonGia DECIMAL(14,2) NOT NULL,
    ThanhTien DECIMAL(16,2) NOT NULL,
    FOREIGN KEY(MaPhieuNhap) REFERENCES PhieuNhap(MaPhieuNhap),
    FOREIGN KEY(MaLoHoaChat) REFERENCES LoHoaChat(MaLoHoaChat)
);

CREATE TABLE ChiTietXuatHoaChat (
    MaChiTietXuatHoaChat BIGINT IDENTITY(1,1) PRIMARY KEY,
    MaPhieuXuat BIGINT NOT NULL,
    MaLoHoaChat BIGINT NOT NULL,
    SoLuong DECIMAL(14,2) NOT NULL,
    FOREIGN KEY(MaPhieuXuat) REFERENCES PhieuXuat(MaPhieuXuat),
    FOREIGN KEY(MaLoHoaChat) REFERENCES LoHoaChat(MaLoHoaChat)
);
GO