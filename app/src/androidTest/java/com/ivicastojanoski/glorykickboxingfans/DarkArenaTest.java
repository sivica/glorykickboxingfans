package com.ivicastojanoski.glorykickboxingfans;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.ivicastojanoski.glorykickboxingfans.DeviceTestSupport.capture;

/** Resolved production theme/control checks, not a Figma pixel or manual accessibility pass. */
@RunWith(AndroidJUnit4.class)
public class DarkArenaTest {
    @Test public void darkControlsAndSystemBarsRetainReadableForegrounds() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screen.onActivity(activity -> {
                assertEquals(Color.parseColor("#F7F7F8"), ((TextView) activity.findViewById(R.id.screen_title)).getCurrentTextColor());
                assertEquals(Color.WHITE, ((TextView) activity.findViewById(R.id.demo_banner)).getCurrentTextColor());
                assertEquals(Color.parseColor("#B8BCC7"), ((TextView) activity.findViewById(android.support.v7.appcompat.R.id.search_src_text)).getCurrentTextColor());
                assertEquals(Color.WHITE, ((TextView) activity.findViewById(R.id.card_footer)).getCurrentTextColor());
                int lightBars = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 26) lightBars |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                if (Build.VERSION.SDK_INT >= 30) {
                    int appearance = activity.getWindow().getInsetsController().getSystemBarsAppearance();
                    assertEquals(0, appearance & (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS));
                } else assertEquals(0, activity.getWindow().getDecorView().getSystemUiVisibility() & lightBars);
                for (int[] colors : new int[][] {
                        {R.color.archive_text, R.color.archive_background},
                        {R.color.archive_secondary, R.color.archive_surface},
                        {R.color.archive_secondary, R.color.archive_notice_surface},
                        {R.color.archive_on_dark, R.color.archive_accent},
                        {R.color.archive_on_dark, R.color.archive_warning_surface}}) {
                    assertTrue("Normal text contrast >= 4.5", android.support.v4.graphics.ColorUtils.calculateContrast(
                            activity.getColor(colors[0]), activity.getColor(colors[1])) >= 4.5);
                }
            });
        }
    }
    @Test public void nativeYearDialogRemainsReadableAndInteractive() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.year_filter)).perform(click());
            onView(withText(R.string.archive_year_title)).check(matches(isDisplayed()));
            onView(withText("2025")).check(matches(isDisplayed()));
            onView(withText(R.string.archive_close)).check((view, missing) -> {
                if (missing != null) throw missing;
                assertEquals(Color.WHITE, ((TextView) view).getCurrentTextColor());
            });
            capture("year-selector");
            onView(withText("2025")).perform(click());
            onView(withId(R.id.year_filter)).check(matches(withText("2025 ✓")));
            onView(withId(R.id.result_count)).check(matches(withText("2 matching events")));
        }
    }
}
