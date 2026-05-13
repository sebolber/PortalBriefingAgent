package app.briefingagent.summary;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AudienceTypeConverter extends DbValuedEnumConverter<AudienceType> {

    public AudienceTypeConverter() {
        super(AudienceType.class);
    }
}
