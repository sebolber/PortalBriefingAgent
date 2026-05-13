package app.briefingagent.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.TestEntities;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisSourceType;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SummaryReviewServiceTest {

    @Mock
    SummaryRepository repository;
    @Mock
    LlmClient llmClient;

    SummaryReviewService service;

    private UserAccount author;
    private Ereignis ereignis;
    private Topic topic;
    private Summary summary;

    @BeforeEach
    void setUp() {
        service = new SummaryReviewService(repository, llmClient);
        author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
        ereignis = TestEntities.withRandomId(new Ereignis(author, EreignisSourceType.TEXT));
        ereignis.setTranscriptText("Heute Workshop mit Anna");
        topic = TestEntities.withRandomId(new Topic(author, "My Notes", "personal"));
        summary = TestEntities.withRandomId(Summary.forTopic(ereignis, topic, "## Ursprünglich"));
    }

    private void stubSavePassThrough() {
        when(repository.save(any(Summary.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void edit_appends_history_and_flips_state() {
        stubSavePassThrough();
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));

        Summary updated = service.edit(summary.getId(), author.getId(), "## Geändert");

        assertThat(updated.getSummaryText()).isEqualTo("## Geändert");
        assertThat(updated.getEditState()).isEqualTo(EditState.MANUALLY_EDITED);
        assertThat(updated.getEditHistory()).singleElement().satisfies(entry -> {
            assertThat(entry.changeType()).isEqualTo(EditHistoryEntry.TYPE_MANUAL_EDIT);
            assertThat(entry.previousText()).isEqualTo("## Ursprünglich");
            assertThat(entry.newText()).isEqualTo("## Geändert");
            assertThat(entry.changedByAuthorId()).isEqualTo(author.getId());
        });
    }

    @Test
    void edit_with_identical_text_is_a_noop() {
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));

        service.edit(summary.getId(), author.getId(), "## Ursprünglich");

        assertThat(summary.getEditHistory()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void edit_rejects_blank_text() {
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.edit(summary.getId(), author.getId(), "   "))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void edit_rejects_already_accepted_summary() {
        summary.setAcceptedAt(Instant.now());
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.edit(summary.getId(), author.getId(), "x"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void regenerate_passes_feedback_into_llm_call_and_logs_history() {
        stubSavePassThrough();
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("## Neu");

        Summary updated = service.regenerate(summary.getId(), author.getId(), "kürzer bitte");

        assertThat(updated.getSummaryText()).isEqualTo("## Neu");
        assertThat(updated.getEditState()).isEqualTo(EditState.REGENERATED);
        assertThat(updated.getRegenerationFeedback()).isEqualTo("kürzer bitte");
        assertThat(updated.getEditHistory()).singleElement().satisfies(entry -> {
            assertThat(entry.changeType()).isEqualTo(EditHistoryEntry.TYPE_REGENERATED);
            assertThat(entry.feedback()).isEqualTo("kürzer bitte");
            assertThat(entry.previousText()).isEqualTo("## Ursprünglich");
            assertThat(entry.newText()).isEqualTo("## Neu");
        });
    }

    @Test
    void regenerate_with_blank_feedback_works_and_records_null_feedback() {
        stubSavePassThrough();
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("## Neu");

        Summary updated = service.regenerate(summary.getId(), author.getId(), "   ");

        assertThat(updated.getRegenerationFeedback()).isNull();
        assertThat(updated.getEditHistory()).singleElement()
                .satisfies(entry -> assertThat(entry.feedback()).isNull());
    }

    @Test
    void regenerate_rejects_already_accepted_summary() {
        summary.setAcceptedAt(Instant.now());
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.regenerate(summary.getId(), author.getId(), "x"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void accept_sets_accepted_at_and_is_idempotent() {
        stubSavePassThrough();
        when(repository.findById(summary.getId())).thenReturn(Optional.of(summary));

        Summary first = service.accept(summary.getId());
        Instant firstAccepted = first.getAcceptedAt();
        assertThat(firstAccepted).isNotNull();

        Summary second = service.accept(summary.getId());

        assertThat(second.getAcceptedAt()).isEqualTo(firstAccepted);
    }

    @Test
    void unknown_summary_returns_404() {
        UUID missing = UUID.randomUUID();
        when(repository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(missing))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
