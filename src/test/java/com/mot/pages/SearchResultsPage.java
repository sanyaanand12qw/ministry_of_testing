package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.Log;

/**
 * The search results the site shows after a query is submitted.
 */
public class SearchResultsPage {

    private final Page page;
    private final Locator topicResults;

    public SearchResultsPage(Page page) {
        this.page = page;
        // The dropdown mixes three kinds of link: a "search all topics" row, tag
        // shortcuts, and the real topics. Only a real topic contains a
        // .topic-title, so :has() keeps just those -- which makes "the first
        // link" mean the first actual topic.
        this.topicResults = page.locator("a.search-link:has(.topic-title)");
    }

    /** No sleeps anywhere: waitFor() polls until the element exists. */
    SearchResultsPage waitForResults() {
        topicResults.first().waitFor();
        Log.info("Search returned " + resultCount() + " topic(s)");
        return this;
    }

    public int resultCount() {
        return topicResults.count();
    }

    public String firstResultTitle() {
        String title = topicResults.first().locator(".topic-title").first().innerText().trim();
        Log.info("First result: " + title);
        return title;
    }

    public TopicPage openFirstResult() {
        Log.info("Clicking the first result");
        topicResults.first().click();
        return new TopicPage(page).waitUntilLoaded();
    }
}
