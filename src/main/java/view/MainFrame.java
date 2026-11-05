package view;

import com.formdev.flatlaf.FlatLightLaf;
import bus.auth.AuthorizationBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import model.auth.AuthenticatedSession;

public class MainFrame extends JFrame {
    static final Color NAVY = new Color(24, 43, 69);
    static final Color BLUE = new Color(0x02B5AB);
    static final Color TEAL = BLUE;
    static final Color PAGE = new Color(246, 248, 251);
    static final Color TEXT = new Color(34, 48, 67);
    static final Color MUTED = new Color(119, 132, 149);
    static final Color BORDER = new Color(229, 234, 240);

    private final AuthenticatedSession session;
    private final AuthorizationBUS authorizationBUS = new AuthorizationBUS();
    private final SidebarPanel sidebar;
    private final HeaderPanel header;
    private final JPanel pageHost = new JPanel(new BorderLayout());
    private final List<String> availablePages;

    public MainFrame(AuthenticatedSession session) {
        super("Phòng khám Thu Cúc | Quản lý phòng khám");
        if (session == null) {
            throw new IllegalArgumentException("Không thể mở MainFrame khi chưa xác thực.");
        }
        this.session = session;
        sidebar = new SidebarPanel(session, this::showPage);
        availablePages = sidebar.getAvailablePages();
        header = new HeaderPanel(session, availablePages,
                this::showPage, this::logout, sidebar::toggleCollapsed);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
        workspace.add(pageHost, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
        showPage("Tổng quan");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
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
        JPanel page = title.equals("Tổng quan")
                ? new DashboardPanel(this::showPage, availablePages::contains)
                : new ModulePanel(title);
        JScrollPane scrollPane = new JScrollPane(page);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getViewport().setBackground(PAGE);
        pageHost.removeAll();
        pageHost.add(scrollPane, BorderLayout.CENTER);
        pageHost.revalidate();
        pageHost.repaint();
    }

    private void logout() {
        dispose();
        new view.auth.LoginFrame().setVisible(true);
    }
}