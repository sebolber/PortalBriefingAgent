package app.briefingagent.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisSourceType;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.person.PersonSource;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.task.Task;
import app.briefingagent.task.TaskRepository;
import app.briefingagent.task.TaskStatus;
import app.briefingagent.task.TaskStatusHistory;
import app.briefingagent.task.TaskStatusHistoryRepository;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import java.time.LocalDate;
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
class TaskExtractionServiceTest {

    @Mock LlmClient llmClient;
    @Mock TaskRepository taskRepository;
    @Mock TaskStatusHistoryRepository historyRepository;
    @Mock PersonRepository personRepository;
    @Mock PersonGroupRepository personGroupRepository;
    @Mock TopicRepository topicRepository;

    TaskExtractionService service;

    private UserAccount author;
    private Ereignis ereignis;

    @BeforeEach
    void setUp() {
        service = new TaskExtractionService(llmClient, taskRepository, historyRepository,
                personRepository, personGroupRepository, topicRepository);
        author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
        ereignis = TestEntities.withRandomId(new Ereignis(author, EreignisSourceType.TEXT));
        ereignis.setTranscriptText("Heute mit Anna geplant: Folgetermin und Demo bauen.");
    }

    private void stubSavePassThrough() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return TestEntities.withRandomId(t);
        });
    }

    @Test
    void empty_transcript_skips_llm() {
        ereignis.setTranscriptText("");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).isEmpty();
        verify(llmClient, never()).complete(any());
    }

    @Test
    void empty_task_array_persists_nothing() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("{\"tasks\":[]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).isEmpty();
        verify(taskRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void malformed_json_is_swallowed() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("not json");

        assertThat(service.extractAndPersist(ereignis)).isEmpty();
    }

    @Test
    void candidate_without_title_is_dropped() {
        when(llmClient.complete(any(LlmRequest.class)))
                .thenReturn("{\"tasks\":[{\"description\":\"no title\",\"assignee\":\"self\"}]}");

        assertThat(service.extractAndPersist(ereignis)).isEmpty();
    }

    @Test
    void persists_self_assigned_task_with_open_status_and_initial_history() {
        stubSavePassThrough();
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"Folgetermin\",\"assignee\":\"self\",\"due_date\":\"2026-06-01\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).hasSize(1);
        Task saved = result.get(0);
        assertThat(saved.getTitle()).isEqualTo("Folgetermin");
        assertThat(saved.isAssignedToSelf()).isTrue();
        assertThat(saved.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(saved.getEreignis()).isSameAs(ereignis);

        ArgumentCaptor<TaskStatusHistory> captor = ArgumentCaptor.forClass(TaskStatusHistory.class);
        verify(historyRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isNull();
        assertThat(captor.getValue().getToStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void person_assignee_resolves_to_person_task() {
        stubSavePassThrough();
        Person anna = TestEntities.withRandomId(new Person("Anna Müller", PersonSource.MANUAL));
        when(personRepository.findById(anna.getId())).thenReturn(Optional.of(anna));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"Demo bauen\",\"assignee\":\"person:" + anna.getId() + "\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).singleElement().satisfies(t -> {
            assertThat(t.getAssignedToPerson()).isSameAs(anna);
            assertThat(t.isAssignedToSelf()).isFalse();
        });
    }

    @Test
    void persongroup_assignee_resolves_to_persongroup_task() {
        stubSavePassThrough();
        PersonGroup board = TestEntities.withRandomId(new PersonGroup(author, "Vorstand", "p"));
        when(personGroupRepository.findById(board.getId())).thenReturn(Optional.of(board));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"Status\",\"assignee\":\"persongroup:" + board.getId() + "\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).singleElement().satisfies(t ->
                assertThat(t.getAssignedToPersonGroup()).isSameAs(board));
    }

    @Test
    void topic_assignee_resolves_to_topic_task() {
        stubSavePassThrough();
        Topic alpha = TestEntities.withRandomId(new Topic(author, "Alpha", "p"));
        when(topicRepository.findById(alpha.getId())).thenReturn(Optional.of(alpha));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"Spec\",\"assignee\":\"topic:" + alpha.getId() + "\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).singleElement().satisfies(t ->
                assertThat(t.getAssignedToTopic()).isSameAs(alpha));
    }

    @Test
    void unknown_assignee_id_falls_back_to_self() {
        stubSavePassThrough();
        UUID unknown = UUID.randomUUID();
        when(personRepository.findById(unknown)).thenReturn(Optional.empty());
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"X\",\"assignee\":\"person:" + unknown + "\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).singleElement().satisfies(t -> assertThat(t.isAssignedToSelf()).isTrue());
    }

    @Test
    void missing_assignee_falls_back_to_self() {
        stubSavePassThrough();
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"Self-assign me\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).singleElement().satisfies(t -> assertThat(t.isAssignedToSelf()).isTrue());
    }

    @Test
    void invalid_due_date_is_dropped_silently() {
        stubSavePassThrough();
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"tasks\":[{\"title\":\"X\",\"assignee\":\"self\",\"due_date\":\"not-a-date\"}]}");

        List<Task> result = service.extractAndPersist(ereignis);

        assertThat(result).singleElement().satisfies(t -> assertThat(t.getDueDate()).isNull());
    }

    @Test
    void llm_request_targets_task_extraction_purpose() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("{\"tasks\":[]}");

        service.extractAndPersist(ereignis);

        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).complete(captor.capture());
        assertThat(captor.getValue().purpose()).isEqualTo(LlmPurpose.TASK_EXTRACTION);
        assertThat(captor.getValue().userPrompt()).contains("Autor: Demo");
    }
}
