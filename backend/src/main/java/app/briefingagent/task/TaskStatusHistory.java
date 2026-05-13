package app.briefingagent.task;

import app.briefingagent.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only audit row for a {@link Task}'s status transitions. The
 * primary timestamp is {@code changed_at}, which is why this entity does
 * not extend {@code AuditedEntity}: there is no separate {@code
 * created_at} in the underlying table.
 */
@Entity
@Table(name = "task_status_history")
public class TaskStatusHistory {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "from_status", length = 20)
    private TaskStatus fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private TaskStatus toStatus;

    @Column(name = "note", columnDefinition = "text")
    private String note;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_author_id", nullable = false)
    private UserAccount changedByAuthor;

    protected TaskStatusHistory() {
    }

    public TaskStatusHistory(Task task, TaskStatus fromStatus, TaskStatus toStatus,
                             String note, UserAccount changedByAuthor) {
        this.task = task;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.changedByAuthor = changedByAuthor;
        this.changedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public TaskStatus getFromStatus() {
        return fromStatus;
    }

    public TaskStatus getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public UUID getChangedByAuthorId() {
        return changedByAuthor.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskStatusHistory other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
