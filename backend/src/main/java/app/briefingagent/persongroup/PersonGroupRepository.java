package app.briefingagent.persongroup;

import app.briefingagent.user.UserAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonGroupRepository extends JpaRepository<PersonGroup, UUID> {

    List<PersonGroup> findByAuthor(UserAccount author);
}
