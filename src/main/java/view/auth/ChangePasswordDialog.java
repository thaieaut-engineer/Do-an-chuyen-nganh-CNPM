package view.auth;

import bus.auth.AuthenticationBUS;
import bus.auth.AuthenticationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.auth.AuthenticatedSession;

public final class ChangePasswordDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(ChangePasswordDialog.class.getName());
    private static final Color ACCENT = new Color(0x02B5AB);
    private static final Color TEXT = new Color(34, 48, 67);
    private static final Color MUTED = new Color(119, 132, 149);

    private final AuthenticatedSession session;
    private final AuthenticationBUS authenticationBUS;
    private final JPasswordField currentPassword = new JPasswordField();
    private final JPasswordField newPassword = new JPasswordField();
    private final JPasswordField confirmation = new JPasswordField();
    private final JLabel message = new JLabel(" ");
    private final JButton confirmButton = new JButton("Xác nhận");
    private final JButton cancelButton = new JButton("Hủy");
    private boolean busy;

    public ChangePasswordDialog(
            Frame owner,
            AuthenticatedSession session,
            AuthenticationBUS authenticationBUS) {
        super(owner, "Đổi mật khẩu", true);
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Phiên đăng nhập không còn hoạt động.");
        }
        this.session = session;
        this.authenticationBUS = authenticationBUS;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(470, 370));
        setSize(500, 400);
        setLocationRelativeTo(owner);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                cancel();
            }
        });
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(26, 30, 24, 30));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addPasswordRow(fields, 0, "Mật khẩu hiện tại", currentPassword);
        addPasswordRow(fields, 1, "Mật khẩu mới", newPassword);
        addPasswordRow(fields, 2, "Xác nhận mật khẩu mới", confirmation);

        JCheckBox showPasswords = new JCheckBox("Hiện mật khẩu");
        showPasswords.setOpaque(false);
        showPasswords.setForeground(MUTED);
        showPasswords.addActionListener(event -> {
            char echo = showPasswords.isSelected() ? (char) 0 : '\u2022';
            currentPassword.setEchoChar(echo);
            newPassword.setEchoChar(echo);
            confirmation.setEchoChar(echo);
        });
        GridBagConstraints visibility = new GridBagConstraints();
        visibility.gridx = 1;
        visibility.gridy = 3;
        visibility.anchor = GridBagConstraints.WEST;
        visibility.insets = new Insets(3, 0, 10, 0);
        fields.add(showPasswords, visibility);

        message.setForeground(new Color(190, 62, 62));
        message.setFont(message.getFont().deriveFont(12f));
        GridBagConstraints messageConstraints = new GridBagConstraints();
        messageConstraints.gridx = 0;
        messageConstraints.gridy = 4;
        messageConstraints.gridwidth = 2;
        messageConstraints.fill = GridBagConstraints.HORIZONTAL;
        messageConstraints.insets = new Insets(4, 0, 5, 0);
        fields.add(message, messageConstraints);
        root.add(fields, BorderLayout.CENTER);

        confirmButton.setForeground(Color.WHITE);
        confirmButton.setBackground(ACCENT);
        confirmButton.setOpaque(true);
        confirmButton.setBorderPainted(false);
        confirmButton.addActionListener(event -> submit());
        cancelButton.addActionListener(event -> cancel());
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(confirmButton);
        root.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(confirmButton);
        return root;
    }

    private void addPasswordRow(JPanel fields, int row, String labelText, JPasswordField field) {
        JLabel label = new JLabel(labelText);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(7, 0, 5, 14);
        fields.add(label, labelConstraints);

        field.setFont(field.getFont().deriveFont(13f));
        field.setPreferredSize(new Dimension(250, 36));
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        fieldConstraints.insets = new Insets(7, 0, 5, 0);
        fields.add(field, fieldConstraints);
    }

    private void submit() {
        char[] current = currentPassword.getPassword();
        char[] replacement = newPassword.getPassword();
        char[] repeated = confirmation.getPassword();
        setBusy(true);
        message.setForeground(ACCENT);
        message.setText("Đang cập nhật mật khẩu...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws AuthenticationException {
                try {
                    authenticationBUS.changePassword(session, current, replacement, repeated);
                    return null;
                } finally {
                    Arrays.fill(current, '\0');
                    Arrays.fill(replacement, '\0');
                    Arrays.fill(repeated, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    clearPasswordFields();
                    JOptionPane.showMessageDialog(
                            ChangePasswordDialog.this,
                            "Mật khẩu đã được cập nhật thành công.",
                            "Đổi mật khẩu",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure("Quá trình đổi mật khẩu bị gián đoạn. Vui lòng thử lại.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof AuthenticationException authException) {
                        message.setText(authException.getMessage());
                    } else {
                        LOGGER.log(Level.SEVERE, "Unexpected error while changing password.", cause);
                        message.setText("Không thể đổi mật khẩu. Vui lòng thử lại.");
                    }
                    clearPasswordFields();
                } finally {
                    if (isDisplayable()) {
                        setBusy(false);
                    }
                }
            }
        }.execute();
    }

    private void showFailure(String text, Exception exception) {
        LOGGER.log(Level.WARNING, "Password change was interrupted.", exception);
        message.setText(text);
        clearPasswordFields();
    }

    private void setBusy(boolean value) {
        busy = value;
        currentPassword.setEnabled(!value);
        newPassword.setEnabled(!value);
        confirmation.setEnabled(!value);
        confirmButton.setEnabled(!value);
        cancelButton.setEnabled(!value);
        confirmButton.setText(value ? "Đang xử lý..." : "Xác nhận");
    }

    private void cancel() {
        if (busy) {
            return;
        }
        clearPasswordFields();
        dispose();
    }

    private void clearPasswordFields() {
        currentPassword.setText("");
        newPassword.setText("");
        confirmation.setText("");
    }
}
