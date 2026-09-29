package com.mot.core;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.microsoft.playwright.Page;
import com.mot.report.ExtentManager;
import com.mot.utils.ArtifactUtil;
import com.mot.utils.Log;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Path;

/**
 * Every test class extends this. It owns the per-test browser lifecycle and all
 * evidence collection, so a test class contains nothing but test logic.
 *
 * <p>Evidence is deliberately gathered in {@code @AfterMethod} rather than in the
 * listener: this is the last point at which the page is still alive, so the
 * screenshot, trace and video can all be captured here regardless of the order
 * TestNG happens to fire its listener callbacks in.
 */
public abstract class BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        PlaywrightFactory.createPage(testName(result));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = testName(result);
        boolean failed = result.getStatus() == ITestResult.FAILURE;

        if (failed) {
            captureScreenshot(testName);
        }

        // Order matters: tracing must stop while the context is open, and the
        // video only lands on disk once the context has closed.
        Path trace = PlaywrightFactory.stopTracing(testName, failed);
        Path video = PlaywrightFactory.closeAndCollectVideo(testName, failed);

        attachLink("Playwright trace (open with: npx playwright show-trace)", trace);
        attachLink("Execution video", video);
    }

    /** The page the current test is driving. */
    protected Page page() {
        return DriverManager.getPage();
    }

    private void captureScreenshot(String testName) {
        Page page = DriverManager.getPage();
        if (page == null || page.isClosed()) {
            Log.warn("No live page available, screenshot skipped for '%s'".formatted(testName));
            return;
        }
        try {
            Path file = ArtifactUtil.screenshotPath(testName);
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));

            ExtentTest test = ExtentManager.getCurrentTest();
            if (test != null) {
                test.info("Screenshot at the point of failure",
                        MediaEntityBuilder.createScreenCaptureFromPath(
                                ArtifactUtil.relativeToReport(file)).build());
            }
            Log.debug("Screenshot written to " + file);
        } catch (Exception e) {
            Log.warn("Could not capture a screenshot for '%s': %s".formatted(testName, e.getMessage()));
        }
    }

    private void attachLink(String label, Path artifact) {
        if (artifact == null) {
            return;
        }
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.info("<a href='%s' target='_blank'>%s</a>"
                    .formatted(ArtifactUtil.relativeToReport(artifact), label));
        }
        Log.debug("%s retained at %s".formatted(label, artifact));
    }

    private String testName(ITestResult result) {
        return result.getMethod().getMethodName();
    }
}
