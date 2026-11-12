package model.medicalrecord;

public record MedicalRecordDraft(
        long maLuotKham,
        String lyDoKham,
        String trieuChung,
        String dauHieuSinhTon,
        String ketQuaLamSang,
        String chanDoan,
        String huongDieuTri) {
}
