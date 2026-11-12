package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import model.serviceorder.ServiceOrderDetail;

final class ServiceOrderDetailDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    ServiceOrderDetailDialog(Window owner, ServiceOrderDetail detail) {
        super(owner, "Chi tiết chỉ định " + detail.maChiDinh(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JTextArea content = new JTextArea(format(detail));
        content.setEditable(false);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        content.setForeground(MainFrame.TEXT);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 14, 14, 14)));
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);
        setContentPane(root);
        setMinimumSize(new Dimension(560, 520));
        setSize(650, 650);
        setLocationRelativeTo(owner);
    }

    private String format(ServiceOrderDetail detail) {
        return "CHỈ ĐỊNH DỊCH VỤ\n"
                + "Mã chỉ định: " + detail.maChiDinh() + "\n"
                + "Trạng thái: " + statusLabel(detail.trangThai()) + "\n"
                + "Thời gian chỉ định: " + dateTime(detail.thoiGianChiDinh()) + "\n"
                + "Thời gian thực hiện: " + dateTime(detail.thoiGianThucHien()) + "\n\n"
                + "LƯỢT KHÁM / PHIẾU KHÁM\n"
                + "Mã lượt khám: " + detail.maLuotKham() + "\n"
                + "Mã phiếu khám: " + detail.maPhieuKham() + "\n"
                + "Trạng thái phiếu: " + safe(detail.trangThaiPhieuKham()) + "\n\n"
                + "BỆNH NHÂN\n"
                + "Mã bệnh nhân: " + safe(detail.maBenhNhan()) + "\n"
                + "Họ tên: " + safe(detail.tenBenhNhan()) + "\n"
                + "Ngày sinh: " + date(detail.ngaySinh()) + "\n"
                + "Giới tính: " + safe(detail.gioiTinh()) + "\n\n"
                + "BÁC SĨ PHỤ TRÁCH\n"
                + "Mã nhân viên: " + safe(detail.maBacSi()) + "\n"
                + "Họ tên: " + safe(detail.tenBacSi()) + "\n"
                + "Chuyên khoa: " + safe(detail.tenChuyenKhoa()) + "\n\n"
                + "DỊCH VỤ\n"
                + "Mã dịch vụ: " + safe(detail.maDichVu()) + "\n"
                + "Tên dịch vụ: " + safe(detail.tenDichVu()) + "\n"
                + "Loại dịch vụ: " + safe(detail.loaiDichVu()) + "\n"
                + "Đơn vị tính: " + safe(detail.donViTinh()) + "\n"
                + "Người thực hiện: " + safe(detail.tenNhanVienThucHien()) + "\n\n"
                + "Ghi chú: " + safe(detail.ghiChu()) + "\n"
                + "Kết quả: " + safe(detail.ketQua()) + "\n"
                + "Tình trạng tính phí: "
                + (detail.daTinhPhi() ? "Đã có chi tiết phiếu tính phí"
                        : "Chưa có chi tiết phiếu tính phí")
                + "\n\nGiá và số lượng không được lưu trên bảng chỉ định; "
                + "không thể khôi phục giá lịch sử từ màn hình này.";
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "ChoThucHien" -> "Chờ thực hiện";
            case "DangThucHien" -> "Đang thực hiện";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            default -> safe(status);
        };
    }

    private String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String dateTime(java.time.LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
