package app.briefingagent.summary;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClassificationConfidenceConverter extends DbValuedEnumConverter<ClassificationConfidence> {

    public ClassificationConfidenceConverter() {
        super(ClassificationConfidence.class);
    }
}
