package com.hilight.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public final class FrameVisibilityTest {

    @Test
    public void offPatternDoesNotNeedALightSession() throws Exception {
        int[] frame = new Renderer().frame(new JSONObject().put("mode", "off"), 0, 8);

        assertFalse(FrameVisibility.isVisible(frame));
    }

    @Test
    public void blackArgbPixelsDoNotNeedALightSession() {
        assertFalse(FrameVisibility.isVisible(new int[]{0, 0xFF000000, 0x01000000}));
    }

    @Test
    public void visibleRgbPixelsKeepTheLightSession() {
        assertTrue(FrameVisibility.isVisible(new int[]{0xFF000000, 0xFF0000FF}));
    }
}
