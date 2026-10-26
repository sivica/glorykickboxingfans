package com.ivicastojanoski.glorykickboxingfans;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static com.ivicastojanoski.glorykickboxingfans.DeviceTestSupport.*;

/** Real production XML/Views and packaged fictional data; no service, phone or live result. */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static Intent item(Class<?> screen, String id) { return new Intent(context(), screen).putExtra(ArchiveScreen.ITEM_ID, id); }
    private static void query(String value) { onView(withId(android.support.v7.appcompat.R.id.search_src_text)).perform(replaceText(value), androidx.test.espresso.action.ViewActions.closeSoftKeyboard()); }

    @Test public void archiveLaunchDisclosesDemoAndPreservesPackageWithoutPermissions() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.screen_title)).check(matches(withText("RINGSIDE")));
            onView(withId(R.id.demo_banner)).check(matches(withText("Demo archive · Fictional results")));
            onView(withTagValue(is("demo-event-northern-2025"))).check(matches(isDisplayed()));
            PackageInfo info = context().getPackageManager().getPackageInfo(context().getPackageName(), PackageManager.GET_PERMISSIONS);
            assertEquals("com.ivicastojanoski.glorykickboxingfans", info.packageName);
            assertEquals(24, info.applicationInfo.minSdkVersion); assertEquals(36, info.applicationInfo.targetSdkVersion);
            assertTrue(info.requestedPermissions == null || info.requestedPermissions.length == 0);
            screen.onActivity(activity -> safeBounds(activity.findViewById(R.id.demo_banner)));
            capture("archive");
        }
    }
    @Test public void searchAliasAndYearIntersectAndClear() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            query("Navigator");
            onView(withId(R.id.year_filter)).perform(click()); onView(withText("2023")).perform(click());
            onView(withId(R.id.result_count)).check(matches(withText("1 matching event")));
            onView(withTagValue(is("demo-event-first-2023"))).check(matches(isDisplayed()));
            capture("search-year");
            onView(withId(R.id.reset_filters)).perform(scrollTo(), click());
            onView(withId(R.id.year_filter)).check(matches(withText(R.string.archive_all_years)));
            onView(withId(R.id.result_count)).check(matches(withText(R.string.archive_heading)));
        }
    }
    @Test public void noResultsExplainsCoverageAndCanReset() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            query("unrepresented-event");
            onView(withId(R.id.empty_heading)).check(matches(withText(R.string.archive_no_results)));
            capture("no-results");
            onView(withId(R.id.reset_filters)).perform(scrollTo(), click());
            onView(withTagValue(is("demo-event-northern-2025"))).check(matches(isDisplayed()));
        }
    }
    @Test public void archiveEventFighterNavigationAndBackPreserveQuery() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            query("Northern");
            onView(withTagValue(is("demo-event-northern-2025"))).perform(scrollTo(), click());
            onView(withId(R.id.screen_title)).check(matches(withText("Northern Lights"))); capture("event");
            onView(withTagValue(is("demo-bout-north-final/demo-fighter-aster"))).perform(scrollTo(), click());
            onView(withId(R.id.screen_title)).check(matches(withText("Aster Vale")));
            onView(withText(containsString("not a full professional record"))).check(matches(isDisplayed())); capture("fighter");
            pressBack(); onView(withId(R.id.screen_title)).check(matches(withText("Northern Lights")));
            pressBack(); onView(withId(android.support.v7.appcompat.R.id.search_src_text)).check(matches(withText("Northern")));
        }
    }
    @Test public void recreationRetainsSearchAndYear() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            query("Northern"); onView(withId(R.id.year_filter)).perform(click()); onView(withText("2025")).perform(click());
            screen.recreate();
            onView(withId(android.support.v7.appcompat.R.id.search_src_text)).check(matches(withText("Northern")));
            onView(withId(R.id.year_filter)).check(matches(withText("2025 ✓")));
            onView(withId(R.id.result_count)).check(matches(withText("1 matching event")));
        }
    }
    @Test public void missingResultHasNoWinnerButBothFighterLinks() throws Exception {
        try (ActivityScenario<EventActivity> screen = ActivityScenario.launch(item(EventActivity.class, "demo-event-summit-2024"))) {
            onView(withTagValue(is("demo-bout-summit-unknown"))).perform(scrollTo())
                    .check(matches(not(hasDescendant(withText(containsString(" · Win"))))));
            onView(withTagValue(is("demo-bout-summit-unknown/demo-fighter-lumen"))).check(matches(withText(containsString("Result unknown"))));
            onView(withTagValue(is("demo-bout-summit-unknown/demo-fighter-rowan"))).check(matches(isDisplayed()));
            capture("missing-result");
        }
    }
    @Test public void cancellationAndNoContestAreNotLosses() {
        try (ActivityScenario<EventActivity> screen = ActivityScenario.launch(item(EventActivity.class, "demo-event-harbor-2025"))) {
            onView(withTagValue(is("demo-bout-harbor-nc/demo-fighter-rowan"))).perform(scrollTo()).check(matches(withText(containsString("No contest"))));
            onView(withTagValue(is("demo-bout-harbor-cancelled/demo-fighter-lumen"))).perform(scrollTo()).check(matches(withText(containsString("Cancelled"))));
        }
    }
    @Test public void correctionHistoryRetainsPriorNameAndReason() throws Exception {
        try (ActivityScenario<FighterActivity> screen = ActivityScenario.launch(item(FighterActivity.class, "demo-fighter-aster"))) {
            onView(withId(R.id.correction_action)).perform(scrollTo(), click());
            onView(withText(containsString("Previously: Aster Vail"))).check(matches(isDisplayed()));
            onView(withText(containsString("Now: Aster Vale"))).check(matches(isDisplayed())); capture("correction");
            onView(withText(R.string.archive_close)).perform(click());
        }
    }
    @Test public void unknownStableIdShowsExplicitLocalError() {
        try (ActivityScenario<EventActivity> screen = ActivityScenario.launch(item(EventActivity.class, "demo-not-included"))) {
            onView(withText(R.string.archive_not_found)).check(matches(isDisplayed()));
            onView(withId(R.id.screen_title)).check(matches(withText(R.string.archive_error_title)));
        }
    }
    @Test public void bundledProvenanceStatesFictionalScope() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.provenance_action)).perform(scrollTo(), click());
            onView(withText(containsString("all events, fighters and results are fictional"))).check(matches(isDisplayed()));
            onView(withText(containsString("Revision 2"))).check(matches(isDisplayed()));
            onView(withText(R.string.archive_close)).perform(click());
        }
    }
    @Test public void largeFontPortraitAndLandscapeKeepTextAndActionsReachable() throws Exception {
        String original = shell("settings get system font_scale").trim();
        try {
            shell("settings put system font_scale 2.0");
            try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
                capture("large-font-portrait");
                screen.onActivity(activity -> {
                    assertTrue("Real isolated-device font scale", activity.getResources().getConfiguration().fontScale >= 1.9f);
                    ArchiveAccessibilityTest.validateLayout(activity);
                });
                query("Northern");
                screen.onActivity(ArchiveAccessibilityTest::validateLayout);
                query("");
                screen.onActivity(activity -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                capture("large-font-landscape-before-scroll"); // Wait for the actual rotation animation/layout to settle.
                screen.onActivity(ArchiveAccessibilityTest::validateLayout);
                onView(withId(R.id.provenance_action)).perform(scrollTo(), click());
                onView(withText(R.string.archive_provenance_title)).check(matches(isDisplayed()));
                onView(withText(R.string.archive_close)).perform(click()); capture("large-font-landscape");
            }
        } finally {
            shell("settings put system font_scale " + (original.matches("[0-9]+(?:\\.[0-9]+)?") ? original : "1.0"));
        }
    }
    @Test public void backLeavesArchiveRoot() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            pressBackUnconditionally(); InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertNotEquals(Lifecycle.State.RESUMED, screen.getState());
        }
    }
}
