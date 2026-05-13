import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';

export interface SearchHit {
  readonly type: 'person' | 'persongroup' | 'topic' | 'ereignis' | 'summary';
  readonly id: string;
  readonly label: string;
  readonly snippet: string | null;
  readonly rank: number;
}

export interface SearchResponse {
  readonly query: string;
  readonly hits: readonly SearchHit[];
}

export interface AudienceSummary {
  readonly id: string;
  readonly summaryText: string;
  readonly createdAt: string;
}

export interface AudienceTask {
  readonly id: string;
  readonly title: string;
  readonly status: string;
  readonly dueDate: string | null;
}

export interface AudienceMember {
  readonly id: string;
  readonly displayName: string;
}

export interface AudienceDetail {
  readonly type: 'person' | 'persongroup' | 'topic';
  readonly id: string;
  readonly name: string;
  readonly personaText: string | null;
  readonly members: readonly AudienceMember[];
  readonly summaries: readonly AudienceSummary[];
  readonly openTasks: readonly AudienceTask[];
}

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  search(q: string): Observable<SearchResponse> {
    return this.http.get<SearchResponse>(`${this.baseUrl}/api/search`, {
      params: new HttpParams().set('q', q),
    });
  }

  audienceDetail(type: string, id: string): Observable<AudienceDetail> {
    return this.http.get<AudienceDetail>(`${this.baseUrl}/api/audiences/${type}/${id}`);
  }
}
