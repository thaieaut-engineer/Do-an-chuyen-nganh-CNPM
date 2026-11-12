package model.serviceorder;

import java.util.List;

public record ServiceOrderPage(
        List<ServiceOrderListItem> orders,
        long totalCount,
        int page,
        int pageSize) {
    public ServiceOrderPage {
        orders = List.copyOf(orders);
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
