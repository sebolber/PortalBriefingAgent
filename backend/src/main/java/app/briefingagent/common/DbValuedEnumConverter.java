package app.briefingagent.common;

import jakarta.persistence.AttributeConverter;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class DbValuedEnumConverter<E extends Enum<E> & DbValuedEnum>
        implements AttributeConverter<E, String> {

    private final Class<E> enumClass;
    private final Map<String, E> byDbValue;

    protected DbValuedEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
        this.byDbValue = EnumSet.allOf(enumClass).stream()
                .collect(Collectors.toUnmodifiableMap(DbValuedEnum::dbValue, Function.identity()));
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        E value = byDbValue.get(dbData);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Unknown DB value '" + dbData + "' for " + enumClass.getSimpleName());
        }
        return value;
    }
}
