package app.briefingagent.llm.config;

import app.briefingagent.llm.LlmPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LlmProviderUsageRepository extends JpaRepository<LlmProviderUsage, UUID> {

    List<LlmProviderUsage> findByProvider(LlmProvider provider);

    Optional<LlmProviderUsage> findByPurposeAndActiveTrue(LlmPurpose purpose);

    Optional<LlmProviderUsage> findByProviderAndPurpose(LlmProvider provider, LlmPurpose purpose);
}
