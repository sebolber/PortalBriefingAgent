package app.briefingagent.dashboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.briefingagent.common.ApiExceptionHandler;
import app.briefingagent.common.TestEntities;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisService;
import app.briefingagent.ereignis.EreignisSourceType;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthorUserDetailsService.class,
        ApiExceptionHandler.class, CurrentAuthor.class})
@ActiveProfiles("test")
class DashboardControllerTest {

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
            TestEntities.withId(new UserAccount("demo", "x", "Demo", "demo@example.invalid"), demoUserId));

    @Test
    void recent_returns_empty_list_when_nothing_captured() throws Exception {
        when(ereignisService.recent(eq(demoUserId), any(Integer.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/recent").with(user(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ereignisse").isArray())
                .andExpect(jsonPath("$.ereignisse.length()").value(0));
    }

    @Test
    void recent_returns_excerpt_and_summaries() throws Exception {
        UserAccount author = TestEntities.withId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"), demoUserId);
        Topic topic = TestEntities.withRandomId(new Topic(author, "My Notes", "persona"));
        Ereignis ereignis = TestEntities.withRandomId(new Ereignis(author, EreignisSourceType.TEXT));
        ereignis.setTranscriptText("Heute Workshop mit Anna besprochen über Cybersecurity");
        Summary summary = TestEntities.withRandomId(
                Summary.forTopic(ereignis, topic, "## Summary\n\nbody"));

        when(ereignisService.recent(eq(demoUserId), any(Integer.class))).thenReturn(List.of(ereignis));
        when(summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/dashboard/recent").with(user(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ereignisse[0].sourceType").value("text"))
                .andExpect(jsonPath("$.ereignisse[0].transcriptExcerpt").value("Heute Workshop mit Anna besprochen über Cybersecurity"))
                .andExpect(jsonPath("$.ereignisse[0].summaries[0].audienceName").value("My Notes"));
    }

    @Test
    void recent_returns_401_when_anonymous() throws Exception {
        mockMvc.perform(get("/api/dashboard/recent"))
                .andExpect(status().isUnauthorized());
    }
}
