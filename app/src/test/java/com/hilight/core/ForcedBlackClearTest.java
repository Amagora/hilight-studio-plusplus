package com.hilight.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class ForcedBlackClearTest {

    @Test
    public void retryAndCanonicalFramesAreBothRgbBlack() {
        assertEquals(0, ForcedBlackClear.RETRY_COLOR & 0x00FFFFFF);
        assertEquals(0, ForcedBlackClear.CANONICAL_COLOR & 0x00FFFFFF);
        assertFalse(FrameVisibility.isVisible(new int[]{ForcedBlackClear.RETRY_COLOR}));
        assertFalse(FrameVisibility.isVisible(new int[]{ForcedBlackClear.CANONICAL_COLOR}));
    }

    @Test
    public void retryChangesFrameworkStateBeforeReturningToCanonicalBlack() {
        assertNotEquals(ForcedBlackClear.RETRY_COLOR, ForcedBlackClear.CANONICAL_COLOR);
        assertEquals(0, ForcedBlackClear.CANONICAL_COLOR);
    }

    @Test
    public void writesRetryThenCanonicalBlackOneUpdatePeriodApart() {
        List<Integer> writes = new ArrayList<>();
        long[] slept = {-1};

        ForcedBlackClear.apply(33, writes::add, millis -> slept[0] = millis);

        assertEquals(
                Arrays.asList(ForcedBlackClear.RETRY_COLOR, ForcedBlackClear.CANONICAL_COLOR),
                writes
        );
        assertEquals(33, slept[0]);
    }

    @Test
    public void zeroAdvertisedPeriodStillSeparatesWrites() {
        long[] slept = {-1};

        ForcedBlackClear.apply(0, ignored -> {}, millis -> slept[0] = millis);

        assertEquals(1, slept[0]);
    }
}
