package app.briefingagent.ereignis;

import app.briefingagent.common.DbValuedEnum;

public enum TranscriptSource implements DbValuedEnum {
    WHISPER("whisper"),
    MANUAL("manual");

    private final String dbValue;

    TranscriptSource(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
