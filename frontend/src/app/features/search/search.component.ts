import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { SearchHit, SearchService } from './search.service';

@Component({
  selector: 'ba-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss',
})
export class SearchComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(SearchService);
  private readonly destroy$ = new Subject<void>();

  @ViewChild('qInput') protected qInput?: ElementRef<HTMLInputElement>;

  protected readonly form = this.fb.nonNullable.group({
    q: [''],
  });

  protected readonly hits = signal<SearchHit[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly query = signal<string>('');

  ngOnInit(): void {
    const initial = this.route.snapshot.queryParamMap.get('q') ?? '';
    this.form.patchValue({ q: initial });
    this.query.set(initial);
    if (initial.trim()) {
      this.runSearch(initial.trim());
    }

    this.form.controls.q.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        takeUntil(this.destroy$),
        switchMap((value) => {
          const q = value?.trim() ?? '';
          this.query.set(q);
          this.error.set(null);
          if (q === '') {
            this.hits.set([]);
            return [];
          }
          this.busy.set(true);
          return this.service.search(q);
        })
      )
      .subscribe({
        next: (response) => {
          this.busy.set(false);
          this.hits.set([...(response?.hits ?? [])]);
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(this.errorMessage(err));
        },
      });
  }

  ngAfterViewInit(): void {
    queueMicrotask(() => this.qInput?.nativeElement.focus());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected hitLink(hit: SearchHit): unknown[] {
    if (hit.type === 'ereignis' || hit.type === 'summary') {
      const ereignisId = hit.id;
      return ['/review', ereignisId];
    }
    return ['/audiences', hit.type, hit.id];
  }

  protected typeLabel(type: string): string {
    return {
      person: 'Person',
      persongroup: 'Personengruppe',
      topic: 'Thema',
      ereignis: 'Ereignis',
      summary: 'Summary',
    }[type] ?? type;
  }

  private runSearch(q: string): void {
    this.busy.set(true);
    this.service.search(q).subscribe({
      next: (response) => {
        this.busy.set(false);
        this.hits.set([...(response?.hits ?? [])]);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(this.errorMessage(err));
      },
    });
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Backend nicht erreichbar.';
    if (err.status === 400) return 'Suchanfrage nicht gültig.';
    return 'Suche fehlgeschlagen.';
  }
}
