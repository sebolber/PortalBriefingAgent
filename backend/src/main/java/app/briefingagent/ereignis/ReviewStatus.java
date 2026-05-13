package app.briefingagent.ereignis;

import app.briefingagent.common.DbValuedEnum;

public enum ReviewStatus implements DbValuedEnum {
    PENDING("pending"),
    REVIEWED("reviewed"),
    RELEASED("released");

    private final String dbValue;

    ReviewStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
