import { TestBed } from '@angular/core/testing';

import { AudioRecorderService } from './audio-recorder.service';

interface FakeMediaRecorder {
  state: 'recording' | 'inactive';
  start: jasmine.Spy;
  stop: jasmine.Spy;
  addEventListener: jasmine.Spy;
  trigger(event: 'dataavailable' | 'stop' | 'error', payload?: unknown): void;
}

describe('AudioRecorderService', () => {
  let service: AudioRecorderService;
  let originalMediaDevices: MediaDevices | undefined;
  let originalMediaRecorder: typeof MediaRecorder | undefined;
  let lastFakeRecorder: FakeMediaRecorder | undefined;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AudioRecorderService);
    originalMediaDevices = navigator.mediaDevices;
    originalMediaRecorder = (window as unknown as { MediaRecorder: typeof MediaRecorder }).MediaRecorder;
  });

  afterEach(() => {
    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: originalMediaDevices,
    });
    (window as unknown as { MediaRecorder?: typeof MediaRecorder }).MediaRecorder =
      originalMediaRecorder;
    service.reset();
  });

  function installMediaStack(getUserMediaResult: MediaStream | Error): FakeMediaRecorder {
    const fakeStream = {
      getTracks: () => [{ stop: jasmine.createSpy('stop') }],
    } as unknown as MediaStream;
    const getUserMediaSpy = jasmine.createSpy('getUserMedia').and.returnValue(
      getUserMediaResult instanceof Error
        ? Promise.reject(getUserMediaResult)
        : Promise.resolve(getUserMediaResult ?? fakeStream)
    );

    Object.defineProperty(navigator, 'mediaDevices', {
      configurable: true,
      value: { getUserMedia: getUserMediaSpy } as unknown as MediaDevices,
    });

    const listeners = new Map<string, ((event: unknown) => void)[]>();
    const fake: FakeMediaRecorder = {
      state: 'inactive',
      start: jasmine.createSpy('start').and.callFake(function (this: FakeMediaRecorder) {
        this.state = 'recording';
      }),
      stop: jasmine.createSpy('stop').and.callFake(function (this: FakeMediaRecorder) {
        this.state = 'inactive';
        this.trigger('stop');
      }),
      addEventListener: jasmine
        .createSpy('addEventListener')
        .and.callFake((type: string, fn: (event: unknown) => void) => {
          const list = listeners.get(type) ?? [];
          list.push(fn);
          listeners.set(type, list);
        }),
      trigger(type, payload) {
        listeners.get(type)?.forEach((fn) => fn(payload ?? { data: new Blob(['x']) }));
      },
    };

    class FakeMediaRecorderCtor {
      static isTypeSupported(): boolean {
        return true;
      }

      constructor() {
        Object.assign(this, fake);
        lastFakeRecorder = this as unknown as FakeMediaRecorder;
      }
    }
    (window as unknown as { MediaRecorder: typeof MediaRecorder }).MediaRecorder =
      FakeMediaRecorderCtor as unknown as typeof MediaRecorder;
    return fake;
  }

  it('reports unavailable when MediaRecorder is missing', async () => {
    Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: undefined });
    (window as unknown as { MediaRecorder?: typeof MediaRecorder }).MediaRecorder = undefined;

    await service.start();

    expect(service.state()).toBe('unavailable');
    expect(service.error()).toContain('Browser');
  });

  it('marks denied when getUserMedia rejects', async () => {
    installMediaStack(new Error('NotAllowed'));

    await service.start();

    expect(service.state()).toBe('denied');
    expect(service.error()).toBeTruthy();
  });

  it('transitions to recording on a successful start', async () => {
    installMediaStack({} as unknown as MediaStream);

    await service.start();

    expect(service.state()).toBe('recording');
    expect(lastFakeRecorder?.start).toHaveBeenCalled();
  });

  it('resolves stop() with a blob containing the captured chunks', async () => {
    installMediaStack({} as unknown as MediaStream);
    await service.start();

    const stopPromise = service.stop();
    lastFakeRecorder?.trigger('dataavailable', { data: new Blob(['hello']) });
    lastFakeRecorder?.trigger('stop');
    const blob = await stopPromise;

    expect(blob.size).toBeGreaterThan(0);
    expect(service.state()).toBe('stopped');
  });

  it('rejects stop() when no recording is in progress', async () => {
    await expectAsync(service.stop()).toBeRejected();
  });

  it('reset clears state and elapsed seconds', async () => {
    installMediaStack({} as unknown as MediaStream);
    await service.start();
    service.reset();

    expect(service.state()).toBe('idle');
    expect(service.elapsedSeconds()).toBe(0);
    expect(service.error()).toBeNull();
  });
});
