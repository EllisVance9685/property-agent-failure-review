package learning.propertyagent;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public final class PropertyAgentLoopServiceTest {
    public static void main(String[] args) throws Exception {
        AtomicInteger captures = new AtomicInteger();
        FailureReporter reporter = (request, step, failure) -> {
            require(request.requestId().equals("maint-42"), "capture keeps the maintenance id");
            require(step.equals("inspection-document-check"), "capture identifies the failed agent step");
            captures.incrementAndGet();
        };
        PropertyAgentLoopService service = new PropertyAgentLoopService(reporter);
        PropertyWork.MaintenanceRequest request = new PropertyWork.MaintenanceRequest("maint-42", "unit-2A", "Check boiler pressure");
        PropertyWork.InspectionReminder reminder = new PropertyWork.InspectionReminder("inspect-42",
                LocalDate.of(2026, 10, 2), PropertyWork.InspectionReminder.State.WAITING_FOR_AGENT);

        PropertyWork.LoopResult result = service.process(request,
                new PropertyWork.TenantDocument("lease-9", "tenant-7", PropertyWork.TenantDocument.Status.MISSING), reminder);

        require(result.reminder().state() == PropertyWork.InspectionReminder.State.READY_FOR_MANAGER_REVIEW,
                "a missing document preserves the reminder for manager review");
        require(result.failureCaptured(), "the result exposes that capture completed");
        require(captures.get() == 1, "the failed step is captured once");
        System.out.println("PASS missing document moves reminder to manager review and captures one failure");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
