package model.prescription;

import java.util.List;

public record PrescriptionDraft(
        long maLuotKham,
        String loiDan,
        List<PrescriptionLineDraft> lines) {
    public PrescriptionDraft {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
