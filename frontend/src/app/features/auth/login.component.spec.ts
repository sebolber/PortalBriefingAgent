import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    // Swallow the bootstrap CSRF call triggered by the constructor.
    http.expectOne('/api/csrf').flush(null);
  });

  afterEach(() => http.verify());

  function setField(name: string, value: string): void {
    const input: HTMLInputElement = fixture.nativeElement.querySelector(`#${name}`);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('blocks submit when the form is empty', () => {
    fixture.nativeElement.querySelector('button[type="submit"]').click();
    http.expectNone('/api/auth/login');
  });

  it('navigates to dashboard after successful login', async () => {
    spyOn(router, 'navigateByUrl');

    setField('username', 'demo');
    setField('password', 'demo-password-change-me');
    fixture.nativeElement.querySelector('button[type="submit"]').click();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ username: 'demo', password: 'demo-password-change-me' });

    req.flush({
      id: 'u',
      username: 'demo',
      fullName: 'Demo',
      email: 'demo@example.invalid',
    });
    fixture.detectChanges();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('shows the credential error message on 401', () => {
    setField('username', 'demo');
    setField('password', 'wrong');
    fixture.nativeElement.querySelector('button[type="submit"]').click();

    http.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.error');
    expect(error?.textContent).toContain('Benutzername oder Passwort');
  });

  it('shows backend-unreachable message on network error', () => {
    setField('username', 'demo');
    setField('password', 'demo-password');
    fixture.nativeElement.querySelector('button[type="submit"]').click();

    http.expectOne('/api/auth/login').error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.error');
    expect(error?.textContent).toContain('Backend nicht erreichbar');
  });
});
