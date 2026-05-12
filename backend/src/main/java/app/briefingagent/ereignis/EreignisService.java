package app.briefingagent.ereignis;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.DefaultTopicProvider;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EreignisService {

    private static final String SYSTEM_PROMPT_SUMMARY =
            "Du bist ein Briefing-Assistent. Erstelle eine prägnante deutsche Zusammenfassung "
                    + "im Markdown-Format für die folgende Audience.";

    private final EreignisRepository ereignisRepository;
    private final SummaryRepository summaryRepository;
    private final UserAccountRepository userRepository;
    private final DefaultTopicProvider defaultTopicProvider;
    private final LlmClient llmClient;

    public EreignisService(EreignisRepository ereignisRepository,
                           SummaryRepository summaryRepository,
                           UserAccountRepository userRepository,
                           DefaultTopicProvider defaultTopicProvider,
                           LlmClient llmClient) {
        this.ereignisRepository = ereignisRepository;
        this.summaryRepository = summaryRepository;
        this.userRepository = userRepository;
        this.defaultTopicProvider = defaultTopicProvider;
        this.llmClient = llmClient;
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

        Topic defaultTopic = defaultTopicProvider.ensureDefaultTopic(author);
        String summaryText = llmClient.complete(new LlmRequest(
                LlmPurpose.SUMMARY_GENERATION,
                SYSTEM_PROMPT_SUMMARY,
                trimmed));
        summaryRepository.save(Summary.forTopic(ereignis, defaultTopic, summaryText));
        return ereignis;
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
