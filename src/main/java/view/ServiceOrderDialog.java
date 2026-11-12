package view;

import bus.serviceorder.ServiceOrderException;
import bus.serviceorder.ServiceOrderBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.LongConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.auth.AuthenticatedSession;
import model.serviceorder.EligibleOrderVisit;
import model.serviceorder.ServiceCatalogItem;
import model.serviceorder.ServiceOrderDetail;
import model.serviceorder.ServiceOrderDraft;

final class ServiceOrderDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(ServiceOrderDialog.class.getName());
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final ServiceOrderBUS serviceOrderBUS;
    private final Long orderId;
    private final LongConsumer onSaved;
    private final JComboBox<EligibleOrderVisit> visitCombo = new JComboBox<>();
    private final JComboBox<String> typeCombo = new JComboBox<>();
    private final JComboBox<ServiceCatalogItem> serviceCombo = new JComboBox<>();
    private final JTextField serviceSearch = new JTextField();
    private final JLabel visitInfo = new JLabel(" ");
    private final JLabel serviceInfo = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final JTextArea noteArea = new JTextArea(4, 30);
    private final JButton saveButton = new JButton("Lưu chỉ định");
    private final JButton cancelButton = new JButton("Hủy");
    private List<EligibleOrderVisit> visits = List.of();
    private List<ServiceCatalogItem> services = List.of();
    private ServiceOrderDetail loadedDetail;
    private boolean loading = true;
    private boolean saving;
    private boolean allowSave;

    ServiceOrderDialog(
            java.awt.Window owner,
            AuthenticatedSession session,
            ServiceOrderBUS serviceOrderBUS,
            Long orderId,
            LongConsumer onSaved) {
        super(owner,
                orderId == null ? "Tạo chỉ định dịch vụ" : "Chỉnh sửa chỉ định dịch vụ",
                ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.serviceOrderBUS = serviceOrderBUS;
        this.orderId = orderId;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(660, 480));
        setSize(760, 560);
        setLocationRelativeTo(owner);
        configureEvents();
        loadData();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 22, 16, 22));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        if (orderId == null) {
            addRow(form, row++, "Lượt khám *", visitCombo);
        } else {
            addRow(form, row++, "Mã chỉ định", new JLabel(Long.toString(orderId)));
        }
        visitInfo.setForeground(MainFrame.MUTED);
        addRow(form, row++, "Bệnh nhân / bác sĩ", visitInfo);
        serviceSearch.setPreferredSize(new Dimension(0, 34));
        addRow(form, row++, "Tìm dịch vụ", serviceSearch);
        typeCombo.setPreferredSize(new Dimension(0, 34));
        addRow(form, row++, "Loại dịch vụ", typeCombo);
        serviceCombo.setPreferredSize(new Dimension(0, 36));
        addRow(form, row++, "Dịch vụ *", serviceCombo);
        serviceInfo.setForeground(MainFrame.MUTED);
        addRow(form, row++, "Giá hiện hành", serviceInfo);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(7, 8, 7, 8)));
        addRow(form, row, "Ghi chú", new JScrollPane(noteArea));

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);
        message.setForeground(MainFrame.MUTED);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        cancelButton.addActionListener(event -> dispose());
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(MainFrame.BLUE);
        saveButton.setOpaque(true);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(event -> save());
        buttons.add(cancelButton);
        buttons.add(saveButton);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(message, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
        setControlsEnabled(false);
        return root;
    }

    private void configureEvents() {
        visitCombo.addActionListener(event -> showSelectedVisit());
        serviceCombo.addActionListener(event -> showSelectedService());
        typeCombo.addActionListener(event -> refreshServices());
        serviceSearch.addActionListener(event -> refreshServices());
        serviceSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                refreshServices();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                refreshServices();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                refreshServices();
            }
        });
    }

    private void loadData() {
        message.setText(orderId == null
                ? "Đang tải lượt khám và dịch vụ..." : "Đang tải chỉ định...");
        new SwingWorker<DialogData, Void>() {
            @Override
            protected DialogData doInBackground() throws ServiceOrderException {
                List<EligibleOrderVisit> eligible = orderId == null
                        ? serviceOrderBUS.getEligibleVisits(session) : List.of();
                List<ServiceCatalogItem> catalog =
                        serviceOrderBUS.getServices(session, "", "");
                ServiceOrderDetail detail = orderId == null
                        ? null : serviceOrderBUS.getById(session, orderId);
                return new DialogData(eligible, catalog, detail);
            }

            @Override
            protected void done() {
                try {
                    DialogData data = get();
                    visits = data.visits();
                    services = data.services();
                    loadedDetail = data.detail();
                    populateTypes();
                    if (orderId == null) {
                        for (EligibleOrderVisit visit : visits) {
                            visitCombo.addItem(visit);
                        }
                        showSelectedVisit();
                    } else {
                        showLoadedVisit();
                    }
                    refreshServices();
                    if (services.isEmpty()) {
                        setMessage("Danh mục dịch vụ hiện chưa có dữ liệu.", true);
                        return;
                    }
                    if (orderId == null && visits.isEmpty()) {
                        setMessage("Không có lượt khám đang hoạt động, có phiếu khám hợp lệ "
                                + "và thuộc phạm vi của bạn.", true);
                        return;
                    }
                    if (orderId != null && !isEditable(loadedDetail)) {
                        setMessage("Chỉ định không còn đủ điều kiện để chỉnh sửa.", true);
                        return;
                    }
                    if (orderId != null) {
                        noteArea.setText(loadedDetail.ghiChu());
                        selectService(loadedDetail.maDichVu());
                    }
                    allowSave = true;
                    setControlsEnabled(true);
                    setMessage(orderId == null
                            ? "Giá hiển thị chỉ tham khảo; schema không lưu giá tại thời điểm chỉ định."
                            : "Chỉ được đổi dịch vụ/ghi chú khi đang chờ thực hiện và chưa tính phí.",
                            false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải dữ liệu chỉ định bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải dữ liệu chỉ định.", exception.getCause());
                }
            }
        }.execute();
    }

    private void populateTypes() {
        List<String> types = services.stream()
                .map(ServiceCatalogItem::loaiDichVu)
                .filter(type -> type != null && !type.isBlank())
                .distinct()
                .sorted()
                .toList();
        typeCombo.addItem("Tất cả loại");
        for (String type : types) {
            typeCombo.addItem(type);
        }
    }

    private void refreshServices() {
        if (loading && services.isEmpty()) {
            return;
        }
        ServiceCatalogItem previous = (ServiceCatalogItem) serviceCombo.getSelectedItem();
        String oldServiceId = previous == null ? "" : previous.maDichVu();
        Object selectedType = typeCombo.getSelectedItem();
        String type = selectedType == null || "Tất cả loại".equals(selectedType)
                ? "" : selectedType.toString();
        String query = serviceSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
        serviceCombo.removeAllItems();
        for (ServiceCatalogItem service : services) {
            boolean isCurrentlySelectedInactive = orderId != null
                    && loadedDetail != null
                    && service.maDichVu().equals(loadedDetail.maDichVu());
            if ((!service.dangApDung() && !isCurrentlySelectedInactive)
                    || (!type.isEmpty() && !type.equals(service.loaiDichVu()))) {
                continue;
            }
            String searchable = (service.maDichVu() + " " + service.tenDichVu() + " "
                    + (service.loaiDichVu() == null ? "" : service.loaiDichVu()))
                    .toLowerCase(java.util.Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) {
                continue;
            }
            serviceCombo.addItem(service);
            if (service.maDichVu().equals(oldServiceId)) {
                serviceCombo.setSelectedItem(service);
            }
        }
        showSelectedService();
    }

    private void selectService(String serviceId) {
        for (int i = 0; i < serviceCombo.getItemCount(); i++) {
            if (serviceCombo.getItemAt(i).maDichVu().equals(serviceId)) {
                serviceCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void showSelectedVisit() {
        EligibleOrderVisit visit = (EligibleOrderVisit) visitCombo.getSelectedItem();
        if (visit == null) {
            visitInfo.setText("Không có lượt khám được chọn.");
            return;
        }
        visitInfo.setText("BN " + visit.maBenhNhan() + " · " + visit.tenBenhNhan()
                + " · " + (visit.ngaySinh() == null ? "—"
                        : DateTimeFormatter.ofPattern("dd/MM/yyyy").format(visit.ngaySinh()))
                + " · " + safe(visit.gioiTinh())
                + " · Bác sĩ: " + visit.tenBacSi()
                + " · " + safe(visit.tenChuyenKhoa())
                + " · Lượt " + visit.maLuotKham()
                + " / Phiếu " + visit.maPhieuKham());
    }

    private void showLoadedVisit() {
        if (loadedDetail == null) {
            return;
        }
        visitInfo.setText("BN " + loadedDetail.maBenhNhan() + " · "
                + loadedDetail.tenBenhNhan() + " · Bác sĩ: " + loadedDetail.tenBacSi()
                + " · Lượt " + loadedDetail.maLuotKham()
                + " / Phiếu " + loadedDetail.maPhieuKham());
    }

    private void showSelectedService() {
        ServiceCatalogItem service = (ServiceCatalogItem) serviceCombo.getSelectedItem();
        if (service == null) {
            serviceInfo.setText("Không có dịch vụ phù hợp.");
            return;
        }
        String price = service.giaHienHanh() == null
                ? "Chưa có giá hiện hành"
                : service.giaHienHanh().toPlainString() + " đ (tham khảo)";
        serviceInfo.setText(price + " · " + safe(service.donViTinh())
                + " · " + safe(service.trangThai()));
    }

    private void save() {
        if (loading || saving || !allowSave) {
            return;
        }
        long visitId;
        if (orderId == null) {
            EligibleOrderVisit visit = (EligibleOrderVisit) visitCombo.getSelectedItem();
            if (visit == null) {
                setMessage("Hãy chọn lượt khám.", true);
                return;
            }
            visitId = visit.maLuotKham();
        } else {
            visitId = loadedDetail.maLuotKham();
        }
        ServiceCatalogItem service = (ServiceCatalogItem) serviceCombo.getSelectedItem();
        if (service == null || !service.dangApDung()) {
            setMessage("Chỉ được chọn dịch vụ đang áp dụng.", true);
            return;
        }
        ServiceOrderDraft draft = new ServiceOrderDraft(
                visitId, service.maDichVu(), noteArea.getText().trim());
        saving = true;
        setControlsEnabled(false);
        setMessage("Đang lưu chỉ định...", false);
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws ServiceOrderException {
                if (orderId == null) {
                    return serviceOrderBUS.create(session, draft);
                }
                serviceOrderBUS.update(session, orderId, draft);
                return orderId;
            }

            @Override
            protected void done() {
                try {
                    long savedId = get();
                    onSaved.accept(savedId);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Lưu chỉ định bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể lưu chỉ định.", exception.getCause());
                } finally {
                    saving = false;
                    if (isDisplayable()) {
                        setControlsEnabled(allowSave);
                    }
                }
            }
        }.execute();
    }

    private boolean isEditable(ServiceOrderDetail detail) {
        return detail != null && "ChoThucHien".equals(detail.trangThai())
                && !detail.daTinhPhi()
                && ("DangKham".equals(detail.trangThaiLuotKham())
                || "ChoKetQua".equals(detail.trangThaiLuotKham()))
                && (session.hasPermission("NHANSU_SUA")
                || session.getEmployeeId().equals(detail.maBacSi()));
    }

    private void setControlsEnabled(boolean enabled) {
        loading = !enabled && !saving;
        visitCombo.setEnabled(enabled && orderId == null && !visits.isEmpty());
        serviceSearch.setEnabled(enabled);
        typeCombo.setEnabled(enabled);
        serviceCombo.setEnabled(enabled);
        noteArea.setEditable(enabled);
        saveButton.setEnabled(enabled && allowSave);
        cancelButton.setEnabled(!saving);
    }

    private void setMessage(String value, boolean warning) {
        message.setText(value);
        message.setForeground(warning ? new Color(170, 110, 30) : MainFrame.MUTED);
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
        if (!(cause instanceof ServiceOrderException)) {
            LOGGER.log(Level.SEVERE, title, cause);
        }
        saving = false;
        loading = false;
        setMessage(detail, true);
        setControlsEnabled(allowSave);
        JOptionPane.showMessageDialog(this, detail, "Chỉ định dịch vụ",
                JOptionPane.ERROR_MESSAGE);
    }

    private void addRow(JPanel form, int row, String title, java.awt.Component component) {
        JLabel label = new JLabel(title);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(8, 0, 8, 12);
        form.add(label, labelConstraints);

        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1;
        field.gridy = row;
        field.weightx = 1;
        field.fill = GridBagConstraints.HORIZONTAL;
        field.anchor = GridBagConstraints.NORTHWEST;
        field.insets = new Insets(5, 0, 5, 0);
        if (component instanceof JScrollPane) {
            field.weighty = 1;
        }
        form.add(component, field);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private record DialogData(
            List<EligibleOrderVisit> visits,
            List<ServiceCatalogItem> services,
            ServiceOrderDetail detail) {
    }
}
