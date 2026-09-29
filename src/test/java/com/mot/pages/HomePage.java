package com.mot.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.mot.Config;
import com.mot.Log;

/**
 * The Club home page and its search bar.
 *
 * <p>A page object holds the locators and exposes plain actions. The test then
 * reads as the user journey with no CSS selectors in it.
 */
public class HomePage {

    private final Page page;
    private final Locator searchBox;

    public HomePage(Page page) {
        this.page = page;
        // Verified against the live site: the home page search bar is the input
        // in the welcome banner.
        this.searchBox = page.locator("#welcome-banner-search-input");
    }

    public HomePage open() {
        Log.info("Opening " + Config.BASE_URL);
        page.navigate(Config.BASE_URL);
        return this;
    }

    /**
     * Types the query and submits it. The site shows the matches in a dropdown
     * instead of loading a new page, so the results are read from there.
     */
    public SearchResultsPage searchFor(String query) {
        Log.info("Typing '" + query + "' into the search bar");
        searchBox.click();
        searchBox.fill(query);
        searchBox.press("Enter");
        return new SearchResultsPage(page).waitForResults();
    }
}
