package view;

import bus.billing.BillingBUS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.billing.BillingModels.InvoiceRow;
import model.billing.BillingModels.PaymentDraft;
import model.billing.BillingModels.PaymentMethod;
import view.component.UIStyle;

final class BillingPanel extends JPanel {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final AuthenticatedSession session; private final BillingBUS bus; private final boolean paymentMode;
    private final JTextField search=new JTextField(); private final JComboBox<String> filter=new JComboBox<>(new String[]{"Tất cả trạng thái","ChuaThanhToan","ThanhToanMotPhan","DaThanhToan","DaHuy","DaHoanTien"});
    private final JLabel stats=new JLabel("Đang tải..."); private final DefaultTableModel model=new DefaultTableModel(new Object[]{"Mã HĐ","Số hóa đơn","Bệnh nhân","Ngày lập","Tổng tiền","Đã thu","Còn phải thu","Trạng thái","__row"},0){public boolean isCellEditable(int r,int c){return false;}}; private final JTable table=new JTable(model); private final JButton pay=new JButton("Ghi nhận thanh toán");
    BillingPanel(AuthenticatedSession s,BillingBUS b,boolean mode){session=s;bus=b;paymentMode=mode;if(mode)filter.setSelectedIndex(1);setLayout(new BorderLayout(0,16));setBackground(MainFrame.PAGE);setBorder(BorderFactory.createEmptyBorder(25,28,30,28));add(header(),BorderLayout.NORTH);add(body(),BorderLayout.CENTER);load();}
    private JPanel header(){JPanel p=new JPanel(new BorderLayout(12,0));p.setOpaque(false);JPanel l=new JPanel();l.setOpaque(false);l.setLayout(new javax.swing.BoxLayout(l,javax.swing.BoxLayout.Y_AXIS));JLabel t=new JLabel(paymentMode?"Thanh toán":"Quản lý hóa đơn");t.setFont(new Font("Segoe UI",Font.BOLD,25));t.setForeground(MainFrame.TEXT);JLabel d=new JLabel(paymentMode?"Ghi nhận tiền thu và theo dõi công nợ theo từng hóa đơn.":"Tra cứu hóa đơn, số tiền đã thu và số tiền còn phải thu.");d.setForeground(MainFrame.MUTED);l.add(t);l.add(d);p.add(l,BorderLayout.WEST);p.add(stats,BorderLayout.EAST);return p;}
    private JPanel body(){JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(MainFrame.BORDER),BorderFactory.createEmptyBorder(16,16,16,16)));JPanel bar=new JPanel(new BorderLayout(8,0));bar.setOpaque(false);search.setPreferredSize(new Dimension(340,36));search.putClientProperty("JTextField.placeholderText","Tìm số hóa đơn, mã bệnh nhân, họ tên...");bar.add(search,BorderLayout.CENTER);bar.add(filter,BorderLayout.EAST);UIStyle.primaryButton(pay);pay.setEnabled(false);pay.addActionListener(e->openPayment());bar.add(pay,BorderLayout.SOUTH);table.setRowHeight(40);UIStyle.table(table);table.setAutoCreateRowSorter(true);table.getSelectionModel().addListSelectionListener(e->pay.setEnabled(paymentMode&&table.getSelectedRow()>=0));p.add(bar,BorderLayout.NORTH);p.add(new JScrollPane(table),BorderLayout.CENTER);search.addActionListener(e->load());filter.addActionListener(e->load());return p;}
    private InvoiceRow selected(){int r=table.getSelectedRow();if(r<0)return null;return (InvoiceRow)table.getValueAt(table.convertRowIndexToModel(r),8);}
    private void openPayment(){InvoiceRow row=selected();if(row==null)return;if(row.remaining().compareTo(BigDecimal.ZERO)<=0){UIStyle.showInfo(this,"Hóa đơn này đã thanh toán đủ.");return;}JTextField amount=new JTextField(row.remaining().toPlainString()),ref=new JTextField(),note=new JTextField();JComboBox<PaymentMethod> method=new JComboBox<>();try{for(PaymentMethod m:bus.methods(session))method.addItem(m);}catch(RuntimeException e){UIStyle.showError(this,e.getMessage());return;}JPanel p=new JPanel(new java.awt.GridLayout(0,2,8,8));p.add(new JLabel("Còn phải thu"));p.add(new JLabel(money(row.remaining())));p.add(new JLabel("Số tiền thu *"));p.add(amount);p.add(new JLabel("Phương thức *"));p.add(method);p.add(new JLabel("Mã tham chiếu"));p.add(ref);p.add(new JLabel("Ghi chú"));p.add(note);if(JOptionPane.showConfirmDialog(this,p,"Ghi nhận thanh toán · "+row.number(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;try{bus.pay(session,new PaymentDraft(row.id(),((PaymentMethod)method.getSelectedItem()).id(),new BigDecimal(amount.getText().trim()),ref.getText(),note.getText()));UIStyle.showSuccess(this,"Đã ghi nhận thanh toán thành công.");load();}catch(RuntimeException e){UIStyle.showError(this,e.getMessage());}}
    private void load(){new SwingWorker<java.util.List<InvoiceRow>,Void>(){protected java.util.List<InvoiceRow> doInBackground(){String st=filter.getSelectedIndex()==0?"":(String)filter.getSelectedItem();return paymentMode?bus.paymentQueue(session,search.getText(),st):bus.invoices(session,search.getText(),st);}protected void done(){try{model.setRowCount(0);for(InvoiceRow r:get())model.addRow(new Object[]{r.id(),r.number(),r.patientId()+" - "+r.patientName(),r.issuedAt().format(DATE),money(r.total()),money(r.paid()),money(r.remaining()),r.status(),r});stats.setText("Tổng: "+model.getRowCount()+" hóa đơn");table.getColumnModel().getColumn(8).setMinWidth(0);table.getColumnModel().getColumn(8).setMaxWidth(0);table.getColumnModel().getColumn(8).setWidth(0);}catch(Exception e){UIStyle.showError(BillingPanel.this,e.getCause()==null?e.getMessage():e.getCause().getMessage());}}}.execute();}
    private static String money(BigDecimal n){return n==null?"0":n.stripTrailingZeros().toPlainString();}
}
