package view;

import bus.staff.StaffBUS;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import model.auth.AuthenticatedSession;
import model.staff.StaffModels.*;
import view.component.UIStyle;

final class StaffDialog extends JDialog {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final AuthenticatedSession session; private final StaffBUS bus; private final Staff editing; private final boolean doctor;
    private final JTextField id=new JTextField(), name=new JTextField(), birth=new JTextField(), phone=new JTextField(), email=new JTextField(), address=new JTextField(), start=new JTextField();
    private final JComboBox<String> gender=new JComboBox<>(new String[]{"Nam","Nữ","Khác"}), status=new JComboBox<>(new String[]{"ThuViec","DangLam","TamNghi","DaNghi"});
    private final JComboBox<Option> department=new JComboBox<>(), title=new JComboBox<>(), specialty=new JComboBox<>();
    private final JComboBox<String> degree=new JComboBox<>(new String[]{"", "Bác sĩ", "Thạc sĩ", "Tiến sĩ", "Cử nhân", "Dược sĩ"});
    private final JTextField specialtyText=new JTextField(), license=new JTextField();

    StaffDialog(Window owner, AuthenticatedSession s, StaffBUS b, Staff e, boolean d) {
        super(owner,e==null?"Thêm nhân viên":"Cập nhật nhân viên",ModalityType.APPLICATION_MODAL);
        session=s; bus=b; editing=e; doctor=d; loadCatalogs(); setSize(610,d?650:510); setLocationRelativeTo(owner); build();
    }

    private void loadCatalogs() {
        try { for(Option x:bus.departments(session)) department.addItem(x); for(Option x:bus.titles(session)) title.addItem(x); for(Option x:bus.specialties(session)) specialty.addItem(x); }
        catch(Exception e) { UIStyle.showError(this,e.getMessage()); }
    }

    private void build() {
        JPanel f=new JPanel(new GridBagLayout()); f.setBorder(BorderFactory.createEmptyBorder(15,22,8,22)); int r=0;
        row(f,r++,"Mã nhân viên *",id); row(f,r++,"Họ tên *",name); row(f,r++,"Ngày sinh",birth); row(f,r++,"Giới tính",gender); row(f,r++,"Điện thoại",phone); row(f,r++,"Email",email); row(f,r++,"Địa chỉ",address); row(f,r++,"Ngày bắt đầu *",start); row(f,r++,"Trạng thái",status); row(f,r++,"Bộ phận",department); row(f,r++,"Chức danh",title);
        if(doctor){row(f,r++,"Học vị",degree); row(f,r++,"Chuyên môn",specialtyText); row(f,r++,"Chuyên khoa",specialty); row(f,r++,"Chứng chỉ hành nghề",license);}
        if(editing!=null){id.setText(editing.id());id.setEnabled(false);name.setText(editing.name());birth.setText(fmt(editing.birth()));gender.setSelectedItem(editing.gender());phone.setText(editing.phone());email.setText(editing.email());address.setText(editing.address());start.setText(fmt(editing.startDate()));status.setSelectedItem(editing.status());select(department,editing.departmentId());select(title,editing.titleId());degree.setSelectedItem(editing.degree());specialtyText.setText(editing.specialty());select(specialty,editing.specialtyId());license.setText(editing.license());}else start.setText(fmt(LocalDate.now()));
        add(f,BorderLayout.CENTER); JButton cancel=new JButton("Hủy"), save=new JButton(editing==null?"Thêm nhân viên":"Lưu thay đổi"); UIStyle.secondaryButton(cancel); UIStyle.primaryButton(save); cancel.addActionListener(e->dispose()); save.addActionListener(e->save()); JPanel a=new JPanel(new FlowLayout(FlowLayout.RIGHT)); a.add(cancel); a.add(save); add(a,BorderLayout.SOUTH);
    }

    private static void select(JComboBox<Option> box,String id){for(int i=0;i<box.getItemCount();i++)if(box.getItemAt(i).id().equals(id)){box.setSelectedIndex(i);return;}}
    private void row(JPanel p,int r,String l,Component c){GridBagConstraints a=new GridBagConstraints();a.gridx=0;a.gridy=r;a.anchor=GridBagConstraints.WEST;a.insets=new Insets(5,0,5,12);p.add(new JLabel(l),a);GridBagConstraints b=new GridBagConstraints();b.gridx=1;b.gridy=r;b.weightx=1;b.fill=GridBagConstraints.HORIZONTAL;b.insets=new Insets(5,0,5,0);p.add(c,b);if(c instanceof JTextField t)t.setPreferredSize(new Dimension(340,30));}
    private void save(){try{Option dep=(Option)department.getSelectedItem(),tit=(Option)title.getSelectedItem(),spec=(Option)specialty.getSelectedItem();StaffDraft d=new StaffDraft(id.getText().trim(),name.getText().trim(),parse(birth.getText()),(String)gender.getSelectedItem(),phone.getText().trim(),email.getText().trim(),address.getText().trim(),parse(start.getText()),(String)status.getSelectedItem(),dep==null?"":dep.id(),tit==null?"":tit.id(),(String)degree.getSelectedItem(),specialtyText.getText().trim(),spec==null?"":spec.id(),license.getText().trim());bus.save(session,d,doctor);UIStyle.showSuccess(this,"Đã lưu hồ sơ nhân sự.");dispose();}catch(Exception e){UIStyle.showError(this,e.getMessage());}}
    private static LocalDate parse(String s){return s==null||s.isBlank()?null:LocalDate.parse(s.trim(),F);} private static String fmt(LocalDate d){return d==null?"":F.format(d);}
}
