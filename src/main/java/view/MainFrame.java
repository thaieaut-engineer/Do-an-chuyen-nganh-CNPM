package view;

import com.formdev.flatlaf.FlatLightLaf;
import bus.auth.AuthenticationBUS;
import bus.auth.AuthenticationException;
import bus.auth.AuthorizationBUS;
import bus.appointment.LichHenBUS;
import bus.dashboard.DashboardBUS;
import bus.medicalrecord.PhieuKhamBUS;
import bus.patient.BenhNhanBUS;
import bus.prescription.DonThuocBUS;
import bus.reception.TiepNhanBUS;
import bus.serviceorder.ServiceOrderBUS;
import bus.visit.LuotKhamBUS;
import bus.warehouse.WarehouseBUS;
import bus.chemical.ChemicalBUS;
import bus.fee.FeeBUS;
import bus.schedule.ScheduleBUS;
import bus.account.AccountBUS;
import bus.billing.BillingBUS;
import bus.insurance.InsuranceBUS;
import bus.staff.StaffBUS;
import bus.device.DeviceBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import model.auth.AuthenticatedSession;
import view.component.UIStyle;

public class MainFrame extends JFrame {
    static final Color NAVY = new Color(24, 43, 69);
    static final Color BLUE = new Color(0x02B5AB);
    static final Color TEAL = BLUE;
    static final Color PAGE = new Color(246, 248, 251);
    static final Color TEXT = new Color(34, 48, 67);
    static final Color MUTED = new Color(119, 132, 149);
    static final Color BORDER = new Color(229, 234, 240);

    private final AuthenticatedSession session;
    private final AuthenticationBUS authenticationBUS = new AuthenticationBUS();
    private final AuthorizationBUS authorizationBUS = new AuthorizationBUS();
    private final LichHenBUS appointmentBUS = new LichHenBUS();
    private final TiepNhanBUS receptionBUS = new TiepNhanBUS();
    private final LuotKhamBUS visitBUS = new LuotKhamBUS();
    private final DashboardBUS dashboardBUS = new DashboardBUS();
    private final PhieuKhamBUS medicalRecordBUS = new PhieuKhamBUS();
    private final ServiceOrderBUS serviceOrderBUS = new ServiceOrderBUS();
    private final BenhNhanBUS patientBUS = new BenhNhanBUS();
    private final DonThuocBUS prescriptionBUS = new DonThuocBUS();
    private final WarehouseBUS warehouseBUS = new WarehouseBUS();
    private final ChemicalBUS chemicalBUS = new ChemicalBUS();
    private final FeeBUS feeBUS = new FeeBUS();
    private final ScheduleBUS scheduleBUS = new ScheduleBUS();
    private final AccountBUS accountBUS = new AccountBUS();
    private final BillingBUS billingBUS = new BillingBUS();
    private final InsuranceBUS insuranceBUS = new InsuranceBUS();
    private final StaffBUS staffBUS = new StaffBUS();
    private final DeviceBUS deviceBUS = new DeviceBUS();
    private final SidebarPanel sidebar;
    private final HeaderPanel header;
    private final JPanel pageHost = new JPanel(new BorderLayout());
    private final JPanel overviewHost = new JPanel(new BorderLayout());
    private final JPanel workspaceCenter = new JPanel(new BorderLayout());
    private final JPanel statisticsHost = new JPanel(new BorderLayout());
    private final JTabbedPane workspaceTabs = new JTabbedPane();
    private final List<String> availablePages;
    private PatientPanel patientPanel;
    private AppointmentPanel appointmentPanel;
    private ReceptionPanel receptionPanel;
    private VisitPanel visitPanel;
    private MedicalRecordPanel medicalRecordPanel;
    private ServiceOrderPanel serviceOrderPanel;
    private PrescriptionPanel prescriptionPanel;
    private MedicineWarehousePanel warehousePanel;
    private ChemicalWarehousePanel chemicalWarehousePanel;
    private FeePanel feePanel;
    private SchedulePanel schedulePanel;
    private AccountPanel accountPanel;
    private BillingPanel invoicePanel;
    private BillingPanel paymentPanel;
    private InsurancePanel insurancePanel;
    private StaffPanel staffPanel;
    private StaffPanel doctorPanel;
    private DevicePanel devicePanel;
    private MaintenancePanel maintenanceSchedulePanel;
    private MaintenancePanel maintenanceRepairPanel;
    private AdminCrudPanel evaluationPanel;
    private AdminCrudPanel configPanel;
    private AdminCrudPanel notificationPanel;
    private boolean logoutStarted;

    public MainFrame(AuthenticatedSession session) {
        super("Phòng khám Thu Cúc | Quản lý phòng khám");
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Không thể mở MainFrame khi chưa xác thực.");
        }
        this.session = session;
        sidebar = new SidebarPanel(session, this::showPage);
        availablePages = sidebar.getAvailablePages();
        header = new HeaderPanel(session, availablePages,
                this::showPage, this::confirmAndLogout,
                this::openChangePasswordDialog, sidebar::toggleCollapsed);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                confirmCloseProgram();
            }
        });
        setMinimumSize(new Dimension(1180, 760));
        setSize(1440, 900);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PAGE);
        root.add(sidebar, BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(PAGE);
        workspace.add(header, BorderLayout.NORTH);
        pageHost.setBackground(PAGE);
        workspaceTabs.addTab("Nghiệp vụ", new JPanel());
        statisticsHost.setBackground(PAGE);
        workspaceTabs.addTab("Thống kê chức năng", statisticsHost);
        workspaceTabs.setBackground(PAGE);
        workspaceTabs.setForeground(TEXT);
        overviewHost.setBackground(PAGE);
        workspaceCenter.add(overviewHost, BorderLayout.CENTER);
        workspace.add(workspaceCenter, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
        showPage("Tổng quan");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIStyle.applyGlobalDefaults();
                UIManager.put("Button.arc", 12);
                UIManager.put("Component.arc", 12);
                UIManager.put("ProgressBar.arc", 12);
                UIManager.put("Component.focusColor", new Color(0x02B5AB));
                UIManager.put("Component.focusedBorderColor", new Color(0x02B5AB));

                new view.auth.LoginFrame().setVisible(true);
            } catch (Exception e) {
                System.err.println("Không thể khởi động giao diện: " + e.getMessage());
            }
        });
    }

    private void showPage(String title) {
        if (!availablePages.contains(title)) {
            return;
        }
        try {
            authorizationBUS.requirePageAccess(session, title);
        } catch (SecurityException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sidebar.setActivePage(title);
        header.setPageTitle(title);
        workspaceTabs.setSelectedIndex(0);
        showWorkspaceLayout("Tổng quan".equals(title));
        JPanel page;
        if (title.equals("Tổng quan")) {
            page = new DashboardPanel(session, dashboardBUS,
                    this::showPage, availablePages::contains);
        } else if (title.equals("Bệnh nhân")) {
            if (patientPanel == null) {
                patientPanel = new PatientPanel(session, patientBUS);
            } else {
                patientPanel.refreshData();
            }
            page = patientPanel;
        } else if (title.equals("Lịch hẹn")) {
            if (appointmentPanel == null) {
                appointmentPanel = new AppointmentPanel(
                        session, appointmentBUS, patientBUS);
            } else {
                appointmentPanel.refreshData();
            }
            page = appointmentPanel;
        } else if (title.equals("Tiếp nhận bệnh nhân")) {
            if (receptionPanel == null) {
                receptionPanel = new ReceptionPanel(session, receptionBUS);
            }
            page = receptionPanel;
        } else if (title.equals("Lượt khám")) {
            if (visitPanel == null) {
                visitPanel = new VisitPanel(session, visitBUS, serviceOrderBUS);
            } else {
                visitPanel.refreshData();
            }
            page = visitPanel;
        } else if (title.equals("Phiếu khám")) {
            if (medicalRecordPanel == null) {
                medicalRecordPanel = new MedicalRecordPanel(session, medicalRecordBUS);
            } else {
                medicalRecordPanel.refreshData();
            }
            page = medicalRecordPanel;
        } else if (title.equals("Chỉ định dịch vụ")) {
            if (serviceOrderPanel == null) {
                serviceOrderPanel = new ServiceOrderPanel(session, serviceOrderBUS);
            } else {
                serviceOrderPanel.refreshData();
            }
            page = serviceOrderPanel;
        } else if (title.equals("Kê đơn thuốc")) {
            if (prescriptionPanel == null) {
                prescriptionPanel = new PrescriptionPanel(session, prescriptionBUS);
            } else {
                prescriptionPanel.refreshData();
            }
            page = prescriptionPanel;
        } else if (title.equals("Danh mục thuốc") || title.equals("Kho thuốc")) {
            if (warehousePanel == null) {
                warehousePanel = new MedicineWarehousePanel(
                        session, warehouseBUS, title);
            } else {
                warehousePanel.showSection(title);
            }
            page = warehousePanel;
        } else if (title.equals("Kho hóa chất")) {
            if (chemicalWarehousePanel == null) {
                chemicalWarehousePanel = new ChemicalWarehousePanel(session, chemicalBUS);
            }
            page = chemicalWarehousePanel;
        } else if (title.equals("Viện phí")) {
            if (feePanel == null) {
                feePanel = new FeePanel(session, feeBUS);
            }
            page = feePanel;
        } else if (title.equals("Lịch làm việc")) {
            if (schedulePanel == null) {
                schedulePanel = new SchedulePanel(session, scheduleBUS);
            }
            page = schedulePanel;
        } else if (title.equals("Tài khoản")) {
            if (accountPanel == null) {
                accountPanel = new AccountPanel(session, accountBUS);
            } else {
                accountPanel.refreshData();
            }
            page = accountPanel;
        } else if (title.equals("Hóa đơn")) {
            if (invoicePanel == null) {
                invoicePanel = new BillingPanel(session, billingBUS, false);
            }
            page = invoicePanel;
        } else if (title.equals("Thanh toán")) {
            if (paymentPanel == null) {
                paymentPanel = new BillingPanel(session, billingBUS, true);
            }
            page = paymentPanel;
        } else if (title.equals("Bảo hiểm y tế")) {
            if (insurancePanel == null) {
                insurancePanel = new InsurancePanel(session, insuranceBUS);
            }
            page = insurancePanel;
        } else if (title.equals("Nhân viên")) {
            if (staffPanel == null) {
                staffPanel = new StaffPanel(session, staffBUS, false);
            }
            page = staffPanel;
        } else if (title.equals("Bác sĩ")) {
            if (doctorPanel == null) {
                doctorPanel = new StaffPanel(session, staffBUS, true);
            }
            page = doctorPanel;
        } else if (title.equals("Thiết bị")) {
            if (devicePanel == null) {
                devicePanel = new DevicePanel(session, deviceBUS);
            }
            page = devicePanel;
        } else if (title.equals("Lịch bảo dưỡng")) {
            if (maintenanceSchedulePanel == null) {
                maintenanceSchedulePanel = new MaintenancePanel(session, true);
            }
            page = maintenanceSchedulePanel;
        } else if (title.equals("Sửa chữa thiết bị")) {
            if (maintenanceRepairPanel == null) {
                maintenanceRepairPanel = new MaintenancePanel(session, false);
            }
            page = maintenanceRepairPanel;
        } else if (title.equals("Đánh giá nhân viên")) {
            if (evaluationPanel == null) {
                evaluationPanel = new AdminCrudPanel(session, title);
            }
            page = evaluationPanel;
        } else if (title.equals("Cấu hình hệ thống")) {
            if (configPanel == null) {
                configPanel = new AdminCrudPanel(session, title);
            }
            page = configPanel;
        } else if (title.equals("Thông báo")) {
            if (notificationPanel == null) {
                notificationPanel = new AdminCrudPanel(session, title);
            }
            page = notificationPanel;
        } else if (title.equals("Phân quyền")
                || title.equals("Nhật ký hệ thống")
                || title.equals("Báo cáo")) {
            page = new AdminPanel(session, title);
        } else {
            page = new ModulePanel(title);
        }
        JScrollPane scrollPane = new JScrollPane(page);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getViewport().setBackground(PAGE);
        if ("Tổng quan".equals(title)) {
            overviewHost.removeAll();
            overviewHost.add(scrollPane, BorderLayout.CENTER);
        } else {
            pageHost.removeAll();
            pageHost.add(scrollPane, BorderLayout.CENTER);
        }
        updateStatisticsTab(title, page);
        showWorkspaceLayout("Tổng quan".equals(title));
        pageHost.revalidate();
        pageHost.repaint();
    }

    private void updateStatisticsTab(String title, JPanel page) {
        statisticsHost.removeAll();
        JPanel statistics;
        if (page instanceof FunctionStatisticsProvider provider) {
            statistics = provider.createStatisticsView();
        } else if ("Tổng quan".equals(title)) {
            statistics = new DashboardPanel(session, dashboardBUS,
                    this::showPage, availablePages::contains);
        } else {
            statistics = new JPanel(new BorderLayout());
            statistics.setBackground(PAGE);
            javax.swing.JLabel message = new javax.swing.JLabel(
                    "Chưa có bảng thống kê riêng cho chức năng này.",
                    javax.swing.SwingConstants.CENTER);
            message.setForeground(MUTED);
            statistics.add(message, BorderLayout.CENTER);
        }
        JPanel statisticsPage = new JPanel();
        statisticsPage.setLayout(new BoxLayout(statisticsPage, BoxLayout.Y_AXIS));
        statisticsPage.setBackground(PAGE);
        statisticsPage.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 26, 26, 26));
        statisticsPage.add(statisticsHeading(title));
        statisticsPage.add(Box.createVerticalStrut(16));
        statistics.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        statistics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        statisticsPage.add(statistics);
        statisticsPage.add(Box.createVerticalGlue());
        JScrollPane statisticsScroll = new JScrollPane(statisticsPage);
        statisticsScroll.setBorder(null);
        statisticsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        statisticsScroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        statisticsScroll.getViewport().setBackground(PAGE);
        statisticsHost.add(statisticsScroll, BorderLayout.CENTER);
        workspaceTabs.setTitleAt(1, "Thống kê: " + title);
        statisticsHost.revalidate();
        statisticsHost.repaint();
    }

    private void showWorkspaceLayout(boolean overview) {
        workspaceCenter.removeAll();
        if (overview) {
            workspaceCenter.add(overviewHost, BorderLayout.CENTER);
        } else {
            workspaceTabs.setComponentAt(0, pageHost);
            workspaceCenter.add(workspaceTabs, BorderLayout.CENTER);
        }
        workspaceCenter.revalidate();
        workspaceCenter.repaint();
    }

    private JPanel statisticsHeading(String title) {
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        heading.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        javax.swing.JLabel label = new javax.swing.JLabel("Thống kê " + title.toLowerCase());
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        label.setForeground(TEXT);
        javax.swing.JLabel description = new javax.swing.JLabel(
                "Theo dõi các chỉ số tổng hợp của chức năng đang chọn.");
        description.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        description.setForeground(MUTED);
        heading.add(label);
        heading.add(Box.createVerticalStrut(5));
        heading.add(description);
        return heading;
    }

    private void openChangePasswordDialog() {
        new view.auth.ChangePasswordDialog(this, session, authenticationBUS).setVisible(true);
    }

    private void confirmAndLogout() {
        if (logoutStarted) {
            return;
        }
        int choice = JOptionPane.showOptionDialog(
                this,
                "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Hủy", "Đăng xuất"},
                "Hủy");
        if (choice != 1) {
            return;
        }

        logoutAndReturnToLogin();
    }

    private void confirmCloseProgram() {
        if (logoutStarted) {
            return;
        }
        int choice = JOptionPane.showOptionDialog(
                this,
                "Bạn muốn đăng xuất để quay lại màn hình đăng nhập hay thoát hẳn chương trình?",
                "Đóng chương trình",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Hủy", "Đăng xuất", "Thoát chương trình"},
                "Hủy");
        if (choice == 1) {
            logoutAndReturnToLogin();
        } else if (choice == 2) {
            exitProgram();
        }
    }

    private void logoutAndReturnToLogin() {
        logoutStarted = true;
        String auditWarning = null;
        try {
            authenticationBUS.logout(session);
        } catch (AuthenticationException exception) {
            auditWarning = exception.getMessage();
        } finally {
            session.revoke();
            pageHost.removeAll();
            dispose();
            new view.auth.LoginFrame().setVisible(true);
        }
        if (auditWarning != null) {
            JOptionPane.showMessageDialog(
                    null, auditWarning, "Đăng xuất", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exitProgram() {
        logoutStarted = true;
        try {
            authenticationBUS.logout(session);
        } catch (AuthenticationException exception) {
            // Vẫn đóng ứng dụng nếu chỉ ghi nhật ký đăng xuất gặp lỗi.
        } finally {
            session.revoke();
            pageHost.removeAll();
            dispose();
            System.exit(0);
        }
    }

}
