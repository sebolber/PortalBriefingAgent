package app.briefingagent.stt.config;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.config.ProviderConnectionTester;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stt-providers")
@Transactional
public class SttProviderController {

    private final SttProviderRepository repository;

    public SttProviderController(SttProviderRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<View> list() {
        return repository.findAll().stream().map(View::from).toList();
    }

    @PostMapping
    public ResponseEntity<View> create(@Valid @RequestBody Request body) {
        SttProvider p = new SttProvider(body.name(), body.endpointUrl(), body.modelName());
        p.setApiKeySecretRef(body.apiKeySecretRef());
        SttProvider saved = repository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(View.from(saved));
    }

    @PatchMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request body) {
        SttProvider p = load(id);
        p.setName(body.name());
        p.setEndpointUrl(body.endpointUrl());
        p.setModelName(body.modelName());
        p.setApiKeySecretRef(body.apiKeySecretRef());
        return View.from(repository.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        SttProvider p = load(id);
        if (p.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "STT provider is active — disable it first");
        }
        repository.delete(p);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activation")
    public View setActivation(@PathVariable UUID id, @Valid @RequestBody ActivationRequest body) {
        SttProvider p = load(id);
        if (body.active()) {
            repository.findFirstByActiveTrue().ifPresent(other -> {
                if (!other.getId().equals(p.getId())) {
                    other.setActive(false);
                    repository.save(other);
                }
            });
        }
        p.setActive(body.active());
        try {
            repository.save(p);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Another STT provider is already active");
        }
        return View.from(p);
    }

    @PostMapping("/{id}/test")
    public TestResult test(@PathVariable UUID id) {
        SttProvider p = load(id);
        ProviderConnectionTester.Result result = ProviderConnectionTester.probe(p.getEndpointUrl());
        p.setLastTestedAt(Instant.now());
        p.setLastTestResult(result.success() ? "success" : "failed");
        p.setLastTestMessage(result.message());
        repository.save(p);
        return new TestResult(result.success(), result.message(), result.latency().toMillis());
    }

    private SttProvider load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STT provider not found"));
    }

    public record Request(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 500) String endpointUrl,
            @NotBlank @Size(max = 200) String modelName,
            @Size(max = 200) String apiKeySecretRef) {
    }

    public record ActivationRequest(boolean active) {
    }

    public record View(String id, String name, String endpointUrl, String modelName,
                       String apiKeySecretRef, boolean active,
                       String lastTestResult, String lastTestMessage, Instant lastTestedAt) {

        public static View from(SttProvider p) {
            return new View(p.getId().toString(), p.getName(), p.getEndpointUrl(), p.getModelName(),
                    p.getApiKeySecretRef(), p.isActive(),
                    p.getLastTestResult(), p.getLastTestMessage(), p.getLastTestedAt());
        }
    }

    public record TestResult(boolean success, String message, long latencyMs) {
    }
}
