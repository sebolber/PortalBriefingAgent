package app.briefingagent.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisRepository;
import app.briefingagent.ereignis.EreignisSourceType;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.person.PersonSource;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.DefaultTopicProvider;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class SchemaMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserAccountRepository userRepository;
    @Autowired PersonRepository personRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired DefaultTopicProvider defaultTopicProvider;
    @Autowired EreignisRepository ereignisRepository;
    @Autowired SummaryRepository summaryRepository;

    @Test
    void migration_applies_and_baseline_inserts_work() {
        UserAccount author = userRepository.save(
                new UserAccount("demo-it", "x", "Demo IT", "demo-it@example.invalid"));
        Topic topic = defaultTopicProvider.ensureDefaultTopic(author);
        Ereignis ereignis = new Ereignis(author, EreignisSourceType.TEXT);
        ereignis.setTranscriptText("hello");
        ereignis.setCharacterCount(5);
        ereignis = ereignisRepository.save(ereignis);

        Summary summary = Summary.forTopic(ereignis, topic, "## Summary");
        summary = summaryRepository.save(summary);

        assertThat(summary.getId()).isNotNull();
        assertThat(topic.getId()).isNotNull();
    }

    @Test
    void summary_check_constraint_rejects_zero_audience_targets() {
        UserAccount author = userRepository.save(
                new UserAccount("two-audience", "x", "Demo", "two@example.invalid"));
        Topic topic = defaultTopicProvider.ensureDefaultTopic(author);
        Ereignis ereignis = ereignisRepository.save(new Ereignis(author, EreignisSourceType.TEXT));

        Summary illegal = Summary.forTopic(ereignis, topic, "x");
        // Strip the topic to violate "exactly one" rule.
        nullAudienceTopic(illegal);

        assertThatThrownBy(() -> summaryRepository.saveAndFlush(illegal))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void person_can_be_tombstoned_without_losing_referential_integrity() {
        Person p = personRepository.save(new Person("Anna Müller", PersonSource.MANUAL));

        p.setDeletedAt(java.time.Instant.now());
        p.setPseudonym("Gelöschte Person #1");
        Person updated = personRepository.save(p);

        assertThat(updated.isTombstoned()).isTrue();
        assertThat(updated.getDisplayName()).isEqualTo("Gelöschte Person #1");
    }

    private static void nullAudienceTopic(Summary s) {
        try {
            Field f = Summary.class.getDeclaredField("audienceTopic");
            f.setAccessible(true);
            f.set(s, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
