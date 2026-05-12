package app.briefingagent.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for entities that record only a creation timestamp. Used for
 * append-only history rows where mutating an existing record makes no sense.
 */
@MappedSuperclass
public abstract class AuditedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditedEntity() {
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditedEntity other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(id);
    }
}
