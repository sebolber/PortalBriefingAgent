import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AudienceDetail, SearchService } from './search.service';
import { MarkdownRendererService } from '../../shared/markdown-renderer.service';

@Component({
  selector: 'ba-audience-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './audience-detail.component.html',
  styleUrl: './audience-detail.component.scss',
})
export class AudienceDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(SearchService);
  private readonly markdown = inject(MarkdownRendererService);

  protected readonly detail = signal<AudienceDetail | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const type = this.route.snapshot.paramMap.get('type');
    const id = this.route.snapshot.paramMap.get('id');
    if (!type || !id) {
      this.error.set('Audience-Parameter fehlen.');
      return;
    }
    this.busy.set(true);
    this.service.audienceDetail(type, id).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.busy.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        if (err.status === 404) {
          this.error.set('Audience nicht gefunden.');
        } else if (err.status === 0) {
          this.error.set('Backend nicht erreichbar.');
        } else {
          this.error.set('Audience konnte nicht geladen werden.');
        }
      },
    });
  }

  protected renderMarkdown(text: string) {
    return this.markdown.render(text);
  }
}
