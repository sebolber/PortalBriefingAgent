package app.briefingagent.person;

import app.briefingagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "person")
public class Person extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "role", length = 200)
    private String role;

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "source", nullable = false, length = 20)
    private PersonSource source = PersonSource.MANUAL;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "pseudonym", length = 100)
    private String pseudonym;

    protected Person() {
    }

    public Person(String fullName, PersonSource source) {
        this.fullName = fullName;
        this.source = source;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public PersonSource getSource() {
        return source;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getPseudonym() {
        return pseudonym;
    }

    public void setPseudonym(String pseudonym) {
        this.pseudonym = pseudonym;
    }

    public boolean isTombstoned() {
        return deletedAt != null;
    }

    public String getDisplayName() {
        return isTombstoned() && pseudonym != null ? pseudonym : fullName;
    }
}
