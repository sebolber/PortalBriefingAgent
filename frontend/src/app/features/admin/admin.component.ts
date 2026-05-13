import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminPersonView, AdminService, AdminUserView } from './admin.service';

@Component({
  selector: 'ba-admin',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  private readonly admin = inject(AdminService);

  protected readonly users = signal<AdminUserView[]>([]);
  protected readonly persons = signal<AdminPersonView[]>([]);
  protected readonly notice = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refresh();
  }

  protected refresh(): void {
    this.admin.listUsers().subscribe({
      next: (list) => this.users.set(list),
      error: (err: HttpErrorResponse) => this.error.set(this.message(err)),
    });
    this.admin.listPersons().subscribe({
      next: (list) => this.persons.set(list),
      error: (err: HttpErrorResponse) => this.error.set(this.message(err)),
    });
  }

  protected deactivate(user: AdminUserView): void {
    this.admin.deactivate(user.id).subscribe({
      next: () => {
        this.notice.set(`${user.username} deaktiviert.`);
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.message(err)),
    });
  }

  protected reactivate(user: AdminUserView): void {
    this.admin.reactivate(user.id).subscribe({
      next: () => {
        this.notice.set(`${user.username} reaktiviert.`);
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.message(err)),
    });
  }

  protected tombstone(person: AdminPersonView): void {
    this.admin.tombstone(person.id).subscribe({
      next: () => {
        this.notice.set(`${person.fullName} tombstoned.`);
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.message(err)),
    });
  }

  private message(err: HttpErrorResponse): string {
    if (err.status === 403) return 'Nur Admins dürfen diese Aktion ausführen.';
    if (err.status === 409) return (err.error?.title as string | undefined) ?? 'Konflikt.';
    if (err.status === 0) return 'Backend nicht erreichbar.';
    return 'Vorgang fehlgeschlagen.';
  }
}
