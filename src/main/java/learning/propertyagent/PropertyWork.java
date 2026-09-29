package learning.propertyagent;

import java.time.LocalDate;

public final class PropertyWork {
    private PropertyWork() {}

    public record MaintenanceRequest(String requestId, String unitId, String summary) {}

    public record TenantDocument(String documentId, String tenantId, Status status) {
        public enum Status { VERIFIED, MISSING }
    }

    public record InspectionReminder(String reminderId, LocalDate dueOn, State state) {
        public enum State { WAITING_FOR_AGENT, READY_FOR_MANAGER_REVIEW }

        InspectionReminder readyForManagerReview() {
            return new InspectionReminder(reminderId, dueOn, State.READY_FOR_MANAGER_REVIEW);
        }
    }

    public record LoopResult(String requestId, InspectionReminder reminder, boolean failureCaptured) {}
}
