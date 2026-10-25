package com.ivicastojanoski.glorykickboxingfans;

/** Keeps the original content padding so repeated window-inset dispatch never accumulates it. */
final class InsetPadding {
    private final int left;
    private final int top;
    private final int right;
    private final int bottom;

    InsetPadding(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    int[] withInsets(int insetLeft, int insetTop, int insetRight, int insetBottom) {
        return new int[] {left + Math.max(0, insetLeft), top + Math.max(0, insetTop),
                right + Math.max(0, insetRight), bottom + Math.max(0, insetBottom)};
    }
}
