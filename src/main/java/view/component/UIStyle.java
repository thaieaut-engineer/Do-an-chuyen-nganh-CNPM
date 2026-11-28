package view.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;

/** Bộ quy chuẩn hiển thị dùng chung cho các màn hình của hệ thống. */
public final class UIStyle {
    public static final Color PRIMARY = new Color(0x02B5AB);
    public static final Color PRIMARY_DARK = new Color(0x008F88);
    public static final Color SUCCESS = new Color(0x2E8B68);
    public static final Color WARNING = new Color(0xB4771A);
    public static final Color DANGER = new Color(0xC44D5A);
    public static final Color INFO = new Color(0x3C79B8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SUBTLE = new Color(0xF8FAFC);
    public static final Color BORDER = new Color(0xE3E9EF);
    public static final Color TEXT = new Color(0x223043);
    public static final Color MUTED = new Color(0x778495);
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 12);

    private UIStyle() {
    }

    public static void applyGlobalDefaults() {
        javax.swing.UIManager.put("defaultFont", BODY_FONT);
        javax.swing.UIManager.put("Label.font", BODY_FONT);
        javax.swing.UIManager.put("Button.font", LABEL_FONT);
        javax.swing.UIManager.put("TextField.font", BODY_FONT);
        javax.swing.UIManager.put("ComboBox.font", BODY_FONT);
        javax.swing.UIManager.put("Table.font", BODY_FONT);
        javax.swing.UIManager.put("TableHeader.font", LABEL_FONT);
        javax.swing.UIManager.put("Table.rowHeight", 40);
        javax.swing.UIManager.put("Table.showGrid", false);
        javax.swing.UIManager.put("ScrollBar.width", 11);
        javax.swing.UIManager.put("Component.arc", 10);
        javax.swing.UIManager.put("Button.arc", 10);
        javax.swing.UIManager.put("TextComponent.arc", 8);
        javax.swing.UIManager.put("Component.focusColor", PRIMARY);
        javax.swing.UIManager.put("Component.focusedBorderColor", PRIMARY);
        javax.swing.UIManager.put("Button.hoverBackground", new Color(0xDDF5F2));
        javax.swing.UIManager.put("Button.pressedBackground", new Color(0xBEEBE7));
        javax.swing.UIManager.put("TextField.placeholderForeground", new Color(0xA0ACB8));
        javax.swing.UIManager.put("Table.alternateRowColor", new Color(0xFBFCFD));
        javax.swing.UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
        javax.swing.UIManager.put("TableHeader.separatorColor", BORDER);
        javax.swing.UIManager.put("ScrollBar.track", new Color(0xF4F7F9));
        javax.swing.UIManager.put("ScrollBar.thumb", new Color(0xC7D2DC));
        javax.swing.UIManager.put("ScrollBar.hoverThumbColor", new Color(0xAEBBC7));
        javax.swing.UIManager.put("ScrollBar.pressedThumbColor", PRIMARY);
        javax.swing.UIManager.put("ToolTip.background", TEXT);
        javax.swing.UIManager.put("ToolTip.foreground", Color.WHITE);
        javax.swing.UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 9, 6, 9));
        javax.swing.UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        javax.swing.UIManager.put("OptionPane.background", SURFACE);
        javax.swing.UIManager.put("OptionPane.messageForeground", TEXT);
        javax.swing.UIManager.put("OptionPane.messageFont", BODY_FONT);
        javax.swing.UIManager.put("OptionPane.buttonFont", LABEL_FONT);
        javax.swing.UIManager.put("OptionPane.minimumSize", new java.awt.Dimension(420, 150));
        javax.swing.UIManager.put("OptionPane.buttonPadding", 12);
        javax.swing.UIManager.put("Panel.background", SURFACE);
        javax.swing.UIManager.put("TabbedPane.selectedBackground", SURFACE);
        javax.swing.UIManager.put("TabbedPane.underlineColor", PRIMARY);
    }

    public static void primaryButton(JButton button) {
        button.setFont(LABEL_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setMargin(new Insets(10, 16, 10, 16));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "focusWidth: 0; hoverBackground: #0AAEA5; pressedBackground: #008F88");
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    public static void secondaryButton(JButton button) {
        button.setFont(LABEL_FONT);
        button.setForeground(TEXT);
        button.setBackground(SURFACE);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 14, 8, 14)));
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "focusWidth: 0; hoverBackground: #F1F8F8");
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    public static void dangerButton(JButton button) {
        primaryButton(button);
        button.setBackground(DANGER);
    }

    public static void field(JTextField field) {
        field.setFont(BODY_FONT);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 11, 8, 11)));
        field.putClientProperty("FlatLaf.style", "focusWidth: 1; focusedBorderColor: #02B5AB");
    }

    public static void table(JTable table) {
        table.setFont(BODY_FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setSelectionBackground(new Color(0xDDF5F2));
        table.setSelectionForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setRowHeight(42);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new java.awt.Dimension(0, 42));
        header.setBackground(SUBTLE);
        header.setForeground(MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setReorderingAllowed(false);
    }

    public static JLabel messageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(BODY_FONT);
        label.setBorder(new EmptyBorder(7, 2, 7, 2));
        return label;
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thông tin", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Cần kiểm tra", JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Không thể thực hiện", JOptionPane.ERROR_MESSAGE);
    }

    /** Hiển thị form dialog có kích thước ổn định và có thể cuộn. */
    public static int showFormDialog(Component parent, javax.swing.JPanel form, String title, java.awt.Dimension size) {
        form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(size);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        return JOptionPane.showConfirmDialog(parent, scroll, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    public static String friendlyError(Throwable error, String fallback) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                String message = current.getMessage();
                if (message.toLowerCase().contains("duplicate")
                        || message.toLowerCase().contains("unique")) {
                    return "Dữ liệu đã tồn tại. Vui lòng kiểm tra lại thông tin nhập.";
                }
                if (message.toLowerCase().contains("foreign key")) {
                    return "Dữ liệu đang được sử dụng ở phân hệ khác nên chưa thể thay đổi.";
                }
                return message;
            }
            current = current.getCause();
        }
        return fallback;
    }
}
