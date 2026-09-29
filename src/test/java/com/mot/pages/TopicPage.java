package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.utils.Log;

/**
 * A Discourse topic (thread) page — where the first search result lands.
 */
public class TopicPage extends BasePage {

    private final Locator heading = page.locator("#topic-title .fancy-title");
    private final Locator posts = page.locator(".topic-post");

    public TopicPage(Page page) {
        super(page);
    }

    TopicPage waitUntilLoaded() {
        heading.waitFor();
        Log.info("Topic page opened: " + currentUrl());
        return this;
    }

    public String heading() {
        return heading.innerText().trim();
    }

    public int postCount() {
        return posts.count();
    }
}
