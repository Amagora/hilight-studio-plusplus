package com.hilight.core;

/** Shared answer to whether a rendered frame contains visible RGB light. */
final class FrameVisibility {

    private FrameVisibility() {}

    /**
     * Ignores alpha and tiny rounding noise. The same threshold is used by the safety timer and by
     * the light-session owner, so a frame cannot be considered dark by one and lit by the other.
     */
    static boolean isVisible(int[] frame) {
        for (int color : frame) {
            int rgb = ((color >> 16) & 0xFF) + ((color >> 8) & 0xFF) + (color & 0xFF);
            if (rgb > 12) return true;
        }
        return false;
    }
}
