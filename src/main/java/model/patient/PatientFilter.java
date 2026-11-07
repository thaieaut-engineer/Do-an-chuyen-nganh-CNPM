package model.patient;

public record PatientFilter(
        String searchText,
        String gender,
        String status,
        int page,
        int pageSize) {
}
