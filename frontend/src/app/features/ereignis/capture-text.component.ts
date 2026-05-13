import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { EreignisService } from './ereignis.service';
import { EreignisResponse, TEXT_HARD_CAP_CHARS } from './ereignis.model';

@Component({
  selector: 'ba-capture-text',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './capture-text.component.html',
  styleUrl: './capture-text.component.scss',
})
export class CaptureTextComponent {
  private readonly fb = inject(FormBuilder);
  private readonly ereignisService = inject(EreignisService);
  private readonly router = inject(Router);

  protected readonly hardCap = TEXT_HARD_CAP_CHARS;

  protected readonly form = this.fb.nonNullable.group({
    text: ['', [Validators.required, Validators.maxLength(TEXT_HARD_CAP_CHARS)]],
  });

  protected readonly submitting = signal(false);
  protected readonly result = signal<EreignisResponse | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly remainingChars = computed(() => {
    const value = this.form.controls.text.value ?? '';
    return TEXT_HARD_CAP_CHARS - value.length;
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.submitting.set(true);

    const { text } = this.form.getRawValue();
    this.ereignisService.captureText(text).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.result.set(response);
      },
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        if (err.status === 400) {
          const detail = (err.error?.errors?.[0] as string | undefined) ?? err.error?.title;
          this.error.set(detail ?? 'Eingabe nicht gültig.');
        } else if (err.status === 401) {
          this.router.navigate(['/login']);
        } else if (err.status === 0) {
          this.error.set('Backend nicht erreichbar.');
        } else {
          this.error.set('Speichern fehlgeschlagen.');
        }
      },
    });
  }

  protected reset(): void {
    this.form.reset({ text: '' });
    this.result.set(null);
    this.error.set(null);
  }
}
