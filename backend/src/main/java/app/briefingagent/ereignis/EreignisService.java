package app.briefingagent.ereignis;

import app.briefingagent.audience.AudienceQueryService;
import app.briefingagent.audience.AudienceRef;
import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.pipeline.AudienceClassificationService;
import app.briefingagent.pipeline.AudienceMatch;
import app.briefingagent.pipeline.SummaryGenerationService;
import app.briefingagent.stt.SttProviderClient;
import app.briefingagent.stt.TranscriptionResult;
import app.briefingagent.summary.ClassificationConfidence;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.DefaultTopicProvider;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EreignisService {

    private static final Logger LOG = LoggerFactory.getLogger(EreignisService.class);

    private static final String SYSTEM_PROMPT_SUMMARY =
            "Du bist ein Briefing-Assistent. Erstelle eine prägnante deutsche Zusammenfassung "
                    + "im Markdown-Format für die folgende Audience.";

    private final EreignisRepository ereignisRepository;
    private final SummaryRepository summaryRepository;
    private final UserAccountRepository userRepository;
    private final DefaultTopicProvider defaultTopicProvider;
    private final LlmClient llmClient;
    private final SttProviderClient sttClient;
    private final AudienceQueryService audienceQueryService;
    private final AudienceClassificationService classificationService;
    private final SummaryGenerationService summaryGenerationService;

    public EreignisService(EreignisRepository ereignisRepository,
                           SummaryRepository summaryRepository,
                           UserAccountRepository userRepository,
                           DefaultTopicProvider defaultTopicProvider,
                           LlmClient llmClient,
                           SttProviderClient sttClient,
                           AudienceQueryService audienceQueryService,
                           AudienceClassificationService classificationService,
                           SummaryGenerationService summaryGenerationService) {
        this.ereignisRepository = ereignisRepository;
        this.summaryRepository = summaryRepository;
        this.userRepository = userRepository;
        this.defaultTopicProvider = defaultTopicProvider;
        this.llmClient = llmClient;
        this.sttClient = sttClient;
        this.audienceQueryService = audienceQueryService;
        this.classificationService = classificationService;
        this.summaryGenerationService = summaryGenerationService;
    }

    @Transactional
    public Ereignis captureText(UUID authorId, String text) {
        UserAccount author = loadAuthor(authorId);
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Text must not be empty");
        }
        if (trimmed.length() > EreignisLimits.TEXT_HARD_CAP_CHARS) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Text exceeds hard cap of " + EreignisLimits.TEXT_HARD_CAP_CHARS + " characters");
        }

        Ereignis ereignis = new Ereignis(author, EreignisSourceType.TEXT);
        ereignis.setTranscriptText(trimmed);
        ereignis.setTranscriptSource(TranscriptSource.MANUAL);
        ereignis.setCharacterCount(trimmed.length());
        ereignisRepository.save(ereignis);
        runPipeline(author, ereignis, trimmed);
        return ereignis;
    }

    @Transactional
    public Ereignis captureAudio(UUID authorId, InputStream audioStream, String contentType,
                                 String filename, long sizeBytes) {
        UserAccount author = loadAuthor(authorId);
        if (sizeBytes <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio payload is empty");
        }
        if (!AudioMediaTypes.isAccepted(contentType)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported audio MIME type: " + contentType);
        }

        TranscriptionResult result;
        try (InputStream wrapped = audioStream) {
            result = sttClient.transcribe(wrapped, contentType, filename);
        } catch (IOException ex) {
            LOG.warn("Failed to read audio stream from upload: {}", ex.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read audio payload");
        }

        if (result.text().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Whisper returned no text");
        }

        Ereignis ereignis = new Ereignis(author, EreignisSourceType.AUDIO);
        ereignis.setTranscriptText(result.text());
        ereignis.setTranscriptSource(TranscriptSource.WHISPER);
        ereignis.setLanguage(result.language());
        ereignis.setDurationSeconds(result.durationSeconds());
        if (result.durationSeconds() != null
                && result.durationSeconds() >= EreignisLimits.AUDIO_HARD_CAP_SECONDS) {
            ereignis.setTruncatedAtLimit(true);
        }
        ereignis.setCharacterCount(result.text().length());
        ereignisRepository.save(ereignis);
        runPipeline(author, ereignis, result.text());
        return ereignis;
    }

    private void runPipeline(UserAccount author, Ereignis ereignis, String transcript) {
        List<AudienceRef> audiences = audienceQueryService.allFor(author);
        List<AudienceMatch> matches = classificationService.classify(transcript, audiences);
        if (matches.isEmpty()) {
            Topic fallback = defaultTopicProvider.ensureDefaultTopic(author);
            String body = llmClient.complete(new LlmRequest(
                    LlmPurpose.SUMMARY_GENERATION,
                    SYSTEM_PROMPT_SUMMARY,
                    transcript));
            Summary fallbackSummary = Summary.forTopic(ereignis, fallback, body);
            fallbackSummary.setClassificationConfidence(ClassificationConfidence.LOW);
            fallbackSummary.setClassificationReasoning(
                    "Keine relevante Audience erkannt — Fallback auf das persönliche Topic.");
            summaryRepository.save(fallbackSummary);
            return;
        }
        summaryGenerationService.generate(ereignis, matches);
    }

    @Transactional(readOnly = true)
    public List<Ereignis> recent(UUID authorId, int days) {
        UserAccount author = loadAuthor(authorId);
        Instant since = Instant.now().minus(Duration.ofDays(days));
        return ereignisRepository.findByAuthorAndCreatedAtAfterOrderByCreatedAtDesc(
                author, since, org.springframework.data.domain.PageRequest.of(0, 50));
    }

    private UserAccount loadAuthor(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
    }
}
