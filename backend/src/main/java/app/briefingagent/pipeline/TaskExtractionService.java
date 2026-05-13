package app.briefingagent.pipeline;

import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.llm.LlmClient;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.llm.LlmRequest;
import app.briefingagent.person.Person;
import app.briefingagent.person.PersonRepository;
import app.briefingagent.persongroup.PersonGroup;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.task.Task;
import app.briefingagent.task.TaskRepository;
import app.briefingagent.task.TaskStatus;
import app.briefingagent.task.TaskStatusHistory;
import app.briefingagent.task.TaskStatusHistoryRepository;
import app.briefingagent.topic.Topic;
import app.briefingagent.topic.TopicRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls task candidates out of a transcript via the configured LLM.
 *
 * <p>The pipeline saves the resulting candidates as {@link Task} rows with
 * {@code status = open}; the review screen lets the author edit, drop or
 * complete them. Following the iteration plan's edge-case rule, an
 * assignee the LLM either omits or cannot resolve falls back to
 * {@code assignedToSelf = true}.
 */
@Service
public class TaskExtractionService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskExtractionService.class);

    private static final String SYSTEM_PROMPT = """
            Du bist ein Aufgaben-Extraktor für ein internes Briefing-Tool.
            Liefere alle aus dem Transkript entstehenden Aufgaben in JSON.
            """;

    private final LlmClient llmClient;
    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository historyRepository;
    private final PersonRepository personRepository;
    private final PersonGroupRepository personGroupRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public TaskExtractionService(LlmClient llmClient,
                                 TaskRepository taskRepository,
                                 TaskStatusHistoryRepository historyRepository,
                                 PersonRepository personRepository,
                                 PersonGroupRepository personGroupRepository,
                                 TopicRepository topicRepository) {
        this.llmClient = llmClient;
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.personRepository = personRepository;
        this.personGroupRepository = personGroupRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public List<Task> extractAndPersist(Ereignis ereignis) {
        if (ereignis.getTranscriptText() == null || ereignis.getTranscriptText().isBlank()) {
            return List.of();
        }
        String userPrompt = "Autor: " + ereignis.getAuthor().getFullName()
                + "\n\nTranskript:\n" + ereignis.getTranscriptText();
        String response = llmClient.complete(new LlmRequest(
                LlmPurpose.TASK_EXTRACTION, SYSTEM_PROMPT, userPrompt));
        List<Candidate> candidates = parse(response);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Task> created = new ArrayList<>();
        for (Candidate c : candidates) {
            Task task = buildTask(ereignis, c);
            task.setEreignis(ereignis);
            Task saved = taskRepository.save(task);
            historyRepository.save(new TaskStatusHistory(
                    saved, null, TaskStatus.OPEN, "extracted from transcript",
                    ereignis.getAuthor()));
            created.add(saved);
        }
        return created;
    }

    Task buildTask(Ereignis ereignis, Candidate c) {
        Task task = resolveAssignment(ereignis, c.title(), c.assignee());
        task.setDescription(c.description());
        task.setDueDate(c.dueDate());
        return task;
    }

    private Task resolveAssignment(Ereignis ereignis, String title, String assignee) {
        if (assignee == null || assignee.isBlank() || "self".equalsIgnoreCase(assignee)) {
            return Task.forSelf(ereignis.getAuthor(), title);
        }
        int colon = assignee.indexOf(':');
        if (colon < 0) {
            return Task.forSelf(ereignis.getAuthor(), title);
        }
        String type = assignee.substring(0, colon).toLowerCase();
        String idPart = assignee.substring(colon + 1).trim();
        UUID id;
        try {
            id = UUID.fromString(idPart);
        } catch (IllegalArgumentException ex) {
            LOG.debug("Task extractor returned non-UUID assignee id '{}'", idPart);
            return Task.forSelf(ereignis.getAuthor(), title);
        }
        return switch (type) {
            case "person" -> personRepository.findById(id)
                    .map(p -> (Task) Task.forPerson(ereignis.getAuthor(), title, p))
                    .orElseGet(() -> Task.forSelf(ereignis.getAuthor(), title));
            case "persongroup" -> personGroupRepository.findById(id)
                    .map(g -> (Task) Task.forPersonGroup(ereignis.getAuthor(), title, g))
                    .orElseGet(() -> Task.forSelf(ereignis.getAuthor(), title));
            case "topic" -> topicRepository.findById(id)
                    .map(t -> (Task) Task.forTopic(ereignis.getAuthor(), title, t))
                    .orElseGet(() -> Task.forSelf(ereignis.getAuthor(), title));
            default -> Task.forSelf(ereignis.getAuthor(), title);
        };
    }

    private List<Candidate> parse(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = mapper.readTree(response);
        } catch (Exception ex) {
            LOG.warn("Task extractor returned malformed JSON: {}", ex.getMessage());
            return List.of();
        }
        JsonNode array = root.get("tasks");
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<Candidate> out = new ArrayList<>();
        for (JsonNode entry : array) {
            String title = textOrNull(entry, "title");
            if (title == null || title.isBlank()) {
                continue;
            }
            Candidate candidate = new Candidate(
                    title.length() > 200 ? title.substring(0, 200) : title,
                    textOrNull(entry, "description"),
                    parseDate(textOrNull(entry, "due_date")),
                    textOrNull(entry, "assignee"));
            out.add(candidate);
        }
        return out;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    record Candidate(String title, String description, LocalDate dueDate, String assignee) {
    }

    List<Candidate> parseForTesting(String response) {
        return parse(response);
    }
}
