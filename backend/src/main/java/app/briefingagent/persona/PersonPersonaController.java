package app.briefingagent.persona;

import app.briefingagent.common.ApiException;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persons/{personId}/persona")
@Transactional
public class PersonPersonaController {

    private final PersonPersonaRepository repository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userRepository;
    private final CurrentAuthor currentAuthor;

    public PersonPersonaController(PersonPersonaRepository repository,
                                   PersonRepository personRepository,
                                   UserAccountRepository userRepository,
                                   CurrentAuthor currentAuthor) {
        this.repository = repository;
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.currentAuthor = currentAuthor;
    }

    @PutMapping
    public ResponseEntity<View> upsert(@PathVariable UUID personId,
                                       @Valid @RequestBody Request body) {
        UserAccount author = author();
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Person not found"));

        PersonPersona persona = repository.findByAuthorAndPerson(author, person)
                .orElseGet(() -> new PersonPersona(author, person, body.personaText()));
        persona.setPersonaText(body.personaText());
        PersonPersona saved = repository.save(persona);
        return ResponseEntity.ok(View.from(saved));
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@PathVariable UUID personId) {
        UserAccount author = author();
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Person not found"));
        repository.findByAuthorAndPerson(author, person).ifPresent(repository::delete);
        return ResponseEntity.noContent().build();
    }

    private UserAccount author() {
        return userRepository.findById(currentAuthor.requireUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
    }

    public record Request(@NotBlank @Size(max = 4000) String personaText) {
    }

    public record View(String id, String personId, String personaText) {
        public static View from(PersonPersona pp) {
            return new View(pp.getId().toString(),
                    pp.getPerson().getId().toString(),
                    pp.getPersonaText());
        }
    }
}
