package app.briefingagent.ereignis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.ApiExceptionHandler;
import app.briefingagent.common.TestEntities;
import app.briefingagent.security.AuthorPrincipal;
import app.briefingagent.security.AuthorUserDetailsService;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.security.PasswordEncoderConfig;
import app.briefingagent.security.SecurityConfig;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EreignisController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthorUserDetailsService.class,
        ApiExceptionHandler.class, CurrentAuthor.class})
@ActiveProfiles("test")
class EreignisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EreignisService ereignisService;

    @MockBean
    private SummaryRepository summaryRepository;

    @MockBean
    private UserAccountRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    private final UUID demoUserId = UUID.randomUUID();
    private final AuthorPrincipal demo = new AuthorPrincipal(
            TestEntities.withId(new UserAccount("demo", "irrelevant", "Demo", "demo@example.invalid"), demoUserId));

    @Test
    void post_text_creates_ereignis_and_returns_201_with_summary() throws Exception {
        UserAccount author = TestEntities.withId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"), demoUserId);
        Topic topic = TestEntities.withRandomId(new Topic(author, "My Notes", "persona"));
        Ereignis ereignis = new Ereignis(author, EreignisSourceType.TEXT);
        ereignis.setTranscriptText("text body");
        ereignis.setTranscriptSource(TranscriptSource.MANUAL);
        ereignis.setCharacterCount(9);
        TestEntities.withRandomId(ereignis);

        Summary summary = TestEntities.withRandomId(Summary.forTopic(ereignis, topic, "## Summary\n\nbody"));

        when(ereignisService.captureText(eq(demoUserId), any())).thenReturn(ereignis);
        when(summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis)).thenReturn(List.of(summary));

        mockMvc.perform(post("/api/ereignisse")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"text body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("text"))
                .andExpect(jsonPath("$.summaries[0].audienceName").value("My Notes"))
                .andExpect(jsonPath("$.summaries[0].summaryText").value("## Summary\n\nbody"));
    }

    @Test
    void post_text_returns_400_when_text_missing() throws Exception {
        mockMvc.perform(post("/api/ereignisse")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_text_returns_400_when_text_too_long() throws Exception {
        String tooLong = "a".repeat(EreignisLimits.TEXT_HARD_CAP_CHARS + 1);
        String body = "{\"text\":\"" + tooLong + "\"}";

        mockMvc.perform(post("/api/ereignisse")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_text_returns_401_when_anonymous() throws Exception {
        mockMvc.perform(post("/api/ereignisse")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_text_propagates_service_validation_error() throws Exception {
        when(ereignisService.captureText(eq(demoUserId), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Text must not be empty"));

        mockMvc.perform(post("/api/ereignisse")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Text must not be empty"));
    }
}
