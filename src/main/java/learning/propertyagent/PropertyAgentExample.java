package learning.propertyagent;

import java.time.LocalDate;

import static learning.propertyagent.PropertyWork.InspectionReminder;
import static learning.propertyagent.PropertyWork.MaintenanceRequest;
import static learning.propertyagent.PropertyWork.TenantDocument;

public final class PropertyAgentExample {
    private PropertyAgentExample() {}

    public static void main(String[] args) throws Exception {
        ServiceConfig config = ServiceConfig.fromEnvironment();
        PropertyAgentLoopService service = new PropertyAgentLoopService(new InfraiFailureReporter(config));
        MaintenanceRequest request = new MaintenanceRequest("maint-1042", "unit-3B", "Water stain near the kitchen window");
        TenantDocument document = new TenantDocument("lease-77", "tenant-18", TenantDocument.Status.MISSING);
        InspectionReminder reminder = new InspectionReminder("inspect-1042", LocalDate.now().plusDays(2),
                InspectionReminder.State.WAITING_FOR_AGENT);

        PropertyWork.LoopResult result = service.process(request, document, reminder);
        System.out.printf("request=%s reminder=%s failureCaptured=%s%n",
                result.requestId(), result.reminder().state(), result.failureCaptured());
    }
}
