import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { ReviewService } from './review.service';
import { EreignisDetail, SummaryDetail } from './review.models';
import { MarkdownRendererService } from '../../shared/markdown-renderer.service';

interface SummaryUiState {
  detail: SummaryDetail;
  editing: boolean;
  showFeedback: boolean;
  draftText: string;
  feedback: string;
  saving: boolean;
  error: string | null;
}

@Component({
  selector: 'ba-review',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './review.component.html',
  styleUrl: './review.component.scss',
})
export class ReviewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly reviewService = inject(ReviewService);
  private readonly markdown = inject(MarkdownRendererService);

  protected readonly ereignis = signal<EreignisDetail | null>(null);
  protected readonly summaryStates = signal<SummaryUiState[]>([]);
  protected readonly transcriptDraft = signal<string>('');
  protected readonly transcriptEditing = signal(false);
  protected readonly transcriptSaving = signal(false);
  protected readonly globalError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly transcriptForm = this.fb.nonNullable.group({
    transcript: ['', [Validators.required, Validators.maxLength(10_000)]],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('ereignisId');
    if (id) {
      this.load(id);
    }
  }

  protected renderMarkdown(text: string) {
    return this.markdown.render(text);
  }

  protected startTranscriptEdit(): void {
    const e = this.ereignis();
    if (!e) {
      return;
    }
    this.transcriptForm.setValue({ transcript: e.transcript ?? '' });
    this.transcriptDraft.set(e.transcript ?? '');
    this.transcriptEditing.set(true);
  }

  protected cancelTranscriptEdit(): void {
    this.transcriptEditing.set(false);
  }

  protected saveTranscript(): void {
    if (this.transcriptForm.invalid) {
      this.transcriptForm.markAllAsTouched();
      return;
    }
    const e = this.ereignis();
    if (!e) {
      return;
    }
    this.transcriptSaving.set(true);
    this.reviewService
      .editTranscript(e.id, this.transcriptForm.controls.transcript.value)
      .subscribe({
        next: (updated) => {
          this.ereignis.set(updated);
          this.transcriptEditing.set(false);
          this.transcriptSaving.set(false);
          this.notice.set('Transkript gespeichert.');
        },
        error: (err: HttpErrorResponse) => {
          this.transcriptSaving.set(false);
          this.globalError.set(this.errorMessage(err));
        },
      });
  }

  protected release(): void {
    const e = this.ereignis();
    if (!e) {
      return;
    }
    this.reviewService.releaseEreignis(e.id).subscribe({
      next: (updated) => {
        this.ereignis.set(updated);
        this.notice.set('Ereignis freigegeben.');
      },
      error: (err: HttpErrorResponse) => this.globalError.set(this.errorMessage(err)),
    });
  }

  protected startSummaryEdit(state: SummaryUiState): void {
    this.updateState(state.detail.id, (s) => ({
      ...s,
      editing: true,
      draftText: state.detail.summaryText,
      error: null,
    }));
  }

  protected cancelSummaryEdit(state: SummaryUiState): void {
    this.updateState(state.detail.id, (s) => ({ ...s, editing: false, error: null }));
  }

  protected updateDraft(state: SummaryUiState, value: string): void {
    this.updateState(state.detail.id, (s) => ({ ...s, draftText: value }));
  }

  protected saveSummaryEdit(state: SummaryUiState): void {
    this.updateState(state.detail.id, (s) => ({ ...s, saving: true, error: null }));
    this.reviewService.editSummary(state.detail.id, state.draftText).subscribe({
      next: (updated) => this.replaceSummary(updated, 'Summary gespeichert.'),
      error: (err: HttpErrorResponse) =>
        this.updateState(state.detail.id, (s) => ({
          ...s,
          saving: false,
          error: this.errorMessage(err),
        })),
    });
  }

  protected toggleFeedback(state: SummaryUiState): void {
    this.updateState(state.detail.id, (s) => ({
      ...s,
      showFeedback: !s.showFeedback,
      feedback: s.showFeedback ? '' : s.feedback,
    }));
  }

  protected updateFeedback(state: SummaryUiState, value: string): void {
    this.updateState(state.detail.id, (s) => ({ ...s, feedback: value }));
  }

  protected regenerate(state: SummaryUiState): void {
    this.updateState(state.detail.id, (s) => ({ ...s, saving: true, error: null }));
    const feedback = state.feedback.trim() === '' ? null : state.feedback.trim();
    this.reviewService.regenerateSummary(state.detail.id, feedback).subscribe({
      next: (updated) => this.replaceSummary(updated, 'Summary neu generiert.'),
      error: (err: HttpErrorResponse) =>
        this.updateState(state.detail.id, (s) => ({
          ...s,
          saving: false,
          error: this.errorMessage(err),
        })),
    });
  }

  protected accept(state: SummaryUiState): void {
    this.updateState(state.detail.id, (s) => ({ ...s, saving: true, error: null }));
    this.reviewService.acceptSummary(state.detail.id).subscribe({
      next: (updated) => this.replaceSummary(updated, 'Summary akzeptiert.'),
      error: (err: HttpErrorResponse) =>
        this.updateState(state.detail.id, (s) => ({
          ...s,
          saving: false,
          error: this.errorMessage(err),
        })),
    });
  }

  private load(id: string): void {
    this.reviewService.getEreignis(id).subscribe({
      next: (e) => {
        this.ereignis.set(e);
        this.summaryStates.set(
          e.summaries.map((s) => ({
            detail: this.detailFromView(s),
            editing: false,
            showFeedback: false,
            draftText: s.summaryText,
            feedback: '',
            saving: false,
            error: null,
          }))
        );
      },
      error: (err: HttpErrorResponse) => this.globalError.set(this.errorMessage(err)),
    });
  }

  private detailFromView(view: EreignisDetail['summaries'][number]): SummaryDetail {
    return {
      id: view.id,
      audienceType: view.audienceType,
      summaryText: view.summaryText,
      editState: 'ai_generated',
      acceptedAt: null,
      history: [],
    };
  }

  private replaceSummary(updated: SummaryDetail, message: string): void {
    this.notice.set(message);
    this.summaryStates.update((states) =>
      states.map((s) =>
        s.detail.id === updated.id
          ? {
              ...s,
              detail: updated,
              editing: false,
              showFeedback: false,
              feedback: '',
              draftText: updated.summaryText,
              saving: false,
              error: null,
            }
          : s
      )
    );
  }

  private updateState(id: string, mutator: (s: SummaryUiState) => SummaryUiState): void {
    this.summaryStates.update((states) =>
      states.map((s) => (s.detail.id === id ? mutator(s) : s))
    );
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 409) {
      return (err.error?.title as string | undefined) ?? 'Konflikt mit aktuellem Zustand.';
    }
    if (err.status === 400) {
      return (err.error?.title as string | undefined) ?? 'Eingabe nicht gültig.';
    }
    if (err.status === 502) {
      return 'LLM-Provider derzeit nicht erreichbar.';
    }
    if (err.status === 0) {
      return 'Backend nicht erreichbar.';
    }
    return 'Vorgang fehlgeschlagen.';
  }
}
