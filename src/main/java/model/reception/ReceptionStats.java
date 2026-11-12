package model.reception;

public record ReceptionStats(
        long totalToday,
        long waitingForReception,
        long receivedToday,
        long cancelledOrNoShow) {
}
