package model.visit;

import java.util.List;

public record VisitPage(
        List<VisitListItem> visits,
        long totalCount,
        int page,
        int pageSize) {
    public VisitPage {
        visits = List.copyOf(visits);
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
