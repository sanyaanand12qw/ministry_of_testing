package com.mot;

import com.aventstack.extentreports.ExtentTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One call writes the same sentence in two places:
 * <ol>
 *   <li>target/logs/automation.log (archived by Jenkins)</li>
 *   <li>the current section of the HTML report</li>
 * </ol>
 *
 * <p>That is the only reason page objects use this instead of a plain logger.
 */
public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger("test");

    /** A normal step. */
    public static void info(String message) {
        LOGGER.info(message);
        ExtentTest test = ExtentReport.currentTest();
        if (test != null) {
            test.info(message);
        }
    }

    /** A step that verified something successfully. */
    public static void pass(String message) {
        LOGGER.info("PASS - {}", message);
        ExtentTest test = ExtentReport.currentTest();
        if (test != null) {
            test.pass(message);
        }
    }

    public static void fail(String message) {
        LOGGER.error(message);
        ExtentTest test = ExtentReport.currentTest();
        if (test != null) {
            test.fail(message);
        }
    }

    private Log() {
    }
}
