package view.auth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import view.MainFrame;
import view.component.AppIcon;

public final class LoginFrame extends JFrame {
    private static final Color BLUE = new Color(0x02B5AB);
    private static final Color TEXT = new Color(34, 48, 67);
    private static final Color MUTED = new Color(119, 132, 149);
    private static final Color BORDER = new Color(229, 234, 240);

    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private JButton loginButton;
    private JCheckBox remember;
    private static String rememberedUsername = "";
    private final JComboBox<String> role = new JComboBox<>(new String[]{
        "Quản trị viên", "Lễ tân", "Bác sĩ",
        "Thủ kho / Dược sĩ", "Thu ngân", "Quản lý phòng khám"
    });
    private final JLabel error = new JLabel(" ");

    public LoginFrame() {
        super("Đăng nhập | Phòng khám Thu Cúc");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setContentPane(createContent());
        getRootPane().setDefaultButton(loginButton);
        username.setText(rememberedUsername);
        remember.setSelected(!rememberedUsername.isEmpty());
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(242, 247, 252));

        JPanel card = new JPanel(new GridLayout(1, 2));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(860, 540));
        card.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(createBrandPanel());
        card.add(createFormPanel());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(24, 24, 24, 24);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        root.add(card, constraints);
        return root;
    }

    private JPanel createBrandPanel() {
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(new Color(18, 63, 72));
        brand.setBorder(new EmptyBorder(36, 32, 36, 32));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));

        JLabel mark = new JLabel(new AppIcon("medical", Color.WHITE, 34), SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(BLUE);
        mark.setForeground(Color.WHITE);
        mark.setPreferredSize(new Dimension(58, 58));
        mark.setMaximumSize(new Dimension(58, 58));
        mark.setAlignmentX(LEFT_ALIGNMENT);
        JLabel name = new JLabel("THU CÚC");
        name.setForeground(Color.WHITE);
        name.setFont(new Font("Segoe UI", Font.BOLD, 28));
        JLabel system = new JLabel("HỆ THỐNG QUẢN LÝ PHÒNG KHÁM");
        system.setForeground(new Color(206, 221, 238));
        system.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel description = new JLabel("<html>Quản lý lịch hẹn, hồ sơ bệnh nhân<br>"
                + "và hoạt động phòng khám trên một nền tảng.</html>");
        description.setForeground(new Color(220, 231, 244));
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JLabel demo = new JLabel("GIAO DIỆN BẢN DEMO · MOCK DATA");
        demo.setForeground(new Color(172, 199, 224));
        demo.setFont(new Font("Segoe UI", Font.BOLD, 9));

        content.add(mark);
        content.add(Box.vertical(24));
        content.add(name);
        content.add(Box.vertical(6));
        content.add(system);
        content.add(Box.vertical(22));
        content.add(description);
        content.add(Box.vertical(28));
        content.add(demo);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1;
        brand.add(content, constraints);
        return brand;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(34, 40, 30, 40));
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new javax.swing.BoxLayout(fields, javax.swing.BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Chào mừng trở lại");
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Đăng nhập để tiếp tục vào hệ thống");
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fields.add(title);
        fields.add(Box.vertical(7));
        fields.add(subtitle);
        fields.add(Box.vertical(25));

        fields.add(fieldLabel("Tên đăng nhập"));
        styleInput(username);
        username.putClientProperty("JTextField.placeholderText", "Nhập tên đăng nhập");
        fields.add(Box.vertical(7));
        fields.add(username);
        fields.add(Box.vertical(15));

        fields.add(fieldLabel("Mật khẩu"));
        styleInput(password);
        password.putClientProperty("JPasswordField.placeholderText", "Nhập mật khẩu");
        fields.add(Box.vertical(7));
        JPanel passwordRow = new JPanel(new BorderLayout(6, 0));
        passwordRow.setOpaque(false);
        passwordRow.add(password, BorderLayout.CENTER);
        JButton togglePassword = new JButton("Hiện", new AppIcon("eye", BLUE, 14));
        togglePassword.setFocusPainted(false);
        togglePassword.setForeground(BLUE);
        togglePassword.addActionListener(event -> togglePassword(togglePassword));
        passwordRow.add(togglePassword, BorderLayout.EAST);
        fields.add(passwordRow);
        fields.add(Box.vertical(15));

        fields.add(fieldLabel("Vai trò demo"));
        role.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        role.setPreferredSize(new Dimension(0, 38));
        fields.add(Box.vertical(7));
        fields.add(role);
        fields.add(Box.vertical(12));

        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        remember = new JCheckBox("Ghi nhớ đăng nhập");
        remember.setOpaque(false);
        remember.setForeground(MUTED);
        remember.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JButton forgot = new JButton("Quên mật khẩu?");
        forgot.setBorderPainted(false);
        forgot.setContentAreaFilled(false);
        forgot.setFocusPainted(false);
        forgot.setForeground(BLUE);
        forgot.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        forgot.addActionListener(event -> JOptionPane.showMessageDialog(
                this, "Vui lòng liên hệ quản trị viên để được hỗ trợ đặt lại mật khẩu.",
                "Quên mật khẩu", JOptionPane.INFORMATION_MESSAGE));
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);
        fields.add(options);
        fields.add(Box.vertical(9));

        error.setForeground(new Color(190, 62, 62));
        error.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fields.add(error);
        loginButton = createLoginButton();
        fields.add(loginButton);
        fields.add(Box.vertical(12));
        JLabel note = new JLabel("Bản demo: mọi tài khoản có đủ thông tin đều được chấp nhận.");
        note.setForeground(MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        fields.add(note);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        form.add(fields, constraints);
        return form;
    }

    private JButton createLoginButton() {
        JButton login = new JButton("Đăng nhập");
        login.setFont(new Font("Segoe UI", Font.BOLD, 14));
        login.setForeground(Color.WHITE);
        login.setBackground(BLUE);
        login.setOpaque(true);
        login.setBorderPainted(false);
        login.setFocusPainted(false);
        login.setPreferredSize(new Dimension(0, 42));
        login.addActionListener(event -> signIn());
        return login;
    }

    private void signIn() {
        String user = username.getText().trim();
        if (user.isEmpty() || password.getPassword().length == 0) {
            error.setText("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        rememberedUsername = remember.isSelected() ? user : "";
        new MainFrame(user, (String) role.getSelectedItem()).setVisible(true);
        dispose();
    }

    private void togglePassword(JButton toggle) {
        if (password.getEchoChar() == 0) {
            password.setEchoChar('\u2022');
            toggle.setText("Hiện");
        } else {
            password.setEchoChar((char) 0);
            toggle.setText("Ẩn");
        }
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private void styleInput(JTextField input) {
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setPreferredSize(new Dimension(0, 38));
    }

    private static final class Box {
        private static JPanel vertical(int height) {
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            spacer.setPreferredSize(new Dimension(1, height));
            spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            return spacer;
        }
    }
}
