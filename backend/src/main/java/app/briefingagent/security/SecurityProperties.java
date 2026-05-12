package app.briefingagent.security;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "briefingagent.security")
public class SecurityProperties {

    /**
     * BCrypt cost factor. The Briefing Agent decision log requires at least 12.
     */
    @Min(10)
    private int bcryptStrength = 12;

    public int getBcryptStrength() {
        return bcryptStrength;
    }

    public void setBcryptStrength(int bcryptStrength) {
        this.bcryptStrength = bcryptStrength;
    }
}
