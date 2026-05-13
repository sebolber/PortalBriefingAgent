package app.briefingagent.task;

import app.briefingagent.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "task_reminder",
        uniqueConstraints = @UniqueConstraint(
                name = "task_reminder_unique",
                columnNames = {"task_id", "reminder_type", "reminded_on"}))
public class TaskReminder extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "reminded_at", nullable = false)
    private Instant remindedAt = Instant.now();

    @Column(name = "reminded_on", nullable = false)
    private LocalDate remindedOn;

    @Column(name = "reminder_type", nullable = false, length = 30)
    private TaskReminderType reminderType;

    protected TaskReminder() {
    }

    public TaskReminder(Task task, TaskReminderType reminderType, LocalDate remindedOn) {
        this.task = task;
        this.reminderType = reminderType;
        this.remindedOn = remindedOn;
    }

    public Task getTask() {
        return task;
    }

    public Instant getRemindedAt() {
        return remindedAt;
    }

    public LocalDate getRemindedOn() {
        return remindedOn;
    }

    public TaskReminderType getReminderType() {
        return reminderType;
    }
}
