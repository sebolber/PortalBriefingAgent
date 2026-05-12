import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
    title: 'Login · Briefing Agent',
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'Dashboard · Briefing Agent',
      },
      {
        path: 'capture',
        loadComponent: () =>
          import('./features/ereignis/capture-text.component').then((m) => m.CaptureTextComponent),
        title: 'Neues Ereignis · Briefing Agent',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
