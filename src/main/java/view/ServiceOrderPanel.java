package view;

import bus.serviceorder.ServiceOrderException;
import bus.serviceorder.ServiceOrderBUS;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
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
import model.auth.AuthenticatedSession;
import model.serviceorder.ServiceCatalogItem;
import model.serviceorder.ServiceOrderDetail;
import model.serviceorder.ServiceOrderFilter;
import model.serviceorder.ServiceOrderListItem;
import model.serviceorder.ServiceOrderPage;
import model.serviceorder.ServiceOrderStats;

final class ServiceOrderPanel extends JPanel {
    private static final Logger LOGGER =
            Logger.getLogger(ServiceOrderPanel.class.getName());
    private static final int PAGE_SIZE = 25;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final ServiceOrderBUS serviceOrderBUS;
    private final JTextField searchField = new JTextField();
    private final JCheckBox dateFilter = new JCheckBox("Lọc ngày chỉ định");
    private final JSpinner fromDate = new JSpinner(new SpinnerDateModel());
    private final JSpinner toDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<Choice> orderTypeFilter = new JComboBox<>();
    private final JComboBox<Choice> statusFilter = new JComboBox<>();
    private final JTextField catalogSearch = new JTextField();
    private final JComboBox<Choice> catalogTypeFilter = new JComboBox<>();
    private final JLabel totalStat = new JLabel("—");
    private final JLabel todayStat = new JLabel("—");
    private final JLabel pendingStat = new JLabel("—");
    private final JLabel completedStat = new JLabel("—");
    private final JLabel resultCount = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JLabel message = new JLabel(" ");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton searchButton = new JButton("Tìm kiếm");
    private final JButton clearButton = new JButton("Xóa bộ lọc");
    private final JButton createButton = new JButton("+ Tạo chỉ định");
    private final JButton previousButton = new JButton("Trước");
    private final JButton nextButton = new JButton("Sau");
    private final JButton detailButton = new JButton("Xem chi tiết");
    private final JButton visitOrdersButton = new JButton("Xem theo lượt");
    private final JButton editButton = new JButton("Chỉnh sửa");
    private final JButton cancelOrderButton = new JButton("Hủy chỉ định");
    private final JButton catalogSearchButton = new JButton("Tìm dịch vụ");
    private final DefaultTableModel orderModel = new DefaultTableModel(
            new Object[]{"Mã chỉ định", "Mã lượt", "Mã BN", "Bệnh nhân",
                "Dịch vụ", "Loại dịch vụ", "Bác sĩ phụ trách",
                "Thời gian chỉ định", "Trạng thái", "Tính phí"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable orderTable = new JTable(orderModel);
    private final DefaultTableModel catalogModel = new DefaultTableModel(
            new Object[]{"Mã dịch vụ", "Tên dịch vụ", "Loại dịch vụ",
                "Đơn vị tính", "Giá hiện hành (tham khảo)", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable catalogTable = new JTable(catalogModel);
    private List<ServiceOrderListItem> orders = List.of();
    private List<ServiceCatalogItem> services = List.of();
    private List<String> serviceTypes = List.of();
    private int currentPage;
    private int totalPages = 1;
    private long loadVersion;
    private boolean suppressFilterEvents;
    private boolean referencesLoaded;

    ServiceOrderPanel(AuthenticatedSession session, ServiceOrderBUS serviceOrderBUS) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException(
                    "Không thể mở chỉ định dịch vụ khi chưa đăng nhập.");
        }
        this.session = session;
        this.serviceOrderBUS = serviceOrderBUS;
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
            loadOrders();
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
        JLabel title = new JLabel("Quản lý chỉ định dịch vụ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel subtitle = new JLabel(
                "Quản lý các dịch vụ được bác sĩ chỉ định cho bệnh nhân.");
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
        cards.add(statCard("Tổng số chỉ định", totalStat, new Color(0xE8F7F5)));
        cards.add(statCard("Chỉ định trong ngày", todayStat, new Color(0xEAF2FF)));
        cards.add(statCard("Chờ thực hiện", pendingStat, new Color(0xFFF5E5)));
        cards.add(statCard("Đã hoàn tất", completedStat, new Color(0xEAF7EF)));
        cards.setPreferredSize(new Dimension(0, 90));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        return cards;
    }

    private JPanel statCard(String title, JLabel value, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 16, 12, 16)));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MainFrame.MUTED);
        value.setOpaque(true);
        value.setBackground(color);
        value.setFont(new Font("Segoe UI", Font.BOLD, 23));
        value.setForeground(MainFrame.TEXT);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.addTab("Danh sách chỉ định", createOrderTab());
        tabs.addTab("Danh mục dịch vụ", createCatalogTab());
        return tabs;
    }

    private JPanel createOrderTab() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);
        root.add(createOrderFilters(), BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 12, 10, 12)));
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel title = new JLabel("Danh sách chỉ định dịch vụ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(MainFrame.TEXT);
        titleBar.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        detailButton.addActionListener(event -> showSelectedDetail());
        visitOrdersButton.addActionListener(event -> showOrdersForSelectedVisit());
        editButton.addActionListener(event -> {
            ServiceOrderListItem item = selectedOrder();
            if (item != null && serviceOrderBUS.canEdit(session, item)) {
                openEditor(item.maChiDinh());
            }
        });
        cancelOrderButton.addActionListener(event -> cancelSelected());
        actions.add(visitOrdersButton);
        actions.add(detailButton);
        actions.add(editButton);
        actions.add(cancelOrderButton);
        titleBar.add(actions, BorderLayout.EAST);
        card.add(titleBar, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(orderTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 420));
        card.add(tableScroll, BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);
        root.add(card, BorderLayout.CENTER);
        return root;
    }

    private JPanel createOrderFilters() {
        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchField.setPreferredSize(new Dimension(260, 34));
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
            boolean enabled = dateFilter.isSelected();
            fromDate.setEnabled(enabled);
            toDate.setEnabled(enabled);
            resetAndLoad();
        });
        filterRow.add(dateFilter);
        filterRow.add(fromDate);
        filterRow.add(new JLabel("Đến"));
        filterRow.add(toDate);
        statusFilter.setPreferredSize(new Dimension(175, 34));
        orderTypeFilter.setPreferredSize(new Dimension(210, 34));
        filterRow.add(statusFilter);
        filterRow.add(orderTypeFilter);
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
        catalogTypeFilter.setPreferredSize(new Dimension(220, 34));
        catalogSearchButton.addActionListener(event -> loadCatalog());
        filters.add(catalogSearch);
        filters.add(catalogTypeFilter);
        filters.add(catalogSearchButton);
        card.add(filters, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(catalogTable);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 470));
        card.add(scroll, BorderLayout.CENTER);
        JLabel note = new JLabel(
                "Giá lấy từ bảng giá đang hiệu lực; chỉ để tham khảo, không được lưu trên chỉ định.");
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
                loadOrders();
            }
        });
        nextButton.addActionListener(event -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadOrders();
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
        configureTable(orderTable, new int[]{90, 85, 90, 160, 185, 140, 150, 140, 120, 90});
        orderTable.getColumnModel().getColumn(8).setCellRenderer(new StatusRenderer());
        orderTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        orderTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && orderTable.getSelectedRow() >= 0) {
                    showSelectedDetail();
                }
            }
        });
        configureTable(catalogTable, new int[]{110, 230, 190, 110, 180, 130});
        catalogTable.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());
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
        orderTypeFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                resetAndLoad();
            }
        });
        catalogTypeFilter.addActionListener(event -> {
            if (!suppressFilterEvents) {
                loadCatalog();
            }
        });
    }

    private void loadInitialData() {
        setMessage("Đang tải dữ liệu chỉ định và danh mục...", MainFrame.MUTED);
        setControlsEnabled(false);
        long request = ++loadVersion;
        ServiceOrderFilter filter = currentFilter();
        new SwingWorker<InitialData, Void>() {
            @Override
            protected InitialData doInBackground() throws ServiceOrderException {
                ServiceOrderStats stats = serviceOrderBUS.getStats(session);
                ServiceOrderPage page = serviceOrderBUS.search(session, filter);
                List<String> types = serviceOrderBUS.getServiceTypes(session);
                List<String> statuses = serviceOrderBUS.getOrderStatuses(session);
                List<ServiceCatalogItem> catalog =
                        serviceOrderBUS.getServices(session, "", "");
                return new InitialData(stats, page, types, statuses, catalog);
            }

            @Override
            protected void done() {
                if (request != loadVersion) {
                    return;
                }
                try {
                    InitialData data = get();
                    serviceTypes = data.types();
                    populateFilters(data.statuses());
                    services = data.catalog();
                    renderStats(data.stats());
                    renderPage(data.page());
                    renderCatalog(services);
                    referencesLoaded = true;
                    setMessage(data.page().totalCount() == 0
                            ? "Chưa có chỉ định phù hợp với bộ lọc." : " ",
                            MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải dữ liệu bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải dữ liệu chỉ định.", exception.getCause());
                } finally {
                    if (request == loadVersion) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private void populateFilters(List<String> statuses) {
        suppressFilterEvents = true;
        statusFilter.removeAllItems();
        statusFilter.addItem(new Choice("", "Tất cả trạng thái"));
        for (String status : statuses) {
            statusFilter.addItem(new Choice(status, statusLabel(status)));
        }
        orderTypeFilter.removeAllItems();
        orderTypeFilter.addItem(new Choice("", "Tất cả loại dịch vụ"));
        catalogTypeFilter.removeAllItems();
        catalogTypeFilter.addItem(new Choice("", "Tất cả loại dịch vụ"));
        for (String type : serviceTypes) {
            Choice choice = new Choice(type, type);
            orderTypeFilter.addItem(choice);
            catalogTypeFilter.addItem(choice);
        }
        suppressFilterEvents = false;
    }

    private void loadOrders() {
        long request = ++loadVersion;
        ServiceOrderFilter filter = currentFilter();
        setControlsEnabled(false);
        setMessage("Đang tải chỉ định...", MainFrame.MUTED);
        new SwingWorker<DataBundle, Void>() {
            @Override
            protected DataBundle doInBackground() throws ServiceOrderException {
                return new DataBundle(
                        serviceOrderBUS.getStats(session),
                        serviceOrderBUS.search(session, filter));
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
                            ? "Chưa có chỉ định phù hợp với bộ lọc." : " ",
                            MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chỉ định bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    totalStat.setText("!");
                    todayStat.setText("!");
                    pendingStat.setText("!");
                    completedStat.setText("!");
                    showError("Không thể tải chỉ định.", exception.getCause());
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
        Choice type = (Choice) catalogTypeFilter.getSelectedItem();
        String selectedType = type == null ? "" : type.code();
        setMessage("Đang tải danh mục dịch vụ...", MainFrame.MUTED);
        new SwingWorker<List<ServiceCatalogItem>, Void>() {
            @Override
            protected List<ServiceCatalogItem> doInBackground() throws ServiceOrderException {
                return serviceOrderBUS.getServices(session, query, selectedType);
            }

            @Override
            protected void done() {
                try {
                    services = get();
                    renderCatalog(services);
                    setMessage(services.isEmpty()
                            ? "Không có dịch vụ phù hợp." : " ", MainFrame.MUTED);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải danh mục dịch vụ bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải danh mục dịch vụ.", exception.getCause());
                }
            }
        }.execute();
    }

    private ServiceOrderFilter currentFilter() {
        LocalDate from = dateFilter.isSelected() ? dateValue(fromDate) : null;
        LocalDate to = dateFilter.isSelected() ? dateValue(toDate) : null;
        Choice type = (Choice) orderTypeFilter.getSelectedItem();
        Choice status = (Choice) statusFilter.getSelectedItem();
        return new ServiceOrderFilter(
                searchField.getText(),
                from,
                to,
                type == null ? "" : type.code(),
                status == null ? "" : status.code(),
                currentPage,
                PAGE_SIZE);
    }

    private void renderStats(ServiceOrderStats stats) {
        totalStat.setText(Long.toString(stats.total()));
        todayStat.setText(Long.toString(stats.orderedToday()));
        pendingStat.setText(Long.toString(stats.pending()));
        completedStat.setText(Long.toString(stats.completed()));
    }

    private void renderPage(ServiceOrderPage page) {
        orders = page.orders();
        currentPage = page.page();
        totalPages = page.totalPages();
        orderModel.setRowCount(0);
        for (ServiceOrderListItem order : orders) {
            orderModel.addRow(new Object[]{
                    order.maChiDinh(),
                    order.maLuotKham(),
                    safe(order.maBenhNhan()),
                    safe(order.tenBenhNhan()),
                    safe(order.tenDichVu()),
                    safe(order.loaiDichVu()),
                    safe(order.tenBacSi()),
                    format(order.thoiGianChiDinh()),
                    statusLabel(order.trangThai()),
                    order.daTinhPhi() ? "Đã tính" : "Chưa tính"
            });
        }
        resultCount.setText("Tổng " + page.totalCount() + " chỉ định");
        pageLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage + 1 < totalPages);
        updateSelectionActions();
    }

    private void renderCatalog(List<ServiceCatalogItem> catalog) {
        catalogModel.setRowCount(0);
        for (ServiceCatalogItem item : catalog) {
            catalogModel.addRow(new Object[]{
                    item.maDichVu(),
                    item.tenDichVu(),
                    safe(item.loaiDichVu()),
                    safe(item.donViTinh()),
                    item.giaHienHanh() == null
                            ? "Chưa có giá" : item.giaHienHanh().toPlainString() + " đ",
                    item.dangApDung() ? "Đang áp dụng" : "Ngừng áp dụng"
            });
        }
    }

    private void showSelectedDetail() {
        ServiceOrderListItem selected = selectedOrder();
        if (selected == null) {
            return;
        }
        setControlsEnabled(false);
        new SwingWorker<ServiceOrderDetail, Void>() {
            @Override
            protected ServiceOrderDetail doInBackground() throws ServiceOrderException {
                return serviceOrderBUS.getById(session, selected.maChiDinh());
            }

            @Override
            protected void done() {
                try {
                    new ServiceOrderDetailDialog(
                            SwingUtilities.getWindowAncestor(ServiceOrderPanel.this), get())
                            .setVisible(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chi tiết chỉ định bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải chi tiết chỉ định.", exception.getCause());
                } finally {
                    setControlsEnabled(true);
                }
            }
        }.execute();
    }

    private void showOrdersForSelectedVisit() {
        ServiceOrderListItem selected = selectedOrder();
        if (selected == null) {
            return;
        }
        setControlsEnabled(false);
        new SwingWorker<List<ServiceOrderListItem>, Void>() {
            @Override
            protected List<ServiceOrderListItem> doInBackground()
                    throws ServiceOrderException {
                return serviceOrderBUS.getByVisit(session, selected.maLuotKham());
            }

            @Override
            protected void done() {
                try {
                    new ServiceOrderVisitDialog(
                            SwingUtilities.getWindowAncestor(ServiceOrderPanel.this),
                            selected.maLuotKham(),
                            get(),
                            ServiceOrderPanel.this::showOrderDetail)
                            .setVisible(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chỉ định theo lượt khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải chỉ định theo lượt khám.", exception.getCause());
                } finally {
                    setControlsEnabled(true);
                }
            }
        }.execute();
    }

    private void showOrderDetail(long orderId) {
        new SwingWorker<ServiceOrderDetail, Void>() {
            @Override
            protected ServiceOrderDetail doInBackground() throws ServiceOrderException {
                return serviceOrderBUS.getById(session, orderId);
            }

            @Override
            protected void done() {
                try {
                    new ServiceOrderDetailDialog(
                            SwingUtilities.getWindowAncestor(ServiceOrderPanel.this), get())
                            .setVisible(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải chi tiết chỉ định bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải chi tiết chỉ định.", exception.getCause());
                }
            }
        }.execute();
    }

    private void openEditor(Long orderId) {
        new ServiceOrderDialog(
                SwingUtilities.getWindowAncestor(this),
                session,
                serviceOrderBUS,
                orderId,
                savedId -> {
                    currentPage = 0;
                    refreshData();
                    JOptionPane.showMessageDialog(
                            this,
                            (orderId == null ? "Đã tạo chỉ định mã " : "Đã cập nhật chỉ định mã ")
                                    + savedId + ".",
                            "Chỉ định dịch vụ",
                            JOptionPane.INFORMATION_MESSAGE);
                }).setVisible(true);
    }

    private void cancelSelected() {
        ServiceOrderListItem selected = selectedOrder();
        if (selected == null || !serviceOrderBUS.canCancel(session, selected)) {
            return;
        }
        String reason = JOptionPane.showInputDialog(
                this, "Nhập lý do hủy (tối đa 255 ký tự):", "Hủy chỉ định",
                JOptionPane.QUESTION_MESSAGE);
        if (reason == null || reason.isBlank()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this, "Hủy chỉ định " + selected.maChiDinh() + "?",
                "Xác nhận hủy", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        setControlsEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws ServiceOrderException {
                serviceOrderBUS.cancel(session, selected.maChiDinh(), reason.trim());
                return null;
            }

            @Override
            protected void done() {
                boolean reloading = false;
                try {
                    get();
                    setMessage("Đã hủy chỉ định " + selected.maChiDinh() + ".",
                            new Color(35, 130, 80));
                    loadOrders();
                    reloading = true;
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Hủy chỉ định bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể hủy chỉ định.", exception.getCause());
                } finally {
                    if (!reloading) {
                        setControlsEnabled(true);
                    }
                }
            }
        }.execute();
    }

    private ServiceOrderListItem selectedOrder() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = orderTable.convertRowIndexToModel(selectedRow);
        return modelRow < 0 || modelRow >= orders.size() ? null : orders.get(modelRow);
    }

    private void updateSelectionActions() {
        ServiceOrderListItem selected = selectedOrder();
        detailButton.setEnabled(selected != null);
        visitOrdersButton.setEnabled(selected != null);
        editButton.setEnabled(serviceOrderBUS.canEdit(session, selected));
        cancelOrderButton.setEnabled(serviceOrderBUS.canCancel(session, selected));
    }

    private void clearFilters() {
        suppressFilterEvents = true;
        searchField.setText("");
        dateFilter.setSelected(false);
        fromDate.setValue(today());
        toDate.setValue(today());
        fromDate.setEnabled(false);
        toDate.setEnabled(false);
        if (statusFilter.getItemCount() > 0) {
            statusFilter.setSelectedIndex(0);
        }
        if (orderTypeFilter.getItemCount() > 0) {
            orderTypeFilter.setSelectedIndex(0);
        }
        suppressFilterEvents = false;
        resetAndLoad();
    }

    private void resetAndLoad() {
        if (!suppressFilterEvents) {
            currentPage = 0;
            loadOrders();
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
        refreshButton.setEnabled(enabled);
        searchField.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        dateFilter.setEnabled(enabled);
        fromDate.setEnabled(enabled && dateFilter.isSelected());
        toDate.setEnabled(enabled && dateFilter.isSelected());
        statusFilter.setEnabled(enabled);
        orderTypeFilter.setEnabled(enabled);
        createButton.setEnabled(enabled);
        orderTable.setEnabled(enabled);
        catalogSearch.setEnabled(enabled);
        catalogSearchButton.setEnabled(enabled);
        catalogTypeFilter.setEnabled(enabled);
        if (enabled) {
            updateSelectionActions();
            previousButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage + 1 < totalPages);
        } else {
            detailButton.setEnabled(false);
            visitOrdersButton.setEnabled(false);
            editButton.setEnabled(false);
            cancelOrderButton.setEnabled(false);
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
        }
    }

    private void showError(String title, Throwable cause) {
        Throwable source = cause;
        while (source != null && source.getCause() != null
                && !(source instanceof ServiceOrderException)) {
            source = source.getCause();
        }
        String detail = cause instanceof ServiceOrderException
                ? cause.getMessage()
                : source != null && source.getMessage() != null
                        ? source.getMessage() : title;
        setMessage(detail, new Color(190, 62, 62));
        if (!(cause instanceof ServiceOrderException)) {
            LOGGER.log(Level.SEVERE, title, cause);
        }
        JOptionPane.showMessageDialog(
                this, detail, "Quản lý chỉ định dịch vụ", JOptionPane.ERROR_MESSAGE);
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
            case "ChoThucHien" -> "Chờ thực hiện";
            case "DangThucHien" -> "Đang thực hiện";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }

    private record Choice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record InitialData(
            ServiceOrderStats stats,
            ServiceOrderPage page,
            List<String> types,
            List<String> statuses,
            List<ServiceCatalogItem> catalog) {
    }

    private record DataBundle(ServiceOrderStats stats, ServiceOrderPage page) {
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            String text = value == null ? "" : value.toString();
            label.setHorizontalAlignment(CENTER);
            if (!selected) {
                if ("Chờ thực hiện".equals(text)) {
                    label.setForeground(new Color(160, 105, 25));
                } else if ("Đang thực hiện".equals(text)) {
                    label.setForeground(new Color(30, 95, 175));
                } else if ("Hoàn tất".equals(text)
                        || "Đang áp dụng".equals(text)) {
                    label.setForeground(new Color(30, 130, 75));
                } else if ("Đã hủy".equals(text)
                        || "Ngừng áp dụng".equals(text)) {
                    label.setForeground(new Color(170, 55, 55));
                }
            }
            return label;
        }
    }
}
