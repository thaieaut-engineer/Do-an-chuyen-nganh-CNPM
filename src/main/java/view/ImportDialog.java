package view;

import bus.warehouse.WarehouseBUS;
import bus.warehouse.WarehouseException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.warehouse.WarehouseModels.CreatedReceipt;
import model.warehouse.WarehouseModels.ImportDraft;
import model.warehouse.WarehouseModels.ImportLineDraft;

final class ImportDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(ImportDialog.class.getName());

    private final AuthenticatedSession session;
    private final WarehouseBUS warehouseBUS;
    private final Runnable onSaved;
    private final JTextField supplierField = new JTextField();
    private final JTextArea noteField = new JTextArea(2, 30);
    private final JTextField medicineIdField = new JTextField();
    private final JTextField batchNumberField = new JTextField();
    private final JTextField manufactureDateField = new JTextField();
    private final JTextField expiryDateField = new JTextField();
    private final JTextField quantityField = new JTextField();
    private final JTextField unitCostField = new JTextField();
    private final JTextField locationField = new JTextField();
    private final JLabel message = new JLabel(" ");
    private final JButton addLineButton = new JButton("Thêm dòng thuốc");
    private final JButton removeLineButton = new JButton("Bỏ dòng");
    private final JButton saveButton = new JButton("Xác nhận nhập");
    private final DefaultTableModel lineModel = new DefaultTableModel(
            new Object[]{"Mã thuốc", "Số lô", "Ngày SX", "Hạn SD",
                "Số lượng", "Đơn giá", "Vị trí"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable lineTable = new JTable(lineModel);
    private final List<ImportLineDraft> lines = new ArrayList<>();

    ImportDialog(
            java.awt.Window owner,
            AuthenticatedSession session,
            WarehouseBUS warehouseBUS,
            Runnable onSaved) {
        super(owner, "Tạo phiếu nhập thuốc", ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.warehouseBUS = warehouseBUS;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(760, 500));
        setSize(980, 650);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(14, 16, 13, 16));
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createLines(), BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        JPanel supplier = new JPanel(new BorderLayout(8, 0));
        supplier.setOpaque(false);
        JLabel supplierLabel = new JLabel("Nhà cung cấp");
        supplierLabel.setPreferredSize(new Dimension(100, 32));
        supplierField.setPreferredSize(new Dimension(0, 34));
        supplier.add(supplierLabel, BorderLayout.WEST);
        supplier.add(supplierField, BorderLayout.CENTER);
        header.add(supplier);
        header.add(javax.swing.Box.createVerticalStrut(6));
        JPanel note = new JPanel(new BorderLayout(8, 0));
        note.setOpaque(false);
        JLabel noteLabel = new JLabel("Ghi chú");
        noteLabel.setPreferredSize(new Dimension(100, 44));
        noteField.setLineWrap(true);
        noteField.setWrapStyleWord(true);
        noteField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(4, 6, 4, 6)));
        note.add(noteLabel, BorderLayout.WEST);
        note.add(new JScrollPane(noteField), BorderLayout.CENTER);
        header.add(note);
        return header;
    }

    private JPanel createLines() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        JPanel lineForm = new JPanel();
        lineForm.setOpaque(false);
        lineForm.setLayout(new javax.swing.BoxLayout(
                lineForm, javax.swing.BoxLayout.Y_AXIS));
        JPanel first = new JPanel(new GridLayout(1, 4, 8, 0));
        first.setOpaque(false);
        first.add(field("Mã thuốc *", medicineIdField));
        first.add(field("Số lô *", batchNumberField));
        first.add(field("Ngày SX (yyyy-MM-dd)", manufactureDateField));
        first.add(field("Hạn SD * (yyyy-MM-dd)", expiryDateField));
        JPanel second = new JPanel(new GridLayout(1, 4, 8, 0));
        second.setOpaque(false);
        second.add(field("Số lượng *", quantityField));
        second.add(field("Đơn giá nhập *", unitCostField));
        second.add(field("Vị trí", locationField));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 3));
        actions.setOpaque(false);
        actions.add(removeLineButton);
        actions.add(addLineButton);
        second.add(actions);
        lineForm.add(first);
        lineForm.add(javax.swing.Box.createVerticalStrut(6));
        lineForm.add(second);
        addLineButton.addActionListener(event -> addLine());
        removeLineButton.addActionListener(event -> removeLine());
        center.add(lineForm, BorderLayout.NORTH);

        lineTable.setRowHeight(32);
        lineTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lineTable.setGridColor(MainFrame.BORDER);
        lineTable.setAutoCreateRowSorter(true);
        lineTable.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(lineTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        center.add(tableScroll, BorderLayout.CENTER);
        return center;
    }

    private JPanel field(String label, JTextField input) {
        JPanel field = new JPanel(new BorderLayout(0, 3));
        field.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setForeground(MainFrame.MUTED);
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        input.setPreferredSize(new Dimension(0, 32));
        field.add(caption, BorderLayout.NORTH);
        field.add(input, BorderLayout.CENTER);
        return field;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(message, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = new JButton("Hủy");
        cancel.addActionListener(event -> dispose());
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(MainFrame.BLUE);
        saveButton.setOpaque(true);
        saveButton.setBorderPainted(false);
        saveButton.addActionListener(event -> save());
        actions.add(cancel);
        actions.add(saveButton);
        footer.add(actions, BorderLayout.EAST);
        getRootPane().setDefaultButton(addLineButton);
        return footer;
    }

    private void addLine() {
        try {
            String manufactureText = manufactureDateField.getText().trim();
            LocalDate manufacture = manufactureText.isBlank()
                    ? null : LocalDate.parse(manufactureText);
            LocalDate expiry = LocalDate.parse(expiryDateField.getText().trim());
            ImportLineDraft line = new ImportLineDraft(
                    medicineIdField.getText().trim(),
                    batchNumberField.getText().trim(),
                    manufacture,
                    expiry,
                    new java.math.BigDecimal(quantityField.getText().trim()),
                    new java.math.BigDecimal(unitCostField.getText().trim()),
                    locationField.getText().trim());
            lines.add(line);
            lineModel.addRow(new Object[]{
                    line.medicineId(), line.batchNumber(),
                    line.manufactureDate() == null ? "—" : line.manufactureDate(),
                    line.expiryDate(), line.quantity(), line.unitCost(),
                    line.location() == null || line.location().isBlank() ? "—" : line.location()});
            clearLineInputs();
            updateMessage("Đã thêm dòng. Lô chỉ được ghi nhận khi xác nhận phiếu.");
        } catch (NumberFormatException | DateTimeParseException exception) {
            showMessage("Hãy nhập số lượng, đơn giá và ngày theo đúng định dạng.");
        }
    }

    private void removeLine() {
        int selected = lineTable.getSelectedRow();
        if (selected < 0) {
            showMessage("Hãy chọn dòng cần bỏ.");
            return;
        }
        int modelRow = lineTable.convertRowIndexToModel(selected);
        lines.remove(modelRow);
        lineModel.removeRow(modelRow);
        updateMessage(" ");
    }

    private void save() {
        ImportDraft draft = new ImportDraft(
                supplierField.getText().trim(), noteField.getText().trim(), lines);
        setSaving(true);
        new SwingWorker<CreatedReceipt, Void>() {
            @Override
            protected CreatedReceipt doInBackground() throws WarehouseException {
                return warehouseBUS.createImport(session, draft);
            }

            @Override
            protected void done() {
                try {
                    CreatedReceipt receipt = get();
                    JOptionPane.showMessageDialog(
                            ImportDialog.this,
                            "Đã xác nhận phiếu nhập " + receipt.number() + ".",
                            "Nhập kho thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    onSaved.run();
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError(exception);
                } catch (ExecutionException exception) {
                    showError(exception.getCause());
                } finally {
                    setSaving(false);
                }
            }
        }.execute();
    }

    private void clearLineInputs() {
        medicineIdField.setText("");
        batchNumberField.setText("");
        manufactureDateField.setText("");
        expiryDateField.setText("");
        quantityField.setText("");
        unitCostField.setText("");
        locationField.setText("");
        medicineIdField.requestFocusInWindow();
    }

    private void setSaving(boolean saving) {
        saveButton.setEnabled(!saving);
        addLineButton.setEnabled(!saving);
        removeLineButton.setEnabled(!saving);
        updateMessage(saving ? "Đang ghi phiếu nhập và cập nhật tồn kho..." : " ");
    }

    private void showMessage(String text) {
        message.setForeground(new Color(0xB42318));
        message.setText(text);
    }

    private void updateMessage(String text) {
        message.setForeground(MainFrame.MUTED);
        message.setText(text == null || text.isBlank() ? " " : text);
    }

    private void showError(Throwable error) {
        LOGGER.log(Level.SEVERE, "Không thể tạo phiếu nhập thuốc.", error);
        String text = error == null || error.getMessage() == null
                ? "Lỗi không xác định." : error.getMessage();
        showMessage(text);
        JOptionPane.showMessageDialog(
                this, text, "Không thể nhập kho", JOptionPane.ERROR_MESSAGE);
    }
}
