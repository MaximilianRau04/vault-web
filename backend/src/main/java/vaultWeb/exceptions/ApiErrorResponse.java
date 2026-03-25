package vaultWeb.exceptions;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {}
