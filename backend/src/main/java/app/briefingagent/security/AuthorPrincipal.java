package app.briefingagent.security;

import app.briefingagent.user.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthorPrincipal implements UserDetails {

    public static final String ROLE_AUTHOR = "ROLE_AUTHOR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public AuthorPrincipal(UserAccount user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.isActive();
        if (user.isAdmin()) {
            this.authorities = List.of(
                    new SimpleGrantedAuthority(ROLE_AUTHOR),
                    new SimpleGrantedAuthority(ROLE_ADMIN));
        } else {
            this.authorities = List.of(new SimpleGrantedAuthority(ROLE_AUTHOR));
        }
    }

    public UUID userId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Phase 1 conflates the four UserDetails lifecycle flags into a single
    // active/inactive switch: an inactive account is treated as locked,
    // expired and credential-expired all at once. The Spring Security
    // contract demands the four separate methods, so the delegates stay.
    @Override
    @SuppressWarnings("java:S4144")
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    @SuppressWarnings("java:S4144")
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    @SuppressWarnings("java:S4144")
    public boolean isCredentialsNonExpired() {
        return enabled;
    }
}
