package view;

import bus.warehouse.WarehouseBUS;
import bus.warehouse.WarehouseException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.auth.AuthenticatedSession;
import model.warehouse.WarehouseModels.Medicine;
import model.warehouse.WarehouseModels.MedicineDraft;

final class MedicineDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(MedicineDialog.class.getName());

    private final AuthenticatedSession session;
    private final WarehouseBUS warehouseBUS;
    private final Medicine existing;
    private final Runnable onSaved;
    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField ingredientField = new JTextField();
    private final JTextField strengthField = new JTextField();
    private final JTextField formField = new JTextField();
    private final JTextField unitField = new JTextField();
    private final JTextField routeField = new JTextField();
    private final JTextField manufacturerField = new JTextField();
    private final JTextField countryField = new JTextField();
    private final JTextArea descriptionField = new JTextArea(3, 32);
    private final JComboBox<Choice> statusCombo = new JComboBox<>();
    private final JButton saveButton = new JButton("Lưu thuốc");
    private final JLabel message = new JLabel(" ");

    MedicineDialog(
            java.awt.Window owner,
            AuthenticatedSession session,
            WarehouseBUS warehouseBUS,
            Medicine existing,
            Runnable onSaved) {
        super(owner, existing == null ? "Thêm thuốc" : "Cập nhật thuốc",
                ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.warehouseBUS = warehouseBUS;
        this.existing = existing;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        statusCombo.addItem(new Choice("DangSuDung", "Đang sử dụng"));
        statusCombo.addItem(new Choice("NgungSuDung", "Ngừng sử dụng"));
        if (existing != null) {
            populate(existing);
        }
        setContentPane(createContent());
        setMinimumSize(new Dimension(560, 500));
        setSize(650, 650);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(15, 18, 14, 18));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, 0, "Mã thuốc *", idField);
        addField(form, 1, "Tên thuốc *", nameField);
        addField(form, 2, "Hoạt chất", ingredientField);
        addField(form, 3, "Hàm lượng", strengthField);
        addField(form, 4, "Dạng bào chế", formField);
        addField(form, 5, "Đơn vị tính", unitField);
        addField(form, 6, "Đường dùng mặc định", routeField);
        addField(form, 7, "Hãng sản xuất", manufacturerField);
        addField(form, 8, "Nước sản xuất", countryField);
        addField(form, 9, "Trạng thái", statusCombo);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        descriptionField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descriptionField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(5, 7, 5, 7)));
        addField(form, 10, "Mô tả", descriptionField);
        idField.setEnabled(existing == null);
        root.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        message.setForeground(MainFrame.MUTED);
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
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
        return root;
    }

    private void addField(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(5, 3, 5, 8);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        if (field instanceof JTextArea area) {
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area);
            scroll.setPreferredSize(new Dimension(0, 64));
            panel.add(scroll, c);
        } else {
            field.setPreferredSize(new Dimension(0, 34));
            panel.add(field, c);
        }
    }

    private void populate(Medicine medicine) {
        idField.setText(medicine.id());
        nameField.setText(medicine.name());
        ingredientField.setText(medicine.activeIngredient());
        strengthField.setText(medicine.strength());
        formField.setText(medicine.dosageForm());
        unitField.setText(medicine.unit());
        routeField.setText(medicine.defaultRoute());
        manufacturerField.setText(medicine.manufacturer());
        countryField.setText(medicine.country());
        descriptionField.setText(medicine.description());
        for (int i = 0; i < statusCombo.getItemCount(); i++) {
            if (statusCombo.getItemAt(i).code().equals(medicine.status())) {
                statusCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void save() {
        Choice status = (Choice) statusCombo.getSelectedItem();
        MedicineDraft draft = new MedicineDraft(
                idField.getText().trim(), nameField.getText().trim(),
                ingredientField.getText().trim(), strengthField.getText().trim(),
                formField.getText().trim(), unitField.getText().trim(),
                routeField.getText().trim(), manufacturerField.getText().trim(),
                countryField.getText().trim(), descriptionField.getText().trim(),
                status == null ? "" : status.code());
        setSaving(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws WarehouseException {
                warehouseBUS.saveMedicine(session, draft, existing == null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
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

    private void setSaving(boolean saving) {
        saveButton.setEnabled(!saving);
        message.setText(saving ? "Đang lưu thuốc..." : " ");
    }

    private void showError(Throwable error) {
        LOGGER.log(Level.SEVERE, "Không thể lưu thuốc.", error);
        String text = error == null || error.getMessage() == null
                ? "Lỗi không xác định." : error.getMessage();
        message.setText(text);
        message.setForeground(new Color(0xB42318));
        JOptionPane.showMessageDialog(this, text, "Không thể lưu thuốc",
                JOptionPane.ERROR_MESSAGE);
    }

    private record Choice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
