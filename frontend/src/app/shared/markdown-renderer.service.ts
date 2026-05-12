import { Injectable, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';
import { marked } from 'marked';

/**
 * Renders LLM-generated Markdown to sanitised HTML for display in the
 * review screens. The pipeline is: marked → DOMPurify → SafeHtml. We
 * never trust the model's output, even when the LLM provider runs
 * on-prem.
 */
@Injectable({ providedIn: 'root' })
export class MarkdownRendererService {
  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    marked.setOptions({ gfm: true, breaks: false });
  }

  render(markdown: string | null): SafeHtml {
    if (!markdown) {
      return '';
    }
    const dirtyHtml = marked.parse(markdown, { async: false }) as string;
    const cleanHtml = DOMPurify.sanitize(dirtyHtml, {
      USE_PROFILES: { html: true },
      FORBID_TAGS: ['style', 'iframe', 'object', 'embed', 'script'],
      FORBID_ATTR: ['style', 'on*'],
    });
    return this.sanitizer.bypassSecurityTrustHtml(cleanHtml);
  }
}
