package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.border.EmptyBorder;
import model.prescription.PrescriptionDetail;
import model.prescription.PrescriptionLineDetail;

final class PrescriptionDetailDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    PrescriptionDetailDialog(Window owner, PrescriptionDetail detail) {
        super(owner, "Chi tiết đơn thuốc " + detail.maDonThuoc(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(detail));
        setMinimumSize(new Dimension(820, 620));
        setSize(1040, 740);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(PrescriptionDetail detail) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel header = new JPanel(new GridLayout(0, 2, 14, 5));
        header.setOpaque(false);
        addLabel(header, "Mã đơn thuốc: " + detail.maDonThuoc());
        addLabel(header, "Ngày kê: " + dateTime(detail.ngayKe()));
        addLabel(header, "Trạng thái: " + statusLabel(detail.trangThai()));
        addLabel(header, "Bác sĩ kê: " + safe(detail.tenBacSi())
                + " (" + safe(detail.maBacSi()) + ")");
        addLabel(header, "Chuyên khoa: " + safe(detail.tenChuyenKhoa()));
        addLabel(header, "Mã lượt khám: " + detail.maLuotKham()
                + " · " + statusLabel(detail.trangThaiLuotKham()));
        addLabel(header, "Tiếp nhận: " + dateTime(detail.thoiGianTiepNhan()));
        addLabel(header, "Tình trạng xuất: "
                + (detail.daXuatThuoc() ? "Đã liên kết phiếu xuất" : "Chưa xuất"));
        addLabel(header, "Bệnh nhân: " + safe(detail.tenBenhNhan())
                + " (" + safe(detail.maBenhNhan()) + ")");
        addLabel(header, "Điện thoại: " + safe(detail.soDienThoai()));
        addLabel(header, "Ngày sinh: " + date(detail.ngaySinh())
                + " · " + safe(detail.gioiTinh()));
        JTextArea directions = new JTextArea(
                "Lời dặn: " + safe(detail.loiDan()));
        directions.setEditable(false);
        directions.setLineWrap(true);
        directions.setWrapStyleWord(true);
        directions.setRows(2);
        directions.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        directions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        header.add(directions);
        root.add(header, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Mã thuốc", "Tên thuốc", "Hoạt chất / hàm lượng",
                    "Liều dùng", "Đường dùng", "Tần suất", "Thời gian",
                    "Số lượng", "Đơn vị", "Hướng dẫn"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (PrescriptionLineDetail line : detail.lines()) {
            model.addRow(new Object[]{
                    line.maThuoc(),
                    line.tenThuoc(),
                    join(line.hoatChat(), line.hamLuong()),
                    line.lieuDung(),
                    safe(line.duongDung()),
                    safe(line.tanSuat()),
                    safe(line.thoiGianDung()),
                    line.soLuong(),
                    safe(line.donViTinh()),
                    safe(line.huongDan())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tableHeader.setPreferredSize(new Dimension(0, 36));
        tableHeader.setForeground(MainFrame.MUTED);
        tableHeader.setBackground(new Color(0xF5F7FA));
        tableHeader.setReorderingAllowed(false);
        int[] widths = {95, 160, 180, 130, 100, 170, 105, 90, 85, 270};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableScroll.getViewport().setBackground(Color.WHITE);
        root.add(tableScroll, BorderLayout.CENTER);

        JLabel note = new JLabel(detail.lines().size() + " dòng thuốc"
                + (detail.daXuatThuoc()
                        ? " · Đơn đã liên kết với nghiệp vụ xuất thuốc."
                        : " · Kê đơn không làm thay đổi tồn kho."));
        note.setForeground(MainFrame.MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        root.add(note, BorderLayout.SOUTH);
        return root;
    }

    private void addLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(label);
    }

    private String join(String first, String second) {
        if (first == null || first.isBlank()) {
            return safe(second);
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + " · " + second;
    }

    private String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String dateTime(java.time.LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "Nhap" -> "Nháp";
            case "DaKy" -> "Đã ký";
            case "DaGuiKho" -> "Đã gửi kho";
            case "DaCapPhat" -> "Đã cấp phát";
            case "Huy" -> "Đã hủy";
            case "DangKham" -> "Đang khám";
            case "ChoKetQua" -> "Chờ kết quả";
            case "HoanTat" -> "Hoàn tất";
            default -> safe(status);
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
