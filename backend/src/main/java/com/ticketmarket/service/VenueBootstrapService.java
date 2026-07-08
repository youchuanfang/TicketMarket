package com.ticketmarket.service;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn("databaseSchemaInitializer")
public class VenueBootstrapService {
    private final JdbcTemplate jdbcTemplate;

    public VenueBootstrapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    @Transactional
    public void bootstrap() {
        cleanupSelfTestRows();
    }

    private void cleanupSelfTestRows() {
        String selfTest = "%\u81ea\u6d4b%";
        jdbcTemplate.update("""
                update performance
                set deleted=1, publish_status='OFFLINE', updated_at=now()
                where deleted=0 and lower(title) like '%codex%' and title like ?
                """, selfTest);
        jdbcTemplate.update("""
                update movie
                set deleted=1, status='OFFLINE', updated_at=now()
                where deleted=0 and lower(title) like '%codex%' and title like ?
                """, selfTest);
        jdbcTemplate.update("""
                update venue
                set deleted=1, status='DISABLED', updated_at=now()
                where deleted=0 and lower(name) like '%codex%' and name like ?
                """, selfTest);
    }
}
