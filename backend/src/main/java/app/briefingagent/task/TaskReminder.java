package app.briefingagent.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Idempotency record for the daily reminder scheduler. The primary
 * timestamp is {@code reminded_at}; this entity intentionally does not
 * extend {@code AuditedEntity} because the underlying table has no
 * {@code created_at} column.
 */
@Entity
@Table(
        name = "task_reminder",
        uniqueConstraints = @UniqueConstraint(
                name = "task_reminder_unique",
                columnNames = {"task_id", "reminder_type", "reminded_on"}))
public class TaskReminder {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

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

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (remindedAt == null) {
            remindedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskReminder other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
