package app.briefingagent.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint whose only job is to make Spring Security materialise the CSRF
 * token so the cookie repository writes the {@code XSRF-TOKEN} cookie. The
 * Angular HTTP client then auto-attaches the value as the
 * {@code X-XSRF-TOKEN} header on subsequent mutating requests.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        // The CsrfToken parameter is required: resolving it as a controller
        // argument is what triggers the token to be written to the response
        // cookie.
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
