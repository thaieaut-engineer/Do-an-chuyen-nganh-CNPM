package view;

import bus.insurance.InsuranceBUS;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import model.auth.AuthenticatedSession;
import model.insurance.InsuranceModels.*;
import view.component.UIStyle;

final class InsuranceDialog extends JDialog {
    private static final DateTimeFormatter F=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final AuthenticatedSession session; private final InsuranceBUS bus; private final Card editing;
    private final JComboBox<PatientOption> patient=new JComboBox<>(); private final JTextField number=new JTextField(),name=new JTextField(),birth=new JTextField(),facility=new JTextField(),from=new JTextField(),to=new JTextField(),coverage=new JTextField("80"); private final JComboBox<String> status=new JComboBox<>(new String[]{"ChuaXacThuc","HopLe","HetHan","KhongHopLe","ChoXacMinh"});
    InsuranceDialog(Window owner,AuthenticatedSession s,InsuranceBUS b,Card c){super(owner,c==null?"Thêm thẻ BHYT":"Cập nhật thẻ BHYT",ModalityType.APPLICATION_MODAL);session=s;bus=b;editing=c;setSize(520,570);setLocationRelativeTo(owner);build();load();}
    private void build(){JPanel form=new JPanel(new GridBagLayout());form.setBorder(BorderFactory.createEmptyBorder(16,22,8,22));add(form,BorderLayout.CENTER);field(form,0,"Bệnh nhân *",patient);field(form,1,"Số thẻ *",number);field(form,2,"Họ tên trên thẻ",name);field(form,3,"Ngày sinh (dd/MM/yyyy)",birth);field(form,4,"Nơi đăng ký KCB",facility);field(form,5,"Từ ngày (dd/MM/yyyy)",from);field(form,6,"Đến ngày (dd/MM/yyyy)",to);field(form,7,"Mức hưởng (%)",coverage);field(form,8,"Trạng thái",status);JButton cancel=new JButton("Hủy"),save=new JButton(editing==null?"Thêm thẻ":"Lưu thay đổi");UIStyle.secondaryButton(cancel);UIStyle.primaryButton(save);cancel.addActionListener(e->dispose());save.addActionListener(e->save());JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));actions.add(cancel);actions.add(save);add(actions,BorderLayout.SOUTH);}
    private void field(JPanel p,int row,String label,Component c){GridBagConstraints a=new GridBagConstraints();a.gridx=0;a.gridy=row;a.anchor=GridBagConstraints.WEST;a.insets=new Insets(6,0,6,12);p.add(new JLabel(label),a);GridBagConstraints b=new GridBagConstraints();b.gridx=1;b.gridy=row;b.weightx=1;b.fill=GridBagConstraints.HORIZONTAL;b.insets=new Insets(6,0,6,0);p.add(c,b);if(c instanceof JTextField t)t.setPreferredSize(new Dimension(280,32));if(c instanceof JComboBox<?> x)x.setPreferredSize(new Dimension(280,32));}
    private void load(){try{List<PatientOption> p=bus.patients(session);patient.setModel(new DefaultComboBoxModel<>(p.toArray(PatientOption[]::new)));if(editing!=null){number.setText(editing.cardNumber());name.setText(editing.nameOnCard());birth.setText(fmt(editing.birthDate()));facility.setText(editing.facility());from.setText(fmt(editing.fromDate()));to.setText(fmt(editing.toDate()));coverage.setText(editing.coverage()==null?"":editing.coverage().toPlainString());status.setSelectedItem(editing.status());for(int i=0;i<p.size();i++)if(p.get(i).id().equals(editing.patientId()))patient.setSelectedIndex(i);}}catch(RuntimeException e){UIStyle.showError(this,e.getMessage());}}
    private void save(){try{PatientOption p=(PatientOption)patient.getSelectedItem();if(p==null)throw new IllegalArgumentException("Vui lòng chọn bệnh nhân.");CardDraft d=new CardDraft(editing==null?0:editing.id(),p.id(),number.getText().trim(),name.getText().trim(),parse(birth.getText()),facility.getText().trim(),parse(from.getText()),parse(to.getText()),new java.math.BigDecimal(coverage.getText().trim()),(String)status.getSelectedItem());bus.save(session,d);UIStyle.showSuccess(this,"Đã lưu thẻ BHYT thành công.");dispose();}catch(Exception e){UIStyle.showError(this,e.getMessage());}}
    private static LocalDate parse(String s){return s==null||s.isBlank()?null:LocalDate.parse(s.trim(),F);}private static String fmt(LocalDate d){return d==null?"":F.format(d);}
}
