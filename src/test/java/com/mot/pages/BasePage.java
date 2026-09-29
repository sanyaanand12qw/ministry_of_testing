package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.utils.Log;

/**
 * Shared plumbing for every page object.
 *
 * <p>Playwright auto-waits before each action, so there is deliberately no
 * explicit-wait helper zoo here — only the few things every page needs.
 */
public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    protected void navigateTo(String url) {
        Log.info("Navigating to " + url);
        page.navigate(url);
    }

    protected void click(Locator locator, String description) {
        Log.info("Clicking " + description);
        locator.click();
    }

    protected void type(Locator locator, String text, String description) {
        Log.info("Typing '%s' into %s".formatted(text, description));
        locator.fill(text);
    }

    public String title() {
        return page.title();
    }

    public String currentUrl() {
        return page.url();
    }
}
