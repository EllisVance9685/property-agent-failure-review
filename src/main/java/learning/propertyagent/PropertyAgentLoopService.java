package learning.propertyagent;

import java.io.IOException;

public final class PropertyAgentLoopService {
    private final FailureReporter failures;

    public PropertyAgentLoopService(FailureReporter failures) {
        this.failures = failures;
    }

    public PropertyWork.LoopResult process(PropertyWork.MaintenanceRequest request,
                                           PropertyWork.TenantDocument document,
                                           PropertyWork.InspectionReminder reminder)
            throws IOException, InterruptedException {
        try {
            classifyForInspection(request, document);
            return new PropertyWork.LoopResult(request.requestId(), reminder, false);
        } catch (AgentStepException failure) {
            PropertyWork.InspectionReminder review = reminder.readyForManagerReview();
            failures.capture(request, "inspection-document-check", failure);
            return new PropertyWork.LoopResult(request.requestId(), review, true);
        }
    }

    private void classifyForInspection(PropertyWork.MaintenanceRequest request,
                                       PropertyWork.TenantDocument document) throws AgentStepException {
        if (request.summary().isBlank()) throw new AgentStepException("Maintenance summary is empty");
        if (document.status() == PropertyWork.TenantDocument.Status.MISSING) {
            throw new AgentStepException("Tenant document is missing for inspection planning");
        }
    }

    private static final class AgentStepException extends Exception {
        private static final long serialVersionUID = 1L;

        AgentStepException(String message) { super(message); }
    }
}
