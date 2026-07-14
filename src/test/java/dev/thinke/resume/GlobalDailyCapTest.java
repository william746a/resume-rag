package dev.thinke.resume;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobalDailyCapTest {

    @Test
    void enforcesConfiguredCap() {
        GlobalDailyCap cap = new GlobalDailyCap();
        cap.dailyCap = 2;

        assertTrue(cap.tryAcquire());
        assertTrue(cap.tryAcquire());
        assertFalse(cap.tryAcquire());
    }
}
