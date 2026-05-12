package app.briefingagent.summary;

import app.briefingagent.common.AuditedEntity;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.person.Person;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.topic.Topic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "summary")
public class Summary extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ereignis_id", nullable = false)
    private Ereignis ereignis;

    @Column(name = "audience_type", nullable = false, length = 20)
    private AudienceType audienceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_person_id")
    private Person audiencePerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_persongroup_id")
    private PersonGroup audiencePersonGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audience_topic_id")
    private Topic audienceTopic;

    @Column(name = "summary_text", nullable = false, columnDefinition = "text")
    private String summaryText;

    @Column(name = "classification_confidence", length = 10)
    private ClassificationConfidence classificationConfidence;

    @Column(name = "classification_reasoning", columnDefinition = "text")
    private String classificationReasoning;

    @Column(name = "edit_state", nullable = false, length = 20)
    private EditState editState = EditState.AI_GENERATED;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "regeneration_feedback", columnDefinition = "text")
    private String regenerationFeedback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "edit_history", nullable = false, columnDefinition = "jsonb")
    private List<EditHistoryEntry> editHistory = new ArrayList<>();

    protected Summary() {
    }

    public static Summary forTopic(Ereignis ereignis, Topic topic, String summaryText) {
        Summary s = new Summary();
        s.ereignis = ereignis;
        s.audienceType = AudienceType.TOPIC;
        s.audienceTopic = topic;
        s.summaryText = summaryText;
        return s;
    }

    public static Summary forPerson(Ereignis ereignis, Person person, String summaryText) {
        Summary s = new Summary();
        s.ereignis = ereignis;
        s.audienceType = AudienceType.PERSON;
        s.audiencePerson = person;
        s.summaryText = summaryText;
        return s;
    }

    public static Summary forPersonGroup(Ereignis ereignis, PersonGroup group, String summaryText) {
        Summary s = new Summary();
        s.ereignis = ereignis;
        s.audienceType = AudienceType.PERSONGROUP;
        s.audiencePersonGroup = group;
        s.summaryText = summaryText;
        return s;
    }

    public Ereignis getEreignis() {
        return ereignis;
    }

    public AudienceType getAudienceType() {
        return audienceType;
    }

    public Person getAudiencePerson() {
        return audiencePerson;
    }

    public PersonGroup getAudiencePersonGroup() {
        return audiencePersonGroup;
    }

    public Topic getAudienceTopic() {
        return audienceTopic;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public ClassificationConfidence getClassificationConfidence() {
        return classificationConfidence;
    }

    public void setClassificationConfidence(ClassificationConfidence c) {
        this.classificationConfidence = c;
    }

    public String getClassificationReasoning() {
        return classificationReasoning;
    }

    public void setClassificationReasoning(String classificationReasoning) {
        this.classificationReasoning = classificationReasoning;
    }

    public EditState getEditState() {
        return editState;
    }

    public void setEditState(EditState editState) {
        this.editState = editState;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getRegenerationFeedback() {
        return regenerationFeedback;
    }

    public void setRegenerationFeedback(String regenerationFeedback) {
        this.regenerationFeedback = regenerationFeedback;
    }

    public List<EditHistoryEntry> getEditHistory() {
        return editHistory == null ? List.of() : List.copyOf(editHistory);
    }

    public void appendHistory(EditHistoryEntry entry) {
        if (editHistory == null) {
            editHistory = new ArrayList<>();
        }
        editHistory.add(entry);
    }
}
