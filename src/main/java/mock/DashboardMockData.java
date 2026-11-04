package mock;

import java.time.LocalDate;
import java.util.List;

public final class DashboardMockData {
    public static final List<Statistic> STATISTICS = List.of(
            new Statistic("TỔNG BỆNH NHÂN", "2.486", "+12,5% so với tháng trước", "patients"),
            new Statistic("LỊCH HẸN HÔM NAY", "38", "8 lịch đang chờ xác nhận", "calendar"),
            new Statistic("LƯỢT KHÁM HÔM NAY", "26", "19 lượt đã hoàn tất", "medical"),
            new Statistic("BÁC SĨ ĐANG LÀM VIỆC", "12", "Trên 4 chuyên khoa", "doctor"),
            new Statistic("DOANH THU HÔM NAY", "18,6 triệu", "+8,2% so với hôm qua", "wallet"),
            new Statistic("HÓA ĐƠN CHƯA THANH TOÁN", "7", "Tổng phải thu 4,25 triệu", "invoice")
    );

    public static final List<Appointment> APPOINTMENTS = List.of(
            new Appointment("08:30", "Nguyễn Minh Anh", "BS. Trần Thu Hà", "Nội tổng quát", "Đã xác nhận"),
            new Appointment("09:00", "Trần Hoàng Nam", "BS. Lê Quang Huy", "Tim mạch", "Đã đến"),
            new Appointment("09:30", "Phạm Ngọc Lan", "BS. Nguyễn Mai Linh", "Nhi khoa", "Chờ xác nhận"),
            new Appointment("10:00", "Đỗ Gia Bảo", "BS. Trần Thu Hà", "Nội tổng quát", "Đang khám"),
            new Appointment("10:30", "Lê Khánh Vy", "BS. Vũ Đức Long", "Da liễu", "Đã xác nhận"),
            new Appointment("11:00", "Bùi Thanh Tùng", "BS. Lê Quang Huy", "Tim mạch", "Chờ xác nhận")
    );

    public static final List<Activity> ACTIVITIES = List.of(
            new Activity("Lịch hẹn mới", "Nguyễn Minh Anh · Nội tổng quát", "08:24",
                    "calendar", new java.awt.Color(0x02B5AB)),
            new Activity("Đã tiếp nhận bệnh nhân", "Trần Hoàng Nam · BN-2026-1284", "08:15",
                    "patients", new java.awt.Color(28, 145, 132)),
            new Activity("Hoàn tất lượt khám", "Phạm Ngọc Lan · BS. Nguyễn Mai Linh", "07:58",
                    "doctor", new java.awt.Color(123, 92, 176)),
            new Activity("Thanh toán hóa đơn", "HD-2026-0831 · 850.000 đ", "07:42",
                    "invoice", new java.awt.Color(202, 139, 54)),
            new Activity("Cập nhật lịch làm việc", "Khoa Nội tổng quát · Ca sáng", "07:30",
                    "settings", new java.awt.Color(94, 112, 132))
    );

    public static final List<AppointmentStatus> APPOINTMENT_STATUSES = List.of(
            new AppointmentStatus("Đã xác nhận", 15, new java.awt.Color(0x02B5AB)),
            new AppointmentStatus("Đã đến / Đang khám", 11, new java.awt.Color(28, 145, 132)),
            new AppointmentStatus("Chờ xác nhận", 8, new java.awt.Color(202, 139, 54)),
            new AppointmentStatus("Hoàn tất", 4, new java.awt.Color(123, 92, 176))
    );

    public static final List<ChartPoint> PATIENT_TREND = List.of(
            new ChartPoint("T1", 42), new ChartPoint("T2", 55), new ChartPoint("T3", 49),
            new ChartPoint("T4", 68), new ChartPoint("T5", 61), new ChartPoint("T6", 82),
            new ChartPoint("T7", 75), new ChartPoint("T8", 96), new ChartPoint("T9", 88),
            new ChartPoint("T10", 112), new ChartPoint("T11", 106), new ChartPoint("T12", 128)
    );

    public static final List<ChartPoint> REVENUE_TREND = List.of(
            new ChartPoint("T1", 48), new ChartPoint("T2", 57), new ChartPoint("T3", 52),
            new ChartPoint("T4", 65), new ChartPoint("T5", 72), new ChartPoint("T6", 68),
            new ChartPoint("T7", 84), new ChartPoint("T8", 79), new ChartPoint("T9", 92),
            new ChartPoint("T10", 88), new ChartPoint("T11", 106), new ChartPoint("T12", 118)
    );

    private DashboardMockData() {
    }

    public static String todayLabel() {
        return LocalDate.now().toString();
    }

    public static final class Statistic {
        public final String title;
        public final String value;
        public final String detail;
        public final String icon;

        private Statistic(String title, String value, String detail, String icon) {
            this.title = title;
            this.value = value;
            this.detail = detail;
            this.icon = icon;
        }
    }

    public static final class Appointment {
        public final String time;
        public final String patient;
        public final String doctor;
        public final String specialty;
        public final String status;

        private Appointment(String time, String patient, String doctor, String specialty, String status) {
            this.time = time;
            this.patient = patient;
            this.doctor = doctor;
            this.specialty = specialty;
            this.status = status;
        }
    }

    public static final class Activity {
        public final String title;
        public final String detail;
        public final String time;
        public final String icon;
        public final java.awt.Color color;

        private Activity(String title, String detail, String time, String icon, java.awt.Color color) {
            this.title = title;
            this.detail = detail;
            this.time = time;
            this.icon = icon;
            this.color = color;
        }
    }

    public static final class AppointmentStatus {
        public final String label;
        public final int count;
        public final java.awt.Color color;

        private AppointmentStatus(String label, int count, java.awt.Color color) {
            this.label = label;
            this.count = count;
            this.color = color;
        }
    }

    public static final class ChartPoint {
        public final String label;
        public final int value;

        private ChartPoint(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }
}
