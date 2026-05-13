import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { Observable, isObservable } from 'rxjs';

import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function callGuard(url: string): boolean | UrlTree | Observable<boolean | UrlTree> {
    return TestBed.runInInjectionContext(
      () => authGuard({} as never, { url } as never) as boolean | UrlTree | Observable<boolean | UrlTree>
    );
  }

  it('allows authenticated users straight through without HTTP', (done) => {
    const auth = TestBed.inject(AuthService);
    auth.refresh().subscribe();
    http
      .expectOne('/api/auth/me')
      .flush({ id: 'u', username: 'demo', fullName: 'Demo', email: 'demo@example.invalid' });
    expect(auth.isAuthenticated()).toBeTrue();

    const result = callGuard('/dashboard');
    if (isObservable(result)) {
      result.subscribe((value) => {
        expect(value).toBeTrue();
        done();
      });
    } else {
      expect(result).toBeTrue();
      done();
    }
  });

  it('redirects unauthenticated users to /login with redirect query', (done) => {
    const result = callGuard('/dashboard');
    expect(isObservable(result)).toBeTrue();

    (result as Observable<boolean | UrlTree>).subscribe((value) => {
      expect(value).toBeInstanceOf(UrlTree);
      const serialised = router.serializeUrl(value as UrlTree);
      expect(serialised).toContain('/login');
      expect(serialised).toContain('redirect=%2Fdashboard');
      done();
    });

    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
  });
});
