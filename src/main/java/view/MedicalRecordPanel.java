package view;

import bus.medicalrecord.MedicalRecordException;
import bus.medicalrecord.PhieuKhamBUS;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.auth.AuthenticatedSession;
import model.medicalrecord.MedicalRecordDetail;
import model.medicalrecord.MedicalRecordFilter;
import model.medicalrecord.MedicalRecordListItem;
import model.medicalrecord.MedicalRecordPage;
import model.medicalrecord.MedicalRecordStats;

final class MedicalRecordPanel extends JPanel {
    private static final Logger LOGGER =
            Logger.getLogger(MedicalRecordPanel.class.getName());
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final PhieuKhamBUS recordBUS;
    private final JTextField searchField = new JTextField();
    private final JCheckBox dateFilterEnabled = new JCheckBox("Lọc ngày lập");
    private final JSpinner fromDate = new JSpinner(new SpinnerDateModel());
    private final JSpinner toDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<StatusChoice> statusFilter = new JComboBox<>();
    private final JComboBox<DoctorChoice> doctorFilter = new JComboBox<>();
    private final JComboBox<SpecialtyChoice> specialtyFilter = new JComboBox<>();
    private final JLabel totalStat = new JLabel("—");
    private final JLabel todayStat = new JLabel("—");
    private final JLabel draftStat = new JLabel("—");
    private final JLabel completedStat = new JLabel("—");
    private final JLabel resultCount = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JLabel message = new JLabel(" ");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton searchButton = new JButton("Tìm kiếm");
    private final JButton clearButton = new JButton("Xóa bộ lọc");
    private final JButton createButton = new JButton("+ Tạo phiếu khám");
    private final JButton previousButton = new JButton("Trước");
    private final JButton nextButton = new JButton("Sau");
    private final JButton detailsButton = new JButton("Xem chi tiết");
    private final JButton editButton = new JButton("Chỉnh sửa");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã phiếu", "Mã lượt", "Mã BN", "Bệnh nhân", "Bác sĩ",
                "Chuyên khoa", "Ngày lập", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<MedicalRecordListItem> records = List.of();
    private List<AppointmentDoctor> doctors = List.of();
    private List<AppointmentSpecialty> specialties = List.of();
    private int currentPage;
    private int totalPages = 1;
    private long loadVersion;
    private boolean suppressFilterEvents;

    MedicalRecordPanel(AuthenticatedSession session, PhieuKhamBUS recordBUS) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Không thể mở phiếu khám khi chưa đăng nhập.");
        }
        this.session = session;
        this.recordBUS = recordBUS;
        configureDate(fromDate);
        configureDate(toDate);
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(24, 28, 28, 28));
        add(createContent(), BorderLayout.NORTH);
        configureTable();
        configureEvents();
        loadReferences();
        refreshData();
    }

    void refreshData() {
        loadPage();
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(gap(16));
        content.add(createStatistics());
        content.add(gap(16));
        content.add(createFilters());
        content.add(gap(12));
        content.add(createListCard());
        return content;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý phiếu khám");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel subtitle = new JLabel("Quản lý thông tin và kết quả khám bệnh của bệnh nhân.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(gap(5));
        labels.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton.addActionListener(event -> refreshData());
        actions.add(refreshButton);
        createButton.setForeground(Color.WHITE);
        createButton.setBackground(MainFrame.BLUE);
        createButton.setOpaque(true);
        createButton.setBorderPainted(false);
        createButton.setFocusPainted(false);
        createButton.addActionListener(event -> openEditor(null));
        actions.add(createButton);
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    private JPanel createStatistics() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(statCard("Tổng số phiếu khám", totalStat, new Color(0xE8F7F5)));
        cards.add(statCard("Lập trong ngày", todayStat, new Color(0xEAF2FF)));
        cards.add(statCard("Phiếu nháp", draftStat, new Color(0xFFF5E5)));
        cards.add(statCard("Đã hoàn thành / ký", completedStat, new Color(0xEAF7EF)));
        cards.setPreferredSize(new Dimension(0, 90));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        return cards;
    }

    private JPanel statCard(String title, JLabel value, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MainFrame.MUTED);
        value.setOpaque(true);
        value.setBackground(valueColor);
        value.setForeground(MainFrame.TEXT);
        value.setFont(new Font("Segoe UI", Font.BOLD, 19));
        value.setHorizontalAlignment(JLabel.CENTER);
        value.setBorder(new EmptyBorder(3, 10, 3, 10));
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFilters() {
        JPanel card = new JPanel(new BorderLayout(8, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchField.putClientProperty(
                "JTextField.placeholderText",
                "Mã phiếu, mã lượt, bệnh nhân, số điện thoại hoặc bác sĩ");
        searchRow.add(searchField, BorderLayout.CENTER);
        searchButton.addActionListener(event -> resetAndLoad());
        searchRow.add(searchButton, BorderLayout.EAST);
        card.add(searchRow, BorderLayout.NORTH);

        JPanel filters = new JPanel();
        filters.setOpaque(false);
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        JPanel dateAndStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        dateAndStatus.setOpaque(false);
        dateFilterEnabled.setOpaque(false);
        dateAndStatus.add(dateFilterEnabled);
        dateAndStatus.add(new JLabel("Từ"));
        dateAndStatus.add(fromDate);
        dateAndStatus.add(new JLabel("Đến"));
        dateAndStatus.add(toDate);
        statusFilter.setPreferredSize(new Dimension(150, 34));
        dateAndStatus.add(statusFilter);
        clearButton.addActionListener(event -> clearFilters());
        dateAndStatus.add(clearButton);

        JPanel catalog = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        catalog.setOpaque(false);
        specialtyFilter.setPreferredSize(new Dimension(190, 34));
        doctorFilter.setPreferredSize(new Dimension(240, 34));
        catalog.add(specialtyFilter);
        catalog.add(doctorFilter);
        filters.add(dateAndStatus);
        filters.add(Box.createVerticalStrut(7));
        filters.add(catalog);
        card.add(filters, BorderLayout.CENTER);
        return card;
    }

    private JPanel createListCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 14, 10, 14)));
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel title = new JLabel("Danh sách phiếu khám");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(MainFrame.TEXT);
        titleBar.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        detailsButton.addActionListener(event -> showSelectedDetails());
        editButton.addActionListener(event -> {
            MedicalRecordListItem record = selectedRecord();
            if (record != null && recordBUS.canEdit(session, record)) {
                openEditor(record.maPhieuKham());
            }
        });
        actions.add(detailsButton);
        actions.add(editButton);
        titleBar.add(actions, BorderLayout.EAST);
        card.add(titleBar, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 430));
        card.add(tableScroll, BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        JPanel info = new JPanel(new BorderLayout(10, 0));
        info.setOpaque(false);
        resultCount.setForeground(MainFrame.MUTED);
        resultCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        info.add(resultCount, BorderLayout.WEST);
        info.add(message, BorderLayout.CENTER);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paging.setOpaque(false);
        previousButton.addActionListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                loadPage();
            }
        });
        nextButton.addActionListener(event -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadPage();
            }
        });
        paging.add(previousButton);
        paging.add(pageLabel);
        paging.add(nextButton);
        footer.add(info, BorderLayout.CENTER);
        footer.add(paging, BorderLayout.EAST);
        return footer;
    }

    private void configureTable() {
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 38));
        header.setForeground(MainFrame.MUTED);
        header.setBackground(new Color(0xF5F7FA));
        header.setReorderingAllowed(false);
        int[] widths = {90, 90, 90, 190, 150, 160, 140, 120};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showSelectedDetails();
                }
            }
        });
        updateSelectionActions();
    }

    private void configureEvents() {
        dateFilterEnabled.addActionListener(event -> {
            boolean enabled = dateFilterEnabled.isSelected();
            fromDate.setEnabled(enabled);
            toDate.setEnabled(enabled);
            resetAndLoad();
        });
        fromDate.addChangeListener(event -> {
            if (!suppressFilterEvents && dateFilterEnabled.isSelected()) {
                resetAndLoad();
            }
        });
        toDate.addChangeListener(event -> {
            if (!suppressFilterEvents && dateFilterEnabled.isSelected()) {
                resetAndLoad();
            }
        });
        statusFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
        specialtyFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                refreshDoctorFilter();
                resetAndLoad();
            }
        });
        doctorFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
    }

    private void loadReferences() {
        setMessage("Đang tải bộ lọc...", MainFrame.MUTED);
        new SwingWorker<FilterReferences, Void>() {
            @Override
            protected FilterReferences doInBackground() throws MedicalRecordException {
                return new FilterReferences(
                        recordBUS.getStatuses(session),
                        recordBUS.getDoctors(session),
                        recordBUS.getSpecialties(session));
            }

            @Override
            protected void done() {
                try {
                    FilterReferences references = get();
                    suppressFilterEvents = true;
                    statusFilter.removeAllItems();
                    statusFilter.addItem(new StatusChoice("", "Tất cả trạng thái"));
                    for (String status : references.statuses()) {
                        statusFilter.addItem(new StatusChoice(status, statusLabel(status)));
                    }
                    specialtyFilter.removeAllItems();
                    specialtyFilter.addItem(new SpecialtyChoice("", "Tất cả chuyên khoa"));
                    specialties = references.specialties();
                    for (AppointmentSpecialty specialty : specialties) {
                        specialtyFilter.addItem(new SpecialtyChoice(
                                specialty.maChuyenKhoa(), specialty.tenChuyenKhoa()));
                    }
                    doctors = references.doctors();
                    refreshDoctorFilter();
                    suppressFilterEvents = false;
                    setMessage(" ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải bộ lọc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải dữ liệu bộ lọc.", exception.getCause());
                }
            }
        }.execute();
    }

    private void refreshDoctorFilter() {
        boolean oldSuppression = suppressFilterEvents;
        suppressFilterEvents = true;
        DoctorChoice selected = (DoctorChoice) doctorFilter.getSelectedItem();
        String oldId = selected == null ? "" : selected.id();
        SpecialtyChoice specialty = (SpecialtyChoice) specialtyFilter.getSelectedItem();
        String specialtyId = specialty == null ? "" : specialty.id();
        doctorFilter.removeAllItems();
        doctorFilter.addItem(new DoctorChoice("", "Tất cả bác sĩ"));
        for (AppointmentDoctor doctor : doctors) {
            if (specialtyId.isEmpty() || specialtyId.equals(doctor.maChuyenKhoa())) {
                doctorFilter.addItem(new DoctorChoice(
                        doctor.maNhanVien(),
                        doctor.hoTen() + (doctor.tenChuyenKhoa() == null
                                ? "" : " - " + doctor.tenChuyenKhoa())));
            }
        }
        for (int i = 0; i < doctorFilter.getItemCount(); i++) {
            if (doctorFilter.getItemAt(i).id().equals(oldId)) {
                doctorFilter.setSelectedIndex(i);
                break;
            }
        }
        suppressFilterEvents = oldSuppression;
    }

    private void loadPage() {
        long request = ++loadVersion;
        MedicalRecordFilter filter;
        try {
            LocalDate from = dateFilterEnabled.isSelected() ? dateValue(fromDate) : null;
            LocalDate to = dateFilterEnabled.isSelected() ? dateValue(toDate) : null;
            if (from != null && to != null && from.isAfter(to)) {
                setMessage("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.",
                        new Color(190, 62, 62));
                return;
            }
            StatusChoice status = (StatusChoice) statusFilter.getSelectedItem();
            DoctorChoice doctor = (DoctorChoice) doctorFilter.getSelectedItem();
            SpecialtyChoice specialty = (SpecialtyChoice) specialtyFilter.getSelectedItem();
            filter = new MedicalRecordFilter(
                    searchField.getText(),
                    from,
                    to,
                    status == null ? "" : status.code(),
                    doctor == null ? "" : doctor.id(),
                    specialty == null ? "" : specialty.id(),
                    currentPage,
                    PAGE_SIZE);
        } catch (RuntimeException exception) {
            showError("Không thể đọc điều kiện lọc phiếu khám.", exception);
            return;
        }

        setControlsEnabled(false);
        setMessage("Đang tải phiếu khám...", MainFrame.MUTED);
        new SwingWorker<DataBundle, Void>() {
            @Override
            protected DataBundle doInBackground() throws MedicalRecordException {
                MedicalRecordStats stats = recordBUS.getStats(session);
                MedicalRecordPage page = recordBUS.search(session, filter);
                return new DataBundle(stats, page);
            }

            @Override
            protected void done() {
                if (request != loadVersion) {
                    return;
                }
                try {
                    DataBundle data = get();
                    renderStats(data.stats());
                    renderPage(data.page());
                    setMessage(data.page().totalCount() == 0
                            ? "Chưa có phiếu khám phù hợp với bộ lọc."
                            : " ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải phiếu khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    totalStat.setText("!");
                    todayStat.setText("!");
                    draftStat.setText("!");
                    completedStat.setText("!");
                    showError("Không thể tải dữ liệu phiếu khám.", exception.getCause());
                } finally {
                    if (request == loadVersion) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private void renderStats(MedicalRecordStats stats) {
        totalStat.setText(Long.toString(stats.total()));
        todayStat.setText(Long.toString(stats.createdToday()));
        draftStat.setText(Long.toString(stats.drafts()));
        completedStat.setText(Long.toString(stats.completed()));
    }

    private void renderPage(MedicalRecordPage page) {
        records = page.records();
        currentPage = page.page();
        totalPages = page.totalPages();
        tableModel.setRowCount(0);
        for (MedicalRecordListItem record : records) {
            tableModel.addRow(new Object[]{
                    record.maPhieuKham(),
                    record.maLuotKham(),
                    safe(record.maBenhNhan()),
                    safe(record.tenBenhNhan()),
                    safe(record.tenBacSi()),
                    safe(record.tenChuyenKhoa()),
                    formatTime(record.thoiGianLap()),
                    statusLabel(record.trangThai())
            });
        }
        resultCount.setText("Tổng " + page.totalCount() + " phiếu khám");
        pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
        updateSelectionActions();
    }

    private void showSelectedDetails() {
        MedicalRecordListItem selected = selectedRecord();
        if (selected == null) {
            return;
        }
        setControlsEnabled(false);
        setMessage("Đang tải chi tiết phiếu khám...", MainFrame.MUTED);
        new SwingWorker<MedicalRecordDetail, Void>() {
            @Override
            protected MedicalRecordDetail doInBackground() throws MedicalRecordException {
                return recordBUS.getById(session, selected.maPhieuKham());
            }

            @Override
            protected void done() {
                try {
                    MedicalRecordDetail detail = get();
                    new MedicalRecordDetailDialog(
                            SwingUtilities.getWindowAncestor(MedicalRecordPanel.this), detail)
                            .setVisible(true);
                    setMessage(" ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chi tiết phiếu khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải chi tiết phiếu khám.", exception.getCause());
                } finally {
                    setControlsEnabled(true);
                }
            }
        }.execute();
    }

    private void openEditor(Long recordId) {
        new MedicalRecordDialog(
                SwingUtilities.getWindowAncestor(this),
                session,
                recordBUS,
                recordId,
                savedRecordId -> {
                    currentPage = 0;
                    refreshData();
                    JOptionPane.showMessageDialog(
                            this,
                            (recordId == null ? "Đã tạo phiếu khám mã " : "Đã cập nhật phiếu khám mã ")
                                    + savedRecordId + ".",
                            "Phiếu khám",
                            JOptionPane.INFORMATION_MESSAGE);
                }).setVisible(true);
    }

    private MedicalRecordListItem selectedRecord() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        return modelRow < 0 || modelRow >= records.size() ? null : records.get(modelRow);
    }

    private void updateSelectionActions() {
        MedicalRecordListItem selected = selectedRecord();
        detailsButton.setEnabled(selected != null);
        editButton.setEnabled(recordBUS.canEdit(session, selected));
        editButton.setToolTipText(selected != null && !recordBUS.canEdit(session, selected)
                ? "Chỉ được sửa phiếu nháp của lượt khám đang thực hiện do bạn phụ trách."
                : null);
    }

    private void clearFilters() {
        suppressFilterEvents = true;
        searchField.setText("");
        dateFilterEnabled.setSelected(false);
        fromDate.setValue(today());
        toDate.setValue(today());
        fromDate.setEnabled(false);
        toDate.setEnabled(false);
        statusFilter.setSelectedIndex(0);
        specialtyFilter.setSelectedIndex(0);
        refreshDoctorFilter();
        doctorFilter.setSelectedIndex(0);
        suppressFilterEvents = false;
        resetAndLoad();
    }

    private void resetAndLoad() {
        if (!suppressFilterEvents) {
            currentPage = 0;
            loadPage();
        }
    }

    private void configureDate(JSpinner spinner) {
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        spinner.setValue(today());
        spinner.setEnabled(false);
        spinner.setPreferredSize(new Dimension(120, 32));
    }

    private Date today() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate dateValue(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void setControlsEnabled(boolean enabled) {
        searchField.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        createButton.setEnabled(enabled);
        dateFilterEnabled.setEnabled(enabled);
        statusFilter.setEnabled(enabled);
        doctorFilter.setEnabled(enabled);
        specialtyFilter.setEnabled(enabled);
        fromDate.setEnabled(enabled && dateFilterEnabled.isSelected());
        toDate.setEnabled(enabled && dateFilterEnabled.isSelected());
        table.setEnabled(enabled);
        if (!enabled) {
            detailsButton.setEnabled(false);
            editButton.setEnabled(false);
        } else {
            updateSelectionActions();
        }
    }

    private void setMessage(String value, Color color) {
        message.setText(value);
        message.setForeground(color);
    }

    private void showError(String messageText, Throwable cause) {
        Throwable source = cause;
        while (source != null && source.getCause() != null
                && !(source instanceof MedicalRecordException)) {
            source = source.getCause();
        }
        String detail = cause instanceof MedicalRecordException
                ? cause.getMessage()
                : source != null && source.getMessage() != null
                        ? source.getMessage() : messageText;
        setMessage(detail, new Color(190, 62, 62));
        if (!(cause instanceof MedicalRecordException)) {
            LOGGER.log(Level.SEVERE, messageText, cause);
        }
        JOptionPane.showMessageDialog(
                this, detail, "Quản lý phiếu khám", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel gap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(1, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return gap;
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "Nhap" -> "Nháp";
            case "HoanTat" -> "Hoàn tất";
            case "DaKy" -> "Đã ký";
            case "DaKhoa" -> "Đã khóa";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private record DataBundle(MedicalRecordStats stats, MedicalRecordPage page) {
    }

    private record FilterReferences(
            List<String> statuses,
            List<AppointmentDoctor> doctors,
            List<AppointmentSpecialty> specialties) {
    }

    private record StatusChoice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record DoctorChoice(String id, String label) {
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

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable source, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    source, value, selected, focus, row, column);
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            if (!selected) {
                String status = String.valueOf(value);
                label.setForeground("Đã khóa".equals(status) || "Đã ký".equals(status)
                        || "Hoàn tất".equals(status)
                        ? new Color(35, 125, 85) : new Color(160, 110, 30));
            }
            return label;
        }
    }
}
