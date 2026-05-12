package app.briefingagent.user;

import app.briefingagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "entra_object_id", length = 36)
    private String entraObjectId;

    @Column(name = "entra_upn", length = 255)
    private String entraUpn;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, String fullName, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEntraObjectId() {
        return entraObjectId;
    }

    public String getEntraUpn() {
        return entraUpn;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void setDeactivatedAt(Instant deactivatedAt) {
        this.deactivatedAt = deactivatedAt;
    }

    public Instant getDeletionScheduledAt() {
        return deletionScheduledAt;
    }

    public void setDeletionScheduledAt(Instant deletionScheduledAt) {
        this.deletionScheduledAt = deletionScheduledAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
