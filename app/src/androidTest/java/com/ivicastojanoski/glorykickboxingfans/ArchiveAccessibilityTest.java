package com.ivicastojanoski.glorykickboxingfans;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.ivicastojanoski.glorykickboxingfans.DeviceTestSupport.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Native node/layout/navigation evidence; does not claim a manual TalkBack pass. */
@RunWith(AndroidJUnit4.class)
public class ArchiveAccessibilityTest {
    private static Intent event() { return item(EventActivity.class, "demo-event-northern-2025"); }
    private static Intent fighter() { return item(FighterActivity.class, "demo-fighter-aster"); }
    private static Intent item(Class<?> type, String id) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), type).putExtra(ArchiveScreen.ITEM_ID,id);
    }
    private static void at200Percent(Checked action) throws Exception {
        String original = shell("settings get system font_scale").trim();
        try { shell("settings put system font_scale 2.0"); action.run(); }
        finally { shell("settings put system font_scale " + (original.matches("[0-9]+(?:\\.[0-9]+)?") ? original : "1.0")); }
    }
    private interface Checked { void run() throws Exception; }
    static void validateLayout(ArchiveScreen activity) {
        assertTrue(activity.getResources().getConfiguration().fontScale >= 1.9f);
        noEllipsis(activity.findViewById(R.id.main_content));
        textAndTargetsFit(activity.findViewById(R.id.main_content));
    }
    private static void textAndTargetsFit(View view) {
        if (view.getVisibility() != View.VISIBLE || view.getWidth()==0 || view.getHeight()==0) return;
        if (view instanceof TextView) {
            TextView text=(TextView)view;
            if (text.getLayout()!=null) assertTrue("Text must fit vertically: "+text.getClass().getSimpleName()+" id="+text.getId()
                    +" value="+text.getText()+" layout="+text.getLayout().getHeight()+" height="+text.getHeight()
                    +" padding="+text.getCompoundPaddingTop()+"/"+text.getCompoundPaddingBottom(),
                    text.getLayout().getHeight() <= text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom()+1);
        }
        if (view instanceof Button) assertTrue("Minimum 48dp target", view.getHeight()+1 >= 48*view.getResources().getDisplayMetrics().density);
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                View child=group.getChildAt(i);
                if(child.getVisibility()==View.VISIBLE) assertTrue("No horizontal overflow", child.getLeft()>=0 && child.getRight()<=group.getWidth()+1);
                textAndTargetsFit(child);
            }
        }
    }
    @Test public void eventAndFighter200PercentPortraitKeepAllActions() throws Exception {
        at200Percent(() -> {
            try(ActivityScenario<EventActivity> screen=ActivityScenario.launch(event())) {
                screen.onActivity(ArchiveAccessibilityTest::validateLayout);
                capture("large-event-portrait");
                onView(withTagValue(is("demo-bout-north-final/demo-fighter-aster"))).perform(scrollTo(),click());
                onView(withId(R.id.screen_title)).check(matches(withText("Aster Vale")));
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> validateLayout((ArchiveScreen)resumed()));
                capture("large-fighter-portrait");
                onView(withId(R.id.correction_action)).perform(scrollTo(),click());
                onView(withText(R.string.archive_correction_title)).check(matches(isDisplayed()));
                onView(withText(R.string.archive_close)).perform(click());
                onView(withId(R.id.provenance_action)).perform(scrollTo(),click());
                onView(withText(R.string.archive_close)).perform(click());
                pressBack();
                onView(withId(R.id.screen_title)).perform(scrollTo()).check(matches(withText("Northern Lights")));
            }
        });
    }
    @Test public void eventAndFighter200PercentLandscapeFooterReachable() throws Exception {
        at200Percent(() -> {
            try(ActivityScenario<EventActivity> screen=ActivityScenario.launch(event())) {
                screen.onActivity(a->a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                capture("large-event-landscape-top");
                screen.onActivity(ArchiveAccessibilityTest::validateLayout);
                onView(withId(R.id.provenance_action)).perform(scrollTo(),click());
                onView(withText(R.string.archive_close)).perform(click());capture("large-event-landscape");
            }
            try(ActivityScenario<FighterActivity> screen=ActivityScenario.launch(fighter())) {
                screen.onActivity(a->a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                capture("large-fighter-landscape-top");
                screen.onActivity(ArchiveAccessibilityTest::validateLayout);
                onView(withId(R.id.provenance_action)).perform(scrollTo(),click());
                onView(withText(R.string.archive_close)).perform(click());capture("large-fighter-landscape");
            }
        });
    }
    @Test public void detailRecreationRetainsStableIdAndNativeBackContext() {
        try(ActivityScenario<EventActivity> screen=ActivityScenario.launch(event())) {
            screen.recreate();onView(withId(R.id.screen_title)).check(matches(withText("Northern Lights")));
            onView(withTagValue(is("demo-bout-north-final/demo-fighter-aster"))).perform(scrollTo(),click());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->resumed().recreate());
            onView(withId(R.id.screen_title)).check(matches(withText("Aster Vale")));
            pressBack();onView(withId(R.id.screen_title)).perform(scrollTo()).check(matches(withText("Northern Lights")));
        }
    }
    @Test public void missingFighterIdentityShowsLocalError() {
        try(ActivityScenario<FighterActivity> screen=ActivityScenario.launch(new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),FighterActivity.class))) {
            onView(withText(R.string.archive_not_found)).check(matches(isDisplayed()));
        }
    }
    @Test public void nativeClearSearchPreservesSelectedYear() {
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)) {
            onView(withId(android.support.v7.appcompat.R.id.search_src_text)).perform(replaceText("Northern"), androidx.test.espresso.action.ViewActions.closeSoftKeyboard());
            onView(withId(R.id.year_filter)).perform(click());onView(withText("2025")).perform(click());
            onView(withId(android.support.v7.appcompat.R.id.search_close_btn)).perform(click());
            onView(withId(R.id.year_filter)).check(matches(withText("2025 ✓")));
            onView(withId(R.id.result_count)).check(matches(withText("2 matching events")));
        }
    }
    @Test public void accessibilityNodesExposeFictionalScopeAndActions() {
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)) {
            screen.onActivity(activity->{
                View card=activity.findViewById(R.id.archive_results).findViewWithTag("demo-event-northern-2025");
                AccessibilityNodeInfo node=card.createAccessibilityNodeInfo();
                assertEquals(Button.class.getName(),node.getClassName().toString());assertTrue(node.isClickable());
                assertTrue(node.getContentDescription().toString().contains("FICTIONAL EVENT"));
                assertTrue(node.getContentDescription().toString().contains("View results"));
                node.recycle();
            });
        }
        try(ActivityScenario<EventActivity> screen=ActivityScenario.launch(event())) {
            screen.onActivity(activity->{
                View fighter=activity.findViewById(R.id.screen_content).findViewWithTag("demo-bout-north-final/demo-fighter-aster");
                AccessibilityNodeInfo node=fighter.createAccessibilityNodeInfo();
                assertTrue(node.isClickable());assertTrue(node.getContentDescription().toString().contains("Win"));
                assertTrue(node.getContentDescription().toString().contains("Fictional demonstration"));node.recycle();
            });
        }
    }
    @Test public void partialMethodAndRoundRemainVisibleInBothScreenFormatters() {
        for(Class<?> type:new Class<?>[] {EventActivity.class,FighterActivity.class}) {
            try(ActivityScenario<ArchiveScreen> screen=ActivityScenario.launch(item(type,type==EventActivity.class ? "demo-event-northern-2025" : "demo-fighter-aster"))) {
                screen.onActivity(activity->{
                    for(Object[] values:new Object[][] {{"KO",null,"KO · Round unavailable"},{null,2,"Method unavailable · Round 2"},{null,null,"Method unavailable · Round unavailable"},{"KO",2,"KO · Round 2"}}) {
                        com.ivicastojanoski.glorykickboxingfans.archive.Archive.Bout bout=new com.ivicastojanoski.glorykickboxingfans.archive.Archive.Bout(
                                "demo-bout-partial",1,"demo-fighter-aster","demo-fighter-cedar","completed","red_win",(String)values[0],(Integer)values[1],null,null,java.util.Collections.singletonList("demo-source-original"));
                        assertEquals(values[2],activity.boutDetail(bout));
                    }
                    com.ivicastojanoski.glorykickboxingfans.archive.Archive.Bout unknown=new com.ivicastojanoski.glorykickboxingfans.archive.Archive.Bout(
                            "demo-bout-unknown",1,"demo-fighter-aster","demo-fighter-cedar","completed","unknown","KO",null,null,null,java.util.Collections.singletonList("demo-source-original"));
                    assertTrue(activity.boutDetail(unknown).contains("No winner assigned"));
                    assertTrue(activity.boutDetail(unknown).contains("KO · Round unavailable"));
                });
            }
        }
    }
    private static Activity resumed() {
        for(Activity activity: ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) return activity;
        throw new AssertionError("Resumed prototype Activity required");
    }
}
