package app.briefingagent.summary;

import app.briefingagent.common.DbValuedEnum;

public enum ClassificationConfidence implements DbValuedEnum {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String dbValue;

    ClassificationConfidence(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
