package com.framehost.analytics.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Records a room creation event.
    public void recordRoomCreated() {

        String sql = """
                INSERT INTO analytics_events (
                    event_id,
                    event_type
                )
                VALUES (
                    gen_random_uuid(),
                    ?
                )
                """;

        jdbcTemplate.update(sql, "ROOM_CREATED");
    }

    // Returns the total number of successfully created rooms.
    public long countRoomsCreated() {

        String sql = """
                SELECT COUNT(*)
                FROM analytics_events
                WHERE event_type = ?
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                "ROOM_CREATED"
        );
    }
}
