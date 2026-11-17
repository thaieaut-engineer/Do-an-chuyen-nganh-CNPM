package model.fee;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class FeeModels {
    private FeeModels() {}
    public record Page<T>(List<T> rows,long totalCount,int page,int pageSize){
        public Page{rows=List.copyOf(rows);}
        public int totalPages(){return Math.max(1,(int)Math.ceil((double)totalCount/pageSize));}
    }
    public record Stats(long eligibleVisits,long calculatedBills,BigDecimal todayRevenue){}
    public record Visit(long id,String patientId,String patientName,String doctorName,
            LocalDateTime receivedAt,String visitStatus,String feeStatus,BigDecimal total,long lineCount){}
    public record Line(String type,String description,BigDecimal quantity,BigDecimal unitPrice,
            BigDecimal amount,boolean covered,BigDecimal coverageRate){}
    public record Detail(long id,long visitId,String patientId,String patientName,
            LocalDateTime calculatedAt,int calculationNo,BigDecimal gross,BigDecimal discount,
            BigDecimal insurance,BigDecimal patientPay,String status,List<Line> lines){
        public Detail{lines=List.copyOf(lines);}
    }
}
