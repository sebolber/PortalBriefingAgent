import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { DashboardService } from './dashboard.service';
import { DashboardResponse } from './dashboard.model';

type LoadState = 'idle' | 'loading' | 'loaded' | 'error';

@Component({
  selector: 'ba-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  protected readonly state = signal<LoadState>('idle');
  protected readonly data = signal<DashboardResponse | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.dashboardService.recent().subscribe({
      next: (response) => {
        this.data.set(response);
        this.state.set('loaded');
      },
      error: () => {
        this.state.set('error');
      },
    });
  }
}
