package model.dashboard;

import java.math.BigDecimal;

public record DashboardStatistics(
        long appointmentsToday,
        long confirmedAppointmentsToday,
        long cancelledAppointmentsToday,
        long visitsToday,
        long completedVisitsToday,
        long activeVisitsToday,
        long totalPatients,
        long newPatientsThisMonth,
        BigDecimal revenueToday,
        BigDecimal outstandingBalance) {
}
