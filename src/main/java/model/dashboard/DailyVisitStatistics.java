package model.dashboard;

import java.time.LocalDate;

public record DailyVisitStatistics(LocalDate date, long visitCount) {
}
