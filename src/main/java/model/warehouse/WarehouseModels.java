package model.warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WarehouseModels {
    private WarehouseModels() {
    }

    public record Page<T>(List<T> rows, long totalCount, int page, int pageSize) {
        public Page {
            rows = List.copyOf(rows);
        }

        public int totalPages() {
            return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        }
    }

    public record Stats(
            long medicineCount,
            long batchCount,
            BigDecimal totalStock,
            long expiringLots,
            long expiredLots,
            long outOfStockLots,
            long importedReceipts,
            long exportedReceipts) {
    }

    public record Medicine(
            String id,
            String name,
            String activeIngredient,
            String strength,
            String dosageForm,
            String unit,
            String defaultRoute,
            String manufacturer,
            String country,
            String description,
            String status) {
    }

    public record MedicineDraft(
            String id,
            String name,
            String activeIngredient,
            String strength,
            String dosageForm,
            String unit,
            String defaultRoute,
            String manufacturer,
            String country,
            String description,
            String status) {
    }

    public record BatchFilter(
            String searchText,
            String medicineId,
            String status,
            LocalDate expiryFrom,
            LocalDate expiryTo,
            int page,
            int pageSize) {
    }

    public record InventoryFilter(
            String searchText,
            String stockStatus,
            int page,
            int pageSize) {
    }

    public record ReceiptFilter(
            String searchText,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int pageSize) {
    }

    public record MovementFilter(
            String searchText,
            String movementType,
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int pageSize) {
    }

    public record Batch(
            long id,
            String medicineId,
            String medicineName,
            String batchNumber,
            LocalDate manufactureDate,
            LocalDate expiryDate,
            BigDecimal unitCost,
            BigDecimal stock,
            String location,
            String status,
            BigDecimal confirmedIn,
            BigDecimal confirmedOut) {
    }

    public record InventoryItem(
            String medicineId,
            String medicineName,
            String activeIngredient,
            String strength,
            String unit,
            String medicineStatus,
            BigDecimal totalStock,
            long batchCount,
            long expiringBatchCount,
            long expiredBatchCount,
            long outOfStockBatchCount) {
    }

    public record Receipt(
            long id,
            String number,
            LocalDateTime date,
            String employeeId,
            String employeeName,
            String counterparty,
            String kind,
            Long prescriptionId,
            String status,
            BigDecimal total,
            long medicineLineCount,
            long chemicalLineCount) {
    }

    public record ReceiptLine(
            String medicineId,
            String medicineName,
            long batchId,
            String batchNumber,
            LocalDate manufactureDate,
            LocalDate expiryDate,
            BigDecimal quantity,
            BigDecimal unitCost,
            BigDecimal lineTotal) {
    }

    public record ReceiptDetail(Receipt receipt, List<ReceiptLine> medicineLines) {
        public ReceiptDetail {
            medicineLines = List.copyOf(medicineLines);
        }
    }

    public record StockMovement(
            LocalDateTime date,
            String movementType,
            String receiptNumber,
            String status,
            long batchId,
            String batchNumber,
            String medicineId,
            String medicineName,
            BigDecimal quantity,
            BigDecimal unitCost,
            String employeeName,
            String note) {
    }

    public record BatchDetail(
            Batch batch,
            BigDecimal confirmedIn,
            BigDecimal confirmedOut,
            BigDecimal expectedStockFromHistory,
            boolean reconciled,
            List<StockMovement> movements) {
        public BatchDetail {
            movements = List.copyOf(movements);
        }
    }

    public record ImportLineDraft(
            String medicineId,
            String batchNumber,
            LocalDate manufactureDate,
            LocalDate expiryDate,
            BigDecimal quantity,
            BigDecimal unitCost,
            String location) {
    }

    public record ImportDraft(
            String supplier,
            String note,
            List<ImportLineDraft> lines) {
        public ImportDraft {
            lines = lines == null ? List.of() : List.copyOf(lines);
        }
    }

    public record CreatedReceipt(long id, String number) {
    }

    public record ExportCandidate(
            long prescriptionId,
            long visitId,
            String patientId,
            String patientName,
            String doctorName,
            LocalDateTime prescribedAt,
            String status,
            long medicationCount) {
    }

    public record ExportFilter(String searchText, int page, int pageSize) {
    }

    public record BatchAllocationPreview(
            long batchId,
            String batchNumber,
            LocalDate expiryDate,
            BigDecimal availableQuantity,
            BigDecimal unitCost) {
    }

    public record ExportLinePreview(
            long prescriptionLineId,
            String medicineId,
            String medicineName,
            BigDecimal prescribedQuantity,
            BigDecimal availableQuantity,
            List<BatchAllocationPreview> batches) {
        public ExportLinePreview {
            batches = List.copyOf(batches);
        }
    }

    public record ExportPreview(
            ExportCandidate prescription,
            List<ExportLinePreview> lines,
            boolean canFulfill) {
        public ExportPreview {
            lines = List.copyOf(lines);
        }
    }
}
