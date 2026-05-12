package app.briefingagent.persona;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonPersonaRepository extends JpaRepository<PersonPersona, UUID> {
}
