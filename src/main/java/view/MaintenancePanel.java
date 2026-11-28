package view;

import bus.device.MaintenanceBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.device.MaintenanceModels.*;
import view.component.UIStyle;

final class MaintenancePanel extends JPanel {
    private final AuthenticatedSession session;
    private final MaintenanceBUS bus = new MaintenanceBUS();
    private final boolean schedule;
    private final JTextField search = new JTextField();
    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = new JTable(model);
    private final JButton edit = new JButton("Chỉnh sửa");
    private final JButton cancel = new JButton("Hủy");

    MaintenancePanel(AuthenticatedSession session, boolean schedule) {
        this.session = session; this.schedule = schedule;
        setLayout(new BorderLayout(0, 12)); setBackground(MainFrame.PAGE);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        add(header(), BorderLayout.NORTH); add(body(), BorderLayout.CENTER); load();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        JLabel title = new JLabel(schedule ? "Lịch bảo dưỡng thiết bị" : "Phiếu sửa chữa thiết bị");
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20)); title.setForeground(MainFrame.TEXT);
        JButton add = new JButton("+ Thêm mới"); UIStyle.primaryButton(add); add.addActionListener(e -> open(null));
        p.add(title, BorderLayout.WEST); p.add(add, BorderLayout.EAST); return p;
    }

    private JPanel body() {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MainFrame.BORDER), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        search.setPreferredSize(new Dimension(360, 34)); UIStyle.field(search); search.putClientProperty("JTextField.placeholderText", "Tìm mã thiết bị hoặc nội dung..."); search.addActionListener(e -> load());
        JButton refresh = new JButton("Làm mới"); UIStyle.secondaryButton(refresh); refresh.addActionListener(e -> load());
        UIStyle.secondaryButton(edit); UIStyle.dangerButton(cancel); edit.setEnabled(false); cancel.setEnabled(false);
        edit.addActionListener(e -> open(selected())); cancel.addActionListener(e -> remove());
        table.getSelectionModel().addListSelectionListener(e -> { boolean on = selected() != null; edit.setEnabled(on); cancel.setEnabled(on); });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); actions.setOpaque(false); actions.add(edit); actions.add(cancel); actions.add(refresh);
        JPanel top = new JPanel(new BorderLayout(8, 0)); top.setOpaque(false); top.add(search, BorderLayout.CENTER); top.add(actions, BorderLayout.EAST);
        UIStyle.table(table); table.setAutoCreateRowSorter(true); p.add(top, BorderLayout.NORTH); p.add(new JScrollPane(table), BorderLayout.CENTER); return p;
    }

    private void load() {
        new SwingWorker<List<?>, Void>() {
            protected List<?> doInBackground() { return schedule ? bus.schedules(session, search.getText()) : bus.repairs(session, search.getText()); }
            protected void done() {
                try { List<?> rows = get(); if (schedule) setSchedules(rows); else setRepairs(rows); }
                catch (Exception e) { UIStyle.showError(MaintenancePanel.this, message(e)); }
            }
        }.execute();
    }

    private void setSchedules(List<?> rows) {
        model.setDataVector(new Object[0][8], new Object[]{"Mã", "Thiết bị", "Ngày dự kiến", "Ngày thực hiện", "Nội dung", "Nhân viên", "Trạng thái", "__row"});
        for (Object o : rows) { Schedule x = (Schedule) o; model.addRow(new Object[]{x.id(), x.deviceId()+" - "+x.deviceName(), x.expectedDate(), x.completedDate(), x.content(), x.employeeId(), x.status(), x}); }
        hideRowColumn();
    }

    private void setRepairs(List<?> rows) {
        model.setDataVector(new Object[0][8], new Object[]{"Mã", "Thiết bị", "Ngày báo", "Mô tả sự cố", "Ngày sửa", "Nhân viên", "Trạng thái", "__row"});
        for (Object o : rows) { Repair x = (Repair) o; model.addRow(new Object[]{x.id(), x.deviceId()+" - "+x.deviceName(), x.reportedDate(), x.description(), x.repairedDate(), x.employeeId(), x.status(), x}); }
        hideRowColumn();
    }

    private void hideRowColumn() { table.getColumnModel().getColumn(7).setMinWidth(0); table.getColumnModel().getColumn(7).setMaxWidth(0); table.getColumnModel().getColumn(7).setWidth(0); }

    private void open(Object old) {
        try {
            JTextField device = field(old == null ? "" : schedule ? ((Schedule) old).deviceId() : ((Repair) old).deviceId());
            JTextField date = field(old == null ? LocalDate.now().toString() : schedule ? String.valueOf(((Schedule) old).expectedDate()) : String.valueOf(((Repair) old).repairedDate()));
            JTextField completed = field(old == null ? "" : schedule ? String.valueOf(((Schedule) old).completedDate() == null ? "" : ((Schedule) old).completedDate()) : "");
            JTextField description = field(old == null ? "" : schedule ? safe(((Schedule) old).content()) : safe(((Repair) old).description()));
            JTextField vendor = field(old == null ? "" : schedule ? safe(((Schedule) old).vendor()) : safe(((Repair) old).vendor()));
            JTextField employee = field(old == null ? "" : schedule ? safe(((Schedule) old).employeeId()) : safe(((Repair) old).employeeId()));
            JTextField cost = field(old == null ? "" : money(schedule ? ((Schedule) old).cost() : ((Repair) old).cost()));
            JTextField note = field(old == null ? "" : schedule ? "" : safe(((Repair) old).note()));
            JComboBox<String> state = new JComboBox<>(schedule ? new String[]{"LenLich", "DaThucHien", "QuaHan", "Huy"} : new String[]{"MoiBao", "DangXuLy", "DaSua", "KhongTheSua", "Huy"});
            if (old != null) state.setSelectedItem(schedule ? ((Schedule) old).status() : ((Repair) old).status());
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            add(form, "Mã thiết bị", device); add(form, schedule ? "Ngày dự kiến (yyyy-MM-dd)" : "Ngày sửa (yyyy-MM-dd)", date);
            if (schedule) add(form, "Ngày thực hiện (yyyy-MM-dd)", completed);
            add(form, schedule ? "Nội dung" : "Mô tả sự cố", description); add(form, "Đơn vị thực hiện", vendor); add(form, schedule ? "Nhân viên phụ trách" : "Nhân viên tiếp nhận", employee); add(form, "Chi phí", cost);
            if (!schedule) add(form, "Ghi chú", note); add(form, "Trạng thái", state);
            if (UIStyle.showFormDialog(this, form, schedule ? "Lịch bảo dưỡng" : "Phiếu sửa chữa", new Dimension(520, 390)) != JOptionPane.OK_OPTION) return;
            Long id = old == null ? null : schedule ? ((Schedule) old).id() : ((Repair) old).id();
            BigDecimal amount = cost.getText().isBlank() ? null : new BigDecimal(cost.getText().trim()); String status = (String) state.getSelectedItem();
            if (schedule) bus.save(session, new ScheduleDraft(id, device.getText(), LocalDate.parse(date.getText().trim()), parseDate(completed.getText()), description.getText(), vendor.getText(), employee.getText(), amount, status));
            else bus.save(session, new RepairDraft(id, device.getText(), description.getText(), parseDate(date.getText()), vendor.getText(), amount, status, employee.getText(), note.getText()));
            load();
        } catch (Exception e) { UIStyle.showError(this, message(e)); }
    }

    private Object selected() { int r = table.getSelectedRow(); return r < 0 ? null : model.getValueAt(table.convertRowIndexToModel(r), 7); }
    private void remove() { Object old = selected(); if (old == null || JOptionPane.showConfirmDialog(this, "Chuyển bản ghi sang trạng thái Hủy?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return; try { bus.delete(session, schedule ? ((Schedule) old).id() : ((Repair) old).id(), schedule); load(); } catch (Exception e) { UIStyle.showError(this, message(e)); } }
    private static void add(JPanel p, String label, JComponent value) { p.add(new JLabel(label)); p.add(value); }
    private static JTextField field(String value) { JTextField f = new JTextField(value == null || "null".equals(value) ? "" : value); UIStyle.field(f); return f; }
    private static LocalDate parseDate(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value.trim()); }
    private static String money(BigDecimal value) { return value == null ? "" : value.toPlainString(); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String message(Exception e) { Throwable c = e.getCause() == null ? e : e.getCause(); return c.getMessage() == null ? "Không thể thực hiện thao tác." : c.getMessage(); }
}
