package app.briefingagent.person;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.briefingagent.common.ApiExceptionHandler;
import app.briefingagent.common.TestEntities;
import app.briefingagent.security.AuthorPrincipal;
import app.briefingagent.security.AuthorUserDetailsService;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.security.PasswordEncoderConfig;
import app.briefingagent.security.SecurityConfig;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PersonController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthorUserDetailsService.class,
        ApiExceptionHandler.class, CurrentAuthor.class})
@ActiveProfiles("test")
class PersonControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PersonRepository personRepository;
    @MockBean
    UserAccountRepository userRepository;
    @MockBean
    AuthenticationManager authenticationManager;

    private final UUID demoUserId = UUID.randomUUID();
    private final AuthorPrincipal demo = new AuthorPrincipal(
            TestEntities.withId(new UserAccount("demo", "x", "Demo", "demo@example.invalid"), demoUserId));

    @Test
    void list_returns_persons_sorted_by_name() throws Exception {
        Person a = TestEntities.withRandomId(new Person("Anna", PersonSource.MANUAL));
        Person b = TestEntities.withRandomId(new Person("Bernd", PersonSource.MANUAL));
        when(personRepository.findAll(any(Sort.class))).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/persons").with(user(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Anna"))
                .andExpect(jsonPath("$[1].fullName").value("Bernd"));
    }

    @Test
    void create_returns_201_and_persists_manual_source() throws Exception {
        when(personRepository.save(any(Person.class)))
                .thenAnswer(inv -> TestEntities.withRandomId(inv.getArgument(0)));

        mockMvc.perform(post("/api/persons")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Anna Müller\",\"email\":\"anna@example.invalid\","
                                + "\"role\":\"VP Sales\",\"company\":\"Beispiel-Kunde GmbH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Anna Müller"))
                .andExpect(jsonPath("$.email").value("anna@example.invalid"))
                .andExpect(jsonPath("$.tombstoned").value(false));
    }

    @Test
    void create_returns_400_when_full_name_blank() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns_409_for_tombstoned_person() throws Exception {
        Person p = TestEntities.withRandomId(new Person("Phantom", PersonSource.MANUAL));
        p.setDeletedAt(java.time.Instant.now());
        p.setPseudonym("Gelöschte Person #7");
        when(personRepository.findById(p.getId())).thenReturn(Optional.of(p));

        mockMvc.perform(patch("/api/persons/" + p.getId())
                        .with(user(demo))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"new name\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_returns_204() throws Exception {
        Person p = TestEntities.withRandomId(new Person("Anna", PersonSource.MANUAL));
        when(personRepository.findById(p.getId())).thenReturn(Optional.of(p));

        mockMvc.perform(delete("/api/persons/" + p.getId())
                        .with(user(demo))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void list_returns_401_when_anonymous() throws Exception {
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isUnauthorized());
    }
}
