package app.briefingagent.pipeline;

import app.briefingagent.audience.AudienceRef;
import app.briefingagent.summary.ClassificationConfidence;

public record AudienceMatch(
        AudienceRef audience,
        ClassificationConfidence confidence,
        String reasoning) {
}
