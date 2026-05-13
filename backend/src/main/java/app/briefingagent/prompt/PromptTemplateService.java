package app.briefingagent.prompt;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Author-scoped prompt templates with monotonic versioning. Saving a
 * new version flips {@code active} on the previous one in the same
 * transaction; the partial unique index in the schema enforces the
 * "one active per (author, purpose)" rule even under concurrent saves.
 */
@Service
public class PromptTemplateService {

    private final PromptTemplateRepository repository;
    private final UserAccountRepository userRepository;

    public PromptTemplateService(PromptTemplateRepository repository,
                                 UserAccountRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PromptTemplate> listFor(UUID authorId) {
        UserAccount author = loadAuthor(authorId);
        return repository.findByAuthorOrderByPurposeAscVersionDesc(author);
    }

    @Transactional(readOnly = true)
    public List<PromptTemplate> versionsOf(UUID authorId, LlmPurpose purpose) {
        UserAccount author = loadAuthor(authorId);
        return repository.findByAuthorAndPurposeOrderByVersionDesc(author, purpose);
    }

    @Transactional
    public PromptTemplate saveNewVersion(UUID authorId, LlmPurpose purpose, String content) {
        UserAccount author = loadAuthor(authorId);
        rejectMissingPlaceholders(purpose, content);
        List<PromptTemplate> existing =
                repository.findByAuthorAndPurposeOrderByVersionDesc(author, purpose);
        int nextVersion = existing.stream().mapToInt(PromptTemplate::getVersion).max().orElse(0) + 1;
        existing.stream().filter(PromptTemplate::isActive).forEach(t -> t.setActive(false));
        repository.saveAll(existing);
        PromptTemplate t = new PromptTemplate(author, purpose, content, nextVersion, true, author);
        return repository.save(t);
    }

    @Transactional
    public PromptTemplate restoreVersion(UUID authorId, UUID templateId) {
        UserAccount author = loadAuthor(authorId);
        PromptTemplate target = repository.findById(templateId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        if (!target.getAuthor().getId().equals(author.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Template not found");
        }
        if (target.isActive()) {
            return target;
        }
        repository.findByAuthorAndPurposeOrderByVersionDesc(author, target.getPurpose())
                .stream()
                .filter(PromptTemplate::isActive)
                .forEach(t -> t.setActive(false));
        target.setActive(true);
        return repository.save(target);
    }

    @Transactional(readOnly = true)
    public PromptTemplate activeFor(UUID authorId, LlmPurpose purpose) {
        UserAccount author = loadAuthor(authorId);
        return repository.findByAuthorAndPurposeAndActiveTrue(author, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No active prompt for purpose " + purpose));
    }

    private void rejectMissingPlaceholders(LlmPurpose purpose, String content) {
        List<String> missing = PromptPlaceholders.missingFrom(purpose, content);
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Prompt is missing required placeholders: " + String.join(", ", missing));
        }
    }

    private UserAccount loadAuthor(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Author not found"));
    }
}
