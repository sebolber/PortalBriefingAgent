package app.briefingagent.ereignis;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EreignisSourceTypeConverter extends DbValuedEnumConverter<EreignisSourceType> {

    public EreignisSourceTypeConverter() {
        super(EreignisSourceType.class);
    }
}
