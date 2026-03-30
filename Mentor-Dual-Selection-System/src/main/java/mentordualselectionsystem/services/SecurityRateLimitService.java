package mentordualselectionsystem.services;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityRateLimitService {

    private final Map<String, RateWindow> forgotPasswordWindows = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> resetPasswordWindows = new ConcurrentHashMap<>();

    public void checkForgotPasswordAllowed(String email, String clientIp) {
        enforceLimit(forgotPasswordWindows, "forgot:email:" + normalize(email), 5, Duration.ofMinutes(15));
        enforceLimit(forgotPasswordWindows, "forgot:ip:" + normalize(clientIp), 20, Duration.ofMinutes(15));
    }

    public void checkResetPasswordAllowed(String token, String clientIp) {
        enforceLimit(resetPasswordWindows, "reset:token:" + normalize(token), 10, Duration.ofMinutes(15));
        enforceLimit(resetPasswordWindows, "reset:ip:" + normalize(clientIp), 20, Duration.ofMinutes(15));
    }

    private void enforceLimit(Map<String, RateWindow> storage, String key, int limit, Duration windowDuration) {
        long now = System.currentTimeMillis();
        long windowMillis = windowDuration.toMillis();

        RateWindow window = storage.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                return new RateWindow(now, 1);
            }
            existing.attempts += 1;
            return existing;
        });

        if (window.attempts > limit) {
            throw new IllegalStateException("请求过于频繁，请稍后再试");
        }

        storage.entrySet().removeIf(entry -> now - entry.getValue().windowStartMillis >= windowMillis);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static final class RateWindow {
        private final long windowStartMillis;
        private int attempts;

        private RateWindow(long windowStartMillis, int attempts) {
            this.windowStartMillis = windowStartMillis;
            this.attempts = attempts;
        }
    }
}
