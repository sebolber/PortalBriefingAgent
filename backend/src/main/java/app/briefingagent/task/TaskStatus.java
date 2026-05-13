package app.briefingagent.task;

import app.briefingagent.common.DbValuedEnum;

public enum TaskStatus implements DbValuedEnum {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    DONE("done"),
    DROPPED("dropped");

    private final String dbValue;

    TaskStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    public boolean isTerminal() {
        return this == DONE || this == DROPPED;
    }
}
