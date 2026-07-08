package com.ticketmarket.service;

import com.ticketmarket.common.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@DependsOn("databaseSchemaInitializer")
public class Phase3ResourceService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public Phase3ResourceService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        Integer venueCount = jdbcTemplate.queryForObject("select count(*) from venue where deleted = 0", Integer.class);
        if (venueCount == null || venueCount == 0) {
            seedVenues();
            seedAreas();
            seedSeats();
        }
        Integer sessionCount = jdbcTemplate.queryForObject("select count(*) from performance_session where deleted = 0", Integer.class);
        if (sessionCount == null || sessionCount == 0) {
            seedSessions();
            seedTicketLevels();
            seedBatches();
            initSessionSeats(1001L);
            initSessionSeats(1002L);
            initSessionSeats(1003L);
        }
        refreshBatchStatus();
    }

    public List<Map<String, Object>> venues() {
        return rows("""
                select id, city_id cityId, city_name cityName, name, address, capacity, description,
                       venue_type venueType, stage_label stageLabel, status,
                       created_at createdAt, updated_at updatedAt
                from venue where deleted = 0 order by id
                """);
    }

    public List<Map<String, Object>> hotVenues() {
        return rows("""
                select id, city_id cityId, city_name cityName, name, address, capacity, description,
                       venue_type venueType, stage_label stageLabel, status,
                       created_at createdAt, updated_at updatedAt
                from venue where deleted = 0 and status = 'ENABLED' order by id limit 6
                """);
    }

    public Map<String, Object> venue(Long id) {
        return one("""
                select id, city_id cityId, city_name cityName, name, address, capacity, description,
                       venue_type venueType, stage_label stageLabel, status,
                       created_at createdAt, updated_at updatedAt
                from venue where id = ? and deleted = 0
                """, "场馆不存在", id);
    }

    public Map<String, Object> createVenue(Map<String, Object> payload) {
        jdbcTemplate.update("""
                insert into venue (city_id, city_name, name, address, intro, description, venue_type, stage_label, capacity, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """,
                longValue(payload, "cityId", 1L),
                str(payload, "cityName", "上海"),
                str(payload, "name", "新场馆"),
                str(payload, "address", "待完善地址"),
                str(payload, "description", "场馆信息待完善"),
                str(payload, "description", "场馆信息待完善"),
                str(payload, "venueType", "THEATER"),
                str(payload, "stageLabel", "舞台"),
                intValue(payload, "capacity", 0)
        );
        return venue(lastId());
    }

    public Map<String, Object> updateVenue(Long id, Map<String, Object> payload) {
        findVenue(id);
        jdbcTemplate.update("""
                update venue set name=?, city_name=?, address=?, capacity=?, intro=?, description=?, venue_type=?, stage_label=?, status=?, updated_at=now()
                where id=? and deleted=0
                """,
                str(payload, "name", ""),
                str(payload, "cityName", ""),
                str(payload, "address", ""),
                intValue(payload, "capacity", 0),
                str(payload, "description", ""),
                str(payload, "description", ""),
                str(payload, "venueType", "THEATER"),
                str(payload, "stageLabel", "舞台"),
                str(payload, "status", "ENABLED"),
                id
        );
        return venue(id);
    }

    public void disableVenue(Long id) {
        jdbcTemplate.update("update venue set status='DISABLED', updated_at=now() where id=? and deleted=0", id);
    }

    @Transactional
    public void deleteVenue(Long id) {
        findVenue(id);
        clearVenueSeats(id);
        jdbcTemplate.update("update venue_area set status='DISABLED', deleted=1, updated_at=now() where venue_id=? and deleted=0", id);
        jdbcTemplate.update("update venue set status='DISABLED', deleted=1, updated_at=now() where id=? and deleted=0", id);
    }

    public List<Map<String, Object>> areas(Long venueId) {
        return rows("""
                select id, venue_id venueId, name areaName, area_type areaType, default_ticket_level defaultTicketLevel,
                       sort_order sortOrder, color, status, created_at createdAt, updated_at updatedAt
                from venue_area where venue_id = ? and deleted = 0 order by sort_order, id
                """, venueId);
    }

    public Map<String, Object> createArea(Long venueId, Map<String, Object> payload) {
        findVenue(venueId);
        jdbcTemplate.update("""
                insert into venue_area
                (venue_id, name, area_type, default_ticket_level, sort_order, color, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """,
                venueId,
                str(payload, "areaName", "新区"),
                str(payload, "areaType", "SEATED"),
                str(payload, "defaultTicketLevel", "标准票"),
                intValue(payload, "sortOrder", 1),
                str(payload, "color", "#d9303e")
        );
        return area(lastId());
    }

    public Map<String, Object> updateArea(Long id, Map<String, Object> payload) {
        area(id);
        jdbcTemplate.update("""
                update venue_area set name=?, area_type=?, default_ticket_level=?, sort_order=?, color=?, status=?, updated_at=now()
                where id=? and deleted=0
                """,
                str(payload, "areaName", ""),
                str(payload, "areaType", "SEATED"),
                str(payload, "defaultTicketLevel", "标准票"),
                intValue(payload, "sortOrder", 1),
                str(payload, "color", "#d9303e"),
                str(payload, "status", "ENABLED"),
                id
        );
        return area(id);
    }

    public void deleteArea(Long id) {
        jdbcTemplate.update("update venue_area set status='DISABLED', deleted=1, updated_at=now() where id=?", id);
    }

    public List<Map<String, Object>> seats(Long venueId) {
        return rows("""
                select s.id, s.venue_id venueId, s.area_id areaId, s.row_no rowNo, s.seat_no seatNo,
                       s.seat_label seatLabel, s.x, s.y, s.is_aisle isAisle, s.is_disabled isDisabled,
                       s.status, s.created_at createdAt, s.updated_at updatedAt
                from seat s where s.venue_id = ? and s.deleted = 0 order by s.area_id, s.y, s.x, s.id
                """, venueId);
    }

    public List<Map<String, Object>> generateSeats(Long venueId, Map<String, Object> payload) {
        if (boolValue(payload, "clearExisting", true)) {
            clearVenueSeats(venueId);
        }
        if ("STADIUM".equals(str(payload, "layoutType", ""))) {
            return generateStadiumSeats(venueId);
        }
        Long targetAreaId = longValue(payload, "areaId", null);
        if (targetAreaId == null) {
            targetAreaId = (Long) createArea(venueId, map(
                    "areaName", str(payload, "areaName", "A区"),
                    "areaType", "SEATED",
                    "defaultTicketLevel", "标准票",
                    "color", str(payload, "areaColor", "#d9303e")
            )).get("id");
        }
        Map<String, Object> area = area(targetAreaId);
        String areaName = String.valueOf(area.get("areaName"));
        int rowStart = intValue(payload, "rowStart", 1);
        int rowEnd = intValue(payload, "rowEnd", rowStart);
        int seatsPerRow = intValue(payload, "seatsPerRow", 12);
        int startX = intValue(payload, "startX", 40);
        int startY = intValue(payload, "startY", 60);
        int gapX = intValue(payload, "gapX", 28);
        int gapY = intValue(payload, "gapY", 28);
        List<Integer> aisles = parseAisles(str(payload, "aisleAfterSeats", ""));
        List<Map<String, Object>> generated = new ArrayList<>();
        for (int row = rowStart; row <= rowEnd; row++) {
            for (int number = 1; number <= seatsPerRow; number++) {
                int currentNumber = number;
                int aisleOffset = (int) aisles.stream().filter(value -> currentNumber > value).count() * gapX;
                jdbcTemplate.update("""
                        insert into seat
                        (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, 0, 'AVAILABLE', now(), now(), 0)
                        """,
                        venueId, targetAreaId, String.valueOf(row), String.valueOf(number),
                        areaName + "-" + row + "排" + number + "座",
                        startX + (number - 1) * gapX + aisleOffset,
                        startY + (row - rowStart) * gapY,
                        aisles.contains(number)
                );
                generated.add(seat(lastId()));
            }
        }
        return generated;
    }

    private List<Map<String, Object>> generateStadiumSeats(Long venueId) {
        Long fieldAreaId = ensureArea(venueId, "内场", "STANDING", "内场票", "#ff4d4f", 1);
        Long standAreaId = ensureArea(venueId, "看台", "SEATED", "看台票", "#74c0fc", 2);
        List<Long> ids = new ArrayList<>();

        String[] fieldRows = {"A", "B", "C", "D"};
        int[][] fieldOrigins = {{300, 355}, {520, 255}, {360, 155}, {210, 255}};
        for (int row = 0; row < fieldRows.length; row++) {
            for (int number = 1; number <= 6; number++) {
                int x = fieldOrigins[row][0] + (row == 1 || row == 3 ? 0 : (number - 1) * 38);
                int y = fieldOrigins[row][1] + (row == 1 || row == 3 ? (number - 1) * 34 : 0);
                if (row == 3) x = fieldOrigins[row][0];
                ids.add(insertSectionSeat(venueId, fieldAreaId, fieldRows[row] + number + "区", x, y));
            }
        }

        ids.addAll(generateRing(venueId, standAreaId, 100, 18, 380, 285, 240, 145, 205, 335));
        ids.addAll(generateRing(venueId, standAreaId, 200, 18, 380, 285, 300, 190, 190, 350));
        ids.addAll(generateRing(venueId, standAreaId, 500, 22, 380, 285, 350, 230, 175, 365));
        ids.addAll(generateRing(venueId, standAreaId, 600, 22, 380, 285, 390, 255, 170, 370));

        return ids.stream().map(this::seat).toList();
    }

    private List<Long> generateRing(Long venueId, Long areaId, int prefix, int count, int centerX, int centerY, int radiusX, int radiusY, int startDegree, int endDegree) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(startDegree + (endDegree - startDegree) * (count == 1 ? 0 : (double) i / (count - 1)));
            int x = centerX + (int) Math.round(Math.cos(angle) * radiusX);
            int y = centerY + (int) Math.round(Math.sin(angle) * radiusY);
            ids.add(insertSectionSeat(venueId, areaId, (prefix + i + 1) + "区", x, y));
        }
        return ids;
    }

    private Long ensureArea(Long venueId, String name, String type, String level, String color, int sortOrder) {
        List<Map<String, Object>> existing = rows("""
                select id, venue_id venueId, name areaName
                from venue_area where venue_id=? and name=? and deleted=0
                """, venueId, name);
        if (!existing.isEmpty()) return (Long) existing.get(0).get("id");
        return (Long) createArea(venueId, map(
                "areaName", name,
                "areaType", type,
                "defaultTicketLevel", level,
                "sortOrder", sortOrder,
                "color", color
        )).get("id");
    }

    private Long insertSectionSeat(Long venueId, Long areaId, String label, int x, int y) {
        List<Map<String, Object>> existing = rows("""
                select id from seat where venue_id=? and area_id=? and row_no='AREA' and seat_no=? and deleted=0
                """, venueId, areaId, label);
        if (!existing.isEmpty()) {
            Long id = (Long) existing.get(0).get("id");
            jdbcTemplate.update("update seat set seat_label=?, x=?, y=?, status='AVAILABLE', updated_at=now() where id=?", label, x, y, id);
            return id;
        }
        jdbcTemplate.update("""
                insert into seat
                (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                values (?, ?, 'AREA', ?, ?, ?, ?, 0, 0, 'AVAILABLE', now(), now(), 0)
                """, venueId, areaId, label, label, x, y);
        return jdbcTemplate.queryForObject("""
                select id from seat where venue_id=? and area_id=? and row_no='AREA' and seat_no=? and deleted=0
                """, Long.class, venueId, areaId, label);
    }

    public Map<String, Object> updateSeat(Long id, Map<String, Object> payload) {
        seat(id);
        jdbcTemplate.update("""
                update seat set x=?, y=?, is_aisle=?, is_disabled=?, status=?, updated_at=now()
                where id=? and deleted=0
                """,
                intValue(payload, "x", 0),
                intValue(payload, "y", 0),
                boolValue(payload, "isAisle", false),
                boolValue(payload, "isDisabled", false),
                str(payload, "status", "AVAILABLE"),
                id
        );
        return seat(id);
    }

    public void batchSeatDisabled(List<Long> ids, boolean disabled) {
        if (ids == null || ids.isEmpty()) return;
        String in = placeholders(ids.size());
        List<Object> args = new ArrayList<>();
        args.add(disabled);
        args.add(disabled ? "DISABLED" : "AVAILABLE");
        args.addAll(ids);
        jdbcTemplate.update("update seat set is_disabled=?, status=?, updated_at=now() where id in (" + in + ")", args.toArray());
    }

    @Transactional
    public int clearVenueSeats(Long venueId) {
        findVenue(venueId);
        int sessionSeats = jdbcTemplate.update("delete from session_seat where venue_id=?", venueId);
        int seats = jdbcTemplate.update("delete from seat where venue_id=?", venueId);
        return sessionSeats + seats;
    }

    public List<Map<String, Object>> sessions() {
        refreshBatchStatus();
        return rows(sessionSelect() + " where ps.deleted = 0 order by ps.start_time, ps.id");
    }

    public Map<String, Object> session(Long id) {
        return one(sessionSelect() + " where ps.id = ? and ps.deleted = 0", "场次不存在", id);
    }

    public List<Map<String, Object>> sessionsByPerformance(Long performanceId) {
        return rows(sessionSelect() + " where ps.performance_id = ? and ps.deleted = 0 order by ps.start_time, ps.id", performanceId);
    }

    public Map<String, Object> createSession(Map<String, Object> payload) {
        jdbcTemplate.update("""
                insert into performance_session
                (performance_id, venue_id, session_name, hall_name, sale_start_time, lock_time, entry_time, start_time, end_time,
                 sale_mode, purchase_mode, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED', now(), now(), 0)
                """,
                longValue(payload, "performanceId", 101L),
                longValue(payload, "venueId", 1L),
                str(payload, "sessionName", "新增场次"),
                str(payload, "sessionName", "新增场次"),
                timeValue(payload, "saleStartTime", "2026-07-20 10:00:00"),
                timeValue(payload, "lockTime", "2026-08-01 18:00:00"),
                timeValue(payload, "entryTime", "2026-08-18 18:00:00"),
                timeValue(payload, "startTime", "2026-08-18 19:30:00"),
                timeValue(payload, "endTime", "2026-08-18 22:00:00"),
                str(payload, "purchaseMode", "SELECTABLE"),
                str(payload, "purchaseMode", "SELECTABLE")
        );
        return session(lastId());
    }

    public Map<String, Object> updateSession(Long id, Map<String, Object> payload) {
        session(id);
        jdbcTemplate.update("""
                update performance_session
                set performance_id=?, venue_id=?, session_name=?, hall_name=?, sale_start_time=?, lock_time=?, entry_time=?,
                    start_time=?, end_time=?, sale_mode=?, purchase_mode=?, status=?, updated_at=now()
                where id=? and deleted=0
                """,
                longValue(payload, "performanceId", null),
                longValue(payload, "venueId", null),
                str(payload, "sessionName", ""),
                str(payload, "sessionName", ""),
                timeValue(payload, "saleStartTime", "2026-07-20 10:00:00"),
                timeValue(payload, "lockTime", "2026-08-01 18:00:00"),
                timeValue(payload, "entryTime", "2026-08-18 18:00:00"),
                timeValue(payload, "startTime", "2026-08-18 19:30:00"),
                timeValue(payload, "endTime", "2026-08-18 22:00:00"),
                str(payload, "purchaseMode", "SELECTABLE"),
                str(payload, "purchaseMode", "SELECTABLE"),
                str(payload, "status", "SCHEDULED"),
                id
        );
        return session(id);
    }

    public void deleteSession(Long id) {
        jdbcTemplate.update("update performance_session set status='CANCELLED', deleted=1, updated_at=now() where id=?", id);
    }

    public List<Map<String, Object>> ticketLevels(Long sessionId) {
        return rows(ticketLevelSelect() + " where tl.session_id = ? and tl.deleted = 0 order by tl.price, tl.id", sessionId);
    }

    public List<Map<String, Object>> frontTicketLevels(Long sessionId) {
        Map<String, Object> saleStatus = frontSaleStatus(sessionId);
        String status = String.valueOf(saleStatus.get("status"));
        Long batchId = saleStatus.get("batchId") == null ? null : Long.valueOf(String.valueOf(saleStatus.get("batchId")));
        return ticketLevels(sessionId).stream().peek(row -> {
            row.put("availableStock", batchId == null ? 0 : stockFor(batchId, row));
            row.put("frontStatus", ticketLevelFrontStatus(row, status));
        }).toList();
    }

    public Map<String, Object> ticketLevel(Long id) {
        return one(ticketLevelSelect() + " where tl.id = ? and tl.deleted = 0", "票档不存在", id);
    }

    public Map<String, Object> createTicketLevel(Map<String, Object> payload) {
        Long areaId = longValue(payload, "areaId", null);
        BigDecimal price = decimalValue(payload, "price", "180");
        int totalStock = intValue(payload, "stock", intValue(payload, "totalStock", 0));
        jdbcTemplate.update("""
                insert into ticket_level
                (session_id, name, area_id, price, total_stock, released_stock, unreleased_stock, sold_stock,
                 locked_stock, refunded_stock, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 'ENABLED', now(), now(), 0)
                """,
                longValue(payload, "sessionId", 1001L),
                autoTicketLevelName(areaId, price),
                areaId,
                price,
                totalStock,
                totalStock,
                0
        );
        return ticketLevel(lastId());
    }

    public Map<String, Object> updateTicketLevel(Long id, Map<String, Object> payload) {
        ticketLevel(id);
        Long areaId = longValue(payload, "areaId", null);
        BigDecimal price = decimalValue(payload, "price", "180");
        int stock = intValue(payload, "stock", intValue(payload, "totalStock", 0));
        jdbcTemplate.update("""
                update ticket_level set name=?, area_id=?, price=?, total_stock=?, released_stock=?, unreleased_stock=?, status=?, updated_at=now()
                where id=? and deleted=0
                """,
                autoTicketLevelName(areaId, price),
                areaId,
                price,
                stock,
                stock,
                0,
                str(payload, "status", "ENABLED"),
                id
        );
        return ticketLevel(id);
    }

    public void deleteTicketLevel(Long id) {
        jdbcTemplate.update("update ticket_level set status='DISABLED', deleted=1, updated_at=now() where id=?", id);
    }

    public List<Map<String, Object>> sessionSeats(Long sessionId) {
        return rows("""
                select ss.id, ss.session_id sessionId, ss.seat_id seatId, ss.venue_id venueId, ss.area_id areaId, ss.ticket_level_id ticketLevelId,
                       ss.batch_id batchId, ss.status, ss.lock_user_id lockUserId, ss.lock_expire_time lockExpireTime,
                       ss.seat_label seatLabel, coalesce(s.row_no, '') rowNo, coalesce(s.seat_no, '') seatNo,
                       ss.x, ss.y, ss.created_at createdAt, ss.updated_at updatedAt
                from session_seat ss left join seat s on ss.seat_id = s.id
                where ss.session_id = ? order by ss.y, ss.x, ss.id
                """, sessionId);
    }

    public List<Map<String, Object>> initSessionSeats(Long sessionId) {
        Map<String, Object> targetSession = session(sessionId);
        Long venueId = (Long) targetSession.get("venueId");
        Integer count = jdbcTemplate.queryForObject("select count(*) from session_seat where session_id=?", Integer.class, sessionId);
        List<Map<String, Object>> levels = ticketLevels(sessionId);
        if (count != null && count > 0) {
            refreshSessionSeatTicketLevels(sessionId, levels);
            return sessionSeats(sessionId);
        }
        for (Map<String, Object> seat : seats(venueId)) {
            Long areaId = (Long) seat.get("areaId");
            Long levelId = levels.stream()
                    .filter(level -> Objects.equals(level.get("areaId"), areaId))
                    .map(level -> (Long) level.get("id"))
                    .findFirst()
                    .orElse(levels.isEmpty() ? null : (Long) levels.get(0).get("id"));
            jdbcTemplate.update("""
                    insert into session_seat
                    (session_id, seat_id, venue_id, area_id, ticket_level_id, batch_id, status, lock_user_id,
                     lock_expire_time, seat_label, x, y, created_at, updated_at)
                    values (?, ?, ?, ?, ?, null, ?, null, null, ?, ?, ?, now(), now())
                    """,
                    sessionId, seat.get("id"), venueId, areaId, levelId,
                    Boolean.TRUE.equals(seat.get("isDisabled")) ? "DISABLED" : "AVAILABLE",
                    seat.get("seatLabel"), seat.get("x"), seat.get("y")
            );
        }
        return sessionSeats(sessionId);
    }

    private void refreshSessionSeatTicketLevels(Long sessionId, List<Map<String, Object>> levels) {
        for (Map<String, Object> level : levels) {
            Object areaId = level.get("areaId");
            if (areaId == null) continue;
            jdbcTemplate.update("""
                    update session_seat
                    set ticket_level_id=?, updated_at=now()
                    where session_id=? and area_id=? and status in ('AVAILABLE', 'UNRELEASED', 'DISABLED')
                    """, level.get("id"), sessionId, areaId);
        }
        if (!levels.isEmpty()) {
            jdbcTemplate.update("""
                    update session_seat
                    set ticket_level_id=?, updated_at=now()
                    where session_id=? and ticket_level_id is null and status in ('AVAILABLE', 'UNRELEASED', 'DISABLED')
                    """, levels.get(0).get("id"), sessionId);
        }
    }

    public void updateSessionSeatStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) return;
        jdbcTemplate.update("update session_seat set status=?, updated_at=now() where id in (" + placeholders(ids.size()) + ")", args(status, ids));
    }

    public synchronized List<Map<String, Object>> lockSessionSeats(Long sessionId, List<Long> ids, Long userId, Long batchId, int minutes) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Map<String, Object>> targets = rows("select id, status from session_seat where session_id=? and id in (" + placeholders(ids.size()) + ")", prepend(sessionId, ids));
        if (targets.size() != ids.size()) throw new ApiException(404, "部分座位不存在");
        if (targets.stream().anyMatch(item -> !"AVAILABLE".equals(item.get("status")))) {
            throw new ApiException(409, "所选座位已被占用，请重新选择");
        }
        String expireTime = FORMATTER.format(LocalDateTime.now().plusMinutes(minutes));
        List<Object> args = new ArrayList<>();
        args.add(batchId);
        args.add(userId);
        args.add(Timestamp.valueOf(parseTime(expireTime)));
        args.addAll(ids);
        jdbcTemplate.update("update session_seat set status='LOCKED', batch_id=?, lock_user_id=?, lock_expire_time=?, updated_at=now() where id in (" + placeholders(ids.size()) + ")", args.toArray());
        return rows("select * from (" + sessionSeatSelect() + ") t where t.id in (" + placeholders(ids.size()) + ")", ids.toArray());
    }

    public synchronized List<Map<String, Object>> autoAllocateSeats(Long sessionId, Long ticketLevelId, int quantity, Long userId, Long batchId) {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id from session_seat
                where session_id=? and ticket_level_id=? and status='AVAILABLE'
                order by y, x, id limit ?
                """, Long.class, sessionId, ticketLevelId, quantity);
        if (ids.size() < quantity) {
            Map<String, Object> level = ticketLevel(ticketLevelId);
            ids = jdbcTemplate.queryForList("""
                    select id from session_seat
                    where session_id=? and area_id=? and status='AVAILABLE'
                    order by y, x, id limit ?
                    """, Long.class, sessionId, level.get("areaId"), quantity);
        }
        if (ids.size() < quantity) throw new ApiException(409, "可售座位不足，请调整票档或数量");
        return lockSessionSeats(sessionId, ids, userId, batchId, 5);
    }

    public synchronized void releaseSessionSeats(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) return;
        List<Object> args = new ArrayList<>();
        if (userId != null) args.add(userId);
        args.addAll(ids);
        jdbcTemplate.update("update session_seat set status='AVAILABLE', batch_id=null, lock_user_id=null, lock_expire_time=null, updated_at=now() where status='LOCKED' " +
                (userId == null ? "" : "and lock_user_id=? ") + "and id in (" + placeholders(ids.size()) + ")", args.toArray());
    }

    public synchronized void markSessionSeatsSold(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) return;
        List<Object> args = new ArrayList<>();
        if (userId != null) args.add(userId);
        args.addAll(ids);
        jdbcTemplate.update("update session_seat set status='SOLD', lock_expire_time=null, updated_at=now() where " +
                (userId == null ? "" : "lock_user_id=? and ") + "id in (" + placeholders(ids.size()) + ")", args.toArray());
    }

    public synchronized void increaseTicketLevelSold(Long id, int quantity) {
        jdbcTemplate.update("""
                update ticket_level
                set sold_stock=sold_stock+?, locked_stock=greatest(0, locked_stock-?), updated_at=now()
                where id=? and deleted=0
                """, quantity, quantity, id);
    }

    public List<Map<String, Object>> saleBatches() {
        refreshBatchStatus();
        return rows(batchSelect() + " where sb.deleted = 0 order by sb.sale_start_time, sb.id");
    }

    public Map<String, Object> saleBatch(Long id) {
        refreshBatchStatus();
        return one(batchSelect() + " where sb.id=? and sb.deleted=0", "售票批次不存在", id);
    }

    public Map<String, Object> createSaleBatch(Map<String, Object> payload) {
        jdbcTemplate.update("""
                insert into sale_batch
                (session_id, name, batch_name, sale_start_time, lock_time, open_mode, release_type, open_stock,
                 release_quantity, release_ratio, allow_return_current_round, allow_return_during_sale,
                 limit_per_user, purchase_limit, queue_enabled, enable_queue, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'NOT_STARTED', now(), now(), 0)
                """,
                longValue(payload, "sessionId", 1001L),
                str(payload, "batchName", "新批次"),
                str(payload, "batchName", "新批次"),
                timeValue(payload, "saleStartTime", "2026-07-20 10:00:00"),
                timeValue(payload, "lockTime", "2026-08-01 18:00:00"),
                str(payload, "releaseType", "QUANTITY"),
                str(payload, "releaseType", "QUANTITY"),
                intValue(payload, "releaseQuantity", 50),
                intValue(payload, "releaseQuantity", 50),
                intValue(payload, "releaseRatio", 0),
                boolValue(payload, "allowReturnDuringSale", true),
                boolValue(payload, "allowReturnDuringSale", true),
                intValue(payload, "purchaseLimit", 2),
                intValue(payload, "purchaseLimit", 2),
                boolValue(payload, "enableQueue", true),
                boolValue(payload, "enableQueue", true)
        );
        return saleBatch(lastId());
    }

    public Map<String, Object> updateSaleBatch(Long id, Map<String, Object> payload) {
        saleBatch(id);
        jdbcTemplate.update("""
                update sale_batch
                set session_id=?, name=?, batch_name=?, sale_start_time=?, lock_time=?, open_mode=?, release_type=?,
                    open_stock=?, release_quantity=?, release_ratio=?, allow_return_current_round=?,
                    allow_return_during_sale=?, limit_per_user=?, purchase_limit=?, queue_enabled=?, enable_queue=?,
                    status=?, updated_at=now()
                where id=? and deleted=0
                """,
                longValue(payload, "sessionId", 1001L),
                str(payload, "batchName", ""),
                str(payload, "batchName", ""),
                timeValue(payload, "saleStartTime", "2026-07-20 10:00:00"),
                timeValue(payload, "lockTime", "2026-08-01 18:00:00"),
                str(payload, "releaseType", "QUANTITY"),
                str(payload, "releaseType", "QUANTITY"),
                intValue(payload, "releaseQuantity", 0),
                intValue(payload, "releaseQuantity", 0),
                intValue(payload, "releaseRatio", 0),
                boolValue(payload, "allowReturnDuringSale", true),
                boolValue(payload, "allowReturnDuringSale", true),
                intValue(payload, "purchaseLimit", 2),
                intValue(payload, "purchaseLimit", 2),
                boolValue(payload, "enableQueue", true),
                boolValue(payload, "enableQueue", true),
                str(payload, "status", "NOT_STARTED"),
                id
        );
        return saleBatch(id);
    }

    public Map<String, Object> changeBatchStatus(Long id, String status) {
        jdbcTemplate.update("update sale_batch set status=?, updated_at=now() where id=? and deleted=0", status, id);
        if ("SELLING".equals(status)) initRedisStock(id);
        return saleBatch(id);
    }

    public Map<String, Object> activeBatch(Long sessionId) {
        Map<String, Object> status = frontSaleStatus(sessionId);
        Object batchId = status.get("batchId");
        return batchId == null ? null : saleBatch(Long.valueOf(String.valueOf(batchId)));
    }

    public Map<String, Object> frontSaleStatus(Long sessionId) {
        refreshBatchStatus();
        Map<String, Object> session = session(sessionId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = parseTime(String.valueOf(session.get("startTime")));
        if (!now.isBefore(sessionStart)) {
            return frontStatus(sessionId, "ENDED", "已结束", false, false, null, session.get("startTime"));
        }
        Map<String, Object> batch = frontBatch(sessionId);
        if (batch == null) return frontStatus(sessionId, "SOLD_OUT", "已售罄", false, false, null, session.get("startTime"));
        LocalDateTime saleStart = parseTime(String.valueOf(batch.get("saleStartTime")));
        LocalDateTime lockTime = parseTime(String.valueOf(batch.get("lockTime")));
        boolean hasStock = hasFrontStock(batch);
        if (now.isBefore(saleStart)) return frontStatus(batch, "COMING_SOON", "预约抢票", true, hasStock);
        if (!now.isBefore(lockTime) || "LOCKED".equals(batch.get("status"))) {
            return frontStatus(batch, "SOLD_OUT", "已售罄", false, false);
        }
        if (!hasStock) return frontStatus(batch, "SOLD_OUT", "已售罄", false, false);
        return frontStatus(batch, "ON_SALE", "立即购票", true, true);
    }

    public Map<String, Object> frontPerformanceStatus(Long performanceId) {
        List<Map<String, Object>> sessionStatuses = sessionsByPerformance(performanceId).stream()
                .map(session -> frontSaleStatus((Long) session.get("id")))
                .toList();
        if (sessionStatuses.isEmpty()) {
            return map("status", "COMING_SOON", "frontStatus", "COMING_SOON", "buttonText", "预约抢票", "clickable", false, "hasStock", false);
        }
        return sessionStatuses.stream().filter(status -> "ON_SALE".equals(status.get("status"))).findFirst()
                .or(() -> sessionStatuses.stream().filter(status -> "COMING_SOON".equals(status.get("status"))).findFirst())
                .or(() -> sessionStatuses.stream().filter(status -> "SOLD_OUT".equals(status.get("status"))).findFirst())
                .orElse(sessionStatuses.get(0));
    }

    public void assertFrontSaleOpen(Long sessionId, Long batchId) {
        Map<String, Object> batch = batchId == null ? frontBatch(sessionId) : saleBatch(batchId);
        if (batch == null || !Objects.equals(batch.get("sessionId"), sessionId)) {
            throw new ApiException(404, "当前场次暂无可用售票批次");
        }
        Map<String, Object> status = frontSaleStatus(sessionId);
        if (!Objects.equals(status.get("batchId"), batch.get("id")) || !"ON_SALE".equals(status.get("status"))) {
            throw new ApiException(409, "当前场次暂未开放购票，可先预约抢票");
        }
    }

    public void assertSeatSelectionOpen(Long sessionId, Long batchId) {
        if (isMovieSession(sessionId)) return;
        try {
            assertFrontSaleOpen(sessionId, batchId);
        } catch (ApiException ex) {
            throw new ApiException(ex.getCode(), "当前场次暂未开放选座，可先预约抢票");
        }
    }

    public boolean isMovieSession(Long sessionId) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from performance_session
                where id=? and movie_id is not null and deleted=0
                """, Integer.class, sessionId);
        return count != null && count > 0;
    }

    public Map<String, Object> initRedisStock(Long batchId) {
        Map<String, Object> batch = saleBatch(batchId);
        Long sessionId = (Long) batch.get("sessionId");
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> level : ticketLevels(sessionId)) {
            String key = redisStockKey(batchId, (Long) level.get("id"));
            int released = intValue(level, "releasedStock", 0);
            int sold = intValue(level, "soldStock", 0);
            int locked = intValue(level, "lockedStock", 0);
            int stock = Math.max(0, released - sold - locked);
            redisTemplate.opsForValue().set(key, String.valueOf(stock));
            result.put(key, stock);
        }
        return result;
    }

    public Map<String, Object> batchStockSummary(Long batchId) {
        Map<String, Object> batch = saleBatch(batchId);
        Long sessionId = (Long) batch.get("sessionId");
        List<Map<String, Object>> rows = ticketLevels(sessionId).stream().map(level -> {
            String key = redisStockKey(batchId, (Long) level.get("id"));
            return map(
                    "ticketLevelId", level.get("id"),
                    "ticketLevelName", level.get("name"),
                    "redisKey", key,
                    "redisStock", redisTemplate.opsForValue().get(key),
                    "releasedStock", level.get("releasedStock"),
                    "soldStock", level.get("soldStock"),
                    "lockedStock", level.get("lockedStock")
            );
        }).toList();
        return map("batch", batch, "levels", rows);
    }

    public List<Map<String, Object>> stockPool() {
        return rows("""
                select id, session_id sessionId, ticket_level_id ticketLevelId, session_seat_id seatId,
                       source_type sourceType, status stockStatus, available_for_next_batch availableForNextBatch,
                       created_at createdAt, updated_at updatedAt
                from stock_pool order by id desc
                """);
    }

    public List<Map<String, Object>> inventory(Long sessionId, Long ticketLevelId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where tl.deleted=0 and ps.deleted=0 ");
        if (sessionId != null) {
            where.append(" and ps.id=? ");
            args.add(sessionId);
        }
        if (ticketLevelId != null) {
            where.append(" and tl.id=? ");
            args.add(ticketLevelId);
        }
        List<Map<String, Object>> rows = rows("""
                select ps.id sessionId,
                       coalesce(p.title, m.title, ps.session_name, ps.hall_name) itemTitle,
                       coalesce(ps.session_name, ps.hall_name) sessionName,
                       date_format(ps.start_time, '%Y-%m-%d %H:%i:%s') startTime,
                       v.name venueName,
                       tl.id ticketLevelId, tl.name ticketLevelName, tl.price,
                       tl.total_stock totalStock, tl.released_stock releasedStock,
                       tl.unreleased_stock unreleasedStock, tl.sold_stock soldStock,
                       tl.locked_stock lockedStock, tl.refunded_stock refundedStock,
                       tl.status status,
                       coalesce(sum(case when sp.status='WAITING_RELEASE' then 1 else 0 end), 0) waitingPoolStock,
                       coalesce(sum(case when sp.status='RELEASED' then 1 else 0 end), 0) releasedPoolStock
                from ticket_level tl
                join performance_session ps on ps.id=tl.session_id
                left join performance p on p.id=ps.performance_id and p.deleted=0
                left join movie m on m.id=ps.movie_id and m.deleted=0
                left join venue v on v.id=ps.venue_id
                left join stock_pool sp on sp.ticket_level_id=tl.id and sp.session_id=ps.id
                """ + where + """
                group by ps.id, p.title, m.title, ps.session_name, ps.hall_name, ps.start_time, v.name,
                         tl.id, tl.name, tl.price, tl.total_stock, tl.released_stock, tl.unreleased_stock,
                         tl.sold_stock, tl.locked_stock, tl.refunded_stock, tl.status
                order by ps.start_time desc, ps.id desc, tl.price, tl.id
                """, args.toArray());
        rows.forEach(row -> {
            int released = intValue(row, "releasedStock", 0);
            int sold = intValue(row, "soldStock", 0);
            int locked = intValue(row, "lockedStock", 0);
            row.put("availableStock", Math.max(0, released - sold - locked));
        });
        return rows;
    }

    public List<Map<String, Object>> stockPoolBySession(Long sessionId) {
        return rows("""
                select id, session_id sessionId, ticket_level_id ticketLevelId, session_seat_id seatId,
                       source_type sourceType, status stockStatus, available_for_next_batch availableForNextBatch,
                       created_at createdAt, updated_at updatedAt
                from stock_pool where session_id=? order by id desc
                """, sessionId);
    }

    public Map<String, Object> addStockPool(Map<String, Object> payload) {
        jdbcTemplate.update("""
                insert into stock_pool
                (session_id, ticket_level_id, session_seat_id, source_type, status, available_for_next_batch, created_at, updated_at)
                values (?, ?, ?, ?, 'WAITING_RELEASE', ?, now(), now())
                """,
                longValue(payload, "sessionId", 1001L),
                longValue(payload, "ticketLevelId", null),
                longValue(payload, "seatId", null),
                str(payload, "sourceType", "ADMIN_ADD"),
                boolValue(payload, "availableForNextBatch", true)
        );
        return stockPool().stream().filter(row -> Objects.equals(row.get("id"), lastId())).findFirst().orElse(Map.of());
    }

    public Map<String, Object> releaseStockToBatch(Map<String, Object> payload) {
        Long targetBatchId = longValue(payload, "batchId", null);
        if (targetBatchId == null) throw new ApiException(400, "请选择要释放到的售票批次");
        jdbcTemplate.update("""
                update stock_pool set status='RELEASED', updated_at=now()
                where status='WAITING_RELEASE' and available_for_next_batch=1
                """);
        return batchStockSummary(targetBatchId);
    }

    public void refreshBatchStatus() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("update sale_batch set status='SELLING', updated_at=now() where deleted=0 and status='NOT_STARTED' and sale_start_time <= ?", Timestamp.valueOf(now));
        jdbcTemplate.update("update sale_batch set status='LOCKED', updated_at=now() where deleted=0 and status='SELLING' and lock_time <= ?", Timestamp.valueOf(now));
    }

    private void seedVenues() {
        insertVenue(1L, "星河影城一号厅", 1L, "上海", "浦东新区星河路 18 号", 96, "适合电影选座演示的小厅");
        insertVenue(2L, "城市剧院中厅", 2L, "杭州", "西湖区文艺路 16 号", 384, "适合话剧和音乐会的中型剧院");
        insertVenue(3L, "海川体育馆", 3L, "深圳", "南山区滨海大道 188 号", 1200, "适合演唱会和体育赛事的大场馆");
    }

    private void seedAreas() {
        insertArea(1L, 1L, "影厅A区", "电影票", 1, "#d9303e");
        insertArea(2L, 2L, "一层池座", "优选票", 1, "#177e89");
        insertArea(3L, 2L, "二层楼座", "看台票", 2, "#b7791f");
        insertArea(4L, 3L, "A区", "内场票", 1, "#d9303e");
        insertArea(5L, 3L, "B区", "看台票", 2, "#177e89");
    }

    private void seedSeats() {
        generateSeats(1L, map("areaId", 1L, "rowStart", 1, "rowEnd", 8, "seatsPerRow", 12, "startX", 60, "startY", 80, "gapX", 30, "gapY", 30, "aisleAfterSeats", "6"));
        generateSeats(2L, map("areaId", 2L, "rowStart", 1, "rowEnd", 10, "seatsPerRow", 24, "startX", 40, "startY", 80, "gapX", 24, "gapY", 26, "aisleAfterSeats", "8,16"));
        generateSeats(2L, map("areaId", 3L, "rowStart", 11, "rowEnd", 16, "seatsPerRow", 24, "startX", 40, "startY", 360, "gapX", 24, "gapY", 26, "aisleAfterSeats", "8,16"));
        generateSeats(3L, map("areaId", 4L, "rowStart", 1, "rowEnd", 8, "seatsPerRow", 20, "startX", 50, "startY", 90, "gapX", 24, "gapY", 26, "aisleAfterSeats", "10"));
        generateSeats(3L, map("areaId", 5L, "rowStart", 1, "rowEnd", 8, "seatsPerRow", 20, "startX", 50, "startY", 330, "gapX", 24, "gapY", 26, "aisleAfterSeats", "10"));
    }

    private void seedSessions() {
        insertSession(1001L, 102L, 2L, "夜航西窗 8月21日晚场", "2026-07-20 10:00:00", "2026-08-21 18:00:00", "2026-08-21 18:15:00", "2026-08-21 19:00:00", "2026-08-21 21:20:00", "SELECTABLE");
        insertSession(1002L, 101L, 3L, "星河回声 上海站", "2026-07-20 10:00:00", "2026-08-18 18:30:00", "2026-08-18 18:00:00", "2026-08-18 19:30:00", "2026-08-18 22:00:00", "AUTO_ALLOCATE");
        insertSession(1003L, 201L, 1L, "深空旅人 10:30", "2026-07-10 10:00:00", "2026-12-31 23:00:00", "2026-07-18 10:00:00", "2026-07-18 10:30:00", "2026-07-18 12:40:00", "SELECTABLE");
    }

    private void seedTicketLevels() {
        insertTicketLevel(2001L, 1001L, "池座优选票", "380", 2L, 240, 120, 120);
        insertTicketLevel(2002L, 1001L, "楼座惠民票", "180", 3L, 144, 60, 84);
        insertTicketLevel(2003L, 1002L, "内场票", "680", 4L, 160, 80, 80);
        insertTicketLevel(2004L, 1002L, "看台票", "380", 5L, 160, 80, 80);
    }

    private void seedBatches() {
        insertBatch(3001L, 1001L, "第一轮开售", "2026-07-20 10:00:00", "2026-08-01 18:00:00", "QUANTITY", 120, 0, "SELLING");
        insertBatch(3002L, 1002L, "锁票后库存入池", "2026-07-22 10:00:00", "2026-07-30 18:00:00", "QUANTITY", 80, 0, "LOCKED");
        insertBatch(3003L, 1001L, "第二轮重新开放", "2026-08-05 10:00:00", "2026-08-20 18:00:00", "RATIO", 0, 30, "NOT_STARTED");
    }

    private void insertVenue(Long id, String name, Long cityId, String cityName, String address, int capacity, String description) {
        String type = capacity >= 1000 ? "STADIUM" : "THEATER";
        jdbcTemplate.update("""
                insert into venue (id, city_id, city_name, name, address, intro, description, venue_type, stage_label, capacity, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """, id, cityId, cityName, name, address, description, description, type, "舞台", capacity);
    }

    private void insertArea(Long id, Long venueId, String name, String level, int sort, String color) {
        jdbcTemplate.update("""
                insert into venue_area (id, venue_id, name, area_type, default_ticket_level, sort_order, color, status, created_at, updated_at, deleted)
                values (?, ?, ?, 'SEATED', ?, ?, ?, 'ENABLED', now(), now(), 0)
                """, id, venueId, name, level, sort, color);
    }

    private void insertSession(Long id, Long performanceId, Long venueId, String name, String saleStart, String lock, String entry, String start, String end, String mode) {
        jdbcTemplate.update("""
                insert into performance_session
                (id, performance_id, venue_id, session_name, hall_name, sale_start_time, lock_time, entry_time, start_time, end_time, sale_mode, purchase_mode, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED', now(), now(), 0)
                """, id, performanceId, venueId, name, name, Timestamp.valueOf(parseTime(saleStart)), Timestamp.valueOf(parseTime(lock)), Timestamp.valueOf(parseTime(entry)), Timestamp.valueOf(parseTime(start)), Timestamp.valueOf(parseTime(end)), mode, mode);
    }

    private void insertTicketLevel(Long id, Long sessionId, String name, String price, Long areaId, int total, int released, int unreleased) {
        jdbcTemplate.update("""
                insert into ticket_level
                (id, session_id, name, area_id, price, total_stock, released_stock, unreleased_stock, sold_stock, locked_stock, refunded_stock, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 'ENABLED', now(), now(), 0)
                """, id, sessionId, name, areaId, new BigDecimal(price), total, released, unreleased);
    }

    private void insertBatch(Long id, Long sessionId, String name, String saleStart, String lock, String releaseType, int quantity, int ratio, String status) {
        jdbcTemplate.update("""
                insert into sale_batch
                (id, session_id, name, batch_name, sale_start_time, lock_time, open_mode, release_type, open_stock, release_quantity,
                 release_ratio, allow_return_current_round, allow_return_during_sale, limit_per_user, purchase_limit,
                 queue_enabled, enable_queue, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 2, 2, 1, 1, ?, now(), now(), 0)
                """, id, sessionId, name, name, Timestamp.valueOf(parseTime(saleStart)), Timestamp.valueOf(parseTime(lock)), releaseType, releaseType, quantity, quantity, ratio, status);
    }

    private Map<String, Object> findVenue(Long id) {
        return venue(id);
    }

    private Map<String, Object> area(Long id) {
        return one("""
                select id, venue_id venueId, name areaName, area_type areaType, default_ticket_level defaultTicketLevel,
                       sort_order sortOrder, color, status, created_at createdAt, updated_at updatedAt
                from venue_area where id = ? and deleted = 0
                """, "区域不存在", id);
    }

    private Map<String, Object> seat(Long id) {
        return one("""
                select id, venue_id venueId, area_id areaId, row_no rowNo, seat_no seatNo, seat_label seatLabel,
                       x, y, is_aisle isAisle, is_disabled isDisabled, status, created_at createdAt, updated_at updatedAt
                from seat where id = ? and deleted = 0
                """, "座位不存在", id);
    }

    private String sessionSelect() {
        return """
                select ps.id, ps.performance_id performanceId, ps.movie_id movieId, ps.venue_id venueId,
                       p.title performanceTitle, m.title movieTitle,
                       coalesce(ps.session_name, ps.hall_name) sessionName,
                       date_format(ps.sale_start_time, '%Y-%m-%d %H:%i:%s') saleStartTime,
                       date_format(ps.lock_time, '%Y-%m-%d %H:%i:%s') lockTime,
                       date_format(ps.entry_time, '%Y-%m-%d %H:%i:%s') entryTime,
                       date_format(ps.start_time, '%Y-%m-%d %H:%i:%s') startTime,
                       date_format(ps.end_time, '%Y-%m-%d %H:%i:%s') endTime,
                       coalesce(ps.purchase_mode, ps.sale_mode) purchaseMode,
                       ps.status, ps.created_at createdAt, ps.updated_at updatedAt
                from performance_session ps
                left join performance p on p.id=ps.performance_id and p.deleted=0
                left join movie m on m.id=ps.movie_id and m.deleted=0
                """;
    }

    private String ticketLevelSelect() {
        return """
                select tl.id, tl.session_id sessionId, tl.name, tl.price, tl.area_id areaId, tl.total_stock totalStock,
                       tl.released_stock releasedStock, tl.unreleased_stock unreleasedStock, tl.sold_stock soldStock,
                       tl.locked_stock lockedStock, tl.refunded_stock refundedStock, tl.status, tl.created_at createdAt, tl.updated_at updatedAt
                from ticket_level tl
                """;
    }

    private String batchSelect() {
        return """
                select sb.id, sb.session_id sessionId, coalesce(sb.batch_name, sb.name) batchName,
                       date_format(sb.sale_start_time, '%Y-%m-%d %H:%i:%s') saleStartTime,
                       date_format(sb.lock_time, '%Y-%m-%d %H:%i:%s') lockTime,
                       coalesce(sb.release_type, sb.open_mode) releaseType,
                       coalesce(sb.release_quantity, sb.open_stock) releaseQuantity,
                       sb.release_ratio releaseRatio, sb.allow_return_during_sale allowReturnDuringSale,
                       coalesce(sb.purchase_limit, sb.limit_per_user) purchaseLimit,
                       coalesce(sb.enable_queue, sb.queue_enabled) enableQueue,
                       sb.status, sb.created_at createdAt, sb.updated_at updatedAt
                from sale_batch sb
                """;
    }

    private String sessionSeatSelect() {
        return """
                select ss.id, ss.session_id sessionId, ss.seat_id seatId, ss.venue_id venueId, ss.area_id areaId, ss.ticket_level_id ticketLevelId,
                       ss.batch_id batchId, ss.status, ss.lock_user_id lockUserId,
                       date_format(ss.lock_expire_time, '%Y-%m-%d %H:%i:%s') lockExpireTime,
                       ss.seat_label seatLabel, coalesce(s.row_no, '') rowNo, coalesce(s.seat_no, '') seatNo,
                       ss.x, ss.y, ss.created_at createdAt, ss.updated_at updatedAt
                from session_seat ss left join seat s on ss.seat_id = s.id
                """;
    }

    private Map<String, Object> frontBatch(Long sessionId) {
        List<Map<String, Object>> candidates = rows(batchSelect() + """
                where sb.session_id = ? and sb.deleted = 0 and sb.status <> 'CANCELLED'
                order by sb.sale_start_time asc, sb.id asc
                """, sessionId);
        LocalDateTime now = LocalDateTime.now();
        return candidates.stream()
                .filter(item -> now.isBefore(parseTime(String.valueOf(item.get("lockTime"))))
                        || "SELLING".equals(item.get("status"))
                        || "NOT_STARTED".equals(item.get("status")))
                .findFirst()
                .orElse(candidates.stream().max(Comparator.comparing(item -> parseTime(String.valueOf(item.get("lockTime"))))).orElse(null));
    }

    private Map<String, Object> frontStatus(Map<String, Object> batch, String status, String buttonText, boolean clickable, boolean hasStock) {
        return map("batchId", batch.get("id"), "status", status, "frontStatus", status, "buttonText", buttonText,
                "clickable", clickable, "hasStock", hasStock, "saleStartTime", batch.get("saleStartTime"), "saleEndTime", batch.get("lockTime"));
    }

    private Map<String, Object> frontStatus(Long sessionId, String status, String buttonText, boolean clickable, boolean hasStock,
                                            Object saleStartTime, Object saleEndTime) {
        return map("batchId", null, "sessionId", sessionId, "status", status, "frontStatus", status, "buttonText", buttonText,
                "clickable", clickable, "hasStock", hasStock, "saleStartTime", saleStartTime, "saleEndTime", saleEndTime);
    }

    private boolean hasFrontStock(Map<String, Object> batch) {
        Long batchId = (Long) batch.get("id");
        Long sessionId = (Long) batch.get("sessionId");
        return ticketLevels(sessionId).stream().anyMatch(level -> stockFor(batchId, level) > 0);
    }

    private int stockFor(Long batchId, Map<String, Object> level) {
        String redisStock = redisTemplate.opsForValue().get(redisStockKey(batchId, (Long) level.get("id")));
        if (redisStock != null && !redisStock.isBlank()) return Math.max(0, Integer.parseInt(redisStock));
        int released = intValue(level, "releasedStock", 0);
        int sold = intValue(level, "soldStock", 0);
        int locked = intValue(level, "lockedStock", 0);
        return Math.max(0, released - sold - locked);
    }

    private String autoTicketLevelName(Long areaId, BigDecimal price) {
        if (areaId == null) return "票档" + price.stripTrailingZeros().toPlainString();
        List<Map<String, Object>> rows = rows("select name areaName from venue_area where id=? and deleted=0", areaId);
        String areaName = rows.isEmpty() ? "票档" : String.valueOf(rows.get(0).get("areaName"));
        return areaName + price.stripTrailingZeros().toPlainString();
    }

    private String ticketLevelFrontStatus(Map<String, Object> level, String saleStatus) {
        if ("COMING_SOON".equals(saleStatus)) return "暂未开售";
        if (!"ON_SALE".equals(saleStatus)) return "不可售";
        int stock = stockFor((Long) frontSaleStatus((Long) level.get("sessionId")).get("batchId"), level);
        if (stock <= 0) return "缺货";
        return stock <= 10 ? "票量紧张" : "可选";
    }

    private String redisStockKey(Long batchId, Long ticketLevelId) {
        return "ticket:batch:" + batchId + ":level:" + ticketLevelId + ":stock";
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbcTemplate.query(sql, this::row, args);
    }

    private Map<String, Object> one(String sql, String message, Object... args) {
        List<Map<String, Object>> rows = rows(sql, args);
        if (rows.isEmpty()) throw new ApiException(404, message);
        return rows.get(0);
    }

    private Map<String, Object> row(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Object value = rs.getObject(i);
            if (value instanceof Timestamp timestamp) value = FORMATTER.format(timestamp.toLocalDateTime());
            map.put(meta.getColumnLabel(i), value);
        }
        return map;
    }

    private Object[] args(Object first, List<Long> rest) {
        List<Object> args = new ArrayList<>();
        args.add(first);
        args.addAll(rest);
        return args.toArray();
    }

    private Object[] prepend(Object first, List<Long> rest) {
        return args(first, rest);
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private Long lastId() {
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private Timestamp timeValue(Map<String, Object> payload, String key, String fallback) {
        return Timestamp.valueOf(parseTime(str(payload, key, fallback)));
    }

    private LocalDateTime parseTime(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }

    private List<Integer> parseAisles(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<Integer> result = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) result.add(Integer.parseInt(part.trim()));
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private String str(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Long longValue(Map<String, Object> payload, String key, Long fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        return Long.valueOf(String.valueOf(value));
    }

    private int intValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        return new BigDecimal(String.valueOf(value)).intValue();
    }

    private BigDecimal decimalValue(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return new BigDecimal(value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value));
    }

    private boolean boolValue(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        if (value == null) return fallback;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) map.put(String.valueOf(values[i]), values[i + 1]);
        return map;
    }
}
