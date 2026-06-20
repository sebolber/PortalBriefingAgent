import { Component, ElementRef, HostListener, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { filter } from 'rxjs';

import { AuthService } from '../core/auth/auth.service';

interface NavItem {
  path: string;
  label: string;
  iconId: string;
  exact?: boolean;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

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
  protected readonly mobileNavOpen = signal(false);

  protected readonly navGroups: readonly NavGroup[] = [
    {
      label: 'Workspace',
      items: [
        { path: '/dashboard', label: 'Dashboard', iconId: 'icon-dashboard', exact: true },
        { path: '/capture/text', label: 'Text-Notiz', iconId: 'icon-text' },
        { path: '/capture/audio', label: 'Audio-Notiz', iconId: 'icon-audio' },
      ],
    },
    {
      label: 'Inhalte',
      items: [
        { path: '/audiences', label: 'Audiences', iconId: 'icon-audiences' },
        { path: '/tasks', label: 'Aufgaben', iconId: 'icon-tasks' },
      ],
    },
    {
      label: 'Einstellungen',
      items: [
        { path: '/configuration', label: 'Konfiguration', iconId: 'icon-config' },
        { path: '/admin', label: 'Admin', iconId: 'icon-admin' },
      ],
    },
  ];

  @ViewChild('searchInput') protected searchInput?: ElementRef<HTMLInputElement>;

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.mobileNavOpen.set(false));
  }

  @HostListener('window:keydown', ['$event'])
  protected onKeydown(event: KeyboardEvent): void {
    const target = event.target as HTMLElement | null;
    const isInput = target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName);
    if (event.key === '/' && !isInput && !event.metaKey && !event.ctrlKey && !event.altKey) {
      event.preventDefault();
      this.searchInput?.nativeElement.focus();
    }
    if (event.key === 'Escape' && this.mobileNavOpen()) {
      this.mobileNavOpen.set(false);
    }
  }

  protected toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
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
