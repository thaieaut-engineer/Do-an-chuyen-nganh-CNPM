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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.warehouse.WarehouseModels.ReceiptDetail;
import model.warehouse.WarehouseModels.ReceiptLine;

final class WarehouseReceiptDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    WarehouseReceiptDialog(Window owner, ReceiptDetail detail) {
        super(owner, "Chi tiết phiếu " + detail.receipt().number(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(detail));
        setMinimumSize(new Dimension(720, 400));
        setSize(980, 600);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(ReceiptDetail detail) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(15, 17, 14, 17));
        var receipt = detail.receipt();
        JPanel header = new JPanel(new GridLayout(0, 3, 12, 6));
        header.setOpaque(false);
        addLabel(header, "Số phiếu: " + receipt.number());
        addLabel(header, "Ngày: "
                + (receipt.date() == null ? "—" : DATE.format(receipt.date())));
        addLabel(header, "Loại phiếu: " + ("Nhap".equals(receipt.kind()) ? "Nhập thuốc" : "Xuất thuốc"));
        addLabel(header, "Nhân viên: " + safe(receipt.employeeName()));
        addLabel(header, "Đối tác/người nhận: " + safe(receipt.counterparty()));
        addLabel(header, "Trạng thái: " + receiptStatus(receipt.status()));
        addLabel(header, "Số dòng thuốc: " + receipt.medicineLineCount());
        addLabel(header, "Dòng hóa chất cùng phiếu: " + receipt.chemicalLineCount());
        addLabel(header, "Tổng tiền thuốc: " + number(receipt.total()));
        root.add(header, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Mã thuốc", "Tên thuốc", "Mã lô", "Số lô",
                    "Ngày SX", "Hạn SD", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (ReceiptLine line : detail.medicineLines()) {
            model.addRow(new Object[]{
                    line.medicineId(), line.medicineName(), line.batchId(),
                    line.batchNumber(), date(line.manufactureDate()),
                    date(line.expiryDate()), number(line.quantity()),
                    number(line.unitCost()), number(line.lineTotal())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xF5F7FA));
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {90, 180, 75, 110, 95, 95, 90, 100, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        root.add(scroll, BorderLayout.CENTER);

        JLabel note = new JLabel("Chi tiết hóa chất không hiển thị trong phân hệ kho thuốc.");
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

    private String date(java.time.LocalDate value) {
        return value == null ? "—" : value.toString();
    }

    private String number(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String receiptStatus(String status) {
        return switch (status) {
            case "Nhap" -> "Đang nhập";
            case "DaXacNhan" -> "Đã xác nhận";
            case "DaHuy" -> "Đã hủy";
            default -> safe(status);
        };
    }
}
