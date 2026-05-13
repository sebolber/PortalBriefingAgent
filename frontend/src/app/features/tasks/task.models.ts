export type TaskStatus = 'open' | 'in_progress' | 'done' | 'dropped';

export interface TaskView {
  readonly id: string;
  readonly title: string;
  readonly description: string | null;
  readonly status: TaskStatus;
  readonly dueDate: string | null;
  readonly assignment: string;
  readonly assignedToSelf: boolean;
}

export interface TaskCreateRequest {
  title: string;
  description: string | null;
  assignedToSelf: boolean;
  assignedToPersonId: string | null;
  assignedToPersonGroupId: string | null;
  assignedToTopicId: string | null;
  dueDate: string | null;
}

export interface TaskStatusChange {
  to: TaskStatus;
  note: string | null;
}

export interface NotificationView {
  readonly taskId: string;
  readonly taskTitle: string;
  readonly reminderType: 'one_day_before' | 'on_due_date';
  readonly remindedAt: string;
}
