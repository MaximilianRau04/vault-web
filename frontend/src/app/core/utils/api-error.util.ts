export interface ApiErrorPayload {
  code?: string;
  message?: string;
}

export function extractApiErrorPayload(error: unknown): ApiErrorPayload | null {
  const raw = (error as { error?: unknown } | null)?.error;
  if (!raw) {
    return null;
  }

  if (typeof raw === 'object') {
    return raw as ApiErrorPayload;
  }

  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw) as unknown;
      if (typeof parsed === 'object' && parsed !== null) {
        return parsed as ApiErrorPayload;
      }
      return { message: raw };
    } catch {
      return { message: raw };
    }
  }

  return null;
}
