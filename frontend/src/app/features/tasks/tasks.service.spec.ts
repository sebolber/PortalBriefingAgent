import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { TasksService } from './tasks.service';

describe('TasksService', () => {
  let service: TasksService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TasksService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('GETs the task list', () => {
    service.list().subscribe();
    const req = http.expectOne('/api/tasks');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('POSTs a new task with a self-assignment payload', () => {
    service
      .create({
        title: 'Follow up',
        description: null,
        assignedToSelf: true,
        assignedToPersonId: null,
        assignedToPersonGroupId: null,
        assignedToTopicId: null,
        dueDate: null,
      })
      .subscribe();

    const req = http.expectOne('/api/tasks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.assignedToSelf).toBeTrue();
    req.flush({});
  });

  it('POSTs a status change to /tasks/:id/status', () => {
    service.changeStatus('t1', { to: 'done', note: 'ok' }).subscribe();
    const req = http.expectOne('/api/tasks/t1/status');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ to: 'done', note: 'ok' });
    req.flush({});
  });

  it('GETs notifications', () => {
    service.notifications().subscribe();
    const req = http.expectOne('/api/notifications');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
