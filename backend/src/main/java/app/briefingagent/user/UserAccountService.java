package app.briefingagent.user;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.prompt.DefaultPromptContent;
import app.briefingagent.prompt.PromptTemplateService;
import app.briefingagent.topic.DefaultTopicProvider;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultTopicProvider defaultTopicProvider;
    private final PromptTemplateService promptTemplateService;

    public UserAccountService(UserAccountRepository repository,
                              PasswordEncoder passwordEncoder,
                              DefaultTopicProvider defaultTopicProvider,
                              PromptTemplateService promptTemplateService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.defaultTopicProvider = defaultTopicProvider;
        this.promptTemplateService = promptTemplateService;
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
        seedDefaultPrompts(saved.getId());
        return saved;
    }

    private void seedDefaultPrompts(UUID authorId) {
        for (LlmPurpose purpose : LlmPurpose.values()) {
            String content = DefaultPromptContent.BY_PURPOSE.get(purpose);
            if (content != null) {
                promptTemplateService.saveNewVersion(authorId, purpose, content);
            }
        }
    }
}
