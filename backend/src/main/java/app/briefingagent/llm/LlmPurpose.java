package app.briefingagent.llm;

import app.briefingagent.common.DbValuedEnum;

public enum LlmPurpose implements DbValuedEnum {
    AUDIENCE_CLASSIFICATION("audience_classification"),
    SUMMARY_GENERATION("summary_generation"),
    TASK_EXTRACTION("task_extraction"),
    TRANSCRIPT_CORRECTION("transcript_correction");

    private final String dbValue;

    LlmPurpose(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
