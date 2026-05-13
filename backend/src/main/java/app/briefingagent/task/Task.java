package app.briefingagent.task;

import app.briefingagent.common.BaseEntity;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.person.Person;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "task")
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ereignis_id")
    private Ereignis ereignis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_person_id")
    private Person assignedToPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_persongroup_id")
    private PersonGroup assignedToPersonGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_topic_id")
    private Topic assignedToTopic;

    @Column(name = "assigned_to_self", nullable = false)
    private boolean assignedToSelf;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.OPEN;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "dropped_at")
    private Instant droppedAt;

    protected Task() {
    }

    public static Task forSelf(UserAccount author, String title) {
        Task t = new Task();
        t.author = author;
        t.title = title;
        t.assignedToSelf = true;
        return t;
    }

    public static Task forPerson(UserAccount author, String title, Person p) {
        Task t = new Task();
        t.author = author;
        t.title = title;
        t.assignedToPerson = p;
        return t;
    }

    public static Task forPersonGroup(UserAccount author, String title, PersonGroup g) {
        Task t = new Task();
        t.author = author;
        t.title = title;
        t.assignedToPersonGroup = g;
        return t;
    }

    public static Task forTopic(UserAccount author, String title, Topic to) {
        Task t = new Task();
        t.author = author;
        t.title = title;
        t.assignedToTopic = to;
        return t;
    }

    public Ereignis getEreignis() {
        return ereignis;
    }

    public void setEreignis(Ereignis ereignis) {
        this.ereignis = ereignis;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Person getAssignedToPerson() {
        return assignedToPerson;
    }

    public PersonGroup getAssignedToPersonGroup() {
        return assignedToPersonGroup;
    }

    public Topic getAssignedToTopic() {
        return assignedToTopic;
    }

    public boolean isAssignedToSelf() {
        return assignedToSelf;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getDroppedAt() {
        return droppedAt;
    }

    public void setDroppedAt(Instant droppedAt) {
        this.droppedAt = droppedAt;
    }
}
