package view;

import bus.fee.FeeBUS;
import bus.fee.FeeException;
import java.awt.*;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.auth.AuthenticatedSession;
import model.fee.FeeModels.*;
import view.component.UIStyle;

public final class FeePanel extends JPanel {
    private final AuthenticatedSession session; private final FeeBUS bus;
    private final JTextField search=new JTextField(); private final JLabel stats=new JLabel("Đang tải..."); private final JLabel paging=new JLabel(" ");
    private final DefaultTableModel tableModel=model("Mã lượt khám","Mã bệnh nhân","Bệnh nhân","Bác sĩ","Thời gian","Trạng thái","Phiếu phí","Bệnh nhân trả","Số khoản"); private final JTable table=new JTable(tableModel); private Page<Visit> page; private int currentPage;
    public FeePanel(AuthenticatedSession s,FeeBUS b){session=s;bus=b;setLayout(new BorderLayout(0,12));setBackground(MainFrame.PAGE);setBorder(BorderFactory.createEmptyBorder(18,20,20,20));add(header(),BorderLayout.NORTH);JPanel body=new JPanel(new BorderLayout(0,10));body.setOpaque(false);body.add(toolbar(),BorderLayout.NORTH);UIStyle.table(table);table.setAutoCreateRowSorter(true);table.getSelectionModel().addListSelectionListener(e->{});body.add(new JScrollPane(table),BorderLayout.CENTER);body.add(footer(),BorderLayout.SOUTH);add(body,BorderLayout.CENTER);loadStats();load();}
    private JPanel header(){JPanel p=new JPanel(new BorderLayout());p.setOpaque(false);JLabel title=new JLabel("Viện phí");title.setFont(new Font("Segoe UI",Font.BOLD,25));title.setForeground(MainFrame.TEXT);p.add(title,BorderLayout.WEST);p.add(stats,BorderLayout.CENTER);JButton r=new JButton("Làm mới");r.addActionListener(e->{loadStats();load();});p.add(r,BorderLayout.EAST);return p;}
    private JPanel toolbar(){JPanel p=new JPanel(new BorderLayout(8,0));p.setOpaque(false);search.setPreferredSize(new Dimension(330,34));search.addActionListener(e->{currentPage=0;load();});p.add(search,BorderLayout.WEST);JButton find=new JButton("Tìm lượt khám");find.addActionListener(e->{currentPage=0;load();});p.add(find,BorderLayout.CENTER);JButton calculate=new JButton("Tính viện phí");calculate.setBackground(MainFrame.BLUE);calculate.setForeground(Color.WHITE);calculate.setOpaque(true);calculate.setBorderPainted(false);calculate.addActionListener(e->calculate());p.add(calculate,BorderLayout.EAST);JButton detail=new JButton("Xem phiếu phí");detail.addActionListener(e->showDetail());p.add(detail,BorderLayout.SOUTH);return p;}
    private JPanel footer(){JPanel p=new JPanel(new BorderLayout());p.setOpaque(false);JButton prev=new JButton("‹ Trước"),next=new JButton("Sau ›");prev.addActionListener(e->{if(currentPage>0){currentPage--;load();}});next.addActionListener(e->{if(page!=null&&currentPage+1<page.totalPages()){currentPage++;load();}});JPanel nav=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));nav.setOpaque(false);nav.add(prev);nav.add(paging);nav.add(next);p.add(nav,BorderLayout.EAST);return p;}
    private void loadStats(){worker(()->bus.stats(session),s->stats.setText("Lượt chờ tính: "+s.eligibleVisits()+"  ·  Đã tính: "+s.calculatedBills()+"  ·  Thu hôm nay: "+money(s.todayRevenue())));}
    private void load(){worker(()->bus.visits(session,search.getText(),currentPage,20),p->{page=p;tableModel.setRowCount(0);for(Visit v:p.rows())tableModel.addRow(new Object[]{v.id(),v.patientId(),v.patientName(),v.doctorName(),v.receivedAt(),v.visitStatus(),v.feeStatus(),money(v.total()),v.lineCount()});paging.setText("Trang "+(currentPage+1)+" / "+p.totalPages()+" · "+p.totalCount()+" lượt");});}
    private Visit selected(){int r=table.getSelectedRow();if(r<0||page==null)return null;int m=table.convertRowIndexToModel(r);return m>=0&&m<page.rows().size()?page.rows().get(m):null;}
    private void calculate(){Visit v=selected();if(v==null){warn("Hãy chọn một lượt khám hoàn tất.");return;}if(!"Chưa tính".equals(v.feeStatus())){warn("Lượt khám này đã có phiếu tính phí.");return;}worker(()->bus.calculate(session,v.id()),d->{showDetail(d);loadStats();load();});}
    private void showDetail(){Visit v=selected();if(v==null){warn("Hãy chọn lượt khám cần xem.");return;}if(v.total()==null||v.lineCount()==0){warn("Lượt khám chưa có phiếu tính phí.");return;}worker(()->bus.latestDetail(session,v.id()),this::showDetail);}
    private void showDetail(Detail d){if(d==null){warn("Không tìm thấy phiếu tính phí.");return;}StringBuilder x=new StringBuilder("Phiếu tính phí #").append(d.id()).append("\nBệnh nhân: ").append(d.patientName()).append(" (").append(d.patientId()).append(")\nTổng trước giảm: ").append(money(d.gross())).append("\nBHYT chi trả: ").append(money(d.insurance())).append("\nBệnh nhân phải trả: ").append(money(d.patientPay())).append("\n\nChi tiết:\n");for(Line l:d.lines())x.append("• ").append(l.description()).append(" — ").append(l.quantity()).append(" × ").append(money(l.unitPrice())).append(" = ").append(money(l.amount())).append("\n");JTextArea area=new JTextArea(x.toString(),18,55);area.setEditable(false);area.setFont(new Font("Segoe UI",Font.PLAIN,13));JOptionPane.showMessageDialog(this,new JScrollPane(area),"Chi tiết phiếu tính phí",JOptionPane.INFORMATION_MESSAGE);}
    private <T>void worker(java.util.concurrent.Callable<T> call,java.util.function.Consumer<T> done){new SwingWorker<T,Void>(){protected T doInBackground()throws Exception{return call.call();}protected void done(){try{done.accept(get());}catch(InterruptedException e){Thread.currentThread().interrupt();warn(e.getMessage());}catch(ExecutionException e){warn(e.getCause()==null?e.getMessage():e.getCause().getMessage());}}}.execute();}
    private void warn(String s){JOptionPane.showMessageDialog(this,s,"Viện phí",JOptionPane.WARNING_MESSAGE);}
    private static DefaultTableModel model(String... c){return new DefaultTableModel(c,0){public boolean isCellEditable(int r,int c){return false;}};}
    private static String money(BigDecimal n){return n==null?"0":n.stripTrailingZeros().toPlainString();}
}
