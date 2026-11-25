package view;

import bus.device.DeviceBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.device.DeviceModels.Device;
import model.device.DeviceModels.DeviceDraft;
import model.device.DeviceModels.TypeOption;
import view.component.UIStyle;

final class DevicePanel extends JPanel {
    private final AuthenticatedSession session;
    private final DeviceBUS bus;
    private final JTextField search = new JTextField();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã thiết bị", "Tên thiết bị", "Loại", "Model", "Serial", "Vị trí", "Tình trạng", "__row"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton edit = new JButton("Chỉnh sửa");
    private final JButton status = new JButton("Đổi tình trạng");

    DevicePanel(AuthenticatedSession session, DeviceBUS bus) {
        this.session = session; this.bus = bus;
        setLayout(new BorderLayout(0, 16)); setBackground(MainFrame.PAGE);
        setBorder(BorderFactory.createEmptyBorder(25, 28, 30, 28));
        add(header(), BorderLayout.NORTH); add(body(), BorderLayout.CENTER); load();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        JPanel labels = new JPanel(); labels.setOpaque(false);
        labels.setLayout(new javax.swing.BoxLayout(labels, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý thiết bị"); title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 25)); title.setForeground(MainFrame.TEXT);
        JLabel sub = new JLabel("Theo dõi thiết bị, vị trí, tình trạng và thông tin bảo trì."); sub.setForeground(MainFrame.MUTED);
        labels.add(title); labels.add(javax.swing.Box.createVerticalStrut(5)); labels.add(sub);
        JButton add = new JButton("+ Thêm thiết bị"); UIStyle.primaryButton(add); add.addActionListener(e -> open(null));
        p.add(labels, BorderLayout.WEST); p.add(add, BorderLayout.EAST); return p;
    }

    private JPanel body() {
        JPanel card = new JPanel(new BorderLayout(0, 12)); card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MainFrame.BORDER), BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        JPanel toolbar = new JPanel(new BorderLayout(8, 0)); toolbar.setOpaque(false);
        search.setPreferredSize(new Dimension(380, 36)); UIStyle.field(search); search.putClientProperty("JTextField.placeholderText", "Tìm mã, tên hoặc serial..."); search.addActionListener(e -> load());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); actions.setOpaque(false);
        UIStyle.secondaryButton(edit); UIStyle.secondaryButton(status); edit.setEnabled(false); status.setEnabled(false);
        edit.addActionListener(e -> open(selected())); status.addActionListener(e -> changeStatus());
        actions.add(edit); actions.add(status); toolbar.add(search, BorderLayout.CENTER); toolbar.add(actions, BorderLayout.EAST);
        UIStyle.table(table); table.setAutoCreateRowSorter(true); table.setRowHeight(40); table.getColumnModel().getColumn(7).setMinWidth(0); table.getColumnModel().getColumn(7).setMaxWidth(0); table.getColumnModel().getColumn(7).setWidth(0);
        table.getSelectionModel().addListSelectionListener(e -> updateActions());
        card.add(toolbar, BorderLayout.NORTH); card.add(new JScrollPane(table), BorderLayout.CENTER); return card;
    }

    private void load() {
        new SwingWorker<List<Device>, Void>() {
            protected List<Device> doInBackground() { return bus.find(session, search.getText()); }
            protected void done() { try { model.setRowCount(0); for (Device d : get()) model.addRow(new Object[]{d.id(),d.name(),d.typeName(),d.model(),d.serial(),d.location(),statusText(d.status()),d}); updateActions(); } catch (Exception e) { UIStyle.showError(DevicePanel.this, message(e)); } }
        }.execute();
    }

    private void open(Device existing) {
        try {
            List<TypeOption> types = bus.types(session);
            JTextField id = new JTextField(existing == null ? "" : existing.id()); id.setEditable(existing == null); UIStyle.field(id);
            JTextField name = new JTextField(existing == null ? "" : existing.name()); UIStyle.field(name);
            JTextField modelField = new JTextField(existing == null ? "" : safe(existing.model())); UIStyle.field(modelField);
            JTextField serial = new JTextField(existing == null ? "" : safe(existing.serial())); UIStyle.field(serial);
            JTextField manufacturer = new JTextField(existing == null ? "" : safe(existing.manufacturer())); UIStyle.field(manufacturer);
            JTextField location = new JTextField(existing == null ? "" : safe(existing.location())); UIStyle.field(location);
            JTextField purchase = new JTextField(existing == null || existing.purchaseDate() == null ? "" : existing.purchaseDate().toString()); UIStyle.field(purchase);
            JTextField inUse = new JTextField(existing == null || existing.inUseDate() == null ? "" : existing.inUseDate().toString()); UIStyle.field(inUse);
            JComboBox<TypeOption> type = new JComboBox<>(types.toArray(TypeOption[]::new));
            JComboBox<String> state = new JComboBox<>(new String[]{"Tot","DangSuDung","CanBaoTri","DangSuaChua","NgungSuDung","ThanhLy"});
            if (existing != null) { selectType(type, existing.typeId()); state.setSelectedItem(existing.status()); }
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8)); form.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            form.add(new JLabel("Mã thiết bị")); form.add(id); form.add(new JLabel("Tên thiết bị")); form.add(name); form.add(new JLabel("Loại thiết bị")); form.add(type); form.add(new JLabel("Model")); form.add(modelField); form.add(new JLabel("Serial")); form.add(serial); form.add(new JLabel("Hãng sản xuất")); form.add(manufacturer); form.add(new JLabel("Ngày mua (yyyy-MM-dd)")); form.add(purchase); form.add(new JLabel("Ngày sử dụng (yyyy-MM-dd)")); form.add(inUse); form.add(new JLabel("Vị trí")); form.add(location); form.add(new JLabel("Tình trạng")); form.add(state);
            int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Thêm thiết bị" : "Chỉnh sửa thiết bị", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            TypeOption selectedType = (TypeOption) type.getSelectedItem();
            bus.save(session, new DeviceDraft(id.getText(), name.getText(), selectedType == null ? null : selectedType.id(), modelField.getText(), serial.getText(), manufacturer.getText(), parseDate(purchase.getText()), parseDate(inUse.getText()), location.getText(), (String) state.getSelectedItem()));
            UIStyle.showSuccess(this, "Đã lưu thông tin thiết bị."); load();
        } catch (Exception e) { UIStyle.showError(this, message(e)); }
    }

    private void changeStatus() {
        Device d = selected(); if (d == null) return;
        String[] values = {"Tot","DangSuDung","CanBaoTri","DangSuaChua","NgungSuDung","ThanhLy"};
        String next = (String) JOptionPane.showInputDialog(this, "Chọn tình trạng mới:", "Cập nhật tình trạng", JOptionPane.PLAIN_MESSAGE, null, values, d.status());
        if (next != null) try { bus.updateStatus(session, d.id(), next); load(); } catch (Exception e) { UIStyle.showError(this, message(e)); }
    }

    private Device selected() { int row = table.getSelectedRow(); return row < 0 ? null : (Device) model.getValueAt(table.convertRowIndexToModel(row), 7); }
    private void updateActions() { boolean enabled = selected() != null; edit.setEnabled(enabled); status.setEnabled(enabled); }
    private static void selectType(JComboBox<TypeOption> box, String id) { for (int i=0;i<box.getItemCount();i++) if (id != null && id.equals(box.getItemAt(i).id())) { box.setSelectedIndex(i); return; } }
    private static LocalDate parseDate(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value.trim()); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String statusText(String value) { return switch (value == null ? "" : value) { case "Tot" -> "Tốt"; case "DangSuDung" -> "Đang sử dụng"; case "CanBaoTri" -> "Cần bảo trì"; case "DangSuaChua" -> "Đang sửa chữa"; case "NgungSuDung" -> "Ngừng sử dụng"; case "ThanhLy" -> "Thanh lý"; default -> value; }; }
    private static String message(Exception e) { Throwable c=e.getCause()==null?e:e.getCause(); return c.getMessage()==null?"Không thể thực hiện thao tác.":c.getMessage(); }
}
