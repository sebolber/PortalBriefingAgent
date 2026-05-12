import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';

import { API_BASE_URL } from '../api/api.config';
import { AuthUser } from './user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  private readonly currentUser = signal<AuthUser | null>(null);

  readonly user = computed(() => this.currentUser());
  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  bootstrapCsrf(): Observable<void> {
    return this.http.get<void>(`${this.baseUrl}/api/csrf`);
  }

  login(username: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.baseUrl}/api/auth/login`, { username, password })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.baseUrl}/api/auth/logout`, null)
      .pipe(tap(() => this.currentUser.set(null)));
  }

  refresh(): Observable<AuthUser | null> {
    return this.http.get<AuthUser>(`${this.baseUrl}/api/auth/me`).pipe(
      tap((user) => this.currentUser.set(user)),
      map((user) => user),
      catchError(() => {
        this.currentUser.set(null);
        return of(null);
      })
    );
  }

  clear(): void {
    this.currentUser.set(null);
  }
}
