package vaultWeb.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiRateLimit {
  int capacity() default 5;

  int refillTokens() default 5;

  int refillDurationMinutes() default 1;

  /** If true, uses IP address for rate limiting. If false, uses user identifier (token). */
  boolean useIpAddress() default true;
}
