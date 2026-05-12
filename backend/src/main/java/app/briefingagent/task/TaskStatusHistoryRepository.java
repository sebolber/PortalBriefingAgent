package app.briefingagent.task;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, UUID> {

    List<TaskStatusHistory> findByTaskOrderByChangedAtAsc(Task task);
}
