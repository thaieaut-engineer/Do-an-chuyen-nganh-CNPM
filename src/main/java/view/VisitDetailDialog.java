package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import model.visit.VisitDetail;

final class VisitDetailDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    VisitDetailDialog(Window owner, VisitDetail visit, boolean canViewMedicalRecord) {
        super(owner, "Chi tiết lượt khám " + visit.maLuotKham(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(visit, canViewMedicalRecord));
        setMinimumSize(new Dimension(560, 520));
        setSize(600, 580);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(VisitDetail visit, boolean canViewMedicalRecord) {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        JLabel title = new JLabel("Chi tiết lượt khám");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(MainFrame.TEXT);
        root.add(title, BorderLayout.NORTH);

        JTextArea details = new JTextArea(format(visit, canViewMedicalRecord));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        details.setForeground(MainFrame.TEXT);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 14, 14, 14)));
        JScrollPane scroll = new JScrollPane(details);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private String format(VisitDetail visit, boolean canViewMedicalRecord) {
        String details = "LƯỢT KHÁM\n"
                + "Mã lượt khám: " + visit.maLuotKham() + "\n"
                + "Trạng thái: " + statusLabel(visit.trangThai()) + "\n"
                + "Tiếp nhận: " + dateTime(visit.thoiGianTiepNhan()) + "\n"
                + "Kết thúc: " + dateTime(visit.thoiGianKetThuc()) + "\n"
                + "Ghi chú: " + safe(visit.ghiChu()) + "\n\n"
                + "BỆNH NHÂN\n"
                + "Mã bệnh nhân: " + safe(visit.maBenhNhan()) + "\n"
                + "Họ tên: " + safe(visit.tenBenhNhan()) + "\n"
                + "Ngày sinh: " + date(visit.ngaySinhBenhNhan()) + "\n"
                + "Giới tính: " + safe(visit.gioiTinhBenhNhan()) + "\n"
                + "Số điện thoại: " + safe(visit.soDienThoaiBenhNhan()) + "\n"
                + "Địa chỉ: " + safe(visit.diaChiBenhNhan()) + "\n\n"
                + "BÁC SĨ PHỤ TRÁCH\n"
                + "Mã nhân viên: " + safe(visit.maNhanVien()) + "\n"
                + "Họ tên: " + safe(visit.tenBacSi()) + "\n"
                + "Chuyên khoa: " + safe(visit.tenChuyenKhoa()) + "\n\n"
                + "LỊCH HẸN\n"
                + "Mã lịch hẹn: " + (visit.maLichHen() == null ? "—" : visit.maLichHen()) + "\n"
                + "Thời gian hẹn: " + dateTime(visit.thoiGianHen()) + "\n"
                + "Trạng thái lịch hẹn: " + statusLabel(visit.trangThaiLichHen());
        if (canViewMedicalRecord) {
            details += "\n\nPHIẾU KHÁM\n"
                    + "Mã phiếu khám: "
                    + (visit.maPhieuKham() == null ? "Chưa có" : visit.maPhieuKham()) + "\n"
                    + "Trạng thái phiếu: " + statusLabel(visit.trangThaiPhieuKham()) + "\n"
                    + "Chỉ định dịch vụ đang xử lý: " + visit.chiDinhDangXuLy();
        }
        return details;
    }

    private String dateTime(java.time.LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String date(java.time.LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "ChoKham" -> "Chờ khám";
            case "DangKham" -> "Đang khám";
            case "ChoKetQua" -> "Chờ kết quả";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            case "DaDen" -> "Đã đến";
            case "ChoXacNhan" -> "Chờ xác nhận";
            case "DaXacNhan" -> "Đã xác nhận";
            case "VangMat" -> "Vắng mặt";
            case "Nhap" -> "Nháp";
            case "DaKy" -> "Đã ký";
            case "DaKhoa" -> "Đã khóa";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }
}
