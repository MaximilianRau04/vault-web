package vaultWeb.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import vaultWeb.dtos.user.ChangePasswordRequest;
import vaultWeb.dtos.user.UserDto;
import vaultWeb.dtos.user.UserResponseDto;
import vaultWeb.exceptions.DuplicateUsernameException;
import vaultWeb.exceptions.UnauthorizedException;
import vaultWeb.models.User;
import vaultWeb.services.UserService;
import vaultWeb.services.auth.AuthService;
import vaultWeb.services.auth.LoginResult;
import vaultWeb.services.auth.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private UserService userService;

  @Mock private AuthService authService;

  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private UserController userController;

  // ============================================================================
  // Test Data Helper Methods
  // ============================================================================

  /**
   * Creates a test User object with the given ID and username.
   *
   * @param id the user ID
   * @param username the username
   * @return a User object for testing
   */
  private User createTestUser(Long id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setPassword("hashedPassword123");
    return user;
  }

  @Test
  void shouldRegisterUserSuccessfully() {
    UserDto userDto = new UserDto("testuser", "password");
    doNothing().when(userService).registerUser(any(User.class));
    ResponseEntity<String> response = userController.register(userDto);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("User registered successfully", response.getBody());
    verify(userService, times(1)).registerUser(any(User.class));
  }

  @Test
  void shouldLoginSuccessfully() {
    UserDto userDto = new UserDto("testuser", "password");
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    when(authService.login(userDto.getUsername(), userDto.getPassword()))
        .thenReturn(new LoginResult(createTestUser(1L, "testuser"), "mock-jwt-token"));
    doNothing().when(refreshTokenService).create(any(User.class), any(HttpServletResponse.class));
    ResponseEntity<?> response = userController.login(userDto, responseMock);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("mock-jwt-token", ((Map<String, String>) response.getBody()).get("token"));
    verify(authService, times(1)).login(userDto.getUsername(), userDto.getPassword());
    verify(refreshTokenService, times(1)).create(any(User.class), any(HttpServletResponse.class));
  }

  @Test
  void shouldRefreshTokenSuccessfully() {
    String refreshToken = "mock-refresh-token";
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    when(authService.refresh(refreshToken, responseMock))
        .thenAnswer(invocation -> ResponseEntity.ok(Map.of("token", "mock-jwt-token")));
    ResponseEntity<?> response = userController.refresh(refreshToken, responseMock);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("mock-jwt-token", ((Map<String, String>) response.getBody()).get("token"));
    verify(authService, times(1)).refresh(refreshToken, responseMock);
  }

  @Test
  void shouldLogoutSuccessfully() {
    String refreshToken = "mock-refresh-token";
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    doNothing().when(authService).logout(refreshToken, responseMock);
    ResponseEntity<?> response = userController.logout(refreshToken, responseMock);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(authService, times(1)).logout(refreshToken, responseMock);
  }

  @Test
  void shouldCheckUsernameExists_WhenExists() {

    String username = "existingUser";
    when(userService.usernameExists(username)).thenReturn(true);
    ResponseEntity<Map<String, Boolean>> response = userController.checkUsernameExists(username);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(true, ((Map<String, Boolean>) response.getBody()).get("exists"));
    verify(userService, times(1)).usernameExists(username);
  }

  @Test
  void shouldCheckUsernameExists_WhenNotExists() {
    String username = "nonExistingUser";
    when(userService.usernameExists(username)).thenReturn(false);
    ResponseEntity<Map<String, Boolean>> response = userController.checkUsernameExists(username);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(false, ((Map<String, Boolean>) response.getBody()).get("exists"));
    verify(userService, times(1)).usernameExists(username);
  }

  @Test
  void shouldGetAllUsersSuccessfully() {
    List<User> testUsers = List.of(createTestUser(1L, "user1"), createTestUser(2L, "user2"));
    when(userService.getAllUsers()).thenReturn(testUsers);
    ResponseEntity<List<UserResponseDto>> response = userController.getAllUsers();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(testUsers.size(), ((List<UserResponseDto>) response.getBody()).size());
    verify(userService, times(1)).getAllUsers();
    assertEquals(
        testUsers.get(0).getUsername(),
        ((List<UserResponseDto>) response.getBody()).get(0).getUsername());
    assertEquals(
        testUsers.get(1).getUsername(),
        ((List<UserResponseDto>) response.getBody()).get(1).getUsername());
  }

  @Test
  void shouldChangePasswordSuccessfully() {
    User user = createTestUser(1L, "testuser");
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("currentPassword");
    request.setNewPassword("newPassword");
    when(authService.getCurrentUser()).thenReturn(user);
    doNothing()
        .when(userService)
        .changePassword(user, request.getCurrentPassword(), request.getNewPassword());
    ResponseEntity<?> response = userController.changePassword(request);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(userService, times(1))
        .changePassword(user, request.getCurrentPassword(), request.getNewPassword());
  }

  // ============================================================================
  // Error Path Tests (6 tests)
  // ============================================================================

  @Test
  void shouldFailRegistration_WhenDuplicateUsername() {
    UserDto userDto = new UserDto("duplicateuser", "password");
    doThrow(new DuplicateUsernameException("Username already exists"))
        .when(userService)
        .registerUser(any(User.class));
    assertThrows(DuplicateUsernameException.class, () -> userController.register(userDto));
    verify(userService, times(1)).registerUser(any(User.class));
  }

  @Test
  void shouldFailLogin_WhenInvalidCredentials() {
    UserDto userDto = new UserDto("testuser", "wrongpassword");
    HttpServletResponse responseMock = mock(HttpServletResponse.class);

    // Mock: authService.login() throws BadCredentialsException
    when(authService.login(userDto.getUsername(), userDto.getPassword()))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    // Act & Assert: Verify the exception is thrown (GlobalExceptionHandler handles it in real app)
    assertThrows(BadCredentialsException.class, () -> userController.login(userDto, responseMock));
  }

  @Test
  void shouldFailRefresh_WhenNoTokenProvided() {
    String refreshToken = null;
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    ResponseEntity<?> response = userController.refresh(refreshToken, responseMock);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(authService, never()).refresh(anyString(), any(HttpServletResponse.class));
  }

  @Test
  void shouldFailRefresh_WhenInvalidToken() {
    String invalidToken = "invalid-token";
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    when(authService.refresh(invalidToken, responseMock))
        .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    ResponseEntity<?> response = userController.refresh(invalidToken, responseMock);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(authService, times(1)).refresh(invalidToken, responseMock);
  }

  @Test
  void shouldFailChangePassword_WhenNotAuthenticated() {

    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("currentPassword");
    request.setNewPassword("newPassword");
    when(authService.getCurrentUser()).thenReturn(null);
    assertThrows(UnauthorizedException.class, () -> userController.changePassword(request));
    verify(userService, never()).changePassword(any(User.class), anyString(), anyString());
  }

  @Test
  void shouldFailChangePassword_WhenWrongCurrentPassword() {
    User user = createTestUser(1L, "testuser");
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("wrongPassword");
    request.setNewPassword("newPassword");
    when(authService.getCurrentUser()).thenReturn(user);
    doThrow(new UnauthorizedException("Current password is incorrect"))
        .when(userService)
        .changePassword(user, request.getCurrentPassword(), request.getNewPassword());
    assertThrows(UnauthorizedException.class, () -> userController.changePassword(request));
    verify(userService, times(1))
        .changePassword(user, request.getCurrentPassword(), request.getNewPassword());
  }
}
