package app.briefingagent.summary;

import app.briefingagent.common.DbValuedEnum;

public enum AudienceType implements DbValuedEnum {
    PERSON("person"),
    PERSONGROUP("persongroup"),
    TOPIC("topic");

    private final String dbValue;

    AudienceType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
