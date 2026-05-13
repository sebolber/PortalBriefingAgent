package app.briefingagent.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.briefingagent.common.TestEntities;
import app.briefingagent.ereignis.Ereignis;
import app.briefingagent.ereignis.EreignisRepository;
import app.briefingagent.ereignis.EreignisSourceType;
import app.briefingagent.user.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptRetentionJobTest {

    @Mock
    EreignisRepository ereignisRepository;

    private TranscriptRetentionJob job;
    private final RetentionProperties props = new RetentionProperties();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneId.of("UTC"));
        job = new TranscriptRetentionJob(ereignisRepository, props, clock);
    }

    @Test
    void blanks_transcript_text_for_eligible_ereignisse() {
        UserAccount author = TestEntities.withRandomId(
                new UserAccount("demo", "x", "Demo", "demo@example.invalid"));
        Ereignis old = TestEntities.withRandomId(new Ereignis(author, EreignisSourceType.TEXT));
        old.setTranscriptText("alt");

        when(ereignisRepository.findByTranscriptTextIsNotNullAndCreatedAtBefore(
                Instant.parse("2025-05-13T00:00:00Z"))).thenReturn(List.of(old));

        int count = job.runOnce();

        assertThat(count).isEqualTo(1);
        assertThat(old.getTranscriptText()).isNull();
        verify(ereignisRepository, times(1)).save(old);
    }

    @Test
    void second_run_after_blanking_finds_nothing_new() {
        when(ereignisRepository.findByTranscriptTextIsNotNullAndCreatedAtBefore(
                Instant.parse("2025-05-13T00:00:00Z"))).thenReturn(List.of());

        int count = job.runOnce();

        assertThat(count).isZero();
        verify(ereignisRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
