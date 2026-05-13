package app.briefingagent.common;

/**
 * Marker interface for enums that map to a stable lowercase DB string. Pair
 * with a JPA AttributeConverter that delegates to {@link #dbValue()}; this
 * keeps Java enum constants uppercase (per convention) while preserving the
 * DB literals defined in the schema CHECK constraints.
 */
public interface DbValuedEnum {

    String dbValue();
}
