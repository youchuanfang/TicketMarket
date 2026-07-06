package com.ticketmarket.service;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.model.PerformanceCard;
import com.ticketmarket.model.TicketLevel;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@DependsOn("databaseSchemaInitializer")
public class PersistentPerformanceService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final DemoDataService demoDataService;

    public PersistentPerformanceService(JdbcTemplate jdbcTemplate, DemoDataService demoDataService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
        this.demoDataService = demoDataService;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from performance where deleted = 0", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        for (PerformanceCard card : demoDataService.adminPerformances()) {
            Long id = insertPerformance(toPayload(card), "PUBLISHED");
            List<Map<String, Object>> blocks = card.getDetailBlocks() == null ? List.of() : card.getDetailBlocks();
            for (int i = 0; i < blocks.size(); i++) {
                Map<String, Object> block = blocks.get(i);
                createDetailBlock(id, map(
                        "blockType", normalizeBlockType(str(block, "type", "PARAGRAPH")),
                        "title", "TITLE".equals(normalizeBlockType(str(block, "type", "PARAGRAPH"))) ? str(block, "content", "") : "",
                        "content", str(block, "content", ""),
                        "imagePath", "IMAGE".equals(normalizeBlockType(str(block, "type", "PARAGRAPH"))) ? str(block, "content", "") : "",
                        "sortOrder", i + 1
                ));
            }
        }
    }

    public List<PerformanceCard> adminPerformances() {
        repairStoredImagePaths(null);
        return jdbcTemplate.query("select * from performance where deleted = 0 order by updated_at desc, id desc", this::mapPerformance);
    }

    public List<PerformanceCard> publicPerformances() {
        repairStoredImagePaths(null);
        return jdbcTemplate.query("select * from performance where deleted = 0 and publish_status = 'PUBLISHED' order by start_time asc, id desc", this::mapPerformance);
    }

    public PerformanceCard adminPerformance(Long id) {
        repairStoredImagePaths(id);
        PerformanceCard card = jdbcClient.sql("select * from performance where id = ? and deleted = 0")
                .param(id)
                .query(this::mapPerformance)
                .optional()
                .orElseThrow(() -> new ApiException(404, "演出不存在"));
        card.setDetailBlocks(detailBlocks(id));
        return card;
    }

    public PerformanceCard publicPerformance(Long id) {
        repairStoredImagePaths(id);
        PerformanceCard card = jdbcClient.sql("select * from performance where id = ? and deleted = 0 and publish_status = 'PUBLISHED'")
                .param(id)
                .query(this::mapPerformance)
                .optional()
                .orElseThrow(() -> new ApiException(404, "演出不存在或未发布"));
        card.setDetailBlocks(detailBlocks(id));
        return card;
    }

    @Transactional
    public PerformanceCard createPerformance(Map<String, Object> payload) {
        Long id = insertPerformance(payload, str(payload, "publishStatus", "DRAFT"));
        replaceDetailBlocks(id, blocks(payload));
        syncPublishingResources(id, payload);
        return adminPerformance(id);
    }

    @Transactional
    public PerformanceCard updatePerformance(Long id, Map<String, Object> payload) {
        ensureExists(id);
        jdbcTemplate.update("""
                update performance set
                  title=?, subtitle=?, category_id=?, category_name=?, city_id=?, city_name=?,
                  venue_id=?, venue_name=?, address=?, poster_path=?, banner_path=?, detail_image_path=?,
                  price_min=?, price_max=?, summary=?, introduction=?, detail_content=?, artist_intro=?,
                  venue_intro=?, purchase_notice=?, refund_notice=?, entry_notice=?, service_tags=?,
                  purchase_mode=?, publish_status=?, status=?, start_time=?, updated_at=now()
                where id=? and deleted=0
                """,
                str(payload, "title", "未命名演出"),
                str(payload, "subtitle", ""),
                longValue(payload, "categoryId", 0L),
                str(payload, "categoryName", ""),
                longValue(payload, "cityId", 0L),
                str(payload, "city", str(payload, "cityName", "")),
                longValue(payload, "venueId", null),
                str(payload, "venue", str(payload, "venueName", "")),
                str(payload, "address", ""),
                persistImagePath(str(payload, "poster", str(payload, "posterPath", ""))),
                persistImagePath(str(payload, "banner", str(payload, "bannerPath", str(payload, "poster", "")))),
                persistImagePath(str(payload, "detailImage", str(payload, "detailImagePath", ""))),
                decimalValue(payload, "priceMin", BigDecimal.ZERO),
                decimalValue(payload, "priceMax", BigDecimal.ZERO),
                str(payload, "summary", ""),
                str(payload, "intro", str(payload, "introduction", "")),
                str(payload, "detailContent", ""),
                str(payload, "artistInfo", str(payload, "artistIntro", "")),
                str(payload, "venueIntro", ""),
                str(payload, "purchaseNotice", ""),
                str(payload, "refundRule", str(payload, "refundNotice", "")),
                str(payload, "entryRule", str(payload, "entryNotice", "")),
                serviceTags(payload.get("tags"), str(payload, "tagsText", str(payload, "serviceTags", ""))),
                str(payload, "saleMode", str(payload, "purchaseMode", "SELECTABLE")),
                str(payload, "publishStatus", "DRAFT"),
                str(payload, "saleStatus", str(payload, "status", "COMING_SOON")),
                timeValue(payload, "startTime", null),
                id
        );
        if (payload.containsKey("detailBlocks")) {
            replaceDetailBlocks(id, blocks(payload));
        }
        syncPublishingResources(id, payload);
        return adminPerformance(id);
    }

    public PerformanceCard publish(Long id) {
        jdbcTemplate.update("update performance set publish_status='PUBLISHED', updated_at=now() where id=? and deleted=0", id);
        return adminPerformance(id);
    }

    public PerformanceCard offline(Long id) {
        jdbcTemplate.update("update performance set publish_status='OFFLINE', status='LOCKED', updated_at=now() where id=? and deleted=0", id);
        return adminPerformance(id);
    }

    public void deletePerformance(Long id) {
        jdbcTemplate.update("update performance set deleted=1, publish_status='OFFLINE', updated_at=now() where id=? and deleted=0", id);
    }

    public List<Map<String, Object>> detailBlocks(Long performanceId) {
        return jdbcTemplate.query("""
                select * from performance_detail_block
                where performance_id = ? and deleted = 0
                order by sort_order asc, id asc
                """, (rs, rowNum) -> detailBlock(rs), performanceId);
    }

    public Map<String, Object> createDetailBlock(Long performanceId, Map<String, Object> payload) {
        ensureExists(performanceId);
        Integer nextOrder = jdbcTemplate.queryForObject(
                "select coalesce(max(sort_order), 0) + 1 from performance_detail_block where performance_id=? and deleted=0",
                Integer.class,
                performanceId
        );
        jdbcTemplate.update("""
                insert into performance_detail_block
                (performance_id, block_type, title, content, image_path, sort_order, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, now(), now(), 0)
                """,
                performanceId,
                normalizeBlockType(str(payload, "blockType", str(payload, "type", "PARAGRAPH"))),
                str(payload, "title", ""),
                "IMAGE".equals(normalizeBlockType(str(payload, "blockType", str(payload, "type", "PARAGRAPH"))))
                        ? persistImagePath(str(payload, "content", str(payload, "imagePath", "")))
                        : str(payload, "content", ""),
                persistImagePath(str(payload, "imagePath", str(payload, "content", ""))),
                intValue(payload, "sortOrder", nextOrder == null ? 1 : nextOrder)
        );
        Long id = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        return detailBlockById(id);
    }

    public Map<String, Object> updateDetailBlock(Long blockId, Map<String, Object> payload) {
        jdbcTemplate.update("""
                update performance_detail_block
                set block_type=?, title=?, content=?, image_path=?, sort_order=?, updated_at=now()
                where id=? and deleted=0
                """,
                normalizeBlockType(str(payload, "blockType", str(payload, "type", "PARAGRAPH"))),
                str(payload, "title", ""),
                "IMAGE".equals(normalizeBlockType(str(payload, "blockType", str(payload, "type", "PARAGRAPH"))))
                        ? persistImagePath(str(payload, "content", str(payload, "imagePath", "")))
                        : str(payload, "content", ""),
                persistImagePath(str(payload, "imagePath", str(payload, "content", ""))),
                intValue(payload, "sortOrder", 1),
                blockId
        );
        return detailBlockById(blockId);
    }

    public void deleteDetailBlock(Long blockId) {
        jdbcTemplate.update("update performance_detail_block set deleted=1, updated_at=now() where id=?", blockId);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> reorderDetailBlocks(Long performanceId, Map<String, Object> payload) {
        Object idsValue = payload.get("ids");
        if (!(idsValue instanceof List<?> ids)) {
            throw new ApiException(400, "缺少排序 ID");
        }
        for (int i = 0; i < ids.size(); i++) {
            jdbcTemplate.update(
                    "update performance_detail_block set sort_order=?, updated_at=now() where id=? and performance_id=? and deleted=0",
                    i + 1,
                    Long.valueOf(String.valueOf(ids.get(i))),
                    performanceId
            );
        }
        return detailBlocks(performanceId);
    }

    private Long insertPerformance(Map<String, Object> payload, String publishStatus) {
        jdbcTemplate.update("""
                insert into performance
                (title, subtitle, category_id, category_name, city_id, city_name, venue_id, venue_name, address,
                 poster_path, banner_path, detail_image_path, price_min, price_max, summary, introduction,
                 detail_content, artist_intro, venue_intro, purchase_notice, refund_notice, entry_notice,
                 service_tags, purchase_mode, publish_status, status, start_time, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 0)
                """,
                str(payload, "title", "未命名演出"),
                str(payload, "subtitle", ""),
                longValue(payload, "categoryId", 0L),
                str(payload, "categoryName", ""),
                longValue(payload, "cityId", 0L),
                str(payload, "city", str(payload, "cityName", "")),
                longValue(payload, "venueId", null),
                str(payload, "venue", str(payload, "venueName", "")),
                str(payload, "address", ""),
                persistImagePath(str(payload, "poster", str(payload, "posterPath", ""))),
                persistImagePath(str(payload, "banner", str(payload, "bannerPath", str(payload, "poster", "")))),
                persistImagePath(str(payload, "detailImage", str(payload, "detailImagePath", ""))),
                decimalValue(payload, "priceMin", BigDecimal.ZERO),
                decimalValue(payload, "priceMax", BigDecimal.ZERO),
                str(payload, "summary", ""),
                str(payload, "intro", str(payload, "introduction", "")),
                str(payload, "detailContent", ""),
                str(payload, "artistInfo", str(payload, "artistIntro", "")),
                str(payload, "venueIntro", ""),
                str(payload, "purchaseNotice", ""),
                str(payload, "refundRule", str(payload, "refundNotice", "")),
                str(payload, "entryRule", str(payload, "entryNotice", "")),
                serviceTags(payload.get("tags"), str(payload, "tagsText", str(payload, "serviceTags", ""))),
                str(payload, "saleMode", str(payload, "purchaseMode", "SELECTABLE")),
                publishStatus,
                str(payload, "saleStatus", str(payload, "status", "COMING_SOON")),
                timeValue(payload, "startTime", null)
        );
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private void replaceDetailBlocks(Long performanceId, List<Map<String, Object>> blocks) {
        jdbcTemplate.update("update performance_detail_block set deleted=1, updated_at=now() where performance_id=? and deleted=0", performanceId);
        for (int i = 0; i < blocks.size(); i++) {
            Map<String, Object> block = blocks.get(i);
            createDetailBlock(performanceId, map(
                    "blockType", normalizeBlockType(str(block, "blockType", str(block, "type", "PARAGRAPH"))),
                    "title", str(block, "title", ""),
                    "content", str(block, "content", ""),
                    "imagePath", str(block, "imagePath", str(block, "content", "")),
                    "sortOrder", i + 1
            ));
        }
    }

    private void syncPublishingResources(Long performanceId, Map<String, Object> payload) {
        List<String> dates = publishingDates(payload);
        List<Map<String, Object>> levels = publishingTicketLevels(payload);
        if (dates.isEmpty() || levels.isEmpty()) return;

        Long venueId = ensurePublishingVenue(payload);
        jdbcTemplate.update("update performance set venue_id=? where id=? and deleted=0", venueId, performanceId);

        Map<String, Long> areaIds = new LinkedHashMap<>();
        for (int i = 0; i < levels.size(); i++) {
            Map<String, Object> level = levels.get(i);
            String areaName = publishingAreaName(level, venueId);
            Long areaId = ensurePublishingArea(venueId, areaName, str(level, "areaType", "SEATED"), str(level, "name", "标准票"), i + 1);
            areaIds.put(areaName, areaId);
            ensureAreaSeats(venueId, areaId, intValue(level, "totalStock", 0), areaName);
        }

        for (String startTime : dates) {
            Long sessionId = ensurePublishingSession(performanceId, venueId, payload, startTime);
            for (Map<String, Object> level : levels) {
                Long areaId = areaIds.get(publishingAreaName(level, venueId));
                ensurePublishingTicketLevel(sessionId, areaId, level);
            }
            ensurePublishingBatch(sessionId, payload, startTime, levels);
            ensureSessionSeats(sessionId, venueId);
        }
    }

    private List<String> publishingDates(Map<String, Object> payload) {
        List<String> values = new ArrayList<>();
        addDate(values, str(payload, "startTime", ""));
        Object sessionDates = payload.get("sessionDates");
        if (sessionDates instanceof List<?> list) {
            for (Object value : list) addDate(values, String.valueOf(value));
        }
        for (String line : str(payload, "sessionDatesText", "").split("\\R")) {
            addDate(values, line);
        }
        return values.stream().distinct().toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> publishingTicketLevels(Map<String, Object> payload) {
        Object raw = payload.get("quickTicketLevels");
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(item -> intValue(item, "price", 0) > 0 && intValue(item, "totalStock", 0) > 0)
                .toList();
    }

    private void addDate(List<String> values, String value) {
        String normalized = normalizeDateTime(value);
        if (!normalized.isBlank()) values.add(normalized);
    }

    private String normalizeDateTime(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) return "";
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) return text + " 19:30:00";
        if (text.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}")) return text + ":00";
        return text;
    }

    private Long ensurePublishingVenue(Map<String, Object> payload) {
        Long venueId = longValue(payload, "venueId", null);
        if (venueId != null && exists("select count(*) from venue where id=? and deleted=0", venueId)) {
            return venueId;
        }
        String name = str(payload, "venue", str(payload, "venueName", "临时场馆"));
        String city = str(payload, "city", str(payload, "cityName", "上海"));
        String address = str(payload, "address", "待完善地址");
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from venue
                where deleted=0 and name=? and city_name=?
                order by id limit 1
                """, Long.class, name, city);
        if (!existing.isEmpty()) return existing.get(0);
        String venueType = inferVenueType(name, str(payload, "categoryName", ""), str(payload, "saleMode", ""));
        jdbcTemplate.update("""
                insert into venue
                (city_id, city_name, name, address, intro, description, venue_type, stage_label, capacity, status, created_at, updated_at, deleted)
                values (0, ?, ?, ?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """, city, name, address, str(payload, "venueIntro", ""), str(payload, "venueIntro", ""),
                venueType, "CINEMA".equals(venueType) ? "银幕" : "舞台", venueCapacity(venueType));
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private String inferVenueType(String venueName, String categoryName, String saleMode) {
        String text = (venueName + " " + categoryName + " " + saleMode).toLowerCase(Locale.ROOT);
        if (text.contains("影院") || text.contains("影城") || text.contains("电影")) return "CINEMA";
        if (text.contains("体育") || text.contains("鸟巢") || text.contains("演唱会") || text.contains("内场")) return "STADIUM";
        return "THEATER";
    }

    private int venueCapacity(String venueType) {
        return switch (venueType) {
            case "STADIUM" -> 5000;
            case "CINEMA" -> 200;
            default -> 800;
        };
    }

    private Long ensurePublishingArea(Long venueId, String name, String type, String levelName, int sortOrder) {
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from venue_area
                where venue_id=? and name=? and deleted=0
                order by id limit 1
                """, Long.class, venueId, name);
        if (!existing.isEmpty()) return existing.get(0);
        jdbcTemplate.update("""
                insert into venue_area
                (venue_id, name, area_type, default_ticket_level, sort_order, color, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, 'ENABLED', now(), now(), 0)
                """, venueId, name, type, levelName, sortOrder, "STANDING".equals(type) ? "#ff6b6b" : "#74c0fc");
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private String publishingAreaName(Map<String, Object> level, Long venueId) {
        String type = str(level, "areaType", "SEATED");
        String venueType = jdbcTemplate.queryForObject("select venue_type from venue where id=?", String.class, venueId);
        if ("STADIUM".equals(venueType)) return "STANDING".equals(type) ? "内场" : "看台";
        if ("CINEMA".equals(venueType)) return "座位区";
        return "座位区";
    }

    private void ensureAreaSeats(Long venueId, Long areaId, int totalStock, String areaName) {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from seat where venue_id=? and area_id=? and deleted=0", Integer.class, venueId, areaId);
        int target = Math.max(1, Math.min(totalStock, 2000));
        if (existing != null && existing >= target) return;
        int start = existing == null ? 0 : existing;
        for (int i = start; i < target; i++) {
            int row = (i / 20) + 1;
            int number = (i % 20) + 1;
            String label = areaName + "-" + row + "排" + number + "座";
            jdbcTemplate.update("""
                    insert into seat
                    (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                    values (?, ?, ?, ?, ?, ?, ?, 0, 0, 'AVAILABLE', now(), now(), 0)
                    """, venueId, areaId, String.valueOf(row), String.valueOf(number), label,
                    40 + (number - 1) * 28, 60 + (row - 1) * 26);
        }
    }

    private Long ensurePublishingSession(Long performanceId, Long venueId, Map<String, Object> payload, String startTime) {
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from performance_session
                where performance_id=? and start_time=? and deleted=0
                order by id limit 1
                """, Long.class, performanceId, Timestamp.valueOf(LocalDateTime.parse(startTime, FORMATTER)));
        String sessionName = str(payload, "title", "演出") + " " + startTime.substring(5, 16);
        Timestamp saleStart = timeFromText(str(payload, "quickSaleStartTime", str(payload, "startTime", startTime)));
        Timestamp lockTime = timeFromText(str(payload, "quickLockTime", ""));
        if (lockTime == null) lockTime = Timestamp.valueOf(LocalDateTime.parse(startTime, FORMATTER).minusHours(1));
        Timestamp entryTime = Timestamp.valueOf(LocalDateTime.parse(startTime, FORMATTER).minusHours(1));
        Timestamp endTime = Timestamp.valueOf(LocalDateTime.parse(startTime, FORMATTER).plusHours(2));
        String mode = str(payload, "saleMode", str(payload, "purchaseMode", "AUTO_ALLOCATE"));
        if (!existing.isEmpty()) {
            Long id = existing.get(0);
            jdbcTemplate.update("""
                    update performance_session
                    set venue_id=?, session_name=?, hall_name=?, sale_start_time=?, lock_time=?, entry_time=?,
                        end_time=?, sale_mode=?, purchase_mode=?, status='SCHEDULED', updated_at=now()
                    where id=? and deleted=0
                    """, venueId, sessionName, sessionName, saleStart, lockTime, entryTime, endTime, mode, mode, id);
            return id;
        }
        jdbcTemplate.update("""
                insert into performance_session
                (performance_id, venue_id, session_name, hall_name, sale_start_time, lock_time, entry_time, start_time, end_time,
                 sale_mode, purchase_mode, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED', now(), now(), 0)
                """, performanceId, venueId, sessionName, sessionName, saleStart, lockTime, entryTime,
                Timestamp.valueOf(LocalDateTime.parse(startTime, FORMATTER)), endTime, mode, mode);
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private void ensurePublishingTicketLevel(Long sessionId, Long areaId, Map<String, Object> level) {
        String name = str(level, "name", "标准票");
        BigDecimal price = decimalValue(level, "price", BigDecimal.ZERO);
        int total = intValue(level, "totalStock", 0);
        int released = intValue(level, "releasedStock", total);
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from ticket_level
                where session_id=? and (name=? or price=?) and deleted=0
                order by case when name=? then 0 else 1 end, id limit 1
                """, Long.class, sessionId, name, price, name);
        if (!existing.isEmpty()) {
            jdbcTemplate.update("""
                    update ticket_level
                    set name=?, area_id=?, price=?, total_stock=?, released_stock=?, unreleased_stock=?, status='ENABLED', updated_at=now()
                    where id=? and deleted=0
                    """, name, areaId, price, total, released, Math.max(0, total - released), existing.get(0));
            return;
        }
        jdbcTemplate.update("""
                insert into ticket_level
                (session_id, name, area_id, price, total_stock, released_stock, unreleased_stock, sold_stock,
                 locked_stock, refunded_stock, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 'ENABLED', now(), now(), 0)
                """, sessionId, name, areaId, price, total, released, Math.max(0, total - released));
    }

    private void ensurePublishingBatch(Long sessionId, Map<String, Object> payload, String startTime, List<Map<String, Object>> levels) {
        int releaseQuantity = intValue(payload, "quickBatchReleaseQuantity", 0);
        if (releaseQuantity <= 0) {
            releaseQuantity = levels.stream().mapToInt(item -> intValue(item, "releasedStock", intValue(item, "totalStock", 0))).sum();
        }
        Timestamp saleStart = timeFromText(str(payload, "quickSaleStartTime", str(payload, "startTime", startTime)));
        Timestamp lockTime = timeFromText(str(payload, "quickLockTime", ""));
        if (lockTime == null) lockTime = Timestamp.valueOf(LocalDateTime.parse(startTime, FORMATTER).minusHours(1));
        List<Long> existing = jdbcTemplate.queryForList("""
                select id from sale_batch
                where session_id=? and deleted=0
                order by id limit 1
                """, Long.class, sessionId);
        if (!existing.isEmpty()) {
            jdbcTemplate.update("""
                    update sale_batch
                    set name='第一批开售', batch_name='第一批开售', sale_start_time=?, lock_time=?,
                        open_mode='QUANTITY', release_type='QUANTITY', open_stock=?, release_quantity=?,
                        limit_per_user=2, purchase_limit=2, queue_enabled=1, enable_queue=1, updated_at=now()
                    where id=? and deleted=0
                    """, saleStart, lockTime, releaseQuantity, releaseQuantity, existing.get(0));
            return;
        }
        jdbcTemplate.update("""
                insert into sale_batch
                (session_id, name, batch_name, sale_start_time, lock_time, open_mode, release_type, open_stock,
                 release_quantity, release_ratio, allow_return_current_round, allow_return_during_sale,
                 limit_per_user, purchase_limit, queue_enabled, enable_queue, status, created_at, updated_at, deleted)
                values (?, '第一批开售', '第一批开售', ?, ?, 'QUANTITY', 'QUANTITY', ?, ?, 0, 1, 1, 2, 2, 1, 1, 'NOT_STARTED', now(), now(), 0)
                """, sessionId, saleStart, lockTime, releaseQuantity, releaseQuantity);
    }

    private void ensureSessionSeats(Long sessionId, Long venueId) {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from session_seat where session_id=?", Integer.class, sessionId);
        if (existing != null && existing > 0) {
            jdbcTemplate.update("""
                    update session_seat ss
                    join ticket_level tl on tl.session_id=ss.session_id and tl.area_id=ss.area_id and tl.deleted=0
                    set ss.ticket_level_id=tl.id, ss.updated_at=now()
                    where ss.session_id=? and ss.status in ('AVAILABLE', 'UNRELEASED', 'DISABLED')
                    """, sessionId);
            return;
        }
        jdbcTemplate.update("""
                insert into session_seat
                (session_id, seat_id, venue_id, area_id, ticket_level_id, batch_id, status, lock_user_id,
                 lock_expire_time, seat_label, x, y, created_at, updated_at)
                select ?, s.id, s.venue_id, s.area_id, tl.id, null,
                       case when s.is_disabled=1 then 'DISABLED' else 'AVAILABLE' end,
                       null, null, s.seat_label, s.x, s.y, now(), now()
                from seat s
                left join ticket_level tl on tl.session_id=? and tl.area_id=s.area_id and tl.deleted=0
                where s.venue_id=? and s.deleted=0
                """, sessionId, sessionId, venueId);
    }

    private PerformanceCard mapPerformance(ResultSet rs, int rowNum) throws SQLException {
        PerformanceCard card = new PerformanceCard();
        card.setId(rs.getLong("id"));
        card.setTitle(rs.getString("title"));
        card.setSubtitle(rs.getString("subtitle"));
        card.setCategoryName(rs.getString("category_name"));
        card.setCategoryCode(categoryCode(rs.getString("category_name")));
        card.setVenueId(longOrNull(rs, "venue_id"));
        card.setCity(rs.getString("city_name"));
        card.setVenue(rs.getString("venue_name"));
        card.setAddress(rs.getString("address"));
        card.setStartTime(format(rs.getTimestamp("start_time")));
        card.setPriceMin(rs.getBigDecimal("price_min") == null ? 0 : rs.getBigDecimal("price_min").intValue());
        card.setPriceMax(rs.getBigDecimal("price_max") == null ? 0 : rs.getBigDecimal("price_max").intValue());
        card.setPoster(rs.getString("poster_path"));
        card.setBanner(rs.getString("banner_path"));
        card.setDetailImage(rs.getString("detail_image_path"));
        card.setSaleStatus(rs.getString("status"));
        card.setSaleMode(rs.getString("purchase_mode"));
        card.setPublishStatus(rs.getString("publish_status"));
        card.setTags(splitTags(rs.getString("service_tags")));
        card.setSummary(rs.getString("summary"));
        card.setIntro(rs.getString("introduction"));
        card.setArtistInfo(rs.getString("artist_intro"));
        card.setVenueIntro(rs.getString("venue_intro"));
        card.setPurchaseNotice(rs.getString("purchase_notice"));
        card.setRefundRule(rs.getString("refund_notice"));
        card.setEntryRule(rs.getString("entry_notice"));
        card.setTicketLevels(List.of(new TicketLevel(card.getId() * 1000 + 1, "标准票", "", card.getPriceMin(), 0)));
        return card;
    }

    private Map<String, Object> detailBlock(ResultSet rs) throws SQLException {
        String blockType = rs.getString("block_type");
        String content = "IMAGE".equals(blockType) ? rs.getString("image_path") : rs.getString("content");
        return map(
                "id", rs.getLong("id"),
                "performanceId", rs.getLong("performance_id"),
                "blockType", blockType,
                "type", "TITLE".equals(blockType) ? "HEADING" : blockType,
                "title", rs.getString("title"),
                "content", content,
                "imagePath", rs.getString("image_path"),
                "sortOrder", rs.getInt("sort_order")
        );
    }

    private Map<String, Object> detailBlockById(Long blockId) {
        return jdbcClient.sql("select * from performance_detail_block where id=? and deleted=0")
                .param(blockId)
                .query((rs, rowNum) -> detailBlock(rs))
                .optional()
                .orElseThrow(() -> new ApiException(404, "详情块不存在"));
    }

    private void ensureExists(Long id) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from performance where id=? and deleted=0", Integer.class, id);
        if (count == null || count == 0) {
            throw new ApiException(404, "演出不存在");
        }
    }

    private List<Map<String, Object>> blocks(Map<String, Object> payload) {
        Object value = payload.get("detailBlocks");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> block = (Map<String, Object>) item;
                    return block;
                })
                .toList();
    }

    private Map<String, Object> toPayload(PerformanceCard card) {
        return map(
                "title", card.getTitle(),
                "subtitle", card.getSubtitle(),
                "categoryName", card.getCategoryName(),
                "city", card.getCity(),
                "venue", card.getVenue(),
                "venueId", card.getVenueId(),
                "address", card.getAddress(),
                "startTime", card.getStartTime(),
                "priceMin", card.getPriceMin(),
                "priceMax", card.getPriceMax(),
                "poster", card.getPoster(),
                "banner", card.getBanner(),
                "detailImage", card.getDetailImage(),
                "saleStatus", card.getSaleStatus(),
                "saleMode", card.getSaleMode(),
                "tags", card.getTags(),
                "summary", card.getSummary(),
                "intro", card.getIntro(),
                "artistInfo", card.getArtistInfo(),
                "venueIntro", card.getVenueIntro(),
                "purchaseNotice", card.getPurchaseNotice(),
                "refundRule", card.getRefundRule(),
                "entryRule", card.getEntryRule()
        );
    }

    private String normalizeBlockType(String type) {
        if ("HEADING".equals(type)) return "TITLE";
        if ("TITLE".equals(type) || "IMAGE".equals(type) || "PARAGRAPH".equals(type)) return type;
        return "PARAGRAPH";
    }

    private String serviceTags(Object value, String fallback) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(item -> !item.isBlank()).collect(Collectors.joining(","));
        }
        return fallback == null ? "" : fallback;
    }

    private List<String> splitTags(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String categoryCode(String categoryName) {
        if (categoryName == null) return "";
        return switch (categoryName) {
            case "演唱会" -> "concert";
            case "音乐会" -> "music";
            case "话剧歌剧" -> "drama";
            case "体育赛事" -> "sports";
            case "儿童亲子" -> "family";
            case "展览休闲" -> "exhibition";
            case "曲苑杂坛" -> "quyi";
            case "舞蹈芭蕾" -> "dance";
            case "二次元" -> "anime";
            case "旅游展览" -> "travel";
            case "音乐节" -> "festival";
            default -> categoryName;
        };
    }

    private void repairStoredImagePaths(Long performanceId) {
        String performanceWhere = performanceId == null ? "deleted=0" : "id=? and deleted=0";
        Object[] performanceArgs = performanceId == null ? new Object[]{} : new Object[]{performanceId};
        List<Map<String, Object>> performances = jdbcTemplate.queryForList(
                "select id, poster_path, banner_path, detail_image_path from performance where " + performanceWhere,
                performanceArgs);
        for (Map<String, Object> row : performances) {
            Long id = ((Number) row.get("id")).longValue();
            String poster = repairImageValue(Objects.toString(row.get("poster_path"), ""));
            String banner = repairImageValue(Objects.toString(row.get("banner_path"), ""));
            String detailImage = repairImageValue(Objects.toString(row.get("detail_image_path"), ""));
            if (!Objects.equals(poster, Objects.toString(row.get("poster_path"), ""))
                    || !Objects.equals(banner, Objects.toString(row.get("banner_path"), ""))
                    || !Objects.equals(detailImage, Objects.toString(row.get("detail_image_path"), ""))) {
                jdbcTemplate.update("""
                        update performance set poster_path=?, banner_path=?, detail_image_path=?, updated_at=now()
                        where id=? and deleted=0
                        """, poster, banner, detailImage, id);
            }
        }

        String blockWhere = performanceId == null ? "deleted=0 and image_path is not null and image_path <> ''" : "performance_id=? and deleted=0 and image_path is not null and image_path <> ''";
        Object[] blockArgs = performanceId == null ? new Object[]{} : new Object[]{performanceId};
        List<Map<String, Object>> blocks = jdbcTemplate.queryForList(
                "select id, image_path from performance_detail_block where " + blockWhere,
                blockArgs);
        for (Map<String, Object> row : blocks) {
            String original = Objects.toString(row.get("image_path"), "");
            String repaired = repairImageValue(original);
            if (!Objects.equals(repaired, original)) {
                jdbcTemplate.update("update performance_detail_block set image_path=?, updated_at=now() where id=? and deleted=0",
                        repaired, ((Number) row.get("id")).longValue());
            }
        }
    }

    private String repairImageValue(String value) {
        String repaired = persistImagePath(value);
        return repaired == null ? "" : repaired;
    }

    private String persistImagePath(String value) {
        String path = value == null ? "" : value.trim();
        if ((path.startsWith("\"") && path.endsWith("\"")) || (path.startsWith("'") && path.endsWith("'"))) {
            path = path.substring(1, path.length() - 1);
        }
        if (path.isBlank() || path.startsWith("/uploads/") || path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        try {
            Path source = Paths.get(path).normalize();
            if (!Files.isRegularFile(source)) return path;
            String originalName = source.getFileName().toString();
            String suffix = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) suffix = originalName.substring(dot).toLowerCase(Locale.ROOT);
            if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg").contains(suffix)) suffix = ".jpg";
            Path dir = Paths.get("..", "uploads", "admin").normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID().toString().replace("-", "") + suffix);
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/admin/" + target.getFileName();
        } catch (IOException | RuntimeException ignored) {
            return path;
        }
    }

    private Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String format(Timestamp timestamp) {
        return timestamp == null ? "" : FORMATTER.format(timestamp.toLocalDateTime());
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

    private BigDecimal decimalValue(Map<String, Object> payload, String key, BigDecimal fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        return new BigDecimal(String.valueOf(value));
    }

    private Timestamp timeValue(Map<String, Object> payload, String key, Timestamp fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        String text = String.valueOf(value);
        if (text.length() == 16) {
            text = text + ":00";
        }
        return Timestamp.valueOf(LocalDateTime.parse(text, FORMATTER));
    }

    private Timestamp timeFromText(String value) {
        String normalized = normalizeDateTime(value);
        if (normalized.isBlank()) return null;
        return Timestamp.valueOf(LocalDateTime.parse(normalized, FORMATTER));
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
