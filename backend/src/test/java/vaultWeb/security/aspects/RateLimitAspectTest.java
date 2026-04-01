package vaultWeb.security.aspects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vaultWeb.security.JwtUtil;
import vaultWeb.security.annotations.ApiRateLimit;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

  @Mock private JwtUtil jwtUtil;
  @Mock private HttpServletRequest request;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private ApiRateLimit apiRateLimit;

  @InjectMocks private RateLimitAspect rateLimitAspect;

  @BeforeEach
  void setUp() {
    ServletRequestAttributes attrs = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(attrs);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void getRateLimitKey_AnonymousUser_ReturnsStableIpKey() throws Exception {
    // Arrange
    String clientIp = "192.168.1.1";
    when(request.getRemoteAddr()).thenReturn(clientIp);
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(jwtUtil.extractUsernameFromRequest(request)).thenReturn(null);
    when(apiRateLimit.useIpAddress()).thenReturn(true);

    // Act: Using reflection to test the private helper method
    Method method =
        RateLimitAspect.class.getDeclaredMethod(
            "getRateLimitKey",
            org.aspectj.lang.ProceedingJoinPoint.class,
            jakarta.servlet.http.HttpServletRequest.class,
            vaultWeb.security.annotations.ApiRateLimit.class);
    method.setAccessible(true);

    String key1 = (String) method.invoke(rateLimitAspect, joinPoint, request, apiRateLimit);
    String key2 = (String) method.invoke(rateLimitAspect, joinPoint, request, apiRateLimit);

    // Assert: Keys must be identical (No UUID bypass)
    assertThat(key1).isEqualTo("IP:" + clientIp);
    assertThat(key1).isEqualTo(key2);
  }

  @Test
  void getClientIpAddress_WithXForwardedFor_ReturnsFirstIp() throws Exception {
    // Arrange
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18");

    // Act
    Method method =
        RateLimitAspect.class.getDeclaredMethod(
            "getClientIpAddress", jakarta.servlet.http.HttpServletRequest.class);
    method.setAccessible(true);
    String result = (String) method.invoke(rateLimitAspect, request);

    // Assert
    assertThat(result).isEqualTo("203.0.113.195");
  }
}
