import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ConfigurationService } from './configuration.service';
import {
  LlmProviderRequest,
  LlmProviderView,
  LlmPurpose,
  PromptTemplateView,
  SttProviderRequest,
  SttProviderView,
} from './configuration.models';

const PURPOSES: LlmPurpose[] = [
  'audience_classification',
  'summary_generation',
  'task_extraction',
  'transcript_correction',
];

@Component({
  selector: 'ba-configuration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DatePipe],
  templateUrl: './configuration.component.html',
  styleUrl: './configuration.component.scss',
})
export class ConfigurationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ConfigurationService);

  protected readonly purposes = PURPOSES;
  protected readonly llmProviders = signal<LlmProviderView[]>([]);
  protected readonly sttProviders = signal<SttProviderView[]>([]);
  protected readonly templates = signal<PromptTemplateView[]>([]);
  protected readonly busy = signal(false);
  protected readonly notice = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly llmForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    endpointUrl: ['', [Validators.required, Validators.maxLength(500)]],
    modelName: ['', [Validators.required, Validators.maxLength(200)]],
    apiKeySecretRef: ['', [Validators.maxLength(200)]],
    apiType: ['openai_compatible', [Validators.required, Validators.maxLength(50)]],
  });

  protected readonly sttForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    endpointUrl: ['', [Validators.required, Validators.maxLength(500)]],
    modelName: ['whisper-large-v3', [Validators.required, Validators.maxLength(200)]],
    apiKeySecretRef: ['', [Validators.maxLength(200)]],
  });

  protected readonly promptForm = this.fb.nonNullable.group({
    purpose: ['summary_generation' as LlmPurpose, [Validators.required]],
    content: ['', [Validators.required, Validators.maxLength(50_000)]],
  });

  ngOnInit(): void {
    this.refresh();
  }

  protected refresh(): void {
    this.busy.set(true);
    this.service.listLlmProviders().subscribe({
      next: (list) => this.llmProviders.set(list),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
    this.service.listSttProviders().subscribe({
      next: (list) => this.sttProviders.set(list),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
    this.service.listPromptTemplates().subscribe({
      next: (list) => {
        this.templates.set(list);
        this.busy.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(this.errorMessage(err));
      },
    });
  }

  protected createLlmProvider(): void {
    if (this.llmForm.invalid) {
      this.llmForm.markAllAsTouched();
      return;
    }
    const value = this.llmForm.getRawValue();
    const body: LlmProviderRequest = {
      name: value.name,
      endpointUrl: value.endpointUrl,
      modelName: value.modelName,
      apiKeySecretRef: value.apiKeySecretRef || null,
      apiType: value.apiType || null,
    };
    this.service.createLlmProvider(body).subscribe({
      next: () => {
        this.notice.set('LLM-Provider angelegt.');
        this.llmForm.reset({
          name: '', endpointUrl: '', modelName: '', apiKeySecretRef: '', apiType: 'openai_compatible',
        });
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected isPurposeActive(provider: LlmProviderView, purpose: LlmPurpose): boolean {
    return provider.usages.some((u) => u.purpose === purpose && u.active);
  }

  protected toggleLlmUsage(provider: LlmProviderView, purpose: LlmPurpose, active: boolean): void {
    this.service.setLlmUsage(provider.id, purpose, active).subscribe({
      next: () => this.refresh(),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected testLlm(provider: LlmProviderView): void {
    this.service.testLlmProvider(provider.id).subscribe({
      next: (result) =>
        this.notice.set(
          `${provider.name}: ${result.success ? '✓' : '✗'} ${result.message} (${result.latencyMs} ms)`
        ),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected deleteLlmProvider(provider: LlmProviderView): void {
    this.service.deleteLlmProvider(provider.id).subscribe({
      next: () => {
        this.notice.set(`${provider.name} gelöscht.`);
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected createSttProvider(): void {
    if (this.sttForm.invalid) {
      this.sttForm.markAllAsTouched();
      return;
    }
    const value = this.sttForm.getRawValue();
    const body: SttProviderRequest = {
      name: value.name,
      endpointUrl: value.endpointUrl,
      modelName: value.modelName,
      apiKeySecretRef: value.apiKeySecretRef || null,
    };
    this.service.createSttProvider(body).subscribe({
      next: () => {
        this.notice.set('STT-Provider angelegt.');
        this.sttForm.reset({
          name: '', endpointUrl: '', modelName: 'whisper-large-v3', apiKeySecretRef: '',
        });
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected toggleSttActivation(provider: SttProviderView): void {
    this.service.setSttActivation(provider.id, !provider.active).subscribe({
      next: () => this.refresh(),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected testStt(provider: SttProviderView): void {
    this.service.testSttProvider(provider.id).subscribe({
      next: (result) =>
        this.notice.set(
          `${provider.name}: ${result.success ? '✓' : '✗'} ${result.message} (${result.latencyMs} ms)`
        ),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected deleteSttProvider(provider: SttProviderView): void {
    this.service.deleteSttProvider(provider.id).subscribe({
      next: () => {
        this.notice.set(`${provider.name} gelöscht.`);
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected savePromptTemplate(): void {
    if (this.promptForm.invalid) {
      this.promptForm.markAllAsTouched();
      return;
    }
    const value = this.promptForm.getRawValue();
    this.service.savePromptTemplate(value.purpose, value.content).subscribe({
      next: () => {
        this.notice.set('Prompt-Template gespeichert (neue Version).');
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected restoreTemplate(t: PromptTemplateView): void {
    this.service.restorePromptTemplate(t.id).subscribe({
      next: () => {
        this.notice.set(`${t.purpose} v${t.version} aktiviert.`);
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 400 || err.status === 409) {
      return (err.error?.title as string | undefined) ?? 'Eingabe nicht gültig.';
    }
    if (err.status === 0) {
      return 'Backend nicht erreichbar.';
    }
    return 'Vorgang fehlgeschlagen.';
  }
}
