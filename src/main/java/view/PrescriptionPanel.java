package view;

import bus.prescription.DonThuocBUS;
import bus.prescription.PrescriptionException;
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
import model.prescription.MedicationCatalogItem;
import model.prescription.PrescriptionDetail;
import model.prescription.PrescriptionFilter;
import model.prescription.PrescriptionListItem;
import model.prescription.PrescriptionPage;
import model.prescription.PrescriptionStats;

final class PrescriptionPanel extends JPanel {
    private static final Logger LOGGER =
            Logger.getLogger(PrescriptionPanel.class.getName());
    private static final int PAGE_SIZE = 25;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final DonThuocBUS prescriptionBUS;
    private final JTextField searchField = new JTextField();
    private final JCheckBox dateFilter = new JCheckBox("Lọc ngày kê");
    private final JSpinner fromDate = new JSpinner(new SpinnerDateModel());
    private final JSpinner toDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<Choice> statusFilter = new JComboBox<>();
    private final JComboBox<Choice> doctorFilter = new JComboBox<>();
    private final JComboBox<Choice> specialtyFilter = new JComboBox<>();
    private final JTextField catalogSearch = new JTextField();
    private final JComboBox<Choice> catalogStatusFilter = new JComboBox<>();
    private final JLabel totalStat = new JLabel("—");
    private final JLabel todayStat = new JLabel("—");
    private final JLabel draftsStat = new JLabel("—");
    private final JLabel signedStat = new JLabel("—");
    private final JLabel dispensedStat = new JLabel("—");
    private final JLabel resultCount = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JLabel message = new JLabel(" ");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton searchButton = new JButton("Tìm kiếm");
    private final JButton clearButton = new JButton("Xóa bộ lọc");
    private final JButton createButton = new JButton("+ Kê đơn");
    private final JButton previousButton = new JButton("Trước");
    private final JButton nextButton = new JButton("Sau");
    private final JButton detailButton = new JButton("Xem chi tiết");
    private final JButton editButton = new JButton("Chỉnh sửa");
    private final JButton cancelButton = new JButton("Hủy đơn");
    private final JButton catalogSearchButton = new JButton("Tìm thuốc");
    private final DefaultTableModel prescriptionModel = new DefaultTableModel(
            new Object[]{"Mã đơn", "Mã lượt", "Mã BN", "Bệnh nhân", "Điện thoại",
                "Bác sĩ", "Chuyên khoa", "Ngày kê", "Số loại thuốc",
                "Trạng thái", "Xuất thuốc"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable prescriptionTable = new JTable(prescriptionModel);
    private final DefaultTableModel catalogModel = new DefaultTableModel(
            new Object[]{"Mã thuốc", "Tên thuốc", "Hoạt chất", "Hàm lượng",
                "Dạng bào chế", "Đơn vị", "Đường dùng mặc định", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable catalogTable = new JTable(catalogModel);
    private List<PrescriptionListItem> prescriptions = List.of();
    private List<MedicationCatalogItem> medications = List.of();
    private int currentPage;
    private int totalPages = 1;
    private long loadVersion;
    private boolean suppressFilterEvents;
    private boolean referencesLoaded;

    PrescriptionPanel(AuthenticatedSession session, DonThuocBUS prescriptionBUS) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException(
                    "Không thể mở kê đơn thuốc khi chưa đăng nhập.");
        }
        this.session = session;
        this.prescriptionBUS = prescriptionBUS;
        configureDate(fromDate);
        configureDate(toDate);
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(24, 28, 28, 28));
        add(createContent(), BorderLayout.NORTH);
        configureTables();
        configureEvents();
        refreshData();
    }

    void refreshData() {
        if (!referencesLoaded) {
            loadInitialData();
        } else {
            loadPrescriptions();
            loadCatalog();
        }
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(gap(16));
        content.add(createStatistics());
        content.add(gap(16));
        content.add(createTabs());
        return content;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý đơn thuốc");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel subtitle = new JLabel(
                "Tra cứu, kê đơn và quản lý đơn thuốc theo lượt khám.");
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
        JPanel cards = new JPanel(new GridLayout(1, 5, 10, 0));
        cards.setOpaque(false);
        cards.add(statCard("Tổng số đơn", totalStat, new Color(0xE8F7F5)));
        cards.add(statCard("Kê trong ngày", todayStat, new Color(0xEAF2FF)));
        cards.add(statCard("Đơn nháp", draftsStat, new Color(0xFFF5E5)));
        cards.add(statCard("Đã ký", signedStat, new Color(0xF2EEFF)));
        cards.add(statCard("Đã gửi / cấp phát", dispensedStat, new Color(0xEAF7EF)));
        cards.setPreferredSize(new Dimension(0, 90));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        return cards;
    }

    private JPanel statCard(String title, JLabel value, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MainFrame.MUTED);
        value.setOpaque(true);
        value.setBackground(color);
        value.setFont(new Font("Segoe UI", Font.BOLD, 21));
        value.setForeground(MainFrame.TEXT);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private javax.swing.JTabbedPane createTabs() {
        javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.addTab("Danh sách đơn thuốc", createPrescriptionTab());
        tabs.addTab("Danh mục thuốc", createCatalogTab());
        return tabs;
    }

    private JPanel createPrescriptionTab() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);
        root.add(createFilters(), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 12, 10, 12)));
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel title = new JLabel("Danh sách đơn thuốc");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(MainFrame.TEXT);
        titleBar.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        detailButton.addActionListener(event -> showSelectedDetail());
        editButton.addActionListener(event -> {
            PrescriptionListItem item = selectedPrescription();
            if (item != null && prescriptionBUS.canEdit(session, item)) {
                openEditor(item.maDonThuoc());
            }
        });
        cancelButton.addActionListener(event -> cancelSelected());
        actions.add(detailButton);
        actions.add(editButton);
        actions.add(cancelButton);
        titleBar.add(actions, BorderLayout.EAST);
        card.add(titleBar, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(prescriptionTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 420));
        card.add(tableScroll, BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);
        root.add(card, BorderLayout.CENTER);
        return root;
    }

    private JPanel createFilters() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchField.setPreferredSize(new Dimension(280, 34));
        searchField.setToolTipText("Tìm theo mã đơn, lượt khám, bệnh nhân hoặc bác sĩ");
        searchField.addActionListener(event -> resetAndLoad());
        searchButton.addActionListener(event -> resetAndLoad());
        clearButton.addActionListener(event -> clearFilters());
        searchRow.add(searchField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        buttons.setOpaque(false);
        buttons.add(searchButton);
        buttons.add(clearButton);
        searchRow.add(buttons, BorderLayout.EAST);
        card.add(searchRow);
        card.add(Box.createVerticalStrut(8));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        dateFilter.addActionListener(event -> {
            setDateEditorsEnabled(dateFilter.isSelected());
            resetAndLoad();
        });
        filterRow.add(dateFilter);
        filterRow.add(fromDate);
        filterRow.add(new JLabel("Đến"));
        filterRow.add(toDate);
        statusFilter.setPreferredSize(new Dimension(155, 34));
        doctorFilter.setPreferredSize(new Dimension(190, 34));
        specialtyFilter.setPreferredSize(new Dimension(180, 34));
        filterRow.add(statusFilter);
        filterRow.add(doctorFilter);
        filterRow.add(specialtyFilter);
        card.add(filterRow);
        return card;
    }

    private JPanel createCatalogTab() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 12, 12, 12)));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        catalogSearch.setPreferredSize(new Dimension(300, 34));
        catalogSearch.addActionListener(event -> loadCatalog());
        catalogStatusFilter.setPreferredSize(new Dimension(170, 34));
        catalogSearchButton.addActionListener(event -> loadCatalog());
        filters.add(catalogSearch);
        filters.add(catalogStatusFilter);
        filters.add(catalogSearchButton);
        card.add(filters, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(catalogTable);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 470));
        card.add(scroll, BorderLayout.CENTER);
        JLabel note = new JLabel(
                "Danh mục chỉ để tra cứu. Kê đơn không tạo phiếu xuất và không thay đổi tồn kho.");
        note.setForeground(MainFrame.MUTED);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(note, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 0, 0, 0));
        JPanel info = new JPanel(new BorderLayout(8, 0));
        info.setOpaque(false);
        resultCount.setForeground(MainFrame.MUTED);
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        info.add(resultCount, BorderLayout.WEST);
        info.add(message, BorderLayout.CENTER);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paging.setOpaque(false);
        previousButton.addActionListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                loadPrescriptions();
            }
        });
        nextButton.addActionListener(event -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadPrescriptions();
            }
        });
        paging.add(previousButton);
        paging.add(pageLabel);
        paging.add(nextButton);
        footer.add(info, BorderLayout.CENTER);
        footer.add(paging, BorderLayout.EAST);
        return footer;
    }

    private void configureTables() {
        configureTable(prescriptionTable,
                new int[]{85, 80, 85, 160, 115, 145, 145, 130, 105, 110, 125});
        prescriptionTable.getColumnModel().getColumn(9)
                .setCellRenderer(new StatusRenderer());
        prescriptionTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        prescriptionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && prescriptionTable.getSelectedRow() >= 0) {
                    showSelectedDetail();
                }
            }
        });
        configureTable(catalogTable, new int[]{100, 220, 180, 105, 155, 95, 155, 125});
        catalogTable.getColumnModel().getColumn(7)
                .setCellRenderer(new StatusRenderer());
    }

    private void configureTable(JTable table, int[] widths) {
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
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void configureEvents() {
        fromDate.addChangeListener(event -> {
            if (!suppressFilterEvents && dateFilter.isSelected()) {
                resetAndLoad();
            }
        });
        toDate.addChangeListener(event -> {
            if (!suppressFilterEvents && dateFilter.isSelected()) {
                resetAndLoad();
            }
        });
        statusFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
        doctorFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
        specialtyFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
        catalogStatusFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                loadCatalog();
            }
        });
    }

    private void loadInitialData() {
        setMessage("Đang tải đơn thuốc và danh mục...", MainFrame.MUTED);
        setControlsEnabled(false);
        long request = ++loadVersion;
        PrescriptionFilter filter = currentFilter();
        new SwingWorker<InitialData, Void>() {
            @Override
            protected InitialData doInBackground() throws PrescriptionException {
                PrescriptionStats stats = prescriptionBUS.getStats(session);
                PrescriptionPage page = prescriptionBUS.search(session, filter);
                List<String> statuses = prescriptionBUS.getStatuses(session);
                List<AppointmentDoctor> doctors = prescriptionBUS.getDoctors(session);
                List<AppointmentSpecialty> specialties =
                        prescriptionBUS.getSpecialties(session);
                List<MedicationCatalogItem> catalog =
                        prescriptionBUS.getMedications(session, "");
                return new InitialData(
                        stats, page, statuses, doctors, specialties, catalog);
            }

            @Override
            protected void done() {
                if (request != loadVersion) {
                    return;
                }
                try {
                    InitialData data = get();
                    populateFilters(data.statuses(), data.doctors(), data.specialties());
                    medications = data.catalog();
                    renderStats(data.stats());
                    renderPage(data.page());
                    renderCatalog(medications);
                    referencesLoaded = true;
                    setMessage(data.page().totalCount() == 0
                            ? "Chưa có đơn thuốc phù hợp với bộ lọc." : " ",
                            MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải dữ liệu đơn thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    markStatsFailed();
                    showError("Không thể tải dữ liệu đơn thuốc.", exception.getCause());
                } finally {
                    if (request == loadVersion) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private void populateFilters(
            List<String> statuses,
            List<AppointmentDoctor> doctors,
            List<AppointmentSpecialty> specialties) {
        suppressFilterEvents = true;
        statusFilter.removeAllItems();
        statusFilter.addItem(new Choice("", "Tất cả trạng thái"));
        for (String status : statuses) {
            statusFilter.addItem(new Choice(status, statusLabel(status)));
        }
        doctorFilter.removeAllItems();
        doctorFilter.addItem(new Choice("", "Tất cả bác sĩ"));
        for (AppointmentDoctor doctor : doctors) {
            doctorFilter.addItem(new Choice(doctor.maNhanVien(),
                    doctor.hoTen() + " (" + doctor.maNhanVien() + ")"));
        }
        specialtyFilter.removeAllItems();
        specialtyFilter.addItem(new Choice("", "Tất cả chuyên khoa"));
        for (AppointmentSpecialty specialty : specialties) {
            specialtyFilter.addItem(new Choice(
                    specialty.maChuyenKhoa(), specialty.tenChuyenKhoa()));
        }
        catalogStatusFilter.removeAllItems();
        catalogStatusFilter.addItem(new Choice("", "Tất cả trạng thái thuốc"));
        catalogStatusFilter.addItem(new Choice("DangSuDung", "Đang sử dụng"));
        catalogStatusFilter.addItem(new Choice("NgungSuDung", "Ngừng sử dụng"));
        suppressFilterEvents = false;
    }

    private void loadPrescriptions() {
        long request = ++loadVersion;
        PrescriptionFilter filter = currentFilter();
        setControlsEnabled(false);
        setMessage("Đang tải đơn thuốc...", MainFrame.MUTED);
        new SwingWorker<DataBundle, Void>() {
            @Override
            protected DataBundle doInBackground() throws PrescriptionException {
                return new DataBundle(
                        prescriptionBUS.getStats(session),
                        prescriptionBUS.search(session, filter));
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
                            ? "Chưa có đơn thuốc phù hợp với bộ lọc." : " ",
                            MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải đơn thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    markStatsFailed();
                    showError("Không thể tải đơn thuốc.", exception.getCause());
                } finally {
                    if (request == loadVersion) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private void loadCatalog() {
        String query = catalogSearch.getText().trim();
        Choice status = (Choice) catalogStatusFilter.getSelectedItem();
        String selectedStatus = status == null ? "" : status.code();
        catalogSearch.setEnabled(false);
        catalogSearchButton.setEnabled(false);
        catalogStatusFilter.setEnabled(false);
        new SwingWorker<List<MedicationCatalogItem>, Void>() {
            @Override
            protected List<MedicationCatalogItem> doInBackground()
                    throws PrescriptionException {
                return prescriptionBUS.getMedications(session, query);
            }

            @Override
            protected void done() {
                try {
                    List<MedicationCatalogItem> found = get();
                    medications = found;
                    renderCatalog(found.stream()
                            .filter(item -> selectedStatus.isBlank()
                                    || selectedStatus.equals(item.trangThai()))
                            .toList());
                    setMessage(found.isEmpty()
                            ? "Không tìm thấy thuốc phù hợp." : " ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải danh mục thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải danh mục thuốc.", exception.getCause());
                } finally {
                    catalogSearch.setEnabled(true);
                    catalogSearchButton.setEnabled(true);
                    catalogStatusFilter.setEnabled(true);
                }
            }
        }.execute();
    }

    private PrescriptionFilter currentFilter() {
        LocalDate from = dateFilter.isSelected() ? dateValue(fromDate) : null;
        LocalDate to = dateFilter.isSelected() ? dateValue(toDate) : null;
        Choice status = (Choice) statusFilter.getSelectedItem();
        Choice doctor = (Choice) doctorFilter.getSelectedItem();
        Choice specialty = (Choice) specialtyFilter.getSelectedItem();
        return new PrescriptionFilter(
                searchField.getText(),
                from,
                to,
                status == null ? "" : status.code(),
                doctor == null ? "" : doctor.code(),
                specialty == null ? "" : specialty.code(),
                currentPage,
                PAGE_SIZE);
    }

    private void renderStats(PrescriptionStats stats) {
        totalStat.setText(Long.toString(stats.total()));
        todayStat.setText(Long.toString(stats.createdToday()));
        draftsStat.setText(Long.toString(stats.drafts()));
        signedStat.setText(Long.toString(stats.signed()));
        dispensedStat.setText(Long.toString(stats.sentOrDispensed()));
    }

    private void markStatsFailed() {
        totalStat.setText("!");
        todayStat.setText("!");
        draftsStat.setText("!");
        signedStat.setText("!");
        dispensedStat.setText("!");
    }

    private void renderPage(PrescriptionPage page) {
        prescriptions = page.prescriptions();
        currentPage = page.page();
        totalPages = page.totalPages();
        prescriptionModel.setRowCount(0);
        for (PrescriptionListItem item : prescriptions) {
            prescriptionModel.addRow(new Object[]{
                    item.maDonThuoc(),
                    item.maLuotKham(),
                    safe(item.maBenhNhan()),
                    safe(item.tenBenhNhan()),
                    safe(item.soDienThoai()),
                    safe(item.tenBacSi()),
                    safe(item.tenChuyenKhoa()),
                    format(item.ngayKe()),
                    item.soLoaiThuoc(),
                    statusLabel(item.trangThai()),
                    item.daXuatThuoc() ? "Đã liên kết" : "Chưa xuất"
            });
        }
        resultCount.setText("Tổng " + page.totalCount() + " đơn thuốc");
        pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
        updateSelectionActions();
    }

    private void renderCatalog(List<MedicationCatalogItem> catalog) {
        catalogModel.setRowCount(0);
        for (MedicationCatalogItem item : catalog) {
            catalogModel.addRow(new Object[]{
                    item.maThuoc(),
                    item.tenThuoc(),
                    safe(item.hoatChat()),
                    safe(item.hamLuong()),
                    safe(item.dangBaoChe()),
                    safe(item.donViTinh()),
                    safe(item.duongDungMacDinh()),
                    medicationStatusLabel(item.trangThai())
            });
        }
    }

    private void showSelectedDetail() {
        PrescriptionListItem selected = selectedPrescription();
        if (selected == null) {
            return;
        }
        detailButton.setEnabled(false);
        new SwingWorker<PrescriptionDetail, Void>() {
            @Override
            protected PrescriptionDetail doInBackground() throws PrescriptionException {
                return prescriptionBUS.getById(session, selected.maDonThuoc());
            }

            @Override
            protected void done() {
                try {
                    new PrescriptionDetailDialog(
                            SwingUtilities.getWindowAncestor(PrescriptionPanel.this), get())
                            .setVisible(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chi tiết đơn thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải chi tiết đơn thuốc.", exception.getCause());
                } finally {
                    updateSelectionActions();
                }
            }
        }.execute();
    }

    private void openEditor(Long prescriptionId) {
        new PrescriptionDialog(
                SwingUtilities.getWindowAncestor(this),
                session,
                prescriptionBUS,
                prescriptionId,
                savedId -> {
                    currentPage = 0;
                    refreshData();
                    JOptionPane.showMessageDialog(
                            this,
                            (prescriptionId == null ? "Đã tạo đơn thuốc mã "
                                    : "Đã cập nhật đơn thuốc mã ") + savedId + ".",
                            "Kê đơn thuốc",
                            JOptionPane.INFORMATION_MESSAGE);
                }).setVisible(true);
    }

    private void cancelSelected() {
        PrescriptionListItem selected = selectedPrescription();
        if (selected == null || !prescriptionBUS.canCancel(session, selected)) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Hủy đơn thuốc " + selected.maDonThuoc()
                        + "? Đơn và chi tiết vẫn được giữ lại; không hoàn tác xuất thuốc.",
                "Xác nhận hủy đơn thuốc",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        setControlsEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws PrescriptionException {
                prescriptionBUS.cancel(session, selected.maDonThuoc());
                return null;
            }

            @Override
            protected void done() {
                boolean reloading = false;
                try {
                    get();
                    setMessage("Đã hủy đơn thuốc " + selected.maDonThuoc() + ".",
                            new Color(35, 130, 80));
                    loadPrescriptions();
                    reloading = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Hủy đơn thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể hủy đơn thuốc.", exception.getCause());
                } finally {
                    if (!reloading) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private PrescriptionListItem selectedPrescription() {
        int selectedRow = prescriptionTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = prescriptionTable.convertRowIndexToModel(selectedRow);
        return modelRow < 0 || modelRow >= prescriptions.size()
                ? null : prescriptions.get(modelRow);
    }

    private void updateSelectionActions() {
        PrescriptionListItem selected = selectedPrescription();
        detailButton.setEnabled(selected != null);
        editButton.setEnabled(prescriptionBUS.canEdit(session, selected));
        cancelButton.setEnabled(prescriptionBUS.canCancel(session, selected));
    }

    private void clearFilters() {
        suppressFilterEvents = true;
        searchField.setText("");
        dateFilter.setSelected(false);
        fromDate.setValue(today());
        toDate.setValue(today());
        setDateEditorsEnabled(false);
        selectFirst(statusFilter);
        selectFirst(doctorFilter);
        selectFirst(specialtyFilter);
        suppressFilterEvents = false;
        resetAndLoad();
    }

    private void resetAndLoad() {
        if (!suppressFilterEvents) {
            currentPage = 0;
            loadPrescriptions();
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

    private void setDateEditorsEnabled(boolean enabled) {
        fromDate.setEnabled(enabled);
        toDate.setEnabled(enabled);
    }

    private void selectFirst(JComboBox<Choice> combo) {
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        searchField.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        dateFilter.setEnabled(enabled);
        setDateEditorsEnabled(enabled && dateFilter.isSelected());
        statusFilter.setEnabled(enabled);
        doctorFilter.setEnabled(enabled);
        specialtyFilter.setEnabled(enabled);
        createButton.setEnabled(enabled);
        prescriptionTable.setEnabled(enabled);
        catalogSearch.setEnabled(enabled);
        catalogSearchButton.setEnabled(enabled);
        catalogStatusFilter.setEnabled(enabled);
        if (enabled) {
            updateSelectionActions();
            previousButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage + 1 < totalPages);
        } else {
            detailButton.setEnabled(false);
            editButton.setEnabled(false);
            cancelButton.setEnabled(false);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
        }
    }

    private void showError(String title, Throwable cause) {
        Throwable source = cause;
        while (source != null && source.getCause() != null
                && !(source instanceof PrescriptionException)) {
            source = source.getCause();
        }
        String detail = cause instanceof PrescriptionException
                ? cause.getMessage()
                : source != null && source.getMessage() != null
                        ? source.getMessage() : title;
        setMessage(detail, new Color(190, 62, 62));
        if (!(cause instanceof PrescriptionException)) {
            LOGGER.log(Level.SEVERE, title, cause);
        }
        JOptionPane.showMessageDialog(
                this, detail, "Quản lý đơn thuốc", JOptionPane.ERROR_MESSAGE);
    }

    private void setMessage(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }

    private JPanel gap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(1, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return gap;
    }

    private String format(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "Nhap" -> "Nháp";
            case "DaKy" -> "Đã ký";
            case "DaGuiKho" -> "Đã gửi kho";
            case "DaCapPhat" -> "Đã cấp phát";
            case "Huy" -> "Đã hủy";
            default -> safe(status);
        };
    }

    private String medicationStatusLabel(String status) {
        return "DangSuDung".equals(status) ? "Đang sử dụng"
                : "NgungSuDung".equals(status) ? "Ngừng sử dụng" : safe(status);
    }

    private record Choice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record DataBundle(PrescriptionStats stats, PrescriptionPage page) {
    }

    private record InitialData(
            PrescriptionStats stats,
            PrescriptionPage page,
            List<String> statuses,
            List<AppointmentDoctor> doctors,
            List<AppointmentSpecialty> specialties,
            List<MedicationCatalogItem> catalog) {
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            java.awt.Component component = super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            setHorizontalAlignment(CENTER);
            if (!selected) {
                String text = value == null ? "" : value.toString();
                setForeground(text.equals("Nháp") || text.equals("Đang sử dụng")
                        ? new Color(35, 130, 80)
                        : text.equals("Đã hủy") || text.equals("Ngừng sử dụng")
                                ? new Color(170, 65, 65) : MainFrame.TEXT);
            }
            return component;
        }
    }
}
