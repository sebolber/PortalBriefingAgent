package app.briefingagent.llm;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LlmPurposeConverter extends DbValuedEnumConverter<LlmPurpose> {

    public LlmPurposeConverter() {
        super(LlmPurpose.class);
    }
}
