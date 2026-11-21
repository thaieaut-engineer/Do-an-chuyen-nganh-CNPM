package bus.insurance;

import bus.auth.AuthorizationBUS;
import dao.insurance.InsuranceDAO;
import dao.insurance.InsuranceDAOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.insurance.InsuranceModels.Card;
import model.insurance.InsuranceModels.CardDraft;
import model.insurance.InsuranceModels.PatientOption;

public final class InsuranceBUS {
    private final InsuranceDAO dao = new InsuranceDAO();
    private final AuthorizationBUS auth = new AuthorizationBUS();

    public List<Card> find(AuthenticatedSession s, String q, String status) {
        access(s);
        return run(() -> dao.find(q, status));
    }

    public List<PatientOption> patients(AuthenticatedSession s) {
        access(s);
        return run(dao::patients);
    }

    public void save(AuthenticatedSession s, CardDraft d) {
        access(s);
        requirePermission(s, "BN_SUA");
        if (d == null || d.patientId().isBlank() || d.cardNumber().isBlank()) {
            throw new InsuranceException("Bệnh nhân và số thẻ BHYT là bắt buộc.");
        }
        if (d.toDate() != null && d.fromDate() != null && d.toDate().isBefore(d.fromDate())) {
            throw new InsuranceException("Ngày hết hạn phải sau ngày bắt đầu.");
        }
        if (d.coverage() == null || d.coverage().compareTo(BigDecimal.ZERO) < 0
                || d.coverage().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InsuranceException("Mức hưởng phải từ 0 đến 100%.");
        }
        run(() -> { dao.save(d); return null; });
    }

    public void refreshExpired(AuthenticatedSession s, Card c) {
        access(s);
        if (c != null && c.toDate() != null && c.toDate().isBefore(LocalDate.now())) {
            run(() -> { dao.updateStatus(c.id(), "HetHan"); return null; });
        }
    }

    private void access(AuthenticatedSession s) {
        try { auth.requirePageAccess(s, "Bảo hiểm y tế"); }
        catch (SecurityException e) { throw new InsuranceException(e.getMessage(), e); }
    }

    private void requirePermission(AuthenticatedSession s, String code) {
        try { auth.requirePermission(s, code); }
        catch (SecurityException e) { throw new InsuranceException(e.getMessage(), e); }
    }

    private <T> T run(java.util.concurrent.Callable<T> x) {
        try { return x.call(); }
        catch (InsuranceDAOException e) { throw new InsuranceException(e.getMessage(), e); }
        catch (Exception e) { throw new InsuranceException(e.getMessage(), e); }
    }
}
