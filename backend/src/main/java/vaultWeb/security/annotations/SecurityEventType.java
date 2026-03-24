package vaultWeb.security.annotations;

/** Enumeration of security event types for audit logging. */
public enum SecurityEventType {
  LOGIN,
  LOGOUT,
  REGISTER,
  TOKEN_REFRESH,
  PASSWORD_CHANGE
}
