import { Injectable, computed, signal } from '@angular/core';

export type RecorderState =
  | 'idle'
  | 'requesting-permission'
  | 'recording'
  | 'stopped'
  | 'denied'
  | 'unavailable';

export const AUDIO_SOFT_WARN_SECONDS = 10 * 60;
export const AUDIO_HARD_CAP_SECONDS = 15 * 60;

/**
 * Browser MediaRecorder wrapper exposing reactive signals for state and
 * elapsed seconds. Encapsulating MediaRecorder here keeps the component
 * focused on UI and gives tests a single seam to stub.
 */
@Injectable({ providedIn: 'root' })
export class AudioRecorderService {
  private readonly stateSignal = signal<RecorderState>('idle');
  private readonly elapsedSignal = signal(0);
  private readonly errorSignal = signal<string | null>(null);

  readonly state = this.stateSignal.asReadonly();
  readonly elapsedSeconds = this.elapsedSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasSoftWarning = computed(() => this.elapsedSignal() >= AUDIO_SOFT_WARN_SECONDS);
  readonly isRecording = computed(() => this.stateSignal() === 'recording');

  private mediaRecorder?: MediaRecorder;
  private mediaStream?: MediaStream;
  private chunks: Blob[] = [];
  private tickHandle: ReturnType<typeof setInterval> | null = null;
  private startedAt = 0;
  private stopResolver?: (blob: Blob) => void;
  private stopRejector?: (reason: unknown) => void;

  async start(): Promise<void> {
    if (!('mediaDevices' in navigator) || !navigator.mediaDevices.getUserMedia) {
      this.stateSignal.set('unavailable');
      this.errorSignal.set('Audio-Aufnahme wird vom Browser nicht unterstützt.');
      return;
    }
    if (typeof MediaRecorder === 'undefined') {
      this.stateSignal.set('unavailable');
      this.errorSignal.set('MediaRecorder wird vom Browser nicht unterstützt.');
      return;
    }

    this.stateSignal.set('requesting-permission');
    this.errorSignal.set(null);

    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch {
      this.stateSignal.set('denied');
      this.errorSignal.set('Mikrofon-Zugriff abgelehnt.');
      return;
    }

    this.chunks = [];
    const mimeType = pickSupportedMime();
    this.mediaRecorder = new MediaRecorder(this.mediaStream, mimeType ? { mimeType } : undefined);

    this.mediaRecorder.addEventListener('dataavailable', (event: BlobEvent) => {
      if (event.data && event.data.size > 0) {
        this.chunks.push(event.data);
      }
    });

    this.mediaRecorder.addEventListener('error', () => {
      this.errorSignal.set('Aufnahme fehlgeschlagen.');
      this.cleanup();
      this.stateSignal.set('idle');
    });

    this.mediaRecorder.addEventListener('stop', () => {
      const blob = new Blob(this.chunks, { type: mimeType ?? 'audio/webm' });
      this.cleanup();
      this.stateSignal.set('stopped');
      if (this.stopResolver) {
        this.stopResolver(blob);
        this.stopResolver = undefined;
        this.stopRejector = undefined;
      }
    });

    this.startedAt = performance.now();
    this.elapsedSignal.set(0);
    this.mediaRecorder.start();
    this.stateSignal.set('recording');
    this.tickHandle = setInterval(() => this.onTick(), 250);
  }

  stop(): Promise<Blob> {
    if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
      return Promise.reject(new Error('No active recording'));
    }
    return new Promise<Blob>((resolve, reject) => {
      this.stopResolver = resolve;
      this.stopRejector = reject;
      this.mediaRecorder?.stop();
    });
  }

  reset(): void {
    this.cleanup();
    this.chunks = [];
    this.stateSignal.set('idle');
    this.elapsedSignal.set(0);
    this.errorSignal.set(null);
  }

  private onTick(): void {
    const elapsed = Math.floor((performance.now() - this.startedAt) / 1000);
    this.elapsedSignal.set(elapsed);
    if (elapsed >= AUDIO_HARD_CAP_SECONDS && this.mediaRecorder?.state === 'recording') {
      this.mediaRecorder.stop();
    }
  }

  private cleanup(): void {
    if (this.tickHandle !== null) {
      clearInterval(this.tickHandle);
      this.tickHandle = null;
    }
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop());
      this.mediaStream = undefined;
    }
    this.mediaRecorder = undefined;
  }
}

function pickSupportedMime(): string | undefined {
  if (typeof MediaRecorder === 'undefined') {
    return undefined;
  }
  const candidates = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/ogg;codecs=opus',
    'audio/mp4',
  ];
  return candidates.find((m) => MediaRecorder.isTypeSupported(m));
}
