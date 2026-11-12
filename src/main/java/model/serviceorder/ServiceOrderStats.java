package model.serviceorder;

public record ServiceOrderStats(
        long total,
        long orderedToday,
        long pending,
        long completed) {
}
