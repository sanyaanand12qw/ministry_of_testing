package com.mot.utils;

import com.aventstack.extentreports.ExtentTest;
import com.mot.report.ExtentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One call, two destinations: the log file/console (Log4j2 via SLF4J) and the
 * current Extent node.
 *
 * <p>Page objects and tests use this instead of a raw logger, which is why the
 * HTML report reads like a step-by-step narrative without anyone writing the
 * same sentence twice.
 */
public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger("com.mot.step");

    private Log() {
    }

    /** A normal step. */
    public static void info(String message) {
        LOGGER.info(message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.info(message);
        }
    }

    /** A step whose verification succeeded. */
    public static void pass(String message) {
        LOGGER.info("PASS - {}", message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.pass(message);
        }
    }

    public static void warn(String message) {
        LOGGER.warn(message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.warning(message);
        }
    }

    public static void fail(String message) {
        LOGGER.error(message);
        ExtentTest test = ExtentManager.getCurrentTest();
        if (test != null) {
            test.fail(message);
        }
    }

    /** Framework-internal detail; file/console only, kept out of the report. */
    public static void debug(String message) {
        LOGGER.debug(message);
    }
}
