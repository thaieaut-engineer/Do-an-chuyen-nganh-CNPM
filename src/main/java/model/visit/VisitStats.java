package model.visit;

public record VisitStats(
        long totalToday,
        long waiting,
        long inProgress,
        long completed) {
}
