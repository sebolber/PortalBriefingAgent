package app.briefingagent.ereignis;

import app.briefingagent.common.DbValuedEnum;

public enum EreignisSourceType implements DbValuedEnum {
    AUDIO("audio"),
    TEXT("text");

    private final String dbValue;

    EreignisSourceType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
