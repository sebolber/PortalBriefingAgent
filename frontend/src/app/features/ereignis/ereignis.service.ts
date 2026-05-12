import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';
import { EreignisResponse } from './ereignis.model';

@Injectable({ providedIn: 'root' })
export class EreignisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  captureText(text: string): Observable<EreignisResponse> {
    return this.http.post<EreignisResponse>(`${this.baseUrl}/api/ereignisse`, { text });
  }

  captureAudio(blob: Blob, filename: string): Observable<EreignisResponse> {
    const form = new FormData();
    form.append('audio', blob, filename);
    return this.http.post<EreignisResponse>(`${this.baseUrl}/api/ereignisse/audio`, form);
  }
}
