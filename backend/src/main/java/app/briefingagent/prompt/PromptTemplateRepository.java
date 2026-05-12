package app.briefingagent.prompt;

import app.briefingagent.llm.LlmPurpose;
import app.briefingagent.user.UserAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Optional<PromptTemplate> findByAuthorAndPurposeAndActiveTrue(UserAccount author, LlmPurpose purpose);

    List<PromptTemplate> findByAuthorAndPurposeOrderByVersionDesc(UserAccount author, LlmPurpose purpose);

    List<PromptTemplate> findByAuthorOrderByPurposeAscVersionDesc(UserAccount author);
}
