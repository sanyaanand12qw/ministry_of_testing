package com.mot.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Thread-confined holder for the Playwright objects of one test method.
 *
 * <p>Playwright's Java objects are <b>not</b> thread-safe, so each test thread
 * gets its own {@link Playwright} driver, {@link Browser}, {@link BrowserContext}
 * and {@link Page}. Keeping them in ThreadLocals is what makes
 * {@code parallel="methods"} in testng.xml safe, and it means page objects and
 * listeners can reach "the current page" without it being passed through every
 * constructor.
 */
public final class DriverManager {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    private DriverManager() {
    }

    static void setPlaywright(Playwright playwright) {
        PLAYWRIGHT.set(playwright);
    }

    static void setBrowser(Browser browser) {
        BROWSER.set(browser);
    }

    static void setContext(BrowserContext context) {
        CONTEXT.set(context);
    }

    static void setPage(Page page) {
        PAGE.set(page);
    }

    public static Playwright getPlaywright() {
        return PLAYWRIGHT.get();
    }

    public static Browser getBrowser() {
        return BROWSER.get();
    }

    public static BrowserContext getContext() {
        return CONTEXT.get();
    }

    /** The page the current thread is driving; null once the test has torn down. */
    public static Page getPage() {
        return PAGE.get();
    }

    /** Detaches every reference so the thread can be reused without leaking. */
    static void unload() {
        PAGE.remove();
        CONTEXT.remove();
        BROWSER.remove();
        PLAYWRIGHT.remove();
    }
}
