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
      {
        path: 'tasks',
        loadComponent: () =>
          import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
        title: 'Aufgaben · Briefing Agent',
      },
      {
        path: 'configuration',
        loadComponent: () =>
          import('./features/config/configuration.component').then((m) => m.ConfigurationComponent),
        title: 'Konfiguration · Briefing Agent',
      },
      {
        path: 'admin',
        loadComponent: () =>
          import('./features/admin/admin.component').then((m) => m.AdminComponent),
        title: 'Admin · Briefing Agent',
      },
      {
        path: 'review/:ereignisId',
        loadComponent: () =>
          import('./features/review/review.component').then((m) => m.ReviewComponent),
        title: 'Review · Briefing Agent',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
