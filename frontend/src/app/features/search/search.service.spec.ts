import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { SearchService } from './search.service';

describe('SearchService', () => {
  let service: SearchService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SearchService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('GETs /api/search with the q param', () => {
    service.search('Anna Müller').subscribe();
    const req = http.expectOne((r) => r.url === '/api/search' && r.params.get('q') === 'Anna Müller');
    expect(req.request.method).toBe('GET');
    req.flush({ query: 'Anna Müller', hits: [] });
  });

  it('GETs the audience detail endpoint for the given type/id', () => {
    service.audienceDetail('topic', 'topic-1').subscribe();
    const req = http.expectOne('/api/audiences/topic/topic-1');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });
});
