package app.briefingagent.dashboard;

import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisLimits;
import app.briefingagent.ereignis.EreignisService;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final EreignisService ereignisService;
    private final SummaryRepository summaryRepository;
    private final CurrentAuthor currentAuthor;

    public DashboardController(EreignisService ereignisService,
                               SummaryRepository summaryRepository,
                               CurrentAuthor currentAuthor) {
        this.ereignisService = ereignisService;
        this.summaryRepository = summaryRepository;
        this.currentAuthor = currentAuthor;
    }

    @GetMapping("/recent")
    public ResponseEntity<RecentResponse> recent() {
        List<Ereignis> recent = ereignisService.recent(
                currentAuthor.requireUserId(), EreignisLimits.RECENT_DASHBOARD_DAYS);
        List<RecentEntry> entries = recent.stream()
                .map(e -> new RecentEntry(
                        e.getId().toString(),
                        e.getCreatedAt().toString(),
                        e.getSourceType().dbValue(),
                        excerpt(e.getTranscriptText()),
                        summaryRepository.findByEreignisOrderByCreatedAtAsc(e).stream()
                                .map(s -> new SummaryEntry(
                                        s.getId().toString(),
                                        s.getAudienceType().dbValue(),
                                        audienceName(s),
                                        excerpt(s.getSummaryText())))
                                .toList()))
                .toList();
        return ResponseEntity.ok(new RecentResponse(EreignisLimits.RECENT_DASHBOARD_DAYS, entries));
    }

    private static String audienceName(Summary s) {
        return switch (s.getAudienceType()) {
            case TOPIC -> s.getAudienceTopic().getName();
            case PERSON -> s.getAudiencePerson().getDisplayName();
            case PERSONGROUP -> s.getAudiencePersonGroup().getName();
        };
    }

    private static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.strip();
        return trimmed.length() <= 240 ? trimmed : trimmed.substring(0, 240) + "…";
    }

    public record RecentResponse(int windowDays, List<RecentEntry> ereignisse) {
    }

    public record RecentEntry(
            String id,
            String createdAt,
            String sourceType,
            String transcriptExcerpt,
            List<SummaryEntry> summaries) {
    }

    public record SummaryEntry(
            String id,
            String audienceType,
            String audienceName,
            String summaryExcerpt) {
    }
}
