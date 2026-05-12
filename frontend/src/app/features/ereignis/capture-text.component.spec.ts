import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { CaptureTextComponent } from './capture-text.component';
import { EreignisResponse, TEXT_HARD_CAP_CHARS } from './ereignis.model';

describe('CaptureTextComponent', () => {
  let fixture: ComponentFixture<CaptureTextComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaptureTextComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(CaptureTextComponent);
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function setText(value: string): void {
    const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    textarea.value = value;
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('disables submit when textarea is empty (form invalid)', () => {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    button.click();
    fixture.detectChanges();

    http.expectNone('/api/ereignisse');
    const textarea: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    expect(textarea.getAttribute('aria-invalid')).toBe('true');
  });

  it('renders the response after a successful capture', () => {
    setText('Heute habe ich Anna getroffen.');

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    button.click();

    const req = http.expectOne('/api/ereignisse');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ text: 'Heute habe ich Anna getroffen.' });

    const response: EreignisResponse = {
      id: 'e1',
      sourceType: 'text',
      reviewStatus: 'pending',
      transcript: 'Heute habe ich Anna getroffen.',
      summaries: [
        {
          id: 's1',
          audienceType: 'topic',
          audienceName: 'My Notes',
          summaryText: '## Mock\n\nbody',
        },
      ],
    };
    req.flush(response);
    fixture.detectChanges();

    const result = fixture.nativeElement.querySelector('[data-testid="result"]');
    expect(result).not.toBeNull();
    expect(result.textContent).toContain('My Notes');
    expect(result.textContent).toContain('Mock');
  });

  it('shows the backend validation message on 400', () => {
    setText('x');

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    button.click();

    http.expectOne('/api/ereignisse').flush(
      { type: 'about:blank', title: 'Text must not be empty', status: 400 },
      { status: 400, statusText: 'Bad Request' }
    );
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.error');
    expect(error?.textContent).toContain('Text must not be empty');
  });

  it('shows offline message when backend is unreachable', () => {
    setText('hi');

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    button.click();

    http.expectOne('/api/ereignisse').error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.error');
    expect(error?.textContent).toContain('Backend nicht erreichbar');
  });

  it('counts down remaining characters as the user types', () => {
    setText('hello');

    const hint = fixture.nativeElement.querySelector('.hint');
    expect(hint.textContent).toContain((TEXT_HARD_CAP_CHARS - 5).toString());
  });
});
