package app.briefingagent.topic;

import app.briefingagent.common.BaseEntity;
import app.briefingagent.person.Person;
import app.briefingagent.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "topic")
public class Topic extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "persona_text", nullable = false, columnDefinition = "text")
    private String personaText;

    @Column(name = "summary_retention_months")
    private Integer summaryRetentionMonths;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "topic_member",
            joinColumns = @JoinColumn(name = "topic_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id"))
    private Set<Person> members = new LinkedHashSet<>();

    protected Topic() {
    }

    public Topic(UserAccount author, String name, String personaText) {
        this.author = author;
        this.name = name;
        this.personaText = personaText;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonaText() {
        return personaText;
    }

    public void setPersonaText(String personaText) {
        this.personaText = personaText;
    }

    public Integer getSummaryRetentionMonths() {
        return summaryRetentionMonths;
    }

    public void setSummaryRetentionMonths(Integer summaryRetentionMonths) {
        this.summaryRetentionMonths = summaryRetentionMonths;
    }

    public Set<Person> getMembers() {
        return members;
    }
}
