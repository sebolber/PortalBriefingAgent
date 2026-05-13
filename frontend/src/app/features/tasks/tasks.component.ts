import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { TasksService } from './tasks.service';
import { TaskCreateRequest, TaskStatus, TaskView } from './task.models';

@Component({
  selector: 'ba-tasks',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly tasksService = inject(TasksService);

  protected readonly tasks = signal<TaskView[]>([]);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(4_000)]],
    dueDate: this.fb.nonNullable.control<string | null>(null),
  });

  ngOnInit(): void {
    this.refresh();
  }

  protected refresh(): void {
    this.busy.set(true);
    this.tasksService.list().subscribe({
      next: (list) => {
        this.tasks.set(list);
        this.busy.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(this.errorMessage(err));
      },
    });
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body: TaskCreateRequest = {
      title: value.title,
      description: value.description || null,
      assignedToSelf: true,
      assignedToPersonId: null,
      assignedToPersonGroupId: null,
      assignedToTopicId: null,
      dueDate: value.dueDate,
    };
    this.tasksService.create(body).subscribe({
      next: () => {
        this.notice.set('Aufgabe angelegt.');
        this.form.reset({ title: '', description: '', dueDate: null });
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected setStatus(task: TaskView, to: TaskStatus): void {
    this.tasksService.changeStatus(task.id, { to, note: null }).subscribe({
      next: () => this.refresh(),
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected remove(task: TaskView): void {
    this.tasksService.delete(task.id).subscribe({
      next: () => {
        this.notice.set('Aufgabe gelöscht.');
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected statusLabel(status: TaskStatus): string {
    return {
      open: 'Offen',
      in_progress: 'In Arbeit',
      done: 'Erledigt',
      dropped: 'Verworfen',
    }[status];
  }

  private errorMessage(err: HttpErrorResponse): string {
    if (err.status === 400 || err.status === 409) {
      return (err.error?.title as string | undefined) ?? 'Eingabe nicht gültig.';
    }
    if (err.status === 0) {
      return 'Backend nicht erreichbar.';
    }
    return 'Vorgang fehlgeschlagen.';
  }
}
