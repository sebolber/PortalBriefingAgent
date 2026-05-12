import { InjectionToken } from '@angular/core';

/**
 * Absolute base URL for the Briefing Agent backend. In development the
 * Angular dev server proxies '/api' to the Spring Boot instance; in
 * production the SPA is served from the same origin as the API. Either
 * way, an empty string keeps requests same-origin and lets the browser's
 * cookie + CSRF wiring work without explicit overrides.
 */
export const API_BASE_URL = new InjectionToken<string>('briefingagent.api.base-url', {
  providedIn: 'root',
  factory: () => '',
});
