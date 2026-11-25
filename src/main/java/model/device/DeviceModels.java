package model.device;

import java.time.LocalDate;

public final class DeviceModels {
    private DeviceModels() { }

    public record Device(
            String id,
            String name,
            String typeId,
            String typeName,
            String model,
            String serial,
            String manufacturer,
            LocalDate purchaseDate,
            LocalDate inUseDate,
            String location,
            String status) { }

    public record TypeOption(String id, String name) { }

    public record DeviceDraft(
            String id,
            String name,
            String typeId,
            String model,
            String serial,
            String manufacturer,
            LocalDate purchaseDate,
            LocalDate inUseDate,
            String location,
            String status) { }
}
