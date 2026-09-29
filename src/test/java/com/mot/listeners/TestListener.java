package com.mot.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.mot.report.ExtentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * The reporting hub. Registered once in testng.xml, so no test or page object
 * ever has to think about the report.
 *
 * <p>Responsibilities are kept narrow: create/close the report, create one node
 * per test, record the outcome and the exception, and put the test name into the
 * logging MDC so parallel log lines stay attributable. Capturing the screenshot,
 * video and trace is {@link com.mot.core.BaseTest}'s job, because that runs while
 * the browser is still open.
 */
public class TestListener implements ITestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        ExtentManager.getInstance();
        MDC.put("test", "suite");
        LOGGER.info("===== Suite '{}' started =====", context.getSuite().getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        MDC.put("test", name);

        ExtentTest test = ExtentManager.getInstance()
                .createTest(name, result.getMethod().getDescription());
        String[] groups = result.getMethod().getGroups();
        if (groups.length > 0) {
            test.assignCategory(groups);
        }
        ExtentManager.setCurrentTest(test);

        LOGGER.info("----- TEST STARTED: {} -----", name);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.log(Status.PASS, "Test passed in %d ms"
                    .formatted(result.getEndMillis() - result.getStartMillis()));
        }
        LOGGER.info("----- TEST PASSED: {} -----", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            // Passing the Throwable makes Extent render the full stack trace in a
            // collapsible block, which is what you actually debug from.
            test.fail(result.getThrowable());
        }
        LOGGER.error("----- TEST FAILED: {} -----", result.getMethod().getMethodName(),
                result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.skip(result.getThrowable() == null
                    ? "Skipped (a dependency or configuration method did not pass)"
                    : result.getThrowable().getMessage());
        }
        LOGGER.warn("----- TEST SKIPPED: {} -----", result.getMethod().getMethodName());
    }

    @Override
    public void onFinish(ITestContext context) {
        // Without this flush the HTML file is never written.
        ExtentManager.flush();
        LOGGER.info(
                "===== Suite finished: {} passed, {} failed, {} skipped. Report: target/extent-report/index.html =====",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        MDC.clear();
    }
}
