package app.briefingagent.summary;

import java.time.Instant;
import java.util.UUID;

/**
 * One row in a Summary's append-only audit trail. Both manual edits and
 * regenerations land here; consumers tell them apart by {@link #changeType()}.
 */
public record EditHistoryEntry(
        String changeType,
        Instant changedAt,
        UUID changedByAuthorId,
        String previousText,
        String newText,
        String feedback) {

    public static final String TYPE_MANUAL_EDIT = "manual_edit";
    public static final String TYPE_REGENERATED = "regenerated";
}
