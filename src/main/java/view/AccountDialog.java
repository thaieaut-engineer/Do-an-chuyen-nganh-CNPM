package view;

import bus.account.AccountBUS;
import bus.account.AccountException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import model.account.AccountModels.AccountDraft;
import model.account.AccountModels.AccountRow;
import model.account.AccountModels.EmployeeOption;
import model.account.AccountModels.RoleOption;
import model.auth.AuthenticatedSession;
import view.component.UIStyle;

final class AccountDialog extends JDialog {
    private final AuthenticatedSession session;
    private final AccountBUS bus;
    private final AccountRow editing;
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JComboBox<EmployeeOption> employee = new JComboBox<>();
    private final JComboBox<String> status = new JComboBox<>(new String[]{"HoatDong","ChoKichHoat","Khoa","ThuHoi"});
    private final JList<RoleOption> roles = new JList<>();
    private final JLabel message = new JLabel(" ");

    AccountDialog(java.awt.Window owner, AuthenticatedSession session, AccountBUS bus, AccountRow editing) {
        super(owner, editing == null ? "Tạo tài khoản" : "Cập nhật tài khoản", ModalityType.APPLICATION_MODAL);
        this.session=session; this.bus=bus; this.editing=editing;
        setSize(540, 520); setLocationRelativeTo(owner); setResizable(false);
        build(); loadOptions();
    }
    private void build() {
        JPanel form = new JPanel(new GridBagLayout()); form.setBorder(BorderFactory.createEmptyBorder(20,24,10,24));
        addRow(form,0,"Tên đăng nhập *",username); addRow(form,1,"Mật khẩu *",password);
        addRow(form,2,"Nhân viên *",employee); addRow(form,3,"Trạng thái",status);
        roles.setVisibleRowCount(5); roles.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addRow(form,4,"Vai trò *",new JScrollPane(roles));
        message.setForeground(new java.awt.Color(210,70,70)); message.setBorder(BorderFactory.createEmptyBorder(0,24,8,24));
        JButton cancel=new JButton("Hủy"), save=new JButton(editing==null?"Tạo tài khoản":"Lưu thay đổi"); UIStyle.secondaryButton(cancel); UIStyle.primaryButton(save);
        cancel.addActionListener(e->dispose()); save.addActionListener(e->save());
        JPanel actions=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT,8,10)); actions.add(cancel); actions.add(save);
        add(form,BorderLayout.CENTER); add(message,BorderLayout.NORTH); add(actions,BorderLayout.SOUTH);
    }
    private void addRow(JPanel p,int row,String label,java.awt.Component c){ GridBagConstraints l=new GridBagConstraints(); l.gridx=0;l.gridy=row;l.anchor=GridBagConstraints.NORTHWEST;l.insets=new Insets(8,0,8,12);p.add(new JLabel(label),l); GridBagConstraints f=new GridBagConstraints();f.gridx=1;f.gridy=row;f.weightx=1;f.fill=GridBagConstraints.HORIZONTAL;f.insets=new Insets(8,0,8,0); if(row==4)f.fill=GridBagConstraints.BOTH; if(row==4)f.weighty=1;p.add(c,f); if(c instanceof JTextField t)t.setPreferredSize(new Dimension(320,34)); if(c instanceof JComboBox<?> b)b.setPreferredSize(new Dimension(320,34)); }
    private void loadOptions(){ try { List<EmployeeOption> es=bus.employees(session, editing==null?0:editing.id()); employee.setModel(new DefaultComboBoxModel<>(es.toArray(EmployeeOption[]::new))); List<RoleOption> rs=bus.roles(session); DefaultListModel<RoleOption> m=new DefaultListModel<>();rs.forEach(m::addElement);roles.setModel(m); if(editing!=null){username.setText(editing.username());status.setSelectedItem(editing.status());for(int i=0;i<m.size();i++)if(editing.roles()!=null&&editing.roles().contains(m.get(i).name()))roles.addSelectionInterval(i,i);for(int i=0;i<es.size();i++)if(es.get(i).id().equals(editing.employeeId()))employee.setSelectedIndex(i);password.setToolTipText("Để trống nếu không đổi mật khẩu");} } catch(RuntimeException e){message.setText(e.getMessage());} }
    private void save(){ try { List<String> selected=new ArrayList<>();for(RoleOption r:roles.getSelectedValuesList())selected.add(r.id()); char[] pass=password.getPassword(); if(employee.getSelectedItem()==null) throw new AccountException("Chưa có nhân viên phù hợp để gắn tài khoản."); AccountDraft d=new AccountDraft(editing==null?0:editing.id(),username.getText().trim(),((EmployeeOption)employee.getSelectedItem()).id(),(String)status.getSelectedItem(),selected,pass);if(editing==null)bus.create(session,d);else bus.update(session,d); UIStyle.showSuccess(this,editing==null?"Đã tạo tài khoản thành công.":"Đã cập nhật tài khoản.");dispose(); }catch(RuntimeException e){message.setText(e.getMessage());UIStyle.showError(this,e.getMessage());} }
}
