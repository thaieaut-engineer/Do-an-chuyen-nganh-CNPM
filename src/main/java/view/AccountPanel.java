package view;

import bus.account.AccountBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import model.account.AccountModels.AccountRow;
import model.auth.AuthenticatedSession;
import view.component.UIStyle;

final class AccountPanel extends JPanel {
    private final AuthenticatedSession session; private final AccountBUS bus; private final JTextField search=new JTextField(); private final JComboBox<String> filter=new JComboBox<>(new String[]{"Tất cả trạng thái","HoatDong","Khoa","ChoKichHoat","ThuHoi"});
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"ID","Tên đăng nhập","Nhân viên","Vai trò","Trạng thái","Ngày tạo","Đăng nhập gần nhất","__row"},0){public boolean isCellEditable(int r,int c){return false;}}; private final JTable table=new JTable(model); private final JButton edit=new JButton("Chỉnh sửa"),toggle=new JButton("Khóa / mở khóa");
    AccountPanel(AuthenticatedSession s,AccountBUS b){session=s;bus=b;setLayout(new BorderLayout(0,16));setBackground(MainFrame.PAGE);setBorder(BorderFactory.createEmptyBorder(25,28,30,28));add(heading(),BorderLayout.NORTH);add(card(),BorderLayout.CENTER);load();}
    void refreshData(){load();}
    private JPanel heading(){JPanel p=new JPanel(new BorderLayout());p.setOpaque(false);JLabel t=new JLabel("Quản lý tài khoản");t.setFont(new Font("Segoe UI",Font.BOLD,25));t.setForeground(MainFrame.TEXT);JLabel d=new JLabel("Tạo tài khoản, phân vai trò và kiểm soát trạng thái truy cập hệ thống.");d.setForeground(MainFrame.MUTED);JPanel l=new JPanel();l.setOpaque(false);l.setLayout(new javax.swing.BoxLayout(l,javax.swing.BoxLayout.Y_AXIS));l.add(t);l.add(d);JButton add=new JButton("+ Tạo tài khoản");UIStyle.primaryButton(add);add.addActionListener(e->open(null));p.add(l,BorderLayout.WEST);p.add(add,BorderLayout.EAST);return p;}
    private JPanel card(){JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MainFrame.BORDER),BorderFactory.createEmptyBorder(16,16,16,16)));JPanel bar=new JPanel(new BorderLayout(8,0));bar.setOpaque(false);search.setPreferredSize(new Dimension(300,36));search.putClientProperty("JTextField.placeholderText","Tìm tên đăng nhập, nhân viên...");bar.add(search,BorderLayout.CENTER);bar.add(filter,BorderLayout.EAST);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));actions.setOpaque(false);UIStyle.secondaryButton(edit);UIStyle.dangerButton(toggle);edit.setEnabled(false);toggle.setEnabled(false);actions.add(edit);actions.add(toggle);bar.add(actions,BorderLayout.SOUTH);p.add(bar,BorderLayout.NORTH);UIStyle.table(table);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);table.getSelectionModel().addListSelectionListener(e->{boolean x=table.getSelectedRow()>=0;edit.setEnabled(x);toggle.setEnabled(x);});edit.addActionListener(e->{AccountRow r=selected();if(r!=null)open(r);});toggle.addActionListener(e->toggle());search.addActionListener(e->load());filter.addActionListener(e->load());p.add(new JScrollPane(table),BorderLayout.CENTER);return p;}
    private AccountRow selected(){int row=table.getSelectedRow();return row<0?null:(AccountRow)table.getValueAt(row,7);}
    private void open(AccountRow row){new AccountDialog(SwingUtilities.getWindowAncestor(this),session,bus,row).setVisible(true);load();}
    private void toggle(){AccountRow r=selected();if(r==null)return;String next="HoatDong".equals(r.status())?"Khoa":"HoatDong";if(javax.swing.JOptionPane.showConfirmDialog(this,("HoatDong".equals(next)?"Mở khóa":"Khóa")+" tài khoản '"+r.username()+"'?","Xác nhận",javax.swing.JOptionPane.YES_NO_OPTION)!=javax.swing.JOptionPane.YES_OPTION)return;try{bus.changeStatus(session,r.id(),next);UIStyle.showSuccess(this,"Đã cập nhật trạng thái tài khoản.");load();}catch(RuntimeException e){UIStyle.showError(this,e.getMessage());}}
    private void load(){new SwingWorker<java.util.List<AccountRow>,Void>(){protected java.util.List<AccountRow> doInBackground(){String st=filter.getSelectedIndex()==0?"":(String)filter.getSelectedItem();return bus.search(session,search.getText(),st);}protected void done(){try{model.setRowCount(0);for(AccountRow r:get()){model.addRow(new Object[]{r.id(),r.username(),r.employeeId()+" - "+r.employeeName(),r.roles(),r.status(),r.createdAt(),r.lastLoginAt(),r});}table.getColumnModel().getColumn(7).setMinWidth(0);table.getColumnModel().getColumn(7).setMaxWidth(0);table.getColumnModel().getColumn(7).setWidth(0);}catch(Exception e){UIStyle.showError(AccountPanel.this,e.getCause()==null?e.getMessage():e.getCause().getMessage());}}}.execute();}
}
