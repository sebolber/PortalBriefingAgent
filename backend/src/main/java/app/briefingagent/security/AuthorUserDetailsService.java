package app.briefingagent.security;

import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    public AuthorUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        if (user.getPasswordHash() == null) {
            throw new UsernameNotFoundException("Account has no local password");
        }
        return new AuthorPrincipal(user);
    }
}
