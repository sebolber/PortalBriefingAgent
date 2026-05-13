import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { DashboardComponent } from './dashboard.component';
import { DashboardResponse } from './dashboard.model';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows a loading state immediately and switches to loaded data', () => {
    fixture.detectChanges();
    let element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.loading')).withContext('loading state').not.toBeNull();

    const response: DashboardResponse = {
      windowDays: 7,
      ereignisse: [
        {
          id: 'e1',
          createdAt: '2026-05-12T10:00:00Z',
          sourceType: 'text',
          transcriptExcerpt: 'Workshop mit Anna',
          summaries: [
            {
              id: 's1',
              audienceType: 'topic',
              audienceName: 'My Notes',
              confidence: 'high',
              reasoning: 'Workshop direkt adressiert',
              summaryExcerpt: 'Mock-Summary',
            },
          ],
        },
      ],
    };
    http.expectOne('/api/dashboard/recent').flush(response);
    fixture.detectChanges();

    element = fixture.nativeElement;
    expect(element.querySelector('.loading')).toBeNull();
    expect(element.querySelector('.entry')?.textContent).toContain('Workshop mit Anna');
    expect(element.querySelector('.audience')?.textContent).toContain('My Notes');
  });

  it('shows an empty state when no events exist', () => {
    fixture.detectChanges();
    http
      .expectOne('/api/dashboard/recent')
      .flush({ windowDays: 7, ereignisse: [] } as DashboardResponse);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.empty')).withContext('empty state').not.toBeNull();
  });

  it('shows an error state when the API fails', () => {
    fixture.detectChanges();
    http.expectOne('/api/dashboard/recent').flush(null, {
      status: 500,
      statusText: 'Server Error',
    });
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.error')).not.toBeNull();
  });
});
