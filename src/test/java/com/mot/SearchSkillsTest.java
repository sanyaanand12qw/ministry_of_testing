package com.mot;

import com.mot.pages.HomePage;
import com.mot.pages.SearchResultsPage;
import com.mot.pages.TopicPage;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Search the Club forum for "skills" and open the first result.
 *
 * <p>Notice what is <b>not</b> here: no browser setup, no CSS selectors, no
 * waits, no reporting code. Setup and teardown come from {@link BaseTest},
 * selectors live in the page objects, and reporting comes from
 * {@link TestListener}.
 */
public class SearchSkillsTest extends BaseTest {

    @Test(description = "Searching for 'skills' returns topics and the first one opens")
    public void searchForSkillsAndOpenFirstResult() {
        HomePage home = new HomePage(page()).open();

        SearchResultsPage results = home.searchFor(Config.SEARCH_QUERY);
        assertTrue(results.resultCount() > 0, "The search returned no topics");

        String firstTitle = results.firstResultTitle();
        assertTrue(firstTitle.toLowerCase().contains(Config.SEARCH_QUERY),
                "The first result should mention '%s' but was '%s'".formatted(Config.SEARCH_QUERY, firstTitle));

        TopicPage topic = results.openFirstResult();
        assertTrue(topic.currentUrl().contains("/t/"), "Not a topic URL: " + topic.currentUrl());
        assertEquals(topic.heading(), firstTitle, "Opened a different topic than the one clicked");

        Log.pass("Opened the first result: " + topic.heading());
    }
}
