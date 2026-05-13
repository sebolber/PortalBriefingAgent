package app.briefingagent.topic;

import app.briefingagent.user.UserAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findFirstByAuthorAndNameOrderByCreatedAtAsc(UserAccount author, String name);

    List<Topic> findByAuthor(UserAccount author);
}
