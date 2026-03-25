package vaultWeb.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import vaultWeb.exceptions.ApiErrorResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    log.warn(
        "Unauthorized request to {} {}",
        request.getMethod(),
        request.getRequestURI(),
        authException);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                new ApiErrorResponse("UNAUTHORIZED", "Unauthorized", Instant.now())));
  }
}
