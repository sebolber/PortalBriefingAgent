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
        pathMatch: 'full',
        redirectTo: 'capture/text',
      },
      {
        path: 'capture/text',
        loadComponent: () =>
          import('./features/ereignis/capture-text.component').then((m) => m.CaptureTextComponent),
        title: 'Text-Notiz · Briefing Agent',
      },
      {
        path: 'capture/audio',
        loadComponent: () =>
          import('./features/ereignis/capture-audio.component').then((m) => m.CaptureAudioComponent),
        title: 'Audio-Notiz · Briefing Agent',
      },
      {
        path: 'audiences',
        loadComponent: () =>
          import('./features/audiences/audiences.component').then((m) => m.AudiencesComponent),
        title: 'Audiences · Briefing Agent',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
