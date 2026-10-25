package com.ivicastojanoski.glorykickboxingfans;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class InsetPaddingTest {
    @Test public void repeatedDispatchDoesNotAccumulatePadding() {
        InsetPadding original = new InsetPadding(8, 12, 8, 12);
        assertArrayEquals(new int[] {8, 36, 8, 60}, original.withInsets(0, 24, 0, 48));
        assertArrayEquals(new int[] {8, 36, 8, 60}, original.withInsets(0, 24, 0, 48));
        assertArrayEquals(new int[] {8, 12, 8, 12}, original.withInsets(0, 0, 0, 0));
    }

    @Test public void landscapeSideBarAndBottomChangesPreserveOriginalSpacing() {
        InsetPadding original = new InsetPadding(16, 0, 16, 0);
        assertArrayEquals(new int[] {80, 0, 16, 24}, original.withInsets(64, 0, 0, 24));
        assertArrayEquals(new int[] {16, 0, 80, 24}, original.withInsets(0, 0, 64, 24));
    }

    @Test public void InvalidNegativeInsetsCannotRemoveOriginalPadding() {
        assertArrayEquals(new int[] {8, 12, 8, 12},
                new InsetPadding(8, 12, 8, 12).withInsets(-1, -100, -1, -100));
    }
}
