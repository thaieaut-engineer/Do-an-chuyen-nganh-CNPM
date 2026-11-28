package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import model.medicalrecord.MedicalRecordDetail;

final class MedicalRecordDetailDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    MedicalRecordDetailDialog(Window owner, MedicalRecordDetail record, Runnable onEdit) {
        super(owner, "Chi tiết phiếu khám " + record.maPhieuKham(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(record, onEdit));
        setMinimumSize(new Dimension(800, 560));
        setSize(900, 620);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(MedicalRecordDetail record, Runnable onEdit) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));

        JPanel summary = new JPanel(new GridLayout(1, 3, 12, 0));
        summary.setOpaque(false);
        summary.add(infoCard("PHIẾU KHÁM", "Mã phiếu", Long.toString(record.maPhieuKham()),
                "Trạng thái", statusLabel(record.trangThaiPhieu())));
        summary.add(infoCard("BỆNH NHÂN", "Mã bệnh nhân", safe(record.maBenhNhan()),
                "Họ tên", safe(record.tenBenhNhan())));
        summary.add(infoCard("BÁC SĨ PHỤ TRÁCH", "Bác sĩ", safe(record.tenBacSi()),
                "Chuyên khoa", safe(record.tenChuyenKhoa())));
        root.add(summary, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(3, 2, 14, 14));
        content.setOpaque(false);
        content.add(contentCard("Lý do khám", record.lyDoKham()));
        content.add(contentCard("Triệu chứng", record.trieuChung()));
        content.add(contentCard("Dấu hiệu sinh tồn", record.dauHieuSinhTon()));
        content.add(contentCard("Kết quả lâm sàng", record.ketQuaLamSang()));
        content.add(contentCard("Chẩn đoán", record.chanDoan()));
        content.add(contentCard("Hướng điều trị", record.huongDieuTri()));
        JScrollPane scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(4, 0, 0, 0));
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        if (onEdit != null) {
            JButton edit = new JButton("Chỉnh sửa");
            view.component.UIStyle.primaryButton(edit);
            edit.addActionListener(event -> {
                dispose();
                onEdit.run();
            });
            actions.add(edit);
        }
        JButton close = new JButton("Đóng");
        view.component.UIStyle.secondaryButton(close);
        close.addActionListener(event -> dispose());
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(close);
        footer.add(actions, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel infoCard(String title, String labelOne, String valueOne,
            String labelTwo, String valueTwo) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(new Color(248, 250, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(11, 13, 11, 13)));
        JLabel heading = new JLabel(title);
        heading.setForeground(MainFrame.BLUE);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JPanel values = new JPanel(new GridLayout(2, 1, 0, 3));
        values.setOpaque(false);
        values.add(valueLine(labelOne, valueOne));
        values.add(valueLine(labelTwo, valueTwo));
        card.add(heading, BorderLayout.NORTH);
        card.add(values, BorderLayout.CENTER);
        return card;
    }

    private JPanel valueLine(String label, String value) {
        JPanel line = new JPanel(new BorderLayout(8, 0));
        line.setOpaque(false);
        JLabel key = new JLabel(label);
        key.setForeground(MainFrame.MUTED);
        key.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel text = new JLabel(value);
        text.setForeground(MainFrame.TEXT);
        text.setFont(new Font("Segoe UI", Font.BOLD, 11));
        line.add(key, BorderLayout.WEST);
        line.add(text, BorderLayout.CENTER);
        return line;
    }

    private JPanel contentCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout(0, 7));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel heading = new JLabel(title);
        heading.setForeground(MainFrame.TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextArea text = new JTextArea(safe(value));
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        text.setForeground(MainFrame.TEXT);
        text.setBackground(new Color(250, 251, 253));
        text.setBorder(new EmptyBorder(8, 9, 8, 9));
        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(0, 108));
        card.add(heading, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
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
