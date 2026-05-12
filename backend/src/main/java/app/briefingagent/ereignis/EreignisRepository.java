package app.briefingagent.ereignis;

import app.briefingagent.user.UserAccount;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EreignisRepository extends JpaRepository<Ereignis, UUID> {

    List<Ereignis> findByAuthorAndCreatedAtAfterOrderByCreatedAtDesc(
            UserAccount author, Instant since, Pageable pageable);
}
