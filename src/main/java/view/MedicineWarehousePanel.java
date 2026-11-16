package view;

import bus.warehouse.WarehouseBUS;
import bus.warehouse.WarehouseException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.auth.AuthenticatedSession;
import model.warehouse.WarehouseModels.Batch;
import model.warehouse.WarehouseModels.BatchDetail;
import model.warehouse.WarehouseModels.BatchFilter;
import model.warehouse.WarehouseModels.CreatedReceipt;
import model.warehouse.WarehouseModels.ExportCandidate;
import model.warehouse.WarehouseModels.ExportFilter;
import model.warehouse.WarehouseModels.ExportPreview;
import model.warehouse.WarehouseModels.ImportDraft;
import model.warehouse.WarehouseModels.InventoryFilter;
import model.warehouse.WarehouseModels.InventoryItem;
import model.warehouse.WarehouseModels.Medicine;
import model.warehouse.WarehouseModels.MedicineDraft;
import model.warehouse.WarehouseModels.MovementFilter;
import model.warehouse.WarehouseModels.Page;
import model.warehouse.WarehouseModels.Receipt;
import model.warehouse.WarehouseModels.ReceiptDetail;
import model.warehouse.WarehouseModels.ReceiptFilter;
import model.warehouse.WarehouseModels.Stats;
import model.warehouse.WarehouseModels.StockMovement;

final class MedicineWarehousePanel extends JPanel {
    private static final Logger LOGGER =
            Logger.getLogger(MedicineWarehousePanel.class.getName());
    private static final int PAGE_SIZE = 25;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final WarehouseBUS warehouseBUS;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel medicineStat = new JLabel("—");
    private final JLabel batchStat = new JLabel("—");
    private final JLabel stockStat = new JLabel("—");
    private final JLabel warningStat = new JLabel("—");
    private final JLabel message = new JLabel(" ");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton addMedicineButton = new JButton("+ Thêm thuốc");
    private final JButton editMedicineButton = new JButton("Chỉnh sửa");
    private final JButton addImportButton = new JButton("+ Tạo phiếu nhập");
    private final JButton medicinePrevious = new JButton("Trước");
    private final JButton medicineNext = new JButton("Sau");
    private final JButton batchPrevious = new JButton("Trước");
    private final JButton batchNext = new JButton("Sau");
    private final JButton inventoryPrevious = new JButton("Trước");
    private final JButton inventoryNext = new JButton("Sau");
    private final JButton importPrevious = new JButton("Trước");
    private final JButton importNext = new JButton("Sau");
    private final JButton exportPrevious = new JButton("Trước");
    private final JButton exportNext = new JButton("Sau");
    private final JButton historyPrevious = new JButton("Trước");
    private final JButton historyNext = new JButton("Sau");
    private final JLabel medicinePageLabel = new JLabel("Trang 1 / 1");
    private final JLabel batchPageLabel = new JLabel("Trang 1 / 1");
    private final JLabel inventoryPageLabel = new JLabel("Trang 1 / 1");
    private final JLabel importPageLabel = new JLabel("Trang 1 / 1");
    private final JLabel exportPageLabel = new JLabel("Trang 1 / 1");
    private final JLabel historyPageLabel = new JLabel("Trang 1 / 1");

    private final JTextField medicineSearch = new JTextField();
    private final JComboBox<Choice> medicineStatus = new JComboBox<>();
    private final JTextField batchSearch = new JTextField();
    private final JComboBox<Choice> batchStatus = new JComboBox<>();
    private final JTextField inventorySearch = new JTextField();
    private final JComboBox<Choice> inventoryStatus = new JComboBox<>();
    private final JTextField importSearch = new JTextField();
    private final JComboBox<Choice> importStatus = new JComboBox<>();
    private final JTextField exportSearch = new JTextField();
    private final JTextField historySearch = new JTextField();
    private final JComboBox<Choice> historyType = new JComboBox<>();
    private final JComboBox<Choice> historyStatus = new JComboBox<>();

    private final DefaultTableModel medicineModel = model(
            "Mã thuốc", "Tên thuốc", "Hoạt chất", "Hàm lượng",
            "Dạng bào chế", "Đơn vị", "Đường dùng", "Trạng thái");
    private final DefaultTableModel batchModel = model(
            "Mã lô", "Mã thuốc", "Tên thuốc", "Số lô", "Hạn sử dụng",
            "Đơn giá nhập", "Tồn", "Đã nhập", "Đã xuất", "Vị trí", "Trạng thái");
    private final DefaultTableModel inventoryModel = model(
            "Mã thuốc", "Tên thuốc", "Hoạt chất", "Hàm lượng", "Đơn vị",
            "Trạng thái thuốc", "Tổng tồn", "Số lô", "Sắp hết hạn", "Hết hạn", "Hết hàng");
    private final DefaultTableModel importModel = model(
            "Mã phiếu", "Số phiếu", "Ngày nhập", "Nhà cung cấp", "Nhân viên",
            "Số dòng thuốc", "Dòng hóa chất cùng phiếu", "Tiền thuốc", "Trạng thái");
    private final DefaultTableModel exportModel = model(
            "Mã đơn", "Ngày kê", "Mã bệnh nhân", "Bệnh nhân", "Bác sĩ",
            "Số loại thuốc", "Trạng thái");
    private final DefaultTableModel historyModel = model(
            "Ngày giờ", "Loại", "Số phiếu", "Mã thuốc", "Tên thuốc",
            "Số lô", "Số lượng", "Đơn giá", "Nhân viên", "Trạng thái", "Ghi chú");
    private final JTable medicineTable = new JTable(medicineModel);
    private final JTable batchTable = new JTable(batchModel);
    private final JTable inventoryTable = new JTable(inventoryModel);
    private final JTable importTable = new JTable(importModel);
    private final JTable exportTable = new JTable(exportModel);
    private final JTable historyTable = new JTable(historyModel);

    private List<Medicine> medicines = List.of();
    private List<Batch> batches = List.of();
    private List<Receipt> importReceipts = List.of();
    private List<ExportCandidate> exportCandidates = List.of();
    private int medicinePage;
    private int batchPage;
    private int inventoryPage;
    private int importPage;
    private int exportPage;
    private int historyPage;
    private int medicineTotalPages = 1;
    private int batchTotalPages = 1;
    private int inventoryTotalPages = 1;
    private int importTotalPages = 1;
    private int exportTotalPages = 1;
    private int historyTotalPages = 1;
    private long loadVersion;
    private long statsLoadVersion;
    private boolean suppressFilterEvents;

    MedicineWarehousePanel(
            AuthenticatedSession session, WarehouseBUS warehouseBUS, String section) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException(
                    "Không thể mở kho thuốc khi chưa đăng nhập.");
        }
        this.session = session;
        this.warehouseBUS = warehouseBUS;
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(24, 28, 28, 28));
        add(createContent(), BorderLayout.NORTH);
        configureTables();
        configureFilters();
        configureActions();
        showSection(section);
    }

    void showSection(String section) {
        int selected = "Danh mục thuốc".equals(section) ? 0 : 1;
        boolean unchanged = tabs.getSelectedIndex() == selected;
        tabs.setSelectedIndex(selected);
        loadStats();
        if (unchanged) {
            loadActiveTab();
        }
    }

    void refreshData() {
        loadStats();
        loadActiveTab();
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(Box.createVerticalStrut(16));
        content.add(createStatistics());
        content.add(Box.createVerticalStrut(12));
        content.add(createStatusMessage());
        content.add(Box.createVerticalStrut(8));
        content.add(createTabs());
        return content;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý thuốc và kho");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel subtitle = new JLabel(
                "Theo dõi danh mục, lô thuốc, tồn kho, nhập xuất và lịch sử giao dịch.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(Box.createVerticalStrut(5));
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
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        cards.add(statCard("Danh mục thuốc", medicineStat, new Color(0xE8F7F5)));
        cards.add(statCard("Lô thuốc", batchStat, new Color(0xEAF2FF)));
        cards.add(statCard("Tổng tồn", stockStat, new Color(0xF2EEFF)));
        cards.add(statCard("Lô cảnh báo / hết hạn", warningStat, new Color(0xFFF5E5)));
        cards.setPreferredSize(new Dimension(0, 86));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        return cards;
    }

    private JPanel statCard(String title, JLabel value, Color background) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(9, 12, 9, 12)));
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(MainFrame.MUTED);
        value.setOpaque(true);
        value.setBackground(background);
        value.setForeground(MainFrame.TEXT);
        value.setFont(new Font("Segoe UI", Font.BOLD, 20));
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusMessage() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        wrapper.add(message, BorderLayout.CENTER);
        return wrapper;
    }

    private JTabbedPane createTabs() {
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.addTab("Danh mục thuốc", createMedicineTab());
        tabs.addTab("Lô thuốc", createBatchTab());
        tabs.addTab("Tồn kho", createInventoryTab());
        tabs.addTab("Nhập kho", createImportTab());
        tabs.addTab("Xuất theo đơn", createExportTab());
        tabs.addTab("Lịch sử", createHistoryTab());
        tabs.addChangeListener(event -> loadActiveTab());
        return tabs;
    }

    private JPanel createMedicineTab() {
        JPanel card = createCard();
        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        medicineSearch.setPreferredSize(new Dimension(300, 34));
        medicineSearch.setToolTipText("Tìm mã thuốc, tên thuốc, hoạt chất hoặc hàm lượng");
        filters.add(medicineSearch, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        medicineStatus.setPreferredSize(new Dimension(180, 34));
        actions.add(medicineStatus);
        actions.add(addMedicineButton);
        actions.add(editMedicineButton);
        filters.add(actions, BorderLayout.EAST);
        card.add(filters, BorderLayout.NORTH);
        card.add(tableScroll(medicineTable), BorderLayout.CENTER);
        card.add(paging(medicinePrevious, medicinePageLabel, medicineNext),
                BorderLayout.SOUTH);
        return card;
    }

    private JPanel createBatchTab() {
        JPanel card = createCard();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        batchSearch.setPreferredSize(new Dimension(280, 34));
        batchStatus.setPreferredSize(new Dimension(170, 34));
        JButton detail = new JButton("Chi tiết lô");
        detail.addActionListener(event -> showSelectedBatch());
        filters.add(batchSearch);
        filters.add(batchStatus);
        filters.add(detail);
        card.add(filters, BorderLayout.NORTH);
        card.add(tableScroll(batchTable), BorderLayout.CENTER);
        card.add(paging(batchPrevious, batchPageLabel, batchNext), BorderLayout.SOUTH);
        batchTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showSelectedBatch();
                }
            }
        });
        return card;
    }

    private JPanel createInventoryTab() {
        JPanel card = createCard();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        inventorySearch.setPreferredSize(new Dimension(300, 34));
        inventoryStatus.setPreferredSize(new Dimension(180, 34));
        filters.add(inventorySearch);
        filters.add(inventoryStatus);
        card.add(filters, BorderLayout.NORTH);
        card.add(tableScroll(inventoryTable), BorderLayout.CENTER);
        card.add(paging(inventoryPrevious, inventoryPageLabel, inventoryNext),
                BorderLayout.SOUTH);
        return card;
    }

    private JPanel createImportTab() {
        JPanel card = createCard();
        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        importSearch.setPreferredSize(new Dimension(300, 34));
        filters.add(importSearch, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        importStatus.setPreferredSize(new Dimension(170, 34));
        addImportButton.setForeground(Color.WHITE);
        addImportButton.setBackground(MainFrame.BLUE);
        addImportButton.setOpaque(true);
        addImportButton.setBorderPainted(false);
        actions.add(importStatus);
        actions.add(addImportButton);
        JButton detail = new JButton("Chi tiết phiếu");
        detail.addActionListener(event -> showSelectedImport());
        actions.add(detail);
        filters.add(actions, BorderLayout.EAST);
        card.add(filters, BorderLayout.NORTH);
        card.add(tableScroll(importTable), BorderLayout.CENTER);
        card.add(paging(importPrevious, importPageLabel, importNext), BorderLayout.SOUTH);
        importTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showSelectedImport();
                }
            }
        });
        return card;
    }

    private JPanel createExportTab() {
        JPanel card = createCard();
        JPanel filters = new JPanel(new BorderLayout(8, 0));
        filters.setOpaque(false);
        exportSearch.setPreferredSize(new Dimension(360, 34));
        filters.add(exportSearch, BorderLayout.CENTER);
        JButton preview = new JButton("Xem trước / xuất thuốc");
        preview.addActionListener(event -> showExportPreview());
        filters.add(preview, BorderLayout.EAST);
        card.add(filters, BorderLayout.NORTH);
        card.add(tableScroll(exportTable), BorderLayout.CENTER);
        card.add(paging(exportPrevious, exportPageLabel, exportNext), BorderLayout.SOUTH);
        exportTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    showExportPreview();
                }
            }
        });
        return card;
    }

    private JPanel createHistoryTab() {
        JPanel card = createCard();
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        historySearch.setPreferredSize(new Dimension(260, 34));
        historyType.setPreferredSize(new Dimension(140, 34));
        historyStatus.setPreferredSize(new Dimension(160, 34));
        filters.add(historySearch);
        filters.add(historyType);
        filters.add(historyStatus);
        card.add(filters, BorderLayout.NORTH);
        card.add(tableScroll(historyTable), BorderLayout.CENTER);
        card.add(paging(historyPrevious, historyPageLabel, historyNext),
                BorderLayout.SOUTH);
        return card;
    }

    private JPanel createCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(11, 12, 10, 12)));
        card.setPreferredSize(new Dimension(0, 530));
        return card;
    }

    private JScrollPane tableScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel paging(JButton previous, JLabel pageLabel, JButton next) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        footer.setOpaque(false);
        previous.addActionListener(event -> changePage(previous, pageLabel, false));
        next.addActionListener(event -> changePage(next, pageLabel, true));
        footer.add(previous);
        footer.add(pageLabel);
        footer.add(next);
        return footer;
    }

    private void configureTables() {
        configureTable(medicineTable, new int[]{95, 190, 160, 105, 130, 80, 140, 120});
        configureTable(batchTable,
                new int[]{70, 90, 180, 105, 105, 105, 85, 85, 85, 130, 105});
        configureTable(inventoryTable,
                new int[]{90, 190, 150, 100, 75, 120, 90, 75, 95, 80, 85});
        configureTable(importTable,
                new int[]{75, 190, 130, 170, 150, 100, 140, 120, 110});
        configureTable(exportTable, new int[]{80, 130, 100, 180, 170, 100, 120});
        configureTable(historyTable,
                new int[]{130, 80, 180, 95, 175, 105, 90, 100, 140, 105, 200});
        medicineTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                editMedicineButton.setEnabled(
                        session.hasPermission("KHO_NHAP")
                                && medicineTable.getSelectedRow() >= 0);
            }
        });
    }

    private void configureTable(JTable table, int[] widths) {
        table.setRowHeight(35);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 37));
        header.setForeground(MainFrame.MUTED);
        header.setBackground(new Color(0xF5F7FA));
        header.setReorderingAllowed(false);
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void configureFilters() {
        medicineStatus.addItem(new Choice("", "Tất cả thuốc"));
        medicineStatus.addItem(new Choice("DangSuDung", "Đang sử dụng"));
        medicineStatus.addItem(new Choice("NgungSuDung", "Ngừng sử dụng"));
        batchStatus.addItem(new Choice("", "Tất cả trạng thái lô"));
        batchStatus.addItem(new Choice("DangTon", "Đang tồn"));
        batchStatus.addItem(new Choice("SapHetHan", "Sắp hết hạn"));
        batchStatus.addItem(new Choice("HetHang", "Hết hàng"));
        batchStatus.addItem(new Choice("HetHan", "Hết hạn"));
        batchStatus.addItem(new Choice("BiKhoa", "Bị khóa"));
        inventoryStatus.addItem(new Choice("", "Tất cả tồn kho"));
        inventoryStatus.addItem(new Choice("ConHang", "Còn tồn"));
        inventoryStatus.addItem(new Choice("HetHang", "Hết hàng"));
        inventoryStatus.addItem(new Choice("SapHetHan", "Có lô sắp hết hạn"));
        inventoryStatus.addItem(new Choice("HetHan", "Có lô hết hạn"));
        addReceiptStatuses(importStatus);
        historyType.addItem(new Choice("", "Tất cả giao dịch"));
        historyType.addItem(new Choice("Nhap", "Nhập thuốc"));
        historyType.addItem(new Choice("Xuat", "Xuất thuốc"));
        historyStatus.addItem(new Choice("", "Tất cả trạng thái"));
        addReceiptStatuses(historyStatus);
        editMedicineButton.setEnabled(false);
        addMedicineButton.setEnabled(session.hasPermission("KHO_NHAP"));
        addImportButton.setEnabled(session.hasPermission("KHO_NHAP"));
        medicineSearch.addActionListener(event -> resetAndLoad(0));
        batchSearch.addActionListener(event -> resetAndLoad(1));
        inventorySearch.addActionListener(event -> resetAndLoad(2));
        importSearch.addActionListener(event -> resetAndLoad(3));
        exportSearch.addActionListener(event -> resetAndLoad(4));
        historySearch.addActionListener(event -> resetAndLoad(5));
        medicineStatus.addActionListener(event -> filterChanged(0));
        batchStatus.addActionListener(event -> filterChanged(1));
        inventoryStatus.addActionListener(event -> filterChanged(2));
        importStatus.addActionListener(event -> filterChanged(3));
        historyType.addActionListener(event -> filterChanged(5));
        historyStatus.addActionListener(event -> filterChanged(5));
        tabs.addChangeListener(event -> {
            if (!suppressFilterEvents) {
                updateMessage(" ");
            }
        });
    }

    private void configureActions() {
        addMedicineButton.addActionListener(event -> openMedicineDialog(null));
        editMedicineButton.addActionListener(event -> {
            Medicine selected = selectedMedicine();
            if (selected != null) {
                openMedicineDialog(selected);
            }
        });
        addImportButton.addActionListener(event -> openImportDialog());
        bindPaging(medicinePrevious, medicineNext, 0);
        bindPaging(batchPrevious, batchNext, 1);
        bindPaging(inventoryPrevious, inventoryNext, 2);
        bindPaging(importPrevious, importNext, 3);
        bindPaging(exportPrevious, exportNext, 4);
        bindPaging(historyPrevious, historyNext, 5);
    }

    private void addReceiptStatuses(JComboBox<Choice> combo) {
        combo.addItem(new Choice("", "Tất cả trạng thái"));
        combo.addItem(new Choice("Nhap", "Đang nhập"));
        combo.addItem(new Choice("DaXacNhan", "Đã xác nhận"));
        combo.addItem(new Choice("DaHuy", "Đã hủy"));
    }

    private void bindPaging(JButton previous, JButton next, int tab) {
        for (java.awt.event.ActionListener listener : previous.getActionListeners()) {
            previous.removeActionListener(listener);
        }
        for (java.awt.event.ActionListener listener : next.getActionListeners()) {
            next.removeActionListener(listener);
        }
        previous.addActionListener(event -> movePage(tab, -1));
        next.addActionListener(event -> movePage(tab, 1));
    }

    private void changePage(JButton button, JLabel label, boolean next) {
        int tab = tabs.getSelectedIndex();
        movePage(tab, next ? 1 : -1);
    }

    private void movePage(int tab, int step) {
        switch (tab) {
            case 0 -> medicinePage = boundedPage(medicinePage + step, medicineTotalPages);
            case 1 -> batchPage = boundedPage(batchPage + step, batchTotalPages);
            case 2 -> inventoryPage = boundedPage(inventoryPage + step, inventoryTotalPages);
            case 3 -> importPage = boundedPage(importPage + step, importTotalPages);
            case 4 -> exportPage = boundedPage(exportPage + step, exportTotalPages);
            case 5 -> historyPage = boundedPage(historyPage + step, historyTotalPages);
            default -> {
                return;
            }
        }
        loadActiveTab();
    }

    private int boundedPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    private void resetAndLoad(int tab) {
        setPage(tab, 0);
        loadActiveTab();
    }

    private void filterChanged(int tab) {
        if (!suppressFilterEvents) {
            resetAndLoad(tab);
        }
    }

    private void setPage(int tab, int page) {
        switch (tab) {
            case 0 -> medicinePage = page;
            case 1 -> batchPage = page;
            case 2 -> inventoryPage = page;
            case 3 -> importPage = page;
            case 4 -> exportPage = page;
            case 5 -> historyPage = page;
            default -> {
            }
        }
    }

    private void loadActiveTab() {
        if (tabs.getTabCount() == 0) {
            return;
        }
        switch (tabs.getSelectedIndex()) {
            case 0 -> loadMedicines();
            case 1 -> loadBatches();
            case 2 -> loadInventory();
            case 3 -> loadImports();
            case 4 -> loadExportCandidates();
            case 5 -> loadHistory();
            default -> {
            }
        }
    }

    private void loadStats() {
        long request = ++statsLoadVersion;
        new SwingWorker<Stats, Void>() {
            @Override
            protected Stats doInBackground() throws WarehouseException {
                return warehouseBUS.getStats(session);
            }

            @Override
            protected void done() {
                if (request != statsLoadVersion) {
                    return;
                }
                try {
                    Stats stats = get();
                    medicineStat.setText(Long.toString(stats.medicineCount()));
                    batchStat.setText(Long.toString(stats.batchCount()));
                    stockStat.setText(formatNumber(stats.totalStock()));
                    warningStat.setText(stats.expiringLots() + " sắp hết hạn · "
                            + stats.expiredLots() + " hết hạn");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Không thể tải thống kê kho thuốc.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải thống kê kho thuốc.", exception.getCause());
                    medicineStat.setText("!");
                    batchStat.setText("!");
                    stockStat.setText("!");
                    warningStat.setText("!");
                }
            }
        }.execute();
    }

    private void loadMedicines() {
        Choice status = selected(medicineStatus);
        load("Không thể tải danh mục thuốc.",
                () -> warehouseBUS.searchMedicines(
                        session, medicineSearch.getText(), status.code(),
                        medicinePage, PAGE_SIZE),
                page -> {
                    medicines = page.rows();
                    medicineTotalPages = page.totalPages();
                    medicineModel.setRowCount(0);
                    for (Medicine item : medicines) {
                        medicineModel.addRow(new Object[]{
                                item.id(), item.name(), safe(item.activeIngredient()),
                                safe(item.strength()), safe(item.dosageForm()),
                                safe(item.unit()), safe(item.defaultRoute()),
                                medicineStatusLabel(item.status())});
                    }
                    updatePaging(medicinePage, medicineTotalPages,
                            page.totalCount(), medicinePageLabel,
                            medicinePrevious, medicineNext);
                    editMedicineButton.setEnabled(session.hasPermission("KHO_NHAP")
                            && !medicines.isEmpty());
                    updateMessage(page.totalCount() == 0
                            ? "Chưa có thuốc phù hợp với điều kiện lọc." : " ");
                });
    }

    private void loadBatches() {
        Choice status = selected(batchStatus);
        BatchFilter filter = new BatchFilter(
                batchSearch.getText(), "", status.code(), null, null,
                batchPage, PAGE_SIZE);
        load("Không thể tải danh sách lô thuốc.",
                () -> warehouseBUS.searchBatches(session, filter),
                page -> {
                    batches = page.rows();
                    batchTotalPages = page.totalPages();
                    batchModel.setRowCount(0);
                    for (Batch item : batches) {
                        batchModel.addRow(new Object[]{
                                item.id(), item.medicineId(), item.medicineName(),
                                item.batchNumber(), date(item.expiryDate()),
                                formatNumber(item.unitCost()), formatNumber(item.stock()),
                                formatNumber(item.confirmedIn()), formatNumber(item.confirmedOut()),
                                safe(item.location()), batchStatusLabel(item.status())});
                    }
                    updatePaging(batchPage, batchTotalPages, page.totalCount(),
                            batchPageLabel, batchPrevious, batchNext);
                    updateMessage(page.totalCount() == 0
                            ? "Không có lô thuốc phù hợp với bộ lọc." : " ");
                });
    }

    private void loadInventory() {
        Choice status = selected(inventoryStatus);
        InventoryFilter filter = new InventoryFilter(
                inventorySearch.getText(), status.code(), inventoryPage, PAGE_SIZE);
        load("Không thể tải bảng tồn kho.",
                () -> warehouseBUS.searchInventory(session, filter),
                page -> {
                    inventoryTotalPages = page.totalPages();
                    inventoryModel.setRowCount(0);
                    for (InventoryItem item : page.rows()) {
                        inventoryModel.addRow(new Object[]{
                                item.medicineId(), item.medicineName(),
                                safe(item.activeIngredient()), safe(item.strength()),
                                safe(item.unit()), medicineStatusLabel(item.medicineStatus()),
                                formatNumber(item.totalStock()), item.batchCount(),
                                item.expiringBatchCount(), item.expiredBatchCount(),
                                item.outOfStockBatchCount()});
                    }
                    updatePaging(inventoryPage, inventoryTotalPages, page.totalCount(),
                            inventoryPageLabel, inventoryPrevious, inventoryNext);
                    updateMessage(page.totalCount() == 0
                            ? "Không có dữ liệu tồn kho phù hợp." : " ");
                });
    }

    private void loadImports() {
        Choice status = selected(importStatus);
        ReceiptFilter filter = new ReceiptFilter(
                importSearch.getText(), status.code(), null, null, importPage, PAGE_SIZE);
        load("Không thể tải danh sách phiếu nhập.",
                () -> warehouseBUS.searchReceipts(session, true, filter),
                page -> {
                    importReceipts = page.rows();
                    importTotalPages = page.totalPages();
                    importModel.setRowCount(0);
                    for (Receipt item : importReceipts) {
                        importModel.addRow(new Object[]{
                                item.id(), item.number(), dateTime(item.date()),
                                safe(item.counterparty()), safe(item.employeeName()),
                                item.medicineLineCount(), item.chemicalLineCount(),
                                formatNumber(item.total()), receiptStatusLabel(item.status())});
                    }
                    updatePaging(importPage, importTotalPages, page.totalCount(),
                            importPageLabel, importPrevious, importNext);
                    updateMessage(page.totalCount() == 0
                            ? "Chưa có phiếu nhập thuốc phù hợp." : " ");
                });
    }

    private void loadExportCandidates() {
        load("Không thể tải đơn thuốc chờ cấp phát.",
                () -> warehouseBUS.searchExportCandidates(
                        session, new ExportFilter(exportSearch.getText(), exportPage, PAGE_SIZE)),
                page -> {
                    exportCandidates = page.rows();
                    exportTotalPages = page.totalPages();
                    exportModel.setRowCount(0);
                    for (ExportCandidate item : exportCandidates) {
                        exportModel.addRow(new Object[]{
                                item.prescriptionId(), dateTime(item.prescribedAt()),
                                item.patientId(), item.patientName(), item.doctorName(),
                                item.medicationCount(), prescriptionStatusLabel(item.status())});
                    }
                    updatePaging(exportPage, exportTotalPages, page.totalCount(),
                            exportPageLabel, exportPrevious, exportNext);
                    updateMessage(page.totalCount() == 0
                            ? "Không có đơn thuốc đang chờ xuất." : " ");
                });
    }

    private void loadHistory() {
        Choice type = selected(historyType);
        Choice status = selected(historyStatus);
        MovementFilter filter = new MovementFilter(
                historySearch.getText(), type.code(), status.code(),
                null, null, historyPage, PAGE_SIZE);
        load("Không thể tải lịch sử nhập xuất.",
                () -> warehouseBUS.searchMovements(session, filter),
                page -> {
                    historyTotalPages = page.totalPages();
                    historyModel.setRowCount(0);
                    for (StockMovement item : page.rows()) {
                        historyModel.addRow(new Object[]{
                                dateTime(item.date()), item.movementType().equals("Nhap")
                                        ? "Nhập" : "Xuất",
                                item.receiptNumber(), item.medicineId(), item.medicineName(),
                                item.batchNumber(), formatNumber(item.quantity()),
                                formatNumber(item.unitCost()), item.employeeName(),
                                receiptStatusLabel(item.status()), safe(item.note())});
                    }
                    updatePaging(historyPage, historyTotalPages, page.totalCount(),
                            historyPageLabel, historyPrevious, historyNext);
                    updateMessage(page.totalCount() == 0
                            ? "Chưa có giao dịch thuốc phù hợp." : " ");
                });
    }

    private <T> void load(
            String failureMessage, Callable<T> operation, Consumer<T> onSuccess) {
        long request = ++loadVersion;
        updateMessage("Đang tải dữ liệu...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operation.call();
            }

            @Override
            protected void done() {
                if (request != loadVersion) {
                    return;
                }
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError(failureMessage, exception);
                } catch (ExecutionException exception) {
                    showError(failureMessage, exception.getCause());
                } finally {
                    if (request == loadVersion) {
                        updatePagingEnabled();
                    }
                }
            }
        }.execute();
    }

    private void updatePaging(
            int current, int total, long count,
            JLabel label, JButton previous, JButton next) {
        label.setText("Trang " + (current + 1) + " / " + total + " · " + count + " dòng");
        previous.setEnabled(current > 0);
        next.setEnabled(current + 1 < total);
    }

    private void updatePagingEnabled() {
        boolean canNavigate = session.hasPermission("KHO_XEM");
        medicinePrevious.setEnabled(canNavigate && medicinePage > 0);
        medicineNext.setEnabled(canNavigate && medicinePage + 1 < medicineTotalPages);
        batchPrevious.setEnabled(canNavigate && batchPage > 0);
        batchNext.setEnabled(canNavigate && batchPage + 1 < batchTotalPages);
        inventoryPrevious.setEnabled(canNavigate && inventoryPage > 0);
        inventoryNext.setEnabled(canNavigate && inventoryPage + 1 < inventoryTotalPages);
        importPrevious.setEnabled(canNavigate && importPage > 0);
        importNext.setEnabled(canNavigate && importPage + 1 < importTotalPages);
        exportPrevious.setEnabled(session.hasPermission("KHO_XUAT") && exportPage > 0);
        exportNext.setEnabled(session.hasPermission("KHO_XUAT")
                && exportPage + 1 < exportTotalPages);
        historyPrevious.setEnabled(canNavigate && historyPage > 0);
        historyNext.setEnabled(canNavigate && historyPage + 1 < historyTotalPages);
    }

    private void updateMessage(String text) {
        message.setText(text == null || text.isBlank() ? " " : text);
        message.setForeground(MainFrame.MUTED);
    }

    private void showError(String title, Throwable error) {
        LOGGER.log(Level.SEVERE, title, error);
        String detail = error == null || error.getMessage() == null
                ? "Lỗi không xác định." : error.getMessage();
        message.setText(title + " " + detail);
        message.setForeground(new Color(0xB42318));
        JOptionPane.showMessageDialog(this, detail, title, JOptionPane.ERROR_MESSAGE);
    }

    private Medicine selectedMedicine() {
        int row = medicineTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = medicineTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < medicines.size() ? medicines.get(modelRow) : null;
    }

    private Batch selectedBatch() {
        int row = batchTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = batchTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < batches.size() ? batches.get(modelRow) : null;
    }

    private Receipt selectedImport() {
        int row = importTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = importTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < importReceipts.size()
                ? importReceipts.get(modelRow) : null;
    }

    private ExportCandidate selectedExportCandidate() {
        int row = exportTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = exportTable.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < exportCandidates.size()
                ? exportCandidates.get(modelRow) : null;
    }

    private void showSelectedBatch() {
        Batch selected = selectedBatch();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một lô thuốc.",
                    "Chi tiết lô", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        load("Không thể tải chi tiết lô.",
                () -> warehouseBUS.getBatchDetail(session, selected.id()),
                detail -> new BatchDetailDialog(
                        SwingUtilities.getWindowAncestor(this), detail).setVisible(true));
    }

    private void showSelectedImport() {
        Receipt selected = selectedImport();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một phiếu nhập.",
                    "Chi tiết phiếu", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        load("Không thể tải chi tiết phiếu nhập.",
                () -> warehouseBUS.getReceiptDetail(session, selected.id(), true),
                detail -> new WarehouseReceiptDialog(
                        SwingUtilities.getWindowAncestor(this), detail).setVisible(true));
    }

    private void showExportPreview() {
        ExportCandidate selected = selectedExportCandidate();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một đơn thuốc.",
                    "Xuất thuốc", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        load("Không thể xem trước cấp phát thuốc.",
                () -> warehouseBUS.getExportPreview(session, selected.prescriptionId()),
                preview -> new ExportPreviewDialog(
                        SwingUtilities.getWindowAncestor(this), session, warehouseBUS,
                        preview, this::onExported).setVisible(true));
    }

    private void onExported(CreatedReceipt receipt) {
        updateMessage("Đã tạo phiếu xuất " + receipt.number() + ".");
        loadStats();
        loadExportCandidates();
    }

    private void openMedicineDialog(Medicine existing) {
        MedicineDialog dialog = new MedicineDialog(
                SwingUtilities.getWindowAncestor(this), session, warehouseBUS, existing,
                () -> {
                    medicinePage = 0;
                    loadStats();
                    loadMedicines();
                });
        dialog.setVisible(true);
    }

    private void openImportDialog() {
        ImportDialog dialog = new ImportDialog(
                SwingUtilities.getWindowAncestor(this), session, warehouseBUS,
                () -> {
                    importPage = 0;
                    loadStats();
                    loadImports();
                    loadBatches();
                    loadInventory();
                });
        dialog.setVisible(true);
    }

    private Choice selected(JComboBox<Choice> combo) {
        Choice selected = (Choice) combo.getSelectedItem();
        return selected == null ? new Choice("", "") : selected;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String formatNumber(java.math.BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String medicineStatusLabel(String status) {
        return "DangSuDung".equals(status) ? "Đang sử dụng" : "Ngừng sử dụng";
    }

    private String batchStatusLabel(String status) {
        return switch (status) {
            case "HetHang" -> "Hết hàng";
            case "HetHan" -> "Hết hạn";
            case "BiKhoa" -> "Bị khóa";
            case "SapHetHan" -> "Sắp hết hạn";
            default -> "Đang tồn";
        };
    }

    private String receiptStatusLabel(String status) {
        return switch (status) {
            case "Nhap" -> "Đang nhập";
            case "DaXacNhan" -> "Đã xác nhận";
            case "DaHuy" -> "Đã hủy";
            default -> safe(status);
        };
    }

    private String prescriptionStatusLabel(String status) {
        return switch (status) {
            case "DaKy" -> "Đã ký";
            case "DaGuiKho" -> "Đã gửi kho";
            default -> safe(status);
        };
    }

    private record Choice(String code, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
