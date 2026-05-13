package app.briefingagent.user;

import app.briefingagent.common.DbValuedEnum;

public enum UserStatus implements DbValuedEnum {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String dbValue;

    UserStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
