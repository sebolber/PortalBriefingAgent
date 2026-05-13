package app.briefingagent.user;

import app.briefingagent.common.DbValuedEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter extends DbValuedEnumConverter<UserStatus> {

    public UserStatusConverter() {
        super(UserStatus.class);
    }
}
