package app.briefingagent.search;

import app.briefingagent.user.UserAccount;
import app.briefingagent.user.UserAccountRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL-backed full-text search across audiences and content rows.
 * Phase 1 issues five small queries — one per searchable table — and
 * lets the database do the heavy lifting via plainto_tsquery + GIN.
 *
 * The query is sent through plainto_tsquery to keep the API impossible
 * to use as an injection vector even before parameter binding.
 */
@Service
public class SearchService {

    private static final int LIMIT_PER_BUCKET = 25;

    private final EntityManager em;
    private final UserAccountRepository userRepository;

    public SearchService(EntityManager em, UserAccountRepository userRepository) {
        this.em = em;
        this.userRepository = userRepository;
    }

    public record Hit(String type, String id, String label, String snippet, double rank) {
    }

    @Transactional(readOnly = true)
    public List<Hit> search(UUID authorId, String rawQuery) {
        String q = rawQuery == null ? "" : rawQuery.strip();
        if (q.isEmpty()) {
            return List.of();
        }
        if (q.length() > 200) {
            q = q.substring(0, 200);
        }
        UserAccount author = userRepository.findById(authorId).orElseThrow();

        List<Hit> hits = new ArrayList<>();
        hits.addAll(personHits(q));
        hits.addAll(personGroupHits(author, q));
        hits.addAll(topicHits(author, q));
        hits.addAll(ereignisHits(author, q));
        hits.addAll(summaryHits(author, q));
        hits.sort((a, b) -> Double.compare(b.rank(), a.rank()));
        return hits;
    }

    private List<Hit> personHits(String q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, COALESCE(pseudonym, full_name), full_name,
                       ts_rank_cd(search_vector, plainto_tsquery('german', unaccent(:q))) AS rank
                FROM person
                WHERE search_vector @@ plainto_tsquery('german', unaccent(:q))
                ORDER BY rank DESC
                LIMIT :lim
                """)
                .setParameter("q", q)
                .setParameter("lim", LIMIT_PER_BUCKET)
                .getResultList();
        return rows.stream()
                .map(r -> new Hit("person", r[0].toString(),
                        r[1].toString(), r[2].toString(), ((Number) r[3]).doubleValue()))
                .toList();
    }

    private List<Hit> personGroupHits(UserAccount author, String q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, name, persona_text,
                       ts_rank_cd(search_vector, plainto_tsquery('german', unaccent(:q))) AS rank
                FROM persongroup
                WHERE author_id = :author
                  AND search_vector @@ plainto_tsquery('german', unaccent(:q))
                ORDER BY rank DESC
                LIMIT :lim
                """)
                .setParameter("q", q)
                .setParameter("author", author.getId())
                .setParameter("lim", LIMIT_PER_BUCKET)
                .getResultList();
        return rows.stream()
                .map(r -> new Hit("persongroup", r[0].toString(),
                        r[1].toString(), excerpt(r[2]), ((Number) r[3]).doubleValue()))
                .toList();
    }

    private List<Hit> topicHits(UserAccount author, String q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, name, persona_text,
                       ts_rank_cd(search_vector, plainto_tsquery('german', unaccent(:q))) AS rank
                FROM topic
                WHERE author_id = :author
                  AND search_vector @@ plainto_tsquery('german', unaccent(:q))
                ORDER BY rank DESC
                LIMIT :lim
                """)
                .setParameter("q", q)
                .setParameter("author", author.getId())
                .setParameter("lim", LIMIT_PER_BUCKET)
                .getResultList();
        return rows.stream()
                .map(r -> new Hit("topic", r[0].toString(),
                        r[1].toString(), excerpt(r[2]), ((Number) r[3]).doubleValue()))
                .toList();
    }

    private List<Hit> ereignisHits(UserAccount author, String q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT id, transcript_text,
                       ts_rank_cd(search_vector, plainto_tsquery('german', unaccent(:q))) AS rank
                FROM ereignis
                WHERE author_id = :author
                  AND transcript_text IS NOT NULL
                  AND search_vector @@ plainto_tsquery('german', unaccent(:q))
                ORDER BY rank DESC
                LIMIT :lim
                """)
                .setParameter("q", q)
                .setParameter("author", author.getId())
                .setParameter("lim", LIMIT_PER_BUCKET)
                .getResultList();
        return rows.stream()
                .map(r -> new Hit("ereignis", r[0].toString(),
                        "Ereignis", excerpt(r[1]), ((Number) r[2]).doubleValue()))
                .toList();
    }

    private List<Hit> summaryHits(UserAccount author, String q) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT s.id, s.summary_text,
                       ts_rank_cd(s.search_vector, plainto_tsquery('german', unaccent(:q))) AS rank
                FROM summary s
                JOIN ereignis e ON e.id = s.ereignis_id
                WHERE e.author_id = :author
                  AND s.search_vector @@ plainto_tsquery('german', unaccent(:q))
                ORDER BY rank DESC
                LIMIT :lim
                """)
                .setParameter("q", q)
                .setParameter("author", author.getId())
                .setParameter("lim", LIMIT_PER_BUCKET)
                .getResultList();
        return rows.stream()
                .map(r -> new Hit("summary", r[0].toString(),
                        "Summary", excerpt(r[1]), ((Number) r[2]).doubleValue()))
                .toList();
    }

    private static String excerpt(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().strip();
        return s.length() <= 240 ? s : s.substring(0, 240) + "…";
    }
}
