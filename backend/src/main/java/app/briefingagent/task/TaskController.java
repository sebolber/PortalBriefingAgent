package app.briefingagent.task;

import app.briefingagent.common.ApiException;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserAccountRepository userRepository;
    private final PersonRepository personRepository;
    private final PersonGroupRepository personGroupRepository;
    private final TopicRepository topicRepository;
    private final CurrentAuthor currentAuthor;

    public TaskController(TaskService taskService,
                          UserAccountRepository userRepository,
                          PersonRepository personRepository,
                          PersonGroupRepository personGroupRepository,
                          TopicRepository topicRepository,
                          CurrentAuthor currentAuthor) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.personGroupRepository = personGroupRepository;
        this.topicRepository = topicRepository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping
    public List<View> list() {
        return taskService.listFor(currentAuthor.requireUserId()).stream()
                .map(View::from).toList();
    }

    @PostMapping
    public ResponseEntity<View> create(@Valid @RequestBody CreateRequest body) {
        UserAccount author = userRepository.findById(currentAuthor.requireUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
        Task task = buildDraft(author, body);
        Task saved = taskService.create(currentAuthor.requireUserId(), task);
        return ResponseEntity.status(HttpStatus.CREATED).body(View.from(saved));
    }

    @PatchMapping("/{id}")
    public View edit(@PathVariable UUID id, @Valid @RequestBody EditRequest body) {
        Task saved = taskService.editFields(
                currentAuthor.requireUserId(), id,
                body.title(), body.description(), body.dueDate());
        return View.from(saved);
    }

    @PostMapping("/{id}/status")
    public View changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest body) {
        TaskStatus to;
        try {
            to = TaskStatus.valueOf(body.to().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status: " + body.to());
        }
        return View.from(taskService.changeStatus(currentAuthor.requireUserId(), id, to, body.note()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        taskService.delete(currentAuthor.requireUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public List<HistoryView> history(@PathVariable UUID id) {
        return taskService.historyOf(currentAuthor.requireUserId(), id).stream()
                .map(HistoryView::from).toList();
    }

    private Task buildDraft(UserAccount author, CreateRequest body) {
        int targets = (body.assignedToPersonId() != null ? 1 : 0)
                + (body.assignedToPersonGroupId() != null ? 1 : 0)
                + (body.assignedToTopicId() != null ? 1 : 0)
                + (Boolean.TRUE.equals(body.assignedToSelf()) ? 1 : 0);
        if (targets != 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Exactly one assignment target must be set");
        }
        Task task;
        if (body.assignedToPersonId() != null) {
            Person p = personRepository.findById(body.assignedToPersonId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown person id"));
            task = Task.forPerson(author, body.title(), p);
        } else if (body.assignedToPersonGroupId() != null) {
            PersonGroup g = personGroupRepository.findById(body.assignedToPersonGroupId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown person group id"));
            task = Task.forPersonGroup(author, body.title(), g);
        } else if (body.assignedToTopicId() != null) {
            Topic t = topicRepository.findById(body.assignedToTopicId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown topic id"));
            task = Task.forTopic(author, body.title(), t);
        } else {
            task = Task.forSelf(author, body.title());
        }
        task.setDescription(body.description());
        task.setDueDate(body.dueDate());
        return task;
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4_000) String description,
            UUID assignedToPersonId,
            UUID assignedToPersonGroupId,
            UUID assignedToTopicId,
            Boolean assignedToSelf,
            LocalDate dueDate) {
    }

    public record EditRequest(
            @Size(max = 200) String title,
            @Size(max = 4_000) String description,
            LocalDate dueDate) {
    }

    public record StatusRequest(@NotBlank String to, @Size(max = 1_000) String note) {
    }

    public record View(
            String id,
            String title,
            String description,
            String status,
            String dueDate,
            String assignment,
            boolean assignedToSelf) {

        public static View from(Task t) {
            String assignment;
            if (t.isAssignedToSelf()) {
                assignment = "self";
            } else if (t.getAssignedToPerson() != null) {
                assignment = "person:" + t.getAssignedToPerson().getId();
            } else if (t.getAssignedToPersonGroup() != null) {
                assignment = "persongroup:" + t.getAssignedToPersonGroup().getId();
            } else if (t.getAssignedToTopic() != null) {
                assignment = "topic:" + t.getAssignedToTopic().getId();
            } else {
                assignment = "unknown";
            }
            return new View(
                    t.getId().toString(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getStatus().dbValue(),
                    t.getDueDate() == null ? null : t.getDueDate().toString(),
                    assignment,
                    t.isAssignedToSelf());
        }
    }

    public record HistoryView(
            String fromStatus,
            String toStatus,
            String note,
            String changedAt,
            String changedByAuthorId) {

        public static HistoryView from(TaskStatusHistory h) {
            return new HistoryView(
                    h.getFromStatus() == null ? null : h.getFromStatus().dbValue(),
                    h.getToStatus().dbValue(),
                    h.getNote(),
                    h.getChangedAt().toString(),
                    h.getChangedByAuthorId().toString());
        }
    }
}
