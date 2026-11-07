package view;

import bus.dashboard.DashboardBUS;
import bus.dashboard.DashboardException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.auth.AuthenticatedSession;
import model.dashboard.DashboardData;
import model.dashboard.DashboardStatistics;
import model.dashboard.UpcomingAppointment;
import view.component.AppIcon;

final class DashboardPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(DashboardPanel.class.getName());
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM, yyyy", VIETNAMESE);
    private static final DateTimeFormatter APPOINTMENT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter CHART_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM");

    private final AuthenticatedSession session;
    private final DashboardBUS dashboardBUS;
    private final List<StatCard> statisticCards = new ArrayList<>();
    private final ChartPlot visitsChart = new ChartPlot(MainFrame.BLUE, true, false);
    private final ChartPlot revenueChart = new ChartPlot(new Color(74, 128, 197), false, true);
    private final JPanel appointmentContent = new JPanel(new CardLayout());
    private final JLabel appointmentEmptyMessage = new JLabel(
            "Đang tải lịch hẹn...", SwingConstants.CENTER);
    private final DefaultTableModel appointmentTableModel = new DefaultTableModel(
            new Object[]{"MÃ LỊCH", "THỜI GIAN", "BỆNH NHÂN", "BÁC SĨ", "CHUYÊN KHOA", "TRẠNG THÁI"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel loadMessage = new JLabel(" ");
    private final JLabel dateLabel = new JLabel();
    private final JButton refreshButton = new JButton("Làm mới");
    private boolean loading;

    DashboardPanel(
            AuthenticatedSession session,
            DashboardBUS dashboardBUS,
            Consumer<String> openPage,
            Predicate<String> canNavigate) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Không thể mở tổng quan khi phiên đăng nhập không hợp lệ.");
        }
        this.session = session;
        this.dashboardBUS = dashboardBUS;

        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(25, 28, 30, 28));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(createHeading(openPage, canNavigate));
        content.add(gap(18));
        content.add(createStatistics());
        content.add(gap(16));
        content.add(createCharts());
        content.add(gap(16));
        content.add(createAppointments(openPage, canNavigate));
        add(content, BorderLayout.NORTH);

        visitsChart.setMessage("Đang tải dữ liệu...");
        revenueChart.setMessage("Đang tải dữ liệu...");
        appointmentContent.setOpaque(false);
        loadDashboardData();
    }

    private JPanel createHeading(Consumer<String> openPage, Predicate<String> canNavigate) {
        JPanel heading = new JPanel(new BorderLayout(15, 0));
        heading.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tổng quan phòng khám");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel greeting = new JLabel("Xin chào, " + session.getEmployeeName());
        greeting.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        greeting.setForeground(MainFrame.MUTED);
        dateLabel.setText(LocalDate.now().format(DATE_FORMAT));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateLabel.setForeground(MainFrame.MUTED);
        loadMessage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        loadMessage.setForeground(MainFrame.MUTED);
        titles.add(title);
        titles.add(gap(4));
        titles.add(greeting);
        titles.add(gap(3));
        titles.add(dateLabel);
        titles.add(loadMessage);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setForeground(MainFrame.BLUE);
        refreshButton.setBackground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setToolTipText("Tải lại số liệu từ cơ sở dữ liệu");
        refreshButton.addActionListener(event -> loadDashboardData());
        actions.add(refreshButton);
        if (canNavigate.test("Lịch hẹn")) {
            JButton booking = primaryButton("Đặt lịch hẹn");
            booking.setIcon(new AppIcon("add", Color.WHITE, 16));
            booking.setIconTextGap(8);
            booking.addActionListener(event -> openPage.accept("Lịch hẹn"));
            actions.add(booking);
        }
        heading.add(titles, BorderLayout.CENTER);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    private JPanel createStatistics() {
        JPanel statistics = new JPanel(new GridLayout(1, 4, 14, 0));
        statistics.setOpaque(false);
        statisticCards.add(new StatCard("LỊCH HẸN HÔM NAY", "calendar"));
        statisticCards.add(new StatCard("LƯỢT KHÁM HÔM NAY", "medical"));
        statisticCards.add(new StatCard("BỆNH NHÂN", "patients"));
        statisticCards.add(new StatCard("DOANH THU HÔM NAY", "wallet"));
        statisticCards.forEach(statistics::add);
        return statistics;
    }

    private JPanel createCharts() {
        JPanel charts = new JPanel(new GridLayout(1, 2, 14, 0));
        charts.setOpaque(false);
        charts.add(new ChartCard(
                "Lượt khám 7 ngày gần nhất", "Số lượt khám", visitsChart));
        charts.add(new ChartCard(
                "Doanh thu thực thu 7 ngày gần nhất", "Đơn vị: VNĐ", revenueChart));
        return charts;
    }

    private JPanel createAppointments(
            Consumer<String> openPage, Predicate<String> canNavigate) {
        JPanel panel = cardPanel();
        boolean canOpenAppointments = canNavigate.test("Lịch hẹn");
        JPanel heading = sectionHeading(
                "Lịch hẹn sắp diễn ra", canOpenAppointments ? "Xem tất cả" : null);
        if (canOpenAppointments) {
            JButton viewAll = (JButton) ((BorderLayout) heading.getLayout())
                    .getLayoutComponent(BorderLayout.EAST);
            viewAll
                    .addActionListener(event -> openPage.accept("Lịch hẹn"));
        }

        JTable table = new JTable(appointmentTableModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(135);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 235));
        appointmentEmptyMessage.setForeground(MainFrame.MUTED);
        appointmentEmptyMessage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        appointmentContent.add(scroll, "table");
        appointmentContent.add(appointmentEmptyMessage, "empty");
        ((CardLayout) appointmentContent.getLayout()).show(appointmentContent, "empty");
        panel.add(heading, BorderLayout.NORTH);
        panel.add(appointmentContent, BorderLayout.CENTER);
        return panel;
    }

    private void loadDashboardData() {
        if (loading) {
            return;
        }
        loading = true;
        refreshButton.setEnabled(false);
        loadMessage.setForeground(MainFrame.MUTED);
        loadMessage.setText("Đang tải số liệu từ cơ sở dữ liệu...");
        visitsChart.setMessage("Đang tải dữ liệu...");
        revenueChart.setMessage("Đang tải dữ liệu...");

        new SwingWorker<DashboardData, Void>() {
            @Override
            protected DashboardData doInBackground() throws DashboardException {
                return dashboardBUS.getDashboardData(session);
            }

            @Override
            protected void done() {
                try {
                    updateDashboard(get());
                    loadMessage.setText("Dữ liệu đã được cập nhật.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadError("Quá trình tải dữ liệu bị gián đoạn. Vui lòng thử lại.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause instanceof DashboardException
                            || cause instanceof SecurityException
                            ? cause.getMessage()
                            : "Không thể tải dữ liệu tổng quan. Vui lòng thử lại.";
                    showLoadError(message, cause);
                } finally {
                    loading = false;
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void updateDashboard(DashboardData data) {
        dateLabel.setText(data.asOfDate().format(DATE_FORMAT));
        DashboardStatistics statistics = data.statistics();
        statisticCards.get(0).setValues(
                Long.toString(statistics.appointmentsToday()),
                statistics.confirmedAppointmentsToday() + " đã xác nhận · "
                        + statistics.cancelledAppointmentsToday() + " đã hủy");
        statisticCards.get(1).setValues(
                Long.toString(statistics.visitsToday()),
                statistics.completedVisitsToday() + " hoàn tất · "
                        + statistics.activeVisitsToday() + " đang xử lý");
        statisticCards.get(2).setValues(
                formatCount(statistics.totalPatients()),
                formatCount(statistics.newPatientsThisMonth()) + " hồ sơ mới trong tháng");
        statisticCards.get(3).setValues(
                formatMoney(statistics.revenueToday()),
                "Cần thu: " + formatMoney(statistics.outstandingBalance()));

        visitsChart.setPoints(data.dailyVisits().stream()
                .map(item -> new ChartPoint(
                        item.date().format(CHART_DATE_FORMAT), item.visitCount()))
                .toList());
        revenueChart.setPoints(data.dailyRevenue().stream()
                .map(item -> new ChartPoint(
                        item.date().format(CHART_DATE_FORMAT), item.revenue().doubleValue()))
                .toList());

        appointmentTableModel.setRowCount(0);
        for (UpcomingAppointment appointment : data.upcomingAppointments()) {
            appointmentTableModel.addRow(new Object[]{
                appointment.appointmentId(),
                appointment.appointmentTime().format(APPOINTMENT_FORMAT),
                appointment.patientName(),
                appointment.doctorName(),
                appointment.specialty(),
                displayStatus(appointment.status())
            });
        }
        appointmentEmptyMessage.setText("Không có lịch hẹn sắp diễn ra.");
        ((CardLayout) appointmentContent.getLayout()).show(
                appointmentContent,
                data.upcomingAppointments().isEmpty() ? "empty" : "table");
    }

    private void showLoadError(String message, Throwable cause) {
        loadMessage.setForeground(new Color(190, 62, 62));
        loadMessage.setText(message);
        visitsChart.setMessage("Không thể tải dữ liệu.");
        revenueChart.setMessage("Không thể tải dữ liệu.");
        appointmentTableModel.setRowCount(0);
        appointmentEmptyMessage.setText("Không thể tải danh sách lịch hẹn.");
        ((CardLayout) appointmentContent.getLayout()).show(appointmentContent, "empty");
        if (!(cause instanceof DashboardException)
                && !(cause instanceof SecurityException)) {
            LOGGER.log(Level.SEVERE, "Unexpected error while loading Dashboard.", cause);
        }
    }

    private String displayStatus(String status) {
        return switch (status) {
            case "ChoXacNhan" -> "Chờ xác nhận";
            case "DaXacNhan" -> "Đã xác nhận";
            case "DaDen" -> "Đã đến";
            case "DangKham" -> "Đang khám";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            case "VangMat" -> "Vắng mặt";
            default -> status;
        };
    }

    private String formatCount(long value) {
        return String.format(VIETNAMESE, "%,d", value);
    }

    private String formatMoney(java.math.BigDecimal amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getNumberInstance(VIETNAMESE);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount) + " đ";
    }

    private JPanel sectionHeading(String title, String action) {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(MainFrame.TEXT);
        heading.add(label, BorderLayout.WEST);
        if (action != null) {
            JButton button = new JButton(action);
            button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            button.setForeground(MainFrame.BLUE);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            heading.add(button, BorderLayout.EAST);
        }
        return heading;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(16, 16, 16, 16)));
        return panel;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(37);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setForeground(MainFrame.TEXT);
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(MainFrame.MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 9));
        header.setReorderingAllowed(false);
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(MainFrame.BLUE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setMargin(new java.awt.Insets(10, 15, 10, 15));
        return button;
    }

    private JPanel gap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(1, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return gap;
    }

    private static final class StatCard extends JPanel {
        private final JLabel value = new JLabel("—");
        private final JLabel detail = new JLabel(" ");

        private StatCard(String titleText, String iconName) {
            super(new BorderLayout(12, 0));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MainFrame.BORDER),
                    new EmptyBorder(13, 14, 13, 14)));
            JPanel details = new JPanel(new BorderLayout(0, 5));
            details.setOpaque(false);
            JLabel title = new JLabel(titleText);
            title.setForeground(MainFrame.MUTED);
            title.setFont(new Font("Segoe UI", Font.BOLD, 9));
            value.setForeground(MainFrame.TEXT);
            value.setFont(new Font("Segoe UI", Font.BOLD, 21));
            detail.setForeground(new Color(92, 139, 115));
            detail.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            details.add(title, BorderLayout.NORTH);
            details.add(value, BorderLayout.CENTER);
            details.add(detail, BorderLayout.SOUTH);
            JLabel icon = new JLabel(new AppIcon(iconName, MainFrame.BLUE, 19), SwingConstants.CENTER);
            icon.setOpaque(true);
            icon.setForeground(MainFrame.BLUE);
            icon.setBackground(new Color(226, 248, 246));
            icon.setPreferredSize(new Dimension(40, 40));
            add(details, BorderLayout.CENTER);
            add(icon, BorderLayout.EAST);
        }

        private void setValues(String currentValue, String currentDetail) {
            value.setText(currentValue);
            detail.setText(currentDetail);
        }
    }

    private static final class ChartCard extends JPanel {
        private ChartCard(String title, String unit, ChartPlot plot) {
            super(new BorderLayout(0, 10));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MainFrame.BORDER),
                    new EmptyBorder(16, 16, 14, 16)));
            JPanel labels = new JPanel(new BorderLayout());
            labels.setOpaque(false);
            JLabel heading = new JLabel(title);
            heading.setForeground(MainFrame.TEXT);
            heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JLabel subheading = new JLabel(unit);
            subheading.setForeground(MainFrame.MUTED);
            subheading.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            labels.add(heading, BorderLayout.NORTH);
            labels.add(subheading, BorderLayout.SOUTH);
            add(labels, BorderLayout.NORTH);
            add(plot, BorderLayout.CENTER);
        }
    }

    private record ChartPoint(String label, double value) {
    }

    private static final class ChartPlot extends JPanel {
        private final Color plotColor;
        private final boolean bars;
        private final boolean money;
        private List<ChartPoint> points = List.of();
        private String message = "Chưa có dữ liệu trong khoảng thời gian này.";

        private ChartPlot(Color plotColor, boolean bars, boolean money) {
            this.plotColor = plotColor;
            this.bars = bars;
            this.money = money;
            setOpaque(false);
            setPreferredSize(new Dimension(380, 185));
        }

        private void setPoints(List<ChartPoint> points) {
            this.points = List.copyOf(points);
            this.message = null;
            repaint();
        }

        private void setMessage(String message) {
            this.message = message;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double maxValue = points.stream().mapToDouble(ChartPoint::value).max().orElse(0);
            if (message != null || points.isEmpty() || maxValue <= 0) {
                String emptyMessage = message != null
                        ? message : "Chưa có dữ liệu trong khoảng thời gian này.";
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(MainFrame.MUTED);
                int textWidth = g.getFontMetrics().stringWidth(emptyMessage);
                g.drawString(emptyMessage, Math.max(8, (getWidth() - textWidth) / 2),
                        getHeight() / 2);
                g.dispose();
                return;
            }

            int left = money ? 58 : 38;
            int right = 10;
            int top = 14;
            int bottom = 28;
            int width = Math.max(1, getWidth() - left - right);
            int height = Math.max(1, getHeight() - top - bottom);
            double tickStep;
            int tickIntervals;
            double upperBound;
            if (bars && !money) {
                upperBound = Math.max(1, Math.ceil(maxValue));
                tickIntervals = (int) Math.min(3, upperBound);
                tickStep = upperBound / tickIntervals;
            } else {
                tickStep = niceAxisStep(maxValue / 3);
                tickIntervals = 3;
                upperBound = tickStep * tickIntervals;
            }
            for (int tick = 0; tick <= tickIntervals; tick++) {
                int y = top + height * tick / tickIntervals;
                double tickValue = upperBound * (tickIntervals - tick) / tickIntervals;
                g.setColor(new Color(235, 239, 244));
                g.drawLine(left, y, left + width, y);
                g.setColor(MainFrame.MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g.drawString(formatAxisValue(tickValue, tickStep), 1, y + 3);
            }

            if (bars) {
                int step = Math.max(1, width / points.size());
                int barWidth = Math.max(5, step / 2);
                for (int i = 0; i < points.size(); i++) {
                    ChartPoint point = points.get(i);
                    int barHeight = (int) Math.round(height * point.value() / upperBound);
                    int x = left + i * step + (step - barWidth) / 2;
                    int y = top + height - barHeight;
                    g.setColor(new Color(
                            plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), 190));
                    if (barHeight > 0) {
                        g.fillRoundRect(x, y, barWidth, barHeight, 5, 5);
                    }
                    drawAxisLabel(g, point.label(), x + barWidth / 2, top + height + 17);
                }
            } else {
                Path2D line = new Path2D.Double();
                for (int i = 0; i < points.size(); i++) {
                    ChartPoint point = points.get(i);
                    int x = points.size() == 1
                            ? left + width / 2
                            : left + i * width / (points.size() - 1);
                    int y = top + height
                            - (int) Math.round(height * point.value() / upperBound);
                    if (i == 0) {
                        line.moveTo(x, y);
                    } else {
                        line.lineTo(x, y);
                    }
                    g.setColor(plotColor);
                    g.fillOval(x - 3, y - 3, 6, 6);
                    drawAxisLabel(g, point.label(), x, top + height + 17);
                }
                g.setColor(plotColor);
                g.setStroke(new BasicStroke(2.2f));
                g.draw(line);
            }
            g.dispose();
        }

        private double niceAxisStep(double rawStep) {
            double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
            double fraction = rawStep / magnitude;
            double niceFraction = fraction <= 1 ? 1
                    : fraction <= 2 ? 2
                    : fraction <= 5 ? 5 : 10;
            return niceFraction * magnitude;
        }

        private String formatAxisValue(double value, double step) {
            if (Math.abs(value) < 1e-9) {
                return "0";
            }
            double unit = 1;
            String suffix = "";
            if (money && value >= 1_000_000) {
                unit = 1_000_000;
                suffix = " tr";
            } else if (money && value >= 1_000) {
                unit = 1_000;
                suffix = " k";
            }
            double displayedValue = value / unit;
            double displayedStep = step / unit;
            int decimalPlaces = Math.max(0, Math.min(3,
                    BigDecimal.valueOf(displayedStep).stripTrailingZeros().scale()));
            return String.format(
                    VIETNAMESE, "%." + decimalPlaces + "f%s", displayedValue, suffix);
        }

        private void drawAxisLabel(Graphics2D g, String text, int x, int y) {
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g.setColor(MainFrame.MUTED);
            g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            if (String.valueOf(value).contains("Chờ")) {
                label.setForeground(new Color(173, 114, 35));
            } else if (String.valueOf(value).contains("Đang")
                    || String.valueOf(value).contains("Đến")) {
                label.setForeground(new Color(28, 125, 117));
            } else {
                label.setForeground(MainFrame.BLUE);
            }
            return label;
        }
    }
}
