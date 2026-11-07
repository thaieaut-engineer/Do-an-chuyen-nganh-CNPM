package model.dashboard;

import java.time.LocalDateTime;

public record UpcomingAppointment(
        long appointmentId,
        LocalDateTime appointmentTime,
        String patientName,
        String doctorName,
        String specialty,
        String status) {
}
