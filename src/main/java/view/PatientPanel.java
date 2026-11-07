package view;

import bus.patient.BenhNhanBUS;
import bus.patient.PatientException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.auth.AuthenticatedSession;
import model.patient.BenhNhan;
import model.patient.PatientFilter;
import model.patient.PatientPage;
import view.component.AppIcon;

final class PatientPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(PatientPanel.class.getName());
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthenticatedSession session;
    private final BenhNhanBUS patientBUS;
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> genderFilter =
            new JComboBox<>(new String[]{"Tất cả giới tính", "Nam", "Nữ", "Khác"});
    private final JComboBox<StatusFilter> statusFilter =
            new JComboBox<>(new StatusFilter[]{
                new StatusFilter("Tất cả trạng thái", ""),
                new StatusFilter("Đang hoạt động", "HoatDong"),
                new StatusFilter("Tạm khóa", "TamKhoa")
            });
    private final JLabel resultCount = new JLabel(" ");
    private final JLabel pageLabel = new JLabel("Trang 1 / 1");
    private final JLabel message = new JLabel(" ");
    private final JButton clearSearchButton = new JButton("Xóa");
    private final JButton previousButton = new JButton("Trước");
    private final JButton nextButton = new JButton("Sau");
    private final JButton refreshButton = new JButton("Làm mới");
    private final JButton addButton = new JButton("+ Thêm bệnh nhân");
    private final JButton detailsButton = new JButton("Xem chi tiết");
    private final JButton editButton = new JButton("Chỉnh sửa");
    private final JButton statusButton = new JButton("Tạm khóa");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Mã bệnh nhân", "Họ tên", "Ngày sinh", "Giới tính",
                "Số điện thoại", "Email", "Trạng thái"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final Timer searchTimer;

    private int currentPage;
    private int totalPages = 1;
    private boolean loading;
    private boolean reloadAfterLoad;

    PatientPanel(
            AuthenticatedSession session,
            BenhNhanBUS patientBUS) {
        if (session == null || !session.isActive()) {
            throw new IllegalArgumentException("Không thể mở danh sách bệnh nhân khi chưa đăng nhập.");
        }
        this.session = session;
        this.patientBUS = patientBUS;
        this.searchTimer = new Timer(350, event -> {
            currentPage = 0;
            loadPatients();
        });
        searchTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(MainFrame.PAGE);
        setBorder(new EmptyBorder(25, 28, 30, 28));
        add(createContent(), BorderLayout.NORTH);
        loadPatients();
    }

    void refreshData() {
        loadPatients();
    }

    private JPanel createContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.add(createHeading());
        content.add(gap(20));
        content.add(createListCard());
        return content;
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new javax.swing.BoxLayout(labels, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản lý bệnh nhân");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(MainFrame.TEXT);
        JLabel description = new JLabel("Quản lý hồ sơ và thông tin bệnh nhân của phòng khám.");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        description.setForeground(MainFrame.MUTED);
        labels.add(title);
        labels.add(gap(5));
        labels.add(description);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.setToolTipText("Làm mới danh sách từ cơ sở dữ liệu");
        refreshButton.addActionListener(event -> {
            currentPage = 0;
            loadPatients();
        });
        actions.add(refreshButton);
        if (canEdit()) {
            configurePrimaryButton(addButton);
            addButton.addActionListener(event -> openPatientDialog(
                    null, PatientDialog.Mode.CREATE));
            actions.add(addButton);
        }
        heading.add(labels, BorderLayout.WEST);
        heading.add(actions, BorderLayout.EAST);
        return heading;
    }

    private JPanel createListCard() {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(16, 16, 14, 16)));
        card.add(createToolbar(), BorderLayout.NORTH);

        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(MainFrame.TEXT);
        table.setGridColor(MainFrame.BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(226, 248, 246));
        table.setSelectionForeground(MainFrame.TEXT);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(135);
        table.getColumnModel().getColumn(5).setPreferredWidth(210);
        table.getColumnModel().getColumn(6).setPreferredWidth(125);
        table.getColumnModel().getColumn(6).setCellRenderer(new PatientStatusRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectionActions();
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showSelectedPatient(PatientDialog.Mode.DETAILS);
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(MainFrame.MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 10));
        header.setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 470));
        card.add(scroll, BorderLayout.CENTER);
        card.add(createFooter(), BorderLayout.SOUTH);
        return card;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        searchField.setPreferredSize(new Dimension(300, 36));
        searchField.putClientProperty(
                "JTextField.placeholderText", "Tìm mã, họ tên, SĐT, email hoặc giấy tờ...");
        clearSearchButton.setToolTipText("Xóa nội dung tìm kiếm");
        clearSearchButton.addActionListener(event -> searchField.setText(""));
        JPanel search = new JPanel(new BorderLayout(5, 0));
        search.setOpaque(false);
        search.add(searchField, BorderLayout.CENTER);
        search.add(clearSearchButton, BorderLayout.EAST);
        filters.add(search);
        genderFilter.addActionListener(event -> resetAndLoad());
        statusFilter.addActionListener(event -> resetAndLoad());
        filters.add(genderFilter);
        filters.add(statusFilter);

        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rowActions.setOpaque(false);
        detailsButton.addActionListener(event -> showSelectedPatient(PatientDialog.Mode.DETAILS));
        detailsButton.setEnabled(false);
        rowActions.add(detailsButton);
        if (canEdit()) {
            editButton.addActionListener(event -> showSelectedPatient(PatientDialog.Mode.EDIT));
            statusButton.addActionListener(event -> changeSelectedPatientStatus());
            editButton.setEnabled(false);
            statusButton.setEnabled(false);
            rowActions.add(editButton);
            rowActions.add(statusButton);
        }
        toolbar.add(filters, BorderLayout.CENTER);
        toolbar.add(rowActions, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                searchTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                searchTimer.restart();
            }
        });
        return toolbar;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 0, 0, 0));
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        resultCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        resultCount.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        message.setForeground(MainFrame.MUTED);
        left.add(resultCount, BorderLayout.WEST);
        left.add(message, BorderLayout.CENTER);

        JPanel pages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pages.setOpaque(false);
        previousButton.addActionListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                loadPatients();
            }
        });
        nextButton.addActionListener(event -> {
            if (currentPage + 1 < totalPages) {
                currentPage++;
                loadPatients();
            }
        });
        pageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pageLabel.setForeground(MainFrame.MUTED);
        pages.add(previousButton);
        pages.add(pageLabel);
        pages.add(nextButton);
        footer.add(left, BorderLayout.CENTER);
        footer.add(pages, BorderLayout.EAST);
        return footer;
    }

    private void resetAndLoad() {
        currentPage = 0;
        loadPatients();
    }

    private void loadPatients() {
        if (loading) {
            reloadAfterLoad = true;
            return;
        }
        loading = true;
        refreshButton.setEnabled(false);
        clearSearchButton.setEnabled(false);
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
        message.setForeground(MainFrame.MUTED);
        message.setText("Đang tải danh sách...");
        String gender = genderFilter.getSelectedIndex() == 0
                ? "" : (String) genderFilter.getSelectedItem();
        StatusFilter status = (StatusFilter) statusFilter.getSelectedItem();
        PatientFilter filter = new PatientFilter(
                searchField.getText().trim(), gender,
                status == null ? "" : status.value(), currentPage, PAGE_SIZE);

        new SwingWorker<PatientPage, Void>() {
            @Override
            protected PatientPage doInBackground() throws PatientException {
                return patientBUS.search(session, filter);
            }

            @Override
            protected void done() {
                try {
                    PatientPage page = get();
                    if (currentPage >= page.totalPages()) {
                        currentPage = page.totalPages() - 1;
                        reloadAfterLoad = true;
                    }
                    populateTable(page);
                    message.setText(page.totalCount() == 0
                            ? (filter.searchText().isBlank()
                                    ? "Chưa có hồ sơ bệnh nhân."
                                    : "Không tìm thấy bệnh nhân phù hợp.")
                            : " ");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    handleLoadFailure("Tải danh sách bị gián đoạn. Vui lòng thử lại.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    handleLoadFailure(cause instanceof PatientException
                            ? cause.getMessage()
                            : "Không thể tải danh sách. Vui lòng kiểm tra kết nối cơ sở dữ liệu.",
                            cause);
                } finally {
                    loading = false;
                    refreshButton.setEnabled(true);
                    clearSearchButton.setEnabled(true);
                    updateSelectionActions();
                    boolean reload = reloadAfterLoad;
                    reloadAfterLoad = false;
                    if (reload) {
                        SwingUtilities.invokeLater(PatientPanel.this::loadPatients);
                    }
                }
            }
        }.execute();
    }

    private void populateTable(PatientPage page) {
        tableModel.setRowCount(0);
        for (BenhNhan patient : page.patients()) {
            tableModel.addRow(new Object[]{
                patient.maBenhNhan(),
                patient.hoTen(),
                patient.ngaySinh() == null ? "—" : patient.ngaySinh().format(DATE_FORMAT),
                patient.gioiTinh() == null ? "—" : patient.gioiTinh(),
                patient.soDienThoai() == null ? "—" : patient.soDienThoai(),
                patient.email() == null ? "—" : patient.email(),
                "HoatDong".equals(patient.trangThai()) ? "Đang hoạt động" : "Tạm khóa"
            });
        }
        totalPages = page.totalPages();
        pageLabel.setText("Trang " + (page.page() + 1) + " / " + totalPages);
        resultCount.setText(page.totalCount() + " bệnh nhân");
        previousButton.setEnabled(page.page() > 0);
        nextButton.setEnabled(page.page() + 1 < totalPages);
    }

    private void handleLoadFailure(String error, Throwable cause) {
        tableModel.setRowCount(0);
        totalPages = 1;
        pageLabel.setText("Trang 1 / 1");
        resultCount.setText("Không tải được dữ liệu");
        message.setForeground(new Color(190, 62, 62));
        message.setText(error);
        if (!(cause instanceof PatientException)) {
            LOGGER.log(Level.SEVERE, "Unexpected error while loading patients.", cause);
        }
    }

    private void showSelectedPatient(PatientDialog.Mode mode) {
        String patientId = selectedPatientId();
        if (patientId == null) {
            return;
        }
        setActionButtonsEnabled(false);
        message.setForeground(MainFrame.MUTED);
        message.setText("Đang tải hồ sơ...");
        new SwingWorker<BenhNhan, Void>() {
            @Override
            protected BenhNhan doInBackground() throws PatientException {
                return patientBUS.getById(session, patientId);
            }

            @Override
            protected void done() {
                try {
                    BenhNhan patient = get();
                    message.setText(" ");
                    openPatientDialog(patient, mode);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showActionError("Tải hồ sơ bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showActionError(cause instanceof PatientException
                            ? cause.getMessage() : "Không thể tải hồ sơ bệnh nhân.", cause);
                } finally {
                    updateSelectionActions();
                }
            }
        }.execute();
    }

    private void openPatientDialog(BenhNhan patient, PatientDialog.Mode mode) {
        PatientDialog dialog = new PatientDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                session, patientBUS, patient, mode,
                patientId -> {
                    message.setForeground(new Color(45, 130, 90));
                    message.setText("Đã lưu hồ sơ " + patientId + ".");
                    loadPatients();
                });
        dialog.setVisible(true);
    }

    private void changeSelectedPatientStatus() {
        String patientId = selectedPatientId();
        if (patientId == null || !canEdit()) {
            return;
        }
        String displayedStatus = String.valueOf(tableModel.getValueAt(
                table.convertRowIndexToModel(table.getSelectedRow()), 6));
        boolean currentlyActive = "Đang hoạt động".equals(displayedStatus);
        String newStatus = currentlyActive ? "TamKhoa" : "HoatDong";
        String action = currentlyActive ? "tạm khóa" : "kích hoạt lại";
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn " + action + " hồ sơ " + patientId + "?\n"
                        + "Hồ sơ và lịch sử khám sẽ được giữ nguyên.",
                "Xác nhận cập nhật trạng thái",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        setActionButtonsEnabled(false);
        message.setForeground(MainFrame.MUTED);
        message.setText("Đang cập nhật trạng thái...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws PatientException {
                patientBUS.updateStatus(session, patientId, newStatus);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    message.setForeground(new Color(45, 130, 90));
                    message.setText(currentlyActive
                            ? "Đã tạm khóa hồ sơ " + patientId + "."
                            : "Đã kích hoạt lại hồ sơ " + patientId + ".");
                    loadPatients();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showActionError("Cập nhật trạng thái bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showActionError(cause instanceof PatientException
                            ? cause.getMessage() : "Không thể cập nhật trạng thái.", cause);
                } finally {
                    updateSelectionActions();
                }
            }
        }.execute();
    }

    private void showActionError(String error, Throwable cause) {
        message.setForeground(new Color(190, 62, 62));
        message.setText(error);
        if (!(cause instanceof PatientException)) {
            LOGGER.log(Level.SEVERE, "Unexpected patient action error.", cause);
        }
    }

    private String selectedPatientId() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        return String.valueOf(tableModel.getValueAt(modelRow, 0));
    }

    private void updateSelectionActions() {
        boolean selected = table.getSelectedRow() >= 0 && !loading;
        detailsButton.setEnabled(selected);
        if (canEdit()) {
            editButton.setEnabled(selected);
            statusButton.setEnabled(selected);
            if (selected) {
                int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                String status = String.valueOf(tableModel.getValueAt(modelRow, 6));
                statusButton.setText("Đang hoạt động".equals(status) ? "Tạm khóa" : "Kích hoạt");
            } else {
                statusButton.setText("Tạm khóa");
            }
        }
    }

    private void setActionButtonsEnabled(boolean enabled) {
        detailsButton.setEnabled(enabled);
        if (canEdit()) {
            editButton.setEnabled(enabled);
            statusButton.setEnabled(enabled);
        }
    }

    private boolean canEdit() {
        return session.hasPermission("BN_SUA");
    }

    private void configurePrimaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(MainFrame.BLUE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setIcon(new AppIcon("add", Color.WHITE, 16));
        button.setIconTextGap(8);
        button.setMargin(new java.awt.Insets(10, 14, 10, 14));
    }

    private JPanel gap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(1, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return gap;
    }

    private record StatusFilter(String label, String value) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final class PatientStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if ("Tạm khóa".equals(value)) {
                label.setForeground(new Color(173, 114, 35));
            } else {
                label.setForeground(new Color(28, 125, 117));
            }
            return label;
        }
    }
}
