package com.mot.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.mot.config.ConfigReader;
import com.mot.utils.ArtifactUtil;

/**
 * Owns the ExtentReports lifecycle.
 *
 * <p>One {@link ExtentReports} instance for the whole JVM (it is the report file),
 * and one {@link ExtentTest} node per <em>thread</em> so parallel test methods
 * never write into each other's section. {@link com.mot.listeners.TestListener}
 * is the only class that calls createTest / flush.
 */
public final class ExtentManager {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();

    private ExtentManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            extent = build();
        }
        return extent;
    }

    private static ExtentReports build() {
        ArtifactUtil.ensure(ArtifactUtil.REPORT_DIR);

        ExtentSparkReporter spark =
                new ExtentSparkReporter(ArtifactUtil.REPORT_DIR.resolve("index.html").toFile());
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("Ministry of Testing - Automation Report");
        spark.config().setReportName("Playwright + TestNG Suite");
        spark.config().setTimeStampFormat("dd-MMM-yyyy HH:mm:ss");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(spark);

        // Environment block at the top of the report: makes a CI run self-describing.
        reports.setSystemInfo("Application", ConfigReader.baseUrl());
        reports.setSystemInfo("Browser", ConfigReader.browser());
        reports.setSystemInfo("Headless", String.valueOf(ConfigReader.headless()));
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        reports.setSystemInfo("Executed By", env("USER", System.getProperty("user.name")));
        reports.setSystemInfo("Jenkins Job", env("JOB_NAME", "local run"));
        reports.setSystemInfo("Build Number", env("BUILD_NUMBER", "N/A"));
        return reports;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static void setCurrentTest(ExtentTest test) {
        CURRENT_TEST.set(test);
    }

    /** May be null when called outside a test method. Callers must tolerate that. */
    public static ExtentTest getCurrentTest() {
        return CURRENT_TEST.get();
    }

    /** Writes the HTML to disk. Called once, from the listener's onFinish. */
    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }
}
