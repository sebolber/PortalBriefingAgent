package app.briefingagent.audience;

import app.briefingagent.persona.PersonPersona;
import app.briefingagent.persona.PersonPersonaRepository;
import app.briefingagent.persongroup.PersonGroupRepository;
import app.briefingagent.summary.AudienceType;
import app.briefingagent.topic.TopicRepository;
import app.briefingagent.user.UserAccount;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the three audience flavours for an author into a uniform
 * list. The classifier and the summary generator both consume this view
 * so they can iterate without thinking about which table a row lives in.
 */
@Service
public class AudienceQueryService {

    private final PersonPersonaRepository personPersonaRepository;
    private final PersonGroupRepository personGroupRepository;
    private final TopicRepository topicRepository;

    public AudienceQueryService(PersonPersonaRepository personPersonaRepository,
                                PersonGroupRepository personGroupRepository,
                                TopicRepository topicRepository) {
        this.personPersonaRepository = personPersonaRepository;
        this.personGroupRepository = personGroupRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<AudienceRef> allFor(UserAccount author) {
        List<AudienceRef> refs = new ArrayList<>();
        for (PersonPersona pp : personPersonaRepository.findByAuthor(author)) {
            if (pp.getPerson().isTombstoned()) {
                continue;
            }
            refs.add(new AudienceRef(
                    AudienceType.PERSON,
                    pp.getPerson().getId(),
                    pp.getPerson().getDisplayName(),
                    pp.getPersonaText()));
        }
        personGroupRepository.findByAuthor(author).forEach(g ->
                refs.add(new AudienceRef(
                        AudienceType.PERSONGROUP,
                        g.getId(),
                        g.getName(),
                        g.getPersonaText())));
        topicRepository.findByAuthor(author).forEach(t ->
                refs.add(new AudienceRef(
                        AudienceType.TOPIC,
                        t.getId(),
                        t.getName(),
                        t.getPersonaText())));
        return List.copyOf(refs);
    }
}
