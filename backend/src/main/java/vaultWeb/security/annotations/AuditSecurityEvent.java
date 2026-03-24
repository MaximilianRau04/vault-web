package vaultWeb.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should trigger security audit logging.
 *
 * <p>When applied to a controller method, the {@link vaultWeb.security.aspects.SecurityAuditAspect}
 * will log the security event with details including username, IP address, timestamp, and
 * success/failure status.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditSecurityEvent {
  SecurityEventType value();
}
