package com.ivicastojanoski.glorykickboxingfans;

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
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Shared bounded capture/layout assertions. No live service or production hook. */
final class DeviceTestSupport {
    private DeviceTestSupport() {}
    static void noEllipsis(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getLayout() != null && !(view instanceof android.widget.EditText)) {
                for (int i = 0; i < text.getLayout().getLineCount(); i++) assertEquals("Text clipped: " + text.getText(), 0, text.getLayout().getEllipsisCount(i));
            }
        }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) noEllipsis(((ViewGroup) view).getChildAt(i));
    }
    static String shell(String command) throws Exception {
        try (ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
             FileInputStream input = new FileInputStream(descriptor.getFileDescriptor())) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(); byte[] buffer = new byte[1024]; int size;
            while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    static void safeBounds(View view) {
        View decor = view.getRootView(); WindowInsets window = decor.getRootWindowInsets(); assertNotNull(window);
        int left, top, right, bottom;
        if (Build.VERSION.SDK_INT >= 30) {
            Insets insets = window.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            left = insets.left; top = insets.top; right = insets.right; bottom = insets.bottom;
        } else { left = window.getSystemWindowInsetLeft(); top = window.getSystemWindowInsetTop(); right = window.getSystemWindowInsetRight(); bottom = window.getSystemWindowInsetBottom(); }
        int[] point = new int[2]; view.getLocationOnScreen(point);
        assertTrue(new Rect(left, top, decor.getWidth()-right, decor.getHeight()-bottom)
                .contains(new Rect(point[0], point[1], point[0]+view.getWidth(), point[1]+view.getHeight())));
    }
    static void capture(String name) throws Exception {
        Bitmap previous = null; Bitmap result = null; int unchanged = 0; long deadline = SystemClock.uptimeMillis()+5000;
        while (SystemClock.uptimeMillis() < deadline) {
            Bitmap current = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(current);
            unchanged = previous != null && current.sameAs(previous) ? unchanged+1 : 0;
            if (previous != null) previous.recycle();
            if (unchanged >= 3) { result = current; break; }
            previous = current; SystemClock.sleep(100);
        }
        assertNotNull("Capture settles within five seconds", result);
        File base = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("archive-captures"); assertNotNull(base);
        String session = InstrumentationRegistry.getArguments().getString("captureSession", "manual");
        assertTrue("Confined capture-session ID", session.matches("[a-z0-9-]{1,80}"));
        File directory = new File(base, session);
        assertTrue(directory.exists() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name+".png"))) { assertTrue(result.compress(Bitmap.CompressFormat.PNG,100,output)); }
        finally { result.recycle(); }
    }
}
