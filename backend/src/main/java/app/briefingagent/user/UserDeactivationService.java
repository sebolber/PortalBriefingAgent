package app.briefingagent.user;

import app.briefingagent.common.ApiException;
import app.briefingagent.retention.RetentionProperties;
import app.briefingagent.task.Task;
import app.briefingagent.task.TaskRepository;
import app.briefingagent.task.TaskStatus;
import app.briefingagent.task.TaskStatusHistory;
import app.briefingagent.task.TaskStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deactivates an author per spec §8.4: status flips to inactive, the
 * deletion grace window starts, and any open self-assigned tasks are
 * dropped with an audit-trail entry. Reactivation is supported as long
 * as the grace window has not yet expired.
 */
@Service
public class UserDeactivationService {

    public static final String DEACTIVATION_NOTE = "User deaktiviert";

    private final UserAccountRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskStatusHistoryRepository historyRepository;
    private final RetentionProperties properties;
    private final Clock clock;

    public UserDeactivationService(UserAccountRepository userRepository,
                                   TaskRepository taskRepository,
                                   TaskStatusHistoryRepository historyRepository,
                                   RetentionProperties properties,
                                   Clock clock) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public UserAccount deactivate(UUID targetUserId, UUID actingAdminId) {
        UserAccount target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        UserAccount admin = userRepository.findById(actingAdminId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Admin not found"));
        if (target.getStatus() == UserStatus.INACTIVE) {
            return target;
        }

        ZonedDateTime nowZ = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC"));
        Instant now = nowZ.toInstant();
        Instant deletion = nowZ.plus(Period.ofMonths(properties.getAuthorDeletionMonths())).toInstant();
        target.setStatus(UserStatus.INACTIVE);
        target.setDeactivatedAt(now);
        target.setDeletionScheduledAt(deletion);
        userRepository.save(target);

        for (Task t : taskRepository.findByAuthorAndStatusOrderByCreatedAtDesc(target, TaskStatus.OPEN)) {
            if (t.isAssignedToSelf()) {
                t.setStatus(TaskStatus.DROPPED);
                t.setDroppedAt(now);
                taskRepository.save(t);
                historyRepository.save(new TaskStatusHistory(
                        t, TaskStatus.OPEN, TaskStatus.DROPPED, DEACTIVATION_NOTE, admin));
            }
        }
        for (Task t : taskRepository.findByAuthorAndStatusOrderByCreatedAtDesc(target, TaskStatus.IN_PROGRESS)) {
            if (t.isAssignedToSelf()) {
                t.setStatus(TaskStatus.DROPPED);
                t.setDroppedAt(now);
                taskRepository.save(t);
                historyRepository.save(new TaskStatusHistory(
                        t, TaskStatus.IN_PROGRESS, TaskStatus.DROPPED, DEACTIVATION_NOTE, admin));
            }
        }

        return target;
    }

    @Transactional
    public UserAccount reactivate(UUID targetUserId) {
        UserAccount target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (target.getStatus() == UserStatus.ACTIVE) {
            return target;
        }
        Instant now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC")).toInstant();
        if (target.getDeletionScheduledAt() != null
                && target.getDeletionScheduledAt().isBefore(now)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Reactivation grace window has expired");
        }
        target.setStatus(UserStatus.ACTIVE);
        target.setDeactivatedAt(null);
        target.setDeletionScheduledAt(null);
        return userRepository.save(target);
    }
}
