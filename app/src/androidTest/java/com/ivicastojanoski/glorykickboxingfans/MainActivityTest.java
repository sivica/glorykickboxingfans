package com.ivicastojanoski.glorykickboxingfans;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Real production Views, static resources only; no data seeding, permissions or service calls. */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Test public void launchPreservesTemplateAndPackageIdentity() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            assertTemplate();
            screen.onActivity(activity -> {
                assertEquals("Glory Kickboxing Fans", activity.getTitle().toString());
                assertSafeBounds(activity);
            });
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            assertEquals("com.ivicastojanoski.glorykickboxingfans", info.packageName);
            assertEquals(24, info.applicationInfo.minSdkVersion);
            assertEquals(36, info.applicationInfo.targetSdkVersion);
            assertTrue(info.requestedPermissions == null || info.requestedPermissions.length == 0);
            capture("content-portrait");
        }
    }

    @Test public void fabShowsTheOriginalPlaceholderMessage() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fab)).perform(click());
            onView(withText("Replace with your own action")).check(matches(isCompletelyDisplayed()));
            // The unchanged template passes a null listener, so no action is shown.
            onView(withId(android.support.design.R.id.snackbar_action))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
            screen.onActivity(activity -> assertSafeBounds(activity,
                    new int[] {android.support.design.R.id.snackbar_text}));
            capture("content-snackbar");
        }
    }

    @Test public void settingsMenuKeepsTheExistingScreen() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
            onView(withText("Settings")).perform(click());
            assertTemplate();
        }
    }

    @Test public void recreationKeepsStaticContentAndSafeBounds() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            assertTemplate();
            screen.recreate();
            assertTemplate();
            screen.onActivity(MainActivityTest::assertSafeBounds);
        }
    }

    @Test public void landscapeKeepsToolbarContentAndFabOutsideSystemBars() throws Exception {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity -> activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            assertTemplate();
            screen.onActivity(activity -> {
                assertEquals(Configuration.ORIENTATION_LANDSCAPE, activity.getResources().getConfiguration().orientation);
                assertSafeBounds(activity);
            });
            capture("content-landscape");
        }
    }

    @Test public void backLeavesTheSingleLauncherScreen() {
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            assertTemplate();
            pressBackUnconditionally();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            // Android can stop the launcher task rather than destroy its root Activity.
            assertNotEquals(Lifecycle.State.RESUMED, screen.getState());
        }
    }

    private static void assertTemplate() {
        onView(withId(R.id.hello_text)).check(matches(withText("Hello World!"))).check(matches(isDisplayed()));
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
                .check(matches(withContentDescription("Show sample message")));
    }

    private static void assertSafeBounds(MainActivity activity) {
        assertSafeBounds(activity, new int[] {R.id.toolbar, R.id.hello_text, R.id.fab});
    }

    private static void assertSafeBounds(MainActivity activity, int[] ids) {
        View decor = activity.getWindow().getDecorView();
        WindowInsets windowInsets = decor.getRootWindowInsets();
        assertNotNull("Window insets are available", windowInsets);
        int left, top, right, bottom;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            assertNotNull("Attached window has an insets controller", controller);
            int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            assertEquals("Dark system icons remain readable on the pale background",
                    lightBars, controller.getSystemBarsAppearance() & lightBars);
            Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
        } else {
            left = windowInsets.getSystemWindowInsetLeft(); top = windowInsets.getSystemWindowInsetTop();
            right = windowInsets.getSystemWindowInsetRight(); bottom = windowInsets.getSystemWindowInsetBottom();
        }
        Rect safe = new Rect(left, top, decor.getWidth() - right, decor.getHeight() - bottom);
        for (int id : ids) {
            View view = activity.findViewById(id);
            assertNotNull("Expected visible view", view);
            int[] location = new int[2]; view.getLocationOnScreen(location);
            Rect bounds = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
            assertTrue("Content crosses system bars/cutout: " + id, safe.contains(bounds));
        }
    }

    private static void capture(String name) throws Exception {
        Bitmap bitmap = name.equals("content-landscape") ? stableRotationScreenshot()
                : InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull("Capture available", bitmap);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("test-captures");
        assertNotNull(directory);
        if (!directory.exists()) assertTrue(directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
        // Actual captures only: no golden baseline or pixel-comparison pass is implied.
    }

    private static Bitmap stableRotationScreenshot() {
        // Activity configuration/layout can settle before the system rotation animation.
        // Require three unchanged frames, bounded to five seconds, without disabling animations.
        Bitmap previous = null;
        int unchanged = 0;
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            Bitmap current = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull("Rotation capture available", current);
            unchanged = previous != null && current.sameAs(previous) ? unchanged + 1 : 0;
            if (previous != null) previous.recycle();
            if (unchanged >= 3) return current;
            previous = current;
            SystemClock.sleep(100);
        }
        if (previous != null) previous.recycle();
        throw new AssertionError("Rotation capture did not stabilize within five seconds");
    }
}
