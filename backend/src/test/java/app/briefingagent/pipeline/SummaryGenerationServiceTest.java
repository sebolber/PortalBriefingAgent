package app.briefingagent.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.audience.AudienceRef;
import app.briefingagent.common.TestEntities;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisSourceType;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.person.PersonSource;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.summary.AudienceType;
import app.briefingagent.summary.ClassificationConfidence;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SummaryGenerationServiceTest {

    @Mock
    private LlmClient llmClient;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private PersonGroupRepository personGroupRepository;
    @Mock
    private TopicRepository topicRepository;

    private SummaryGenerationService service;

    private UserAccount author;
    private Ereignis ereignis;
    private Person anna;
    private PersonGroup board;
    private Topic alpha;

    @BeforeEach
    void setUp() {
        service = new SummaryGenerationService(llmClient, summaryRepository,
                personRepository, personGroupRepository, topicRepository);
        author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
        ereignis = TestEntities.withRandomId(new Ereignis(author, EreignisSourceType.TEXT));
        ereignis.setTranscriptText("Heute Workshop besprochen");

        anna = TestEntities.withRandomId(new Person("Anna Müller", PersonSource.MANUAL));
        board = TestEntities.withRandomId(new PersonGroup(author, "Vorstand", "strategisch"));
        alpha = TestEntities.withRandomId(new Topic(author, "Produkt Alpha", "technisch"));
    }

    private void stubSaveAssignsId() {
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary s = invocation.getArgument(0);
            return TestEntities.withRandomId(s);
        });
    }

    @Test
    void generates_one_summary_per_match_in_iteration_order() {
        stubSaveAssignsId();
        when(personRepository.findById(anna.getId())).thenReturn(Optional.of(anna));
        when(personGroupRepository.findById(board.getId())).thenReturn(Optional.of(board));
        when(topicRepository.findById(alpha.getId())).thenReturn(Optional.of(alpha));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("body");

        List<AudienceMatch> matches = List.of(
                new AudienceMatch(
                        new AudienceRef(AudienceType.PERSON, anna.getId(), "Anna", "kurz"),
                        ClassificationConfidence.HIGH, "Anna war Teil des Termins"),
                new AudienceMatch(
                        new AudienceRef(AudienceType.PERSONGROUP, board.getId(), "Vorstand", "strategic"),
                        ClassificationConfidence.MEDIUM, "Top-Level"),
                new AudienceMatch(
                        new AudienceRef(AudienceType.TOPIC, alpha.getId(), "Alpha", "tech"),
                        ClassificationConfidence.LOW, "Tangential"));

        List<Summary> result = service.generate(ereignis, matches);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAudienceType()).isEqualTo(AudienceType.PERSON);
        assertThat(result.get(1).getAudienceType()).isEqualTo(AudienceType.PERSONGROUP);
        assertThat(result.get(2).getAudienceType()).isEqualTo(AudienceType.TOPIC);
        assertThat(result).extracting(Summary::getClassificationConfidence)
                .containsExactly(ClassificationConfidence.HIGH,
                        ClassificationConfidence.MEDIUM,
                        ClassificationConfidence.LOW);
        verify(llmClient, times(3)).complete(any(LlmRequest.class));
    }

    @Test
    void empty_match_list_means_zero_calls_and_zero_saves() {
        List<Summary> result = service.generate(ereignis, List.of());

        assertThat(result).isEmpty();
        verify(llmClient, times(0)).complete(any(LlmRequest.class));
        verify(summaryRepository, times(0)).save(any());
    }

    @Test
    void persona_text_flows_into_each_system_prompt() {
        stubSaveAssignsId();
        when(personRepository.findById(anna.getId())).thenReturn(Optional.of(anna));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("body");

        AudienceRef ref = new AudienceRef(AudienceType.PERSON, anna.getId(), "Anna",
                "Wenig Smalltalk, klare Asks.");
        service.generate(ereignis, List.of(
                new AudienceMatch(ref, ClassificationConfidence.HIGH, "ok")));

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).complete(captor.capture());
        assertThat(captor.getValue().systemPrompt()).contains("Anna");
        assertThat(captor.getValue().systemPrompt()).contains("Wenig Smalltalk");
    }

    @Test
    void missing_audience_target_propagates_404() {
        UUID missing = UUID.randomUUID();
        when(personRepository.findById(missing)).thenReturn(Optional.empty());
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("body");

        AudienceRef ref = new AudienceRef(AudienceType.PERSON, missing, "Phantom", "x");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.generate(ereignis, List.of(
                                new AudienceMatch(ref, ClassificationConfidence.HIGH, "ok"))))
                .isInstanceOf(app.briefingagent.common.ApiException.class);
    }
}
