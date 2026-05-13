package app.briefingagent.person;

import app.briefingagent.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the spec's tombstone pattern: a person is marked as deleted
 * with a deterministic pseudonym, but referencing rows (personas, group
 * memberships, summary audiences) remain technically intact so the
 * historical record is preserved.
 */
@Service
public class PersonTombstoneService {

    private final PersonRepository personRepository;
    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong(0);

    public PersonTombstoneService(PersonRepository personRepository, Clock clock) {
        this.personRepository = personRepository;
        this.clock = clock;
    }

    @Transactional
    public Person tombstone(UUID personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Person not found"));
        if (person.isTombstoned()) {
            return person;
        }
        Instant now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC")).toInstant();
        person.setDeletedAt(now);
        person.setPseudonym(generatePseudonym());
        return personRepository.save(person);
    }

    private String generatePseudonym() {
        long current = personRepository.findAll().stream()
                .filter(p -> p.getPseudonym() != null && p.getPseudonym().startsWith("Gelöschte Person #"))
                .count();
        long candidate = sequence.updateAndGet(prev -> Math.max(prev + 1, current + 1));
        return "Gelöschte Person #" + candidate;
    }
}
