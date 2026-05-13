package app.briefingagent.topic;

import app.briefingagent.user.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures every author has the canonical default audience used by the
 * walking-skeleton pipeline. Once the multi-shot classifier is wired in
 * (Iter 2) authors typically address richer audiences, but a single
 * always-present topic keeps the unclassified-fallback path well defined.
 */
@Service
public class DefaultTopicProvider {

    public static final String DEFAULT_TOPIC_NAME = "My Notes";
    public static final String DEFAULT_TOPIC_PERSONA =
            "Personal notes — concise summaries of recorded events for the author's own use.";

    private final TopicRepository topicRepository;

    public DefaultTopicProvider(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional
    public Topic ensureDefaultTopic(UserAccount author) {
        return topicRepository
                .findFirstByAuthorAndNameOrderByCreatedAtAsc(author, DEFAULT_TOPIC_NAME)
                .orElseGet(() -> topicRepository.save(
                        new Topic(author, DEFAULT_TOPIC_NAME, DEFAULT_TOPIC_PERSONA)));
    }
}
