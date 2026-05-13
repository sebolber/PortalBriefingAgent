package app.briefingagent.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily scan for due-date-driven reminders. We persist a {@link TaskReminder}
 * per (task, type, calendar day) so a job restart on the same day cannot
 * fire the same reminder twice. Phase 1 only logs the notification — the
 * /api/notifications endpoint exposes them to the frontend poll loop.
 */
@Component
@EnableScheduling
public class TaskReminderScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TaskReminderScheduler.class);

    private final TaskRepository taskRepository;
    private final TaskReminderRepository reminderRepository;
    private final Clock clock;

    public TaskReminderScheduler(TaskRepository taskRepository,
                                 TaskReminderRepository reminderRepository,
                                 Clock clock) {
        this.taskRepository = taskRepository;
        this.reminderRepository = reminderRepository;
        this.clock = clock;
    }

    /**
     * Runs daily at 06:00 UTC; tests can call {@link #runOnce()} directly.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    @Transactional
    public int runOnce() {
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);

        int created = 0;
        List<Task> dueToday = taskRepository.findByDueDateLessThanEqualAndStatusIn(
                today, List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS));
        for (Task task : dueToday) {
            created += persistIfMissing(task, TaskReminderType.ON_DUE_DATE, today);
        }
        List<Task> dueTomorrow = taskRepository.findByDueDateLessThanEqualAndStatusIn(
                tomorrow, List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS));
        for (Task task : dueTomorrow) {
            if (task.getDueDate() != null && task.getDueDate().isEqual(tomorrow)) {
                created += persistIfMissing(task, TaskReminderType.ONE_DAY_BEFORE, today);
            }
        }
        LOG.debug("TaskReminderScheduler ran for {}, created {} reminder rows.", today, created);
        return created;
    }

    private int persistIfMissing(Task task, TaskReminderType type, LocalDate runDate) {
        if (reminderRepository.existsByTaskAndReminderTypeAndRemindedOn(task, type, runDate)) {
            return 0;
        }
        reminderRepository.save(new TaskReminder(task, type, runDate));
        return 1;
    }
}
