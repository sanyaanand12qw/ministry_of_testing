package com.mot.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import com.mot.config.ConfigReader;
import com.mot.utils.ArtifactUtil;
import com.mot.utils.Log;

import java.nio.file.Path;

/**
 * Builds and destroys the browser stack for a single test method, and decides
 * whether the video and trace of that test are kept.
 *
 * <p>The capture rules come from config ({@code capture.video} / {@code capture.trace}):
 * {@code always}, {@code never}, or {@code retain-on-failure} (the default).
 * Recording itself always has to be switched on <em>before</em> the test runs —
 * you cannot retro-fit a video after a failure — so the framework records
 * eagerly and throws the artifact away on success.
 */
public final class PlaywrightFactory {

    private static final String RETAIN_ON_FAILURE = "retain-on-failure";
    private static final String ALWAYS = "always";

    private PlaywrightFactory() {
    }

    /**
     * Starts a driver, browser, fresh context and page for {@code testName},
     * and switches on tracing / video recording per configuration.
     */
    public static Page createPage(String testName) {
        Playwright playwright = Playwright.create();
        DriverManager.setPlaywright(playwright);

        Browser browser = launchBrowser(playwright);
        DriverManager.setBrowser(browser);

        // A brand-new context per test = empty cookies, storage and cache.
        // This is the cheap isolation that stops one test leaking into the next.
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(ConfigReader.viewportWidth(), ConfigReader.viewportHeight());

        if (videoEnabled()) {
            contextOptions
                    .setRecordVideoDir(ArtifactUtil.rawVideoDir(testName))
                    .setRecordVideoSize(ConfigReader.viewportWidth(), ConfigReader.viewportHeight());
        }

        BrowserContext context = browser.newContext(contextOptions);
        context.setDefaultTimeout(ConfigReader.defaultTimeout());
        context.setDefaultNavigationTimeout(ConfigReader.navigationTimeout());
        DriverManager.setContext(context);

        if (traceEnabled()) {
            context.tracing().start(new Tracing.StartOptions()
                    .setTitle(testName)
                    .setScreenshots(true)   // frame-by-frame filmstrip
                    .setSnapshots(true)     // live DOM snapshot per action
                    .setSources(true));     // the Java source line behind each action
        }

        Page page = context.newPage();
        DriverManager.setPage(page);

        Log.debug("Browser stack ready for '%s' [browser=%s, headless=%s, video=%s, trace=%s]"
                .formatted(testName, ConfigReader.browser(), ConfigReader.headless(),
                        ConfigReader.videoMode(), ConfigReader.traceMode()));
        return page;
    }

    private static Browser launchBrowser(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(ConfigReader.headless())
                .setSlowMo(ConfigReader.slowMo());

        String browser = ConfigReader.browser();
        return switch (browser) {
            case "chromium" -> playwright.chromium().launch(options);
            case "chrome" -> playwright.chromium().launch(options.setChannel("chrome"));
            case "edge", "msedge" -> playwright.chromium().launch(options.setChannel("msedge"));
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit", "safari" -> playwright.webkit().launch(options);
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
    }

    /**
     * Stops tracing. Must be called <b>before</b> the context is closed.
     *
     * @return the written trace zip, or null if the trace was discarded
     */
    public static Path stopTracing(String testName, boolean testFailed) {
        BrowserContext context = DriverManager.getContext();
        if (context == null || !traceEnabled()) {
            return null;
        }
        boolean keep = ALWAYS.equalsIgnoreCase(ConfigReader.traceMode()) || testFailed;
        if (!keep) {
            context.tracing().stop();   // no path == Playwright throws the buffer away
            return null;
        }
        Path trace = ArtifactUtil.tracePath(testName);
        context.tracing().stop(new Tracing.StopOptions().setPath(trace));
        return trace;
    }

    /**
     * Closes page, context, browser and driver, then keeps or drops the video.
     *
     * <p>Playwright only finishes writing the {@code .webm} when the context
     * closes, which is why the {@link Video} handle is grabbed first and acted on
     * afterwards.
     *
     * @return the retained video file, or null if there is none
     */
    public static Path closeAndCollectVideo(String testName, boolean testFailed) {
        Page page = DriverManager.getPage();
        BrowserContext context = DriverManager.getContext();
        Browser browser = DriverManager.getBrowser();
        Playwright playwright = DriverManager.getPlaywright();

        Video video = (page != null && videoEnabled()) ? page.video() : null;
        Path retained = null;

        try {
            if (context != null) {
                context.close();        // flushes the video to the scratch folder
            }
            if (video != null) {
                boolean keep = ALWAYS.equalsIgnoreCase(ConfigReader.videoMode()) || testFailed;
                if (keep) {
                    retained = ArtifactUtil.videoPath(testName);
                    video.saveAs(retained);
                }
                video.delete();         // always clear the scratch copy
            }
        } catch (RuntimeException e) {
            // Never let cleanup mask the real test failure.
            Log.warn("Problem while collecting the video for '%s': %s".formatted(testName, e.getMessage()));
        } finally {
            closeQuietly(browser, playwright);
            ArtifactUtil.deleteQuietly(ArtifactUtil.RAW_VIDEO_DIR.resolve(ArtifactUtil.safe(testName)));
            DriverManager.unload();
        }
        return retained;
    }

    private static void closeQuietly(Browser browser, Playwright playwright) {
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (RuntimeException ignored) {
            // already gone
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException ignored) {
            // already gone
        }
    }

    private static boolean videoEnabled() {
        String mode = ConfigReader.videoMode();
        return ALWAYS.equalsIgnoreCase(mode) || RETAIN_ON_FAILURE.equalsIgnoreCase(mode);
    }

    private static boolean traceEnabled() {
        String mode = ConfigReader.traceMode();
        return ALWAYS.equalsIgnoreCase(mode) || RETAIN_ON_FAILURE.equalsIgnoreCase(mode);
    }
}
