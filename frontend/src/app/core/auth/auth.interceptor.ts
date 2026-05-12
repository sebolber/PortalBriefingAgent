import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Ensures every request carries the session cookie so the backend can
 * authenticate via {@code JSESSIONID}. CSRF handling is delegated to the
 * Angular HttpClient's built-in XSRF interceptor configured in app.config.ts.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
