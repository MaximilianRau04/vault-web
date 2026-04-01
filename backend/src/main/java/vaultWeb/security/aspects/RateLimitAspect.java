package vaultWeb.security.aspects;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vaultWeb.exceptions.RateLimitExceededException;
import vaultWeb.security.JwtUtil;
import vaultWeb.security.annotations.ApiRateLimit;

@Aspect
@Component
@Profile("!test")
public class RateLimitAspect {

  // Caffeine Cache with eviction
  private final Cache<String, Bucket> cache =
      Caffeine.newBuilder()
          .expireAfterAccess(Duration.ofMinutes(5)) // remove idle buckets
          .maximumSize(10_000) // safety cap
          .build();

  @Autowired private JwtUtil jwtUtil;

  @Around("@annotation(apiRateLimit)")
  public Object rateLimit(ProceedingJoinPoint joinPoint, ApiRateLimit apiRateLimit)
      throws Throwable {

    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    String key = getRateLimitKey(joinPoint, request, apiRateLimit);
    Bucket bucket = resolveBucket(key, apiRateLimit);

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      request.setAttribute("X-Rate-Limit-Remaining", probe.getRemainingTokens());
      return joinPoint.proceed();
    } else {
      long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
      if (retryAfter == 0) {
        retryAfter = 1;
      }
      throw new RateLimitExceededException(
          "Rate limit exceeded. Try again in " + retryAfter + " seconds", retryAfter);
    }
  }

  private Bucket resolveBucket(String key, ApiRateLimit rateLimit) {
    return cache.get(
        key,
        k -> {
          Bandwidth limit =
              Bandwidth.builder()
                  .capacity(rateLimit.capacity())
                  .refillGreedy(
                      rateLimit.refillTokens(),
                      Duration.ofMinutes(rateLimit.refillDurationMinutes()))
                  .build();

          return Bucket.builder().addLimit(limit).build();
        });
  }

  private String getRateLimitKey(
      ProceedingJoinPoint joinPoint, HttpServletRequest request, ApiRateLimit rateLimit) {

    String ip = getClientIpAddress(request);
    String username = jwtUtil.extractUsernameFromRequest(request);

    // If annotation says "rate-limit by IP"
    if (rateLimit.useIpAddress()) {
      return "IP:" + ip;
    }

    // Otherwise rate-limit by username if present
    if (username != null && !username.isBlank()) {
      return "USER:" + username;
    }
    // Fallback to IP-based key for anonymous users
    return "ANON:" + ip;
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    String xForwardedFor = request.getHeader("X-Forwarded-For");

    // Safety check: If header exists and isn't suspiciously long (spoofing attempt)
    if (xForwardedFor != null && !xForwardedFor.isBlank() && xForwardedFor.length() < 100) {
      return xForwardedFor.split(",")[0].trim();
    }

    return remoteAddr;
  }
}
