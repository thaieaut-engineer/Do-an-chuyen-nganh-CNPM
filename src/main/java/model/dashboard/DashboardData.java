package model.dashboard;

import java.util.List;
import java.time.LocalDate;

public record DashboardData(
        LocalDate asOfDate,
        DashboardStatistics statistics,
        List<DailyVisitStatistics> dailyVisits,
        List<DailyRevenueStatistics> dailyRevenue,
        List<UpcomingAppointment> upcomingAppointments) {

    public DashboardData {
        dailyVisits = List.copyOf(dailyVisits);
        dailyRevenue = List.copyOf(dailyRevenue);
        upcomingAppointments = List.copyOf(upcomingAppointments);
    }
}
