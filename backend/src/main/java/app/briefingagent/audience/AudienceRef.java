package app.briefingagent.audience;

import app.briefingagent.summary.AudienceType;
import java.util.UUID;

/**
 * Lightweight reference to one of the three concrete audience entities.
 * Used by the classification pipeline so consumers do not have to switch
 * on enum + UUID combinations themselves.
 */
public record AudienceRef(AudienceType type, UUID id, String name, String personaText) {
}
