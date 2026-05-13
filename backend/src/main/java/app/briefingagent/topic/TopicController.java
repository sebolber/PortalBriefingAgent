package app.briefingagent.topic;

import app.briefingagent.common.ApiException;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
@Transactional
public class TopicController {

    private final TopicRepository repository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userRepository;
    private final CurrentAuthor currentAuthor;

    public TopicController(TopicRepository repository,
                           PersonRepository personRepository,
                           UserAccountRepository userRepository,
                           CurrentAuthor currentAuthor) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping
    public List<View> list() {
        UserAccount author = author();
        return repository.findAll(Sort.by(Sort.Order.asc("name"))).stream()
                .filter(t -> t.getAuthor().getId().equals(author.getId()))
                .map(View::from)
                .toList();
    }

    @GetMapping("/{id}")
    public View get(@PathVariable UUID id) {
        return View.from(loadOwn(id));
    }

    @PostMapping
    public ResponseEntity<View> create(@Valid @RequestBody Request body) {
        Topic t = new Topic(author(), body.name(), body.personaText());
        t.setSummaryRetentionMonths(body.summaryRetentionMonths());
        applyMembers(t, body.memberIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(View.from(repository.save(t)));
    }

    @PatchMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request body) {
        Topic t = loadOwn(id);
        t.setName(body.name());
        t.setPersonaText(body.personaText());
        t.setSummaryRetentionMonths(body.summaryRetentionMonths());
        t.getMembers().clear();
        applyMembers(t, body.memberIds());
        return View.from(repository.save(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Topic t = loadOwn(id);
        repository.delete(t);
        return ResponseEntity.noContent().build();
    }

    private void applyMembers(Topic topic, List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        for (String idStr : memberIds) {
            UUID id;
            try {
                id = UUID.fromString(idStr);
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid member id: " + idStr);
            }
            Person p = personRepository.findById(id)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown member id: " + idStr));
            topic.getMembers().add(p);
        }
    }

    private Topic loadOwn(UUID id) {
        UserAccount author = author();
        Topic t = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!t.getAuthor().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Topic not found");
        }
        return t;
    }

    private UserAccount author() {
        return userRepository.findById(currentAuthor.requireUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
    }

    public record Request(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 4000) String personaText,
            @Min(1) Integer summaryRetentionMonths,
            List<String> memberIds) {
    }

    public record View(String id, String name, String personaText,
                       Integer summaryRetentionMonths, List<String> memberIds) {

        public static View from(Topic t) {
            return new View(
                    t.getId().toString(),
                    t.getName(),
                    t.getPersonaText(),
                    t.getSummaryRetentionMonths(),
                    t.getMembers().stream().map(p -> p.getId().toString()).toList());
        }
    }
}
