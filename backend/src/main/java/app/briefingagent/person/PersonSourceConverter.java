package app.briefingagent.person;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PersonSourceConverter extends DbValuedEnumConverter<PersonSource> {

    public PersonSourceConverter() {
        super(PersonSource.class);
    }
}
