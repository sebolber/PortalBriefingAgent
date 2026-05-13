package app.briefingagent.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.TestEntities;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;
    @Mock
    TaskStatusHistoryRepository historyRepository;
    @Mock
    UserAccountRepository userRepository;

    TaskService service;

    private UserAccount author;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepository, historyRepository, userRepository);
        author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
        authorId = author.getId();
    }

    @Test
    void create_writes_initial_history_entry() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        Task draft = Task.forSelf(author, "Follow up Anna");
        when(taskRepository.save(draft)).thenReturn(TestEntities.withRandomId(draft));

        service.create(author.getId(), draft);

        ArgumentCaptor<TaskStatusHistory> captor = ArgumentCaptor.forClass(TaskStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isNull();
        assertThat(captor.getValue().getToStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void change_status_appends_history_with_note() {
        Task task = TestEntities.withRandomId(Task.forSelf(author, "do thing"));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        service.changeStatus(author.getId(), task.getId(), TaskStatus.IN_PROGRESS, "started");

        ArgumentCaptor<TaskStatusHistory> captor = ArgumentCaptor.forClass(TaskStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(captor.getValue().getToStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(captor.getValue().getNote()).isEqualTo("started");
    }

    @Test
    void change_status_to_done_sets_completed_at() {
        Task task = TestEntities.withRandomId(Task.forSelf(author, "do thing"));
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task updated = service.changeStatus(author.getId(), task.getId(), TaskStatus.DONE, null);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(updated.getCompletedAt()).isNotNull();
    }

    @Test
    void change_status_is_no_op_when_already_in_target() {
        Task task = TestEntities.withRandomId(Task.forSelf(author, "x"));
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        service.changeStatus(author.getId(), task.getId(), TaskStatus.OPEN, "noop");

        verify(taskRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void change_status_from_terminal_returns_409() {
        Task task = TestEntities.withRandomId(Task.forSelf(author, "x"));
        task.setStatus(TaskStatus.DONE);
        UUID taskId = task.getId();
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.changeStatus(authorId, taskId, TaskStatus.IN_PROGRESS, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void list_for_other_authors_task_returns_404_via_ownership_check() {
        Task task = TestEntities.withRandomId(Task.forSelf(
                TestEntities.withRandomId(new UserAccount("other", "x", "Other", "other@example.invalid")),
                "x"));
        UUID taskId = task.getId();
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.changeStatus(authorId, taskId, TaskStatus.DONE, null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void edit_fields_updates_title_and_description() {
        Task task = TestEntities.withRandomId(Task.forSelf(author, "old"));
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task updated = service.editFields(author.getId(), task.getId(), "new", "details", null);

        assertThat(updated.getTitle()).isEqualTo("new");
        assertThat(updated.getDescription()).isEqualTo("details");
        verify(historyRepository, times(0)).save(any());
    }
}
