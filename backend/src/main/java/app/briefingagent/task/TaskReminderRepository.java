package app.briefingagent.task;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskReminderRepository extends JpaRepository<TaskReminder, UUID> {

    boolean existsByTaskAndReminderTypeAndRemindedOn(
            Task task, TaskReminderType type, LocalDate remindedOn);
}
