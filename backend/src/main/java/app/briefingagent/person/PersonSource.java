package app.briefingagent.person;

import app.briefingagent.common.DbValuedEnum;

public enum PersonSource implements DbValuedEnum {
    MANUAL("manual"),
    ENTRA("entra");

    private final String dbValue;

    PersonSource(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
