import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';

import { CaptureAudioComponent } from './capture-audio.component';
import { AudioRecorderService } from './audio-recorder.service';

class FakeRecorderService {
  private readonly stateSignal = signal<'idle' | 'recording' | 'stopped' | 'denied' | 'unavailable'>('idle');
  private readonly elapsedSignal = signal(0);
  private readonly errorSignal = signal<string | null>(null);

  readonly state = this.stateSignal.asReadonly();
  readonly elapsedSeconds = this.elapsedSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly hasSoftWarning = signal(false).asReadonly();
  readonly isRecording = signal(false).asReadonly();

  startSpy = jasmine.createSpy('start').and.callFake(() => {
    this.stateSignal.set('recording');
    return Promise.resolve();
  });
  stopSpy = jasmine.createSpy('stop').and.returnValue(Promise.resolve(new Blob(['ok'], { type: 'audio/webm' })));
  resetSpy = jasmine.createSpy('reset').and.callFake(() => {
    this.stateSignal.set('idle');
    this.elapsedSignal.set(0);
    this.errorSignal.set(null);
  });

  start(): Promise<void> {
    return this.startSpy();
  }

  stop(): Promise<Blob> {
    return this.stopSpy();
  }

  reset(): void {
    this.resetSpy();
  }
}

describe('CaptureAudioComponent', () => {
  let fixture: ComponentFixture<CaptureAudioComponent>;
  let http: HttpTestingController;
  let recorder: FakeRecorderService;

  beforeEach(async () => {
    recorder = new FakeRecorderService();
    await TestBed.configureTestingModule({
      imports: [CaptureAudioComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AudioRecorderService, useValue: recorder },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CaptureAudioComponent);
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function clickButton(): void {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    button.click();
  }

  it('shows the start button by default', () => {
    expect(fixture.nativeElement.querySelector('button').textContent).toContain('Aufnahme starten');
  });

  it('starts the recorder when clicked', async () => {
    clickButton();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(recorder.startSpy).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('button').textContent).toContain('Aufnahme stoppen');
  });

  it('uploads the captured audio after stopping and shows the response', async () => {
    clickButton();
    await fixture.whenStable();
    fixture.detectChanges();

    clickButton();
    await fixture.whenStable();

    const req = http.expectOne('/api/ereignisse/audio');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();

    req.flush({
      id: 'e1',
      sourceType: 'audio',
      reviewStatus: 'pending',
      transcript: 'Hallo Welt',
      summaries: [
        { id: 's1', audienceType: 'topic', audienceName: 'My Notes', summaryText: '## ok\n\nbody' },
      ],
    });
    fixture.detectChanges();

    const result = fixture.nativeElement.querySelector('[data-testid="result"]');
    expect(result).not.toBeNull();
    expect(result.textContent).toContain('Hallo Welt');
    expect(result.textContent).toContain('My Notes');
  });

  it('shows STT-down message on 502', async () => {
    clickButton();
    await fixture.whenStable();
    fixture.detectChanges();
    clickButton();
    await fixture.whenStable();

    http.expectOne('/api/ereignisse/audio').flush(null, { status: 502, statusText: 'Bad Gateway' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.error').textContent).toContain('Speech-to-Text');
  });

  it('shows offline message when network is down', async () => {
    clickButton();
    await fixture.whenStable();
    fixture.detectChanges();
    clickButton();
    await fixture.whenStable();

    http.expectOne('/api/ereignisse/audio').error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.error').textContent).toContain('Backend nicht erreichbar');
  });
});
