package view;

import bus.appointment.AppointmentException;
import bus.appointment.LichHenBUS;
import bus.patient.BenhNhanBUS;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentFilter;
import model.appointment.AppointmentPage;
import model.appointment.AppointmentSpecialty;
import model.appointment.AppointmentStats;
import model.appointment.LichHen;
import model.auth.AuthenticatedSession;

final class AppointmentPanel extends JPanel {
    private static final Logger LOGGER =
            Logger.getLogger(AppointmentPanel.class.getName());
    private static final int PAGE_SIZE = 15;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final AuthenticatedSession session;
    private final LichHenBUS appointmentBUS;
    private final BenhNhanBUS patientBUS;
    private final JTextField searchField = new JTextField();
    private final JComboBox<DateChoice> dateFilter = new JComboBox<>(DateChoice.values());
    private final JSpinner customDate = new JSpinner(new javax.swing.SpinnerDateModel());
    private final JComboBox<StatusChoice> statusFilter = new JComboBox<>();
    private final JComboBox<SpecialtyChoice> specialtyFilter = new JComboBox<>();
    private final JComboBox<DoctorChoice> doctorFilter = new JComboBox<>();
    private final JLabel totalStat = new JLabel("—");
    private final JLabel pendingStat = new JLabel("—");
    private final JLabel confirmedStat = new JLabel("—");
    private final JLabel cancelledStat = new JLabel("—");
    private final JLabel resultCount = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JLabel message = new JLabel(" ");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton clearFiltersButton = new JButton("Xóa bộ lọc");
    private final JButton addButton = new JButton("+ Đặt lịch hẹn");
    private final JButton previousButton = new JButton("Trước");
    private final JButton nextButton = new JButton("Sau");
    private final JButton detailsButton = new JButton("Xem chi tiết");
    private final JButton editButton = new JButton("Chỉnh sửa");
    private final JButton confirmButton = new JButton("Xác nhận");
    private final JButton cancelButton = new JButton("Hủy lịch");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã lịch", "Thời gian hẹn", "Bệnh nhân", "Điện thoại",
                "Bác sĩ", "Chuyên khoa", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final Timer searchTimer;
    private List<AppointmentSpecialty> specialties = List.of();
    private List<AppointmentDoctor> doctors = List.of();
    private List<String> statuses = List.of();
    private LocalDate databaseDate = LocalDate.now();
    private int currentPage;
    private int totalPages = 1;
    private boolean loading;
    private boolean reloadAfterLoad;
    private boolean loadingReferences;
    private boolean suppressFilterEvents;
    private boolean customDateChanged;

    AppointmentPanel(
            AuthenticatedSession session,
            LichHenBUS appointmentBUS,
            BenhNhanBUS patientBUS) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Không thể mở lịch hẹn khi chưa đăng nhập.");
        }
        this.session = session;
        this.appointmentBUS = appointmentBUS;
        this.patientBUS = patientBUS;
        searchTimer = new Timer(350, event -> resetAndLoad());
        searchTimer.setRepeats(false);
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(24, 28, 28, 28));
        add(createContent(), BorderLayout.NORTH);
        loadReferences();
        refreshData();
    }

    void refreshData() {
        loadStatistics();
        loadAppointments();
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(gap(18));
        content.add(createStatistics());
        content.add(gap(18));
        content.add(createListCard());
        return content;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new javax.swing.BoxLayout(labels, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý lịch hẹn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel subtitle = new JLabel("Theo dõi và quản lý lịch khám của bệnh nhân.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(gap(5));
        labels.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton.addActionListener(event -> refreshData());
        actions.add(refreshButton);
        if (canManage()) {
            addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            addButton.setForeground(Color.WHITE);
            addButton.setBackground(MainFrame.BLUE);
            addButton.setOpaque(true);
            addButton.setBorderPainted(false);
            addButton.setFocusPainted(false);
            addButton.addActionListener(event -> openEditor(null));
            actions.add(addButton);
        }
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    private JPanel createStatistics() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(statCard("Lịch hôm nay", totalStat, new Color(0xE8F7F5)));
        cards.add(statCard("Chờ xác nhận", pendingStat, new Color(0xFFF5E5)));
        cards.add(statCard("Đã xác nhận", confirmedStat, new Color(0xEAF2FF)));
        cards.add(statCard("Đã hủy", cancelledStat, new Color(0xFDEEEE)));
        cards.setPreferredSize(new Dimension(0, 90));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        return cards;
    }

    private JPanel statCard(String title, JLabel value, Color background) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel caption = new JLabel(title);
        caption.setForeground(MainFrame.MUTED);
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        value.setOpaque(true);
        value.setBackground(background);
        value.setForeground(MainFrame.TEXT);
        value.setFont(new Font("Segoe UI", Font.BOLD, 19));
        value.setHorizontalAlignment(SwingConstants.CENTER);
        value.setBorder(new EmptyBorder(3, 10, 3, 10));
        card.add(caption, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel createListCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 14, 12, 14)));
        card.add(createToolbar(), BorderLayout.NORTH);
        table.setRowHeight(39);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(MainFrame.TEXT);
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(226, 248, 246));
        table.setSelectionForeground(MainFrame.TEXT);
        int[] widths = {90, 145, 190, 130, 170, 150, 125};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActions();
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showDetails();
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(MainFrame.MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setReorderingAllowed(false);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 430));
        card.add(tableScroll, BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel createToolbar() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);
        JPanel filters = new JPanel();
        filters.setOpaque(false);
        filters.setLayout(new javax.swing.BoxLayout(filters, javax.swing.BoxLayout.Y_AXIS));
        JPanel dateFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        dateFilters.setOpaque(false);
        searchField.setPreferredSize(new Dimension(245, 34));
        searchField.putClientProperty(
                "JTextField.placeholderText", "Mã lịch, bệnh nhân, SĐT, bác sĩ...");
        dateFilter.setPreferredSize(new Dimension(110, 34));
        dateFilters.add(searchField);
        dateFilters.add(dateFilter);
        customDate.setPreferredSize(new Dimension(115, 34));
        customDate.setValue(Date.from(databaseDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        customDate.setEnabled(false);
        dateFilters.add(customDate);
        filters.add(dateFilters);
        JPanel catalogFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        catalogFilters.setOpaque(false);
        statusFilter.addItem(new StatusChoice("", "Tất cả trạng thái"));
        statusFilter.setPreferredSize(new Dimension(140, 34));
        catalogFilters.add(statusFilter);
        specialtyFilter.addItem(new SpecialtyChoice("", "Tất cả chuyên khoa"));
        specialtyFilter.setPreferredSize(new Dimension(165, 34));
        catalogFilters.add(specialtyFilter);
        doctorFilter.addItem(new DoctorChoice("", "Tất cả bác sĩ", ""));
        doctorFilter.setPreferredSize(new Dimension(150, 34));
        clearFiltersButton.addActionListener(event -> clearFilters());
        catalogFilters.add(doctorFilter);
        catalogFilters.add(clearFiltersButton);
        filters.add(catalogFilters);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.setOpaque(false);
        detailsButton.addActionListener(event -> showDetails());
        editButton.addActionListener(event -> openEditor(selectedAppointmentId()));
        confirmButton.addActionListener(event -> confirmSelected());
        cancelButton.addActionListener(event -> cancelSelected());
        for (JButton button : List.of(detailsButton, editButton, confirmButton, cancelButton)) {
            button.setEnabled(false);
            actions.add(button);
        }
        root.add(filters, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                searchTimer.restart();
            }
        });
        dateFilter.addActionListener(event -> {
            customDate.setEnabled(dateFilter.getSelectedItem() == DateChoice.CUSTOM);
            resetAndLoad();
        });
        customDate.addChangeListener(event -> {
            if (dateFilter.getSelectedItem() == DateChoice.CUSTOM) {
                if (!suppressFilterEvents) {
                    customDateChanged = true;
                    resetAndLoad();
                }
            }
        });
        statusFilter.addActionListener(event -> resetAndLoad());
        specialtyFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                refreshDoctorFilter();
                resetAndLoad();
            }
        });
        doctorFilter.addActionListener(event -> resetAndLoad());
        return root;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(11, 0, 0, 0));
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        resultCount.setForeground(MainFrame.MUTED);
        resultCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        left.add(resultCount, BorderLayout.WEST);
        left.add(message, BorderLayout.CENTER);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paging.setOpaque(false);
        previousButton.addActionListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                loadAppointments();
            }
        });
        nextButton.addActionListener(event -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadAppointments();
            }
        });
        paging.add(previousButton);
        paging.add(pageLabel);
        paging.add(nextButton);
        footer.add(left, BorderLayout.CENTER);
        footer.add(paging, BorderLayout.EAST);
        return footer;
    }

    private void loadReferences() {
        loadingReferences = true;
        new SwingWorker<FilterReferences, Void>() {
            @Override
            protected FilterReferences doInBackground() throws AppointmentException {
                return new FilterReferences(
                        appointmentBUS.getDatabaseDate(session),
                        appointmentBUS.getStatuses(session),
                        appointmentBUS.getSpecialties(session),
                        appointmentBUS.getDoctors(session));
            }

            @Override
            protected void done() {
                try {
                    FilterReferences references = get();
                    databaseDate = references.databaseDate();
                    if (!customDateChanged) {
                        suppressFilterEvents = true;
                        customDate.setValue(Date.from(databaseDate
                                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
                        suppressFilterEvents = false;
                    }
                    statuses = references.statuses();
                    specialties = references.specialties();
                    doctors = references.doctors();
                    suppressFilterEvents = true;
                    for (String status : statuses) {
                        statusFilter.addItem(new StatusChoice(status, statusLabel(status)));
                    }
                    for (AppointmentSpecialty specialty : specialties) {
                        specialtyFilter.addItem(new SpecialtyChoice(
                                specialty.maChuyenKhoa(), specialty.tenChuyenKhoa()));
                    }
                    refreshDoctorFilter();
                    suppressFilterEvents = false;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải bộ lọc lịch hẹn bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải dữ liệu bộ lọc.", exception.getCause());
                } finally {
                    loadingReferences = false;
                }
            }
        }.execute();
    }

    private void refreshDoctorFilter() {
        boolean wasSuppressing = suppressFilterEvents;
        suppressFilterEvents = true;
        DoctorChoice selected = (DoctorChoice) doctorFilter.getSelectedItem();
        String oldId = selected == null ? "" : selected.id();
        SpecialtyChoice specialty = (SpecialtyChoice) specialtyFilter.getSelectedItem();
        String specialtyId = specialty == null ? "" : specialty.id();
        doctorFilter.removeAllItems();
        doctorFilter.addItem(new DoctorChoice("", "Tất cả bác sĩ", ""));
        for (AppointmentDoctor doctor : doctors) {
            if (specialtyId.isEmpty() || specialtyId.equals(doctor.maChuyenKhoa())) {
                doctorFilter.addItem(new DoctorChoice(
                        doctor.maNhanVien(), doctor.hoTen(), doctor.maChuyenKhoa()));
            }
        }
        for (int i = 0; i < doctorFilter.getItemCount(); i++) {
            if (doctorFilter.getItemAt(i).id().equals(oldId)) {
                doctorFilter.setSelectedIndex(i);
                break;
            }
        }
        suppressFilterEvents = wasSuppressing;
    }

    private void loadStatistics() {
        new SwingWorker<AppointmentStats, Void>() {
            @Override
            protected AppointmentStats doInBackground() throws AppointmentException {
                return appointmentBUS.getTodayStats(session);
            }

            @Override
            protected void done() {
                try {
                    AppointmentStats stats = get();
                    totalStat.setText(Long.toString(stats.totalToday()));
                    pendingStat.setText(Long.toString(stats.pendingToday()));
                    confirmedStat.setText(Long.toString(stats.confirmedToday()));
                    cancelledStat.setText(Long.toString(stats.cancelledToday()));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải thống kê bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    totalStat.setText("!");
                    pendingStat.setText("!");
                    confirmedStat.setText("!");
                    cancelledStat.setText("!");
                    showError("Không thể tải thống kê lịch hẹn.", exception.getCause());
                }
            }
        }.execute();
    }

    private void resetAndLoad() {
        if (loadingReferences || suppressFilterEvents) {
            return;
        }
        currentPage = 0;
        loadAppointments();
    }

    private void clearFilters() {
        searchTimer.stop();
        suppressFilterEvents = true;
        searchField.setText("");
        dateFilter.setSelectedItem(DateChoice.ALL);
        customDate.setValue(Date.from(databaseDate
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        statusFilter.setSelectedIndex(0);
        specialtyFilter.setSelectedIndex(0);
        refreshDoctorFilter();
        doctorFilter.setSelectedIndex(0);
        customDate.setEnabled(false);
        suppressFilterEvents = false;
        currentPage = 0;
        loadAppointments();
    }

    private void loadAppointments() {
        if (loading) {
            reloadAfterLoad = true;
            return;
        }
        loading = true;
        setPagingEnabled(false);
        message.setForeground(MainFrame.MUTED);
        message.setText("Đang tải lịch hẹn...");
        AppointmentFilter filter = createFilter();
        new SwingWorker<AppointmentPage, Void>() {
            @Override
            protected AppointmentPage doInBackground() throws AppointmentException {
                return appointmentBUS.search(session, filter);
            }

            @Override
            protected void done() {
                try {
                    AppointmentPage result = get();
                    if (currentPage >= result.totalPages()) {
                        currentPage = result.totalPages() - 1;
                        reloadAfterLoad = true;
                    }
                    populateTable(result);
                    message.setText(result.totalCount() == 0
                            ? "Không tìm thấy lịch hẹn phù hợp." : " ");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadError("Tải lịch hẹn bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showLoadError(cause instanceof AppointmentException
                            ? cause.getMessage()
                            : "Không thể tải lịch hẹn. Hãy kiểm tra kết nối SQL Server.",
                            cause);
                } finally {
                    loading = false;
                    setPagingEnabled(true);
                    updateActions();
                    boolean reload = reloadAfterLoad;
                    reloadAfterLoad = false;
                    if (reload) {
                        SwingUtilities.invokeLater(AppointmentPanel.this::loadAppointments);
                    }
                }
            }
        }.execute();
    }

    private AppointmentFilter createFilter() {
        LocalDate from = null;
        LocalDate to = null;
        DateChoice choice = (DateChoice) dateFilter.getSelectedItem();
        if (choice == DateChoice.TODAY) {
            from = databaseDate;
            to = from;
        } else if (choice == DateChoice.TOMORROW) {
            from = databaseDate.plusDays(1);
            to = from;
        } else if (choice == DateChoice.CUSTOM) {
            LocalDate selected = ((Date) customDate.getValue()).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            from = selected;
            to = selected;
        }
        StatusChoice status = (StatusChoice) statusFilter.getSelectedItem();
        SpecialtyChoice specialty = (SpecialtyChoice) specialtyFilter.getSelectedItem();
        DoctorChoice doctor = (DoctorChoice) doctorFilter.getSelectedItem();
        return new AppointmentFilter(
                searchField.getText().trim(), from, to,
                status == null ? "" : status.code(),
                doctor == null ? "" : doctor.id(),
                specialty == null ? "" : specialty.id(),
                currentPage, PAGE_SIZE);
    }

    private void populateTable(AppointmentPage page) {
        tableModel.setRowCount(0);
        for (LichHen appointment : page.appointments()) {
            tableModel.addRow(new Object[]{
                appointment.maLichHen(),
                appointment.thoiGianHen() == null
                        ? "—" : appointment.thoiGianHen().format(DATE_TIME),
                appointment.tenBenhNhan(),
                emptyLabel(appointment.soDienThoaiBenhNhan()),
                emptyLabel(appointment.tenBacSi()),
                emptyLabel(appointment.tenChuyenKhoa()),
                statusLabel(appointment.trangThai())
            });
        }
        currentPage = page.page();
        totalPages = page.totalPages();
        pageLabel.setText("Trang " + (page.page() + 1) + " / " + totalPages);
        resultCount.setText(page.totalCount() + " lịch hẹn");
    }

    private void showLoadError(String error, Throwable cause) {
        tableModel.setRowCount(0);
        currentPage = 0;
        totalPages = 1;
        pageLabel.setText("Trang 1 / 1");
        resultCount.setText("Không tải được dữ liệu");
        message.setForeground(new Color(190, 62, 62));
        message.setText(error);
        if (!(cause instanceof AppointmentException)) {
            LOGGER.log(Level.SEVERE, "Unexpected appointment list error.", cause);
        }
    }

    private void setPagingEnabled(boolean enabled) {
        previousButton.setEnabled(enabled && currentPage > 0);
        nextButton.setEnabled(enabled && currentPage + 1 < totalPages);
        refreshButton.setEnabled(enabled);
        clearFiltersButton.setEnabled(enabled);
        addButton.setEnabled(enabled && canManage());
    }

    private long selectedAppointmentId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return -1;
        }
        return ((Number) tableModel.getValueAt(table.convertRowIndexToModel(row), 0)).longValue();
    }

    private String selectedStatus() {
        int row = table.getSelectedRow();
        return row < 0 ? "" : String.valueOf(
                tableModel.getValueAt(table.convertRowIndexToModel(row), 6));
    }

    private void updateActions() {
        boolean selected = table.getSelectedRow() >= 0 && !loading;
        detailsButton.setEnabled(selected);
        boolean waiting = "Chờ xác nhận".equals(selectedStatus());
        boolean editable = canManage() && selected
                && (waiting || "Đã xác nhận".equals(selectedStatus()));
        editButton.setEnabled(editable);
        confirmButton.setEnabled(canManage() && selected && waiting);
        cancelButton.setEnabled(editable);
    }

    private void showDetails() {
        long appointmentId = selectedAppointmentId();
        if (appointmentId > 0) {
            new AppointmentDetailDialog(
                    (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                    session, appointmentBUS, appointmentId).setVisible(true);
        }
    }

    private void openEditor(Long appointmentId) {
        AppointmentDialog dialog = new AppointmentDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                session, appointmentBUS, patientBUS, appointmentId, savedId -> {
                    message.setForeground(new Color(45, 130, 90));
                    message.setText(appointmentId == null
                            ? "Đã đặt lịch hẹn " + savedId + "."
                            : "Đã cập nhật lịch hẹn " + savedId + ".");
                    refreshData();
                });
        dialog.setVisible(true);
    }

    private void confirmSelected() {
        long appointmentId = selectedAppointmentId();
        if (appointmentId <= 0 || !canManage()) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this, "Xác nhận lịch hẹn " + appointmentId + "?",
                "Xác nhận lịch hẹn", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        runStatusAction(appointmentId, true, null);
    }

    private void cancelSelected() {
        long appointmentId = selectedAppointmentId();
        if (appointmentId <= 0 || !canManage()) {
            return;
        }
        String reason = JOptionPane.showInputDialog(
                this, "Lý do hủy (không bắt buộc):", "Hủy lịch hẹn",
                JOptionPane.QUESTION_MESSAGE);
        if (reason == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(
                this, "Bạn có chắc chắn muốn hủy lịch hẹn " + appointmentId + "?",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            runStatusAction(appointmentId, false, reason);
        }
    }

    private void runStatusAction(long appointmentId, boolean confirm, String reason) {
        setActionsEnabled(false);
        message.setForeground(MainFrame.MUTED);
        message.setText(confirm ? "Đang xác nhận..." : "Đang hủy lịch...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws AppointmentException {
                if (confirm) {
                    appointmentBUS.confirm(session, appointmentId);
                } else {
                    appointmentBUS.cancel(session, appointmentId, reason);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    message.setForeground(new Color(45, 130, 90));
                    message.setText(confirm ? "Đã xác nhận lịch hẹn." : "Đã hủy lịch hẹn.");
                    refreshData();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showActionError("Thao tác bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showActionError(cause instanceof AppointmentException
                            ? cause.getMessage() : "Không thể cập nhật lịch hẹn.", cause);
                } finally {
                    updateActions();
                }
            }
        }.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        detailsButton.setEnabled(enabled);
        editButton.setEnabled(enabled);
        confirmButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }

    private void showActionError(String error, Throwable cause) {
        message.setForeground(new Color(190, 62, 62));
        message.setText(error);
        if (!(cause instanceof AppointmentException)) {
            LOGGER.log(Level.SEVERE, "Unexpected appointment action error.", cause);
        }
    }

    private void showError(String error, Throwable cause) {
        message.setForeground(new Color(190, 62, 62));
        message.setText(error);
        if (!(cause instanceof AppointmentException)) {
            LOGGER.log(Level.SEVERE, "Unexpected appointment UI error.", cause);
        }
    }

    private boolean canManage() {
        return session.hasPermission("LH_QUANLY");
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "ChoXacNhan" -> "Chờ xác nhận";
            case "DaXacNhan" -> "Đã xác nhận";
            case "DaDen" -> "Đã đến";
            case "DangKham" -> "Đang khám";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            case "VangMat" -> "Vắng mặt";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }

    private String emptyLabel(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private JPanel gap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(1, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return gap;
    }

    private enum DateChoice {
        ALL("Tất cả ngày"),
        TODAY("Hôm nay"),
        TOMORROW("Ngày mai"),
        CUSTOM("Chọn ngày");

        private final String label;

        DateChoice(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record StatusChoice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record SpecialtyChoice(String id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record DoctorChoice(String id, String label, String specialtyId) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record FilterReferences(
            LocalDate databaseDate,
            List<String> statuses,
            List<AppointmentSpecialty> specialties,
            List<AppointmentDoctor> doctors) {
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus,
                int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            if (!selected) {
                String status = String.valueOf(value);
                if ("Đã hủy".equals(status) || "Vắng mặt".equals(status)) {
                    label.setForeground(new Color(170, 60, 60));
                } else if ("Đã xác nhận".equals(status) || "Hoàn tất".equals(status)) {
                    label.setForeground(new Color(35, 125, 85));
                } else {
                    label.setForeground(new Color(160, 110, 30));
                }
            }
            return label;
        }
    }
}
