package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.config.ConfigReader;
import com.mot.utils.Log;

/**
 * The Club home page (a Discourse forum) and its search bar.
 */
public class HomePage extends BasePage {

    /*
     * Locator strategy, verified against the live DOM:
     *  - the search bar on the home page is the welcome-banner input
     *    (id="welcome-banner-search-input", role=searchbox, placeholder="Search")
     *  - .or(...) adds a semantic fallback, so a theme change that renames the id
     *    degrades to the accessible placeholder instead of failing the suite.
     */
    private final Locator searchBox = page.locator("#welcome-banner-search-input")
            .or(page.getByPlaceholder("Search"))
            .first();

    public HomePage(Page page) {
        super(page);
    }

    public HomePage open() {
        navigateTo(ConfigReader.baseUrl());
        page.waitForLoadState();
        Log.info("Home page loaded with title: " + title());
        return this;
    }

    public boolean isSearchBarVisible() {
        return searchBox.isVisible();
    }

    /**
     * Types the query into the search bar and submits it. Discourse renders the
     * matches into its search menu rather than navigating, so the returned page
     * object reads the results from there.
     */
    public SearchResultsPage searchFor(String query) {
        click(searchBox, "the search bar");
        type(searchBox, query, "the search bar");
        Log.info("Submitting the search with Enter");
        searchBox.press("Enter");
        return new SearchResultsPage(page).waitForResults();
    }
}
