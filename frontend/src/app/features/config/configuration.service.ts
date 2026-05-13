import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../core/api/api.config';
import {
  LlmProviderRequest,
  LlmProviderView,
  LlmPurpose,
  PlaceholderInfo,
  PromptTemplateView,
  SttProviderRequest,
  SttProviderView,
} from './configuration.models';

interface TestResult {
  readonly success: boolean;
  readonly message: string;
  readonly latencyMs: number;
}

@Injectable({ providedIn: 'root' })
export class ConfigurationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  listLlmProviders(): Observable<LlmProviderView[]> {
    return this.http.get<LlmProviderView[]>(`${this.baseUrl}/api/llm-providers`);
  }

  createLlmProvider(body: LlmProviderRequest): Observable<LlmProviderView> {
    return this.http.post<LlmProviderView>(`${this.baseUrl}/api/llm-providers`, body);
  }

  setLlmUsage(id: string, purpose: LlmPurpose, active: boolean): Observable<LlmProviderView> {
    return this.http.post<LlmProviderView>(
      `${this.baseUrl}/api/llm-providers/${id}/usages`,
      { purpose, active }
    );
  }

  testLlmProvider(id: string): Observable<TestResult> {
    return this.http.post<TestResult>(`${this.baseUrl}/api/llm-providers/${id}/test`, null);
  }

  deleteLlmProvider(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/llm-providers/${id}`);
  }

  listSttProviders(): Observable<SttProviderView[]> {
    return this.http.get<SttProviderView[]>(`${this.baseUrl}/api/stt-providers`);
  }

  createSttProvider(body: SttProviderRequest): Observable<SttProviderView> {
    return this.http.post<SttProviderView>(`${this.baseUrl}/api/stt-providers`, body);
  }

  setSttActivation(id: string, active: boolean): Observable<SttProviderView> {
    return this.http.post<SttProviderView>(
      `${this.baseUrl}/api/stt-providers/${id}/activation`,
      { active }
    );
  }

  testSttProvider(id: string): Observable<TestResult> {
    return this.http.post<TestResult>(`${this.baseUrl}/api/stt-providers/${id}/test`, null);
  }

  deleteSttProvider(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/stt-providers/${id}`);
  }

  listPromptTemplates(): Observable<PromptTemplateView[]> {
    return this.http.get<PromptTemplateView[]>(`${this.baseUrl}/api/prompt-templates`);
  }

  savePromptTemplate(purpose: LlmPurpose, content: string): Observable<PromptTemplateView> {
    return this.http.post<PromptTemplateView>(
      `${this.baseUrl}/api/prompt-templates`,
      { purpose, content }
    );
  }

  restorePromptTemplate(id: string): Observable<PromptTemplateView> {
    return this.http.post<PromptTemplateView>(
      `${this.baseUrl}/api/prompt-templates/${id}/restore`,
      null
    );
  }

  placeholders(purpose: LlmPurpose): Observable<PlaceholderInfo> {
    return this.http.get<PlaceholderInfo>(
      `${this.baseUrl}/api/prompt-templates/placeholders/${purpose}`
    );
  }
}
