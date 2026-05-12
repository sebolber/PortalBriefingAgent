package app.briefingagent.task;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskReminderTypeConverter extends DbValuedEnumConverter<TaskReminderType> {

    public TaskReminderTypeConverter() {
        super(TaskReminderType.class);
    }
}
