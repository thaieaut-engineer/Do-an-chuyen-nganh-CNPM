package view;

import bus.visit.LuotKhamBUS;
import bus.visit.VisitException;
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
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.auth.AuthenticatedSession;
import model.visit.VisitDetail;
import model.visit.VisitFilter;
import model.visit.VisitListItem;
import model.visit.VisitPage;
import model.visit.VisitStats;

final class VisitPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(VisitPanel.class.getName());
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final LuotKhamBUS visitBUS;
    private final JTextField searchField = new JTextField();
    private final JCheckBox dateRangeEnabled = new JCheckBox("Lọc thời gian");
    private final JSpinner fromDate = new JSpinner(new SpinnerDateModel());
    private final JSpinner toDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<StatusChoice> statusFilter = new JComboBox<>();
    private final JComboBox<DoctorChoice> doctorFilter = new JComboBox<>();
    private final JComboBox<SpecialtyChoice> specialtyFilter = new JComboBox<>();
    private final JLabel totalStat = new JLabel("—");
    private final JLabel waitingStat = new JLabel("—");
    private final JLabel inProgressStat = new JLabel("—");
    private final JLabel completedStat = new JLabel("—");
    private final JLabel resultCount = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JLabel message = new JLabel(" ");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton searchButton = new JButton("Tìm kiếm");
    private final JButton clearButton = new JButton("Xóa bộ lọc");
    private final JButton previousButton = new JButton("Trước");
    private final JButton nextButton = new JButton("Sau");
    private final JButton detailButton = new JButton("Xem chi tiết");
    private final JButton advanceButton = new JButton("Cập nhật trạng thái");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã lượt", "Mã BN", "Họ tên bệnh nhân", "Bác sĩ",
                "Chuyên khoa", "Mã lịch hẹn", "Tiếp nhận lúc", "Kết thúc lúc", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private List<VisitListItem> visits = List.of();
    private int currentPage;
    private int totalPages = 1;
    private long loadVersion;
    private boolean suppressFilterEvents;

    VisitPanel(AuthenticatedSession session, LuotKhamBUS visitBUS) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Không thể mở lượt khám khi chưa đăng nhập.");
        }
        this.session = session;
        this.visitBUS = visitBUS;
        configureDateSpinner(fromDate);
        configureDateSpinner(toDate);
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
        loadCurrentPage();
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
        JLabel title = new JLabel("Quản lý lượt khám");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel subtitle = new JLabel("Theo dõi lượt khám và tiến độ phục vụ bệnh nhân.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(gap(5));
        labels.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton.addActionListener(event -> refreshData());
        actions.add(refreshButton);
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    private JPanel createStatistics() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(statCard("Tổng lượt khám hôm nay", totalStat, new Color(0xE8F7F5)));
        cards.add(statCard("Đang chờ khám", waitingStat, new Color(0xFFF5E5)));
        cards.add(statCard("Đang khám / chờ kết quả", inProgressStat, new Color(0xEAF2FF)));
        cards.add(statCard("Đã hoàn thành", completedStat, new Color(0xEAF7EF)));
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
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MainFrame.MUTED);
        value.setOpaque(true);
        value.setBackground(background);
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
                "Mã lượt, mã bệnh nhân, tên, điện thoại, bác sĩ hoặc mã lịch hẹn");
        searchRow.add(searchField, BorderLayout.CENTER);
        searchButton.addActionListener(event -> resetAndLoad());
        searchRow.add(searchButton, BorderLayout.EAST);
        card.add(searchRow, BorderLayout.NORTH);

        JPanel filters = new JPanel();
        filters.setOpaque(false);
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        JPanel dateAndStatusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        dateAndStatusRow.setOpaque(false);
        dateRangeEnabled.setOpaque(false);
        dateAndStatusRow.add(dateRangeEnabled);
        dateAndStatusRow.add(new JLabel("Từ"));
        dateAndStatusRow.add(fromDate);
        dateAndStatusRow.add(new JLabel("Đến"));
        dateAndStatusRow.add(toDate);
        statusFilter.setPreferredSize(new Dimension(150, 34));
        dateAndStatusRow.add(statusFilter);
        clearButton.addActionListener(event -> clearFilters());
        dateAndStatusRow.add(clearButton);

        JPanel catalogRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        catalogRow.setOpaque(false);
        specialtyFilter.setPreferredSize(new Dimension(190, 34));
        doctorFilter.setPreferredSize(new Dimension(240, 34));
        catalogRow.add(specialtyFilter);
        catalogRow.add(doctorFilter);
        filters.add(dateAndStatusRow);
        filters.add(Box.createVerticalStrut(7));
        filters.add(catalogRow);
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
        JLabel title = new JLabel("Danh sách lượt khám");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(MainFrame.TEXT);
        titleBar.add(title, BorderLayout.WEST);
        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rowActions.setOpaque(false);
        detailButton.addActionListener(event -> showSelectedDetail());
        rowActions.add(detailButton);
        if (session.hasPermission("KHAM_HOSO")) {
            advanceButton.setForeground(Color.WHITE);
            advanceButton.setBackground(MainFrame.BLUE);
            advanceButton.setOpaque(true);
            advanceButton.setBorderPainted(false);
            advanceButton.setFocusPainted(false);
            advanceButton.addActionListener(event -> advanceSelected());
            rowActions.add(advanceButton);
        }
        titleBar.add(rowActions, BorderLayout.EAST);
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
                loadCurrentPage();
            }
        });
        nextButton.addActionListener(event -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadCurrentPage();
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
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 38));
        header.setForeground(MainFrame.MUTED);
        header.setBackground(new Color(0xF5F7FA));
        header.setReorderingAllowed(false);
        table.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {90, 95, 180, 150, 150, 95, 140, 140, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        updateSelectionActions();
    }

    private void configureEvents() {
        dateRangeEnabled.addActionListener(event -> {
            boolean enabled = dateRangeEnabled.isSelected();
            fromDate.setEnabled(enabled);
            toDate.setEnabled(enabled);
            resetAndLoad();
        });
        fromDate.addChangeListener(event -> {
            if (!suppressFilterEvents && dateRangeEnabled.isSelected()) {
                resetAndLoad();
            }
        });
        toDate.addChangeListener(event -> {
            if (!suppressFilterEvents && dateRangeEnabled.isSelected()) {
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
                refreshDoctorChoices();
                resetAndLoad();
            }
        });
        doctorFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showSelectedDetail();
                }
            }
        });
    }

    private void loadReferences() {
        setMessage("Đang tải bộ lọc...", MainFrame.MUTED);
        new SwingWorker<FilterReferences, Void>() {
            @Override
            protected FilterReferences doInBackground() throws VisitException {
                return new FilterReferences(
                        visitBUS.getStatuses(session),
                        visitBUS.getDoctors(session),
                        visitBUS.getSpecialties(session));
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
                    for (AppointmentSpecialty specialty : references.specialties()) {
                        specialtyFilter.addItem(new SpecialtyChoice(
                                specialty.maChuyenKhoa(), specialty.tenChuyenKhoa()));
                    }
                    doctors = references.doctors();
                    refreshDoctorChoices();
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

    private List<AppointmentDoctor> doctors = List.of();

    private void refreshDoctorChoices() {
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

    private void loadCurrentPage() {
        long request = ++loadVersion;
        VisitFilter filter;
        try {
            LocalDate from = dateRangeEnabled.isSelected() ? dateValue(fromDate) : null;
            LocalDate to = dateRangeEnabled.isSelected() ? dateValue(toDate) : null;
            if (from != null && to != null && from.isAfter(to)) {
                setMessage("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.", new Color(190, 62, 62));
                return;
            }
            StatusChoice status = (StatusChoice) statusFilter.getSelectedItem();
            DoctorChoice doctor = (DoctorChoice) doctorFilter.getSelectedItem();
            SpecialtyChoice specialty = (SpecialtyChoice) specialtyFilter.getSelectedItem();
            filter = new VisitFilter(
                    searchField.getText(),
                    from,
                    to,
                    status == null ? "" : status.code(),
                    doctor == null ? "" : doctor.id(),
                    specialty == null ? "" : specialty.id(),
                    currentPage,
                    PAGE_SIZE);
        } catch (RuntimeException exception) {
            showError("Không thể đọc điều kiện lọc lượt khám.", exception);
            return;
        }

        setControlsEnabled(false);
        setMessage("Đang tải lượt khám...", MainFrame.MUTED);
        new SwingWorker<DataBundle, Void>() {
            @Override
            protected DataBundle doInBackground() throws VisitException {
                VisitStats stats = visitBUS.getTodayStats(session);
                VisitPage page = visitBUS.search(session, filter);
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
                            ? "Không có lượt khám phù hợp với bộ lọc."
                            : " ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải lượt khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    totalStat.setText("!");
                    waitingStat.setText("!");
                    inProgressStat.setText("!");
                    completedStat.setText("!");
                    showError("Không thể tải dữ liệu lượt khám.", exception.getCause());
                } finally {
                    if (request == loadVersion) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private void renderStats(VisitStats stats) {
        totalStat.setText(Long.toString(stats.totalToday()));
        waitingStat.setText(Long.toString(stats.waiting()));
        inProgressStat.setText(Long.toString(stats.inProgress()));
        completedStat.setText(Long.toString(stats.completed()));
    }

    private void renderPage(VisitPage page) {
        visits = page.visits();
        currentPage = page.page();
        totalPages = page.totalPages();
        tableModel.setRowCount(0);
        for (VisitListItem visit : visits) {
            tableModel.addRow(new Object[]{
                    visit.maLuotKham(),
                    safe(visit.maBenhNhan()),
                    safe(visit.tenBenhNhan()),
                    safe(visit.tenBacSi()),
                    safe(visit.tenChuyenKhoa()),
                    visit.maLichHen() == null ? "—" : visit.maLichHen(),
                    formatTime(visit.thoiGianTiepNhan()),
                    formatTime(visit.thoiGianKetThuc()),
                    statusLabel(visit.trangThai())
            });
        }
        resultCount.setText("Tổng " + page.totalCount() + " lượt khám");
        pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
        updateSelectionActions();
    }

    private void showSelectedDetail() {
        VisitListItem selected = selectedVisit();
        if (selected == null) {
            return;
        }
        setControlsEnabled(false);
        setMessage("Đang tải chi tiết lượt khám...", MainFrame.MUTED);
        new SwingWorker<VisitDetail, Void>() {
            @Override
            protected VisitDetail doInBackground() throws VisitException {
                return visitBUS.getById(session, selected.maLuotKham());
            }

            @Override
            protected void done() {
                try {
                    VisitDetail detail = get();
                    new VisitDetailDialog(
                            javax.swing.SwingUtilities.getWindowAncestor(VisitPanel.this),
                            detail,
                            session.hasPermission("KHAM_HOSO")).setVisible(true);
                    setMessage(" ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chi tiết lượt khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải chi tiết lượt khám.", exception.getCause());
                } finally {
                    setControlsEnabled(true);
                }
            }
        }.execute();
    }

    private void advanceSelected() {
        VisitListItem selected = selectedVisit();
        if (selected == null || !visitBUS.canUpdateVisit(session, selected.maNhanVien())) {
            return;
        }
        String targetStatus = switch (selected.trangThai()) {
            case "ChoKham" -> "DangKham";
            case "DangKham" -> "ChoKetQua";
            case "ChoKetQua" -> "HoanTat";
            default -> null;
        };
        if (targetStatus == null) {
            return;
        }
        String targetLabel = statusLabel(targetStatus);
        String confirmation = "Chuyển lượt khám " + selected.maLuotKham()
                + " sang trạng thái \"" + targetLabel + "\"?";
        if (JOptionPane.showConfirmDialog(
                this, confirmation, "Xác nhận cập nhật",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                != JOptionPane.YES_OPTION) {
            return;
        }

        setControlsEnabled(false);
        setMessage("Đang cập nhật trạng thái...", MainFrame.MUTED);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws VisitException {
                visitBUS.advanceStatus(session, selected.maLuotKham(), targetStatus);
                return null;
            }

            @Override
            protected void done() {
                boolean reload = false;
                try {
                    get();
                    setMessage("Đã chuyển lượt khám sang " + targetLabel + ".", new Color(45, 130, 90));
                    reload = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Cập nhật trạng thái bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể cập nhật trạng thái.", exception.getCause());
                } finally {
                    if (reload) {
                        currentPage = 0;
                        loadCurrentPage();
                    } else {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private VisitListItem selectedVisit() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        if (modelRow < 0 || modelRow >= visits.size()) {
            return null;
        }
        return visits.get(modelRow);
    }

    private void updateSelectionActions() {
        VisitListItem selected = selectedVisit();
        detailButton.setEnabled(selected != null);
        if (advanceButton.getParent() == null) {
            return;
        }
        boolean hasNext = selected != null
                && ("ChoKham".equals(selected.trangThai())
                || "DangKham".equals(selected.trangThai())
                || "ChoKetQua".equals(selected.trangThai()));
        advanceButton.setEnabled(hasNext && selected != null
                && visitBUS.canUpdateVisit(session, selected.maNhanVien()));
        if (selected != null && "ChoKham".equals(selected.trangThai())) {
            advanceButton.setText("Bắt đầu khám");
        } else if (selected != null && "DangKham".equals(selected.trangThai())) {
            advanceButton.setText("Chuyển chờ kết quả");
        } else if (selected != null && "ChoKetQua".equals(selected.trangThai())) {
            advanceButton.setText("Hoàn thành");
        } else {
            advanceButton.setText("Cập nhật trạng thái");
        }
    }

    private void clearFilters() {
        suppressFilterEvents = true;
        searchField.setText("");
        dateRangeEnabled.setSelected(false);
        fromDate.setValue(today());
        toDate.setValue(today());
        fromDate.setEnabled(false);
        toDate.setEnabled(false);
        statusFilter.setSelectedIndex(0);
        specialtyFilter.setSelectedIndex(0);
        refreshDoctorChoices();
        doctorFilter.setSelectedIndex(0);
        suppressFilterEvents = false;
        resetAndLoad();
    }

    private void resetAndLoad() {
        if (suppressFilterEvents) {
            return;
        }
        currentPage = 0;
        loadCurrentPage();
    }

    private void configureDateSpinner(JSpinner spinner) {
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
        dateRangeEnabled.setEnabled(enabled);
        statusFilter.setEnabled(enabled);
        doctorFilter.setEnabled(enabled);
        specialtyFilter.setEnabled(enabled);
        fromDate.setEnabled(enabled && dateRangeEnabled.isSelected());
        toDate.setEnabled(enabled && dateRangeEnabled.isSelected());
        table.setEnabled(enabled);
        if (!enabled) {
            detailButton.setEnabled(false);
            if (advanceButton.getParent() != null) {
                advanceButton.setEnabled(false);
            }
        } else {
            updateSelectionActions();
        }
    }

    private void setMessage(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }

    private void showError(String text, Throwable cause) {
        Throwable root = cause == null ? null : cause.getCause() == null ? cause : cause.getCause();
        String detail = cause instanceof VisitException
                ? cause.getMessage()
                : root != null && root.getMessage() != null
                        ? root.getMessage() : text;
        setMessage(detail, new Color(190, 62, 62));
        if (!(cause instanceof VisitException)) {
            LOGGER.log(Level.SEVERE, text, cause);
        }
        JOptionPane.showMessageDialog(this, detail, "Quản lý lượt khám", JOptionPane.ERROR_MESSAGE);
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
            case "ChoKham" -> "Chờ khám";
            case "DangKham" -> "Đang khám";
            case "ChoKetQua" -> "Chờ kết quả";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private record DataBundle(VisitStats stats, VisitPage page) {
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
                if ("Hoàn tất".equals(status)) {
                    label.setForeground(new Color(35, 125, 85));
                } else if ("Đã hủy".equals(status)) {
                    label.setForeground(new Color(170, 60, 60));
                } else {
                    label.setForeground(new Color(160, 110, 30));
                }
            }
            return label;
        }
    }
}
