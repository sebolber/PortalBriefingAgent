import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { ReviewService } from './review.service';

describe('ReviewService', () => {
  let service: ReviewService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReviewService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('GETs the ereignis detail', () => {
    service.getEreignis('e1').subscribe();
    const req = http.expectOne('/api/ereignisse/e1');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('PATCHes the transcript', () => {
    service.editTranscript('e1', 'neu').subscribe();
    const req = http.expectOne('/api/ereignisse/e1/transcript');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ transcript: 'neu' });
    req.flush({});
  });

  it('POSTs to release endpoint', () => {
    service.releaseEreignis('e1').subscribe();
    const req = http.expectOne('/api/ereignisse/e1/release');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('PATCHes a summary', () => {
    service.editSummary('s1', 'neu').subscribe();
    const req = http.expectOne('/api/summaries/s1');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ summaryText: 'neu' });
    req.flush({});
  });

  it('regenerates a summary with optional feedback', () => {
    service.regenerateSummary('s1', 'kurz').subscribe();
    const req = http.expectOne('/api/summaries/s1/regenerate');
    expect(req.request.body).toEqual({ feedback: 'kurz' });
    req.flush({});
  });

  it('regenerates a summary with no feedback body', () => {
    service.regenerateSummary('s1', null).subscribe();
    const req = http.expectOne('/api/summaries/s1/regenerate');
    expect(req.request.body).toBeNull();
    req.flush({});
  });

  it('POSTs to accept', () => {
    service.acceptSummary('s1').subscribe();
    const req = http.expectOne('/api/summaries/s1/accept');
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
