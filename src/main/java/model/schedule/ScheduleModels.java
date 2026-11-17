package model.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ScheduleModels {
    private ScheduleModels() {}
    public record Page<T>(List<T> rows,long totalCount,int page,int pageSize){
        public Page{rows=List.copyOf(rows);}
        public int totalPages(){return Math.max(1,(int)Math.ceil((double)totalCount/pageSize));}
    }
    public record Doctor(String id,String name,String specialty) {}
    public record Shift(String id,String name,LocalTime start,LocalTime end) {}
    public record Assignment(long id,String doctorId,String doctorName,String specialty,
            String shiftId,String shiftName,LocalDate workDate,LocalTime start,LocalTime end,
            String location,String status,long slotCount,long bookedCount) {}
    public record Draft(String doctorId,String shiftId,LocalDate workDate,String location,String note) {}
}
