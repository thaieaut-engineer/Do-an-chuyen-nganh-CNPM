package view;

import bus.patient.BenhNhanBUS;
import bus.patient.PatientException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.auth.AuthenticatedSession;
import model.patient.BenhNhan;

final class PatientDialog extends JDialog {
    enum Mode {
        CREATE,
        EDIT,
        DETAILS
    }

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthenticatedSession session;
    private final BenhNhanBUS patientBUS;
    private final BenhNhan patient;
    private final Mode mode;
    private final Consumer<String> onSaved;
    private final JTextField nameField = new JTextField();
    private final JTextField birthDateField = new JTextField();
    private final JComboBox<String> genderField =
            new JComboBox<>(new String[]{"", "Nam", "Nữ", "Khác"});
    private final JTextField documentField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField addressField = new JTextField();
    private final JLabel message = new JLabel(" ");
    private final JButton saveButton = new JButton();
    private boolean busy;

    PatientDialog(
            Frame owner,
            AuthenticatedSession session,
            BenhNhanBUS patientBUS,
            BenhNhan patient,
            Mode mode,
            Consumer<String> onSaved) {
        super(owner, title(mode), true);
        this.session = session;
        this.patientBUS = patientBUS;
        this.patient = patient;
        this.mode = mode;
        this.onSaved = onSaved;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(560, 560));
        setSize(600, 610);
        setLocationRelativeTo(owner);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                cancel();
            }
        });
        if (patient != null) {
            populateFields(patient);
        }
        if (mode == Mode.DETAILS) {
            addDetails();
        }
    }

    private static String title(Mode mode) {
        return switch (mode) {
            case CREATE -> "Thêm bệnh nhân";
            case EDIT -> "Cập nhật bệnh nhân";
            case DETAILS -> "Chi tiết bệnh nhân";
        };
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        if (mode != Mode.CREATE) {
            addRow(form, row++, "Mã bệnh nhân", readOnlyField(patient.maBenhNhan()));
        }
        addRow(form, row++, "Họ tên *", nameField);
        addRow(form, row++, "Ngày sinh", birthDateField);
        birthDateField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        addRow(form, row++, "Giới tính", genderField);
        addRow(form, row++, "Số giấy tờ", documentField);
        addRow(form, row++, "Số điện thoại", phoneField);
        addRow(form, row++, "Email", emailField);
        addRow(form, row++, "Địa chỉ", addressField);
        if (mode == Mode.DETAILS) {
            addRow(form, row++, "Ngày tạo hồ sơ",
                    readOnlyField(patient.ngayTaoHoSo() == null
                            ? "" : patient.ngayTaoHoSo().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            addRow(form, row, "Trạng thái", readOnlyField(statusLabel(patient.trangThai())));
        }

        boolean readOnly = mode == Mode.DETAILS;
        for (JTextField field : new JTextField[]{
            nameField, birthDateField, documentField, phoneField, emailField, addressField
        }) {
            field.setEnabled(!readOnly);
        }
        genderField.setEnabled(!readOnly);

        message.setForeground(new Color(190, 62, 62));
        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(message, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancelButton = new JButton(readOnly ? "Đóng" : "Hủy");
        cancelButton.addActionListener(event -> cancel());
        buttons.add(cancelButton);
        if (!readOnly) {
            saveButton.setText(mode == Mode.CREATE ? "Thêm bệnh nhân" : "Lưu thay đổi");
            saveButton.setForeground(Color.WHITE);
            saveButton.setBackground(MainFrame.BLUE);
            saveButton.setOpaque(true);
            saveButton.setBorderPainted(false);
            saveButton.setFocusPainted(false);
            saveButton.addActionListener(event -> save());
            buttons.add(saveButton);
        }
        footer.add(buttons, BorderLayout.EAST);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(readOnly ? cancelButton : saveButton);
        return root;
    }

    private void addRow(JPanel form, int row, String labelText, java.awt.Component field) {
        JLabel label = new JLabel(labelText);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(6, 0, 6, 14);
        form.add(label, labelConstraints);

        if (field instanceof JTextField textField) {
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            textField.setPreferredSize(new Dimension(0, 36));
        } else if (field instanceof JComboBox<?> comboBox) {
            comboBox.setPreferredSize(new Dimension(0, 36));
        }
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);
        form.add(field, fieldConstraints);
    }

    private JTextField readOnlyField(String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setBackground(new Color(248, 250, 252));
        return field;
    }

    private void populateFields(BenhNhan value) {
        nameField.setText(value.hoTen());
        birthDateField.setText(value.ngaySinh() == null ? "" : value.ngaySinh().toString());
        genderField.setSelectedItem(value.gioiTinh() == null ? "" : value.gioiTinh());
        documentField.setText(value.soGiayTo() == null ? "" : value.soGiayTo());
        phoneField.setText(value.soDienThoai() == null ? "" : value.soDienThoai());
        emailField.setText(value.email() == null ? "" : value.email());
        addressField.setText(value.diaChi() == null ? "" : value.diaChi());
    }

    private void addDetails() {
        setTitle(title(mode) + " · " + patient.maBenhNhan());
    }

    private String statusLabel(String status) {
        return "HoatDong".equals(status) ? "Đang hoạt động" : "Tạm khóa";
    }

    private void save() {
        if (busy) {
            return;
        }
        LocalDate birthDate;
        try {
            String value = birthDateField.getText().trim();
            birthDate = value.isEmpty() ? null : LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            message.setText("Ngày sinh phải theo định dạng YYYY-MM-DD.");
            return;
        }

        String gender = String.valueOf(genderField.getSelectedItem());
        BenhNhan updated = new BenhNhan(
                mode == Mode.CREATE ? null : patient.maBenhNhan(),
                nameField.getText(),
                birthDate,
                gender.isBlank() ? null : gender,
                documentField.getText(),
                phoneField.getText(),
                emailField.getText(),
                addressField.getText(),
                mode == Mode.EDIT ? patient.ngayTaoHoSo() : null,
                mode == Mode.EDIT ? patient.trangThai() : "HoatDong");

        setBusy(true);
        message.setForeground(MainFrame.MUTED);
        message.setText("Đang lưu hồ sơ...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws PatientException {
                if (mode == Mode.CREATE) {
                    return patientBUS.create(session, updated);
                }
                patientBUS.update(session, updated);
                return patient.maBenhNhan();
            }

            @Override
            protected void done() {
                try {
                    String patientId = get();
                    JOptionPane.showMessageDialog(
                            PatientDialog.this,
                            mode == Mode.CREATE
                                    ? "Đã thêm bệnh nhân với mã " + patientId + "."
                                    : "Thông tin bệnh nhân đã được cập nhật.",
                            title(mode),
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    onSaved.accept(patientId);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    message.setText("Quá trình lưu bị gián đoạn. Vui lòng thử lại.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    message.setText(cause instanceof PatientException
                            ? cause.getMessage()
                            : "Không thể lưu hồ sơ bệnh nhân. Vui lòng thử lại.");
                } finally {
                    if (isDisplayable()) {
                        setBusy(false);
                    }
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        saveButton.setEnabled(!value);
        saveButton.setText(value ? "Đang lưu..." : mode == Mode.CREATE ? "Thêm bệnh nhân" : "Lưu thay đổi");
    }

    private void cancel() {
        if (!busy) {
            dispose();
        }
    }
}
