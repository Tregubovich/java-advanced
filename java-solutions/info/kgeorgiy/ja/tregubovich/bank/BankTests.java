package info.kgeorgiy.ja.tregubovich.bank;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class BankTests {
    static void main(final String... args) {
        final LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("info.kgeorgiy.ja.tregubovich.bank"))
                .build();

        final Launcher launcher = LauncherFactory.create();

        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);

        launcher.execute(request);

        final TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new java.io.PrintWriter(System.out));

        summary.getFailures().forEach(failure -> {
            System.out.println("==== FAILURE ====");
            System.out.println("Test: " + failure.getTestIdentifier().getDisplayName());
            failure.getException().printStackTrace(System.out);
        });

        if (summary.getTotalFailureCount() > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}
