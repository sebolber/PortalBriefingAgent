package app.briefingagent.ereignis;

import app.briefingagent.common.ApiException;
import app.briefingagent.security.CurrentAuthor;
import app.briefingagent.summary.Summary;
import app.briefingagent.summary.SummaryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(path = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EreignisResponse> captureAudio(
            @RequestParam("audio") MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio file is required");
        }
        if (!AudioMediaTypes.isAccepted(audio.getContentType())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported audio MIME type: " + audio.getContentType());
        }
        Ereignis ereignis;
        try {
            ereignis = ereignisService.captureAudio(
                    currentAuthor.requireUserId(),
                    audio.getInputStream(),
                    audio.getContentType(),
                    audio.getOriginalFilename() == null ? "audio" : audio.getOriginalFilename(),
                    audio.getSize());
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Audio stream could not be read");
        }
        List<Summary> summaries = summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EreignisResponse.from(ereignis, summaries));
    }

    @GetMapping("/{id}")
    public EreignisResponse get(@PathVariable UUID id) {
        Ereignis ereignis = ereignisService.getOwn(currentAuthor.requireUserId(), id);
        return EreignisResponse.from(ereignis,
                summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis));
    }

    @PatchMapping("/{id}/transcript")
    public EreignisResponse editTranscript(@PathVariable UUID id,
                                           @Valid @RequestBody TranscriptEditRequest body) {
        Ereignis ereignis = ereignisService.editTranscript(
                currentAuthor.requireUserId(), id, body.transcript());
        return EreignisResponse.from(ereignis,
                summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis));
    }

    @PostMapping("/{id}/release")
    public EreignisResponse release(@PathVariable UUID id) {
        Ereignis ereignis = ereignisService.release(currentAuthor.requireUserId(), id);
        return EreignisResponse.from(ereignis,
                summaryRepository.findByEreignisOrderByCreatedAtAsc(ereignis));
    }

    public record TranscriptEditRequest(
            @NotNull @Size(min = 1, max = EreignisLimits.TEXT_HARD_CAP_CHARS) String transcript) {
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
