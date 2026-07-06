package com.ticketmarket.service;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        for (String city : cities()) {
            ensureCinemas(city);
            ensureVenues(city, "STADIUM", "城市体育馆", "主舞台", 1200);
            ensureVenues(city, "THEATER", "大剧院", "舞台", 600);
        }
    }

    private void cleanupSelfTestRows() {
        jdbcTemplate.update("update performance set deleted=1, publish_status='OFFLINE', updated_at=now() where title like '%codex自测%' or title like '%Codex自测%'");
        jdbcTemplate.update("update movie set deleted=1, status='OFFLINE', updated_at=now() where title like '%codex自测%' or title like '%Codex自测%'");
        jdbcTemplate.update("update venue set deleted=1, status='DISABLED', updated_at=now() where name like '%codex自测%' or name like '%Codex自测%'");
    }

    private List<String> cities() {
        Set<String> values = new LinkedHashSet<>(List.of("上海", "北京", "杭州", "南京", "深圳"));
        values.addAll(jdbcTemplate.queryForList("select distinct city_name from venue where deleted=0 and city_name is not null and city_name<>''", String.class));
        values.addAll(jdbcTemplate.queryForList("select distinct city_name from performance where deleted=0 and city_name is not null and city_name<>''", String.class));
        return values.stream().filter(item -> item != null && !item.isBlank()).toList();
    }

    private void ensureCinemas(String city) {
        for (int i = 1; i <= 5; i++) {
            Long venueId = ensureVenue(city, city + "星影电影城" + i, "CINEMA", "银幕", 24);
            ensureCinemaHalls(venueId, 2);
            Long areaId = ensureArea(venueId, "座位区", "SEATED", "电影票", 1, "#2f9e44");
            ensureCinemaSeats(venueId, areaId);
        }
    }

    private void ensureVenues(String city, String type, String suffix, String stageLabel, int capacity) {
        for (int i = 1; i <= 5; i++) {
            Long venueId = ensureVenue(city, city + suffix + i, type, stageLabel, capacity);
            if ("STADIUM".equals(type)) {
                Long seated = ensureArea(venueId, "看台", "SEATED", "看台票", 1, "#74c0fc");
                Long standing = ensureArea(venueId, "内场", "STANDING", "内场票", 2, "#ff6b6b");
                ensureSeats(venueId, seated, 120, "看台");
                ensureSeats(venueId, standing, 80, "内场");
            } else {
                Long areaId = ensureArea(venueId, "座位区", "SEATED", "标准票", 1, "#74c0fc");
                ensureSeats(venueId, areaId, 120, "座位区");
            }
        }
    }

    private Long ensureVenue(String city, String name, String type, String stageLabel, int capacity) {
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from venue where deleted=0 and city_name=? and name=? order by id limit 1
                """, Long.class, city, name);
        if (!existing.isEmpty()) return existing.get(0);
        jdbcTemplate.update("""
                insert into venue
                (city_id, city_name, name, address, intro, description, venue_type, stage_label, capacity, status, created_at, updated_at, deleted)
                values (0, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """, city, name, city + "核心商圈", name + "，适合演出与观影使用。", name + "，适合演出与观影使用。", type, stageLabel, capacity);
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private Long ensureArea(Long venueId, String name, String type, String ticketLevel, int sortOrder, String color) {
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from venue_area where venue_id=? and name=? and deleted=0 order by id limit 1
                """, Long.class, venueId, name);
        if (!existing.isEmpty()) return existing.get(0);
        jdbcTemplate.update("""
                insert into venue_area
                (venue_id, name, area_type, default_ticket_level, sort_order, color, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """, venueId, name, type, ticketLevel, sortOrder, color);
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private void ensureCinemaHalls(Long venueId, int count) {
        for (int i = 1; i <= count; i++) {
            String name = i + "号厅";
            List<Long> existing = jdbcTemplate.queryForList("select id from cinema_hall where venue_id=? and name=? order by id limit 1", Long.class, venueId, name);
            if (existing.isEmpty()) {
                jdbcTemplate.update("""
                        insert into cinema_hall (venue_id, name, sort_order, status, created_at, updated_at, deleted)
                        values (?, ?, ?, 'ENABLED', now(), now(), 0)
                        """, venueId, name, i);
            } else {
                jdbcTemplate.update("update cinema_hall set sort_order=?, status='ENABLED', deleted=0, updated_at=now() where id=?", i, existing.get(0));
            }
        }
    }

    private void ensureCinemaSeats(Long venueId, Long areaId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from seat where venue_id=? and area_id=? and deleted=0", Integer.class, venueId, areaId);
        if (count != null && count > 0) return;
        int rows = 4;
        int seatsPerRow = 6;
        int gapX = 54;
        int gapY = 48;
        int startX = 380 - ((seatsPerRow - 1) * gapX / 2);
        int startY = 150;
        for (int row = 1; row <= rows; row++) {
            for (int seat = 1; seat <= seatsPerRow; seat++) {
                jdbcTemplate.update("""
                        insert into seat (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, 0, 0, 'AVAILABLE', now(), now(), 0)
                        """, venueId, areaId, String.valueOf(row), String.valueOf(seat), row + "排" + seat + "座", startX + (seat - 1) * gapX, startY + (row - 1) * gapY);
            }
        }
    }

    private void ensureSeats(Long venueId, Long areaId, int total, String prefix) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from seat where venue_id=? and area_id=? and deleted=0", Integer.class, venueId, areaId);
        if (count != null && count >= total) return;
        int existing = count == null ? 0 : count;
        List<Object[]> args = new ArrayList<>();
        for (int i = existing; i < total; i++) {
            int row = (i / 20) + 1;
            int seat = (i % 20) + 1;
            args.add(new Object[]{venueId, areaId, String.valueOf(row), String.valueOf(seat), prefix + row + "排" + seat + "座", 60 + (seat - 1) * 30, 80 + (row - 1) * 32});
        }
        jdbcTemplate.batchUpdate("""
                insert into seat (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, 0, 0, 'AVAILABLE', now(), now(), 0)
                """, args);
    }
}
