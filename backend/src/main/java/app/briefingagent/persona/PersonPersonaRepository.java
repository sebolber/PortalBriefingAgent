package app.briefingagent.persona;

import app.briefingagent.person.Person;
import app.briefingagent.user.UserAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonPersonaRepository extends JpaRepository<PersonPersona, UUID> {

    Optional<PersonPersona> findByAuthorAndPerson(UserAccount author, Person person);

    List<PersonPersona> findByAuthor(UserAccount author);
}
