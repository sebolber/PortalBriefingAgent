package app.briefingagent.summary;

import app.briefingagent.ereignis.Ereignis;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {

    List<Summary> findByEreignisOrderByCreatedAtAsc(Ereignis ereignis);
}
