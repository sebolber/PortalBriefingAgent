package app.briefingagent.ereignis;

/**
 * Centralised limits for capture flows. These mirror the spec (Section 6.1)
 * and are referenced by services, controllers and tests.
 */
public final class EreignisLimits {

    public static final int TEXT_HARD_CAP_CHARS = 10_000;
    public static final int AUDIO_SOFT_WARN_SECONDS = 10 * 60;
    public static final int AUDIO_HARD_CAP_SECONDS = 15 * 60;
    public static final int RECENT_DASHBOARD_DAYS = 7;

    private EreignisLimits() {
    }
}
