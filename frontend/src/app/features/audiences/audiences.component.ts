import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AudiencesService } from './audiences.service';
import {
  GroupRequest,
  GroupView,
  PersonRequest,
  PersonView,
  TopicRequest,
  TopicView,
} from './audience.models';

type Tab = 'persons' | 'groups' | 'topics';

@Component({
  selector: 'ba-audiences',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './audiences.component.html',
  styleUrl: './audiences.component.scss',
})
export class AudiencesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly audiencesService = inject(AudiencesService);

  protected readonly activeTab = signal<Tab>('persons');
  protected readonly persons = signal<PersonView[]>([]);
  protected readonly groups = signal<GroupView[]>([]);
  protected readonly topics = signal<TopicView[]>([]);

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly editingPersonId = signal<string | null>(null);
  protected readonly editingGroupId = signal<string | null>(null);
  protected readonly editingTopicId = signal<string | null>(null);

  protected readonly memberOptions = computed(() =>
    this.persons().filter((p) => !p.tombstoned)
  );

  protected readonly personForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.maxLength(255)]],
    role: ['', [Validators.maxLength(200)]],
    company: ['', [Validators.maxLength(200)]],
  });

  protected readonly groupForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    personaText: ['', [Validators.required, Validators.maxLength(4000)]],
    summaryRetentionMonths: this.fb.nonNullable.control<number | null>(null),
    memberIds: this.fb.nonNullable.control<string[]>([]),
  });

  protected readonly topicForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    personaText: ['', [Validators.required, Validators.maxLength(4000)]],
    summaryRetentionMonths: this.fb.nonNullable.control<number | null>(null),
    memberIds: this.fb.nonNullable.control<string[]>([]),
  });

  ngOnInit(): void {
    this.refresh();
  }

  protected switchTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.error.set(null);
    this.notice.set(null);
  }

  protected refresh(): void {
    this.busy.set(true);
    this.audiencesService.listPersons().subscribe({
      next: (list) => this.persons.set(list),
      error: () => this.error.set('Personen konnten nicht geladen werden.'),
    });
    this.audiencesService.listGroups().subscribe({
      next: (list) => this.groups.set(list),
      error: () => this.error.set('Personengruppen konnten nicht geladen werden.'),
    });
    this.audiencesService.listTopics().subscribe({
      next: (list) => {
        this.topics.set(list);
        this.busy.set(false);
      },
      error: () => {
        this.busy.set(false);
        this.error.set('Themen konnten nicht geladen werden.');
      },
    });
  }

  protected editPerson(p: PersonView): void {
    this.editingPersonId.set(p.id);
    this.personForm.reset({
      fullName: p.fullName,
      email: p.email ?? '',
      role: p.role ?? '',
      company: p.company ?? '',
    });
  }

  protected cancelPersonEdit(): void {
    this.editingPersonId.set(null);
    this.personForm.reset();
  }

  protected savePerson(): void {
    if (this.personForm.invalid) {
      this.personForm.markAllAsTouched();
      return;
    }
    const body: PersonRequest = this.personForm.getRawValue();
    const id = this.editingPersonId();
    const obs = id ? this.audiencesService.updatePerson(id, body) : this.audiencesService.createPerson(body);
    obs.subscribe({
      next: () => {
        this.notice.set(id ? 'Person aktualisiert.' : 'Person angelegt.');
        this.cancelPersonEdit();
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected deletePerson(p: PersonView): void {
    this.audiencesService.deletePerson(p.id).subscribe({
      next: () => {
        this.notice.set('Person gelöscht.');
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected editGroup(g: GroupView): void {
    this.editingGroupId.set(g.id);
    this.groupForm.reset({
      name: g.name,
      personaText: g.personaText,
      summaryRetentionMonths: g.summaryRetentionMonths ?? null,
      memberIds: [...g.memberIds],
    });
  }

  protected cancelGroupEdit(): void {
    this.editingGroupId.set(null);
    this.groupForm.reset({ name: '', personaText: '', summaryRetentionMonths: null, memberIds: [] });
  }

  protected saveGroup(): void {
    if (this.groupForm.invalid) {
      this.groupForm.markAllAsTouched();
      return;
    }
    const value = this.groupForm.getRawValue();
    const body: GroupRequest = {
      name: value.name,
      personaText: value.personaText,
      summaryRetentionMonths: value.summaryRetentionMonths,
      memberIds: value.memberIds,
    };
    const id = this.editingGroupId();
    const obs = id ? this.audiencesService.updateGroup(id, body) : this.audiencesService.createGroup(body);
    obs.subscribe({
      next: () => {
        this.notice.set(id ? 'Personengruppe aktualisiert.' : 'Personengruppe angelegt.');
        this.cancelGroupEdit();
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected deleteGroup(g: GroupView): void {
    this.audiencesService.deleteGroup(g.id).subscribe({
      next: () => {
        this.notice.set('Personengruppe gelöscht.');
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected editTopic(t: TopicView): void {
    this.editingTopicId.set(t.id);
    this.topicForm.reset({
      name: t.name,
      personaText: t.personaText,
      summaryRetentionMonths: t.summaryRetentionMonths ?? null,
      memberIds: [...t.memberIds],
    });
  }

  protected cancelTopicEdit(): void {
    this.editingTopicId.set(null);
    this.topicForm.reset({ name: '', personaText: '', summaryRetentionMonths: null, memberIds: [] });
  }

  protected saveTopic(): void {
    if (this.topicForm.invalid) {
      this.topicForm.markAllAsTouched();
      return;
    }
    const value = this.topicForm.getRawValue();
    const body: TopicRequest = {
      name: value.name,
      personaText: value.personaText,
      summaryRetentionMonths: value.summaryRetentionMonths,
      memberIds: value.memberIds,
    };
    const id = this.editingTopicId();
    const obs = id ? this.audiencesService.updateTopic(id, body) : this.audiencesService.createTopic(body);
    obs.subscribe({
      next: () => {
        this.notice.set(id ? 'Thema aktualisiert.' : 'Thema angelegt.');
        this.cancelTopicEdit();
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected deleteTopic(t: TopicView): void {
    this.audiencesService.deleteTopic(t.id).subscribe({
      next: () => {
        this.notice.set('Thema gelöscht.');
        this.refresh();
      },
      error: (err: HttpErrorResponse) => this.error.set(this.errorMessage(err)),
    });
  }

  protected toggleMember(form: 'group' | 'topic', personId: string, checked: boolean): void {
    const control = form === 'group'
      ? this.groupForm.controls.memberIds
      : this.topicForm.controls.memberIds;
    const set = new Set(control.value);
    if (checked) {
      set.add(personId);
    } else {
      set.delete(personId);
    }
    control.setValue([...set]);
  }

  protected isMember(form: 'group' | 'topic', personId: string): boolean {
    const value = form === 'group'
      ? this.groupForm.controls.memberIds.value
      : this.topicForm.controls.memberIds.value;
    return value.includes(personId);
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
