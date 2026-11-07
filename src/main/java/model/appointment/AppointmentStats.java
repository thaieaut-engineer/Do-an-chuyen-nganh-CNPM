package model.appointment;

public record AppointmentStats(
        long totalToday,
        long pendingToday,
        long confirmedToday,
        long cancelledToday) {
}
