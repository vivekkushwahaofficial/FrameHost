package com.framehost.analytics.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsVisitorRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsVisitorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordHeartbeat(UUID visitorId) {

        String sql = """
                INSERT INTO analytics_visitors (visitor_id)
                VALUES (?)
                ON CONFLICT (visitor_id)
                DO UPDATE SET last_seen_at = NOW()
                """;

        jdbcTemplate.update(sql, visitorId);
    }

    public void recordDailyActivity(UUID visitorId) {

        String sql = """
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

        jdbcTemplate.update(sql, visitorId);
    }

    // Returns the total number of unique registered browser identities.
    public long countTotalVisitors() {
        String sql = """
                SELECT COUNT(*)
                FROM analytics_visitors
                """;

        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    // Returns visitors whose last heartbeat was within five minutes.
    public long countActiveVisitors() {
        String sql = """
                SELECT COUNT(*)
                FROM analytics_visitors
                WHERE last_seen_at >= NOW() - INTERVAL '5 minutes'
                """;

        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    // Returns unique visitors recorded for the current UTC date.
    public long countDailyUniqueVisitors() {
        String sql = """
                SELECT COUNT(*)
                FROM analytics_daily_activity
                WHERE activity_date =
                    (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::DATE
                """;

        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
