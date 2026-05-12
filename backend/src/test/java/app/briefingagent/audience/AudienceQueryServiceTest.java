package app.briefingagent.audience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.persona.PersonPersona;
import app.briefingagent.persona.PersonPersonaRepository;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonSource;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.summary.AudienceType;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AudienceQueryServiceTest {

    @Mock
    PersonPersonaRepository personPersonaRepository;
    @Mock
    PersonGroupRepository personGroupRepository;
    @Mock
    TopicRepository topicRepository;

    @InjectMocks
    AudienceQueryService service;

    private UserAccount author;

    @BeforeEach
    void setUp() {
        author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
    }

    @Test
    void aggregates_all_three_audience_types() {
        Person anna = TestEntities.withRandomId(new Person("Anna Müller", PersonSource.MANUAL));
        PersonPersona personaA = TestEntities.withRandomId(new PersonPersona(author, anna, "direkt"));
        PersonGroup board = TestEntities.withRandomId(new PersonGroup(author, "Vorstand", "strategisch"));
        Topic alpha = TestEntities.withRandomId(new Topic(author, "Produkt Alpha", "technisch"));

        when(personPersonaRepository.findByAuthor(author)).thenReturn(List.of(personaA));
        when(personGroupRepository.findByAuthor(author)).thenReturn(List.of(board));
        when(topicRepository.findByAuthor(author)).thenReturn(List.of(alpha));

        List<AudienceRef> result = service.allFor(author);

        assertThat(result).extracting(AudienceRef::type)
                .containsExactly(AudienceType.PERSON, AudienceType.PERSONGROUP, AudienceType.TOPIC);
        assertThat(result.get(0).name()).isEqualTo("Anna Müller");
        assertThat(result.get(0).personaText()).isEqualTo("direkt");
        assertThat(result.get(1).name()).isEqualTo("Vorstand");
        assertThat(result.get(2).name()).isEqualTo("Produkt Alpha");
    }

    @Test
    void omits_tombstoned_persons_from_audience_aggregation() {
        Person ghost = TestEntities.withRandomId(new Person("Phantom", PersonSource.MANUAL));
        ghost.setDeletedAt(Instant.now());
        ghost.setPseudonym("Gelöschte Person #1");
        PersonPersona persona = TestEntities.withRandomId(new PersonPersona(author, ghost, "x"));

        when(personPersonaRepository.findByAuthor(author)).thenReturn(List.of(persona));
        when(personGroupRepository.findByAuthor(author)).thenReturn(List.of());
        when(topicRepository.findByAuthor(author)).thenReturn(List.of());

        assertThat(service.allFor(author)).isEmpty();
    }

    @Test
    void empty_inputs_produce_empty_list() {
        when(personPersonaRepository.findByAuthor(author)).thenReturn(List.of());
        when(personGroupRepository.findByAuthor(author)).thenReturn(List.of());
        when(topicRepository.findByAuthor(author)).thenReturn(List.of());

        assertThat(service.allFor(author)).isEmpty();
    }
}
