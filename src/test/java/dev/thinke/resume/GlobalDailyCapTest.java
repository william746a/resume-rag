package dev.thinke.resume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GlobalDailyCapTest {

    @Test
    void capacityDecrementsAndBlocksAtLimit() {
        GlobalDailyCap cap = newGlobalDailyCapWithCap(2);

        assertTrue(cap.tryAcquire());
        assertEquals(1, cap.remaining());
        assertTrue(cap.tryAcquire());
        assertEquals(0, cap.remaining());
        assertFalse(cap.tryAcquire());
        assertEquals(0, cap.remaining());
    }

    @Test
    void resetsAtUtcMidnightBoundary() {
        GlobalDailyCap cap = newGlobalDailyCapWithCap(2);

        // Simulate yesterday with a saturated counter.
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        cap.clock = Clock.fixed(yesterday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        cap.state.set(new GlobalDailyCap.CapState(yesterday, 2));

        assertEquals(0, cap.remaining());

        // Roll the clock forward to today: cap should be available again.
        cap.clock = Clock.systemUTC();
        assertTrue(cap.tryAcquire());
        assertEquals(1, cap.remaining());
    }

    @Test
    void concurrentAcquiresStayWithinCap() throws Exception {
        int dailyCap = 1_000;
        int threads = 20;
        int attemptsPerThread = 100;
        GlobalDailyCap cap = newGlobalDailyCapWithCap(dailyCap);

        Callable<Integer> worker = () -> {
            int acquired = 0;
            for (int i = 0; i < attemptsPerThread; i++) {
                if (cap.tryAcquire()) {
                    acquired++;
                }
            }
            return acquired;
        };

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> futures = IntStream.range(0, threads)
                    .mapToObj(i -> executor.submit(worker))
                    .toList();

            int totalAcquired = 0;
            for (Future<Integer> future : futures) {
                totalAcquired += future.get();
            }

            assertTrue(
                    totalAcquired <= dailyCap,
                    "total acquired (%d) exceeded daily cap (%d)".formatted(totalAcquired, dailyCap));
            assertEquals(dailyCap, totalAcquired);
            assertEquals(0, cap.remaining());
        } finally {
            executor.shutdownNow();
        }
    }

    private static GlobalDailyCap newGlobalDailyCapWithCap(int dailyCap) {
        GlobalDailyCap cap = new GlobalDailyCap();
        cap.dailyCap = dailyCap;
        cap.meterRegistry = new SimpleMeterRegistry();
        cap.initMetrics();
        return cap;
    }
}
