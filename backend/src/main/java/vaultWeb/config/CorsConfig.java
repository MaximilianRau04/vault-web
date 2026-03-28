package vaultWeb.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Global CORS configuration Source for the application.
 *
 * <p>This configuration allows the frontend application running on a different origin (e.g.,
 * https://localhost:4200) to make HTTP requests to the backend without being blocked by the
 * browser's same-origin policy.
 *
 * <p>It allows all HTTP methods, headers, and credentials to be sent.
 */
@Configuration
public class CorsConfig {

  /**
   * Defines a CORS Configuration source that applies the CORS configuration to all endpoints.
   *
   * @return CorsFilter instance that intercepts requests and adds necessary CORS headers for
   *     allowed origins, methods, headers, and credentials.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(
        List.of(
            "http://localhost",
            "https://localhost",
            "http://localhost:*",
            "https://localhost:*",
            "http://127.0.0.1",
            "https://127.0.0.1",
            "http://127.0.0.1:*",
            "https://127.0.0.1:*",
            "http://100.*.*.*",
            "https://100.*.*.*",
            "http://100.*.*.*:*",
            "https://100.*.*.*:*",
            "http://*.vpn.internal",
            "https://*.vpn.internal",
            "http://*.vpn.internal:*",
            "https://*.vpn.internal:*"));
    config.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Cache-Control"));
    config.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    source.registerCorsConfiguration("/chat.**", config);
    source.registerCorsConfiguration("/groups/**", config);
    return source;
  }
}
