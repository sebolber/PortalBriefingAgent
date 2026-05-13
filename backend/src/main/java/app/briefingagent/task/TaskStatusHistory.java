package app.briefingagent.task;

import app.briefingagent.common.AuditedEntity;
import app.briefingagent.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_status_history")
public class TaskStatusHistory extends AuditedEntity {

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
}
