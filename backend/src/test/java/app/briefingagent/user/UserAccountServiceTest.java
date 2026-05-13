package app.briefingagent.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.prompt.DefaultPromptContent;
import app.briefingagent.prompt.PromptTemplateService;
import app.briefingagent.topic.DefaultTopicProvider;
import app.briefingagent.topic.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    UserAccountRepository repository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    DefaultTopicProvider defaultTopicProvider;
    @Mock
    PromptTemplateService promptTemplateService;

    UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(repository, passwordEncoder,
                defaultTopicProvider, promptTemplateService);
    }

    @Test
    void create_local_author_seeds_one_prompt_per_purpose() {
        when(repository.existsByUsername("demo")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(repository.save(any(UserAccount.class)))
                .thenAnswer(inv -> TestEntities.withRandomId(inv.getArgument(0)));
        when(defaultTopicProvider.ensureDefaultTopic(any()))
                .thenAnswer(inv -> {
                    UserAccount author = inv.getArgument(0);
                    return TestEntities.withRandomId(new Topic(author, "My Notes", "personal"));
                });

        UserAccount saved = service.createLocalAuthor("demo", "pw", "Demo", "demo@example.invalid");

        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        for (LlmPurpose purpose : LlmPurpose.values()) {
            verify(promptTemplateService, times(1))
                    .saveNewVersion(eq(saved.getId()), eq(purpose),
                            eq(DefaultPromptContent.BY_PURPOSE.get(purpose)));
        }
    }

    @Test
    void create_local_author_rejects_duplicate_username() {
        when(repository.existsByUsername("demo")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.createLocalAuthor("demo", "pw", "Demo", "demo@example.invalid"))
                .isInstanceOf(app.briefingagent.common.ApiException.class);

        verify(promptTemplateService, times(0)).saveNewVersion(any(), any(), anyString());
    }
}
