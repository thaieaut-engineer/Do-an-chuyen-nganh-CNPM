package model.device;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class MaintenanceModels {
    private MaintenanceModels() { }
    public record Schedule(Long id,String deviceId,String deviceName,LocalDate expectedDate,LocalDate completedDate,String content,String vendor,String employeeId,BigDecimal cost,String status) { }
    public record ScheduleDraft(Long id,String deviceId,LocalDate expectedDate,LocalDate completedDate,String content,String vendor,String employeeId,BigDecimal cost,String status) { }
    public record Repair(Long id,String deviceId,String deviceName,LocalDate reportedDate,String description,LocalDate repairedDate,String vendor,BigDecimal cost,String status,String employeeId,String note) { }
    public record RepairDraft(Long id,String deviceId,String description,LocalDate repairedDate,String vendor,BigDecimal cost,String status,String employeeId,String note) { }
}
