package vaultWeb.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import vaultWeb.dtos.user.UserDto;
import vaultWeb.security.aspects.SecurityAuditAspect;

class SecurityAuditIntegrationTest extends IntegrationTestBase {

  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUpLogCapture() {
    logger = (Logger) LoggerFactory.getLogger(SecurityAuditAspect.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDownLogCapture() {
    logger.detachAppender(listAppender);
  }

  @Test
  void register_shouldLogSecurityEvent() throws Exception {
    // Arrange
    UserDto userDto = createUserDto("audituser", "Password1!");

    // Act
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isOk());

    // Assert
    assertSecurityEventLogged("REGISTER", "audituser", "SUCCESS");
  }

  @Test
  void login_success_shouldLogSecurityEvent() throws Exception {
    // Arrange - first register the user
    UserDto userDto = createUserDto("loginuser", "Password1!");
    registerUser(userDto);
    listAppender.list.clear();

    // Act - login
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isOk());

    // Assert
    assertSecurityEventLogged("LOGIN", "loginuser", "SUCCESS");
  }

  @Test
  void login_failure_shouldLogSecurityEventWithFailure() throws Exception {
    // Arrange - user does not exist
    UserDto userDto = createUserDto("nonexistent", "Password1!");

    // Act
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isUnauthorized());

    // Assert
    assertSecurityEventLogged("LOGIN", "nonexistent", "FAILURE");
  }

  @Test
  void logout_shouldLogSecurityEventWithUsernameFromRefreshToken() throws Exception {
    // Arrange - register and login to get a valid session
    UserDto userDto = createUserDto("logoutuser", "Password1!");
    registerUser(userDto);

    // Login to get cookies
    var loginResult =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDto)))
            .andExpect(status().isOk())
            .andReturn();

    var refreshTokenCookie = loginResult.getResponse().getCookie("refresh_token");
    listAppender.list.clear();

    // Act - logout with refresh token cookie
    mockMvc.perform(post("/api/auth/logout").cookie(refreshTokenCookie)).andExpect(status().isOk());

    // Assert - should log LOGOUT event with username extracted from refresh token cookie
    assertSecurityEventLogged("LOGOUT", "logoutuser", "SUCCESS");
  }

  @Test
  void refresh_withoutCookie_shouldLogFailure() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());

    assertSecurityEventLogged("TOKEN_REFRESH", null, "FAILURE");
  }

  private UserDto createUserDto(String username, String password) {
    UserDto userDto = new UserDto();
    userDto.setUsername(username);
    userDto.setPassword(password);
    return userDto;
  }

  private void registerUser(UserDto userDto) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isOk());
  }

  private void assertSecurityEventLogged(String eventType, String username, String status) {
    assertThat(listAppender.list)
        .anyMatch(
            event -> {
              String message = event.getFormattedMessage();
              boolean matches =
                  message.contains("SECURITY_EVENT")
                      && message.contains("type=" + eventType)
                      && message.contains("status=" + status);
              if (username != null) {
                matches = matches && message.contains("username=" + username);
              }
              return matches;
            });
  }
}
