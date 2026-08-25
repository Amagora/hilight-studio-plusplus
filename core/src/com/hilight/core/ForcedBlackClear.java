package com.hilight.core;

import java.util.function.IntConsumer;

/** Two framework-distinct states that are both physically RGB black. */
final class ForcedBlackClear {

    /**
     * Android documents that a LightState's alpha channel is ignored. Changing only alpha therefore
     * defeats LightsService's full-integer state dedup without asking the LEDs to emit any colour.
     */
    static final int RETRY_COLOR = 0x01000000;
    static final int CANONICAL_COLOR = 0x00000000;

    private ForcedBlackClear() {}

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    static void apply(long minUpdatePeriodMs, IntConsumer write, Sleeper sleeper) {
        write.accept(RETRY_COLOR);
        try {
            sleeper.sleep(Math.max(1, minUpdatePeriodMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        write.accept(CANONICAL_COLOR);
    }
}
