import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';
import { AuthUser } from './user.model';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts unauthenticated', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
  });

  it('stores the user signal after a successful login', () => {
    const expected: AuthUser = {
      id: '11111111-1111-1111-1111-111111111111',
      username: 'demo',
      fullName: 'Demo Author',
      email: 'demo@example.invalid',
    };

    service.login('demo', 'pw').subscribe((user) => expect(user).toEqual(expected));

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'demo', password: 'pw' });
    req.flush(expected);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.user()).toEqual(expected);
  });

  it('clears the user on logout', () => {
    const user: AuthUser = {
      id: '11111111-1111-1111-1111-111111111111',
      username: 'demo',
      fullName: 'Demo',
      email: 'demo@example.invalid',
    };

    service.login('demo', 'pw').subscribe();
    http.expectOne('/api/auth/login').flush(user);
    expect(service.isAuthenticated()).toBeTrue();

    service.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null);

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
  });

  it('refresh swallows 401 and reports null', (done) => {
    service.refresh().subscribe((result) => {
      expect(result).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
      done();
    });

    http
      .expectOne('/api/auth/me')
      .flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  it('refresh hydrates the user signal on success', (done) => {
    const user: AuthUser = {
      id: 'abcd',
      username: 'demo',
      fullName: 'Demo',
      email: 'demo@example.invalid',
    };

    service.refresh().subscribe((result) => {
      expect(result).toEqual(user);
      expect(service.isAuthenticated()).toBeTrue();
      done();
    });

    http.expectOne('/api/auth/me').flush(user);
  });

  it('bootstrapCsrf hits the dedicated endpoint', () => {
    service.bootstrapCsrf().subscribe();
    const req = http.expectOne('/api/csrf');
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });
});
