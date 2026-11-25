package view;

import bus.admin.AdminBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import view.component.UIStyle;

final class AdminPanel extends JPanel {
    private final AuthenticatedSession session;
    private final String module;
    private final AdminBUS adminBUS = new AdminBUS();
    private final JTextField search = new JTextField();
    private final JLabel count = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final JPanel tableHost = new JPanel(new BorderLayout());
    private final JButton refresh = new JButton("Làm mới");

    AdminPanel(AuthenticatedSession session, String module) {
        this.session = session;
        this.module = module;
        setLayout(new BorderLayout(0, 16));
        setBackground(MainFrame.PAGE);
        setBorder(BorderFactory.createEmptyBorder(25, 28, 30, 28));
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        load();
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setOpaque(false);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(module);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel description = new JLabel(description());
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        description.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(javax.swing.Box.createVerticalStrut(5));
        labels.add(description);

        UIStyle.secondaryButton(refresh);
        refresh.addActionListener(event -> load());
        panel.add(labels, BorderLayout.WEST);
        panel.add(refresh, BorderLayout.EAST);
        return panel;
    }

    private String description() {
        return switch (module) {
            case "Phân quyền" -> "Tra cứu vai trò và các quyền đang được cấp.";
            case "Thiết bị" -> "Theo dõi thiết bị, vị trí và tình trạng vận hành.";
            case "Đánh giá nhân viên" -> "Tra cứu kết quả đánh giá theo nhân viên và kỳ đánh giá.";
            case "Cấu hình hệ thống" -> "Tra cứu các tham số cấu hình đang áp dụng.";
            case "Nhật ký hệ thống" -> "Theo dõi lịch sử thao tác và sự kiện bảo mật.";
            case "Thông báo" -> "Theo dõi thông báo đã tạo, trạng thái và kênh gửi.";
            default -> "Tổng hợp các chỉ số vận hành của phòng khám.";
        };
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        search.setPreferredSize(new Dimension(420, 36));
        UIStyle.field(search);
        search.putClientProperty("JTextField.placeholderText", "Tìm kiếm dữ liệu...");
        search.addActionListener(event -> load());
        count.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        count.setForeground(MainFrame.MUTED);
        toolbar.add(search, BorderLayout.CENTER);
        toolbar.add(count, BorderLayout.EAST);

        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        message.setForeground(MainFrame.MUTED);
        tableHost.setBackground(Color.WHITE);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(tableHost, BorderLayout.CENTER);
        body.add(message, BorderLayout.SOUTH);
        return body;
    }

    private void load() {
        if (!refresh.isEnabled()) {
            return;
        }
        refresh.setEnabled(false);
        message.setText("Đang tải dữ liệu...");
        new SwingWorker<dao.admin.AdminDAO.Result, Void>() {
            @Override
            protected dao.admin.AdminDAO.Result doInBackground() {
                return adminBUS.search(session, module, search.getText());
            }

            @Override
            protected void done() {
                try {
                    dao.admin.AdminDAO.Result result = get();
                    DefaultTableModel model = new DefaultTableModel(result.columns(), 0) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    };
                    for (Object[] row : result.rows()) {
                        model.addRow(row);
                    }
                    JTable table = new JTable(model);
                    UIStyle.table(table);
                    table.setAutoCreateRowSorter(true);
                    tableHost.removeAll();
                    tableHost.add(new JScrollPane(table), BorderLayout.CENTER);
                    count.setText(result.rows().size() + " bản ghi");
                    message.setText("Dữ liệu đã được cập nhật.");
                    tableHost.revalidate();
                    tableHost.repaint();
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null
                            ? exception : exception.getCause();
                    message.setText("Không thể tải dữ liệu.");
                    UIStyle.showError(AdminPanel.this,
                            cause.getMessage() == null
                                    ? "Không thể tải dữ liệu quản trị."
                                    : cause.getMessage());
                } finally {
                    refresh.setEnabled(true);
                }
            }
        }.execute();
    }
}
