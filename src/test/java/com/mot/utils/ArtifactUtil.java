package com.mot.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Owns the on-disk layout of everything the run produces.
 *
 * <p>Screenshots, videos and traces are written <b>inside</b> the Extent report
 * directory on purpose. The report then links them with a short relative path
 * ("screenshots/x.png"), so the whole folder can be copied, zipped, or published
 * by the Jenkins HTML Publisher and every link still resolves.
 *
 * <pre>
 * target/
 *   extent-report/
 *     index.html
 *     screenshots/  videos/  traces/
 *   logs/automation.log
 *   video-raw/            (scratch; Playwright writes here, we move or drop)
 * </pre>
 */
public final class ArtifactUtil {

    public static final Path TARGET = Paths.get("target");
    public static final Path REPORT_DIR = TARGET.resolve("extent-report");
    public static final Path SCREENSHOT_DIR = REPORT_DIR.resolve("screenshots");
    public static final Path VIDEO_DIR = REPORT_DIR.resolve("videos");
    public static final Path TRACE_DIR = REPORT_DIR.resolve("traces");
    public static final Path RAW_VIDEO_DIR = TARGET.resolve("video-raw");

    private ArtifactUtil() {
    }

    public static Path ensure(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create directory " + dir, e);
        }
    }

    /** Strips characters that are unsafe in a file name. */
    public static String safe(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static Path screenshotPath(String testName) {
        return ensure(SCREENSHOT_DIR).resolve(safe(testName) + ".png");
    }

    public static Path videoPath(String testName) {
        return ensure(VIDEO_DIR).resolve(safe(testName) + ".webm");
    }

    public static Path tracePath(String testName) {
        return ensure(TRACE_DIR).resolve(safe(testName) + ".zip");
    }

    /** Scratch folder Playwright records this test's video into. */
    public static Path rawVideoDir(String testName) {
        return ensure(RAW_VIDEO_DIR.resolve(safe(testName)));
    }

    /**
     * Path to embed in the HTML report: relative to index.html, forward slashes,
     * so it works on Windows agents and inside the Jenkins HTML Publisher iframe.
     */
    public static String relativeToReport(Path artifact) {
        return REPORT_DIR.toAbsolutePath()
                .relativize(artifact.toAbsolutePath())
                .toString()
                .replace('\\', '/');
    }

    public static void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort: a stale scratch file must never fail a build
                }
            });
        } catch (IOException ignored) {
            // same
        }
    }
}
