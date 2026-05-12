package app.briefingagent.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.briefingagent.common.ApiExceptionHandler;
import app.briefingagent.common.TestEntities;
import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, CsrfController.class})
@Import({SecurityConfig.class, PasswordEncoderConfig.class, AuthorUserDetailsService.class,
        ApiExceptionHandler.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccountRepository userRepository;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void login_returns_200_with_user_payload_for_valid_credentials() throws Exception {
        UserAccount user = TestEntities.withRandomId(
                new UserAccount("demo", "irrelevant", "Demo", "demo@example.invalid"));
        Authentication authn = UsernamePasswordAuthenticationToken.authenticated(
                new AuthorPrincipal(user), null,
                List.of(new SimpleGrantedAuthority(AuthorPrincipal.ROLE_AUTHOR)));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authn);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"demo-password-change-me\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.fullName").value("Demo"));
    }

    @Test
    void login_returns_401_for_bad_credentials() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("nope"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns_400_for_missing_password() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_returns_401_when_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clears_session() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void csrf_endpoint_is_publicly_reachable() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isNoContent());
    }
}
