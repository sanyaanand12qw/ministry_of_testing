package com.mot;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Owns the one HTML report for the whole run.
 *
 * <p>Two things live here: the report itself, and a pointer to the test node
 * currently being written into. {@link TestListener} sets that pointer before
 * each test, so {@link Log} and {@link BaseTest} always know where to write.
 *
 * <p>Nothing is thread-safe here. That is fine because the suite runs one test
 * at a time (see testng.xml). Running tests in parallel would need these two
 * fields wrapped in ThreadLocal.
 */
public final class ExtentReport {

    /** Screenshots, videos and traces are written inside this folder too. */
    public static final Path DIR = Paths.get("target", "extent-report");

    private static ExtentReports extent;
    private static ExtentTest currentTest;

    /** Called once, at the start of the suite. */
    public static void start() {
        ExtentSparkReporter spark = new ExtentSparkReporter(DIR.resolve("index.html").toString());

        // Offline mode writes Extent's own CSS and JavaScript into the report
        // folder instead of linking them from a CDN. Two reasons:
        //   1. Jenkins serves archived HTML with a strict Content-Security-Policy
        //      that blocks anything not served from Jenkins itself, so a
        //      CDN-linked report renders as unstyled plain text.
        //   2. A CI agent with no internet access can still produce a report
        //      that looks right.
        spark.config().setOfflineMode(true);

        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("Ministry of Testing - Automation Report");
        spark.config().setReportName("Playwright + TestNG");

        extent = new ExtentReports();
        extent.attachReporter(spark);

        extent.setSystemInfo("Application", Config.BASE_URL);
        extent.setSystemInfo("Browser", Config.browser());
        extent.setSystemInfo("Headless", String.valueOf(Config.headless()));
        // Empty when you run locally, filled in by Jenkins.
        extent.setSystemInfo("Jenkins build", System.getenv().getOrDefault("BUILD_NUMBER", "local run"));
    }

    /** Called before each test: creates its section in the report. */
    public static void startTest(String name, String description) {
        currentTest = extent.createTest(name, description);
    }

    public static ExtentTest currentTest() {
        return currentTest;
    }

    /** A skipped test gets its own node; nothing else writes into it. */
    public static void addSkipped(String name, Throwable reason) {
        ExtentTest node = extent.createTest(name);
        if (reason != null) {
            node.skip(reason);
        } else {
            node.skip("Test skipped");
        }
    }

    /** Called once, at the end of the suite. This is what writes index.html. */
    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
    }

    private ExtentReport() {
    }
}
