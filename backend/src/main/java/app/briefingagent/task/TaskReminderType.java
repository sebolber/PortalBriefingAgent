package app.briefingagent.task;

import app.briefingagent.common.DbValuedEnum;

public enum TaskReminderType implements DbValuedEnum {
    ONE_DAY_BEFORE("one_day_before"),
    ON_DUE_DATE("on_due_date");

    private final String dbValue;

    TaskReminderType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }
}
