package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.Log;

/**
 * A topic (discussion thread) page — where the first search result lands.
 */
public class TopicPage {

    private final Page page;
    private final Locator heading;

    public TopicPage(Page page) {
        this.page = page;
        this.heading = page.locator("#topic-title .fancy-title");
    }

    TopicPage waitUntilLoaded() {
        heading.waitFor();
        Log.info("Topic page opened: " + page.url());
        return this;
    }

    public String heading() {
        return heading.innerText().trim();
    }

    public String currentUrl() {
        return page.url();
    }
}
