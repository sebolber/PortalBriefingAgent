package app.briefingagent.llm.config;

import app.briefingagent.common.ApiException;
import app.briefingagent.llm.LlmPurpose;
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
@RequestMapping("/api/llm-providers")
@Transactional
public class LlmProviderController {

    private final LlmProviderRepository providerRepository;
    private final LlmProviderUsageRepository usageRepository;

    public LlmProviderController(LlmProviderRepository providerRepository,
                                 LlmProviderUsageRepository usageRepository) {
        this.providerRepository = providerRepository;
        this.usageRepository = usageRepository;
    }

    @GetMapping
    public List<View> list() {
        return providerRepository.findAll().stream()
                .map(p -> View.from(p, usageRepository.findByProvider(p)))
                .toList();
    }

    @PostMapping
    public ResponseEntity<View> create(@Valid @RequestBody Request body) {
        LlmProvider p = new LlmProvider(body.name(), body.endpointUrl(), body.modelName());
        p.setApiKeySecretRef(body.apiKeySecretRef());
        p.setApiType(body.apiType() == null ? "openai_compatible" : body.apiType());
        LlmProvider saved = providerRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(View.from(saved, List.of()));
    }

    @PatchMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Request body) {
        LlmProvider p = load(id);
        p.setName(body.name());
        p.setEndpointUrl(body.endpointUrl());
        p.setModelName(body.modelName());
        p.setApiKeySecretRef(body.apiKeySecretRef());
        if (body.apiType() != null) {
            p.setApiType(body.apiType());
        }
        return View.from(providerRepository.save(p), usageRepository.findByProvider(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        LlmProvider p = load(id);
        boolean stillActive = usageRepository.findByProvider(p).stream()
                .anyMatch(LlmProviderUsage::isActive);
        if (stillActive) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Provider has active usages — disable them first");
        }
        providerRepository.delete(p);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/usages")
    public View setUsage(@PathVariable UUID id, @Valid @RequestBody UsageRequest body) {
        LlmProvider p = load(id);
        LlmPurpose purpose = parsePurpose(body.purpose());
        LlmProviderUsage usage = usageRepository.findByProviderAndPurpose(p, purpose)
                .orElseGet(() -> new LlmProviderUsage(p, purpose, false));
        if (body.active()) {
            usageRepository.findByPurposeAndActiveTrue(purpose).ifPresent(other -> {
                if (!other.getProvider().getId().equals(p.getId())) {
                    other.setActive(false);
                    usageRepository.save(other);
                }
            });
        }
        usage.setActive(body.active());
        try {
            usageRepository.save(usage);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Another provider is already active for purpose " + body.purpose());
        }
        return View.from(p, usageRepository.findByProvider(p));
    }

    @PostMapping("/{id}/test")
    public TestResult test(@PathVariable UUID id) {
        LlmProvider p = load(id);
        ProviderConnectionTester.Result result = ProviderConnectionTester.probe(p.getEndpointUrl());
        p.setLastTestedAt(Instant.now());
        p.setLastTestResult(result.success() ? "success" : "failed");
        p.setLastTestMessage(result.message());
        providerRepository.save(p);
        return new TestResult(result.success(), result.message(), result.latency().toMillis());
    }

    private LlmProvider load(UUID id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Provider not found"));
    }

    private LlmPurpose parsePurpose(String value) {
        try {
            return LlmPurpose.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown purpose: " + value);
        }
    }

    public record Request(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 500) String endpointUrl,
            @NotBlank @Size(max = 200) String modelName,
            @Size(max = 200) String apiKeySecretRef,
            @Size(max = 50) String apiType) {
    }

    public record UsageRequest(@NotBlank String purpose, boolean active) {
    }

    public record View(
            String id, String name, String endpointUrl, String modelName,
            String apiKeySecretRef, String apiType,
            String lastTestResult, String lastTestMessage, Instant lastTestedAt,
            List<UsageView> usages) {

        public static View from(LlmProvider p, List<LlmProviderUsage> usages) {
            return new View(p.getId().toString(), p.getName(), p.getEndpointUrl(), p.getModelName(),
                    p.getApiKeySecretRef(), p.getApiType(),
                    p.getLastTestResult(), p.getLastTestMessage(), p.getLastTestedAt(),
                    usages.stream().map(UsageView::from).toList());
        }
    }

    public record UsageView(String purpose, boolean active) {
        static UsageView from(LlmProviderUsage u) {
            return new UsageView(u.getPurpose().dbValue(), u.isActive());
        }
    }

    public record TestResult(boolean success, String message, long latencyMs) {
    }
}
