package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import view.component.AppIcon;

final class ModulePanel extends JPanel {
    private final ModuleDefinition definition;

    ModulePanel(String module) {
        definition = ModuleDefinition.forModule(module);
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(26, 30, 30, 30));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(gap(22));
        content.add(createSummaryCards());
        content.add(gap(18));
        content.add(createListSection());
        add(content, BorderLayout.NORTH);
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new javax.swing.BoxLayout(labels, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel(definition.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel description = new JLabel(definition.description);
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        description.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(gap(5));
        labels.add(description);

        JButton primaryAction = new JButton(
                definition.primaryAction, new AppIcon("add", Color.WHITE, 16));
        primaryAction.setIconTextGap(8);
        primaryAction.setFont(new Font("Segoe UI", Font.BOLD, 13));
        primaryAction.setForeground(Color.WHITE);
        primaryAction.setBackground(MainFrame.TEAL);
        primaryAction.setOpaque(true);
        primaryAction.setBorderPainted(false);
        primaryAction.setFocusPainted(false);
        primaryAction.setMargin(new Insets(11, 16, 11, 16));
        primaryAction.addActionListener(event -> showPreviewMessage());

        heading.add(labels, BorderLayout.WEST);
        heading.add(primaryAction, BorderLayout.EAST);
        return heading;
    }

    private JPanel createSummaryCards() {
        JPanel cards = new JPanel(new GridLayout(1, definition.summaryLabels.length, 14, 0));
        cards.setOpaque(false);
        for (String label : definition.summaryLabels) {
            JPanel card = new JPanel(new BorderLayout(0, 7));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MainFrame.BORDER),
                    new EmptyBorder(14, 16, 14, 16)));
            JLabel title = new JLabel(label.toUpperCase());
            title.setFont(new Font("Segoe UI", Font.BOLD, 10));
            title.setForeground(MainFrame.MUTED);
            JLabel value = new JLabel("—");
            value.setFont(new Font("Segoe UI", Font.BOLD, 23));
            value.setForeground(MainFrame.TEXT);
            JLabel hint = new JLabel("Chưa có dữ liệu");
            hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            hint.setForeground(MainFrame.MUTED);
            card.add(title, BorderLayout.NORTH);
            card.add(value, BorderLayout.CENTER);
            card.add(hint, BorderLayout.SOUTH);
            cards.add(card);
        }
        return cards;
    }

    private JPanel createListSection() {
        JPanel section = new JPanel(new BorderLayout(0, 16));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(18, 18, 18, 18)));

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JPanel filters = new JPanel(new BorderLayout(10, 0));
        filters.setOpaque(false);
        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", definition.searchHint);
        search.setPreferredSize(new Dimension(320, 38));
        JComboBox<String> statusFilter = new JComboBox<>(definition.statusFilters);
        statusFilter.setPreferredSize(new Dimension(180, 38));
        filters.add(search, BorderLayout.CENTER);
        filters.add(statusFilter, BorderLayout.EAST);

        JLabel recordCount = new JLabel("0 bản ghi");
        recordCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recordCount.setForeground(MainFrame.MUTED);
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(recordCount, BorderLayout.EAST);

        JTable table = new JTable(new DefaultTableModel(new Object[0][0], definition.columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        table.setRowHeight(42);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(MainFrame.TEXT);
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setPreferredSize(new Dimension(0, 43));
        tableHeader.setBackground(new Color(248, 250, 252));
        tableHeader.setForeground(MainFrame.MUTED);
        tableHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tableHeader.setReorderingAllowed(false);

        JPanel emptyState = new JPanel(new BorderLayout(0, 8));
        emptyState.setOpaque(false);
        emptyState.setBorder(new EmptyBorder(38, 12, 42, 12));
        JLabel icon = new JLabel(new AppIcon("clipboard", new Color(176, 190, 204), 30),
                SwingConstants.CENTER);
        JLabel emptyTitle = new JLabel("Chưa có dữ liệu", SwingConstants.CENTER);
        emptyTitle.setForeground(MainFrame.TEXT);
        emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel emptyDescription = new JLabel(
                "Phân hệ đang ở chế độ giao diện demo; chức năng chi tiết sẽ được triển khai theo giai đoạn.",
                SwingConstants.CENTER);
        emptyDescription.setForeground(MainFrame.MUTED);
        emptyDescription.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel emptyText = new JPanel(new BorderLayout(0, 5));
        emptyText.setOpaque(false);
        emptyText.add(emptyTitle, BorderLayout.NORTH);
        emptyText.add(emptyDescription, BorderLayout.CENTER);
        emptyState.add(icon, BorderLayout.NORTH);
        emptyState.add(emptyText, BorderLayout.CENTER);

        JPanel tableBody = new JPanel(new BorderLayout());
        tableBody.setBackground(Color.WHITE);
        tableBody.add(emptyState, BorderLayout.CENTER);
        JPanel tableFrame = new JPanel(new BorderLayout());
        tableFrame.setBackground(Color.WHITE);
        tableFrame.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableFrame.add(tableHeader, BorderLayout.NORTH);
        tableFrame.add(tableBody, BorderLayout.CENTER);

        section.add(toolbar, BorderLayout.NORTH);
        section.add(tableFrame, BorderLayout.CENTER);
        return section;
    }

    private void showPreviewMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Đây là giao diện demo. Chức năng nghiệp vụ sẽ được tích hợp ở giai đoạn tiếp theo.",
                "Chế độ xem trước",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel gap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(1, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return gap;
    }

    private static final class ModuleDefinition {
        private final String title;
        private final String description;
        private final String primaryAction;
        private final String searchHint;
        private final String[] summaryLabels;
        private final String[] columns;
        private final String[] statusFilters;

        private ModuleDefinition(
                String title,
                String description,
                String primaryAction,
                String searchHint,
                String[] summaryLabels,
                String[] columns,
                String... statusFilters) {
            this.title = title;
            this.description = description;
            this.primaryAction = primaryAction;
            this.searchHint = searchHint;
            this.summaryLabels = summaryLabels;
            this.columns = columns;
            this.statusFilters = statusFilters;
        }

        private static ModuleDefinition forModule(String module) {
            switch (module) {
                case "Lịch hẹn":
                    return new ModuleDefinition(
                            "Lịch hẹn khám",
                            "Quản lý lịch đăng ký khám, khung giờ và trạng thái xác nhận.",
                            "Tạo lịch hẹn",
                            "Tìm theo bệnh nhân, số điện thoại...",
                            new String[]{"Chờ xác nhận", "Đã xác nhận", "Lịch hẹn hôm nay"},
                            new String[]{"MÃ LỊCH", "THỜI GIAN", "BỆNH NHÂN", "CHUYÊN KHOA",
                                "BÁC SĨ", "LÝ DO KHÁM", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Chờ xác nhận", "Đã xác nhận", "Đã đến",
                            "Đang khám", "Hoàn tất", "Đã hủy", "Vắng mặt");
                case "Bệnh nhân":
                    return new ModuleDefinition(
                            "Hồ sơ bệnh nhân",
                            "Tiếp nhận và tra cứu hồ sơ, thông tin liên hệ và bảo hiểm y tế.",
                            "Thêm bệnh nhân",
                            "Tìm theo mã, họ tên, số điện thoại...",
                            new String[]{"Tổng hồ sơ", "Hồ sơ hoạt động", "Có thẻ BHYT"},
                            new String[]{"MÃ BỆNH NHÂN", "HỌ VÀ TÊN", "NGÀY SINH", "GIỚI TÍNH",
                                "SỐ ĐIỆN THOẠI", "SỐ THẺ BHYT", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Hoạt động", "Tạm khóa");
                case "Lượt khám":
                    return new ModuleDefinition(
                            "Quản lý lượt khám",
                            "Theo dõi tiếp nhận, khám bệnh và tiến độ xử lý từng lượt.",
                            "Tiếp nhận lượt khám",
                            "Tìm theo mã lượt khám, bệnh nhân...",
                            new String[]{"Chờ khám", "Đang khám", "Chờ kết quả"},
                            new String[]{"MÃ LƯỢT KHÁM", "THỜI GIAN TIẾP NHẬN", "BỆNH NHÂN",
                                "BÁC SĨ", "MÃ LỊCH HẸN", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Chờ khám", "Đang khám", "Chờ kết quả",
                            "Hoàn tất", "Đã hủy");
                case "Nhân sự":
                    return new ModuleDefinition(
                            "Quản lý nhân sự",
                            "Quản lý nhân viên, bác sĩ, bộ phận, chức danh và chuyên khoa.",
                            "Thêm nhân viên",
                            "Tìm theo mã, họ tên, số điện thoại...",
                            new String[]{"Tổng nhân viên", "Bác sĩ", "Đang làm việc"},
                            new String[]{"MÃ NHÂN VIÊN", "HỌ VÀ TÊN", "BỘ PHẬN", "CHỨC DANH",
                                "CHUYÊN KHOA", "SỐ ĐIỆN THOẠI", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Thử việc", "Đang làm", "Tạm nghỉ", "Đã nghỉ");
                case "Thuốc & kho":
                    return new ModuleDefinition(
                            "Thuốc & kho",
                            "Tra cứu thuốc, theo dõi tồn kho theo lô và hạn sử dụng.",
                            "Tạo phiếu nhập",
                            "Tìm theo mã thuốc, tên thuốc, số lô...",
                            new String[]{"Danh mục thuốc", "Lô sắp hết hạn", "Phiếu chờ xác nhận"},
                            new String[]{"MÃ THUỐC", "TÊN THUỐC", "SỐ LÔ", "HẠN SỬ DỤNG",
                                "TỒN KHO", "ĐƠN VỊ", "VỊ TRÍ", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Đang tồn", "Hết hàng", "Hết hạn", "Bị khóa");
                case "Viện phí":
                    return new ModuleDefinition(
                            "Viện phí & thanh toán",
                            "Tra cứu phiếu tính phí, hóa đơn và giao dịch thanh toán.",
                            "Lập hóa đơn",
                            "Tìm theo số hóa đơn, bệnh nhân...",
                            new String[]{"Chưa thanh toán", "Thanh toán một phần", "Đã thanh toán"},
                            new String[]{"SỐ HÓA ĐƠN", "BỆNH NHÂN", "NGÀY LẬP", "TỔNG TIỀN",
                                "BHYT CHI TRẢ", "PHẢI THANH TOÁN", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Chưa thanh toán", "Thanh toán một phần",
                            "Đã thanh toán", "Đã hủy", "Đã hoàn tiền");
                case "Bảo hiểm y tế":
                    return new ModuleDefinition(
                            "Bảo hiểm y tế",
                            "Quản lý thông tin thẻ, thời hạn và lịch sử xác thực BHYT.",
                            "Thêm thẻ BHYT",
                            "Tìm theo số thẻ, bệnh nhân...",
                            new String[]{"Thẻ hợp lệ", "Chờ xác minh", "Thẻ sắp hết hạn"},
                            new String[]{"SỐ THẺ BHYT", "BỆNH NHÂN", "NƠI ĐĂNG KÝ KCB",
                                "MỨC HƯỞNG", "TỪ NGÀY", "ĐẾN NGÀY", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Chưa xác thực", "Hợp lệ", "Hết hạn",
                            "Không hợp lệ", "Chờ xác minh");
                case "Thiết bị":
                    return new ModuleDefinition(
                            "Thiết bị y tế",
                            "Theo dõi danh mục thiết bị, tình trạng, bảo dưỡng và sửa chữa.",
                            "Thêm thiết bị",
                            "Tìm theo mã, tên, số serial...",
                            new String[]{"Tổng thiết bị", "Cần bảo trì", "Đang sửa chữa"},
                            new String[]{"MÃ THIẾT BỊ", "TÊN THIẾT BỊ", "LOẠI", "MODEL",
                                "SỐ SERIAL", "VỊ TRÍ", "TÌNH TRẠNG", "BẢO DƯỠNG TIẾP"},
                            "Tất cả tình trạng", "Tốt", "Đang sử dụng", "Cần bảo trì",
                            "Đang sửa chữa", "Ngưng sử dụng", "Thanh lý");
                case "Đánh giá nhân viên":
                    return new ModuleDefinition(
                            "Đánh giá nhân viên",
                            "Theo dõi kỳ đánh giá, điểm số và nhận xét nhân viên.",
                            "Tạo đánh giá",
                            "Tìm theo nhân viên, kỳ đánh giá...",
                            new String[]{"Kỳ đánh giá hiện tại", "Chờ đánh giá", "Đã hoàn thành"},
                            new String[]{"MÃ ĐÁNH GIÁ", "NHÂN VIÊN", "KỲ ĐÁNH GIÁ",
                                "NGÀY ĐÁNH GIÁ", "ĐIỂM", "NGƯỜI ĐÁNH GIÁ"},
                            "Tất cả kỳ đánh giá", "Kỳ hiện tại", "Kỳ trước");
                case "Quản trị hệ thống":
                    return new ModuleDefinition(
                            "Quản trị hệ thống",
                            "Quản lý tài khoản, vai trò, quyền truy cập và nhật ký hệ thống.",
                            "Tạo tài khoản",
                            "Tìm theo tên đăng nhập, nhân viên...",
                            new String[]{"Tài khoản hoạt động", "Chờ kích hoạt", "Tài khoản bị khóa"},
                            new String[]{"TÊN ĐĂNG NHẬP", "NHÂN VIÊN", "VAI TRÒ",
                                "NGÀY TẠO", "ĐĂNG NHẬP GẦN NHẤT", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Chờ kích hoạt", "Hoạt động", "Khóa", "Thu hồi");
                default:
                    return new ModuleDefinition(
                            module,
                            "Tra cứu và quản lý thông tin thuộc phân hệ " + module.toLowerCase() + ".",
                            "Thêm mới",
                            "Tìm kiếm " + module.toLowerCase() + "...",
                            new String[]{"Tổng số", "Đang xử lý", "Hoàn tất"},
                            new String[]{"MÃ", "TÊN / TIÊU ĐỀ", "THÔNG TIN", "NGÀY CẬP NHẬT", "TRẠNG THÁI"},
                            "Tất cả trạng thái", "Đang xử lý", "Hoàn tất", "Đã hủy");
            }
        }
    }
}
