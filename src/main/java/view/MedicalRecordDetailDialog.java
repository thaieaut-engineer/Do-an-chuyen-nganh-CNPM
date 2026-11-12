package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import model.medicalrecord.MedicalRecordDetail;

final class MedicalRecordDetailDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    MedicalRecordDetailDialog(Window owner, MedicalRecordDetail record) {
        super(owner, "Chi tiết phiếu khám " + record.maPhieuKham(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(record));
        setMinimumSize(new Dimension(620, 620));
        setSize(700, 720);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(MedicalRecordDetail record) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        JTextArea details = new JTextArea(format(record));
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

    private String format(MedicalRecordDetail record) {
        return "PHIẾU KHÁM\n"
                + "Mã phiếu: " + record.maPhieuKham() + "\n"
                + "Trạng thái: " + statusLabel(record.trangThaiPhieu()) + "\n"
                + "Ngày lập: " + dateTime(record.thoiGianLap()) + "\n"
                + "Ngày ký: " + dateTime(record.thoiGianKy()) + "\n\n"
                + "BỆNH NHÂN\n"
                + "Mã bệnh nhân: " + safe(record.maBenhNhan()) + "\n"
                + "Họ tên: " + safe(record.tenBenhNhan()) + "\n"
                + "Ngày sinh: " + date(record.ngaySinh()) + "\n"
                + "Giới tính: " + safe(record.gioiTinh()) + "\n"
                + "Số điện thoại: " + safe(record.soDienThoai()) + "\n"
                + "Địa chỉ: " + safe(record.diaChi()) + "\n\n"
                + "LƯỢT KHÁM\n"
                + "Mã lượt khám: " + record.maLuotKham() + "\n"
                + "Thời gian tiếp nhận: " + dateTime(record.thoiGianTiepNhan()) + "\n"
                + "Trạng thái lượt: " + statusLabel(record.trangThaiLuotKham()) + "\n\n"
                + "BÁC SĨ\n"
                + "Mã nhân viên: " + safe(record.maNhanVien()) + "\n"
                + "Họ tên: " + safe(record.tenBacSi()) + "\n"
                + "Chuyên khoa: " + safe(record.tenChuyenKhoa()) + "\n\n"
                + "NỘI DUNG KHÁM\n"
                + "Lý do khám:\n" + safe(record.lyDoKham()) + "\n\n"
                + "Triệu chứng:\n" + safe(record.trieuChung()) + "\n\n"
                + "Dấu hiệu sinh tồn:\n" + safe(record.dauHieuSinhTon()) + "\n\n"
                + "Kết quả lâm sàng:\n" + safe(record.ketQuaLamSang()) + "\n\n"
                + "Chẩn đoán:\n" + safe(record.chanDoan()) + "\n\n"
                + "Hướng điều trị:\n" + safe(record.huongDieuTri());
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
            case "Nhap" -> "Nháp";
            case "HoanTat" -> "Hoàn tất";
            case "DaKy" -> "Đã ký";
            case "DaKhoa" -> "Đã khóa";
            case "ChoKham" -> "Chờ khám";
            case "DangKham" -> "Đang khám";
            case "ChoKetQua" -> "Chờ kết quả";
            case "DaHuy" -> "Đã hủy";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }
}
