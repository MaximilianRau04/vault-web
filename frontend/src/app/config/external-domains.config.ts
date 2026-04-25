export interface ExternalDomainLink {
  name: string;
  url: string;
}

declare global {
  interface Window {
    __VAULT_WEB_EXTERNAL_LINKS__?: unknown;
  }
}

function isValidLink(value: unknown): value is { name: string; url: string } {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  const candidate = value as { name?: unknown; url?: unknown };
  return (
    typeof candidate.name === 'string' &&
    candidate.name.trim().length > 0 &&
    typeof candidate.url === 'string' &&
    candidate.url.trim().length > 0
  );
}

function resolveExternalLinks(): ExternalDomainLink[] {
  const links = window.__VAULT_WEB_EXTERNAL_LINKS__;
  if (!Array.isArray(links)) {
    return [];
  }

  return links.filter(isValidLink).map((link) => ({
    name: link.name.trim(),
    url: link.url.trim(),
  }));
}

export const EXTERNAL_DOMAIN_LINKS: ExternalDomainLink[] =
  resolveExternalLinks();
