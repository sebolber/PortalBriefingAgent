package app.briefingagent.summary;

import app.briefingagent.common.DbValuedEnum;

public enum EditState implements DbValuedEnum {
    AI_GENERATED("ai_generated"),
    MANUALLY_EDITED("manually_edited"),
    REGENERATED("regenerated");

    private final String dbValue;

    EditState(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
