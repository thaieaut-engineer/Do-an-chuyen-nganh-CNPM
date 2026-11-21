package model.staff;
import java.time.LocalDate;
public final class StaffModels {
    private StaffModels(){}
    public record Staff(String id,String name,LocalDate birth,String gender,String phone,String email,String address,LocalDate startDate,String status,String departmentId,String departmentName,String titleId,String titleName,boolean doctor,String degree,String specialty,String specialtyId,String license){ }
    public record StaffDraft(String id,String name,LocalDate birth,String gender,String phone,String email,String address,LocalDate startDate,String status,String departmentId,String titleId,String degree,String specialty,String specialtyId,String license){ }
}
