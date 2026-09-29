package com.mot;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Every test class extends this. It owns the browser and all evidence
 * collection, so a test class contains nothing but test steps.
 *
 * <p><b>Before</b> each test: launch a browser, start recording video, start the
 * trace.
 * <br><b>After</b> each test: if it failed, keep the screenshot, video and
 * trace. If it passed, throw them away.
 *
 * <p>The key idea: recording has to be switched on <em>before</em> the test
 * runs. You cannot go back and record a video of a failure that already
 * happened. So the framework always records, and deletes on success.
 *
 * <p>Evidence is collected here in {@code @AfterMethod}, not in the listener,
 * because this is the last moment the browser is still open.
 */
public abstract class BaseTest {

    /** Scratch folder Playwright records into before we keep or drop the file. */
    private static final Path RAW_VIDEO_DIR = Paths.get("target", "video-raw");

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    /** The browser page the current test is driving. */
    protected Page page() {
        return page;
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        playwright = Playwright.create();
        browser = launchBrowser();

        // A fresh context per test means empty cookies and storage, so one test
        // can never leak into the next one.
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(Config.VIEWPORT_WIDTH, Config.VIEWPORT_HEIGHT)
                .setRecordVideoDir(RAW_VIDEO_DIR));            // <- video ON
        context.setDefaultTimeout(Config.TIMEOUT_MS);

        context.tracing().start(new Tracing.StartOptions()      // <- trace ON
                .setScreenshots(true)   // frame-by-frame filmstrip
                .setSnapshots(true)     // the DOM at every single action
                .setSources(true));     // the Java line behind each action

        page = context.newPage();

        Log.info("Started '%s' on %s (headless=%s)".formatted(
                testName(result), Config.browser(), Config.headless()));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = testName(result);
        boolean failed = result.getStatus() == ITestResult.FAILURE;

        if (failed) {
            saveScreenshot(testName);
        }
        saveTrace(testName, failed);        // must happen while the context is open
        saveVideoAndClose(testName, failed);  // the video only exists once it closes
    }

    private Browser launchBrowser() {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(Config.headless());

        // These three match the BROWSER choices in the Jenkinsfile.
        return switch (Config.browser()) {
            case "chromium" -> playwright.chromium().launch(options);
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> throw new IllegalArgumentException("Unknown browser: " + Config.browser());
        };
    }

    private void saveScreenshot(String testName) {
        try {
            Path file = artifactPath("screenshots", testName + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));

            ExtentTest test = ExtentReport.currentTest();
            if (test != null) {
                // The path is relative to index.html, which is why the image
                // still shows up after Jenkins publishes the folder.
                test.info("Screenshot at the moment it failed",
                        MediaEntityBuilder.createScreenCaptureFromPath(
                                "screenshots/" + testName + ".png").build());
            }
            Log.info("Screenshot saved: " + file);
        } catch (Exception e) {
            // A problem here must never hide the real test failure.
            Log.info("Could not take a screenshot: " + e.getMessage());
        }
    }

    private void saveTrace(String testName, boolean failed) {
        if (!failed) {
            context.tracing().stop();   // no path given == Playwright discards it
            return;
        }
        Path file = artifactPath("traces", testName + ".zip");
        context.tracing().stop(new Tracing.StopOptions().setPath(file));
        addLink("Playwright trace (npx playwright show-trace)", "traces/" + testName + ".zip");
        Log.info("Trace saved: " + file);
    }

    private void saveVideoAndClose(String testName, boolean failed) {
        // Playwright only finishes writing the .webm when the context closes,
        // so take the handle first, close, then decide what to do with it.
        Video video = page.video();
        context.close();

        try {
            if (failed) {
                Path file = artifactPath("videos", testName + ".webm");
                video.saveAs(file);
                addLink("Execution video", "videos/" + testName + ".webm");
                Log.info("Video saved: " + file);
            }
            video.delete();   // always remove the scratch copy
        } catch (Exception e) {
            Log.info("Could not save the video: " + e.getMessage());
        }

        browser.close();
        playwright.close();
    }

    private void addLink(String label, String relativePath) {
        ExtentTest test = ExtentReport.currentTest();
        if (test != null) {
            test.info("<a href='%s' target='_blank'>%s</a>".formatted(relativePath, label));
        }
    }

    /**
     * Artifacts go <b>inside</b> target/extent-report on purpose. The report can
     * then link them as "screenshots/x.png", so every link still works once
     * Jenkins publishes that one folder.
     */
    private static Path artifactPath(String folder, String fileName) {
        Path dir = ExtentReport.DIR.resolve(folder);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + dir, e);
        }
        return dir.resolve(fileName);
    }

    private static String testName(ITestResult result) {
        return result.getMethod().getMethodName();
    }
}
