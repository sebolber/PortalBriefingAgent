package app.briefingagent.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.briefingagent.audience.AudienceRef;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.summary.AudienceType;
import app.briefingagent.summary.ClassificationConfidence;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AudienceClassificationServiceTest {

    @Mock
    private LlmClient llmClient;

    private AudienceClassificationService service;

    private final UUID annaId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID boardId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID topicId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final List<AudienceRef> audiences = List.of(
            new AudienceRef(AudienceType.PERSON, annaId, "Anna Müller", "Direkter Stil, kurz."),
            new AudienceRef(AudienceType.PERSONGROUP, boardId, "Vorstand", "Strategischer Fokus."),
            new AudienceRef(AudienceType.TOPIC, topicId, "Produkt Alpha", "Technische Tiefe."));

    @BeforeEach
    void setUp() {
        service = new AudienceClassificationService(llmClient);
    }

    @Test
    void empty_transcript_skips_llm() {
        List<AudienceMatch> result = service.classify("   ", audiences);

        assertThat(result).isEmpty();
        verifyNoInteractions(llmClient);
    }

    @Test
    void empty_audiences_skip_llm() {
        List<AudienceMatch> result = service.classify("Heute Workshop", List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(llmClient);
    }

    @Test
    void parses_happy_path_with_confidence_and_reasoning() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"audiences\":[{\"id\":\"" + annaId + "\",\"type\":\"person\","
                        + "\"confidence\":\"high\",\"reasoning\":\"Anna war Teil des Termins\"}]}");

        List<AudienceMatch> result = service.classify("Anna war heute …", audiences);

        assertThat(result).hasSize(1);
        AudienceMatch match = result.get(0);
        assertThat(match.audience().id()).isEqualTo(annaId);
        assertThat(match.confidence()).isEqualTo(ClassificationConfidence.HIGH);
        assertThat(match.reasoning()).isEqualTo("Anna war Teil des Termins");
    }

    @Test
    void llm_request_targets_classification_purpose() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("{\"audiences\":[]}");
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);

        service.classify("Heute Workshop", audiences);

        org.mockito.Mockito.verify(llmClient).complete(captor.capture());
        assertThat(captor.getValue().purpose()).isEqualTo(LlmPurpose.AUDIENCE_CLASSIFICATION);
        assertThat(captor.getValue().userPrompt()).contains(annaId.toString());
        assertThat(captor.getValue().userPrompt()).contains("Vorstand");
    }

    @Test
    void malformed_json_returns_empty_list() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("not json");

        assertThat(service.classify("anything", audiences)).isEmpty();
    }

    @Test
    void unknown_audience_id_is_filtered_out() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"audiences\":[{\"id\":\"99999999-9999-9999-9999-999999999999\","
                        + "\"type\":\"person\",\"confidence\":\"low\",\"reasoning\":\"foo\"}]}");

        assertThat(service.classify("text", audiences)).isEmpty();
    }

    @Test
    void mismatched_type_for_known_id_is_filtered_out() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"audiences\":[{\"id\":\"" + annaId + "\",\"type\":\"topic\","
                        + "\"confidence\":\"high\"}]}");

        assertThat(service.classify("text", audiences)).isEmpty();
    }

    @Test
    void mixed_audience_types_are_preserved() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"audiences\":["
                        + "{\"id\":\"" + annaId + "\",\"type\":\"person\",\"confidence\":\"high\",\"reasoning\":\"a\"},"
                        + "{\"id\":\"" + boardId + "\",\"type\":\"persongroup\",\"confidence\":\"medium\"},"
                        + "{\"id\":\"" + topicId + "\",\"type\":\"topic\",\"confidence\":\"low\"}"
                        + "]}");

        List<AudienceMatch> result = service.classify("text", audiences);

        assertThat(result).extracting(m -> m.audience().type())
                .containsExactly(AudienceType.PERSON, AudienceType.PERSONGROUP, AudienceType.TOPIC);
        assertThat(result).extracting(AudienceMatch::confidence)
                .containsExactly(ClassificationConfidence.HIGH,
                        ClassificationConfidence.MEDIUM,
                        ClassificationConfidence.LOW);
    }

    @Test
    void invalid_confidence_string_falls_back_to_low() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn(
                "{\"audiences\":[{\"id\":\"" + annaId + "\",\"type\":\"person\","
                        + "\"confidence\":\"super\"}]}");

        assertThat(service.classify("x", audiences))
                .singleElement()
                .extracting(AudienceMatch::confidence)
                .isEqualTo(ClassificationConfidence.LOW);
    }

    @Test
    void empty_audiences_array_returns_empty() {
        when(llmClient.complete(any(LlmRequest.class))).thenReturn("{\"audiences\":[]}");

        assertThat(service.classify("text", audiences)).isEmpty();
    }
}
