package com.mot;

/**
 * Every setting the framework has.
 *
 * <p>Two of them can be overridden from the command line, which is exactly how
 * Jenkins passes its build parameters in:
 * {@code mvn test -Dbrowser=firefox -Dheadless=true}
 */
public final class Config {

    public static final String BASE_URL = "https://club.ministryoftesting.com/";
    public static final String SEARCH_QUERY = "skills";
    public static final int TIMEOUT_MS = 30_000;
    public static final int VIEWPORT_WIDTH = 1920;
    public static final int VIEWPORT_HEIGHT = 1080;

    /** -Dbrowser=chromium|firefox|webkit, default chromium. */
    public static String browser() {
        return System.getProperty("browser", "chromium");
    }

    /** -Dheadless=true|false, default true. Set false locally to watch it run. */
    public static boolean headless() {
        return Boolean.parseBoolean(System.getProperty("headless", "true"));
    }

    private Config() {
    }
}
