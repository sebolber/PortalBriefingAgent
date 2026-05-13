package app.briefingagent.retention;

import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import app.briefingagent.user.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cascade-deletes inactive users whose grace window expired. The
 * cascade follows JPA fetch boundaries; tasks, summaries and ereignisse
 * tied to the author are removed by the schema's ON DELETE rules and
 * application-level deletes inside this job.
 */
@Component
public class AuthorDeletionJob {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorDeletionJob.class);

    private final UserAccountRepository userRepository;
    private final Clock clock;

    public AuthorDeletionJob(UserAccountRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public int runOnce() {
        Instant now = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("UTC")).toInstant();
        List<UserAccount> all = userRepository.findAll();
        int deleted = 0;
        for (UserAccount user : all) {
            if (user.getStatus() == UserStatus.INACTIVE
                    && user.getDeletionScheduledAt() != null
                    && !user.getDeletionScheduledAt().isAfter(now)) {
                userRepository.delete(user);
                deleted++;
            }
        }
        LOG.debug("AuthorDeletionJob deleted {} accounts past their grace window.", deleted);
        return deleted;
    }
}
