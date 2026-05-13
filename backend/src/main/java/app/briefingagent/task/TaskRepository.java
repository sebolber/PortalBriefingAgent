package app.briefingagent.task;

import app.briefingagent.user.UserAccount;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByAuthorOrderByCreatedAtDesc(UserAccount author);

    List<Task> findByAuthorAndStatusOrderByCreatedAtDesc(UserAccount author, TaskStatus status);

    List<Task> findByDueDateLessThanEqualAndStatusIn(LocalDate dueDate, List<TaskStatus> statuses);
}
