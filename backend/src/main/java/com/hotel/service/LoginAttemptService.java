package com.hotel.service;

import com.hotel.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks password guessing: after 5 wrong attempts within 15 minutes an account (email) is locked for 15 minutes.
 * Kept in memory, so it resets when the backend restarts.
 */
@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);
    private static final int MAX_TRACKED = 10_000;

    private record Entry(int failures, Instant firstFailure, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    /** Throws 429 if this key is currently locked. */
    public void checkAllowed(String key) {
        Entry e = attempts.get(key);
        Instant now = Instant.now();
        if (e != null && e.lockedUntil() != null && now.isBefore(e.lockedUntil())) {
            long minutes = Math.max(1, Duration.between(now, e.lockedUntil()).toMinutes() + 1);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Please try again in " + minutes + " minute(s).");
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        if (attempts.size() > MAX_TRACKED) purge(now);
        attempts.compute(key, (k, e) -> {
            boolean expired = e == null
                    || (e.lockedUntil() != null && !now.isBefore(e.lockedUntil()))
                    || (e.lockedUntil() == null && now.isAfter(e.firstFailure().plus(WINDOW)));
            if (expired) return new Entry(1, now, null);
            int failures = e.failures() + 1;
            return failures >= MAX_FAILURES
                    ? new Entry(failures, e.firstFailure(), now.plus(LOCK))
                    : new Entry(failures, e.firstFailure(), null);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void purge(Instant now) {
        attempts.entrySet().removeIf(en -> {
            Entry e = en.getValue();
            return e.lockedUntil() != null ? !now.isBefore(e.lockedUntil()) : now.isAfter(e.firstFailure().plus(WINDOW));
        });
    }
}
