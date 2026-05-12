package app.briefingagent.prompt;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.security.CurrentAuthor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prompt-templates")
public class PromptTemplateController {

    private final PromptTemplateService service;
    private final CurrentAuthor currentAuthor;

    public PromptTemplateController(PromptTemplateService service, CurrentAuthor currentAuthor) {
        this.service = service;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping
    public List<View> list() {
        return service.listFor(currentAuthor.requireUserId()).stream().map(View::from).toList();
    }

    @PostMapping
    public View save(@Valid @RequestBody Request body) {
        LlmPurpose purpose = parsePurpose(body.purpose());
        return View.from(service.saveNewVersion(
                currentAuthor.requireUserId(), purpose, body.content()));
    }

    @PostMapping("/{id}/restore")
    public View restore(@PathVariable UUID id) {
        return View.from(service.restoreVersion(currentAuthor.requireUserId(), id));
    }

    @GetMapping("/placeholders/{purpose}")
    public PlaceholderInfo placeholders(@PathVariable String purpose) {
        return new PlaceholderInfo(purpose, PromptPlaceholders.requiredFor(parsePurpose(purpose)));
    }

    private LlmPurpose parsePurpose(String value) {
        try {
            return LlmPurpose.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown purpose: " + value);
        }
    }

    public record Request(@NotBlank String purpose,
                          @NotNull @Size(min = 1, max = 50_000) String content) {
    }

    public record View(String id, String purpose, String content, int version,
                       boolean active, Instant createdAt) {

        public static View from(PromptTemplate t) {
            return new View(
                    t.getId().toString(),
                    t.getPurpose().dbValue(),
                    t.getContent(),
                    t.getVersion(),
                    t.isActive(),
                    t.getCreatedAt());
        }
    }

    public record PlaceholderInfo(String purpose, Set<String> required) {
    }
}
