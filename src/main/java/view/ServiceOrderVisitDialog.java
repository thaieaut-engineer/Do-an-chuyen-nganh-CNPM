package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.LongConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.border.EmptyBorder;
import model.serviceorder.ServiceOrderListItem;

final class ServiceOrderVisitDialog extends JDialog {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JTable table;
    private final List<ServiceOrderListItem> orders;
    private final LongConsumer onOpenDetail;
    private final JButton detailsButton = new JButton("Xem chi tiết");

    ServiceOrderVisitDialog(
            java.awt.Window owner,
            long visitId,
            List<ServiceOrderListItem> orders,
            LongConsumer onOpenDetail) {
        super(owner, "Chỉ định dịch vụ · Lượt khám " + visitId,
                ModalityType.APPLICATION_MODAL);
        this.orders = List.copyOf(orders);
        this.onOpenDetail = onOpenDetail;
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Mã chỉ định", "Dịch vụ", "Loại dịch vụ",
                    "Bác sĩ phụ trách", "Thời gian", "Trạng thái", "Tính phí"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (ServiceOrderListItem order : this.orders) {
            model.addRow(new Object[]{
                    order.maChiDinh(),
                    order.tenDichVu(),
                    safe(order.loaiDichVu()),
                    order.tenBacSi(),
                    order.thoiGianChiDinh() == null
                            ? "—" : DATE_TIME.format(order.thoiGianChiDinh()),
                    statusLabel(order.trangThai()),
                    order.daTinhPhi() ? "Đã tính" : "Chưa tính"
            });
        }
        table = new JTable(model);
        configureTable();
        setContentPane(createContent(visitId));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 420));
        setSize(900, 520);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(long visitId) {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(16, 18, 14, 18));
        JLabel heading = new JLabel("Toàn bộ chỉ định của lượt khám " + visitId);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(MainFrame.TEXT);
        root.add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel info = new JLabel(orders.isEmpty()
                ? "Lượt khám này chưa có chỉ định dịch vụ."
                : orders.size() + " chỉ định");
        info.setForeground(MainFrame.MUTED);
        footer.add(info, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        detailsButton.addActionListener(event -> openSelected());
        detailsButton.setEnabled(false);
        JButton close = new JButton("Đóng");
        close.addActionListener(event -> dispose());
        actions.add(detailsButton);
        actions.add(close);
        footer.add(actions, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private void configureTable() {
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(0, 36));
        header.setForeground(MainFrame.MUTED);
        header.setBackground(new Color(0xF5F7FA));
        header.setReorderingAllowed(false);
        int[] widths = {90, 200, 150, 150, 140, 120, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                detailsButton.setEnabled(table.getSelectedRow() >= 0);
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    openSelected();
                }
            }
        });
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow >= 0 && modelRow < orders.size()) {
            onOpenDetail.accept(orders.get(modelRow).maChiDinh());
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "ChoThucHien" -> "Chờ thực hiện";
            case "DangThucHien" -> "Đang thực hiện";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            default -> safe(status);
        };
    }
}
