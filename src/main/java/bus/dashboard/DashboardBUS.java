package bus.dashboard;

import bus.auth.AuthorizationBUS;
import dao.dashboard.DashboardDAO;
import dao.dashboard.DashboardDAOException;
import model.auth.AuthenticatedSession;
import model.dashboard.DashboardData;
import model.dashboard.DashboardStatistics;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DashboardBUS {
    private static final Logger LOGGER = Logger.getLogger(DashboardBUS.class.getName());
    private final DashboardDAO dashboardDAO;
    private final AuthorizationBUS authorizationBUS;

    public DashboardBUS() {
        this(new DashboardDAO(), new AuthorizationBUS());
    }

    DashboardBUS(DashboardDAO dashboardDAO, AuthorizationBUS authorizationBUS) {
        this.dashboardDAO = dashboardDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public DashboardData getDashboardData(AuthenticatedSession session)
            throws DashboardException {
        authorizationBUS.requirePageAccess(session, "Tổng quan");
        try {
            DashboardData data = dashboardDAO.getDashboardData();
            validate(data);
            return data;
        } catch (DashboardDAOException exception) {
            LOGGER.log(Level.SEVERE, "Unable to load clinic Dashboard data.", exception);
            throw new DashboardException(
                    "Không thể tải dữ liệu tổng quan. Vui lòng kiểm tra kết nối cơ sở dữ liệu.",
                    exception);
        }
    }

    private void validate(DashboardData data) throws DashboardException {
        if (data == null || data.statistics() == null
                || data.dailyVisits().size() != 7
                || data.dailyRevenue().size() != 7
                || data.upcomingAppointments().size() > 5) {
            throw new DashboardException(
                    "Dữ liệu tổng quan nhận được không đầy đủ. Vui lòng làm mới lại.", null);
        }

        DashboardStatistics statistics = data.statistics();
        if (statistics.appointmentsToday() < 0
                || statistics.confirmedAppointmentsToday() < 0
                || statistics.cancelledAppointmentsToday() < 0
                || statistics.visitsToday() < 0
                || statistics.completedVisitsToday() < 0
                || statistics.activeVisitsToday() < 0
                || statistics.totalPatients() < 0
                || statistics.newPatientsThisMonth() < 0
                || statistics.revenueToday() == null
                || statistics.outstandingBalance() == null
                || statistics.revenueToday().signum() < 0
                || statistics.outstandingBalance().signum() < 0) {
            throw new DashboardException(
                    "Dữ liệu tổng quan nhận được không hợp lệ. Vui lòng làm mới lại.", null);
        }
    }
}
