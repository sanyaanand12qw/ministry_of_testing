package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.utils.Log;

/**
 * The search results Discourse renders after a query is submitted.
 */
public class SearchResultsPage extends BasePage {

    /*
     * The results list mixes three kinds of a.search-link: an assistant row
     * ("... in all topics and posts", href="#"), tag shortcuts (href="/tag/..."),
     * and the actual topics. Only topics carry a .topic-title, so :has() filters
     * to real results and "the first link" means the first real topic.
     */
    private final Locator topicResults = page.locator("a.search-link:has(.topic-title)");

    public SearchResultsPage(Page page) {
        super(page);
    }

    SearchResultsPage waitForResults() {
        Log.info("Waiting for topic results to render");
        topicResults.first().waitFor();
        Log.info("Search returned %d topic result(s)".formatted(resultCount()));
        return this;
    }

    public int resultCount() {
        return topicResults.count();
    }

    /** Plain text of the first result's title, with the search highlighting stripped. */
    public String firstResultTitle() {
        String title = topicResults.first().locator(".topic-title").first().innerText().trim();
        Log.info("First result title: " + title);
        return title;
    }

    /** Clicks the first topic result and returns the topic page it opens. */
    public TopicPage openFirstResult() {
        click(topicResults.first(), "the first search result");
        return new TopicPage(page).waitUntilLoaded();
    }
}
