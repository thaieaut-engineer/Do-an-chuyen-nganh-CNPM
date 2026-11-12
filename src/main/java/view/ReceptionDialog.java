package view;

import bus.reception.TiepNhanBUS;
import bus.reception.TiepNhanException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.auth.AuthenticatedSession;
import model.reception.ReceptionAppointment;

final class ReceptionDialog extends JDialog {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final TiepNhanBUS receptionBUS;
    private final long appointmentId;
    private final JLabel message = new JLabel(" ");
    private final JButton confirmButton = new JButton("Xác nhận tiếp nhận");
    private final JButton cancelButton = new JButton("Hủy");
    private final JTextArea patientInfo = new JTextArea(8, 28);
    private final JTextArea appointmentInfo = new JTextArea(8, 28);
    private final JTextArea noteArea = new JTextArea(3, 30);
    private boolean busy;

    ReceptionDialog(Frame owner, AuthenticatedSession session, TiepNhanBUS receptionBUS, long appointmentId) {
        super(owner, "Tiếp nhận bệnh nhân", true);
        this.session = session;
        this.receptionBUS = receptionBUS;
        this.appointmentId = appointmentId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(680, 590));
        setSize(720, 640);
        setLocationRelativeTo(owner);
        reloadDetails();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 22, 16, 22));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 0, 8, 12);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        JLabel patientLabel = new JLabel("Thông tin bệnh nhân");
        patientLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        patientLabel.setForeground(MainFrame.TEXT);
        fields.add(patientLabel, c);
        c.gridx = 1;
        JLabel appointmentLabel = new JLabel("Thông tin lịch hẹn");
        appointmentLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        appointmentLabel.setForeground(MainFrame.TEXT);
        fields.add(appointmentLabel, c);

        c.gridy = 1;
        c.gridx = 0;
        c.weighty = 1;
        patientInfo.setEditable(false);
        patientInfo.setLineWrap(true);
        patientInfo.setWrapStyleWord(true);
        patientInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(8, 8, 8, 8)));
        patientInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fields.add(new JScrollPane(patientInfo), c);

        c.gridx = 1;
        appointmentInfo.setEditable(false);
        appointmentInfo.setLineWrap(true);
        appointmentInfo.setWrapStyleWord(true);
        appointmentInfo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(8, 8, 8, 8)));
        appointmentInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fields.add(new JScrollPane(appointmentInfo), c);

        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weighty = 0;
        JLabel noteLabel = new JLabel("Ghi chú tiếp nhận");
        noteLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        noteLabel.setForeground(MainFrame.TEXT);
        fields.add(noteLabel, c);
        c.gridy = 3;
        noteArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(8, 8, 8, 8)));
        noteArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fields.add(new JScrollPane(noteArea), c);

        message.setForeground(new Color(190, 62, 62));
        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(message, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        cancelButton.addActionListener(event -> dispose());
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setBackground(MainFrame.BLUE);
        confirmButton.setOpaque(true);
        confirmButton.setBorderPainted(false);
        confirmButton.addActionListener(event -> confirmReception());
        buttons.add(cancelButton);
        buttons.add(confirmButton);
        footer.add(buttons, BorderLayout.EAST);

        root.add(fields, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private void reloadDetails() {
        setBusy(true, "Đang tải thông tin tiếp nhận...");
        new SwingWorker<ReceptionAppointment, Void>() {
            @Override
            protected ReceptionAppointment doInBackground() throws TiepNhanException {
                return receptionBUS.getAppointment(session, appointmentId);
            }

            @Override
            protected void done() {
                try {
                    ReceptionAppointment appointment = get();
                    patientInfo.setText(
                            "Mã bệnh nhân: " + appointment.maBenhNhan() + "\n"
                            + "Họ tên: " + safe(appointment.hoTenBenhNhan()) + "\n"
                            + "Ngày sinh: " + "\n"
                            + "Giới tính: " + "\n"
                            + "Số điện thoại: " + safe(appointment.soDienThoaiBenhNhan()) + "\n"
                            + "Địa chỉ: " + "\n"
                            + "Email: " + "");
                    appointmentInfo.setText(
                            "Mã lịch hẹn: " + appointment.maLichHen() + "\n"
                            + "Thời gian hẹn: " + formatTime(appointment.thoiGianHen()) + "\n"
                            + "Bác sĩ: " + safe(appointment.tenBacSi()) + "\n"
                            + "Chuyên khoa: " + safe(appointment.tenChuyenKhoa()) + "\n"
                            + "Lý do khám: " + safe(appointment.lyDoKham()) + "\n"
                            + "Trạng thái: " + safe(appointment.trangThai()));
                    setBusy(false, "Sẵn sàng tiếp nhận bệnh nhân.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Tải chi tiết lịch hẹn bị gián đoạn.");
                } catch (ExecutionException exception) {
                    setBusy(false, exception.getCause() instanceof TiepNhanException
                            ? exception.getCause().getMessage() : "Không thể tải chi tiết lịch hẹn.");
                }
            }
        }.execute();
    }

    private void confirmReception() {
        if (busy) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Xác nhận bệnh nhân đã đến và tạo lượt khám cho lịch hẹn " + appointmentId + "?",
                "Tiếp nhận bệnh nhân",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        setBusy(true, "Đang tạo lượt khám...");
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws TiepNhanException {
                return receptionBUS.receivePatient(session, appointmentId);
            }

            @Override
            protected void done() {
                try {
                    long visitId = get();
                    JOptionPane.showMessageDialog(
                            ReceptionDialog.this,
                            "Tiếp nhận thành công. Mã lượt khám: " + visitId,
                            "Tiếp nhận bệnh nhân",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Quá trình tiếp nhận bị gián đoạn.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    setBusy(false, cause instanceof TiepNhanException
                            ? cause.getMessage()
                            : "Không thể tiếp nhận bệnh nhân.");
                }
            }
        }.execute();
    }

    private void setBusy(boolean value, String text) {
        busy = value;
        confirmButton.setEnabled(!value);
        cancelButton.setEnabled(!value);
        message.setForeground(value ? MainFrame.MUTED : new Color(190, 62, 62));
        message.setText(text);
    }

    private String formatTime(java.time.LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_FORMAT);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
