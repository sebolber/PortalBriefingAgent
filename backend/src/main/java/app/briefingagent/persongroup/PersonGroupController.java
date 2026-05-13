package app.briefingagent.persongroup;

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
@RequestMapping("/api/persongroups")
@Transactional
public class PersonGroupController {

    private final PersonGroupRepository repository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userRepository;
    private final CurrentAuthor currentAuthor;

    public PersonGroupController(PersonGroupRepository repository,
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
                .filter(g -> g.getAuthor().getId().equals(author.getId()))
                .map(View::from)
                .toList();
    }

    @GetMapping("/{id}")
    public View get(@PathVariable UUID id) {
        return View.from(loadOwn(id));
    }

    @PostMapping
    public ResponseEntity<View> create(@Valid @RequestBody Request body) {
        PersonGroup g = new PersonGroup(author(), body.name(), body.personaText());
        g.setSummaryRetentionMonths(body.summaryRetentionMonths());
        applyMembers(g, body.memberIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(View.from(repository.save(g)));
    }

    @PatchMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request body) {
        PersonGroup g = loadOwn(id);
        g.setName(body.name());
        g.setPersonaText(body.personaText());
        g.setSummaryRetentionMonths(body.summaryRetentionMonths());
        g.getMembers().clear();
        applyMembers(g, body.memberIds());
        return View.from(repository.save(g));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        PersonGroup g = loadOwn(id);
        repository.delete(g);
        return ResponseEntity.noContent().build();
    }

    private void applyMembers(PersonGroup group, List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        for (String idStr : memberIds) {
            UUID id = parseUuid(idStr);
            Person p = personRepository.findById(id)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "Unknown member id: " + idStr));
            group.getMembers().add(p);
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid member id: " + value);
        }
    }

    private PersonGroup loadOwn(UUID id) {
        UserAccount author = author();
        PersonGroup g = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!g.getAuthor().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Group not found");
        }
        return g;
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

        public static View from(PersonGroup g) {
            return new View(
                    g.getId().toString(),
                    g.getName(),
                    g.getPersonaText(),
                    g.getSummaryRetentionMonths(),
                    g.getMembers().stream().map(p -> p.getId().toString()).toList());
        }
    }
}
