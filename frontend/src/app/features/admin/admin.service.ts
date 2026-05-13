import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';

export interface AdminUserView {
  readonly id: string;
  readonly username: string;
  readonly fullName: string;
  readonly status: 'active' | 'inactive';
  readonly deactivatedAt: string | null;
  readonly deletionScheduledAt: string | null;
  readonly admin: boolean;
}

export interface AdminPersonView {
  readonly id: string;
  readonly fullName: string;
  readonly displayName: string;
  readonly tombstoned: boolean;
  readonly deletedAt: string | null;
  readonly pseudonym: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listUsers(): Observable<AdminUserView[]> {
    return this.http.get<AdminUserView[]>(`${this.baseUrl}/api/admin/users`);
  }

  deactivate(id: string): Observable<AdminUserView> {
    return this.http.post<AdminUserView>(`${this.baseUrl}/api/admin/users/${id}/deactivate`, null);
  }

  reactivate(id: string): Observable<AdminUserView> {
    return this.http.post<AdminUserView>(`${this.baseUrl}/api/admin/users/${id}/reactivate`, null);
  }

  listPersons(): Observable<AdminPersonView[]> {
    return this.http.get<AdminPersonView[]>(`${this.baseUrl}/api/admin/persons`);
  }

  tombstone(id: string): Observable<AdminPersonView> {
    return this.http.post<AdminPersonView>(`${this.baseUrl}/api/admin/persons/${id}/tombstone`, null);
  }
}
