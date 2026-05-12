package app.briefingagent.llm.config;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LlmProviderRepository extends JpaRepository<LlmProvider, UUID> {
}
