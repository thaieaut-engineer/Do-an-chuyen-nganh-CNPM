package view;

import model.auth.AuthenticatedSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import view.component.AppIcon;

final class HeaderPanel extends JPanel {
    private final JLabel pageTitle = new JLabel("Tổng quan");

    HeaderPanel(AuthenticatedSession session, List<String> pages,
            Consumer<String> onNavigate, Runnable onLogout, Runnable onToggleSidebar) {
        super(new BorderLayout(18, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, MainFrame.BORDER),
                new EmptyBorder(13, 22, 13, 22)));
        setPreferredSize(new Dimension(0, 72));

        JButton sidebarToggle = iconButton("menu", "Thu gọn / mở rộng thanh điều hướng", 18);
        sidebarToggle.addActionListener(event -> onToggleSidebar.run());

        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pageTitle.setForeground(MainFrame.TEXT);
        JPanel titleArea = new JPanel(new BorderLayout(14, 0));
        titleArea.setOpaque(false);
        titleArea.add(sidebarToggle, BorderLayout.WEST);
        titleArea.add(pageTitle, BorderLayout.CENTER);

        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", "Tìm nhanh chức năng...");
        search.setPreferredSize(new Dimension(280, 36));
        search.addActionListener(event -> searchPage(search, pages, onNavigate));
        JButton searchButton = iconButton("search", "Tìm kiếm chức năng", 18);
        searchButton.addActionListener(event -> searchPage(search, pages, onNavigate));
        JPanel searchGroup = new JPanel(new BorderLayout(5, 0));
        searchGroup.setOpaque(false);
        searchGroup.add(search, BorderLayout.CENTER);
        searchGroup.add(searchButton, BorderLayout.EAST);

        JButton notifications = iconButton("bell", "Thông báo", 18);
        notifications.addActionListener(this::showNotifications);

        JButton account = new JButton(session.getEmployeeName(), new AppIcon("user", MainFrame.BLUE, 17));
        account.setToolTipText(session.getDisplayRole());
        account.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        account.setForeground(MainFrame.TEXT);
        account.setBackground(Color.WHITE);
        account.setBorder(new EmptyBorder(6, 6, 6, 4));
        account.setIconTextGap(8);
        account.setFocusPainted(false);
        account.addActionListener(event -> showAccountMenu(
                account, session.getEmployeeName(), session.getDisplayRole(), onLogout));

        JPanel actions = new JPanel(new BorderLayout(14, 0));
        actions.setOpaque(false);
        JPanel right = new JPanel(new BorderLayout(12, 0));
        right.setOpaque(false);
        if (session.hasPermission("TAIKHOAN_QUANLY")) {
            right.add(notifications, BorderLayout.WEST);
        }
        right.add(account, BorderLayout.EAST);
        actions.add(searchGroup, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);

        add(titleArea, BorderLayout.WEST);
        add(actions, BorderLayout.EAST);
    }

    void setPageTitle(String title) {
        pageTitle.setText(title);
    }

    private void searchPage(JTextField search, List<String> pages, Consumer<String> onNavigate) {
        String query = search.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            return;
        }
        for (String page : pages) {
            if (page.toLowerCase().contains(query)) {
                onNavigate.accept(page);
                search.setText("");
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Không tìm thấy chức năng phù hợp.",
                "Tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showNotifications(ActionEvent event) {
        JPopupMenu popup = new JPopupMenu();
        JLabel title = new JLabel("  Thông báo gần đây");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setBorder(new EmptyBorder(8, 4, 8, 12));
        popup.add(title);
        popup.add(notificationItem("calendar", "Lịch hẹn mới đang chờ xác nhận"));
        popup.add(notificationItem("equipment", "Có 2 thiết bị cần kiểm tra bảo dưỡng"));
        popup.add(notificationItem("medical", "Bản demo đang sử dụng dữ liệu mẫu"));
        popup.show((JButton) event.getSource(), 0, ((JButton) event.getSource()).getHeight());
    }

    private void showAccountMenu(JButton anchor, String username, String role, Runnable onLogout) {
        JPopupMenu menu = new JPopupMenu();
        JLabel user = new JLabel("  " + username + " · " + role);
        user.setIcon(new AppIcon("user", MainFrame.BLUE, 16));
        user.setBorder(new EmptyBorder(7, 8, 7, 8));
        menu.add(user);
        menu.addSeparator();
        javax.swing.JMenuItem logout = new javax.swing.JMenuItem(
                "Đăng xuất", new AppIcon("logout", MainFrame.MUTED, 16));
        logout.addActionListener(event -> onLogout.run());
        menu.add(logout);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private javax.swing.JMenuItem notificationItem(String icon, String text) {
        return new javax.swing.JMenuItem(text, new AppIcon(icon, MainFrame.TEAL, 15));
    }

    private JButton iconButton(String icon, String tooltip, int size) {
        JButton button = new JButton(new AppIcon(icon, MainFrame.MUTED, size));
        button.setToolTipText(tooltip);
        button.setForeground(MainFrame.MUTED);
        button.setBackground(Color.WHITE);
        button.setBorder(new EmptyBorder(5, 8, 5, 8));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        return button;
    }
}
