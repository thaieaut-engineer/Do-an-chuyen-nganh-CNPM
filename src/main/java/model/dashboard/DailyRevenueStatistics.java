package model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueStatistics(LocalDate date, BigDecimal revenue) {
}
