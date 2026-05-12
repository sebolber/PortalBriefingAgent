package app.briefingagent.task;

import app.briefingagent.security.CurrentAuthor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Duration RECENT_WINDOW = Duration.ofDays(7);

    private final TaskReminderRepository reminderRepository;
    private final CurrentAuthor currentAuthor;

    public NotificationController(TaskReminderRepository reminderRepository,
                                  CurrentAuthor currentAuthor) {
        this.reminderRepository = reminderRepository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping
    public List<View> list() {
        Instant since = Instant.now().minus(RECENT_WINDOW);
        var authorId = currentAuthor.requireUserId();
        return reminderRepository.findAll().stream()
                .filter(r -> r.getRemindedAt().isAfter(since))
                .filter(r -> r.getTask().getAuthor().getId().equals(authorId))
                .sorted((a, b) -> b.getRemindedAt().compareTo(a.getRemindedAt()))
                .map(View::from)
                .toList();
    }

    public record View(String taskId, String taskTitle, String reminderType, String remindedAt) {

        public static View from(TaskReminder r) {
            return new View(
                    r.getTask().getId().toString(),
                    r.getTask().getTitle(),
                    r.getReminderType().dbValue(),
                    r.getRemindedAt().toString());
        }
    }
}
