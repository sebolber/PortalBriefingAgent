package app.briefingagent.summary;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EditStateConverter extends DbValuedEnumConverter<EditState> {

    public EditStateConverter() {
        super(EditState.class);
    }
}
