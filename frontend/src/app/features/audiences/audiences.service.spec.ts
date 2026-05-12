import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { AudiencesService } from './audiences.service';

describe('AudiencesService', () => {
  let service: AudiencesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AudiencesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists persons via GET /api/persons', () => {
    service.listPersons().subscribe((list) => expect(list).toEqual([]));
    const req = http.expectOne('/api/persons');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('creates a group via POST /api/persongroups', () => {
    service
      .createGroup({ name: 'Vorstand', personaText: 'strategisch', summaryRetentionMonths: null, memberIds: [] })
      .subscribe();
    const req = http.expectOne('/api/persongroups');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      name: 'Vorstand',
      personaText: 'strategisch',
      summaryRetentionMonths: null,
      memberIds: [],
    });
    req.flush({});
  });

  it('updates a topic via PATCH /api/topics/:id', () => {
    service
      .updateTopic('abc', { name: 'Alpha', personaText: 'tech', summaryRetentionMonths: 12, memberIds: ['p1'] })
      .subscribe();
    const req = http.expectOne('/api/topics/abc');
    expect(req.request.method).toBe('PATCH');
    req.flush({});
  });

  it('upserts a person persona via PUT /api/persons/:id/persona', () => {
    service.upsertPersonPersona('id-1', 'kurz').subscribe();
    const req = http.expectOne('/api/persons/id-1/persona');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ personaText: 'kurz' });
    req.flush({});
  });

  it('deletes a topic via DELETE /api/topics/:id', () => {
    service.deleteTopic('xyz').subscribe();
    const req = http.expectOne('/api/topics/xyz');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
