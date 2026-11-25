package bus.device;

import bus.auth.AuthorizationBUS;
import dao.device.DeviceDAO;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.device.DeviceModels.Device;
import model.device.DeviceModels.DeviceDraft;
import model.device.DeviceModels.TypeOption;

public final class DeviceBUS {
    private static final List<String> STATUSES = List.of(
            "Tot", "DangSuDung", "CanBaoTri", "DangSuaChua", "NgungSuDung", "ThanhLy");
    private final DeviceDAO dao = new DeviceDAO();
    private final AuthorizationBUS authorization = new AuthorizationBUS();

    public List<Device> find(AuthenticatedSession session, String keyword) {
        access(session);
        return dao.find(normalize(keyword));
    }

    public List<TypeOption> types(AuthenticatedSession session) {
        access(session);
        return dao.types();
    }

    public void save(AuthenticatedSession session, DeviceDraft draft) {
        access(session);
        if (draft == null || normalize(draft.id()).isEmpty()
                || normalize(draft.id()).length() > 20) {
            throw new DeviceException("Mã thiết bị là bắt buộc và tối đa 20 ký tự.");
        }
        if (normalize(draft.name()).isEmpty() || normalize(draft.name()).length() > 150) {
            throw new DeviceException("Tên thiết bị là bắt buộc và tối đa 150 ký tự.");
        }
        if (draft.model() != null && draft.model().length() > 100
                || draft.serial() != null && draft.serial().length() > 100
                || draft.manufacturer() != null && draft.manufacturer().length() > 120
                || draft.location() != null && draft.location().length() > 150) {
            throw new DeviceException("Thông tin thiết bị vượt quá độ dài cho phép.");
        }
        if (draft.purchaseDate() != null && draft.inUseDate() != null
                && draft.inUseDate().isBefore(draft.purchaseDate())) {
            throw new DeviceException("Ngày đưa vào sử dụng không được trước ngày mua.");
        }
        if (!STATUSES.contains(draft.status())) {
            throw new DeviceException("Tình trạng thiết bị không hợp lệ.");
        }
        dao.save(new DeviceDraft(
                normalize(draft.id()), normalize(draft.name()), normalize(draft.typeId()),
                normalize(draft.model()), normalize(draft.serial()), normalize(draft.manufacturer()),
                draft.purchaseDate(), draft.inUseDate(), normalize(draft.location()), draft.status()));
    }

    public void updateStatus(AuthenticatedSession session, String id, String status) {
        access(session);
        if (normalize(id).isEmpty() || !STATUSES.contains(status)) {
            throw new DeviceException("Thông tin tình trạng thiết bị không hợp lệ.");
        }
        dao.updateStatus(normalize(id), status);
    }

    private void access(AuthenticatedSession session) {
        try {
            authorization.requirePageAccess(session, "Thiết bị");
        } catch (SecurityException exception) {
            throw new DeviceException(exception.getMessage(), exception);
        }
    }

    private String normalize(String value) { return value == null ? "" : value.trim(); }
}
