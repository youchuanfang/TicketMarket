package com.ticketmarket.service;

import com.ticketmarket.common.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class Phase3ResourceService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate redisTemplate;
    private final AtomicLong venueId = new AtomicLong(3);
    private final AtomicLong areaId = new AtomicLong(10);
    private final AtomicLong seatId = new AtomicLong(10000);
    private final AtomicLong sessionSeatId = new AtomicLong(30000);
    private final AtomicLong sessionId = new AtomicLong(1003);
    private final AtomicLong ticketLevelId = new AtomicLong(2004);
    private final AtomicLong batchId = new AtomicLong(3003);
    private final AtomicLong poolId = new AtomicLong(4003);

    private final List<Map<String, Object>> venues = new ArrayList<>();
    private final List<Map<String, Object>> areas = new ArrayList<>();
    private final List<Map<String, Object>> seats = new ArrayList<>();
    private final List<Map<String, Object>> sessionSeats = new ArrayList<>();
    private final List<Map<String, Object>> sessions = new ArrayList<>();
    private final List<Map<String, Object>> ticketLevels = new ArrayList<>();
    private final List<Map<String, Object>> saleBatches = new ArrayList<>();
    private final List<Map<String, Object>> batchTicketLevels = new ArrayList<>();
    private final List<Map<String, Object>> batchSeats = new ArrayList<>();
    private final List<Map<String, Object>> stockPool = new ArrayList<>();

    public Phase3ResourceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        seedVenues();
        seedAreas();
        seedSeats();
        seedSessions();
        seedTicketLevels();
        seedBatches();
        initSessionSeats(1001L);
        initSessionSeats(1002L);
        initSessionSeats(1003L);
    }

    public List<Map<String, Object>> venues() {
        return copyList(venues);
    }

    public List<Map<String, Object>> hotVenues() {
        return venues.stream().filter(item -> "ENABLED".equals(item.get("status"))).limit(6).map(this::copy).toList();
    }

    public Map<String, Object> venue(Long id) {
        return copy(find(venues, id, "场馆不存在"));
    }

    public Map<String, Object> createVenue(Map<String, Object> payload) {
        Map<String, Object> venue = map(
                "id", venueId.incrementAndGet(),
                "name", str(payload, "name", "新场馆"),
                "cityId", longValue(payload, "cityId", 1L),
                "cityName", str(payload, "cityName", "上海"),
                "address", str(payload, "address", "待完善地址"),
                "capacity", intValue(payload, "capacity", 0),
                "description", str(payload, "description", "场馆信息待完善"),
                "status", "ENABLED",
                "createdAt", now(),
                "updatedAt", now()
        );
        venues.add(venue);
        return copy(venue);
    }

    public Map<String, Object> updateVenue(Long id, Map<String, Object> payload) {
        Map<String, Object> venue = find(venues, id, "场馆不存在");
        merge(venue, payload, "name", "cityName", "address", "capacity", "description", "status");
        venue.put("updatedAt", now());
        return copy(venue);
    }

    public void deleteVenue(Long id) {
        find(venues, id, "场馆不存在").put("status", "DISABLED");
    }

    public List<Map<String, Object>> areas(Long venueId) {
        return areas.stream().filter(item -> Objects.equals(item.get("venueId"), venueId)).map(this::copy).toList();
    }

    public Map<String, Object> createArea(Long venueId, Map<String, Object> payload) {
        Map<String, Object> area = map(
                "id", areaId.incrementAndGet(),
                "venueId", venueId,
                "areaName", str(payload, "areaName", "新区"),
                "areaType", str(payload, "areaType", "SEATED"),
                "defaultTicketLevel", str(payload, "defaultTicketLevel", "标准票"),
                "sortOrder", intValue(payload, "sortOrder", 1),
                "color", str(payload, "color", "#d9303e"),
                "status", "ENABLED",
                "createdAt", now(),
                "updatedAt", now()
        );
        areas.add(area);
        return copy(area);
    }

    public Map<String, Object> updateArea(Long id, Map<String, Object> payload) {
        Map<String, Object> area = find(areas, id, "区域不存在");
        merge(area, payload, "areaName", "areaType", "defaultTicketLevel", "sortOrder", "color", "status");
        area.put("updatedAt", now());
        return copy(area);
    }

    public void deleteArea(Long id) {
        find(areas, id, "区域不存在").put("status", "DISABLED");
    }

    public List<Map<String, Object>> seats(Long venueId) {
        return seats.stream().filter(item -> Objects.equals(item.get("venueId"), venueId)).map(this::copy).toList();
    }

    public List<Map<String, Object>> generateSeats(Long venueId, Map<String, Object> payload) {
        Long targetAreaId = longValue(payload, "areaId", null);
        if (targetAreaId == null) {
            Map<String, Object> area = createArea(venueId, map(
                    "areaName", str(payload, "areaName", "A区"),
                    "areaType", "SEATED",
                    "defaultTicketLevel", "标准票",
                    "color", str(payload, "areaColor", "#d9303e")
            ));
            targetAreaId = (Long) area.get("id");
        }
        String areaName = String.valueOf(find(areas, targetAreaId, "区域不存在").get("areaName"));
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
                Map<String, Object> seat = map(
                        "id", seatId.incrementAndGet(),
                        "venueId", venueId,
                        "areaId", targetAreaId,
                        "rowNo", String.valueOf(row),
                        "seatNo", String.valueOf(number),
                        "seatLabel", areaName + "-" + row + "排-" + number + "座",
                        "x", startX + (number - 1) * gapX + aisleOffset,
                        "y", startY + (row - rowStart) * gapY,
                        "isAisle", aisles.contains(number),
                        "isDisabled", false,
                        "status", "AVAILABLE",
                        "createdAt", now(),
                        "updatedAt", now()
                );
                seats.add(seat);
                generated.add(copy(seat));
            }
        }
        return generated;
    }

    public Map<String, Object> updateSeat(Long id, Map<String, Object> payload) {
        Map<String, Object> seat = find(seats, id, "座位不存在");
        merge(seat, payload, "x", "y", "isAisle", "isDisabled", "status");
        seat.put("updatedAt", now());
        return copy(seat);
    }

    public void batchSeatDisabled(List<Long> ids, boolean disabled) {
        seats.stream().filter(item -> ids.contains((Long) item.get("id"))).forEach(item -> {
            item.put("isDisabled", disabled);
            item.put("status", disabled ? "DISABLED" : "AVAILABLE");
            item.put("updatedAt", now());
        });
    }

    public List<Map<String, Object>> sessions() {
        return copyList(sessions);
    }

    public List<Map<String, Object>> sessionsByPerformance(Long performanceId) {
        return sessions.stream().filter(item -> Objects.equals(item.get("performanceId"), performanceId)).map(this::copy).toList();
    }

    public Map<String, Object> createSession(Map<String, Object> payload) {
        Map<String, Object> session = map(
                "id", sessionId.incrementAndGet(),
                "performanceId", longValue(payload, "performanceId", 101L),
                "venueId", longValue(payload, "venueId", 1L),
                "sessionName", str(payload, "sessionName", "新增场次"),
                "saleStartTime", str(payload, "saleStartTime", "2026-07-20 10:00:00"),
                "lockTime", str(payload, "lockTime", "2026-08-01 18:00:00"),
                "entryTime", str(payload, "entryTime", "2026-08-18 18:00:00"),
                "startTime", str(payload, "startTime", "2026-08-18 19:30:00"),
                "endTime", str(payload, "endTime", "2026-08-18 22:00:00"),
                "purchaseMode", str(payload, "purchaseMode", "SELECTABLE"),
                "status", "SCHEDULED",
                "createdAt", now(),
                "updatedAt", now()
        );
        sessions.add(session);
        return copy(session);
    }

    public Map<String, Object> updateSession(Long id, Map<String, Object> payload) {
        Map<String, Object> session = find(sessions, id, "场次不存在");
        merge(session, payload, "sessionName", "saleStartTime", "lockTime", "entryTime", "startTime", "endTime", "purchaseMode", "status");
        session.put("updatedAt", now());
        return copy(session);
    }

    public void deleteSession(Long id) {
        find(sessions, id, "场次不存在").put("status", "CANCELLED");
    }

    public List<Map<String, Object>> ticketLevels(Long sessionId) {
        return ticketLevels.stream().filter(item -> Objects.equals(item.get("sessionId"), sessionId)).map(this::copy).toList();
    }

    public Map<String, Object> createTicketLevel(Map<String, Object> payload) {
        Map<String, Object> level = map(
                "id", ticketLevelId.incrementAndGet(),
                "sessionId", longValue(payload, "sessionId", 1001L),
                "name", str(payload, "name", "标准票"),
                "price", decimalValue(payload, "price", "180"),
                "areaId", longValue(payload, "areaId", 1L),
                "totalStock", intValue(payload, "totalStock", 0),
                "releasedStock", intValue(payload, "releasedStock", 0),
                "unreleasedStock", intValue(payload, "unreleasedStock", 0),
                "soldStock", 0,
                "lockedStock", 0,
                "refundedStock", 0,
                "status", "ENABLED",
                "createdAt", now(),
                "updatedAt", now()
        );
        ticketLevels.add(level);
        return copy(level);
    }

    public Map<String, Object> updateTicketLevel(Long id, Map<String, Object> payload) {
        Map<String, Object> level = find(ticketLevels, id, "票档不存在");
        merge(level, payload, "name", "price", "areaId", "totalStock", "releasedStock", "unreleasedStock", "status");
        level.put("updatedAt", now());
        return copy(level);
    }

    public void deleteTicketLevel(Long id) {
        find(ticketLevels, id, "票档不存在").put("status", "DISABLED");
    }

    public List<Map<String, Object>> sessionSeats(Long sessionId) {
        return sessionSeats.stream().filter(item -> Objects.equals(item.get("sessionId"), sessionId)).map(this::copy).toList();
    }

    public List<Map<String, Object>> initSessionSeats(Long sessionId) {
        Map<String, Object> session = find(sessions, sessionId, "场次不存在");
        Long venueId = (Long) session.get("venueId");
        if (sessionSeats.stream().anyMatch(item -> Objects.equals(item.get("sessionId"), sessionId))) {
            return sessionSeats(sessionId);
        }
        List<Map<String, Object>> levels = ticketLevels(sessionId);
        List<Map<String, Object>> venueSeats = seats(venueId);
        for (Map<String, Object> seat : venueSeats) {
            Long areaId = (Long) seat.get("areaId");
            Long levelId = levels.stream()
                    .filter(level -> Objects.equals(level.get("areaId"), areaId))
                    .map(level -> (Long) level.get("id"))
                    .findFirst()
                    .orElse(levels.isEmpty() ? null : (Long) levels.get(0).get("id"));
            Map<String, Object> sessionSeat = map(
                    "id", sessionSeatId.incrementAndGet(),
                    "sessionId", sessionId,
                    "seatId", seat.get("id"),
                    "venueId", venueId,
                    "areaId", areaId,
                    "ticketLevelId", levelId,
                    "batchId", null,
                    "status", Boolean.TRUE.equals(seat.get("isDisabled")) ? "DISABLED" : "AVAILABLE",
                    "lockUserId", null,
                    "lockExpireTime", null,
                    "seatLabel", seat.get("seatLabel"),
                    "x", seat.get("x"),
                    "y", seat.get("y"),
                    "createdAt", now(),
                    "updatedAt", now()
            );
            sessionSeats.add(sessionSeat);
        }
        return sessionSeats(sessionId);
    }

    public void updateSessionSeatStatus(List<Long> ids, String status) {
        sessionSeats.stream().filter(item -> ids.contains((Long) item.get("id"))).forEach(item -> {
            item.put("status", status);
            item.put("updatedAt", now());
        });
    }

    public List<Map<String, Object>> saleBatches() {
        return copyList(saleBatches);
    }

    public Map<String, Object> saleBatch(Long id) {
        return copy(find(saleBatches, id, "售票批次不存在"));
    }

    public Map<String, Object> createSaleBatch(Map<String, Object> payload) {
        Map<String, Object> batch = map(
                "id", batchId.incrementAndGet(),
                "sessionId", longValue(payload, "sessionId", 1001L),
                "batchName", str(payload, "batchName", "新批次"),
                "saleStartTime", str(payload, "saleStartTime", "2026-07-20 10:00:00"),
                "lockTime", str(payload, "lockTime", "2026-08-01 18:00:00"),
                "releaseType", str(payload, "releaseType", "QUANTITY"),
                "releaseQuantity", intValue(payload, "releaseQuantity", 100),
                "releaseRatio", intValue(payload, "releaseRatio", 0),
                "status", str(payload, "status", "NOT_STARTED"),
                "allowReturnDuringSale", boolValue(payload, "allowReturnDuringSale", true),
                "purchaseLimit", intValue(payload, "purchaseLimit", 2),
                "enableQueue", boolValue(payload, "enableQueue", true),
                "createdAt", now(),
                "updatedAt", now()
        );
        saleBatches.add(batch);
        return copy(batch);
    }

    public Map<String, Object> updateSaleBatch(Long id, Map<String, Object> payload) {
        Map<String, Object> batch = find(saleBatches, id, "售票批次不存在");
        merge(batch, payload, "batchName", "saleStartTime", "lockTime", "releaseType", "releaseQuantity", "releaseRatio", "status", "allowReturnDuringSale", "purchaseLimit", "enableQueue");
        batch.put("updatedAt", now());
        return copy(batch);
    }

    public Map<String, Object> changeBatchStatus(Long id, String status) {
        Map<String, Object> batch = find(saleBatches, id, "售票批次不存在");
        batch.put("status", status);
        batch.put("updatedAt", now());
        return copy(batch);
    }

    public Map<String, Object> activeBatch(Long sessionId) {
        return saleBatches.stream()
                .filter(item -> Objects.equals(item.get("sessionId"), sessionId))
                .filter(item -> "SELLING".equals(item.get("status")) || "NOT_STARTED".equals(item.get("status")))
                .findFirst()
                .map(this::copy)
                .orElse(null);
    }

    public Map<String, Object> initRedisStock(Long batchId) {
        Map<String, Object> batch = find(saleBatches, batchId, "售票批次不存在");
        Long sessionId = (Long) batch.get("sessionId");
        List<Map<String, Object>> levels = ticketLevels(sessionId);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> level : levels) {
            String key = redisStockKey(batchId, (Long) level.get("id"));
            int stock = Math.min((Integer) level.get("releasedStock"), (Integer) batch.get("releaseQuantity"));
            redisTemplate.opsForValue().set(key, String.valueOf(stock));
            result.put(key, stock);
        }
        return result;
    }

    public Map<String, Object> batchStockSummary(Long batchId) {
        Map<String, Object> batch = find(saleBatches, batchId, "售票批次不存在");
        Long sessionId = (Long) batch.get("sessionId");
        List<Map<String, Object>> levels = ticketLevels(sessionId);
        List<Map<String, Object>> rows = levels.stream().map(level -> {
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
        return map("batch", copy(batch), "levels", rows);
    }

    public List<Map<String, Object>> stockPool() {
        return copyList(stockPool);
    }

    public List<Map<String, Object>> stockPoolBySession(Long sessionId) {
        return stockPool.stream().filter(item -> Objects.equals(item.get("sessionId"), sessionId)).map(this::copy).toList();
    }

    public Map<String, Object> addStockPool(Map<String, Object> payload) {
        Map<String, Object> row = map(
                "id", poolId.incrementAndGet(),
                "sessionId", longValue(payload, "sessionId", 1001L),
                "ticketLevelId", longValue(payload, "ticketLevelId", 2001L),
                "seatId", longValue(payload, "seatId", null),
                "sourceType", str(payload, "sourceType", "ADMIN_ADD"),
                "stockStatus", "WAITING_RELEASE",
                "availableForNextBatch", boolValue(payload, "availableForNextBatch", true),
                "createdAt", now(),
                "updatedAt", now()
        );
        stockPool.add(row);
        return copy(row);
    }

    public Map<String, Object> releaseStockToBatch(Map<String, Object> payload) {
        Long targetBatchId = longValue(payload, "batchId", null);
        if (targetBatchId == null) {
            throw new ApiException(400, "请选择要释放到的售票批次");
        }
        stockPool.stream()
                .filter(item -> "WAITING_RELEASE".equals(item.get("stockStatus")))
                .filter(item -> Boolean.TRUE.equals(item.get("availableForNextBatch")))
                .forEach(item -> {
                    item.put("stockStatus", "RELEASED");
                    item.put("updatedAt", now());
                });
        return batchStockSummary(targetBatchId);
    }

    @Scheduled(fixedDelay = 30000)
    public void refreshBatchStatus() {
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> batch : saleBatches) {
            LocalDateTime saleStart = parseTime(String.valueOf(batch.get("saleStartTime")));
            LocalDateTime lockTime = parseTime(String.valueOf(batch.get("lockTime")));
            if ("NOT_STARTED".equals(batch.get("status")) && !now.isBefore(saleStart)) {
                batch.put("status", "SELLING");
                initRedisStock((Long) batch.get("id"));
            }
            if ("SELLING".equals(batch.get("status")) && !now.isBefore(lockTime)) {
                batch.put("status", "LOCKED");
            }
        }
    }

    private void seedVenues() {
        venues.add(map("id", 1L, "name", "星河影城一号厅", "cityId", 1L, "cityName", "上海", "address", "浦东新区星河路 18 号", "capacity", 96, "description", "适合电影选座演示的小厅", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        venues.add(map("id", 2L, "name", "城市剧院中厅", "cityId", 2L, "cityName", "杭州", "address", "西湖区文艺路 16 号", "capacity", 384, "description", "适合话剧和音乐会的中型剧院", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        venues.add(map("id", 3L, "name", "海川体育馆", "cityId", 3L, "cityName", "深圳", "address", "南山区滨海大道 188 号", "capacity", 1200, "description", "适合演唱会和体育赛事的大场馆", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
    }

    private void seedAreas() {
        areas.add(map("id", 1L, "venueId", 1L, "areaName", "影厅A区", "areaType", "SEATED", "defaultTicketLevel", "电影票", "sortOrder", 1, "color", "#d9303e", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        areas.add(map("id", 2L, "venueId", 2L, "areaName", "一层池座", "areaType", "SEATED", "defaultTicketLevel", "优选票", "sortOrder", 1, "color", "#177e89", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        areas.add(map("id", 3L, "venueId", 2L, "areaName", "二层楼座", "areaType", "SEATED", "defaultTicketLevel", "看台票", "sortOrder", 2, "color", "#b7791f", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        areas.add(map("id", 4L, "venueId", 3L, "areaName", "A区", "areaType", "SEATED", "defaultTicketLevel", "内场票", "sortOrder", 1, "color", "#d9303e", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        areas.add(map("id", 5L, "venueId", 3L, "areaName", "B区", "areaType", "SEATED", "defaultTicketLevel", "看台票", "sortOrder", 2, "color", "#177e89", "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
    }

    private void seedSeats() {
        generateSeats(1L, map("areaId", 1L, "rowStart", 1, "rowEnd", 8, "seatsPerRow", 12, "startX", 60, "startY", 80, "gapX", 30, "gapY", 30, "aisleAfterSeats", "6"));
        generateSeats(2L, map("areaId", 2L, "rowStart", 1, "rowEnd", 10, "seatsPerRow", 24, "startX", 40, "startY", 80, "gapX", 24, "gapY", 26, "aisleAfterSeats", "8,16"));
        generateSeats(2L, map("areaId", 3L, "rowStart", 11, "rowEnd", 16, "seatsPerRow", 24, "startX", 40, "startY", 360, "gapX", 24, "gapY", 26, "aisleAfterSeats", "8,16"));
        generateSeats(3L, map("areaId", 4L, "rowStart", 1, "rowEnd", 8, "seatsPerRow", 20, "startX", 50, "startY", 90, "gapX", 24, "gapY", 26, "aisleAfterSeats", "10"));
        generateSeats(3L, map("areaId", 5L, "rowStart", 1, "rowEnd", 8, "seatsPerRow", 20, "startX", 50, "startY", 330, "gapX", 24, "gapY", 26, "aisleAfterSeats", "10"));
    }

    private void seedSessions() {
        sessions.add(map("id", 1001L, "performanceId", 102L, "venueId", 2L, "sessionName", "夜航西窗 8月21日晚场", "saleStartTime", "2026-07-20 10:00:00", "lockTime", "2026-08-21 18:00:00", "entryTime", "2026-08-21 18:15:00", "startTime", "2026-08-21 19:00:00", "endTime", "2026-08-21 21:20:00", "purchaseMode", "SELECTABLE", "status", "SCHEDULED", "createdAt", now(), "updatedAt", now()));
        sessions.add(map("id", 1002L, "performanceId", 101L, "venueId", 3L, "sessionName", "星河回声 上海站", "saleStartTime", "2026-07-20 10:00:00", "lockTime", "2026-08-18 18:30:00", "entryTime", "2026-08-18 18:00:00", "startTime", "2026-08-18 19:30:00", "endTime", "2026-08-18 22:00:00", "purchaseMode", "AUTO_ALLOCATE", "status", "SCHEDULED", "createdAt", now(), "updatedAt", now()));
        sessions.add(map("id", 1003L, "performanceId", 201L, "venueId", 1L, "sessionName", "深空旅人 10:30", "saleStartTime", "2026-07-10 10:00:00", "lockTime", "2026-12-31 23:00:00", "entryTime", "2026-07-18 10:00:00", "startTime", "2026-07-18 10:30:00", "endTime", "2026-07-18 12:40:00", "purchaseMode", "SELECTABLE", "status", "SCHEDULED", "createdAt", now(), "updatedAt", now()));
    }

    private void seedTicketLevels() {
        ticketLevels.add(map("id", 2001L, "sessionId", 1001L, "name", "池座优选票", "price", new BigDecimal("380"), "areaId", 2L, "totalStock", 240, "releasedStock", 120, "unreleasedStock", 120, "soldStock", 0, "lockedStock", 0, "refundedStock", 0, "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        ticketLevels.add(map("id", 2002L, "sessionId", 1001L, "name", "楼座惠民票", "price", new BigDecimal("180"), "areaId", 3L, "totalStock", 144, "releasedStock", 60, "unreleasedStock", 84, "soldStock", 0, "lockedStock", 0, "refundedStock", 0, "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        ticketLevels.add(map("id", 2003L, "sessionId", 1002L, "name", "内场票", "price", new BigDecimal("680"), "areaId", 4L, "totalStock", 160, "releasedStock", 80, "unreleasedStock", 80, "soldStock", 0, "lockedStock", 0, "refundedStock", 0, "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
        ticketLevels.add(map("id", 2004L, "sessionId", 1002L, "name", "看台票", "price", new BigDecimal("380"), "areaId", 5L, "totalStock", 160, "releasedStock", 80, "unreleasedStock", 80, "soldStock", 0, "lockedStock", 0, "refundedStock", 0, "status", "ENABLED", "createdAt", now(), "updatedAt", now()));
    }

    private void seedBatches() {
        saleBatches.add(map("id", 3001L, "sessionId", 1001L, "batchName", "第一轮开售", "saleStartTime", "2026-07-20 10:00:00", "lockTime", "2026-08-01 18:00:00", "releaseType", "QUANTITY", "releaseQuantity", 120, "releaseRatio", 0, "status", "SELLING", "allowReturnDuringSale", true, "purchaseLimit", 2, "enableQueue", true, "createdAt", now(), "updatedAt", now()));
        saleBatches.add(map("id", 3002L, "sessionId", 1002L, "batchName", "锁票后库存入池", "saleStartTime", "2026-07-22 10:00:00", "lockTime", "2026-07-30 18:00:00", "releaseType", "QUANTITY", "releaseQuantity", 80, "releaseRatio", 0, "status", "LOCKED", "allowReturnDuringSale", false, "purchaseLimit", 2, "enableQueue", true, "createdAt", now(), "updatedAt", now()));
        saleBatches.add(map("id", 3003L, "sessionId", 1001L, "batchName", "第二轮重新开放", "saleStartTime", "2026-08-05 10:00:00", "lockTime", "2026-08-20 18:00:00", "releaseType", "RATIO", "releaseQuantity", 0, "releaseRatio", 30, "status", "NOT_STARTED", "allowReturnDuringSale", true, "purchaseLimit", 2, "enableQueue", true, "createdAt", now(), "updatedAt", now()));
        stockPool.add(map("id", 4001L, "sessionId", 1002L, "ticketLevelId", 2003L, "seatId", null, "sourceType", "POST_LOCK_RETURN", "stockStatus", "WAITING_RELEASE", "availableForNextBatch", true, "createdAt", now(), "updatedAt", now()));
        stockPool.add(map("id", 4002L, "sessionId", 1001L, "ticketLevelId", 2002L, "seatId", null, "sourceType", "UNRELEASED", "stockStatus", "WAITING_RELEASE", "availableForNextBatch", true, "createdAt", now(), "updatedAt", now()));
    }

    private String redisStockKey(Long batchId, Long ticketLevelId) {
        return "ticket:batch:" + batchId + ":level:" + ticketLevelId + ":stock";
    }

    private Map<String, Object> find(List<Map<String, Object>> source, Long id, String message) {
        return source.stream().filter(item -> Objects.equals(item.get("id"), id)).findFirst().orElseThrow(() -> new ApiException(404, message));
    }

    private List<Map<String, Object>> copyList(List<Map<String, Object>> source) {
        return source.stream().map(this::copy).toList();
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private void merge(Map<String, Object> target, Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (payload.containsKey(key)) {
                target.put(key, payload.get(key));
            }
        }
    }

    private List<Integer> parseAisles(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                result.add(Integer.parseInt(part.trim()));
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private String now() {
        return FORMATTER.format(LocalDateTime.now());
    }

    private LocalDateTime parseTime(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }

    private String str(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Long longValue(Map<String, Object> payload, String key, Long fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private int intValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return new BigDecimal(String.valueOf(value)).intValue();
    }

    private BigDecimal decimalValue(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return new BigDecimal(value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value));
    }

    private boolean boolValue(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
}
