package app.briefingagent.admin;

import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.person.PersonTombstoneService;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import app.briefingagent.user.UserDeactivationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserDeactivationService deactivationService;
    private final PersonTombstoneService tombstoneService;
    private final UserAccountRepository userRepository;
    private final PersonRepository personRepository;
    private final CurrentAuthor currentAuthor;

    public AdminController(UserDeactivationService deactivationService,
                           PersonTombstoneService tombstoneService,
                           UserAccountRepository userRepository,
                           PersonRepository personRepository,
                           CurrentAuthor currentAuthor) {
        this.deactivationService = deactivationService;
        this.tombstoneService = tombstoneService;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        return userRepository.findAll().stream().map(UserView::from).toList();
    }

    @PostMapping("/users/{id}/deactivate")
    public UserView deactivate(@PathVariable UUID id) {
        return UserView.from(deactivationService.deactivate(id, currentAuthor.requireUserId()));
    }

    @PostMapping("/users/{id}/reactivate")
    public UserView reactivate(@PathVariable UUID id) {
        return UserView.from(deactivationService.reactivate(id));
    }

    @GetMapping("/persons")
    public List<PersonAdminView> listPersons() {
        return personRepository.findAll().stream().map(PersonAdminView::from).toList();
    }

    @PostMapping("/persons/{id}/tombstone")
    public PersonAdminView tombstone(@PathVariable UUID id) {
        return PersonAdminView.from(tombstoneService.tombstone(id));
    }

    public record UserView(String id, String username, String fullName, String status,
                           Instant deactivatedAt, Instant deletionScheduledAt, boolean admin) {

        public static UserView from(UserAccount u) {
            return new UserView(u.getId().toString(), u.getUsername(), u.getFullName(),
                    u.getStatus().dbValue(), u.getDeactivatedAt(),
                    u.getDeletionScheduledAt(), u.isAdmin());
        }
    }

    public record PersonAdminView(String id, String fullName, String displayName,
                                  boolean tombstoned, Instant deletedAt, String pseudonym) {

        public static PersonAdminView from(Person p) {
            return new PersonAdminView(p.getId().toString(), p.getFullName(),
                    p.getDisplayName(), p.isTombstoned(),
                    p.getDeletedAt(), p.getPseudonym());
        }
    }
}
