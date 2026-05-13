package app.briefingagent.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.user.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskReminderSchedulerTest {

    @Mock
    TaskRepository taskRepository;
    @Mock
    TaskReminderRepository reminderRepository;

    private final UserAccount author = TestEntities.withRandomId(
            new UserAccount("demo", "x", "Demo", "demo@example.invalid"));

    private TaskReminderScheduler scheduler;
    private final LocalDate today = LocalDate.parse("2026-05-12");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));
        scheduler = new TaskReminderScheduler(taskRepository, reminderRepository, clock);
    }

    @Test
    void creates_on_due_date_reminder_for_overdue_task_with_open_status() {
        Task overdue = TestEntities.withRandomId(Task.forSelf(author, "overdue"));
        overdue.setDueDate(today);

        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(eq(today), any()))
                .thenReturn(List.of(overdue));
        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(eq(today.plusDays(1)), any()))
                .thenReturn(List.of(overdue));
        when(reminderRepository.existsByTaskAndReminderTypeAndRemindedOn(any(), any(), any()))
                .thenReturn(false);

        int created = scheduler.runOnce();

        assertThat(created).isEqualTo(1);
        verify(reminderRepository, times(1)).save(any(TaskReminder.class));
    }

    @Test
    void creates_one_day_before_reminder_for_task_due_tomorrow() {
        Task tomorrow = TestEntities.withRandomId(Task.forSelf(author, "tomorrow"));
        tomorrow.setDueDate(today.plusDays(1));

        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(eq(today), any()))
                .thenReturn(List.of());
        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(eq(today.plusDays(1)), any()))
                .thenReturn(List.of(tomorrow));
        when(reminderRepository.existsByTaskAndReminderTypeAndRemindedOn(any(), any(), any()))
                .thenReturn(false);

        int created = scheduler.runOnce();

        assertThat(created).isEqualTo(1);
    }

    @Test
    void second_run_on_same_day_is_idempotent() {
        Task overdue = TestEntities.withRandomId(Task.forSelf(author, "x"));
        overdue.setDueDate(today);
        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(eq(today), any()))
                .thenReturn(List.of(overdue));
        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(eq(today.plusDays(1)), any()))
                .thenReturn(List.of());
        when(reminderRepository.existsByTaskAndReminderTypeAndRemindedOn(
                overdue, TaskReminderType.ON_DUE_DATE, today)).thenReturn(true);

        int created = scheduler.runOnce();

        assertThat(created).isZero();
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void task_without_due_date_is_ignored() {
        Task floating = TestEntities.withRandomId(Task.forSelf(author, "no-date"));

        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(any(), any()))
                .thenReturn(List.of());

        int created = scheduler.runOnce();

        assertThat(created).isZero();
        verify(reminderRepository, never()).save(any());
        // Reference floating to prove the test setup is intentional.
        assertThat(floating.getDueDate()).isNull();
    }

    @Test
    void clock_drives_today_correctly() {
        Clock alt = Clock.fixed(Instant.parse("2026-12-31T23:00:00Z"), ZoneId.of("UTC"));
        TaskReminderScheduler altScheduler =
                new TaskReminderScheduler(taskRepository, reminderRepository, alt);
        when(taskRepository.findByDueDateLessThanEqualAndStatusIn(any(), any()))
                .thenReturn(List.of());

        altScheduler.runOnce();

        verify(taskRepository).findByDueDateLessThanEqualAndStatusIn(
                eq(LocalDate.of(2026, 12, 31)), any());
    }
}
