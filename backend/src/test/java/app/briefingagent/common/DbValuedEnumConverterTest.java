package app.briefingagent.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DbValuedEnumConverterTest {

    enum Sample implements DbValuedEnum {
        ONE("one"), TWO("two");
        private final String dbValue;
        Sample(String dbValue) { this.dbValue = dbValue; }
        @Override public String dbValue() { return dbValue; }
    }

    static class SampleConverter extends DbValuedEnumConverter<Sample> {
        SampleConverter() { super(Sample.class); }
    }

    private final SampleConverter converter = new SampleConverter();

    @Test
    void enum_to_db_value_roundtrips() {
        assertThat(converter.convertToDatabaseColumn(Sample.ONE)).isEqualTo("one");
        assertThat(converter.convertToEntityAttribute("two")).isEqualTo(Sample.TWO);
    }

    @Test
    void null_in_null_out() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void unknown_db_value_is_loud() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("three"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("three")
                .hasMessageContaining("Sample");
    }
}
