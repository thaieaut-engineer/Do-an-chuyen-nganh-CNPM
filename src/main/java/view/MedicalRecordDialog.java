package view;

import bus.medicalrecord.MedicalRecordException;
import bus.medicalrecord.PhieuKhamBUS;
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
import model.medicalrecord.EligibleVisit;
import model.medicalrecord.MedicalRecordDetail;
import model.medicalrecord.MedicalRecordDraft;

final class MedicalRecordDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(MedicalRecordDialog.class.getName());
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuthenticatedSession session;
    private final PhieuKhamBUS recordBUS;
    private final Long recordId;
    private final LongConsumer onSaved;
    private final JComboBox<EligibleVisit> visitSelector = new JComboBox<>();
    private final JLabel visitInfo = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final JTextArea reasonField = createTextArea(3);
    private final JTextArea symptomsField = createTextArea(4);
    private final JTextArea vitalsField = createTextArea(3);
    private final JTextArea clinicalField = createTextArea(4);
    private final JTextArea diagnosisField = createTextArea(4);
    private final JTextArea treatmentField = createTextArea(4);
    private final JButton saveButton = new JButton("Lưu phiếu khám");
    private final JButton cancelButton = new JButton("Hủy");
    private boolean loading = true;
    private boolean saving;
    private boolean allowSave;
    private boolean fieldsEditable;

    MedicalRecordDialog(
            java.awt.Window owner,
            AuthenticatedSession session,
            PhieuKhamBUS recordBUS,
            Long recordId,
            LongConsumer onSaved) {
        super(owner,
                recordId == null ? "Tạo phiếu khám" : "Cập nhật phiếu khám",
                ModalityType.APPLICATION_MODAL);
        this.session = session;
        this.recordBUS = recordBUS;
        this.recordId = recordId;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(690, 650));
        setSize(760, 760);
        setLocationRelativeTo(owner);
        configureEvents();
        loadData();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 22, 16, 22));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int row = 0;
        if (recordId == null) {
            addRow(fields, row++, "Lượt khám *", visitSelector);
        } else {
            addRow(fields, row++, "Mã phiếu khám", readOnlyField(Long.toString(recordId)));
        }
        visitInfo.setForeground(MainFrame.MUTED);
        visitInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRow(fields, row++, "Bệnh nhân / bác sĩ", visitInfo);
        addRow(fields, row++, "Lý do khám", new JScrollPane(reasonField));
        addRow(fields, row++, "Triệu chứng *", new JScrollPane(symptomsField));
        addRow(fields, row++, "Dấu hiệu sinh tồn", new JScrollPane(vitalsField));
        addRow(fields, row++, "Kết quả lâm sàng *", new JScrollPane(clinicalField));
        addRow(fields, row++, "Chẩn đoán *", new JScrollPane(diagnosisField));
        addRow(fields, row, "Hướng điều trị", new JScrollPane(treatmentField));
        JScrollPane formScroll = new JScrollPane(fields);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(formScroll, BorderLayout.CENTER);

        message.setForeground(MainFrame.MUTED);
        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.add(message, BorderLayout.CENTER);
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
        footer.add(buttons, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
        setFieldsEditable(false);
        setSaveEnabled(false);
        return root;
    }

    private void configureEvents() {
        visitSelector.addActionListener(event -> {
            if (!loading) {
                showSelectedVisit();
                updateSaveState();
            }
        });
    }

    private void loadData() {
        setBusy(true, recordId == null
                ? "Đang tải lượt khám đủ điều kiện..." : "Đang tải phiếu khám...");
        if (recordId == null) {
            new SwingWorker<List<EligibleVisit>, Void>() {
                @Override
                protected List<EligibleVisit> doInBackground() throws MedicalRecordException {
                    return recordBUS.getEligibleVisits(session);
                }

                @Override
                protected void done() {
                    try {
                        List<EligibleVisit> visits = get();
                        loading = true;
                        for (EligibleVisit visit : visits) {
                            visitSelector.addItem(visit);
                        }
                        loading = false;
                        showSelectedVisit();
                        if (visits.isEmpty()) {
                            setBusy(false,
                                    "Không có lượt khám đang khám/chờ kết quả chưa lập phiếu.");
                            message.setForeground(new Color(170, 110, 30));
                        } else {
                            allowSave = true;
                            setFieldsEditable(true);
                            setBusy(false, "Phiếu khám sẽ liên kết với bác sĩ phụ trách lượt khám.");
                        }
                        updateSaveState();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        showError("Tải lượt khám bị gián đoạn.", exception);
                    } catch (ExecutionException exception) {
                        showError("Không thể tải lượt khám đủ điều kiện.", exception.getCause());
                    }
                }
            }.execute();
        } else {
            new SwingWorker<MedicalRecordDetail, Void>() {
                @Override
                protected MedicalRecordDetail doInBackground() throws MedicalRecordException {
                    return recordBUS.getById(session, recordId);
                }

                @Override
                protected void done() {
                    try {
                        MedicalRecordDetail detail = get();
                        loadedDetail = detail;
                        populate(detail);
                        boolean editable = recordBUS.canEdit(session, detail);
                        setFieldsEditable(editable);
                        if (editable) {
                            setBusy(false, "Đang chỉnh sửa phiếu nháp.");
                        } else {
                            setBusy(false,
                                    "Phiếu đã ký/hoàn tất/khóa hoặc lượt khám không còn cho phép chỉnh sửa.");
                            message.setForeground(new Color(170, 110, 30));
                        }
                        allowSave = editable;
                        updateSaveState();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        showError("Tải phiếu khám bị gián đoạn.", exception);
                    } catch (ExecutionException exception) {
                        showError("Không thể tải phiếu khám.", exception.getCause());
                    }
                }
            }.execute();
        }
    }

    private void populate(MedicalRecordDetail detail) {
        visitInfo.setText("Lượt " + detail.maLuotKham()
                + " · " + detail.maBenhNhan() + " — " + detail.tenBenhNhan()
                + " · Bác sĩ: " + detail.tenBacSi()
                + " · " + safe(detail.tenChuyenKhoa())
                + " · Tiếp nhận: " + format(detail.thoiGianTiepNhan())
                + " · Trạng thái phiếu: " + statusLabel(detail.trangThaiPhieu()));
        reasonField.setText(detail.lyDoKham());
        symptomsField.setText(detail.trieuChung());
        vitalsField.setText(detail.dauHieuSinhTon());
        clinicalField.setText(detail.ketQuaLamSang());
        diagnosisField.setText(detail.chanDoan());
        treatmentField.setText(detail.huongDieuTri());
    }

    private void showSelectedVisit() {
        EligibleVisit visit = (EligibleVisit) visitSelector.getSelectedItem();
        if (visit == null) {
            visitInfo.setText("Không có lượt khám đủ điều kiện.");
            return;
        }
        visitInfo.setText(visit.maBenhNhan() + " — " + visit.tenBenhNhan()
                + " · Bác sĩ: " + visit.tenBacSi()
                + " · " + safe(visit.tenChuyenKhoa())
                + " · Tiếp nhận: " + format(visit.thoiGianTiepNhan()));
    }

    private void save() {
        if (saving || loading || !allowSave) {
            return;
        }
        long visitId;
        if (recordId == null) {
            EligibleVisit selected = (EligibleVisit) visitSelector.getSelectedItem();
            if (selected == null) {
                setBusy(false, "Hãy chọn lượt khám đủ điều kiện.");
                return;
            }
            visitId = selected.maLuotKham();
        } else {
            saveExistingRecord();
            return;
        }
        MedicalRecordDraft draft = currentDraft(visitId);
        saving = true;
        setBusy(true, "Đang lưu phiếu khám...");
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws MedicalRecordException {
                return recordBUS.create(session, draft);
            }

            @Override
            protected void done() {
                try {
                    long savedId = get();
                    onSaved.accept(savedId);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Lưu phiếu khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể tạo phiếu khám.", exception.getCause());
                } finally {
                    saving = false;
                    if (isDisplayable()) {
                        setFieldsEditable(allowSave);
                        updateSaveState();
                    }
                }
            }
        }.execute();
    }

    private void saveExistingRecord() {
        MedicalRecordDetail detail = loadedDetail;
        if (detail == null) {
            setBusy(false, "Chưa tải được thông tin lượt khám liên kết.");
            return;
        }
        MedicalRecordDraft draft = currentDraft(detail.maLuotKham());
        saving = true;
        setBusy(true, "Đang cập nhật phiếu khám...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws MedicalRecordException {
                recordBUS.update(session, recordId, draft);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    onSaved.accept(recordId);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Cập nhật phiếu khám bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    showError("Không thể cập nhật phiếu khám.", exception.getCause());
                } finally {
                    saving = false;
                    if (isDisplayable()) {
                        setFieldsEditable(allowSave);
                        updateSaveState();
                    }
                }
            }
        }.execute();
    }

    private MedicalRecordDetail loadedDetail;

    private MedicalRecordDraft currentDraft(long visitId) {
        return new MedicalRecordDraft(
                visitId,
                reasonField.getText().trim(),
                symptomsField.getText().trim(),
                vitalsField.getText().trim(),
                clinicalField.getText().trim(),
                diagnosisField.getText().trim(),
                treatmentField.getText().trim());
    }

    private void setFieldsEditable(boolean editable) {
        fieldsEditable = editable;
        boolean enabled = editable && !loading && !saving;
        reasonField.setEditable(enabled);
        symptomsField.setEditable(enabled);
        vitalsField.setEditable(enabled);
        clinicalField.setEditable(enabled);
        diagnosisField.setEditable(enabled);
        treatmentField.setEditable(enabled);
    }

    private void addRow(JPanel form, int row, String title, java.awt.Component field) {
        JLabel label = new JLabel(title);
        label.setForeground(MainFrame.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(8, 0, 8, 12);
        form.add(label, labelConstraints);

        if (field instanceof JComboBox<?> comboBox) {
            comboBox.setPreferredSize(new Dimension(0, 36));
        }
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.NORTHWEST;
        fieldConstraints.insets = new Insets(5, 0, 5, 0);
        if (field instanceof JScrollPane) {
            fieldConstraints.weighty = 1;
        }
        form.add(field, fieldConstraints);
    }

    private JTextArea createTextArea(int rows) {
        JTextArea area = new JTextArea(rows, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(7, 8, 7, 8)));
        return area;
    }

    private JTextField readOnlyField(String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setBackground(new Color(248, 250, 252));
        return field;
    }

    private void setBusy(boolean busy, String text) {
        this.loading = busy && text.startsWith("Đang tải");
        message.setText(text);
        if (text.startsWith("Đang tải") || text.startsWith("Đang lưu")
                || text.startsWith("Đang cập nhật")) {
            message.setForeground(MainFrame.MUTED);
        }
        if (recordId == null) {
            visitSelector.setEnabled(!busy && visitSelector.getItemCount() > 0);
        }
        setFieldsEditable(fieldsEditable);
        cancelButton.setEnabled(!busy);
        updateSaveState();
    }

    private void setSaveEnabled(boolean enabled) {
        saveButton.setEnabled(enabled && !saving && !loading);
    }

    private void updateSaveState() {
        boolean hasVisit = recordId != null || visitSelector.getSelectedItem() != null;
        setSaveEnabled(allowSave && hasVisit && !loading && !saving);
    }

    private void showError(String text, Throwable cause) {
        Throwable source = cause;
        while (source != null && source.getCause() != null
                && !(source instanceof MedicalRecordException)) {
            source = source.getCause();
        }
        String detail = cause instanceof MedicalRecordException
                ? cause.getMessage()
                : source != null && source.getMessage() != null ? source.getMessage() : text;
        message.setForeground(new Color(190, 62, 62));
        message.setText(detail);
        if (!(cause instanceof MedicalRecordException)) {
            LOGGER.log(Level.SEVERE, text, cause);
        }
        saving = false;
        loading = false;
        setFieldsEditable(allowSave);
        cancelButton.setEnabled(true);
        if (recordId == null) {
            visitSelector.setEnabled(visitSelector.getItemCount() > 0);
        }
        updateSaveState();
        JOptionPane.showMessageDialog(this, detail, "Phiếu khám", JOptionPane.ERROR_MESSAGE);
    }

    private String format(java.time.LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "Nhap" -> "Nháp";
            case "HoanTat" -> "Hoàn tất";
            case "DaKy" -> "Đã ký";
            case "DaKhoa" -> "Đã khóa";
            default -> status == null || status.isBlank() ? "—" : status;
        };
    }
}
