package com.mot;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG calls these methods as the run progresses; this class turns them into
 * report entries.
 *
 * <p>Registered once in testng.xml. That single registration is why no test
 * class contains any reporting code.
 *
 * <p>The order TestNG fires things in:
 * <pre>
 *   @BeforeMethod  ->  onTestStart  ->  the test  ->  onTestSuccess/onTestFailure  ->  @AfterMethod
 * </pre>
 * So the stack trace is attached here, and the screenshot/video/trace are added
 * a moment later by {@link BaseTest#tearDown}, while the browser is still open.
 */
public class TestListener implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        ExtentReport.start();
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentReport.startTest(result.getMethod().getMethodName(),
                result.getMethod().getDescription());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentReport.currentTest().pass("Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Log.fail("TEST FAILED: " + result.getMethod().getMethodName());
        ExtentReport.currentTest().fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentReport.addSkipped(result.getMethod().getMethodName(), result.getThrowable());
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReport.flush();   // this is what actually writes index.html
    }
}
