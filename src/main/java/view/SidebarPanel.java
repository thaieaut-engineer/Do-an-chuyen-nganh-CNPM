package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
    private final List<JLabel> sectionLabels = new ArrayList<>();
    private final List<String> availablePages = new ArrayList<>();
    private boolean collapsed;

    SidebarPanel(String role, Consumer<String> onNavigate) {
        super(new BorderLayout());
        setBackground(MainFrame.NAVY);
        setPreferredSize(new Dimension(248, 0));
        setBorder(new EmptyBorder(18, 12, 14, 12));

        JPanel brand = createBrand();
        add(brand, BorderLayout.NORTH);

        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        buildMenu(role, onNavigate);
        JScrollPane menuScroll = new JScrollPane(menu);
        menuScroll.setBorder(null);
        menuScroll.setOpaque(false);
        menuScroll.getViewport().setOpaque(false);
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(menuScroll, BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    List<String> getAvailablePages() {
        return List.copyOf(availablePages);
    }

    void setActivePage(String selected) {
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
        sectionLabels.forEach(label -> label.setVisible(!collapsed));
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

    private JPanel createFooter() {
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
        JLabel preview = new JLabel("Giao diện demo");
        preview.setForeground(Color.WHITE);
        preview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel role = new JLabel("Mock Data");
        role.setForeground(MUTED_NAV);
        role.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerText.add(preview);
        footerText.add(Box.createVerticalStrut(3));
        footerText.add(role);
        footer.add(avatar, BorderLayout.WEST);
        footer.add(footerText, BorderLayout.CENTER);
        return footer;
    }

    private void buildMenu(String role, Consumer<String> onNavigate) {
        List<MenuItem> items = List.of(
                new MenuItem("KHÁM BỆNH", null, SetOf.all()),
                new MenuItem("Tổng quan", "dashboard", SetOf.all()),
                new MenuItem("Bệnh nhân", "patients", SetOf.roles("Quản trị viên", "Lễ tân",
                        "Bác sĩ", "Thu ngân")),
                new MenuItem("Lịch hẹn", "calendar", SetOf.roles("Quản trị viên", "Lễ tân",
                        "Bác sĩ")),
                new MenuItem("Tiếp nhận bệnh nhân", "add", SetOf.roles("Quản trị viên", "Lễ tân")),
                new MenuItem("Lượt khám", "medical", SetOf.roles("Quản trị viên", "Lễ tân", "Bác sĩ")),
                new MenuItem("Phiếu khám", "clipboard", SetOf.roles("Quản trị viên", "Bác sĩ")),
                new MenuItem("Chỉ định dịch vụ", "search", SetOf.roles("Quản trị viên", "Bác sĩ")),
                new MenuItem("Kê đơn thuốc", "medicine", SetOf.roles("Quản trị viên", "Bác sĩ")),
                new MenuItem("VẬN HÀNH", null, SetOf.all()),
                new MenuItem("Danh mục thuốc", "medicine", SetOf.roles("Quản trị viên", "Thủ kho / Dược sĩ")),
                new MenuItem("Kho thuốc", "box", SetOf.roles("Quản trị viên", "Thủ kho / Dược sĩ")),
                new MenuItem("Kho hóa chất", "flask", SetOf.roles("Quản trị viên", "Thủ kho / Dược sĩ")),
                new MenuItem("Viện phí", "invoice", SetOf.roles("Quản trị viên", "Thu ngân")),
                new MenuItem("Hóa đơn", "clipboard", SetOf.roles("Quản trị viên", "Thu ngân")),
                new MenuItem("Thanh toán", "wallet", SetOf.roles("Quản trị viên", "Thu ngân")),
                new MenuItem("Bảo hiểm y tế", "shield", SetOf.roles("Quản trị viên", "Thu ngân")),
                new MenuItem("QUẢN TRỊ", null, SetOf.all()),
                new MenuItem("Nhân viên", "patients", SetOf.roles("Quản trị viên", "Quản lý phòng khám")),
                new MenuItem("Bác sĩ", "doctor", SetOf.roles("Quản trị viên", "Quản lý phòng khám")),
                new MenuItem("Lịch làm việc", "calendar", SetOf.roles("Quản trị viên", "Quản lý phòng khám")),
                new MenuItem("Thiết bị", "equipment", SetOf.roles("Quản trị viên", "Quản lý phòng khám")),
                new MenuItem("Đánh giá nhân viên", "star",
                        SetOf.roles("Quản trị viên", "Quản lý phòng khám")),
                new MenuItem("Tài khoản", "user", SetOf.roles("Quản trị viên")),
                new MenuItem("Phân quyền", "key", SetOf.roles("Quản trị viên")),
                new MenuItem("Cấu hình hệ thống", "settings", SetOf.roles("Quản trị viên")),
                new MenuItem("Nhật ký hệ thống", "report", SetOf.roles("Quản trị viên")),
                new MenuItem("Thông báo", "bell", SetOf.roles("Quản trị viên")),
                new MenuItem("Báo cáo", "report", SetOf.roles("Quản trị viên", "Quản lý phòng khám"))
        );
        boolean isAdmin = role.equals("Quản trị viên");
        for (MenuItem item : items) {
            if (item.icon == null) {
                if (isAdmin || items.stream().anyMatch(candidate ->
                        candidate.icon != null && candidate.roles.contains(role)
                                && belongsToSection(candidate, item.label, items))) {
                    menu.add(sectionLabel(item.label));
                }
                continue;
            }
            if (!item.roles.contains(role)) {
                continue;
            }
            JButton button = new JButton(item.label, new AppIcon(item.icon, MUTED_NAV, 17));
            button.putClientProperty("navIconName", item.icon);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIconTextGap(12);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            button.setForeground(new Color(210, 221, 232));
            button.setBackground(MainFrame.NAVY);
            button.setOpaque(false);
            button.setBorder(new EmptyBorder(10, 11, 10, 8));
            button.setFocusPainted(false);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.addActionListener(event -> onNavigate.accept(item.label));
            buttons.put(item.label, button);
            availablePages.add(item.label);
            button.setIcon(new AppIcon(item.icon, MUTED_NAV, 17));
            menu.add(button);
        }
    }

    private boolean belongsToSection(MenuItem item, String section, List<MenuItem> items) {
        int sectionIndex = items.indexOf(items.stream()
                .filter(candidate -> candidate.label.equals(section)).findFirst().orElse(null));
        int itemIndex = items.indexOf(item);
        int nextSection = items.size();
        for (int i = sectionIndex + 1; i < items.size(); i++) {
            if (items.get(i).icon == null) {
                nextSection = i;
                break;
            }
        }
        return itemIndex > sectionIndex && itemIndex < nextSection;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_NAV);
        label.setFont(new Font("Segoe UI", Font.BOLD, 9));
        label.setBorder(new EmptyBorder(13, 11, 6, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        sectionLabels.add(label);
        return label;
    }

    private static final class MenuItem {
        private final String label;
        private final String icon;
        private final List<String> roles;

        private MenuItem(String label, String icon, List<String> roles) {
            this.label = label;
            this.icon = icon;
            this.roles = roles;
        }
    }

    private static final class SetOf {
        private static List<String> all() {
            return Arrays.asList("Quản trị viên", "Lễ tân", "Bác sĩ",
                    "Thủ kho / Dược sĩ", "Thu ngân", "Quản lý phòng khám");
        }

        private static List<String> roles(String... roles) {
            return Arrays.asList(roles);
        }
    }
}
