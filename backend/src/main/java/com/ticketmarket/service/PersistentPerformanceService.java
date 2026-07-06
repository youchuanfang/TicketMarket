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

    private Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }
}
