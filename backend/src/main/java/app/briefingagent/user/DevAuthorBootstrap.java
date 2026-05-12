package app.briefingagent.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates a development author on first start so the walking skeleton is
 * usable without a manual sign-up flow. Only active in the {@code dev}
 * profile; production deployments must seed accounts deliberately.
 */
@Component
@Profile("dev")
public class DevAuthorBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(DevAuthorBootstrap.class);
    private static final String DEFAULT_USERNAME = "demo";
    private static final String DEFAULT_PASSWORD = "demo-password-change-me";
    private static final String DEFAULT_FULL_NAME = "Demo Author";
    private static final String DEFAULT_EMAIL = "demo@briefing-agent.local";

    private final UserAccountRepository repository;
    private final UserAccountService userAccountService;

    public DevAuthorBootstrap(UserAccountRepository repository, UserAccountService userAccountService) {
        this.repository = repository;
        this.userAccountService = userAccountService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedAuthorIfMissing() {
        if (repository.findByUsername(DEFAULT_USERNAME).isPresent()) {
            return;
        }
        userAccountService.createLocalAuthor(DEFAULT_USERNAME, DEFAULT_PASSWORD,
                DEFAULT_FULL_NAME, DEFAULT_EMAIL);
        LOG.info("Bootstrapped dev author '{}' (change password before any non-local use).",
                DEFAULT_USERNAME);
    }
}
