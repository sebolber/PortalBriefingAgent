package app.briefingagent.persona;

import app.briefingagent.common.BaseEntity;
import app.briefingagent.person.Person;
import app.briefingagent.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "person_persona",
        uniqueConstraints = @UniqueConstraint(
                name = "person_persona_unique",
                columnNames = {"author_id", "person_id"}))
public class PersonPersona extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "persona_text", nullable = false, columnDefinition = "text")
    private String personaText;

    protected PersonPersona() {
    }

    public PersonPersona(UserAccount author, Person person, String personaText) {
        this.author = author;
        this.person = person;
        this.personaText = personaText;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public Person getPerson() {
        return person;
    }

    public String getPersonaText() {
        return personaText;
    }

    public void setPersonaText(String personaText) {
        this.personaText = personaText;
    }
}
