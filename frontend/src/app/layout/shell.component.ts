import { Component, ElementRef, HostListener, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'ba-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ReactiveFormsModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly currentUser = this.auth.user;
  protected readonly searchForm = this.fb.nonNullable.group({ q: [''] });

  @ViewChild('searchInput') protected searchInput?: ElementRef<HTMLInputElement>;

  @HostListener('window:keydown', ['$event'])
  protected onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const isInput = target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName);
    if (event.key === '/' && !isInput && !event.metaKey && !event.ctrlKey && !event.altKey) {
      event.preventDefault();
      this.searchInput?.nativeElement.focus();
    }
  }

  protected onSearchSubmit(event: Event): void {
    event.preventDefault();
    const q = this.searchForm.controls.q.value.trim();
    if (q === '') {
      return;
    }
    this.router.navigate(['/search'], { queryParams: { q } });
  }

  protected logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => {
        this.auth.clear();
        this.router.navigate(['/login']);
      },
    });
  }
}
