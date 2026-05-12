package app.briefingagent.ereignis;

import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ereignisse")
public class EreignisController {

    private final EreignisService ereignisService;
    private final SummaryRepository summaryRepository;
    private final CurrentAuthor currentAuthor;

    public EreignisController(EreignisService ereignisService,
                              SummaryRepository summaryRepository,
                              CurrentAuthor currentAuthor) {
        this.ereignisService = ereignisService;
        this.summaryRepository = summaryRepository;
        this.currentAuthor = currentAuthor;
    }

    @PostMapping
    public ResponseEntity<EreignisResponse> capture(@Valid @RequestBody CaptureTextRequest body) {
        Ereignis ereignis = ereignisService.captureText(currentAuthor.requireUserId(), body.text());
        List<Summary> summaries = summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EreignisResponse.from(ereignis, summaries));
    }

    public record CaptureTextRequest(
            @NotNull @Size(min = 1, max = EreignisLimits.TEXT_HARD_CAP_CHARS) String text) {
    }

    public record EreignisResponse(
            String id,
            String sourceType,
            String reviewStatus,
            String transcript,
            List<SummaryView> summaries) {

        public static EreignisResponse from(Ereignis ereignis, List<Summary> summaries) {
            return new EreignisResponse(
                    ereignis.getId().toString(),
                    ereignis.getSourceType().dbValue(),
                    ereignis.getReviewStatus().dbValue(),
                    ereignis.getTranscriptText(),
                    summaries.stream().map(SummaryView::from).toList());
        }
    }

    public record SummaryView(
            String id,
            String audienceType,
            String audienceName,
            String summaryText) {

        public static SummaryView from(Summary s) {
            String audienceName = switch (s.getAudienceType()) {
                case TOPIC -> s.getAudienceTopic().getName();
                case PERSON -> s.getAudiencePerson().getDisplayName();
                case PERSONGROUP -> s.getAudiencePersonGroup().getName();
            };
            return new SummaryView(
                    s.getId().toString(),
                    s.getAudienceType().dbValue(),
                    audienceName,
                    s.getSummaryText());
        }
    }
}
