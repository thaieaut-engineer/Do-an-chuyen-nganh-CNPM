package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import bus.auth.AuthorizationBUS;
import model.auth.AuthenticatedSession;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import view.component.AppIcon;

final class SidebarPanel extends JPanel {
    private static final Color MUTED_NAV = new Color(166, 183, 201);
    private static final Color ACTIVE = new Color(0x164D58);

    private final JPanel menu = new JPanel();
    private final JPanel brandText = new JPanel();
    private final JPanel footerText = new JPanel();
    private final Map<String, JButton> buttons = new LinkedHashMap<>();
    private final Map<String, JPanel> sectionMenus = new LinkedHashMap<>();
    private final Map<String, JButton> sectionToggles = new LinkedHashMap<>();
    private final Map<String, String> pageSections = new LinkedHashMap<>();
    private final List<String> availablePages = new ArrayList<>();
    private final AuthorizationBUS authorizationBUS = new AuthorizationBUS();
    private boolean collapsed;

    SidebarPanel(AuthenticatedSession session, Consumer<String> onNavigate) {
        super(new BorderLayout());
        setBackground(MainFrame.NAVY);
        setPreferredSize(new Dimension(248, 0));
        setBorder(new EmptyBorder(18, 12, 14, 12));

        JPanel brand = createBrand();
        add(brand, BorderLayout.NORTH);

        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        buildMenu(session, onNavigate);
        JScrollPane menuScroll = new JScrollPane(menu);
        menuScroll.setBorder(null);
        menuScroll.setOpaque(false);
        menuScroll.getViewport().setOpaque(false);
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(menuScroll, BorderLayout.CENTER);
        add(createFooter(session), BorderLayout.SOUTH);
    }

    List<String> getAvailablePages() {
        return List.copyOf(availablePages);
    }

    void setActivePage(String selected) {
        String section = pageSections.get(selected);
        if (section != null) {
            setSectionExpanded(section, true);
        }
        buttons.forEach((name, button) -> {
            boolean active = name.equals(selected);
            button.setOpaque(active);
            button.setBackground(active ? ACTIVE : MainFrame.NAVY);
            button.setForeground(active ? Color.WHITE : new Color(210, 221, 232));
            button.setIcon(new AppIcon(
                    button.getClientProperty("navIconName").toString(),
                    active ? MainFrame.BLUE : MUTED_NAV, 17));
        });
    }

    void toggleCollapsed() {
        collapsed = !collapsed;
        setPreferredSize(new Dimension(collapsed ? 82 : 248, 0));
        buttons.forEach((name, button) -> {
            button.setText(collapsed ? "" : name);
            button.setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
            button.setToolTipText(collapsed ? name : null);
        });
        sectionToggles.forEach((section, button) -> {
            button.setText(collapsed ? ">" : sectionToggleText(section));
            button.setHorizontalAlignment(collapsed
                    ? SwingConstants.CENTER : SwingConstants.LEFT);
            button.setToolTipText(collapsed ? section : null);
        });
        brandText.setVisible(!collapsed);
        footerText.setVisible(!collapsed);
        menu.revalidate();
        revalidate();
        getParent().revalidate();
    }

    private JPanel createBrand() {
        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(4, 8, 24, 4));
        JLabel mark = new JLabel(new AppIcon("medical", Color.WHITE, 24), SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(MainFrame.BLUE);
        mark.setPreferredSize(new Dimension(40, 40));

        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("THU CÚC");
        name.setForeground(Color.WHITE);
        name.setFont(new Font("Segoe UI", Font.BOLD, 17));
        JLabel caption = new JLabel("QUẢN LÝ PHÒNG KHÁM");
        caption.setForeground(MUTED_NAV);
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        brandText.add(name);
        brandText.add(Box.createVerticalStrut(3));
        brandText.add(caption);
        brand.add(mark, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);
        return brand;
    }

    private JPanel createFooter(AuthenticatedSession session) {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(15, 8, 3, 5));
        JLabel avatar = new JLabel(new AppIcon("user", new Color(205, 241, 238), 19), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(new Color(55, 81, 110));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        avatar.setPreferredSize(new Dimension(36, 36));
        footerText.setOpaque(false);
        footerText.setLayout(new BoxLayout(footerText, BoxLayout.Y_AXIS));
        JLabel preview = new JLabel(session.getEmployeeName());
        preview.setForeground(Color.WHITE);
        preview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        preview.setToolTipText(session.getEmployeeName());
        JLabel role = new JLabel(session.getDisplayRole());
        role.setForeground(MUTED_NAV);
        role.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        role.setToolTipText(session.getDisplayRole());
        footerText.add(preview);
        footerText.add(Box.createVerticalStrut(3));
        footerText.add(role);
        footer.add(avatar, BorderLayout.WEST);
        footer.add(footerText, BorderLayout.CENTER);
        return footer;
    }

    private void buildMenu(AuthenticatedSession session, Consumer<String> onNavigate) {
        List<MenuItem> items = List.of(
                new MenuItem("KHÁM BỆNH", null),
                new MenuItem("Tổng quan", "dashboard"),
                new MenuItem("Bệnh nhân", "patients"),
                new MenuItem("Lịch hẹn", "calendar"),
                new MenuItem("Tiếp nhận bệnh nhân", "add"),
                new MenuItem("Lượt khám", "medical"),
                new MenuItem("Phiếu khám", "clipboard"),
                new MenuItem("Chỉ định dịch vụ", "search"),
                new MenuItem("Kê đơn thuốc", "medicine"),
                new MenuItem("VẬN HÀNH", null),
                new MenuItem("Danh mục thuốc", "medicine"),
                new MenuItem("Kho thuốc", "box"),
                new MenuItem("Kho hóa chất", "flask"),
                new MenuItem("Viện phí", "invoice"),
                new MenuItem("Hóa đơn", "clipboard"),
                new MenuItem("Thanh toán", "wallet"),
                new MenuItem("Bảo hiểm y tế", "shield"),
                new MenuItem("QUẢN TRỊ", null),
                new MenuItem("Nhân viên", "patients"),
                new MenuItem("Bác sĩ", "doctor"),
                new MenuItem("Lịch làm việc", "calendar"),
                new MenuItem("Thiết bị", "equipment"),
                new MenuItem("Đánh giá nhân viên", "star"),
                new MenuItem("Tài khoản", "user"),
                new MenuItem("Phân quyền", "key"),
                new MenuItem("Cấu hình hệ thống", "settings"),
                new MenuItem("Nhật ký hệ thống", "report"),
                new MenuItem("Thông báo", "bell"),
                new MenuItem("Báo cáo", "report")
        );
        int index = 0;
        while (index < items.size()) {
            MenuItem heading = items.get(index++);
            if (heading.icon != null) {
                continue;
            }

            List<MenuItem> permittedItems = new ArrayList<>();
            while (index < items.size() && items.get(index).icon != null) {
                MenuItem item = items.get(index++);
                if (authorizationBUS.canAccessPage(session, item.label)) {
                    permittedItems.add(item);
                }
            }
            if (permittedItems.isEmpty()) {
                continue;
            }

            JPanel sectionMenu = new JPanel();
            sectionMenu.setOpaque(false);
            sectionMenu.setLayout(new BoxLayout(sectionMenu, BoxLayout.Y_AXIS));
            for (MenuItem item : permittedItems) {
                JButton button = new JButton(
                        item.label, new AppIcon(item.icon, MUTED_NAV, 17));
                button.putClientProperty("navIconName", item.icon);
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.setIconTextGap(12);
                button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                button.setForeground(new Color(210, 221, 232));
                button.setBackground(MainFrame.NAVY);
                button.setOpaque(false);
                button.setBorder(new EmptyBorder(9, 11, 9, 8));
                button.setFocusPainted(false);
                button.setAlignmentX(LEFT_ALIGNMENT);
                button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                button.addActionListener(event -> onNavigate.accept(item.label));
                buttons.put(item.label, button);
                pageSections.put(item.label, heading.label);
                availablePages.add(item.label);
                sectionMenu.add(button);
            }

            JButton toggle = new JButton(sectionToggleText(heading.label));
            toggle.setHorizontalAlignment(SwingConstants.LEFT);
            toggle.setFont(new Font("Segoe UI", Font.BOLD, 10));
            toggle.setForeground(MUTED_NAV);
            toggle.setBackground(MainFrame.NAVY);
            toggle.setOpaque(false);
            toggle.setBorder(new EmptyBorder(12, 11, 7, 8));
            toggle.setFocusPainted(false);
            toggle.setAlignmentX(LEFT_ALIGNMENT);
            toggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            toggle.addActionListener(event -> {
                JPanel itemsPanel = sectionMenus.get(heading.label);
                setSectionExpanded(
                        heading.label, !itemsPanel.isVisible());
            });
            sectionToggles.put(heading.label, toggle);
            sectionMenus.put(heading.label, sectionMenu);
            menu.add(toggle);
            menu.add(sectionMenu);
            setSectionExpanded(heading.label, false);
        }
    }

    private String sectionToggleText(String section) {
        JPanel sectionMenu = sectionMenus.get(section);
        return (sectionMenu != null && sectionMenu.isVisible() ? "-  " : "+  ") + section;
    }

    private void setSectionExpanded(String section, boolean expanded) {
        JPanel sectionMenu = sectionMenus.get(section);
        JButton toggle = sectionToggles.get(section);
        if (sectionMenu == null || toggle == null) {
            return;
        }
        if (expanded) {
            sectionMenus.forEach((name, itemsPanel) -> {
                if (!name.equals(section)) {
                    itemsPanel.setVisible(false);
                }
            });
        }
        sectionMenu.setVisible(expanded);
        sectionToggles.forEach((name, sectionToggle) ->
                sectionToggle.setText(collapsed ? ">" : sectionToggleText(name)));
        menu.revalidate();
        menu.repaint();
    }

    private static final class MenuItem {
        private final String label;
        private final String icon;

        private MenuItem(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }
    }
}
