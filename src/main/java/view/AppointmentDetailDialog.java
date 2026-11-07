package view;

import bus.appointment.AppointmentException;
import bus.appointment.LichHenBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.appointment.AppointmentHistoryEntry;
import model.appointment.LichHen;
import model.auth.AuthenticatedSession;

final class AppointmentDetailDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(AppointmentDetailDialog.class.getName());
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final AuthenticatedSession session;
    private final LichHenBUS appointmentBUS;
    private final long appointmentId;
    private final JPanel details = new JPanel(new java.awt.GridLayout(0, 2, 14, 8));
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"Thời điểm", "Người thay đổi", "Hành động",
                "Lý do", "Giá trị mới"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel message = new JLabel("Đang tải chi tiết lịch hẹn...");

    AppointmentDetailDialog(
            Frame owner, AuthenticatedSession session,
            LichHenBUS appointmentBUS, long appointmentId) {
        super(owner, "Chi tiết lịch hẹn", true);
        this.session = session;
        this.appointmentBUS = appointmentBUS;
        this.appointmentId = appointmentId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(760, 600));
        setSize(840, 700);
        setLocationRelativeTo(owner);
        loadData();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));
        details.setOpaque(false);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 16, 14, 16)));
        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        JLabel title = new JLabel("Thông tin lịch hẹn");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(MainFrame.TEXT);
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        top.add(title, BorderLayout.NORTH);
        top.add(message, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        JTable history = new JTable(historyModel);
        history.setRowHeight(34);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        history.setFillsViewportHeight(true);
        history.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        history.setPreferredScrollableViewportSize(new Dimension(760, 210));
        JScrollPane historyScroll = new JScrollPane(history);
        historyScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                "Lịch sử thay đổi"));
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(details, BorderLayout.NORTH);
        center.add(historyScroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private void loadData() {
        new SwingWorker<DetailData, Void>() {
            @Override
            protected DetailData doInBackground() throws AppointmentException {
                LichHen appointment = appointmentBUS.getById(session, appointmentId);
                List<AppointmentHistoryEntry> history =
                        appointmentBUS.getHistory(session, appointmentId);
                return new DetailData(appointment, history);
            }

            @Override
            protected void done() {
                try {
                    DetailData data = get();
                    populateDetails(data.appointment());
                    populateHistory(data.history());
                    message.setText(" ");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure("Tải chi tiết lịch hẹn bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showFailure(cause instanceof AppointmentException
                            ? cause.getMessage() : "Không thể tải chi tiết lịch hẹn.", cause);
                }
            }
        }.execute();
    }

    private void populateDetails(LichHen appointment) {
        addValue("Mã lịch hẹn", Long.toString(appointment.maLichHen()));
        addValue("Trạng thái", statusLabel(appointment.trangThai()));
        addValue("Ngày giờ", appointment.thoiGianHen() == null
                ? "—" : appointment.thoiGianHen().format(DATE_TIME));
        addValue("Mã bệnh nhân", appointment.maBenhNhan());
        addValue("Bệnh nhân", appointment.tenBenhNhan());
        addValue("Điện thoại", empty(appointment.soDienThoaiBenhNhan()));
        addValue("Bác sĩ", empty(appointment.tenBacSi()));
        addValue("Chuyên khoa", empty(appointment.tenChuyenKhoa()));
        addValue("Mã khung giờ", appointment.maKhungGio() == null
                ? "—" : appointment.maKhungGio().toString());
        addValue("Người tạo lịch", empty(appointment.tenNguoiTao()));
        addValue("Lý do khám", empty(appointment.lyDoKham()));
        addValue("Ghi chú", empty(appointment.ghiChu()));
    }

    private void addValue(String labelText, String valueText) {
        JLabel label = new JLabel(labelText);
        label.setForeground(MainFrame.MUTED);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        JLabel value = new JLabel(empty(valueText));
        value.setForeground(MainFrame.TEXT);
        value.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        details.add(label);
        details.add(value);
    }

    private void populateHistory(List<AppointmentHistoryEntry> entries) {
        historyModel.setRowCount(0);
        for (AppointmentHistoryEntry entry : entries) {
            historyModel.addRow(new Object[]{
                entry.thoiDiem() == null ? "—" : entry.thoiDiem().format(DATE_TIME),
                empty(entry.nguoiThayDoi()),
                entry.hanhDong(),
                empty(entry.lyDo()),
                empty(entry.giaTriMoi())
            });
        }
        if (entries.isEmpty()) {
            historyModel.addRow(new Object[]{"—", "—", "Chưa có lịch sử", "—", "—"});
        }
    }

    private void showFailure(String error, Throwable cause) {
        message.setForeground(new Color(190, 62, 62));
        message.setText(error);
        if (!(cause instanceof AppointmentException)) {
            LOGGER.log(Level.SEVERE, "Unexpected appointment detail error.", cause);
        }
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
            default -> empty(status);
        };
    }

    private String empty(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private record DetailData(LichHen appointment, List<AppointmentHistoryEntry> history) {
    }
}
