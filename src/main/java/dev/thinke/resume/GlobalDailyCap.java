package dev.thinke.resume;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GlobalDailyCap {

    @ConfigProperty(name = "resume.rate-limit.global-daily-cap", defaultValue = "500")
    int dailyCap;

    private final AtomicReference<LocalDate> day = new AtomicReference<>(utcToday());
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * @return true if the request is allowed under the global daily cap
     */
    public boolean tryAcquire() {
        LocalDate today = utcToday();
        LocalDate current = day.get();
        if (!today.equals(current) && day.compareAndSet(current, today)) {
            count.set(0);
        }
        // Re-check after possible reset
        if (!today.equals(day.get())) {
            day.set(today);
            count.set(0);
        }
        int next = count.incrementAndGet();
        if (next > dailyCap) {
            count.decrementAndGet();
            return false;
        }
        return true;
    }

    public int remaining() {
        return Math.max(0, dailyCap - count.get());
    }

    private static LocalDate utcToday() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}
