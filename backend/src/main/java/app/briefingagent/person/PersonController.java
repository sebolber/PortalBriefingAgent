package app.briefingagent.person;

import app.briefingagent.common.ApiException;
import app.briefingagent.security.CurrentAuthor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonRepository repository;
    @SuppressWarnings("unused")
    private final CurrentAuthor currentAuthor;

    public PersonController(PersonRepository repository, CurrentAuthor currentAuthor) {
        this.repository = repository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping
    public List<PersonView> list() {
        return repository.findAll(Sort.by(Sort.Order.asc("fullName")))
                .stream().map(PersonView::from).toList();
    }

    @GetMapping("/{id}")
    public PersonView get(@PathVariable UUID id) {
        return PersonView.from(load(id));
    }

    @PostMapping
    public ResponseEntity<PersonView> create(@Valid @RequestBody PersonRequest body) {
        Person person = new Person(body.fullName(), PersonSource.MANUAL);
        person.setEmail(body.email());
        person.setRole(body.role());
        person.setCompany(body.company());
        Person saved = repository.save(person);
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonView.from(saved));
    }

    @PatchMapping("/{id}")
    public PersonView update(@PathVariable UUID id, @Valid @RequestBody PersonRequest body) {
        Person p = load(id);
        if (p.isTombstoned()) {
            throw new ApiException(HttpStatus.CONFLICT, "Person is tombstoned");
        }
        p.setFullName(body.fullName());
        p.setEmail(body.email());
        p.setRole(body.role());
        p.setCompany(body.company());
        return PersonView.from(repository.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Person p = load(id);
        repository.delete(p);
        return ResponseEntity.noContent().build();
    }

    private Person load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Person not found"));
    }

    public record PersonRequest(
            @NotBlank @Size(max = 200) String fullName,
            @Size(max = 255) String email,
            @Size(max = 200) String role,
            @Size(max = 200) String company) {
    }

    public record PersonView(
            String id,
            String fullName,
            String email,
            String role,
            String company,
            boolean tombstoned,
            String pseudonym) {

        public static PersonView from(Person p) {
            return new PersonView(
                    p.getId().toString(),
                    p.getFullName(),
                    p.getEmail(),
                    p.getRole(),
                    p.getCompany(),
                    p.isTombstoned(),
                    p.getPseudonym());
        }
    }
}
