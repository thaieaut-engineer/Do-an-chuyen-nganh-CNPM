package bus.schedule;

import bus.auth.AuthorizationBUS;
import dao.schedule.ScheduleDAO;
import dao.schedule.ScheduleDAOException;
import java.time.LocalDate;
import model.auth.AuthenticatedSession;
import model.schedule.ScheduleModels.*;

public final class ScheduleBUS {
    private final ScheduleDAO dao=new ScheduleDAO();private final AuthorizationBUS authorization=new AuthorizationBUS();
    public java.util.List<Doctor> doctors(AuthenticatedSession s)throws ScheduleException{read(s);return query(dao::doctors);}
    public java.util.List<Shift> shifts(AuthenticatedSession s)throws ScheduleException{read(s);return query(dao::shifts);}
    public Page<Assignment> search(AuthenticatedSession s,String q,LocalDate from,LocalDate to,int page,int size)throws ScheduleException{read(s);if(page<0||size<1||size>100)throw new ScheduleException("Thông tin phân trang không hợp lệ.");return query(()->dao.assignments(q,from,to,page,size));}
    public void assign(AuthenticatedSession s,Draft d)throws ScheduleException{write(s);if(d==null||d.doctorId()==null||d.shiftId()==null||d.workDate()==null)throw new ScheduleException("Vui lòng nhập đủ bác sĩ, ca và ngày làm.");if(d.workDate().isBefore(LocalDate.now()))throw new ScheduleException("Không thể phân công cho ngày đã qua.");if(d.location()!=null&&d.location().length()>150)throw new ScheduleException("Địa điểm vượt quá 150 ký tự.");query(()->{dao.assign(d,s.getEmployeeId());return null;});}
    public void cancel(AuthenticatedSession s,long id)throws ScheduleException{write(s);if(id<=0)throw new ScheduleException("Mã phân công không hợp lệ.");query(()->{dao.cancel(id);return null;});}
    private void read(AuthenticatedSession s)throws ScheduleException{access(s,"NHANSU_XEM");}
    private void write(AuthenticatedSession s)throws ScheduleException{access(s,"NHANSU_SUA");}
    private void access(AuthenticatedSession s,String permission)throws ScheduleException{if(s==null||!s.isActive())throw new ScheduleException("Phiên làm việc không hợp lệ.");try{authorization.requirePageAccess(s,"Lịch làm việc");authorization.requirePermission(s,permission);}catch(SecurityException e){throw new ScheduleException(e.getMessage(),e);}}
    private <T>T query(java.util.concurrent.Callable<T> c)throws ScheduleException{try{return c.call();}catch(ScheduleDAOException e){throw new ScheduleException(e.getMessage(),e);}catch(Exception e){throw new ScheduleException(e.getMessage(),e);}}
}
