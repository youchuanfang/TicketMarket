package com.ticketmarket.service;

import com.ticketmarket.model.Category;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.PerformanceCard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class HomepageRecommendationService {
    private static final int MAX_SECTION_ITEMS = 4;

    private final JdbcTemplate jdbcTemplate;
    private final DemoDataService demoDataService;
    private final PersistentPerformanceService performanceService;
    private final PersistentMovieService movieService;

    public HomepageRecommendationService(JdbcTemplate jdbcTemplate,
                                         DemoDataService demoDataService,
                                         PersistentPerformanceService performanceService,
                                         PersistentMovieService movieService) {
        this.jdbcTemplate = jdbcTemplate;
        this.demoDataService = demoDataService;
        this.performanceService = performanceService;
        this.movieService = movieService;
    }

    public List<Map<String, Object>> sections() {
        List<Map<String, Object>> sections = new ArrayList<>();
        sections.add(section("hot", "热门推荐", "", true, true));
        for (Category category : demoDataService.categories()) {
            if ("movie".equals(category.code())) continue;
            sections.add(section(category.code(), category.name(), category.code(), true, false));
        }
        sections.add(section("movie", "电影热映", "movie", false, true));
        return sections;
    }

    public Map<String, Object> adminModel() {
        List<Map<String, Object>> performances = performanceService.adminPerformances().stream()
                .map(this::performanceCard)
                .toList();
        List<Map<String, Object>> movies = movieService.adminMovies().stream()
                .map(this::movieCard)
                .toList();
        List<Map<String, Object>> sectionRows = sections().stream().map(section -> {
            String code = String.valueOf(section.get("code"));
            List<Map<String, Object>> candidates = candidatesFor(section, performances, movies);
            List<Map<String, Object>> selected = selectedItems(code, candidates);
            Map<String, Object> row = new LinkedHashMap<>(section);
            row.put("candidates", candidates);
            row.put("selected", selected);
            return row;
        }).toList();
        return Map.of("sections", sectionRows);
    }

    public Map<String, Object> portalRecommendations(List<PerformanceCard> publicPerformances, List<MovieCard> publicMovies) {
        List<Map<String, Object>> performances = publicPerformances.stream().map(this::performanceCard).toList();
        List<Map<String, Object>> movies = publicMovies.stream().map(this::movieCard).toList();
        List<Map<String, Object>> hotCandidates = new ArrayList<>();
        hotCandidates.addAll(performances);
        hotCandidates.addAll(movies);
        List<Map<String, Object>> hot = selectedOrFallback("hot", hotCandidates);
        List<Map<String, Object>> categorySections = sections().stream()
                .filter(section -> Boolean.TRUE.equals(section.get("performanceOnly")))
                .filter(section -> !"hot".equals(section.get("code")))
                .map(section -> {
                    String code = String.valueOf(section.get("code"));
                    List<Map<String, Object>> candidates = performances.stream()
                            .filter(item -> Objects.equals(item.get("categoryCode"), code))
                            .toList();
                    return Map.<String, Object>of(
                            "code", code,
                            "name", section.get("name"),
                            "items", selectedOrFallback(code, candidates)
                    );
                })
                .filter(section -> !((List<?>) section.get("items")).isEmpty())
                .toList();
        return Map.of(
                "hot", hot,
                "banners", hot,
                "categorySections", categorySections,
                "movies", selectedOrFallback("movie", movies)
        );
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveSection(String sectionCode, Map<String, Object> payload) {
        jdbcTemplate.update("delete from homepage_recommendation where section_code=?", sectionCode);
        Object raw = payload.get("items");
        if (raw instanceof List<?> list) {
            int order = 1;
            for (Object item : list.stream().limit(MAX_SECTION_ITEMS).toList()) {
                if (!(item instanceof Map<?, ?> rawMap)) continue;
                Map<String, Object> row = (Map<String, Object>) rawMap;
                String targetType = str(row.get("targetType"), str(row.get("itemType"), "PERFORMANCE")).toUpperCase();
                Long targetId = longValue(row.get("targetId") == null ? row.get("id") : row.get("targetId"));
                if (targetId == null) continue;
                jdbcTemplate.update("""
                        insert into homepage_recommendation (section_code, target_type, target_id, sort_order, created_at, updated_at)
                        values (?, ?, ?, ?, now(), now())
                        """, sectionCode, targetType, targetId, order++);
            }
        }
        return adminModel();
    }

    private List<Map<String, Object>> candidatesFor(Map<String, Object> section,
                                                    List<Map<String, Object>> performances,
                                                    List<Map<String, Object>> movies) {
        String code = String.valueOf(section.get("code"));
        if ("hot".equals(code)) {
            List<Map<String, Object>> all = new ArrayList<>();
            all.addAll(performances);
            all.addAll(movies);
            return all;
        }
        if ("movie".equals(code)) return movies;
        return performances.stream().filter(item -> Objects.equals(item.get("categoryCode"), code)).toList();
    }

    private List<Map<String, Object>> selectedOrFallback(String sectionCode, List<Map<String, Object>> candidates) {
        List<Map<String, Object>> selected = selectedItems(sectionCode, candidates);
        return (selected.isEmpty() ? candidates : selected).stream().limit(MAX_SECTION_ITEMS).toList();
    }

    private List<Map<String, Object>> selectedItems(String sectionCode, List<Map<String, Object>> candidates) {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> candidate : candidates) {
            byKey.put(candidate.get("targetType") + ":" + candidate.get("targetId"), candidate);
        }
        return jdbcTemplate.query("""
                        select target_type targetType, target_id targetId, sort_order sortOrder
                        from homepage_recommendation where section_code=? order by sort_order, id
                        """,
                (rs, rowNum) -> byKey.get(rs.getString("targetType") + ":" + rs.getLong("targetId")),
                sectionCode).stream().filter(Objects::nonNull).limit(MAX_SECTION_ITEMS).toList();
    }

    private Map<String, Object> section(String code, String name, String categoryCode, boolean performanceOnly, boolean allowMovie) {
        return Map.of(
                "code", code,
                "name", name,
                "categoryCode", categoryCode,
                "performanceOnly", performanceOnly,
                "allowMovie", allowMovie,
                "maxItems", MAX_SECTION_ITEMS
        );
    }

    private Map<String, Object> performanceCard(PerformanceCard item) {
        return mutableCard(
                "PERFORMANCE", item.getId(), "/performances/" + item.getId(), item.getTitle(), item.getPoster(),
                item.getCategoryCode(), item.getCategoryName(), item.getCity(), item.getVenue(), item.getStartTime(),
                item.getPriceMin(), item.getSaleStatus(), item.getSaleMode(), item.getHomeRecommended(), item.getHomeSort()
        );
    }

    private Map<String, Object> movieCard(MovieCard item) {
        Map<String, Object> first = jdbcTemplate.query("""
                        select v.city_name city, v.name venue, min(ps.start_time) start_time,
                               coalesce(min(tl.price), 0) price_min
                        from performance_session ps
                        left join venue v on v.id=ps.venue_id
                        left join ticket_level tl on tl.session_id=ps.id and tl.deleted=0
                        where ps.movie_id=? and ps.deleted=0
                        group by v.city_name, v.name
                        order by min(ps.start_time), min(v.id) limit 1
                        """,
                rs -> rs.next() ? Map.<String, Object>of(
                        "city", str(rs.getString("city"), ""),
                        "venue", str(rs.getString("venue"), "影院待排片"),
                        "startTime", rs.getTimestamp("start_time") == null ? item.getReleaseDate() : rs.getTimestamp("start_time").toLocalDateTime().toString().replace('T', ' '),
                        "priceMin", rs.getBigDecimal("price_min") == null ? 0 : rs.getBigDecimal("price_min")
                ) : Map.of("city", "", "venue", "影院待排片", "startTime", item.getReleaseDate(), "priceMin", 0),
                item.getId());
        return mutableCard(
                "MOVIE", item.getId(), "/movies/" + item.getId(), item.getTitle(), item.getPoster(),
                "movie", "电影", str(first.get("city"), ""), str(first.get("venue"), ""), str(first.get("startTime"), item.getReleaseDate()),
                first.get("priceMin"), "ON_SALE", "SELECTABLE", item.getHomeRecommended(), item.getHomeSort()
        );
    }

    private Map<String, Object> mutableCard(String targetType, Long targetId, String detailPath, String title, String poster,
                                            String categoryCode, String categoryName, String city, String venue,
                                            String startTime, Object priceMin, String saleStatus, String saleMode,
                                            Boolean homeRecommended, Integer homeSort) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", targetId);
        row.put("targetId", targetId);
        row.put("targetType", targetType);
        row.put("itemType", targetType);
        row.put("detailPath", detailPath);
        row.put("title", title);
        row.put("poster", poster);
        row.put("categoryCode", categoryCode);
        row.put("categoryName", categoryName);
        row.put("city", city);
        row.put("venue", venue);
        row.put("startTime", startTime);
        row.put("priceMin", priceMin);
        row.put("saleStatus", saleStatus);
        row.put("saleMode", saleMode);
        row.put("homeRecommended", homeRecommended);
        row.put("homeSort", homeSort == null ? 0 : homeSort);
        return row;
    }

    private String str(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.parseLong(String.valueOf(value));
    }
}
