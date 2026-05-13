import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';
import { NotificationView, TaskCreateRequest, TaskStatusChange, TaskView } from './task.models';

@Injectable({ providedIn: 'root' })
export class TasksService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(): Observable<TaskView[]> {
    return this.http.get<TaskView[]>(`${this.baseUrl}/api/tasks`);
  }

  create(body: TaskCreateRequest): Observable<TaskView> {
    return this.http.post<TaskView>(`${this.baseUrl}/api/tasks`, body);
  }

  changeStatus(id: string, change: TaskStatusChange): Observable<TaskView> {
    return this.http.post<TaskView>(`${this.baseUrl}/api/tasks/${id}/status`, change);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/tasks/${id}`);
  }

  notifications(): Observable<NotificationView[]> {
    return this.http.get<NotificationView[]>(`${this.baseUrl}/api/notifications`);
  }
}
