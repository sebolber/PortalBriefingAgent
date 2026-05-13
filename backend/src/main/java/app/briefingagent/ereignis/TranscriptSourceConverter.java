package app.briefingagent.ereignis;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TranscriptSourceConverter extends DbValuedEnumConverter<TranscriptSource> {

    public TranscriptSourceConverter() {
        super(TranscriptSource.class);
    }
}
