package com.ticketmarket.service;

import com.ticketmarket.common.ApiException;
import com.ticketmarket.model.MovieCard;
import com.ticketmarket.model.SessionOption;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@DependsOn("databaseSchemaInitializer")
public class PersistentMovieService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final DemoDataService demoDataService;
    private final Phase3ResourceService resourceService;

    public PersistentMovieService(JdbcTemplate jdbcTemplate, DemoDataService demoDataService, Phase3ResourceService resourceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.demoDataService = demoDataService;
        this.resourceService = resourceService;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from movie where deleted=0", Integer.class);
        if (count != null && count > 0) return;
        for (MovieCard movie : demoDataService.movies()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("title", movie.getTitle());
            payload.put("genre", movie.getGenre());
            payload.put("releaseDate", movie.getReleaseDate());
            payload.put("durationMinutes", movie.getDurationMinutes());
            payload.put("director", movie.getDirector());
            payload.put("actors", movie.getActors());
            payload.put("rating", movie.getRating());
            payload.put("summary", movie.getSummary());
            payload.put("poster", movie.getPoster());
            payload.put("homeRecommended", movie.getId() <= 203);
            payload.put("homeSort", movie.getId() - 200);
            payload.put("sessions", movie.getSessions().stream().map(session -> Map.of(
                    "cinemaName", "星河影城一号厅",
                    "hallName", session.hallName(),
                    "city", "上海",
                    "startTime", session.startTime(),
                    "saleStartTime", session.saleStartTime(),
                    "lockTime", session.lockTime(),
                    "price", 68,
                    "stock", 96
            )).toList());
            createMovie(payload);
        }
    }

    public List<MovieCard> movies() {
        return jdbcTemplate.query("""
                select * from movie where deleted=0 and status='PUBLISHED'
                order by home_recommended desc, home_sort, release_date, id
                """, this::mapMovieWithSessions);
    }

    public List<MovieCard> adminMovies() {
        return jdbcTemplate.query("select * from movie where deleted=0 order by updated_at desc, id desc", this::mapMovieWithSessions);
    }

    public MovieCard movie(Long id) {
        List<MovieCard> rows = jdbcTemplate.query("select * from movie where id=? and deleted=0", this::mapMovieWithSessions, id);
        if (rows.isEmpty()) throw new ApiException(404, "电影不存在");
        return rows.get(0);
    }

    @Transactional
    public MovieCard createMovie(Map<String, Object> payload) {
        jdbcTemplate.update("""
                insert into movie
                (title, genre, release_date, duration_minutes, director, actors, rating, summary, poster_path,
                 home_recommended, home_sort, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', now(), now(), 0)
                """,
                str(payload, "title", "新电影"),
                str(payload, "genre", ""),
                dateValue(payload.get("releaseDate")),
                intValue(payload, "durationMinutes", 120),
                str(payload, "director", ""),
                str(payload, "actors", ""),
                str(payload, "rating", ""),
                str(payload, "summary", ""),
                str(payload, "poster", str(payload, "posterPath", "")),
                boolValue(payload, "homeRecommended", false),
                intValue(payload, "homeSort", 0)
        );
        Long id = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        syncMovieSessions(id, payload);
        return movie(id);
    }

    @Transactional
    public MovieCard updateMovie(Long id, Map<String, Object> payload) {
        movie(id);
        jdbcTemplate.update("""
                update movie set title=?, genre=?, release_date=?, duration_minutes=?, director=?, actors=?, rating=?,
                  summary=?, poster_path=?, home_recommended=?, home_sort=?, status=?, updated_at=now()
                where id=? and deleted=0
                """,
                str(payload, "title", "新电影"),
                str(payload, "genre", ""),
                dateValue(payload.get("releaseDate")),
                intValue(payload, "durationMinutes", 120),
                str(payload, "director", ""),
                str(payload, "actors", ""),
                str(payload, "rating", ""),
                str(payload, "summary", ""),
                str(payload, "poster", str(payload, "posterPath", "")),
                boolValue(payload, "homeRecommended", false),
                intValue(payload, "homeSort", 0),
                str(payload, "status", "PUBLISHED"),
                id
        );
        syncMovieSessions(id, payload);
        return movie(id);
    }

    public void deleteMovie(Long id) {
        jdbcTemplate.update("update movie set deleted=1, status='OFFLINE', updated_at=now() where id=?", id);
    }

    @SuppressWarnings("unchecked")
    private void syncMovieSessions(Long movieId, Map<String, Object> payload) {
        Object raw = payload.get("sessions");
        if (!(raw instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) continue;
            Map<String, Object> session = (Map<String, Object>) rawMap;
            Long venueId = ensureCinemaVenue(session);
            Long areaId = ensureCinemaArea(venueId);
            int stock = intValue(session, "stock", 96);
            ensureCinemaSeats(venueId, areaId, stock);
            Long sessionId = ensureMovieSession(movieId, venueId, session);
            ensureMovieTicketLevel(sessionId, areaId, intValue(session, "price", 68), stock);
            resourceService.initSessionSeats(sessionId);
        }
    }

    private Long ensureCinemaVenue(Map<String, Object> payload) {
        String name = str(payload, "cinemaName", str(payload, "venueName", "电影院"));
        String city = str(payload, "city", "上海");
        List<Long> ids = jdbcTemplate.queryForList("select id from venue where deleted=0 and name=? and city_name=? order by id limit 1", Long.class, name, city);
        if (!ids.isEmpty()) return ids.get(0);
        jdbcTemplate.update("""
                insert into venue (city_id, city_name, name, address, intro, description, venue_type, stage_label, capacity, status, created_at, updated_at, deleted)
                values (0, ?, ?, ?, '电影院', '电影院', 'CINEMA', '银幕', 120, 'ENABLED', now(), now(), 0)
                """, city, name, str(payload, "address", "待完善地址"));
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private Long ensureCinemaArea(Long venueId) {
        List<Long> ids = jdbcTemplate.queryForList("select id from venue_area where venue_id=? and name='座位区' and deleted=0 order by id limit 1", Long.class, venueId);
        if (!ids.isEmpty()) return ids.get(0);
        jdbcTemplate.update("""
                insert into venue_area (venue_id, name, area_type, default_ticket_level, sort_order, color, status, created_at, updated_at, deleted)
                values (?, '座位区', 'SEATED', '电影票', 1, '#d9303e', 'ENABLED', now(), now(), 0)
                """, venueId);
        return jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
    }

    private void ensureCinemaSeats(Long venueId, Long areaId, int stock) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from seat where venue_id=? and area_id=? and deleted=0", Integer.class, venueId, areaId);
        int existing = count == null ? 0 : count;
        for (int i = existing + 1; i <= Math.max(stock, 1); i++) {
            int row = ((i - 1) / 12) + 1;
            int number = ((i - 1) % 12) + 1;
            jdbcTemplate.update("""
                    insert into seat (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                    values (?, ?, ?, ?, ?, ?, ?, 0, 0, 'AVAILABLE', now(), now(), 0)
                    """, venueId, areaId, String.valueOf(row), String.valueOf(number), "座位区-" + row + "排" + number + "座", 40 + number * 28, 60 + row * 28);
        }
    }

    private Long ensureMovieSession(Long movieId, Long venueId, Map<String, Object> payload) {
        Timestamp start = timeValue(payload, "startTime", "2026-08-01 19:30:00");
        List<Long> ids = jdbcTemplate.queryForList("select id from performance_session where movie_id=? and start_time=? and deleted=0 order by id limit 1", Long.class, movieId, start);
        Timestamp saleStart = timeValue(payload, "saleStartTime", "2026-07-01 10:00:00");
        Timestamp lockTime = timeValue(payload, "lockTime", "2026-12-31 23:00:00");
        if (!ids.isEmpty()) {
            jdbcTemplate.update("""
                    update performance_session set venue_id=?, session_name=?, hall_name=?, sale_start_time=?, lock_time=?,
                      entry_time=?, start_time=?, end_time=?, sale_mode='SELECTABLE', purchase_mode='SELECTABLE', status='SCHEDULED', updated_at=now()
                    where id=? and deleted=0
                    """, venueId, str(payload, "hallName", "影厅"), str(payload, "hallName", "影厅"), saleStart, lockTime, start, start, Timestamp.valueOf(start.toLocalDateTime().plusHours(2)), ids.get(0));
            ensureMovieBatch(ids.get(0), saleStart, lockTime);
            return ids.get(0);
        }
        jdbcTemplate.update("""
                insert into performance_session
                (movie_id, venue_id, session_name, hall_name, sale_start_time, lock_time, entry_time, start_time, end_time,
                 sale_mode, purchase_mode, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SELECTABLE', 'SELECTABLE', 'SCHEDULED', now(), now(), 0)
                """, movieId, venueId, str(payload, "hallName", "影厅"), str(payload, "hallName", "影厅"), saleStart, lockTime, start, start, Timestamp.valueOf(start.toLocalDateTime().plusHours(2)));
        Long id = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        ensureMovieBatch(id, saleStart, lockTime);
        return id;
    }

    private void ensureMovieTicketLevel(Long sessionId, Long areaId, int price, int stock) {
        List<Long> ids = jdbcTemplate.queryForList("select id from ticket_level where session_id=? and deleted=0 order by id limit 1", Long.class, sessionId);
        String name = "座位区" + price;
        if (!ids.isEmpty()) {
            jdbcTemplate.update("update ticket_level set name=?, area_id=?, price=?, total_stock=?, released_stock=?, unreleased_stock=0, status='ENABLED', updated_at=now() where id=?",
                    name, areaId, price, stock, stock, ids.get(0));
            return;
        }
        jdbcTemplate.update("""
                insert into ticket_level
                (session_id, name, area_id, price, total_stock, released_stock, unreleased_stock, sold_stock, locked_stock, refunded_stock, status, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 'ENABLED', now(), now(), 0)
                """, sessionId, name, areaId, price, stock, stock);
    }

    private void ensureMovieBatch(Long sessionId, Timestamp saleStart, Timestamp lockTime) {
        List<Long> ids = jdbcTemplate.queryForList("select id from sale_batch where session_id=? and deleted=0 order by id limit 1", Long.class, sessionId);
        if (!ids.isEmpty()) {
            jdbcTemplate.update("update sale_batch set sale_start_time=?, lock_time=?, release_quantity=0, open_stock=0, updated_at=now() where id=?", saleStart, lockTime, ids.get(0));
            return;
        }
        jdbcTemplate.update("""
                insert into sale_batch
                (session_id, name, batch_name, sale_start_time, lock_time, open_mode, release_type, open_stock, release_quantity,
                 release_ratio, allow_return_current_round, allow_return_during_sale, limit_per_user, purchase_limit, queue_enabled, enable_queue, status, created_at, updated_at, deleted)
                values (?, '电影开售', '电影开售', ?, ?, 'QUANTITY', 'QUANTITY', 0, 0, 0, 1, 1, 6, 6, 1, 1, 'NOT_STARTED', now(), now(), 0)
                """, sessionId, saleStart, lockTime);
    }

    private MovieCard mapMovieWithSessions(ResultSet rs, int rowNum) throws SQLException {
        MovieCard movie = new MovieCard();
        movie.setId(rs.getLong("id"));
        movie.setTitle(rs.getString("title"));
        movie.setGenre(rs.getString("genre"));
        movie.setReleaseDate(rs.getDate("release_date") == null ? "" : rs.getDate("release_date").toString());
        movie.setDurationMinutes(rs.getInt("duration_minutes"));
        movie.setDirector(rs.getString("director"));
        movie.setActors(rs.getString("actors"));
        movie.setRating(rs.getString("rating"));
        movie.setSummary(rs.getString("summary"));
        movie.setPoster(rs.getString("poster_path"));
        movie.setHomeRecommended(rs.getBoolean("home_recommended"));
        movie.setHomeSort(rs.getInt("home_sort"));
        movie.setSessions(movieSessions(movie.getId()));
        return movie;
    }

    private List<SessionOption> movieSessions(Long movieId) {
        return jdbcTemplate.query("""
                select id, date_format(start_time, '%Y-%m-%d %H:%i:%s') startTime,
                       date_format(sale_start_time, '%Y-%m-%d %H:%i:%s') saleStartTime,
                       date_format(lock_time, '%Y-%m-%d %H:%i:%s') lockTime,
                       coalesce(hall_name, session_name) hallName, purchase_mode purchaseMode
                from performance_session
                where movie_id=? and deleted=0 order by start_time, id
                """, (rs, rowNum) -> new SessionOption(rs.getLong("id"), rs.getString("startTime"), rs.getString("saleStartTime"),
                rs.getString("lockTime"), rs.getString("hallName"), rs.getString("purchaseMode")), movieId);
    }

    private String str(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private int intValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : Integer.parseInt(String.valueOf(value));
    }

    private boolean boolValue(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private java.sql.Date dateValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return java.sql.Date.valueOf(LocalDate.parse(String.valueOf(value).substring(0, 10)));
    }

    private Timestamp timeValue(Map<String, Object> payload, String key, String fallback) {
        String value = str(payload, key, fallback);
        if (value.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}$")) value += ":00";
        return Timestamp.valueOf(LocalDateTime.parse(value, FORMATTER));
    }
}
