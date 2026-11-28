package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.warehouse.WarehouseModels.BatchDetail;
import model.warehouse.WarehouseModels.StockMovement;

final class BatchDetailDialog extends JDialog {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    BatchDetailDialog(Window owner, BatchDetail detail) {
        super(owner, "Chi tiết lô " + detail.batch().batchNumber(),
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(detail));
        setMinimumSize(new Dimension(760, 420));
        setSize(1040, 650);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(BatchDetail detail) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(15, 17, 14, 17));
        var batch = detail.batch();
        JPanel header = new JPanel(new GridLayout(0, 3, 12, 6));
        header.setOpaque(false);
        addLabel(header, "Thuốc: " + batch.medicineName() + " (" + batch.medicineId() + ")");
        addLabel(header, "Số lô: " + batch.batchNumber());
        addLabel(header, "Hạn sử dụng: " + date(batch.expiryDate()));
        addLabel(header, "Tồn hiện tại: " + number(batch.stock()));
        addLabel(header, "Đã nhập xác nhận: " + number(detail.confirmedIn()));
        addLabel(header, "Đã xuất xác nhận: " + number(detail.confirmedOut()));
        addLabel(header, "Tồn tính từ lịch sử: "
                + number(detail.expectedStockFromHistory()));
        addLabel(header, "Vị trí: " + safe(batch.location()));
        addLabel(header, "Trạng thái: " + batchStatus(batch.status()));
        root.add(header, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Ngày giờ", "Loại", "Số phiếu", "Số lượng",
                    "Đơn giá", "Nhân viên", "Trạng thái", "Ghi chú"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (StockMovement movement : detail.movements()) {
            model.addRow(new Object[]{
                    movement.date() == null ? "—" : DATE_TIME.format(movement.date()),
                    "Nhap".equals(movement.movementType()) ? "Nhập" : "Xuất",
                    movement.receiptNumber(),
                    number(movement.quantity()),
                    number(movement.unitCost()),
                    safe(movement.employeeName()),
                    receiptStatus(movement.status()),
                    safe(movement.note())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xF5F7FA));
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        root.add(scroll, BorderLayout.CENTER);

        JLabel reconciliation = new JLabel(detail.reconciled()
                ? "Đối soát: tồn hiện tại khớp tổng nhập trừ tổng xuất đã xác nhận."
                : "Cảnh báo: tồn hiện tại không khớp lịch sử phiếu thuốc đã xác nhận.");
        reconciliation.setForeground(detail.reconciled()
                ? new Color(0x167647) : new Color(0xB42318));
        reconciliation.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(reconciliation, BorderLayout.CENTER);
        JButton close = new JButton("Đóng");
        view.component.UIStyle.secondaryButton(close);
        close.addActionListener(event -> dispose());
        footer.add(close, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private void addLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(label);
    }

    private String date(java.time.LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    private String number(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String batchStatus(String status) {
        return switch (status) {
            case "HetHang" -> "Hết hàng";
            case "HetHan" -> "Hết hạn";
            case "BiKhoa" -> "Bị khóa";
            default -> "Đang tồn";
        };
    }

    private String receiptStatus(String status) {
        return switch (status) {
            case "DaXacNhan" -> "Đã xác nhận";
            case "DaHuy" -> "Đã hủy";
            case "Nhap" -> "Đang nhập";
            default -> safe(status);
        };
    }
}
