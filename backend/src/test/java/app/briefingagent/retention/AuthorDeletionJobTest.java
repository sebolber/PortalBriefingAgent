package app.briefingagent.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import app.briefingagent.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorDeletionJobTest {

    @Mock
    UserAccountRepository userRepository;

    private AuthorDeletionJob job;
    private final Instant now = Instant.parse("2026-05-13T00:00:00Z");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
        job = new AuthorDeletionJob(userRepository, clock);
    }

    @Test
    void deletes_inactive_user_past_grace_window() {
        UserAccount due = TestEntities.withRandomId(
                new UserAccount("a", "x", "A", "a@example.invalid"));
        due.setStatus(UserStatus.INACTIVE);
        due.setDeletionScheduledAt(now.minusSeconds(60));
        when(userRepository.findAll()).thenReturn(List.of(due));

        int deleted = job.runOnce();

        assertThat(deleted).isEqualTo(1);
        verify(userRepository, times(1)).delete(due);
    }

    @Test
    void leaves_active_user_alone_even_with_scheduled_deletion() {
        UserAccount active = TestEntities.withRandomId(
                new UserAccount("a", "x", "A", "a@example.invalid"));
        active.setStatus(UserStatus.ACTIVE);
        active.setDeletionScheduledAt(now.minusSeconds(60));
        when(userRepository.findAll()).thenReturn(List.of(active));

        int deleted = job.runOnce();

        assertThat(deleted).isZero();
        verify(userRepository, never()).delete(active);
    }

    @Test
    void leaves_inactive_user_inside_grace_window_alone() {
        UserAccount inGrace = TestEntities.withRandomId(
                new UserAccount("a", "x", "A", "a@example.invalid"));
        inGrace.setStatus(UserStatus.INACTIVE);
        inGrace.setDeletionScheduledAt(now.plusSeconds(86_400));
        when(userRepository.findAll()).thenReturn(List.of(inGrace));

        int deleted = job.runOnce();

        assertThat(deleted).isZero();
        verify(userRepository, never()).delete(inGrace);
    }
}
