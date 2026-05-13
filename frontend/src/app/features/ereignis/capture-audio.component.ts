import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { EreignisService } from './ereignis.service';
import { EreignisResponse } from './ereignis.model';
import { AudioRecorderService, AUDIO_HARD_CAP_SECONDS, AUDIO_SOFT_WARN_SECONDS } from './audio-recorder.service';

@Component({
  selector: 'ba-capture-audio',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './capture-audio.component.html',
  styleUrl: './capture-audio.component.scss',
})
export class CaptureAudioComponent {
  private readonly ereignisService = inject(EreignisService);
  private readonly recorder = inject(AudioRecorderService);
  private readonly router = inject(Router);

  protected readonly recorderState = this.recorder.state;
  protected readonly elapsed = this.recorder.elapsedSeconds;
  protected readonly softWarning = this.recorder.hasSoftWarning;
  protected readonly recorderError = this.recorder.error;

  protected readonly uploading = signal(false);
  protected readonly result = signal<EreignisResponse | null>(null);
  protected readonly uploadError = signal<string | null>(null);

  protected readonly softWarnLabel =
    `${formatDuration(AUDIO_SOFT_WARN_SECONDS)} überschritten — Aufnahme stoppt automatisch bei ${formatDuration(AUDIO_HARD_CAP_SECONDS)}.`;

  protected readonly elapsedLabel = computed(() => formatDuration(this.elapsed()));

  protected async startRecording(): Promise<void> {
    this.uploadError.set(null);
    this.result.set(null);
    await this.recorder.start();
  }

  protected async stopRecording(): Promise<void> {
    if (this.recorderState() !== 'recording') {
      return;
    }
    try {
      const blob = await this.recorder.stop();
      await this.upload(blob);
    } catch {
      this.uploadError.set('Aufnahme konnte nicht beendet werden.');
    }
  }

  protected reset(): void {
    this.recorder.reset();
    this.result.set(null);
    this.uploadError.set(null);
  }

  private async upload(blob: Blob): Promise<void> {
    if (blob.size === 0) {
      this.uploadError.set('Keine Audio-Daten aufgenommen.');
      return;
    }
    this.uploading.set(true);
    const filename = filenameFor(blob.type);
    this.ereignisService.captureAudio(blob, filename).subscribe({
      next: (response) => {
        this.uploading.set(false);
        this.result.set(response);
      },
      error: (err: HttpErrorResponse) => {
        this.uploading.set(false);
        if (err.status === 401) {
          this.router.navigate(['/login']);
        } else if (err.status === 0) {
          this.uploadError.set('Backend nicht erreichbar.');
        } else if (err.status === 502) {
          this.uploadError.set('Speech-to-Text-Dienst ist gerade nicht erreichbar.');
        } else if (err.status === 415) {
          this.uploadError.set('Audio-Format wird nicht unterstützt.');
        } else if (err.status === 422) {
          this.uploadError.set('Whisper konnte keinen Text erkennen.');
        } else {
          this.uploadError.set('Hochladen fehlgeschlagen.');
        }
      },
    });
  }
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function filenameFor(mime: string): string {
  if (mime.startsWith('audio/webm')) {
    return 'capture.webm';
  }
  if (mime.startsWith('audio/ogg')) {
    return 'capture.ogg';
  }
  if (mime.startsWith('audio/mp4')) {
    return 'capture.m4a';
  }
  return 'capture.audio';
}
