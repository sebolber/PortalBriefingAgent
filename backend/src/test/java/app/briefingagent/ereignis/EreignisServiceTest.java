package app.briefingagent.ereignis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.TestEntities;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.DefaultTopicProvider;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EreignisServiceTest {

    @Mock
    private EreignisRepository ereignisRepository;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private DefaultTopicProvider defaultTopicProvider;
    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private EreignisService service;

    private UserAccount author;
    private Topic defaultTopic;

    @BeforeEach
    void setUp() {
        author = TestEntities.withRandomId(
                new UserAccount("demo", "irrelevant", "Demo Author", "demo@example.invalid"));
        defaultTopic = TestEntities.withRandomId(new Topic(author, "My Notes", "persona"));
    }

    @Test
    void capture_text_persists_ereignis_and_summary() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(defaultTopicProvider.ensureDefaultTopic(author)).thenReturn(defaultTopic);
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("## Summary\n\nbody");

        Ereignis result = service.captureText(author.getId(), "Heute Workshop mit Anna.");

        assertThat(result.getSourceType()).isEqualTo(EreignisSourceType.TEXT);
        assertThat(result.getTranscriptText()).isEqualTo("Heute Workshop mit Anna.");
        assertThat(result.getTranscriptSource()).isEqualTo(TranscriptSource.MANUAL);
        assertThat(result.getCharacterCount()).isEqualTo(24);

        ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
        verify(summaryRepository, times(1)).save(summaryCaptor.capture());
        Summary saved = summaryCaptor.getValue();
        assertThat(saved.getAudienceTopic()).isSameAs(defaultTopic);
        assertThat(saved.getSummaryText()).isEqualTo("## Summary\n\nbody");

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).complete(requestCaptor.capture());
        assertThat(requestCaptor.getValue().purpose()).isEqualTo(LlmPurpose.SUMMARY_GENERATION);
    }

    @Test
    void capture_text_strips_outer_whitespace() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(defaultTopicProvider.ensureDefaultTopic(author)).thenReturn(defaultTopic);
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("ok");

        Ereignis result = service.captureText(author.getId(), "  hello world  ");

        assertThat(result.getTranscriptText()).isEqualTo("hello world");
        assertThat(result.getCharacterCount()).isEqualTo(11);
    }

    @Test
    void capture_text_rejects_empty_input() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.captureText(author.getId(), "   "))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(ereignisRepository, never()).save(any());
        verify(summaryRepository, never()).save(any());
        verify(llmClient, never()).complete(any());
    }

    @Test
    void capture_text_rejects_null_input() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.captureText(author.getId(), null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void capture_text_rejects_input_above_hard_cap() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        String tooLong = "a".repeat(EreignisLimits.TEXT_HARD_CAP_CHARS + 1);

        assertThatThrownBy(() -> service.captureText(author.getId(), tooLong))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(String.valueOf(EreignisLimits.TEXT_HARD_CAP_CHARS));

        verify(llmClient, never()).complete(any());
    }

    @Test
    void capture_text_rejects_unknown_author() {
        UUID stranger = UUID.randomUUID();
        when(userRepository.findById(stranger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.captureText(stranger, "hi"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void recent_passes_window_to_repository() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(ereignisRepository
                .findByAuthorAndCreatedAtAfterOrderByCreatedAtDesc(eq(author), any(), any()))
                .thenReturn(java.util.List.of());

        service.recent(author.getId(), 7);

        verify(ereignisRepository, times(1))
                .findByAuthorAndCreatedAtAfterOrderByCreatedAtDesc(eq(author), any(), any());
    }
}
