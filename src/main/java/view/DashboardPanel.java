package view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import mock.DashboardMockData;
import mock.DashboardMockData.Activity;
import mock.DashboardMockData.Appointment;
import mock.DashboardMockData.AppointmentStatus;
import mock.DashboardMockData.ChartPoint;
import mock.DashboardMockData.Statistic;
import view.component.AppIcon;

final class DashboardPanel extends JPanel {
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    DashboardPanel(Consumer<String> openPage, Predicate<String> canNavigate) {
        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(25, 28, 30, 28));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(createHeading(openPage, canNavigate));
        content.add(gap(20));
        content.add(createStatistics());
        content.add(gap(18));
        content.add(createCharts());
        content.add(gap(18));
        content.add(createAppointmentAndActivity(openPage, canNavigate));
        content.add(gap(18));
        content.add(createAppointmentStatus());
        add(content, BorderLayout.NORTH);
    }

    private JPanel createHeading(Consumer<String> openPage, Predicate<String> canNavigate) {
        JPanel heading = new JPanel(new BorderLayout(15, 0));
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tổng quan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel date = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM, yyyy", VIETNAMESE)));
        date.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        date.setForeground(MainFrame.MUTED);
        titles.add(title);
        titles.add(gap(4));
        titles.add(date);

        heading.add(titles, BorderLayout.WEST);
        if (canNavigate.test("Lịch hẹn")) {
            JButton booking = primaryButton("Đặt lịch hẹn");
            booking.setIcon(new AppIcon("add", Color.WHITE, 16));
            booking.setIconTextGap(8);
            booking.addActionListener(event -> openPage.accept("Lịch hẹn"));
            heading.add(booking, BorderLayout.EAST);
        }
        return heading;
    }

    private JPanel createStatistics() {
        JPanel stats = new JPanel(new GridLayout(2, 3, 14, 14));
        stats.setOpaque(false);
        for (Statistic statistic : DashboardMockData.STATISTICS) {
            stats.add(new StatCard(statistic));
        }
        return stats;
    }

    private JPanel createCharts() {
        JPanel charts = new JPanel(new GridLayout(1, 2, 14, 0));
        charts.setOpaque(false);
        charts.add(new ChartCard("Bệnh nhân mới theo tháng", "Số hồ sơ", DashboardMockData.PATIENT_TREND,
                MainFrame.BLUE, false));
        charts.add(new ChartCard("Doanh thu theo tháng", "Đơn vị: triệu đồng",
                DashboardMockData.REVENUE_TREND, MainFrame.TEAL, true));
        return charts;
    }

    private JPanel createAppointmentAndActivity(
            Consumer<String> openPage, Predicate<String> canNavigate) {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.add(createAppointments(openPage, canNavigate));
        row.add(createRecentActivities());
        return row;
    }

    private JPanel createAppointments(Consumer<String> openPage, Predicate<String> canNavigate) {
        JPanel panel = cardPanel();
        boolean canOpenAppointments = canNavigate.test("Lịch hẹn");
        JPanel heading = sectionHeading("Lịch hẹn gần nhất",
                canOpenAppointments ? "Xem tất cả" : null);
        if (canOpenAppointments) {
            ((JButton) heading.getComponent(1)).addActionListener(event -> openPage.accept("Lịch hẹn"));
        }
        String[] columns = {"GIỜ", "BỆNH NHÂN", "BÁC SĨ", "CHUYÊN KHOA", "TRẠNG THÁI"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Appointment appointment : DashboardMockData.APPOINTMENTS) {
            model.addRow(new Object[]{appointment.time, appointment.patient,
                appointment.doctor, appointment.specialty, appointment.status});
        }
        JTable table = new JTable(model);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(52);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(105);
        table.getColumnModel().getColumn(3).setPreferredWidth(104);
        table.getColumnModel().getColumn(4).setPreferredWidth(105);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRecentActivities() {
        JPanel panel = cardPanel();
        panel.add(sectionHeading("Hoạt động gần đây", null), BorderLayout.NORTH);
        JPanel list = new JPanel(new GridLayout(DashboardMockData.ACTIVITIES.size(), 1, 0, 0));
        list.setOpaque(false);
        for (Activity activity : DashboardMockData.ACTIVITIES) {
            list.add(activityRow(activity));
        }
        panel.add(list, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAppointmentStatus() {
        JPanel panel = cardPanel();
        panel.add(sectionHeading("Tỷ lệ trạng thái lịch hẹn hôm nay", null), BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(DashboardMockData.APPOINTMENT_STATUSES.size(), 1, 0, 8));
        rows.setOpaque(false);
        int total = DashboardMockData.APPOINTMENT_STATUSES.stream().mapToInt(s -> s.count).sum();
        for (AppointmentStatus status : DashboardMockData.APPOINTMENT_STATUSES) {
            rows.add(statusRow(status, total));
        }
        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private JPanel statusRow(AppointmentStatus status, int total) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        JLabel label = new JLabel(status.label);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int height = getHeight();
                int width = Math.max(12, (int) (getWidth() * (status.count / (double) total)));
                g.setColor(new Color(239, 243, 247));
                g.fillRoundRect(0, 2, getWidth(), height - 4, height, height);
                g.setColor(status.color);
                g.fillRoundRect(0, 2, width, height - 4, height, height);
                g.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(180, 16));
        JLabel count = new JLabel(status.count + " lịch", SwingConstants.RIGHT);
        count.setForeground(MainFrame.MUTED);
        count.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        row.add(label, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        row.add(count, BorderLayout.EAST);
        return row;
    }

    private JPanel activityRow(Activity activity) {
        JPanel row = new JPanel(new BorderLayout(11, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));
        JLabel marker = new JLabel(new AppIcon(activity.icon, activity.color, 15), SwingConstants.CENTER);
        marker.setForeground(activity.color);
        marker.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        marker.setPreferredSize(new Dimension(20, 20));
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        JLabel title = new JLabel(activity.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(MainFrame.TEXT);
        JLabel detail = new JLabel(activity.detail);
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        detail.setForeground(MainFrame.MUTED);
        text.add(title, BorderLayout.NORTH);
        text.add(detail, BorderLayout.SOUTH);
        JLabel time = new JLabel(activity.time);
        time.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        time.setForeground(MainFrame.MUTED);
        row.add(marker, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(time, BorderLayout.EAST);
        return row;
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
        private StatCard(Statistic statistic) {
            super(new BorderLayout(12, 0));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MainFrame.BORDER),
                    new EmptyBorder(14, 15, 14, 15)));
            JPanel details = new JPanel(new BorderLayout(0, 6));
            details.setOpaque(false);
            JLabel title = new JLabel(statistic.title);
            title.setForeground(MainFrame.MUTED);
            title.setFont(new Font("Segoe UI", Font.BOLD, 9));
            JLabel value = new JLabel(statistic.value);
            value.setForeground(MainFrame.TEXT);
            value.setFont(new Font("Segoe UI", Font.BOLD, 23));
            JLabel detail = new JLabel(statistic.detail);
            detail.setForeground(new Color(92, 139, 115));
            detail.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            details.add(title, BorderLayout.NORTH);
            details.add(value, BorderLayout.CENTER);
            details.add(detail, BorderLayout.SOUTH);
            JLabel icon = new JLabel(new AppIcon(statistic.icon, MainFrame.BLUE, 19), SwingConstants.CENTER);
            icon.setOpaque(true);
            icon.setForeground(MainFrame.BLUE);
            icon.setBackground(new Color(226, 248, 246));
            icon.setPreferredSize(new Dimension(40, 40));
            add(details, BorderLayout.CENTER);
            add(icon, BorderLayout.EAST);
        }
    }

    private static final class ChartCard extends JPanel {
        private ChartCard(String title, String unit, List<ChartPoint> points, Color lineColor, boolean bars) {
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
            add(new ChartPlot(points, lineColor, bars), BorderLayout.CENTER);
        }
    }

    private static final class ChartPlot extends JPanel {
        private final List<ChartPoint> points;
        private final Color plotColor;
        private final boolean bars;

        private ChartPlot(List<ChartPoint> points, Color plotColor, boolean bars) {
            this.points = points;
            this.plotColor = plotColor;
            this.bars = bars;
            setOpaque(false);
            setPreferredSize(new Dimension(380, 185));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 35;
            int right = 10;
            int top = 14;
            int bottom = 25;
            int width = getWidth() - left - right;
            int height = getHeight() - top - bottom;
            int max = points.stream().mapToInt(p -> p.value).max().orElse(1);
            for (int i = 0; i < 4; i++) {
                int y = top + height * i / 3;
                g.setColor(new Color(235, 239, 244));
                g.drawLine(left, y, left + width, y);
                g.setColor(MainFrame.MUTED);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                g.drawString(String.valueOf((max * (3 - i)) / 3), 1, y + 3);
            }
            if (bars) {
                int step = width / points.size();
                int barWidth = Math.max(5, step / 2);
                for (int i = 0; i < points.size(); i++) {
                    int barHeight = Math.max(2, height * points.get(i).value / max);
                    int x = left + i * step + (step - barWidth) / 2;
                    int y = top + height - barHeight;
                    g.setColor(new Color(plotColor.getRed(), plotColor.getGreen(),
                            plotColor.getBlue(), 190));
                    g.fillRoundRect(x, y, barWidth, barHeight, 5, 5);
                    drawAxisLabel(g, points.get(i).label, x + barWidth / 2, top + height + 16);
                }
            } else {
                Path2D line = new Path2D.Double();
                for (int i = 0; i < points.size(); i++) {
                    int x = left + i * width / (points.size() - 1);
                    int y = top + height - height * points.get(i).value / max;
                    if (i == 0) {
                        line.moveTo(x, y);
                    } else {
                        line.lineTo(x, y);
                    }
                    g.setColor(plotColor);
                    g.fillOval(x - 3, y - 3, 6, 6);
                    if (i % 2 == 0) {
                        drawAxisLabel(g, points.get(i).label, x, top + height + 16);
                    }
                }
                g.setColor(plotColor);
                g.setStroke(new BasicStroke(2.2f));
                g.draw(line);
            }
            g.dispose();
        }

        private void drawAxisLabel(Graphics2D g, String text, int x, int y) {
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g.setColor(MainFrame.MUTED);
            g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            String status = String.valueOf(value);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            if (status.contains("Chờ")) {
                label.setForeground(new Color(173, 114, 35));
            } else if (status.contains("Đang") || status.contains("Đến")) {
                label.setForeground(new Color(28, 125, 117));
            } else {
                label.setForeground(MainFrame.BLUE);
            }
            return label;
        }
    }
}
