package model.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class InsuranceModels {
    private InsuranceModels() { }
    public record Card(long id,String patientId,String patientName,String cardNumber,String nameOnCard,
            LocalDate birthDate,String facility,LocalDate fromDate,LocalDate toDate,BigDecimal coverage,String status) { }
    public record PatientOption(String id,String name) { @Override public String toString(){return id+" - "+name;} }
    public record CardDraft(long id,String patientId,String cardNumber,String nameOnCard,LocalDate birthDate,
            String facility,LocalDate fromDate,LocalDate toDate,BigDecimal coverage,String status) { }
}
