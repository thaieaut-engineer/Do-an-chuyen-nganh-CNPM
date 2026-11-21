package view;

import bus.insurance.InsuranceBUS;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.insurance.InsuranceModels.Card;
import view.component.UIStyle;

final class InsurancePanel extends JPanel {
    private static final DateTimeFormatter F=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final AuthenticatedSession session;private final InsuranceBUS bus;private final JTextField search=new JTextField();private final JComboBox<String> filter=new JComboBox<>(new String[]{"Tất cả trạng thái","ChuaXacThuc","HopLe","HetHan","KhongHopLe","ChoXacMinh"});private final DefaultTableModel model=new DefaultTableModel(new Object[]{"Mã thẻ","Số thẻ","Bệnh nhân","Nơi KCB","Từ ngày","Đến ngày","Mức hưởng","Trạng thái","__row"},0){public boolean isCellEditable(int r,int c){return false;}};private final JTable table=new JTable(model);private final JButton edit=new JButton("Chỉnh sửa");
    InsurancePanel(AuthenticatedSession s,InsuranceBUS b){session=s;bus=b;setLayout(new BorderLayout(0,16));setBackground(MainFrame.PAGE);setBorder(BorderFactory.createEmptyBorder(25,28,30,28));add(header(),BorderLayout.NORTH);add(body(),BorderLayout.CENTER);load();}
    private JPanel header(){JPanel p=new JPanel(new BorderLayout());p.setOpaque(false);JPanel l=new JPanel();l.setOpaque(false);l.setLayout(new BoxLayout(l,BoxLayout.Y_AXIS));JLabel t=new JLabel("Bảo hiểm y tế");t.setFont(new Font("Segoe UI",Font.BOLD,25));t.setForeground(MainFrame.TEXT);JLabel d=new JLabel("Quản lý thẻ BHYT, thời hạn và mức hưởng của bệnh nhân.");d.setForeground(MainFrame.MUTED);l.add(t);l.add(d);JButton add=new JButton("+ Thêm thẻ BHYT");UIStyle.primaryButton(add);add.addActionListener(e->open(null));p.add(l,BorderLayout.WEST);p.add(add,BorderLayout.EAST);return p;}
    private JPanel body(){JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MainFrame.BORDER),BorderFactory.createEmptyBorder(16,16,16,16)));JPanel bar=new JPanel(new BorderLayout(8,0));bar.setOpaque(false);search.setPreferredSize(new Dimension(350,36));search.putClientProperty("JTextField.placeholderText","Tìm số thẻ, mã bệnh nhân, họ tên...");bar.add(search,BorderLayout.CENTER);bar.add(filter,BorderLayout.EAST);UIStyle.secondaryButton(edit);edit.setEnabled(false);edit.addActionListener(e->{Card c=selected();if(c!=null)open(c);});bar.add(edit,BorderLayout.SOUTH);table.setRowHeight(40);UIStyle.table(table);table.setAutoCreateRowSorter(true);table.getSelectionModel().addListSelectionListener(e->edit.setEnabled(table.getSelectedRow()>=0));p.add(bar,BorderLayout.NORTH);p.add(new JScrollPane(table),BorderLayout.CENTER);search.addActionListener(e->load());filter.addActionListener(e->load());return p;}
    private Card selected(){int r=table.getSelectedRow();return r<0?null:(Card)table.getValueAt(table.convertRowIndexToModel(r),8);}private void open(Card c){new InsuranceDialog(SwingUtilities.getWindowAncestor(this),session,bus,c).setVisible(true);load();}
    private void load(){new SwingWorker<java.util.List<Card>,Void>(){protected java.util.List<Card> doInBackground(){String st=filter.getSelectedIndex()==0?"":(String)filter.getSelectedItem();return bus.find(session,search.getText(),st);}protected void done(){try{model.setRowCount(0);for(Card c:get()){bus.refreshExpired(session,c);model.addRow(new Object[]{c.id(),c.cardNumber(),c.patientId()+" - "+c.patientName(),c.facility(),fmt(c.fromDate()),fmt(c.toDate()),c.coverage()+"%",c.status(),c});}table.getColumnModel().getColumn(8).setMinWidth(0);table.getColumnModel().getColumn(8).setMaxWidth(0);table.getColumnModel().getColumn(8).setWidth(0);}catch(Exception e){UIStyle.showError(InsurancePanel.this,e.getCause()==null?e.getMessage():e.getCause().getMessage());}}}.execute();}
    private static String fmt(java.time.LocalDate d){return d==null?"":F.format(d);}
}
