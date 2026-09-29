package learning.propertyagent;

import java.io.IOException;

@FunctionalInterface
public interface FailureReporter {
    void capture(PropertyWork.MaintenanceRequest request, String step, Exception exception) throws IOException, InterruptedException;
}
