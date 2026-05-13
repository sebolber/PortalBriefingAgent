import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';
import { EreignisDetail, SummaryDetail } from './review.models';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getEreignis(id: string): Observable<EreignisDetail> {
    return this.http.get<EreignisDetail>(`${this.baseUrl}/api/ereignisse/${id}`);
  }

  editTranscript(id: string, transcript: string): Observable<EreignisDetail> {
    return this.http.patch<EreignisDetail>(
      `${this.baseUrl}/api/ereignisse/${id}/transcript`,
      { transcript }
    );
  }

  releaseEreignis(id: string): Observable<EreignisDetail> {
    return this.http.post<EreignisDetail>(
      `${this.baseUrl}/api/ereignisse/${id}/release`,
      null
    );
  }

  editSummary(id: string, summaryText: string): Observable<SummaryDetail> {
    return this.http.patch<SummaryDetail>(`${this.baseUrl}/api/summaries/${id}`, { summaryText });
  }

  regenerateSummary(id: string, feedback: string | null): Observable<SummaryDetail> {
    return this.http.post<SummaryDetail>(
      `${this.baseUrl}/api/summaries/${id}/regenerate`,
      feedback ? { feedback } : null
    );
  }

  acceptSummary(id: string): Observable<SummaryDetail> {
    return this.http.post<SummaryDetail>(`${this.baseUrl}/api/summaries/${id}/accept`, null);
  }
}
