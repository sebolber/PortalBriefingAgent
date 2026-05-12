package app.briefingagent.pipeline;

import app.briefingagent.audience.AudienceRef;
import app.briefingagent.common.ApiException;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates one Summary per matched audience. Each audience gets its own
 * prompt with that audience's persona text in the system block — this is
 * the multi-shot pattern from the spec (§6.2). Calls are sequential in
 * phase 1; introducing an executor would only help once the LLM provider
 * supports parallel requests reliably.
 */
@Service
public class SummaryGenerationService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Du bist ein Briefing-Assistent. Erstelle eine prägnante deutsche
            Zusammenfassung im Markdown-Format speziell für folgende Audience:

            Name: %s
            Persona: %s

            Halte dich an Stil und Fokus der Persona-Beschreibung. Antwort: Markdown,
            keine Erklärungen, keine Code-Blöcke.
            """;

    private final LlmClient llmClient;
    private final SummaryRepository summaryRepository;
    private final PersonRepository personRepository;
    private final PersonGroupRepository personGroupRepository;
    private final TopicRepository topicRepository;

    public SummaryGenerationService(LlmClient llmClient,
                                    SummaryRepository summaryRepository,
                                    PersonRepository personRepository,
                                    PersonGroupRepository personGroupRepository,
                                    TopicRepository topicRepository) {
        this.llmClient = llmClient;
        this.summaryRepository = summaryRepository;
        this.personRepository = personRepository;
        this.personGroupRepository = personGroupRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public List<Summary> generate(Ereignis ereignis, List<AudienceMatch> matches) {
        List<Summary> created = new ArrayList<>();
        for (AudienceMatch match : matches) {
            Summary summary = buildSummary(ereignis, match);
            created.add(summaryRepository.save(summary));
        }
        return created;
    }

    private Summary buildSummary(Ereignis ereignis, AudienceMatch match) {
        AudienceRef ref = match.audience();
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE,
                ref.name(),
                ref.personaText() == null ? "(keine Persona hinterlegt)" : ref.personaText());
        String body = llmClient.complete(new LlmRequest(
                LlmPurpose.SUMMARY_GENERATION,
                systemPrompt,
                ereignis.getTranscriptText()));
        Summary summary = switch (ref.type()) {
            case PERSON -> Summary.forPerson(ereignis,
                    personRepository.findById(ref.id())
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Person missing")),
                    body);
            case PERSONGROUP -> Summary.forPersonGroup(ereignis,
                    personGroupRepository.findById(ref.id())
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group missing")),
                    body);
            case TOPIC -> Summary.forTopic(ereignis,
                    topicRepository.findById(ref.id())
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Topic missing")),
                    body);
        };
        summary.setClassificationConfidence(match.confidence());
        summary.setClassificationReasoning(match.reasoning());
        return summary;
    }
}
