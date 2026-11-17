package view;

import bus.chemical.ChemicalBUS;
import bus.chemical.ChemicalException;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.chemical.ChemicalModels.*;
import view.component.UIStyle;

public final class ChemicalWarehousePanel extends JPanel {
    private static final int SIZE=20;
    private final AuthenticatedSession session; private final ChemicalBUS bus;
    private final JTabbedPane tabs=new JTabbedPane(); private final JLabel message=new JLabel(" ");
    private final JLabel stats=new JLabel("Đang tải..."); private final JTextField chemicalSearch=new JTextField(); private final JTextField batchSearch=new JTextField(); private final JTextField historySearch=new JTextField();
    private final DefaultTableModel chemicalModel=model("Mã","Tên hóa chất","Nồng độ","Đơn vị","Hãng SX","Trạng thái");
    private final DefaultTableModel batchModel=model("Mã lô","Mã HC","Tên hóa chất","Số lô","Hạn SD","Tồn","Vị trí","Trạng thái");
    private final DefaultTableModel historyModel=model("Thời gian","Loại","Số phiếu","Mã HC","Tên hóa chất","Số lô","SL","Đơn giá","Nhân viên","Trạng thái","Ghi chú");
    private int chemicalPage,batchPage,historyPage; private Page<Chemical> chemicals; private Page<Batch> batches;
    public ChemicalWarehousePanel(AuthenticatedSession session,ChemicalBUS bus){this.session=session;this.bus=bus;setLayout(new BorderLayout(0,12));setBackground(MainFrame.PAGE);setBorder(BorderFactory.createEmptyBorder(16,18,18,18));add(header(),BorderLayout.NORTH);tabs.addTab("Danh mục hóa chất",chemicalTab());tabs.addTab("Tồn theo lô",batchTab());tabs.addTab("Lịch sử nhập xuất",historyTab());tabs.addChangeListener(e->loadActive());add(tabs,BorderLayout.CENTER);loadStats();loadActive();}
    private JPanel header(){JPanel p=new JPanel(new BorderLayout(10,0));p.setOpaque(false);JLabel title=new JLabel("Kho hóa chất");title.setFont(new Font("Segoe UI",Font.BOLD,24));title.setForeground(MainFrame.TEXT);p.add(title,BorderLayout.WEST);p.add(stats,BorderLayout.CENTER);JButton refresh=new JButton("Làm mới");refresh.addActionListener(e->{loadStats();loadActive();});p.add(refresh,BorderLayout.EAST);return p;}
    private JPanel chemicalTab(){JPanel p=base();p.add(toolbar(chemicalSearch,"Thêm hóa chất",e->edit(null)),BorderLayout.NORTH);JTable t=new JTable(chemicalModel);UIStyle.table(t);t.setAutoCreateRowSorter(true);p.add(new JScrollPane(t),BorderLayout.CENTER);return p;}
    private JPanel batchTab(){JPanel p=base();p.add(toolbar(batchSearch,"Nhập hóa chất",e->importDialog()),BorderLayout.NORTH);JTable t=new JTable(batchModel);UIStyle.table(t);t.setAutoCreateRowSorter(true);p.add(new JScrollPane(t),BorderLayout.CENTER);return p;}
    private JPanel historyTab(){JPanel p=base();p.add(toolbar(historySearch,"Xuất hóa chất",e->exportDialog()),BorderLayout.NORTH);JTable t=new JTable(historyModel);UIStyle.table(t);t.setAutoCreateRowSorter(true);p.add(new JScrollPane(t),BorderLayout.CENTER);return p;}
    private JPanel base(){JPanel p=new JPanel(new BorderLayout(0,8));p.setOpaque(false);return p;}
    private JPanel toolbar(JTextField search,String action,java.awt.event.ActionListener listener){JPanel p=new JPanel(new BorderLayout(8,0));p.setOpaque(false);search.setPreferredSize(new Dimension(300,32));search.addActionListener(e->loadActive());p.add(search,BorderLayout.WEST);JButton find=new JButton("Tìm kiếm");find.addActionListener(e->loadActive());p.add(find,BorderLayout.CENTER);if(session.hasPermission("KHO_NHAP")||session.hasPermission("KHO_XUAT")){JButton b=new JButton(action);b.addActionListener(listener);p.add(b,BorderLayout.EAST);}return p;}
    private void loadStats(){worker(()->bus.stats(session),s->stats.setText("Hóa chất: "+s.chemicalCount()+"  ·  Lô: "+s.batchCount()+"  ·  Tồn: "+s.totalStock()+"  ·  Sắp hết hạn: "+s.expiringBatches()+"  ·  Hết hạn: "+s.expiredBatches()));}
    private void loadActive(){switch(tabs.getSelectedIndex()){case 0->loadChemicals();case 1->loadBatches();case 2->loadHistory();default->{}}}
    private void loadChemicals(){worker(()->bus.search(session,chemicalSearch.getText(),"",chemicalPage,SIZE),p->{chemicals=p;chemicalModel.setRowCount(0);for(Chemical c:p.rows())chemicalModel.addRow(new Object[]{c.id(),c.name(),safe(c.concentration()),safe(c.unit()),safe(c.manufacturer()),c.status()});});}
    private void loadBatches(){worker(()->bus.batches(session,batchSearch.getText(),"",batchPage,SIZE),p->{batches=p;batchModel.setRowCount(0);for(Batch b:p.rows())batchModel.addRow(new Object[]{b.id(),b.chemicalId(),b.chemicalName(),b.batchNumber(),b.expiryDate(),b.stock(),safe(b.location()),b.status()});});}
    private void loadHistory(){worker(()->bus.movements(session,historySearch.getText(),historyPage,SIZE),p->{historyModel.setRowCount(0);for(Movement m:p.rows())historyModel.addRow(new Object[]{m.date(),m.type(),m.receiptNumber(),m.chemicalId(),m.chemicalName(),m.batchNumber(),m.quantity(),m.unitCost(),m.employeeName(),m.status(),safe(m.note())});});}
    private void edit(Chemical old){JTextField id=new JTextField(old==null?"":old.id()),name=new JTextField(old==null?"":old.name()),con=new JTextField(old==null?"":old.concentration()),unit=new JTextField(old==null?"":old.unit()),maker=new JTextField(old==null?"":old.manufacturer()),desc=new JTextField(old==null?"":old.description());JPanel p=new JPanel(new GridLayout(0,2,6,6));p.add(new JLabel("Mã *"));p.add(id);p.add(new JLabel("Tên *"));p.add(name);p.add(new JLabel("Nồng độ"));p.add(con);p.add(new JLabel("Đơn vị"));p.add(unit);p.add(new JLabel("Hãng SX"));p.add(maker);p.add(new JLabel("Mô tả"));p.add(desc);if(JOptionPane.showConfirmDialog(this,p,"Hóa chất",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{bus.save(session,new ChemicalDraft(id.getText(),name.getText(),con.getText(),unit.getText(),maker.getText(),desc.getText(),"DangSuDung"),old==null);loadStats();loadChemicals();}catch(ChemicalException e){error(e);}}
    private void importDialog(){JTextField chemical=new JTextField(),lot=new JTextField(),mfg=new JTextField(),exp=new JTextField(),qty=new JTextField(),cost=new JTextField(),loc=new JTextField(),supplier=new JTextField(),note=new JTextField();JPanel p=form(new String[]{"Mã hóa chất *","Số lô *","Ngày SX yyyy-MM-dd","Hạn SD yyyy-MM-dd *","Số lượng *","Đơn giá *","Vị trí","Nhà cung cấp","Ghi chú"},new JTextField[]{chemical,lot,mfg,exp,qty,cost,loc,supplier,note});if(JOptionPane.showConfirmDialog(this,p,"Nhập hóa chất",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{LocalDate md=mfg.getText().isBlank()?null:LocalDate.parse(mfg.getText());CreatedReceipt r=bus.createImport(session,new ImportDraft(supplier.getText(),note.getText(),List.of(new ImportLine(chemical.getText(),lot.getText(),md,LocalDate.parse(exp.getText()),new BigDecimal(qty.getText()),new BigDecimal(cost.getText()),loc.getText()))));JOptionPane.showMessageDialog(this,"Đã tạo phiếu "+r.number());loadStats();loadActive();}catch(Exception e){error(e);}}
    private void exportDialog(){JTextField chemical=new JTextField(),lot=new JTextField(),qty=new JTextField(),cost=new JTextField(),recipient=new JTextField(),note=new JTextField();JPanel p=form(new String[]{"Mã hóa chất *","Số lô *","Số lượng *","Đơn giá","Nơi nhận","Ghi chú"},new JTextField[]{chemical,lot,qty,cost,recipient,note});if(JOptionPane.showConfirmDialog(this,p,"Xuất hóa chất",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{CreatedReceipt r=bus.createExport(session,new ExportDraft(chemical.getText(),lot.getText(),new BigDecimal(qty.getText()),cost.getText().isBlank()?BigDecimal.ZERO:new BigDecimal(cost.getText()),recipient.getText(),note.getText()));JOptionPane.showMessageDialog(this,"Đã tạo phiếu "+r.number());loadStats();loadHistory();loadBatches();}catch(Exception e){error(e);}}
    private JPanel form(String[] labels,JTextField[] fields){JPanel p=new JPanel(new GridLayout(labels.length,2,6,6));for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(fields[i]);}return p;}
    private <T>void worker(java.util.concurrent.Callable<T> task,java.util.function.Consumer<T> success){new SwingWorker<T,Void>(){protected T doInBackground()throws Exception{return task.call();}protected void done(){try{success.accept(get());}catch(InterruptedException e){Thread.currentThread().interrupt();error(e);}catch(ExecutionException e){error(e.getCause());}}}.execute();}
    private void error(Throwable e){message.setText(e.getMessage()==null?"Có lỗi xảy ra.":e.getMessage());JOptionPane.showMessageDialog(this,message.getText(),"Không thể thực hiện",JOptionPane.ERROR_MESSAGE);}
    private static DefaultTableModel model(String... c){return new DefaultTableModel(c,0){public boolean isCellEditable(int r,int c){return false;}};}
    private static String safe(String s){return s==null||s.isBlank()?"—":s;}
}
