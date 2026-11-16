package view;

import bus.warehouse.WarehouseBUS;
import bus.warehouse.WarehouseException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.warehouse.WarehouseModels.BatchAllocationPreview;
import model.warehouse.WarehouseModels.CreatedReceipt;
import model.warehouse.WarehouseModels.ExportLinePreview;
import model.warehouse.WarehouseModels.ExportPreview;

final class ExportPreviewDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(ExportPreviewDialog.class.getName());
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthenticatedSession session;
    private final WarehouseBUS warehouseBUS;
    private final ExportPreview preview;
    private final Consumer<CreatedReceipt> onExported;
    private final JLabel message = new JLabel(" ");
    private final JButton confirmButton = new JButton("Xác nhận xuất thuốc");

    ExportPreviewDialog(
            Window owner,
            AuthenticatedSession session,
            WarehouseBUS warehouseBUS,
            ExportPreview preview,
            Consumer<CreatedReceipt> onExported) {
        super(owner, "Xem trước cấp phát thuốc", ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.warehouseBUS = warehouseBUS;
        this.preview = preview;
        this.onExported = onExported;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(760, 420));
        setSize(980, 600);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(15, 17, 13, 17));
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createLines(), BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        JLabel patient = new JLabel(
                "Đơn #" + preview.prescription().prescriptionId()
                + " · " + preview.prescription().patientName()
                + " · Bác sĩ: " + preview.prescription().doctorName());
        patient.setFont(new Font("Segoe UI", Font.BOLD, 14));
        patient.setForeground(MainFrame.TEXT);
        JLabel status = new JLabel(preview.canFulfill()
                ? "Đủ tồn khả dụng. Hệ thống sẽ phân bổ theo FEFO (lô hết hạn trước xuất trước)."
                : "Tồn khả dụng không đủ. Không thể xác nhận xuất đơn này.");
        status.setForeground(preview.canFulfill()
                ? new Color(0x167647) : new Color(0xB42318));
        header.add(patient, BorderLayout.NORTH);
        header.add(status, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createLines() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Mã thuốc", "Tên thuốc", "Cần cấp", "Khả dụng", "Lô FEFO dự kiến"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (ExportLinePreview line : preview.lines()) {
            model.addRow(new Object[]{
                    line.medicineId(),
                    line.medicineName(),
                    number(line.prescribedQuantity()),
                    number(line.availableQuantity()),
                    allocations(line)
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xF5F7FA));
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {100, 190, 100, 100, 430};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(message, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = new JButton("Đóng");
        cancel.addActionListener(event -> dispose());
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setBackground(MainFrame.BLUE);
        confirmButton.setOpaque(true);
        confirmButton.setBorderPainted(false);
        confirmButton.setEnabled(preview.canFulfill()
                && session.hasPermission("KHO_XUAT"));
        confirmButton.addActionListener(event -> confirmExport());
        actions.add(cancel);
        actions.add(confirmButton);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private String allocations(ExportLinePreview line) {
        StringBuilder text = new StringBuilder();
        for (BatchAllocationPreview batch : line.batches()) {
            if (text.length() > 0) {
                text.append(" · ");
            }
            text.append(batch.batchNumber())
                    .append(" (HSD ")
                    .append(DATE.format(batch.expiryDate()))
                    .append(", tồn ")
                    .append(number(batch.availableQuantity()))
                    .append(')');
        }
        return text.length() == 0 ? "Không có lô khả dụng" : text.toString();
    }

    private void confirmExport() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Xác nhận trừ tồn theo các lô FEFO và chuyển đơn sang trạng thái đã cấp phát?",
                "Xác nhận xuất thuốc",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        confirmButton.setEnabled(false);
        message.setText("Đang khóa đơn, phân bổ lô và cập nhật tồn kho...");
        new SwingWorker<CreatedReceipt, Void>() {
            @Override
            protected CreatedReceipt doInBackground() throws WarehouseException {
                return warehouseBUS.exportPrescription(
                        session, preview.prescription().prescriptionId());
            }

            @Override
            protected void done() {
                try {
                    CreatedReceipt receipt = get();
                    JOptionPane.showMessageDialog(
                            ExportPreviewDialog.this,
                            "Đã xác nhận phiếu xuất " + receipt.number() + ".",
                            "Xuất thuốc thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    onExported.accept(receipt);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError(exception);
                } catch (ExecutionException exception) {
                    showError(exception.getCause());
                } finally {
                    confirmButton.setEnabled(
                            preview.canFulfill() && session.hasPermission("KHO_XUAT"));
                }
            }
        }.execute();
    }

    private void showError(Throwable error) {
        LOGGER.log(Level.SEVERE, "Không thể xuất thuốc theo đơn.", error);
        String text = error == null || error.getMessage() == null
                ? "Lỗi không xác định." : error.getMessage();
        message.setText(text);
        message.setForeground(new Color(0xB42318));
        JOptionPane.showMessageDialog(
                this, text, "Không thể xuất thuốc", JOptionPane.ERROR_MESSAGE);
    }

    private String number(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
