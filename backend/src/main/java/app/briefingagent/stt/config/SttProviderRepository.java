package app.briefingagent.stt.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SttProviderRepository extends JpaRepository<SttProvider, UUID> {

    Optional<SttProvider> findFirstByActiveTrue();
}
