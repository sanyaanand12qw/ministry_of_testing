package com.mot.tests;

import com.mot.config.ConfigReader;
import com.mot.core.BaseTest;
import com.mot.pages.HomePage;
import com.mot.pages.SearchResultsPage;
import com.mot.pages.TopicPage;
import com.mot.utils.Log;
import org.testng.asserts.SoftAssert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Search the Club forum for "skills" and open the first result.
 *
 * <p>No locators and no waits live here — the test reads as the user journey and
 * every technical detail sits in the page objects and BaseTest.
 */
public class SearchSkillsTest extends BaseTest {

    @Test(groups = {"smoke", "regression"},
            description = "Searching for 'skills' returns matching topics and the first result opens")
    public void searchForSkillsAndOpenFirstResult() {
        String query = ConfigReader.searchQuery();

        HomePage home = new HomePage(page()).open();
        assertTrue(home.isSearchBarVisible(), "The search bar should be visible on the home page");
        Log.pass("Home page is loaded and the search bar is available");

        SearchResultsPage results = home.searchFor(query);
        assertTrue(results.resultCount() > 0, "The search should return at least one topic for '" + query + "'");
        Log.pass("Search for '%s' returned %d topic result(s)".formatted(query, results.resultCount()));

        String firstResultTitle = results.firstResultTitle();
        assertTrue(firstResultTitle.toLowerCase().contains(query.toLowerCase()),
                "The first result title should mention '%s' but was '%s'".formatted(query, firstResultTitle));
        Log.pass("The first result is relevant to the query");

        TopicPage topic = results.openFirstResult();

        // Soft assertions: collect every mismatch on the opened page in one run
        // instead of stopping at the first one.
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(topic.currentUrl().contains("/t/"),
                "A topic URL should contain '/t/' but was " + topic.currentUrl());
        soft.assertEquals(topic.heading(), firstResultTitle,
                "The opened topic heading should match the result that was clicked");
        // TEMPORARY — deliberate failure to prove the CI evidence path. Revert after.
        soft.assertTrue(topic.postCount() > 9999,
                "DELIBERATE FAILURE (proving the Jenkins evidence path): the topic has "
                        + topic.postCount() + " post(s), which is not > 9999");
        soft.assertAll();

        Log.pass("The first search result opened the expected topic: " + topic.heading());
    }
}
