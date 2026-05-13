package app.briefingagent.summary;

import app.briefingagent.security.CurrentAuthor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {

    private final SummaryReviewService reviewService;
    private final CurrentAuthor currentAuthor;

    public SummaryController(SummaryReviewService reviewService, CurrentAuthor currentAuthor) {
        this.reviewService = reviewService;
        this.currentAuthor = currentAuthor;
    }

    @PatchMapping("/{id}")
    public SummaryView edit(@PathVariable UUID id, @Valid @RequestBody EditRequest body) {
        Summary saved = reviewService.edit(id, currentAuthor.requireUserId(), body.summaryText());
        return SummaryView.from(saved);
    }

    @PostMapping("/{id}/regenerate")
    public SummaryView regenerate(@PathVariable UUID id, @RequestBody(required = false) RegenerateRequest body) {
        String feedback = body == null ? null : body.feedback();
        Summary saved = reviewService.regenerate(id, currentAuthor.requireUserId(), feedback);
        return SummaryView.from(saved);
    }

    @PostMapping("/{id}/accept")
    public SummaryView accept(@PathVariable UUID id) {
        return SummaryView.from(reviewService.accept(id));
    }

    public record EditRequest(@NotBlank @Size(max = 50_000) String summaryText) {
    }

    public record RegenerateRequest(@Size(max = 4_000) String feedback) {
    }

    public record SummaryView(
            String id,
            String audienceType,
            String summaryText,
            String editState,
            Instant acceptedAt,
            List<EditHistoryView> history) {

        public static SummaryView from(Summary s) {
            return new SummaryView(
                    s.getId().toString(),
                    s.getAudienceType().dbValue(),
                    s.getSummaryText(),
                    s.getEditState().dbValue(),
                    s.getAcceptedAt(),
                    s.getEditHistory().stream().map(EditHistoryView::from).toList());
        }
    }

    public record EditHistoryView(
            String changeType,
            Instant changedAt,
            String changedByAuthorId,
            String previousText,
            String newText,
            String feedback) {

        public static EditHistoryView from(EditHistoryEntry e) {
            return new EditHistoryView(
                    e.changeType(),
                    e.changedAt(),
                    e.changedByAuthorId() == null ? null : e.changedByAuthorId().toString(),
                    e.previousText(),
                    e.newText(),
                    e.feedback());
        }
    }
}
