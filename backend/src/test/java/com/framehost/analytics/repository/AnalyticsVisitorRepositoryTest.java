package com.framehost.analytics.repository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class AnalyticsVisitorRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AnalyticsVisitorRepository repository;

    @BeforeEach
    public void setUp() {
        repository = new AnalyticsVisitorRepository(jdbcTemplate);
    }

    @Test
    void recordHeartbeatShouldExecutePostgresUpsert() {
        UUID visitorId = UUID.randomUUID();

        String expectedSql = """
                INSERT INTO analytics_visitors (visitor_id)
                VALUES (?)
                ON CONFLICT (visitor_id)
                DO UPDATE SET last_seen_at = NOW()
                """;

        repository.recordHeartbeat(visitorId);

        verify(jdbcTemplate).update(expectedSql, visitorId);
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    void recordDailyActivityShouldUseUtcDateAndIgnoreDuplicates() {
        UUID visitorId = UUID.randomUUID();

        String expectedSql = """
            INSERT INTO analytics_daily_activity (
                visitor_id,
                activity_date
            )
            VALUES (
                ?,
                (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::DATE
            )
            ON CONFLICT (visitor_id, activity_date)
            DO NOTHING
            """;

        repository.recordDailyActivity(visitorId);

        verify(jdbcTemplate).update(expectedSql, visitorId);
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    void countTotalVisitorsShouldReturnVisitorCount() {
        String expectedSql = """
                SELECT COUNT(*)
                FROM analytics_visitors
                """;

        when(jdbcTemplate.queryForObject(expectedSql, Long.class))
                .thenReturn(100L);

        long result = repository.countTotalVisitors();

        assertEquals(100L, result);
        verify(jdbcTemplate).queryForObject(expectedSql, Long.class);
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    void countActiveVisitorsShouldReturnActiveVisitorCount() {
        String expectedSql = """
                SELECT COUNT(*)
                FROM analytics_visitors
                WHERE last_seen_at >= NOW() - INTERVAL '5 minutes'
                """;

        when(jdbcTemplate.queryForObject(expectedSql, Long.class))
                .thenReturn(25L);

        long result = repository.countActiveVisitors();

        assertEquals(25L, result);
        verify(jdbcTemplate).queryForObject(expectedSql, Long.class);
        verifyNoMoreInteractions(jdbcTemplate);
    }

    @Test
    void countDailyUniqueVisitorsShouldReturnDailyVisitorCount() {
        String expectedSql = """
                SELECT COUNT(*)
                FROM analytics_daily_activity
                WHERE activity_date =
                    (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::DATE
                """;

        when(jdbcTemplate.queryForObject(expectedSql, Long.class))
                .thenReturn(40L);

        long result = repository.countDailyUniqueVisitors();

        assertEquals(40L, result);
        verify(jdbcTemplate).queryForObject(expectedSql, Long.class);
        verifyNoMoreInteractions(jdbcTemplate);
    }
}
