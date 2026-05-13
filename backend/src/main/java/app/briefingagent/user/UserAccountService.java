package app.briefingagent.user;

import app.briefingagent.common.ApiException;
import app.briefingagent.topic.DefaultTopicProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultTopicProvider defaultTopicProvider;

    public UserAccountService(UserAccountRepository repository,
                              PasswordEncoder passwordEncoder,
                              DefaultTopicProvider defaultTopicProvider) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.defaultTopicProvider = defaultTopicProvider;
    }

    @Transactional
    public UserAccount createLocalAuthor(String username, String rawPassword,
                                         String fullName, String email) {
        if (repository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        UserAccount user = new UserAccount(username, passwordEncoder.encode(rawPassword), fullName, email);
        UserAccount saved = repository.save(user);
        defaultTopicProvider.ensureDefaultTopic(saved);
        return saved;
    }
}
