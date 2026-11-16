package model.chemical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ChemicalModels {
    private ChemicalModels() {}

    public record Page<T>(List<T> rows, long totalCount, int page, int pageSize) {
        public Page { rows = List.copyOf(rows); }
        public int totalPages() { return Math.max(1, (int) Math.ceil((double) totalCount / pageSize)); }
    }

    public record Stats(long chemicalCount, long batchCount, BigDecimal totalStock,
            long expiringBatches, long expiredBatches, long outOfStockBatches) {}

    public record Chemical(String id, String name, String concentration, String unit,
            String manufacturer, String description, String status) {}

    public record ChemicalDraft(String id, String name, String concentration, String unit,
            String manufacturer, String description, String status) {}

    public record Batch(long id, String chemicalId, String chemicalName, String batchNumber,
            LocalDate manufactureDate, LocalDate expiryDate, BigDecimal stock,
            String location, String status) {}

    public record Receipt(long id, String number, LocalDateTime date, String employeeName,
            String counterparty, String kind, String status, BigDecimal total) {}

    public record Movement(LocalDateTime date, String type, String receiptNumber,
            String chemicalId, String chemicalName, String batchNumber, BigDecimal quantity,
            BigDecimal unitCost, String employeeName, String status, String note) {}

    public record ImportLine(String chemicalId, String batchNumber, LocalDate manufactureDate,
            LocalDate expiryDate, BigDecimal quantity, BigDecimal unitCost, String location) {}

    public record ImportDraft(String supplier, String note, List<ImportLine> lines) {
        public ImportDraft { lines = lines == null ? List.of() : List.copyOf(lines); }
    }

    public record ExportDraft(String chemicalId, String batchNumber, BigDecimal quantity,
            BigDecimal unitCost, String recipient, String note) {}

    public record CreatedReceipt(long id, String number) {}
}
