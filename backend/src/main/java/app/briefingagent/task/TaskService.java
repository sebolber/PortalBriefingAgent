package app.briefingagent.task;

import app.briefingagent.common.ApiException;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository historyRepository;
    private final UserAccountRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskStatusHistoryRepository historyRepository,
                       UserAccountRepository userRepository) {
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Task> listFor(UUID authorId) {
        return taskRepository.findByAuthorOrderByCreatedAtDesc(loadAuthor(authorId));
    }

    @Transactional
    public Task create(UUID authorId, Task draft) {
        UserAccount author = loadAuthor(authorId);
        if (!draft.getAuthor().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot create task on behalf of another author");
        }
        Task saved = taskRepository.save(draft);
        historyRepository.save(new TaskStatusHistory(
                saved, null, saved.getStatus(), "created", author));
        return saved;
    }

    @Transactional
    public Task changeStatus(UUID authorId, UUID taskId, TaskStatus to, String note) {
        UserAccount author = loadAuthor(authorId);
        Task task = loadOwn(authorId, taskId);
        TaskStatus from = task.getStatus();
        if (from == to) {
            return task;
        }
        if (from.isTerminal()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Task is in a terminal state and cannot transition further");
        }
        task.setStatus(to);
        Instant now = Instant.now();
        if (to == TaskStatus.DONE) {
            task.setCompletedAt(now);
        } else if (to == TaskStatus.DROPPED) {
            task.setDroppedAt(now);
        }
        Task saved = taskRepository.save(task);
        historyRepository.save(new TaskStatusHistory(saved, from, to, note, author));
        return saved;
    }

    @Transactional
    public Task editFields(UUID authorId, UUID taskId, String title, String description,
                           java.time.LocalDate dueDate) {
        Task task = loadOwn(authorId, taskId);
        if (title != null) {
            task.setTitle(title);
        }
        task.setDescription(description);
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }

    @Transactional
    public void delete(UUID authorId, UUID taskId) {
        Task task = loadOwn(authorId, taskId);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskStatusHistory> historyOf(UUID authorId, UUID taskId) {
        Task task = loadOwn(authorId, taskId);
        return historyRepository.findByTaskOrderByChangedAtAsc(task);
    }

    private Task loadOwn(UUID authorId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!task.getAuthor().getId().equals(authorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return task;
    }

    private UserAccount loadAuthor(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
    }
}
