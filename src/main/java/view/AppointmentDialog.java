package view;

import bus.appointment.AppointmentException;
import bus.appointment.LichHenBUS;
import bus.patient.BenhNhanBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
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
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentDraft;
import model.appointment.AppointmentPatient;
import model.appointment.AppointmentSlot;
import model.appointment.AppointmentSpecialty;
import model.appointment.LichHen;
import model.auth.AuthenticatedSession;

final class AppointmentDialog extends JDialog {
    private static final Logger LOGGER =
            Logger.getLogger(AppointmentDialog.class.getName());
    private final AuthenticatedSession session;
    private final LichHenBUS appointmentBUS;
    private final BenhNhanBUS patientBUS;
    private final boolean canAddPatients;
    private final Long appointmentId;
    private final LongConsumer onSaved;
    private final JTextField patientSearch = new JTextField();
    private final JComboBox<AppointmentPatient> patientSelector = new JComboBox<>();
    private final JComboBox<AppointmentSpecialty> specialtySelector = new JComboBox<>();
    private final JComboBox<AppointmentDoctor> doctorSelector = new JComboBox<>();
    private final JSpinner appointmentDate = new JSpinner(new javax.swing.SpinnerDateModel());
    private final JComboBox<AppointmentSlot> slotSelector = new JComboBox<>();
    private final JTextArea reasonField = new JTextArea(3, 24);
    private final JTextArea noteField = new JTextArea(2, 24);
    private final JLabel message = new JLabel(" ");
    private final JButton searchPatientButton = new JButton("Tìm bệnh nhân");
    private final JButton addPatientButton = new JButton("+ Thêm bệnh nhân mới");
    private final JButton saveButton = new JButton("Xác nhận đặt lịch");
    private final JButton cancelButton = new JButton("Hủy");
    private boolean initializing = true;
    private boolean updatingSelectors;
    private boolean busy;
    private long slotRequestVersion;
    private long doctorRequestVersion;
    private List<AppointmentDoctor> knownDoctors = List.of();

    AppointmentDialog(
            Frame owner, AuthenticatedSession session, LichHenBUS appointmentBUS,
            BenhNhanBUS patientBUS, Long appointmentId, LongConsumer onSaved) {
        super(owner, appointmentId == null ? "Đặt lịch hẹn" : "Cập nhật lịch hẹn", true);
        this.session = session;
        this.appointmentBUS = appointmentBUS;
        this.patientBUS = patientBUS;
        this.canAddPatients = session.hasPermission("BN_XEM")
                && session.hasPermission("BN_SUA");
        this.appointmentId = appointmentId;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(560, 560));
        setSize(620, 620);
        setLocationRelativeTo(owner);
        appointmentDate.setEditor(new JSpinner.DateEditor(appointmentDate, "dd/MM/yyyy"));
        Date today = Date.from(LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        appointmentDate.setValue(today);
        ((SpinnerDateModel) appointmentDate.getModel()).setStart(today);
        configureEvents();
        initializeData();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 16, 24));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        if (appointmentId != null) {
            addRow(form, row++, "Mã lịch hẹn",
                    readOnlyField(Long.toString(appointmentId)));
        }
        JPanel patientPanel = new JPanel(new BorderLayout(6, 6));
        patientPanel.setOpaque(false);
        patientSearch.putClientProperty(
                "JTextField.placeholderText", "Mã bệnh nhân, họ tên hoặc số điện thoại");
        JPanel patientSearchRow = new JPanel(new BorderLayout(6, 0));
        patientSearchRow.setOpaque(false);
        patientSearchRow.add(patientSearch, BorderLayout.CENTER);
        patientSearchRow.add(searchPatientButton, BorderLayout.EAST);
        patientPanel.add(patientSearchRow, BorderLayout.NORTH);
        patientPanel.add(patientSelector, BorderLayout.CENTER);
        if (appointmentId == null) {
            JPanel patientActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
            patientActions.setOpaque(false);
            addPatientButton.setEnabled(canAddPatients);
            addPatientButton.setToolTipText(canAddPatients
                    ? "Tạo hồ sơ bệnh nhân mới"
                    : "Bạn cần quyền xem và sửa hồ sơ bệnh nhân để thêm mới.");
            addPatientButton.addActionListener(event -> openNewPatientDialog());
            patientActions.add(addPatientButton);
            patientPanel.add(patientActions, BorderLayout.SOUTH);
        }
        addRow(form, row++, "Bệnh nhân *", patientPanel);
        addRow(form, row++, "Chuyên khoa *", specialtySelector);
        addRow(form, row++, "Bác sĩ *", doctorSelector);
        addRow(form, row++, "Ngày khám *", appointmentDate);
        addRow(form, row++, "Khung giờ *", slotSelector);
        reasonField.setLineWrap(true);
        reasonField.setWrapStyleWord(true);
        reasonField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRow(form, row++, "Lý do khám", new JScrollPane(reasonField));
        noteField.setLineWrap(true);
        noteField.setWrapStyleWord(true);
        noteField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRow(form, row, "Ghi chú", new JScrollPane(noteField));
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(formScroll, BorderLayout.CENTER);

        message.setForeground(new Color(190, 62, 62));
        message.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel footer = new JPanel(new BorderLayout(8, 0));
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
        return root;
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

        if (field instanceof JTextField textField) {
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            textField.setPreferredSize(new Dimension(0, 35));
        } else if (field instanceof JComboBox<?> comboBox) {
            comboBox.setPreferredSize(new Dimension(0, 35));
        } else if (field instanceof JSpinner spinner) {
            spinner.setPreferredSize(new Dimension(0, 35));
        }
        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.NORTHWEST;
        fieldConstraints.insets = new Insets(5, 0, 5, 0);
        form.add(field, fieldConstraints);
    }

    private JTextField readOnlyField(String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setBackground(new Color(248, 250, 252));
        return field;
    }

    private void configureEvents() {
        appointmentDate.addChangeListener(event -> {
            if (!initializing && !updatingSelectors) {
                reloadDoctors();
            }
        });
        specialtySelector.addActionListener(event -> {
            if (!initializing && !updatingSelectors) {
                reloadDoctors();
            }
        });
        doctorSelector.addActionListener(event -> {
            if (!initializing && !updatingSelectors) {
                reloadSlots();
            }
        });
        patientSelector.addActionListener(event -> {
            if (!initializing && !updatingSelectors) {
                reloadSlots();
            }
        });
        searchPatientButton.addActionListener(event -> searchPatients());
        patientSearch.addActionListener(event -> searchPatients());
    }

    private void openNewPatientDialog() {
        if (!addPatientButton.isEnabled() || busy) {
            return;
        }
        new PatientDialog(
                (Frame) getOwner(),
                session,
                patientBUS,
                null,
                PatientDialog.Mode.CREATE,
                this::loadCreatedPatient).setVisible(true);
    }

    private void loadCreatedPatient(String patientId) {
        patientSearch.setText(patientId);
        updatingSelectors = true;
        patientSelector.removeAllItems();
        slotSelector.removeAllItems();
        updatingSelectors = false;
        searchPatientButton.setEnabled(false);
        setBusy(true, "Đang tải hồ sơ bệnh nhân mới...");
        new SwingWorker<List<AppointmentPatient>, Void>() {
            @Override
            protected List<AppointmentPatient> doInBackground()
                    throws AppointmentException {
                return appointmentBUS.searchPatients(session, patientId);
            }

            @Override
            protected void done() {
                try {
                    List<AppointmentPatient> patients = get();
                    updatingSelectors = true;
                    patientSelector.removeAllItems();
                    AppointmentPatient createdPatient = patients.stream()
                            .filter(patient -> patient.maBenhNhan().equals(patientId))
                            .findFirst()
                            .orElse(null);
                    if (createdPatient != null) {
                        patientSelector.addItem(createdPatient);
                        patientSelector.setSelectedItem(createdPatient);
                    }
                    updatingSelectors = false;
                    if (createdPatient == null) {
                        setOptionsLoading(false,
                                "Đã tạo hồ sơ nhưng không tìm thấy bệnh nhân để chọn.");
                        message.setForeground(new Color(170, 110, 30));
                    } else {
                        message.setForeground(MainFrame.MUTED);
                        message.setText("Đã chọn hồ sơ bệnh nhân mới.");
                        reloadSlots();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Tải hồ sơ bệnh nhân mới bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    fail("Không thể tải hồ sơ bệnh nhân mới.", exception.getCause());
                } finally {
                    searchPatientButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void initializeData() {
        setBusy(true, "Đang tải dữ liệu đặt lịch...");
        new SwingWorker<InitialData, Void>() {
            @Override
            protected InitialData doInBackground() throws AppointmentException {
                LocalDate defaultDate = appointmentBUS.getNearestScheduleDate(session);
                List<AppointmentSpecialty> specialties =
                        appointmentBUS.getSpecialties(session);
                List<AppointmentPatient> allPatients = appointmentBUS.searchPatients(session, "");
                List<AppointmentDoctor> allDoctors = appointmentBUS.getDoctors(session);
                knownDoctors = List.copyOf(allDoctors);
                LichHen appointment = appointmentId == null
                        ? null : appointmentBUS.getById(session, appointmentId);
                List<AppointmentPatient> patients = appointment == null
                        ? allPatients
                        : List.of(new AppointmentPatient(
                                appointment.maBenhNhan(),
                                appointment.tenBenhNhan(),
                                appointment.soDienThoaiBenhNhan()));
                return new InitialData(defaultDate, specialties, appointment, patients, allPatients, allDoctors);
            }

            @Override
            protected void done() {
                try {
                    InitialData data = get();
                    updatingSelectors = true;
                    Date initialDate = Date.from(data.databaseDate()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant());
                    appointmentDate.setValue(initialDate);
                    ((SpinnerDateModel) appointmentDate.getModel()).setStart(initialDate);
                    for (AppointmentSpecialty specialty : data.specialties()) {
                        specialtySelector.addItem(specialty);
                    }
                    for (AppointmentPatient patient : data.allPatients()) {
                        patientSelector.addItem(patient);
                    }
                    for (AppointmentDoctor doctor : data.allDoctors()) {
                        doctorSelector.addItem(doctor);
                    }
                    if (data.appointment() != null) {
                        LichHen appointment = data.appointment();
                        for (AppointmentPatient patient : data.patients()) {
                            if (data.allPatients().stream()
                                    .noneMatch(existing -> existing.maBenhNhan().equals(patient.maBenhNhan()))) {
                                patientSelector.addItem(patient);
                            }
                        }
                        selectPatient(appointment.maBenhNhan());
                        selectSpecialty(appointment.maChuyenKhoa());
                        appointmentDate.setValue(Date.from(appointment.thoiGianHen()
                                .toLocalDate().atStartOfDay(ZoneId.systemDefault())
                                .toInstant()));
                        reasonField.setText(appointment.lyDoKham() == null
                                ? "" : appointment.lyDoKham());
                        noteField.setText(appointment.ghiChu() == null
                                ? "" : appointment.ghiChu());
                        patientSearch.setText(appointment.maBenhNhan());
                        patientSearch.setEnabled(false);
                        patientSelector.setEnabled(false);
                    }
                    updatingSelectors = false;
                    initializing = false;
                    setBusy(false, " ");
                    if (data.appointment() != null) {
                        reloadDoctors(data.appointment().maNhanVien(),
                                data.appointment().maKhungGio());
                    } else {
                        reloadDoctors();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Tải dữ liệu đặt lịch bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    fail("Không thể tải dữ liệu đặt lịch.", exception.getCause());
                }
            }
        }.execute();
    }

    private void searchPatients() {
        String query = patientSearch.getText().trim();
        searchPatientButton.setEnabled(false);
        message.setForeground(MainFrame.MUTED);
        message.setText("Đang tìm bệnh nhân...");
        new SwingWorker<List<AppointmentPatient>, Void>() {
            @Override
            protected List<AppointmentPatient> doInBackground()
                    throws AppointmentException {
                return appointmentBUS.searchPatients(session, query);
            }

            @Override
            protected void done() {
                try {
                    List<AppointmentPatient> patients = get();
                    updatingSelectors = true;
                    patientSelector.removeAllItems();
                    for (AppointmentPatient patient : patients) {
                        patientSelector.addItem(patient);
                    }
                    updatingSelectors = false;
                    message.setForeground(patients.isEmpty()
                            ? new Color(170, 110, 30) : MainFrame.MUTED);
                    message.setText(patients.isEmpty()
                            ? "Không tìm thấy bệnh nhân đang hoạt động."
                            : "Chọn đúng bệnh nhân trong danh sách.");
                    reloadSlots();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Tìm bệnh nhân bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    fail("Không thể tìm bệnh nhân.", exception.getCause());
                } finally {
                    searchPatientButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void reloadDoctors() {
        reloadDoctors(null, null);
    }

    private void reloadDoctors(String selectedDoctorId, Long selectedSlotId) {
        AppointmentSpecialty specialty =
                (AppointmentSpecialty) specialtySelector.getSelectedItem();
        LocalDate date = selectedDate();
        updatingSelectors = true;
        doctorSelector.removeAllItems();
        slotSelector.removeAllItems();
        slotSelector.setEnabled(false);
        updatingSelectors = false;
        if (specialty == null || date == null) {
            updateSaveState();
            return;
        }
        long requestVersion = ++doctorRequestVersion;
        setOptionsLoading(true, "Đang tải bác sĩ theo lịch phân công...");
        new SwingWorker<List<AppointmentDoctor>, Void>() {
            @Override
            protected List<AppointmentDoctor> doInBackground()
                    throws AppointmentException {
                return appointmentBUS.getDoctors(session, specialty.maChuyenKhoa(), date);
            }

            @Override
            protected void done() {
                if (requestVersion != doctorRequestVersion) {
                    return;
                }
                try {
                    List<AppointmentDoctor> availableDoctors = get();
                    if (availableDoctors.isEmpty() && specialty != null) {
                        availableDoctors = knownDoctors.stream()
                                .filter(doctor -> doctor.maChuyenKhoa() != null
                                        && doctor.maChuyenKhoa().equals(specialty.maChuyenKhoa()))
                                .toList();
                    }
                    updatingSelectors = true;
                    for (AppointmentDoctor doctor : availableDoctors) {
                        doctorSelector.addItem(doctor);
                    }
                    if (selectedDoctorId != null) {
                        selectDoctor(selectedDoctorId);
                    }
                    updatingSelectors = false;
                    setOptionsLoading(false, availableDoctors.isEmpty()
                            ? "Ngày này chưa có bác sĩ được phân công đã xác nhận; đang hiển thị bác sĩ chuyên khoa có sẵn."
                            : " ");
                    if (doctorSelector.getSelectedItem() != null) {
                        reloadSlots(selectedSlotId);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Tải danh sách bác sĩ bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    fail("Không thể tải bác sĩ theo lịch làm việc.", exception.getCause());
                }
            }
        }.execute();
    }

    private void reloadSlots() {
        reloadSlots(null);
    }

    private void reloadSlots(Long preferredSlotId) {
        long version = ++slotRequestVersion;
        AppointmentPatient patient = (AppointmentPatient) patientSelector.getSelectedItem();
        AppointmentSpecialty specialty =
                (AppointmentSpecialty) specialtySelector.getSelectedItem();
        AppointmentDoctor doctor = (AppointmentDoctor) doctorSelector.getSelectedItem();
        LocalDate date = selectedDate();
        updatingSelectors = true;
        slotSelector.removeAllItems();
        slotSelector.setEnabled(false);
        updatingSelectors = false;
        if (patient == null || specialty == null || doctor == null || date == null) {
            setOptionsLoading(false, "Chọn bệnh nhân, chuyên khoa, bác sĩ và ngày để xem giờ trống.");
            return;
        }
        setOptionsLoading(true, "Đang kiểm tra khung giờ khả dụng...");
        new SwingWorker<List<AppointmentSlot>, Void>() {
            @Override
            protected List<AppointmentSlot> doInBackground() throws AppointmentException {
                return appointmentBUS.getAvailableSlots(
                        session, date, doctor.maNhanVien(),
                        specialty.maChuyenKhoa(), patient.maBenhNhan(), appointmentId);
            }

            @Override
            protected void done() {
                if (version != slotRequestVersion) {
                    return;
                }
                try {
                    List<AppointmentSlot> availableSlots = get();
                    updatingSelectors = true;
                    for (AppointmentSlot slot : availableSlots) {
                        slotSelector.addItem(slot);
                    }
                    if (preferredSlotId != null) {
                        for (int i = 0; i < slotSelector.getItemCount(); i++) {
                            if (slotSelector.getItemAt(i).maKhungGio() == preferredSlotId) {
                                slotSelector.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    slotSelector.setEnabled(!availableSlots.isEmpty());
                    updatingSelectors = false;
                    setOptionsLoading(false, availableSlots.isEmpty()
                            ? "Không có khung giờ phù hợp hoặc giờ đã hết chỗ."
                            : " ");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Tải khung giờ bị gián đoạn.", exception);
                } catch (ExecutionException exception) {
                    fail("Không thể tải khung giờ khả dụng.", exception.getCause());
                }
            }
        }.execute();
    }

    private LocalDate selectedDate() {
        Date value = (Date) appointmentDate.getValue();
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void save() {
        if (busy) {
            return;
        }
        AppointmentPatient patient = (AppointmentPatient) patientSelector.getSelectedItem();
        AppointmentSpecialty specialty =
                (AppointmentSpecialty) specialtySelector.getSelectedItem();
        AppointmentDoctor doctor = (AppointmentDoctor) doctorSelector.getSelectedItem();
        AppointmentSlot slot = (AppointmentSlot) slotSelector.getSelectedItem();
        if (patient == null || specialty == null || doctor == null || slot == null) {
            message.setForeground(new Color(190, 62, 62));
            message.setText("Hãy chọn bệnh nhân và khung giờ còn trống.");
            return;
        }
        AppointmentDraft draft = new AppointmentDraft(
                patient.maBenhNhan(),
                doctor.maNhanVien(),
                specialty.maChuyenKhoa(),
                slot.maKhungGio(),
                reasonField.getText().trim(),
                noteField.getText().trim());
        setBusy(true, appointmentId == null
                ? "Đang kiểm tra và đặt lịch..." : "Đang kiểm tra và cập nhật...");
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws AppointmentException {
                if (appointmentId == null) {
                    return appointmentBUS.create(session, draft);
                }
                appointmentBUS.update(session, appointmentId, draft);
                return appointmentId;
            }

            @Override
            protected void done() {
                try {
                    long savedId = get();
                    onSaved.accept(savedId);
                    dispose();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Thao tác bị gián đoạn. Vui lòng thử lại.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    setBusy(false, cause instanceof AppointmentException
                            ? cause.getMessage()
                            : "Không thể lưu lịch hẹn. Vui lòng thử lại.");
                    if (!(cause instanceof AppointmentException)) {
                        LOGGER.log(Level.SEVERE, "Unexpected appointment save error.", cause);
                    }
                }
            }
        }.execute();
    }

    private void selectPatient(String patientId) {
        for (int i = 0; i < patientSelector.getItemCount(); i++) {
            if (patientSelector.getItemAt(i).maBenhNhan().equals(patientId)) {
                patientSelector.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectSpecialty(String specialtyId) {
        for (int i = 0; i < specialtySelector.getItemCount(); i++) {
            if (specialtySelector.getItemAt(i).maChuyenKhoa().equals(specialtyId)) {
                specialtySelector.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectDoctor(String doctorId) {
        for (int i = 0; i < doctorSelector.getItemCount(); i++) {
            if (doctorSelector.getItemAt(i).maNhanVien().equals(doctorId)) {
                doctorSelector.setSelectedIndex(i);
                return;
            }
        }
    }

    private void setBusy(boolean value, String text) {
        busy = value;
        searchPatientButton.setEnabled(!value && appointmentId == null);
        cancelButton.setEnabled(!value);
        patientSearch.setEnabled(!value && appointmentId == null);
        patientSelector.setEnabled(!value && appointmentId == null);
        specialtySelector.setEnabled(!value);
        doctorSelector.setEnabled(!value);
        appointmentDate.setEnabled(!value);
        slotSelector.setEnabled(!value && slotSelector.getItemCount() > 0);
        reasonField.setEnabled(!value);
        noteField.setEnabled(!value);
        addPatientButton.setEnabled(!value && appointmentId == null && canAddPatients);
        saveButton.setEnabled(!value && slotSelector.getSelectedItem() != null);
        message.setForeground(MainFrame.MUTED);
        message.setText(text);
    }

    private void setOptionsLoading(boolean value, String text) {
        busy = value;
        searchPatientButton.setEnabled(!value && appointmentId == null);
        specialtySelector.setEnabled(!value);
        doctorSelector.setEnabled(!value);
        appointmentDate.setEnabled(!value);
        slotSelector.setEnabled(!value && slotSelector.getItemCount() > 0);
        addPatientButton.setEnabled(!value && appointmentId == null && canAddPatients);
        saveButton.setEnabled(!value && slotSelector.getSelectedItem() != null);
        message.setForeground(MainFrame.MUTED);
        message.setText(text);
    }

    private void updateSaveState() {
        saveButton.setEnabled(!busy && slotSelector.getSelectedItem() != null);
    }

    private void fail(String error, Throwable cause) {
        String display = cause instanceof AppointmentException
                ? cause.getMessage() : error;
        setBusy(false, display);
        if (!(cause instanceof AppointmentException)) {
            LOGGER.log(Level.SEVERE, "Unexpected appointment dialog error.", cause);
        }
        JOptionPane.showMessageDialog(
                this, display, "Lịch hẹn", JOptionPane.ERROR_MESSAGE);
    }

    private record InitialData(
            LocalDate databaseDate,
            List<AppointmentSpecialty> specialties,
            LichHen appointment,
            List<AppointmentPatient> patients,
            List<AppointmentPatient> allPatients,
            List<AppointmentDoctor> allDoctors) {
    }
}
