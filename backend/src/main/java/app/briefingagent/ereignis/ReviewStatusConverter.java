package app.briefingagent.ereignis;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReviewStatusConverter extends DbValuedEnumConverter<ReviewStatus> {

    public ReviewStatusConverter() {
        super(ReviewStatus.class);
    }
}
