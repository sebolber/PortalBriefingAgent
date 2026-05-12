package app.briefingagent.summary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.briefingagent.common.ApiException;
import app.briefingagent.common.ApiExceptionHandler;
import app.briefingagent.common.TestEntities;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisSourceType;
import app.briefingagent.security.AuthorPrincipal;
import app.briefingagent.security.AuthorUserDetailsService;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.security.PasswordEncoderConfig;
import app.briefingagent.security.SecurityConfig;
import app.briefingagent.topic.Topic;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
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

@WebMvcTest(controllers = SummaryController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthorUserDetailsService.class,
        ApiExceptionHandler.class, CurrentAuthor.class})
@ActiveProfiles("test")
class SummaryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    SummaryReviewService reviewService;
    @MockBean
    UserAccountRepository userRepository;
    @MockBean
    AuthenticationManager authenticationManager;

    private final UUID demoUserId = UUID.randomUUID();
    private final AuthorPrincipal demo = new AuthorPrincipal(
            TestEntities.withId(new UserAccount("demo", "x", "Demo", "demo@example.invalid"), demoUserId));

    private Summary buildSummary() {
        UserAccount author = TestEntities.withId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"), demoUserId);
        Ereignis ereignis = TestEntities.withRandomId(new Ereignis(author, EreignisSourceType.TEXT));
        Topic topic = TestEntities.withRandomId(new Topic(author, "My Notes", "personal"));
        return TestEntities.withRandomId(Summary.forTopic(ereignis, topic, "## Body"));
    }

    @Test
    void edit_returns_200_with_updated_payload() throws Exception {
        Summary updated = buildSummary();
        updated.setEditState(EditState.MANUALLY_EDITED);
        updated.setSummaryText("## Neu");

        when(reviewService.edit(eq(updated.getId()), eq(demoUserId), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/summaries/" + updated.getId())
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summaryText\":\"## Neu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryText").value("## Neu"))
                .andExpect(jsonPath("$.editState").value("manually_edited"));
    }

    @Test
    void edit_returns_400_for_blank_text() throws Exception {
        mockMvc.perform(patch("/api/summaries/" + UUID.randomUUID())
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summaryText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edit_returns_409_when_summary_is_already_accepted() throws Exception {
        UUID id = UUID.randomUUID();
        when(reviewService.edit(eq(id), eq(demoUserId), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Summary is already accepted"));

        mockMvc.perform(patch("/api/summaries/" + id)
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summaryText\":\"hi\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void regenerate_propagates_optional_feedback() throws Exception {
        Summary updated = buildSummary();
        updated.setEditState(EditState.REGENERATED);
        when(reviewService.regenerate(eq(updated.getId()), eq(demoUserId), eq("kürzer")))
                .thenReturn(updated);

        mockMvc.perform(post("/api/summaries/" + updated.getId() + "/regenerate")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"kürzer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editState").value("regenerated"));
    }

    @Test
    void regenerate_works_without_body() throws Exception {
        Summary updated = buildSummary();
        when(reviewService.regenerate(eq(updated.getId()), eq(demoUserId), eq(null)))
                .thenReturn(updated);

        mockMvc.perform(post("/api/summaries/" + updated.getId() + "/regenerate")
                        .with(user(demo))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void accept_returns_updated_summary() throws Exception {
        Summary updated = buildSummary();
        updated.setAcceptedAt(java.time.Instant.now());
        when(reviewService.accept(updated.getId())).thenReturn(updated);

        mockMvc.perform(post("/api/summaries/" + updated.getId() + "/accept")
                        .with(user(demo))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedAt").exists());
    }

    @Test
    void all_endpoints_require_authentication() throws Exception {
        mockMvc.perform(post("/api/summaries/" + UUID.randomUUID() + "/accept").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
