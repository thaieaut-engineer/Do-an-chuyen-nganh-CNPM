package view;

import bus.prescription.PrescriptionException;
import bus.prescription.DonThuocBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.LongConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import model.auth.AuthenticatedSession;
import model.prescription.EligiblePrescriptionVisit;
import model.prescription.MedicationCatalogItem;
import model.prescription.PrescriptionDetail;
import model.prescription.PrescriptionDraft;
import model.prescription.PrescriptionLineDetail;
import model.prescription.PrescriptionLineDraft;

final class PrescriptionDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(PrescriptionDialog.class.getName());

    private final AuthenticatedSession session;
    private final DonThuocBUS prescriptionBUS;
    private final Long prescriptionId;
    private final LongConsumer onSaved;
    private final JComboBox<EligiblePrescriptionVisit> visitCombo = new JComboBox<>();
    private final JComboBox<MedicationCatalogItem> medicationCombo = new JComboBox<>();
    private final JTextField medicationSearch = new JTextField();
    private final JTextField doseField = new JTextField();
    private final JTextField routeField = new JTextField();
    private final JTextField frequencyField = new JTextField();
    private final JTextField durationField = new JTextField();
    private final JTextField quantityField = new JTextField("1");
    private final JTextField unitField = new JTextField();
    private final JTextArea instructionsField = new JTextArea(3, 24);
    private final JTextArea directionsField = new JTextArea(2, 30);
    private final JLabel visitInfo = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final JButton addLineButton = new JButton("Thêm thuốc");
    private final JButton newLineButton = new JButton("Dòng mới");
    private final JButton removeLineButton = new JButton("Bỏ dòng thuốc");
    private final JButton saveButton = new JButton("Lưu đơn thuốc");
    private final JButton cancelButton = new JButton("Đóng");
    private final DefaultTableModel lineModel = new DefaultTableModel(
            new Object[]{"Mã thuốc", "Tên thuốc", "Liều dùng", "Đường dùng",
                "Tần suất", "Thời gian", "Số lượng", "Đơn vị", "Hướng dẫn"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable lineTable = new JTable(lineModel);
    private List<EligiblePrescriptionVisit> eligibleVisits = List.of();
    private List<MedicationCatalogItem> medications = List.of();
    private final List<LineRow> lines = new ArrayList<>();
    private PrescriptionDetail loadedDetail;
    private boolean loading = true;
    private boolean saving;
    private boolean allowSave;
    private int editingRow = -1;

    PrescriptionDialog(
            java.awt.Window owner,
            AuthenticatedSession session,
            DonThuocBUS prescriptionBUS,
            Long prescriptionId,
            LongConsumer onSaved) {
        super(owner,
                prescriptionId == null ? "Tạo đơn thuốc" : "Chỉnh sửa đơn thuốc",
                ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.prescriptionBUS = prescriptionBUS;
        this.prescriptionId = prescriptionId;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        Dimension available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds().getSize();
        int dialogWidth = Math.max(700, Math.min(1120, available.width - 24));
        int dialogHeight = Math.max(420, Math.min(740, available.height - 16));
        setMinimumSize(new Dimension(
                Math.min(780, dialogWidth), Math.min(460, dialogHeight)));
        setSize(dialogWidth, dialogHeight);
        setLocationRelativeTo(owner);
        configureEvents();
        loadData();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(15, 18, 14, 18));

        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        if (prescriptionId == null) {
            addRow(header, 0, "Lượt khám *", visitCombo);
        } else {
            addRow(header, 0, "Mã đơn thuốc", new JLabel(prescriptionId.toString()));
        }
        visitInfo.setForeground(MainFrame.MUTED);
        addRow(header, 1, "Bệnh nhân / bác sĩ", visitInfo);
        directionsField.setLineWrap(true);
        directionsField.setWrapStyleWord(true);
        directionsField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        directionsField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        JScrollPane directionsScroll = new JScrollPane(directionsField);
        directionsScroll.setPreferredSize(new Dimension(0, 52));
        addRow(header, 2, "Lời dặn", directionsScroll);

        JPanel medicationForm = createMedicationForm();
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(medicationForm);
        center.add(Box.createVerticalStrut(10));
        configureLineTable();
        JScrollPane tableScroll = new JScrollPane(lineTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(MainFrame.BORDER));
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 260));
        tableScroll.setMinimumSize(new Dimension(100, 120));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        center.add(tableScroll);

        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.add(header, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(message, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        cancelButton.addActionListener(event -> dispose());
        saveButton.setForeground(Color.WHITE);
        saveButton.setBackground(MainFrame.BLUE);
        saveButton.setOpaque(true);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(event -> save());
        actions.add(cancelButton);
        actions.add(saveButton);
        footer.add(actions, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
        setControlsEnabled(false);
        return root;
    }

    private JPanel createMedicationForm() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(0xF9FBFC));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        medicationSearch.setPreferredSize(new Dimension(150, 34));
        medicationCombo.setPreferredSize(new Dimension(240, 34));
        doseField.setPreferredSize(new Dimension(130, 34));
        routeField.setPreferredSize(new Dimension(120, 34));
        frequencyField.setPreferredSize(new Dimension(135, 34));
        durationField.setPreferredSize(new Dimension(115, 34));
        quantityField.setPreferredSize(new Dimension(90, 34));
        unitField.setPreferredSize(new Dimension(90, 34));
        instructionsField.setLineWrap(true);
        instructionsField.setWrapStyleWord(true);
        instructionsField.setRows(2);
        instructionsField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instructionsField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(5, 6, 5, 6)));

        JScrollPane instructionScroll = new JScrollPane(instructionsField);
        instructionScroll.setPreferredSize(new Dimension(320, 52));

        addLineButton.addActionListener(event -> addOrUpdateLine());
        newLineButton.addActionListener(event -> clearLineEditor());
        removeLineButton.addActionListener(event -> removeSelectedLine());

        JPanel medicineRow = createFieldRow(
                new String[]{"Tìm thuốc", "Thuốc *", "Liều *"},
                new JComponent[]{medicationSearch, medicationCombo, doseField},
                new double[]{0.24, 0.48, 0.28});
        JPanel usageRow = createFieldRow(
                new String[]{"Đường dùng", "Tần suất", "Thời gian", "Số lượng *", "Đơn vị"},
                new JComponent[]{routeField, frequencyField, durationField,
                    quantityField, unitField},
                new double[]{0.22, 0.24, 0.21, 0.15, 0.18});
        JPanel instructionGroup = createFieldGroup("Hướng dẫn", instructionScroll);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(addLineButton);
        actions.add(newLineButton);
        actions.add(removeLineButton);
        JPanel bottomRow = new JPanel(new BorderLayout(12, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(instructionGroup, BorderLayout.CENTER);
        bottomRow.add(actions, BorderLayout.EAST);

        card.add(medicineRow);
        card.add(Box.createVerticalStrut(9));
        card.add(usageRow);
        card.add(Box.createVerticalStrut(9));
        card.add(bottomRow);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private JPanel createFieldRow(
            String[] labels, JComponent[] fields, double[] columnWeights) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        for (int i = 0; i < fields.length; i++) {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i;
            constraints.gridy = 0;
            constraints.weightx = columnWeights[i];
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, i == 0 ? 0 : 8, 0, 0);
            row.add(createFieldGroup(labels[i], fields[i]), constraints);
        }
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private JPanel createFieldGroup(String label, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 5));
        group.setOpaque(false);
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(MainFrame.MUTED);
        fieldLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        group.add(fieldLabel, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void configureLineTable() {
        lineTable.setRowHeight(34);
        lineTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lineTable.setFillsViewportHeight(true);
        lineTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lineTable.setGridColor(MainFrame.BORDER);
        lineTable.setShowVerticalLines(false);
        lineTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTableHeader header = lineTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 10));
        header.setPreferredSize(new Dimension(0, 34));
        header.setForeground(MainFrame.MUTED);
        header.setBackground(new Color(0xF5F7FA));
        header.setReorderingAllowed(false);
        int[] widths = {90, 150, 125, 90, 150, 105, 80, 75, 240};
        for (int i = 0; i < widths.length; i++) {
            lineTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        lineTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedLine();
            }
        });
    }

    private void configureEvents() {
        visitCombo.addActionListener(event -> showSelectedVisit());
        medicationSearch.addActionListener(event -> loadMedications());
    }

    private void loadData() {
        setMessage(prescriptionId == null
                ? "Đang tải lượt khám và danh mục thuốc..."
                : "Đang tải đơn thuốc và danh mục thuốc...", false);
        new SwingWorker<DialogData, Void>() {
            @Override
            protected DialogData doInBackground() throws PrescriptionException {
                List<EligiblePrescriptionVisit> visits = prescriptionId == null
                        ? prescriptionBUS.getEligibleVisits(session) : List.of();
                List<MedicationCatalogItem> medicineList =
                        prescriptionBUS.getMedications(session, "");
                PrescriptionDetail detail = prescriptionId == null
                        ? null : prescriptionBUS.getById(session, prescriptionId);
                return new DialogData(visits, medicineList, detail);
            }

            @Override
            protected void done() {
                try {
                    DialogData data = get();
                    eligibleVisits = data.visits();
                    medications = data.medications();
                    loadedDetail = data.detail();
                    populateMedicationChoices();
                    if (prescriptionId == null) {
                        for (EligiblePrescriptionVisit visit : eligibleVisits) {
                            visitCombo.addItem(visit);
                        }
                        showSelectedVisit();
                    } else {
                        showLoadedVisit();
                    }
                    if (prescriptionId != null && !prescriptionBUS.canEdit(session, loadedDetail)) {
                        setMessage("Đơn thuốc không còn đủ điều kiện chỉnh sửa.", true);
                        return;
                    }
                    if (prescriptionId == null && eligibleVisits.isEmpty()) {
                        setMessage("Không có lượt khám đang khám/chờ kết quả trong phạm vi của bạn.",
                                true);
                        return;
                    }
                    if (medications.stream().noneMatch(MedicationCatalogItem::dangSuDung)) {
                        setMessage("Danh mục không có thuốc đang sử dụng.", true);
                        return;
                    }
                    if (loadedDetail != null) {
                        directionsField.setText(loadedDetail.loiDan());
                        for (PrescriptionLineDetail line : loadedDetail.lines()) {
                            LineRow row = fromDetail(line);
                            lines.add(row);
                            appendLineToTable(row);
                        }
                    }
                    loading = false;
                    allowSave = true;
                    setControlsEnabled(true);
                    setMessage(prescriptionId == null
                            ? "Kê đơn chỉ tạo đơn và chi tiết; không trừ tồn kho."
                            : "Đơn nháp chưa xuất mới có thể sửa. Các dòng thuốc đã lưu không bị xóa.",
                            false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải dữ liệu đơn thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải dữ liệu đơn thuốc.", exception.getCause());
                }
            }
        }.execute();
    }

    private void loadMedications() {
        if (loading || saving) {
            return;
        }
        setControlsEnabled(false);
        String query = medicationSearch.getText().trim();
        new SwingWorker<List<MedicationCatalogItem>, Void>() {
            @Override
            protected List<MedicationCatalogItem> doInBackground()
                    throws PrescriptionException {
                return prescriptionBUS.getMedications(session, query);
            }

            @Override
            protected void done() {
                try {
                    medications = get();
                    populateMedicationChoices();
                    setMessage(medicationCombo.getItemCount() == 0
                            ? "Không tìm thấy thuốc đang sử dụng."
                            : "Chọn thuốc và nhập liều dùng, số lượng theo đơn vị danh mục.",
                            medicationCombo.getItemCount() == 0);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Tải danh mục thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tải danh mục thuốc.", exception.getCause());
                } finally {
                    loading = false;
                    setControlsEnabled(allowSave);
                }
            }
        }.execute();
    }

    private void populateMedicationChoices() {
        MedicationCatalogItem selected = (MedicationCatalogItem) medicationCombo.getSelectedItem();
        String selectedId = selected == null ? "" : selected.maThuoc();
        medicationCombo.removeAllItems();
        for (MedicationCatalogItem medication : medications) {
            if (medication.dangSuDung()) {
                medicationCombo.addItem(medication);
                if (medication.maThuoc().equals(selectedId)) {
                    medicationCombo.setSelectedItem(medication);
                }
            }
        }
    }

    private void addOrUpdateLine() {
        if (loading || saving || !allowSave) {
            return;
        }
        MedicationCatalogItem medication =
                (MedicationCatalogItem) medicationCombo.getSelectedItem();
        if (medication == null || !medication.dangSuDung()) {
            setMessage("Chọn thuốc đang sử dụng từ danh mục.", true);
            return;
        }
        String dose = doseField.getText().trim();
        if (dose.isEmpty() || dose.length() > 150) {
            setMessage("Liều dùng là bắt buộc (tối đa 150 ký tự).", true);
            return;
        }
        BigDecimal quantity;
        try {
            quantity = new BigDecimal(quantityField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            setMessage("Số lượng phải là số hợp lệ.", true);
            return;
        }
        quantity = quantity.stripTrailingZeros();
        if (quantity.signum() <= 0 || quantity.scale() > 2
                || quantity.compareTo(new BigDecimal("9999999999.99")) > 0) {
            setMessage("Số lượng phải lớn hơn 0 và tối đa 2 chữ số thập phân.", true);
            return;
        }
        LineRow updated = new LineRow(
                editingRow < 0 ? null : lines.get(editingRow).maChiTietDon(),
                medication.maThuoc(),
                medication.tenThuoc(),
                medication.hamLuong(),
                dose,
                routeField.getText().isBlank()
                        ? safeEmpty(medication.duongDungMacDinh())
                        : routeField.getText().trim(),
                frequencyField.getText().trim(),
                durationField.getText().trim(),
                quantity,
                unitField.getText().isBlank()
                        ? safeEmpty(medication.donViTinh())
                        : unitField.getText().trim(),
                instructionsField.getText().trim());
        if (!textLengthsValid(updated)) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            if (i != editingRow && lines.get(i).maThuoc().equals(updated.maThuoc())) {
                setMessage("Thuốc này đã có trong đơn. Chọn dòng hiện có để cập nhật.",
                        true);
                return;
            }
        }
        if (editingRow < 0) {
            lines.add(updated);
        } else {
            lines.set(editingRow, updated);
        }
        rebuildLineTable();
        clearLineEditor();
        setMessage("Đã cập nhật danh sách thuốc trong biểu mẫu.", false);
    }

    private boolean textLengthsValid(LineRow row) {
        if (row.duongDung().length() > 80 || row.tanSuat().length() > 100
                || row.thoiGianDung().length() > 100 || row.donViTinh().length() > 30
                || row.huongDan().length() > 100_000) {
            setMessage("Một trường thông tin thuốc vượt quá độ dài cho phép.", true);
            return false;
        }
        return true;
    }

    private void removeSelectedLine() {
        int selected = lineTable.getSelectedRow();
        if (selected < 0 || selected >= lines.size()) {
            return;
        }
        LineRow row = lines.get(selected);
        if (row.maChiTietDon() != null) {
            setMessage(
                    "Không xóa dòng thuốc đã lưu theo chính sách bảo toàn dữ liệu. "
                    + "Có thể chỉnh nội dung dòng hoặc thêm thuốc khác.",
                    true);
            return;
        }
        lines.remove(selected);
        rebuildLineTable();
        clearLineEditor();
        setMessage("Đã bỏ dòng thuốc chưa lưu.", false);
    }

    private void loadSelectedLine() {
        int selected = lineTable.getSelectedRow();
        if (selected < 0 || selected >= lines.size()) {
            return;
        }
        editingRow = selected;
        LineRow row = lines.get(selected);
        selectMedication(row.maThuoc());
        doseField.setText(row.lieuDung());
        routeField.setText(row.duongDung());
        frequencyField.setText(row.tanSuat());
        durationField.setText(row.thoiGianDung());
        quantityField.setText(row.soLuong().stripTrailingZeros().toPlainString());
        unitField.setText(row.donViTinh());
        instructionsField.setText(row.huongDan());
        addLineButton.setText("Cập nhật dòng");
    }

    private void selectMedication(String medicationId) {
        for (int i = 0; i < medicationCombo.getItemCount(); i++) {
            if (medicationCombo.getItemAt(i).maThuoc().equals(medicationId)) {
                medicationCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearLineEditor() {
        editingRow = -1;
        lineTable.clearSelection();
        doseField.setText("");
        routeField.setText("");
        frequencyField.setText("");
        durationField.setText("");
        quantityField.setText("1");
        unitField.setText("");
        instructionsField.setText("");
        addLineButton.setText("Thêm thuốc");
    }

    private void rebuildLineTable() {
        lineModel.setRowCount(0);
        for (LineRow row : lines) {
            appendLineToTable(row);
        }
    }

    private void appendLineToTable(LineRow row) {
        lineModel.addRow(new Object[]{
                row.maThuoc(),
                row.tenThuoc(),
                row.lieuDung(),
                safe(row.duongDung()),
                safe(row.tanSuat()),
                safe(row.thoiGianDung()),
                row.soLuong(),
                safe(row.donViTinh()),
                safe(row.huongDan())
        });
    }

    private LineRow fromDetail(PrescriptionLineDetail detail) {
        return new LineRow(
                detail.maChiTietDon(),
                detail.maThuoc(),
                detail.tenThuoc(),
                detail.hamLuong(),
                detail.lieuDung(),
                safeEmpty(detail.duongDung()),
                safeEmpty(detail.tanSuat()),
                safeEmpty(detail.thoiGianDung()),
                detail.soLuong(),
                safeEmpty(detail.donViTinh()),
                safeEmpty(detail.huongDan()));
    }

    private void showSelectedVisit() {
        EligiblePrescriptionVisit visit =
                (EligiblePrescriptionVisit) visitCombo.getSelectedItem();
        if (visit == null) {
            visitInfo.setText("Không có lượt khám phù hợp.");
            return;
        }
        visitInfo.setText("BN " + visit.maBenhNhan() + " · " + visit.tenBenhNhan()
                + " · " + date(visit.ngaySinh()) + " · " + safe(visit.gioiTinh())
                + " · BS: " + visit.tenBacSi() + " · " + safe(visit.tenChuyenKhoa())
                + " · Lượt " + visit.maLuotKham()
                + (visit.maPhieuKham() == null ? " · Chưa có phiếu khám"
                        : " · Phiếu " + visit.maPhieuKham()
                                + " (" + safe(visit.trangThaiPhieuKham()) + ")"));
    }

    private void showLoadedVisit() {
        if (loadedDetail == null) {
            return;
        }
        visitInfo.setText("BN " + loadedDetail.maBenhNhan() + " · "
                + loadedDetail.tenBenhNhan() + " · BS: " + loadedDetail.tenBacSi()
                + " · Lượt " + loadedDetail.maLuotKham());
    }

    private void save() {
        if (loading || saving || !allowSave) {
            return;
        }
        if (editingRow >= 0) {
            setMessage("Hãy cập nhật hoặc bỏ thay đổi của dòng đang chọn trước khi lưu.", true);
            return;
        }
        long visitId;
        if (prescriptionId == null) {
            EligiblePrescriptionVisit selected =
                    (EligiblePrescriptionVisit) visitCombo.getSelectedItem();
            if (selected == null) {
                setMessage("Hãy chọn lượt khám.", true);
                return;
            }
            visitId = selected.maLuotKham();
        } else {
            visitId = loadedDetail.maLuotKham();
        }
        List<PrescriptionLineDraft> drafts = lines.stream()
                .map(line -> new PrescriptionLineDraft(
                        line.maChiTietDon(),
                        line.maThuoc(),
                        line.lieuDung(),
                        emptyToNull(line.duongDung()),
                        emptyToNull(line.tanSuat()),
                        emptyToNull(line.thoiGianDung()),
                        line.soLuong(),
                        emptyToNull(line.donViTinh()),
                        emptyToNull(line.huongDan())))
                .toList();
        PrescriptionDraft draft = new PrescriptionDraft(
                visitId, directionsField.getText().trim(), drafts);
        saving = true;
        setControlsEnabled(false);
        setMessage("Đang lưu đơn thuốc...", false);
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws PrescriptionException {
                if (prescriptionId == null) {
                    return prescriptionBUS.create(session, draft);
                }
                prescriptionBUS.update(session, prescriptionId, draft);
                return prescriptionId;
            }

            @Override
            protected void done() {
                try {
                    long savedId = get();
                    onSaved.accept(savedId);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Lưu đơn thuốc bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể lưu đơn thuốc.", exception.getCause());
                } finally {
                    saving = false;
                    if (isDisplayable()) {
                        setControlsEnabled(allowSave);
                    }
                }
            }
        }.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        loading = !enabled && !saving;
        visitCombo.setEnabled(enabled && prescriptionId == null
                && !eligibleVisits.isEmpty());
        directionsField.setEditable(enabled);
        medicationSearch.setEnabled(enabled);
        medicationCombo.setEnabled(enabled);
        doseField.setEnabled(enabled);
        routeField.setEnabled(enabled);
        frequencyField.setEnabled(enabled);
        durationField.setEnabled(enabled);
        quantityField.setEnabled(enabled);
        unitField.setEnabled(enabled);
        instructionsField.setEnabled(enabled);
        addLineButton.setEnabled(enabled);
        newLineButton.setEnabled(enabled);
        removeLineButton.setEnabled(enabled);
        lineTable.setEnabled(enabled);
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
                && !(source instanceof PrescriptionException)) {
            source = source.getCause();
        }
        String detail = cause instanceof PrescriptionException
                ? cause.getMessage()
                : source != null && source.getMessage() != null
                        ? source.getMessage() : title;
        if (!(cause instanceof PrescriptionException)) {
            LOGGER.log(Level.SEVERE, title, cause);
        }
        saving = false;
        loading = false;
        setMessage(detail, true);
        setControlsEnabled(allowSave);
        JOptionPane.showMessageDialog(
                this, detail, "Kê đơn thuốc", JOptionPane.ERROR_MESSAGE);
    }

    private void addRow(JPanel form, int row, String title, java.awt.Component component) {
        JLabel label = new JLabel(title);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(5, 0, 5, 12);
        form.add(label, labelConstraints);

        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1;
        field.gridy = row;
        field.weightx = 1;
        field.fill = GridBagConstraints.HORIZONTAL;
        field.anchor = GridBagConstraints.NORTHWEST;
        field.insets = new Insets(4, 0, 4, 0);
        form.add(component, field);
    }

    private String date(java.time.LocalDate value) {
        return value == null ? "—" : DateTimeFormatter.ofPattern("dd/MM/yyyy").format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String safeEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record DialogData(
            List<EligiblePrescriptionVisit> visits,
            List<MedicationCatalogItem> medications,
            PrescriptionDetail detail) {
    }

    private record LineRow(
            Long maChiTietDon,
            String maThuoc,
            String tenThuoc,
            String hamLuong,
            String lieuDung,
            String duongDung,
            String tanSuat,
            String thoiGianDung,
            BigDecimal soLuong,
            String donViTinh,
            String huongDan) {
    }
}
