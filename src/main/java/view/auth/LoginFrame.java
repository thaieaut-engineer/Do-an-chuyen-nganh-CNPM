package view.auth;

import bus.auth.AuthenticationBUS;
import bus.auth.AuthenticationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.auth.AuthenticatedSession;
import view.MainFrame;
import view.component.AppIcon;

public final class LoginFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());
    private static final Color BLUE = new Color(0x02B5AB);
    private static final Color TEXT = new Color(34, 48, 67);
    private static final Color MUTED = new Color(119, 132, 149);
    private static final Color BORDER = new Color(229, 234, 240);

    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final AuthenticationBUS authenticationBUS = new AuthenticationBUS();
    private JButton loginButton;
    private JCheckBox remember;
    private static String rememberedUsername = "";
    private final JLabel error = new JLabel(" ");

    public LoginFrame() {
        super("Đăng nhập | Phòng khám Thu Cúc");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setMinimumSize(new Dimension(1080, 680));
        setLocationRelativeTo(null);
        setContentPane(createContent());
        getRootPane().setDefaultButton(loginButton);
        username.setText(rememberedUsername);
        remember.setSelected(!rememberedUsername.isEmpty());
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel form = createFormPanel();
        form.setPreferredSize(new Dimension(420, 0));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
                new EmptyBorder(36, 34, 32, 34)));
        root.add(form, BorderLayout.WEST);
        root.add(new WelcomePanel(), BorderLayout.CENTER);
        return root;
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new javax.swing.BoxLayout(fields, javax.swing.BoxLayout.Y_AXIS));
        fields.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel title = new JLabel("Đăng nhập hệ thống");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Đăng nhập để tiếp tục vào hệ thống");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fields.add(title);
        fields.add(Box.vertical(7));
        fields.add(subtitle);
        fields.add(Box.vertical(25));

        JLabel usernameLabel = fieldLabel("Tên đăng nhập");
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(usernameLabel);
        styleInput(username);
        username.putClientProperty("JTextField.placeholderText", "Nhập tên đăng nhập");
        fields.add(Box.vertical(7));
        fields.add(username);
        fields.add(Box.vertical(15));

        JLabel passwordLabel = fieldLabel("Mật khẩu");
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(passwordLabel);
        styleInput(password);
        password.putClientProperty("JPasswordField.placeholderText", "Nhập mật khẩu");
        fields.add(Box.vertical(7));
        JPanel passwordRow = new JPanel(new BorderLayout(6, 0));
        passwordRow.setOpaque(false);
        passwordRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordRow.add(password, BorderLayout.CENTER);
        JButton togglePassword = new JButton("Hiện", new AppIcon("eye", BLUE, 14));
        togglePassword.setFocusPainted(false);
        togglePassword.setForeground(BLUE);
        togglePassword.addActionListener(event -> togglePassword(togglePassword));
        passwordRow.add(togglePassword, BorderLayout.EAST);
        fields.add(passwordRow);
        fields.add(Box.vertical(15));

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
        options.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                options.getPreferredSize().height));
        fields.add(options);
        fields.add(Box.vertical(9));

        error.setForeground(new Color(190, 62, 62));
        error.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        error.setMaximumSize(new Dimension(Integer.MAX_VALUE, error.getPreferredSize().height));
        fields.add(error);
        loginButton = createLoginButton();
        fields.add(loginButton);
        fields.add(Box.vertical(12));
        JLabel note = new JLabel("Sử dụng tài khoản được quản trị viên cấp.");
        note.setForeground(MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        login.setAlignmentX(Component.LEFT_ALIGNMENT);
        login.addActionListener(event -> signIn());
        return login;
    }

    private void signIn() {
        String user = username.getText().trim();
        char[] passwordValue = password.getPassword();
        if (user.isEmpty() || passwordValue.length == 0) {
            Arrays.fill(passwordValue, '\0');
            showMessage("Vui lòng nhập tên đăng nhập và mật khẩu.", false);
            return;
        }

        setBusy(true);
        showMessage("Đang xác thực tài khoản...", true);
        new SwingWorker<AuthenticatedSession, Void>() {
            @Override
            protected AuthenticatedSession doInBackground() throws Exception {
                try {
                    return authenticationBUS.authenticate(user, passwordValue);
                } finally {
                    Arrays.fill(passwordValue, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    AuthenticatedSession session = get();
                    rememberedUsername = remember.isSelected() ? user : "";
                    new MainFrame(session).setVisible(true);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showMessage("Quá trình đăng nhập bị gián đoạn. Vui lòng thử lại.", false);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof AuthenticationException) {
                        showMessage(cause.getMessage(), false);
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected error while signing in.", cause);
                        showMessage("Không thể hoàn tất đăng nhập. Vui lòng thử lại.", false);
                    }
                } finally {
                    if (isDisplayable()) {
                        password.setText("");
                        setBusy(false);
                    }
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        username.setEnabled(!busy);
        password.setEnabled(!busy);
        remember.setEnabled(!busy);
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Đang xử lý..." : "Đăng nhập");
    }

    private void showMessage(String message, boolean processing) {
        error.setForeground(processing ? BLUE : new Color(190, 62, 62));
        error.setText(message);
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
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    private static final class WelcomePanel extends JPanel {
        private final ImageIcon background;

        private WelcomePanel() {
            setOpaque(false);
            URL imageUrl = LoginFrame.class.getResource("/images/thucuc.jpeg");
            if (imageUrl == null) {
                throw new IllegalStateException("Không tìm thấy ảnh nền /images/thucuc.jpeg.");
            }
            background = new ImageIcon(imageUrl);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            if (panelWidth <= 0 || panelHeight <= 0) {
                return;
            }
            double scale = Math.max(
                    (double) panelWidth / 900,
                    (double) panelHeight / 700);
            int width = (int) Math.ceil(panelWidth / scale);
            int height = (int) Math.ceil(panelHeight / scale);
            Graphics2D g2 = (Graphics2D) graphics.create();
            Path2D rightRoundedClip = new Path2D.Float();
            rightRoundedClip.moveTo(0, 0);
            rightRoundedClip.lineTo(panelWidth - 24, 0);
            rightRoundedClip.quadTo(panelWidth, 0, panelWidth, 24);
            rightRoundedClip.lineTo(panelWidth, panelHeight - 24);
            rightRoundedClip.quadTo(
                    panelWidth, panelHeight, panelWidth - 24, panelHeight);
            rightRoundedClip.lineTo(0, panelHeight);
            rightRoundedClip.closePath();
            g2.clip(rightRoundedClip);
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(background.getImage(), 0, 0, width, height,
                    0, 0, 900, 700, this);

            java.awt.GradientPaint overlay = new java.awt.GradientPaint(
                    0, panelHeight * 0.48f, new Color(20, 42, 91, 0),
                    0, panelHeight, new Color(16, 34, 78, 220));
            g2.setPaint(overlay);
            g2.fillRect(0, 0, panelWidth, panelHeight);

            int left = 44;
            int bottom = panelHeight - 48;
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
            g2.drawString("Chào mừng trở lại", left, bottom - 44);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g2.setColor(new Color(240, 246, 255));
            g2.drawString("Hệ thống quản lý phòng khám Thu Cúc", left, bottom - 14);
            g2.dispose();
        }
    }
}
