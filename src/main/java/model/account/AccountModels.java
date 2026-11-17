package model.account;

import java.time.LocalDateTime;
import java.util.List;

public final class AccountModels {
    private AccountModels() { }

    public record AccountRow(long id, String username, String employeeId,
            String employeeName, String roles, String status,
            LocalDateTime createdAt, LocalDateTime lastLoginAt) { }

    public record RoleOption(String id, String name, String status) {
        @Override public String toString() { return name + " (" + id + ")"; }
    }

    public record EmployeeOption(String id, String name, String status) {
        @Override public String toString() { return id + " - " + name; }
    }

    public record AccountDraft(long id, String username, String employeeId,
            String status, List<String> roleIds, char[] password) {
        public AccountDraft {
            roleIds = List.copyOf(roleIds);
        }
    }
}
