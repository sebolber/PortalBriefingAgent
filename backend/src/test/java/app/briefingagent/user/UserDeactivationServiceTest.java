package app.briefingagent.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.TestEntities;
import app.briefingagent.retention.RetentionProperties;
import app.briefingagent.task.Task;
import app.briefingagent.task.TaskRepository;
import app.briefingagent.task.TaskStatus;
import app.briefingagent.task.TaskStatusHistory;
import app.briefingagent.task.TaskStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserDeactivationServiceTest {

    @Mock
    UserAccountRepository userRepository;
    @Mock
    TaskRepository taskRepository;
    @Mock
    TaskStatusHistoryRepository historyRepository;

    UserDeactivationService service;

    private UserAccount target;
    private UserAccount admin;
    private final Instant now = Instant.parse("2026-05-13T00:00:00Z");
    private final RetentionProperties props = new RetentionProperties();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
        service = new UserDeactivationService(userRepository, taskRepository,
                historyRepository, props, clock);
        target = TestEntities.withRandomId(
                new UserAccount("victim", "x", "Victim", "victim@example.invalid"));
        admin = TestEntities.withRandomId(
                new UserAccount("admin", "x", "Admin", "admin@example.invalid"));
        admin.setAdmin(true);
    }

    @Test
    void deactivate_sets_grace_window_six_months_ahead() {
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(taskRepository.findByAuthorAndStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());

        UserAccount result = service.deactivate(target.getId(), admin.getId());

        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(result.getDeactivatedAt()).isEqualTo(now);
        assertThat(result.getDeletionScheduledAt()).isAfter(now);
    }

    @Test
    void deactivate_drops_open_self_assigned_tasks_with_history() {
        Task openSelf = TestEntities.withRandomId(Task.forSelf(target, "do me"));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(taskRepository.findByAuthorAndStatusOrderByCreatedAtDesc(target, TaskStatus.OPEN))
                .thenReturn(List.of(openSelf));
        when(taskRepository.findByAuthorAndStatusOrderByCreatedAtDesc(target, TaskStatus.IN_PROGRESS))
                .thenReturn(List.of());

        service.deactivate(target.getId(), admin.getId());

        assertThat(openSelf.getStatus()).isEqualTo(TaskStatus.DROPPED);
        assertThat(openSelf.getDroppedAt()).isEqualTo(now);
        verify(historyRepository, times(1)).save(any(TaskStatusHistory.class));
    }

    @Test
    void already_inactive_deactivation_is_a_noop() {
        target.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        UserAccount result = service.deactivate(target.getId(), admin.getId());

        assertThat(result).isSameAs(target);
        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void reactivation_inside_grace_window_succeeds() {
        target.setStatus(UserStatus.INACTIVE);
        target.setDeactivatedAt(now.minusSeconds(86_400));
        target.setDeletionScheduledAt(now.plusSeconds(86_400));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        UserAccount result = service.reactivate(target.getId());

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getDeactivatedAt()).isNull();
        assertThat(result.getDeletionScheduledAt()).isNull();
    }

    @Test
    void reactivation_after_grace_window_returns_409() {
        target.setStatus(UserStatus.INACTIVE);
        target.setDeletionScheduledAt(now.minusSeconds(60));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.reactivate(target.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
