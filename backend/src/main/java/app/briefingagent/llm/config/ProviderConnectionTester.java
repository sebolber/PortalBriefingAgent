package app.briefingagent.llm.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Lightweight reachability check for provider endpoints. Phase 1 only
 * verifies that the host responds with a non-5xx status; the deep
 * "send a real prompt" test is deferred until provider auth keys are
 * routed through the SecretStore.
 */
public final class ProviderConnectionTester {

    public record Result(boolean success, String message, Duration latency) {
    }

    private ProviderConnectionTester() {
    }

    public static Result probe(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return new Result(false, "Endpoint URL missing", Duration.ZERO);
        }
        URI uri;
        try {
            uri = URI.create(endpointUrl);
        } catch (IllegalArgumentException ex) {
            return new Result(false, "Invalid URL: " + ex.getMessage(), Duration.ZERO);
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        Instant started = Instant.now();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            Duration latency = Duration.between(started, Instant.now());
            int status = response.statusCode();
            if (status >= 500) {
                return new Result(false, "Endpoint returned HTTP " + status, latency);
            }
            return new Result(true, "HTTP " + status, latency);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Result(false, "Interrupted: " + ex.getMessage(),
                    Duration.between(started, Instant.now()));
        } catch (Exception ex) {
            return new Result(false, "Unreachable: " + ex.getMessage(),
                    Duration.between(started, Instant.now()));
        }
    }
}
