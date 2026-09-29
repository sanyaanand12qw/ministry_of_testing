package com.mot.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Single source of truth for configuration.
 *
 * <p>Resolution order (first match wins):
 * <ol>
 *   <li>-D system property  (mvn test -Dheadless=false)</li>
 *   <li>config.properties   (checked-in defaults)</li>
 *   <li>the fallback passed by the caller</li>
 * </ol>
 * That ordering is what lets Jenkins build parameters drive the run without
 * anyone editing a file in the repo.
 */
public final class ConfigReader {

    private static final String FILE = "config.properties";
    private static final Properties PROPS = load();

    private ConfigReader() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(FILE + " not found on the test classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + FILE, e);
        }
        return properties;
    }

    public static String get(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = PROPS.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration key: " + key);
        }
        return value.trim();
    }

    public static String get(String key, String fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = PROPS.getProperty(key, fallback);
        }
        return value == null ? null : value.trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    // --- typed accessors, so call sites never repeat a magic string ---

    public static String baseUrl() {
        return get("base.url");
    }

    public static String searchQuery() {
        return get("search.query");
    }

    public static String browser() {
        return get("browser").toLowerCase();
    }

    public static boolean headless() {
        return getBoolean("headless");
    }

    public static double slowMo() {
        return Double.parseDouble(get("slowmo", "0"));
    }

    public static int defaultTimeout() {
        return getInt("timeout.default");
    }

    public static int navigationTimeout() {
        return getInt("timeout.navigation");
    }

    public static int viewportWidth() {
        return getInt("viewport.width");
    }

    public static int viewportHeight() {
        return getInt("viewport.height");
    }

    public static String videoMode() {
        return get("capture.video", "retain-on-failure");
    }

    public static String traceMode() {
        return get("capture.trace", "retain-on-failure");
    }
}
