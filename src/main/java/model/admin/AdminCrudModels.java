package model.admin;

import java.time.LocalDate;

public final class AdminCrudModels {
    private AdminCrudModels() { }

    public record Evaluation(long id, String employeeId, String employeeName, String reviewerId,
            String reviewerName, String period, LocalDate date, java.math.BigDecimal score, String comment) { }
    public record EvaluationDraft(Long id, String employeeId, String period, LocalDate date,
            java.math.BigDecimal score, String comment) { }
    public record Config(String id, String value, String type, String description) { }
    public record Notification(long id, String employeeId, String patientId, String title,
            String content, String type, String status, String channel) { }
    public record NotificationDraft(Long id, String employeeId, String patientId, String title,
            String content, String type, String status, String channel) { }
}
