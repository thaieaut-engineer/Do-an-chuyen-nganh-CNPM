package view;

import bus.reception.TiepNhanBUS;
import bus.reception.TiepNhanException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.appointment.AppointmentDoctor;
import model.auth.AuthenticatedSession;
import model.reception.ReceptionAppointment;
import model.reception.ReceptionStats;
import model.reception.ReceptionVisit;

final class ReceptionPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final TiepNhanBUS receptionBUS;
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<DoctorChoice> doctorFilter = new JComboBox<>();
    private final JSpinner appointmentDate = new JSpinner(new SpinnerDateModel());
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton clearButton = new JButton("Xóa bộ lọc");
    private final JButton receiveButton = new JButton("Tiếp nhận");
    private final JLabel todayLabel = new JLabel("—");
    private final JLabel waitingLabel = new JLabel("—");
    private final JLabel receivedLabel = new JLabel("—");
    private final JLabel cancelledLabel = new JLabel("—");
    private final JTable appointmentTable = new JTable();
    private final JTable visitTable = new JTable();
    private final DefaultTableModel appointmentModel = new DefaultTableModel(
            new Object[]{"Mã lịch", "Mã BN", "Họ tên", "SĐT", "Bác sĩ", "Chuyên khoa", "Thời gian hẹn", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel visitModel = new DefaultTableModel(
            new Object[]{"Mã lượt", "Mã lịch", "Mã BN", "Họ tên", "Bác sĩ", "Thời gian tiếp nhận", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private List<ReceptionAppointment> pendingAppointments = List.of();
    private List<ReceptionVisit> receivedVisits = List.of();

    ReceptionPanel(AuthenticatedSession session, TiepNhanBUS receptionBUS) {
        this.session = session;
        this.receptionBUS = receptionBUS;
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(24, 28, 28, 28));
        add(createContent(), BorderLayout.CENTER);
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(appointmentDate, "dd/MM/yyyy");
        appointmentDate.setEditor(dateEditor);
        appointmentDate.setValue(today);
        ((SpinnerDateModel) appointmentDate.getModel()).setStart(today);
        initTable();
        loadReferences();
        loadDefaultDate();
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(Box.createVerticalStrut(16));
        content.add(createStats());
        content.add(Box.createVerticalStrut(16));
        content.add(createFilters());
        content.add(Box.createVerticalStrut(16));
        content.add(createPendingSection());
        content.add(Box.createVerticalStrut(16));
        content.add(createReceivedSection());
        return content;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel("Tiếp nhận bệnh nhân");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel description = new JLabel("Quản lý bệnh nhân đến khám và tạo lượt khám từ lịch hẹn hợp lệ.");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        description.setForeground(MainFrame.MUTED);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(Box.createVerticalStrut(5));
        labels.add(description);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.addActionListener(event -> reloadData());
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearButton.addActionListener(event -> clearFilters());
        actions.add(refreshButton);
        actions.add(clearButton);
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    private JPanel createStats() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(card("Số lịch hẹn hôm nay", todayLabel));
        stats.add(card("Chờ tiếp nhận", waitingLabel));
        stats.add(card("Đã tiếp nhận", receivedLabel));
        stats.add(card("Đã hủy / vắng", cancelledLabel));
        return stats;
    }

    private JPanel card(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(16, 16, 14, 16)));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MainFrame.MUTED);
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(MainFrame.TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFilters() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 16, 14, 16)));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        searchField.putClientProperty("JTextField.placeholderText", "Mã lịch, mã bệnh nhân, tên, số điện thoại");
        top.add(searchField, BorderLayout.CENTER);
        JButton searchButton = new JButton("Tìm kiếm");
        searchButton.addActionListener(event -> reloadData());
        top.add(searchButton, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bottom.setOpaque(false);
        JLabel dateLabel = new JLabel("Ngày");
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottom.add(dateLabel);
        appointmentDate.setPreferredSize(new Dimension(120, 32));
        bottom.add(appointmentDate);
        JLabel statusLabel = new JLabel("Trạng thái");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottom.add(statusLabel);
        statusFilter.setPreferredSize(new Dimension(145, 32));
        bottom.add(statusFilter);
        JLabel doctorLabel = new JLabel("Bác sĩ");
        doctorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottom.add(doctorLabel);
        doctorFilter.setPreferredSize(new Dimension(180, 32));
        bottom.add(doctorFilter);
        bottom.add(receiveButton);
        receiveButton.addActionListener(event -> openSelectedReception());
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createPendingSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JLabel title = new JLabel("Danh sách bệnh nhân chờ tiếp nhận");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(MainFrame.TEXT);
        panel.add(title, BorderLayout.NORTH);

        appointmentTable.setModel(appointmentModel);
        appointmentTable.setRowHeight(36);
        appointmentTable.setFillsViewportHeight(true);
        appointmentTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.setBackground(Color.WHITE);
        appointmentTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        appointmentTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                receiveButton.setEnabled(appointmentTable.getSelectedRow() >= 0);
            }
        });

        JScrollPane scroll = new JScrollPane(appointmentTable);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.setPreferredSize(new Dimension(0, 260));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReceivedSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JLabel title = new JLabel("Danh sách bệnh nhân đã tiếp nhận");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(MainFrame.TEXT);
        panel.add(title, BorderLayout.NORTH);

        visitTable.setModel(visitModel);
        visitTable.setRowHeight(36);
        visitTable.setFillsViewportHeight(true);
        visitTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        visitTable.setBackground(Color.WHITE);
        visitTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(visitTable);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.setPreferredSize(new Dimension(0, 180));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void initTable() {
        receiveButton.setForeground(Color.WHITE);
        receiveButton.setBackground(MainFrame.BLUE);
        receiveButton.setOpaque(true);
        receiveButton.setBorderPainted(false);
        receiveButton.setEnabled(false);
    }

    private void clearFilters() {
        searchField.setText("");
        statusFilter.setSelectedIndex(0);
        doctorFilter.setSelectedIndex(0);
        appointmentDate.setValue(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        reloadData();
    }

    private void loadReferences() {
        new SwingWorker<ReferenceData, Void>() {
            @Override
            protected ReferenceData doInBackground() throws TiepNhanException {
                List<String> statuses = receptionBUS.getStatuses(session);
                List<AppointmentDoctor> doctors = receptionBUS.getDoctors(session);
                return new ReferenceData(statuses, doctors);
            }

            @Override
            protected void done() {
                try {
                    ReferenceData data = get();
                    statusFilter.removeAllItems();
                    statusFilter.addItem("Tất cả");
                    for (String status : data.statuses()) {
                        statusFilter.addItem(status);
                    }
                    doctorFilter.removeAllItems();
                    doctorFilter.addItem(new DoctorChoice("", "Tất cả bác sĩ"));
                    for (AppointmentDoctor doctor : data.doctors()) {
                        doctorFilter.addItem(new DoctorChoice(
                                doctor.maNhanVien(),
                                doctor.hoTen() + (doctor.tenChuyenKhoa() == null ? "" : " - " + doctor.tenChuyenKhoa())));
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    Throwable rootCause = exception.getCause() != null ? exception.getCause() : exception;
                    String detail = rootCause.getMessage() != null && !rootCause.getMessage().isBlank()
                            ? rootCause.getMessage()
                            : "Không thể tải dữ liệu tham chiếu.";
                    JOptionPane.showMessageDialog(ReceptionPanel.this,
                            detail,
                            "Tiếp nhận bệnh nhân",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void reloadData() {
        new SwingWorker<DataBundle, Void>() {
            @Override
            protected DataBundle doInBackground() throws TiepNhanException {
                LocalDate selectedDate = selectedDate();
                String selectedStatus = selectedStatus();
                String doctorId = selectedDoctorId();
                ReceptionStats stats = receptionBUS.getStats(session);
                List<ReceptionAppointment> pending = receptionBUS.getPendingAppointments(
                        session, searchField.getText(), selectedStatus, selectedDate, doctorId);
                List<ReceptionVisit> visits = receptionBUS.getReceivedVisits(
                        session, searchField.getText(), selectedDate);
                return new DataBundle(stats, pending, visits);
            }

            @Override
            protected void done() {
                try {
                    DataBundle data = get();
                    todayLabel.setText(String.valueOf(data.stats().totalToday()));
                    waitingLabel.setText(String.valueOf(data.stats().waitingForReception()));
                    receivedLabel.setText(String.valueOf(data.stats().receivedToday()));
                    cancelledLabel.setText(String.valueOf(data.stats().cancelledOrNoShow()));
                    pendingAppointments = data.pending();
                    receivedVisits = data.visits();
                    renderPending();
                    renderReceived();
                    receiveButton.setEnabled(appointmentTable.getSelectedRow() >= 0);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(ReceptionPanel.this,
                            "Tải dữ liệu tiếp nhận bị gián đoạn.",
                            "Tiếp nhận bệnh nhân",
                            JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException exception) {
                    Throwable rootCause = exception.getCause() != null ? exception.getCause() : exception;
                    String detail = rootCause.getMessage() != null && !rootCause.getMessage().isBlank()
                            ? rootCause.getMessage()
                            : "Không thể tải dữ liệu tiếp nhận.";
                    JOptionPane.showMessageDialog(ReceptionPanel.this,
                            detail,
                            "Tiếp nhận bệnh nhân",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadDefaultDate() {
        new SwingWorker<LocalDate, Void>() {
            @Override
            protected LocalDate doInBackground() throws TiepNhanException {
                return receptionBUS.getLatestAppointmentDate(session);
            }

            @Override
            protected void done() {
                try {
                    LocalDate latestDate = get();
                    if (latestDate != null) {
                        appointmentDate.setValue(Date.from(latestDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    }
                    reloadData();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    reloadData();
                } catch (ExecutionException exception) {
                    reloadData();
                }
            }
        }.execute();
    }

    private void renderPending() {
        appointmentModel.setRowCount(0);
        for (ReceptionAppointment appointment : pendingAppointments) {
            appointmentModel.addRow(new Object[]{
                    appointment.maLichHen(),
                    appointment.maBenhNhan(),
                    appointment.hoTenBenhNhan(),
                    appointment.soDienThoaiBenhNhan(),
                    safe(appointment.tenBacSi()),
                    safe(appointment.tenChuyenKhoa()),
                    formatDateTime(appointment.thoiGianHen()),
                    safe(appointment.trangThai())
            });
        }
        if (pendingAppointments.isEmpty()) {
            appointmentModel.addRow(new Object[]{"—", "—", "—", "—", "—", "—", "—", "Không có lịch hẹn trong ngày đã chọn"});
        }
    }

    private void renderReceived() {
        visitModel.setRowCount(0);
        for (ReceptionVisit visit : receivedVisits) {
            visitModel.addRow(new Object[]{
                    visit.maLuotKham(),
                    visit.maLichHen(),
                    visit.maBenhNhan(),
                    visit.hoTenBenhNhan(),
                    safe(visit.tenBacSi()),
                    formatDateTime(visit.thoiGianTiepNhan()),
                    safe(visit.trangThai())
            });
        }
        if (receivedVisits.isEmpty()) {
            visitModel.addRow(new Object[]{"—", "—", "—", "—", "—", "—", "Chưa có lượt khám nào"});
        }
    }

    private void openSelectedReception() {
        int row = appointmentTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        long appointmentId = ((Number) appointmentTable.getValueAt(row, 0)).longValue();
        new ReceptionDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                session, receptionBUS, appointmentId).setVisible(true);
        reloadData();
    }

    private LocalDate selectedDate() {
        Date date = (Date) appointmentDate.getValue();
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String selectedStatus() {
        Object item = statusFilter.getSelectedItem();
        return item == null || "Tất cả".equals(String.valueOf(item)) ? "" : String.valueOf(item);
    }

    private String selectedDoctorId() {
        DoctorChoice selected = (DoctorChoice) doctorFilter.getSelectedItem();
        return selected == null ? "" : selected.id();
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "—" : time.format(DATE_TIME_FORMAT);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private record DoctorChoice(String id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record DataBundle(ReceptionStats stats, List<ReceptionAppointment> pending, List<ReceptionVisit> visits) {
    }

    private record ReferenceData(List<String> statuses, List<AppointmentDoctor> doctors) {
    }
}
