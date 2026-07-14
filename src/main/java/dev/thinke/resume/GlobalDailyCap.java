package dev.thinke.resume;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Single-instance, in-memory global daily request cap.
 *
 * <p>The count resets at UTC midnight. Because the state is held in an {@link AtomicReference}, both
 * day rollover and acquire checks are race-free under concurrent load.
 *
 * <p><strong>Single-instance assumption:</strong> This implementation is correct only when exactly
 * one Cloud Run instance is running. The deployment is pinned with {@code --max-instances=1}; do
 * not raise it without switching to an external counter (e.g. Redis or Firestore).
 */
@ApplicationScoped
public class GlobalDailyCap {

    @ConfigProperty(name = "resume.rate-limit.global-daily-cap", defaultValue = "500")
    int dailyCap;

    @Inject
    MeterRegistry meterRegistry;

    /** Package-private for tests that need to simulate UTC midnight rollover. */
    Clock clock = Clock.systemUTC();

    record CapState(LocalDate day, int count) {}

    /** Package-private for unit tests that need to seed a specific day/count. */
    final AtomicReference<CapState> state = new AtomicReference<>(new CapState(utcToday(), 0));

    private Counter exhaustedCounter;

    @PostConstruct
    void initMetrics() {
        exhaustedCounter = meterRegistry.counter("resume.rate-limit.global-cap.exhausted");
        meterRegistry.gauge("resume.rate-limit.global-cap.remaining", this, GlobalDailyCap::remaining);
    }

    /**
     * @return {@code true} if the request is allowed under the global daily cap
     */
    public boolean tryAcquire() {
        while (true) {
            LocalDate today = utcToday();
            CapState current = state.get();

            if (!today.equals(current.day())) {
                CapState reset = new CapState(today, 0);
                if (!state.compareAndSet(current, reset)) {
                    continue; // another thread changed state; retry from the top
                }
                current = reset;
            }

            if (current.count() >= dailyCap) {
                exhaustedCounter.increment();
                return false;
            }

            CapState next = new CapState(today, current.count() + 1);
            if (state.compareAndSet(current, next)) {
                return true;
            }
            // else another thread raced; retry
        }
    }

    public int remaining() {
        CapState current = state.get();
        LocalDate today = utcToday();
        if (!today.equals(current.day())) {
            return dailyCap;
        }
        return Math.max(0, dailyCap - current.count());
    }

    private LocalDate utcToday() {
        return LocalDate.now(clock);
    }
}
