package app.briefingagent.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.TestEntities;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    PromptTemplateRepository repository;
    @Mock
    UserAccountRepository userRepository;

    PromptTemplateService service;

    private UserAccount author;
    private UUID authorId;
    private final String validTaskPrompt = "{{transcript}} und {{author_name}}";

    @BeforeEach
    void setUp() {
        service = new PromptTemplateService(repository, userRepository);
        author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
        authorId = author.getId();
    }

    @Test
    void save_new_version_starts_at_1_when_no_history() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(repository.findByAuthorAndPurposeOrderByVersionDesc(author, LlmPurpose.TASK_EXTRACTION))
                .thenReturn(List.of());
        when(repository.save(any(PromptTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        PromptTemplate saved = service.saveNewVersion(author.getId(), LlmPurpose.TASK_EXTRACTION, validTaskPrompt);

        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void save_new_version_increments_and_deactivates_previous() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        PromptTemplate v1 = TestEntities.withRandomId(
                new PromptTemplate(author, LlmPurpose.TASK_EXTRACTION, validTaskPrompt, 1, true, author));
        when(repository.findByAuthorAndPurposeOrderByVersionDesc(author, LlmPurpose.TASK_EXTRACTION))
                .thenReturn(List.of(v1));
        when(repository.save(any(PromptTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        PromptTemplate v2 = service.saveNewVersion(author.getId(), LlmPurpose.TASK_EXTRACTION, validTaskPrompt + " v2");

        assertThat(v2.getVersion()).isEqualTo(2);
        assertThat(v1.isActive()).isFalse();
        verify(repository).saveAll(List.of(v1));
    }

    @Test
    void save_rejects_template_missing_required_placeholders() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.saveNewVersion(authorId, LlmPurpose.TASK_EXTRACTION, "no placeholders"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(repository, never()).save(any());
    }

    @Test
    void restore_version_re_activates_target_and_deactivates_current() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        PromptTemplate v1 = TestEntities.withRandomId(
                new PromptTemplate(author, LlmPurpose.TASK_EXTRACTION, validTaskPrompt, 1, false, author));
        PromptTemplate v2 = TestEntities.withRandomId(
                new PromptTemplate(author, LlmPurpose.TASK_EXTRACTION, validTaskPrompt + " v2", 2, true, author));
        when(repository.findById(v1.getId())).thenReturn(Optional.of(v1));
        when(repository.findByAuthorAndPurposeOrderByVersionDesc(author, LlmPurpose.TASK_EXTRACTION))
                .thenReturn(List.of(v2, v1));
        when(repository.save(any(PromptTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.restoreVersion(author.getId(), v1.getId());

        assertThat(v1.isActive()).isTrue();
        assertThat(v2.isActive()).isFalse();
    }

    @Test
    void restore_unknown_template_returns_404() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(repository.findById(any())).thenReturn(Optional.empty());
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> service.restoreVersion(authorId, randomId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void restore_someone_elses_template_returns_404() {
        UserAccount stranger = TestEntities.withRandomId(
                new UserAccount("other", "x", "Other", "other@example.invalid"));
        PromptTemplate t = TestEntities.withRandomId(
                new PromptTemplate(stranger, LlmPurpose.TASK_EXTRACTION, validTaskPrompt, 1, false, stranger));
        UUID templateId = t.getId();
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(repository.findById(templateId)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.restoreVersion(authorId, templateId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void active_for_returns_404_when_no_active_template_exists() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(repository.findByAuthorAndPurposeAndActiveTrue(author, LlmPurpose.TASK_EXTRACTION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activeFor(authorId, LlmPurpose.TASK_EXTRACTION))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void save_new_version_writes_audit_owner_to_created_by() {
        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
        when(repository.findByAuthorAndPurposeOrderByVersionDesc(author, LlmPurpose.TASK_EXTRACTION))
                .thenReturn(List.of());
        when(repository.save(any(PromptTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.saveNewVersion(author.getId(), LlmPurpose.TASK_EXTRACTION, validTaskPrompt);

        ArgumentCaptor<PromptTemplate> captor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCreatedByAuthor()).isSameAs(author);
    }
}
