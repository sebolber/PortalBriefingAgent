package app.briefingagent.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentAuthor {

    public Optional<UUID> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof AuthorPrincipal principal) {
            return Optional.of(principal.userId());
        }
        return Optional.empty();
    }

    public UUID requireUserId() {
        return currentUserId().orElseThrow(() ->
                new IllegalStateException("No authenticated author in security context"));
    }
}
