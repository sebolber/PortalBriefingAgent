package app.briefingagent.audience;

import app.briefingagent.common.ApiException;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.task.Task;
import app.briefingagent.task.TaskRepository;
import app.briefingagent.task.TaskStatus;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audiences")
public class AudienceDetailController {

    private final PersonRepository personRepository;
    private final PersonGroupRepository personGroupRepository;
    private final TopicRepository topicRepository;
    private final SummaryRepository summaryRepository;
    private final TaskRepository taskRepository;
    private final UserAccountRepository userRepository;
    private final CurrentAuthor currentAuthor;

    public AudienceDetailController(PersonRepository personRepository,
                                    PersonGroupRepository personGroupRepository,
                                    TopicRepository topicRepository,
                                    SummaryRepository summaryRepository,
                                    TaskRepository taskRepository,
                                    UserAccountRepository userRepository,
                                    CurrentAuthor currentAuthor) {
        this.personRepository = personRepository;
        this.personGroupRepository = personGroupRepository;
        this.topicRepository = topicRepository;
        this.summaryRepository = summaryRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping("/{type}/{id}")
    @Transactional(readOnly = true)
    public Detail get(@PathVariable String type, @PathVariable UUID id) {
        UserAccount author = userRepository.findById(currentAuthor.requireUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
        return switch (type.toLowerCase()) {
            case "person" -> personDetail(author, id);
            case "persongroup" -> personGroupDetail(author, id);
            case "topic" -> topicDetail(author, id);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown audience type: " + type);
        };
    }

    private Detail personDetail(UserAccount author, UUID id) {
        Person p = personRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Person not found"));
        List<Task> tasks = taskRepository.findByAuthorOrderByCreatedAtDesc(author).stream()
                .filter(t -> t.getAssignedToPerson() != null
                        && t.getAssignedToPerson().getId().equals(id)
                        && t.getStatus() != TaskStatus.DONE
                        && t.getStatus() != TaskStatus.DROPPED)
                .toList();
        return new Detail("person", id.toString(), p.getDisplayName(), null,
                List.of(), summariesFor(s -> s.getAudiencePerson() != null
                        && s.getAudiencePerson().getId().equals(id)),
                tasks.stream().map(TaskBrief::from).toList());
    }

    private Detail personGroupDetail(UserAccount author, UUID id) {
        PersonGroup g = personGroupRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!g.getAuthor().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Group not found");
        }
        return new Detail("persongroup", id.toString(), g.getName(), g.getPersonaText(),
                g.getMembers().stream().map(p -> new Member(p.getId().toString(), p.getDisplayName())).toList(),
                summariesFor(s -> s.getAudiencePersonGroup() != null
                        && s.getAudiencePersonGroup().getId().equals(id)),
                openTasks(author, t -> t.getAssignedToPersonGroup() != null
                        && t.getAssignedToPersonGroup().getId().equals(id)));
    }

    private Detail topicDetail(UserAccount author, UUID id) {
        Topic t = topicRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!t.getAuthor().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Topic not found");
        }
        return new Detail("topic", id.toString(), t.getName(), t.getPersonaText(),
                t.getMembers().stream().map(p -> new Member(p.getId().toString(), p.getDisplayName())).toList(),
                summariesFor(s -> s.getAudienceTopic() != null
                        && s.getAudienceTopic().getId().equals(id)),
                openTasks(author, ta -> ta.getAssignedToTopic() != null
                        && ta.getAssignedToTopic().getId().equals(id)));
    }

    private List<TaskBrief> openTasks(UserAccount author, java.util.function.Predicate<Task> filter) {
        return taskRepository.findByAuthorOrderByCreatedAtDesc(author).stream()
                .filter(filter)
                .filter(t -> t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.DROPPED)
                .map(TaskBrief::from)
                .toList();
    }

    private List<SummaryBrief> summariesFor(java.util.function.Predicate<Summary> filter) {
        return summaryRepository.findAll().stream()
                .filter(filter)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .map(SummaryBrief::from)
                .toList();
    }

    public record Detail(String type, String id, String name, String personaText,
                         List<Member> members, List<SummaryBrief> summaries,
                         List<TaskBrief> openTasks) {
    }

    public record Member(String id, String displayName) {
    }

    public record SummaryBrief(String id, String summaryText, String createdAt) {
        static SummaryBrief from(Summary s) {
            return new SummaryBrief(s.getId().toString(), s.getSummaryText(),
                    s.getCreatedAt().toString());
        }
    }

    public record TaskBrief(String id, String title, String status, String dueDate) {
        static TaskBrief from(Task t) {
            return new TaskBrief(t.getId().toString(), t.getTitle(), t.getStatus().dbValue(),
                    t.getDueDate() == null ? null : t.getDueDate().toString());
        }
    }
}
