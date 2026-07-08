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
import java.util.ArrayList;
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
        if (count != null && count > 0) {
            normalizeExistingMovieVenues();
            return;
        }
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
                    "cinemaName", "星河影城",
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
        normalizeExistingMovieVenues();
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

    public List<Map<String, Object>> adminCinemas() {
        normalizeAllCinemaVenues();
        return jdbcTemplate.query("""
                select id, city_name cityName, name, address, capacity, status, description
                from venue where venue_type='CINEMA' and deleted=0 order by city_name, name, id
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("cityName", rs.getString("cityName"));
            row.put("name", rs.getString("name"));
            row.put("address", rs.getString("address"));
            row.put("capacity", rs.getInt("capacity"));
            row.put("status", rs.getString("status"));
            row.put("description", rs.getString("description"));
            row.put("venueType", "CINEMA");
            row.put("stageLabel", "银幕");
            row.put("halls", cinemaHalls(rs.getLong("id")));
            row.put("hallCount", ((List<?>) row.get("halls")).size());
            return row;
        });
    }

    @Transactional
    public Map<String, Object> createCinema(Map<String, Object> payload) {
        jdbcTemplate.update("""
                insert into venue (city_id, city_name, name, address, intro, description, venue_type, stage_label, capacity, status, created_at, updated_at, deleted)
                values (0, ?, ?, ?, ?, ?, 'CINEMA', '银幕', 24, 'ENABLED', now(), now(), 0)
                """,
                str(payload, "cityName", str(payload, "city", "上海")),
                str(payload, "name", "新电影院"),
                str(payload, "address", "待完善地址"),
                str(payload, "description", "电影院"),
                str(payload, "description", "电影院"));
        Long id = jdbcTemplate.queryForObject("select last_insert_id()", Long.class);
        syncCinemaHalls(id, intValue(payload, "hallCount", 1));
        Long areaId = ensureCinemaArea(id);
        ensureCinemaSeats(id, areaId, 24);
        return cinema(id);
    }

    @Transactional
    public Map<String, Object> updateCinema(Long id, Map<String, Object> payload) {
        cinema(id);
        jdbcTemplate.update("""
                update venue set city_name=?, name=?, address=?, description=?, intro=?, capacity=?, status=?, updated_at=now()
                where id=? and venue_type='CINEMA' and deleted=0
                """,
                str(payload, "cityName", "上海"),
                str(payload, "name", "电影院"),
                str(payload, "address", "待完善地址"),
                str(payload, "description", ""),
                str(payload, "description", ""),
                countCinemaSeats(id),
                str(payload, "status", "ENABLED"),
                id);
        syncCinemaHalls(id, intValue(payload, "hallCount", 1));
        Long areaId = ensureCinemaArea(id);
        ensureCinemaSeats(id, areaId, 24);
        return cinema(id);
    }

    @Transactional
    public void deleteCinema(Long id) {
        jdbcTemplate.update("update cinema_hall set deleted=1, status='DISABLED', updated_at=now() where venue_id=?", id);
        jdbcTemplate.update("update venue set deleted=1, status='DISABLED', updated_at=now() where id=? and venue_type='CINEMA'", id);
    }

    public Map<String, Object> cinema(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                select id, city_name cityName, name, address, capacity, status, description
                from venue where id=? and venue_type='CINEMA' and deleted=0
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("cityName", rs.getString("cityName"));
            row.put("name", rs.getString("name"));
            row.put("address", rs.getString("address"));
            row.put("capacity", rs.getInt("capacity"));
            row.put("status", rs.getString("status"));
            row.put("description", rs.getString("description"));
            row.put("venueType", "CINEMA");
            row.put("stageLabel", "银幕");
            row.put("halls", cinemaHalls(rs.getLong("id")));
            row.put("hallCount", ((List<?>) row.get("halls")).size());
            return row;
        }, id);
        if (rows.isEmpty()) throw new ApiException(404, "电影院不存在");
        return rows.get(0);
    }

    public Map<String, Object> movieSchedule(Long movieId, String city) {
        movie(movieId);
        String cityFilter = city == null || city.isBlank() ? "%" : city.trim();
        List<Map<String, Object>> sessions = jdbcTemplate.query("""
                select ps.id sessionId, ps.venue_id cinemaId, v.city_name city, v.name cinemaName, v.address,
                       coalesce(ps.hall_name, ps.session_name) hallName,
                       date_format(ps.start_time, '%Y-%m-%d %H:%i:%s') startTime,
                       date_format(date(ps.start_time), '%Y-%m-%d') showDate,
                       coalesce(min(tl.price), 0) price
                from performance_session ps
                join venue v on v.id=ps.venue_id and v.deleted=0
                left join ticket_level tl on tl.session_id=ps.id and tl.deleted=0
                where ps.movie_id=? and ps.deleted=0 and v.venue_type='CINEMA' and (?='%' or v.city_name=?)
                group by ps.id, ps.venue_id, v.city_name, v.name, v.address, ps.hall_name, ps.session_name, ps.start_time
                order by ps.start_time, ps.id
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sessionId", rs.getLong("sessionId"));
            row.put("cinemaId", rs.getLong("cinemaId"));
            row.put("city", rs.getString("city"));
            row.put("cinemaName", rs.getString("cinemaName"));
            row.put("address", rs.getString("address"));
            row.put("hallName", rs.getString("hallName"));
            row.put("startTime", rs.getString("startTime"));
            row.put("showDate", rs.getString("showDate"));
            row.put("price", rs.getBigDecimal("price"));
            return row;
        }, movieId, cityFilter, cityFilter);
        Map<Long, Map<String, Object>> cinemas = new LinkedHashMap<>();
        for (Map<String, Object> session : sessions) {
            Long cinemaId = (Long) session.get("cinemaId");
            cinemas.computeIfAbsent(cinemaId, key -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("cinemaId", cinemaId);
                row.put("cinemaName", session.get("cinemaName"));
                row.put("city", session.get("city"));
                row.put("address", session.get("address"));
                row.put("sessions", new ArrayList<Map<String, Object>>());
                return row;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cinemaSessions = (List<Map<String, Object>>) cinemas.get(cinemaId).get("sessions");
            cinemaSessions.add(session);
        }
        List<String> dates = sessions.stream().map(item -> String.valueOf(item.get("showDate"))).distinct().limit(5).toList();
        return Map.of("dates", dates, "cinemas", cinemas.values().stream().toList(), "sessions", sessions);
    }

    @SuppressWarnings("unchecked")
    private void syncMovieSessions(Long movieId, Map<String, Object> payload) {
        Object raw = payload.get("sessions");
        if (!(raw instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) continue;
            Map<String, Object> session = (Map<String, Object>) rawMap;
            Long venueId = longValue(session.get("cinemaId"));
            if (venueId == null) venueId = longValue(session.get("venueId"));
            if (venueId == null) venueId = ensureCinemaVenue(session);
            Long areaId = ensureCinemaArea(venueId);
            ensureCinemaSeats(venueId, areaId, 24);
            int stock = Math.max(1, countCinemaSeats(venueId));
            Long sessionId = ensureMovieSession(movieId, venueId, session);
            ensureMovieTicketLevel(sessionId, areaId, intValue(session, "price", 68), stock);
            resourceService.initSessionSeats(sessionId);
        }
    }

    private void normalizeExistingMovieVenues() {
        List<Long> venueIds = jdbcTemplate.queryForList("""
                select distinct venue_id from performance_session
                where movie_id is not null and venue_id is not null and deleted=0
                """, Long.class);
        for (Long venueId : venueIds) {
            normalizeCinemaVenue(venueId, true);
        }
        normalizeAllCinemaVenues();
    }

    private void normalizeAllCinemaVenues() {
        List<Long> venueIds = jdbcTemplate.queryForList("select id from venue where venue_type='CINEMA' and deleted=0", Long.class);
        for (Long venueId : venueIds) {
            normalizeCinemaVenue(venueId, false);
        }
    }

    private void normalizeCinemaVenue(Long venueId, boolean fromMovieSessions) {
            jdbcTemplate.update("""
                    update venue set venue_type='CINEMA', stage_label='银幕', capacity=greatest(capacity, 24), updated_at=now()
                    where id=? and deleted=0
                    """, venueId);
            Long areaId = ensureCinemaArea(venueId);
            ensureCinemaSeats(venueId, areaId, 24);
            List<String> halls = fromMovieSessions ? jdbcTemplate.queryForList("""
                    select distinct coalesce(hall_name, session_name, '1号厅') from performance_session
                    where movie_id is not null and venue_id=? and deleted=0
                    """, String.class, venueId) : cinemaHalls(venueId).stream().map(item -> String.valueOf(item.get("name"))).toList();
            if (halls.isEmpty()) syncCinemaHalls(venueId, 1);
            else syncCinemaHalls(venueId, halls.size());
            int stock = Math.max(1, countCinemaSeats(venueId));
            List<Long> sessionIds = jdbcTemplate.queryForList("select id from performance_session where movie_id is not null and venue_id=? and deleted=0", Long.class, venueId);
            for (Long sessionId : sessionIds) {
                jdbcTemplate.update("""
                        update ticket_level set area_id=?, total_stock=?, released_stock=?, unreleased_stock=0, updated_at=now()
                        where session_id=? and deleted=0
                        """, areaId, stock, stock, sessionId);
                resourceService.initSessionSeats(sessionId);
            }
            jdbcTemplate.update("update venue set capacity=?, updated_at=now() where id=?", stock, venueId);
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
        if (existing > 0) return;
        int rows = 4;
        int seatsPerRow = 6;
        int gapX = 54;
        int gapY = 48;
        int startX = 380 - ((seatsPerRow - 1) * gapX / 2);
        int startY = 150;
        for (int i = 1; i <= Math.max(stock, rows * seatsPerRow); i++) {
            int row = ((i - 1) / seatsPerRow) + 1;
            int number = ((i - 1) % seatsPerRow) + 1;
            jdbcTemplate.update("""
                    insert into seat (venue_id, area_id, row_no, seat_no, seat_label, x, y, is_aisle, is_disabled, status, created_at, updated_at, deleted)
                    values (?, ?, ?, ?, ?, ?, ?, 0, 0, 'AVAILABLE', now(), now(), 0)
                    """, venueId, areaId, String.valueOf(row), String.valueOf(number), row + "排" + number + "座", startX + (number - 1) * gapX, startY + (row - 1) * gapY);
        }
    }

    private Long ensureMovieSession(Long movieId, Long venueId, Map<String, Object> payload) {
        Timestamp start = timeValue(payload, "startTime", "2026-08-01 19:30:00");
        List<Long> ids = jdbcTemplate.queryForList("select id from performance_session where movie_id=? and start_time=? and deleted=0 order by id limit 1", Long.class, movieId, start);
        Timestamp saleStart = optionalTimeValue(payload, "saleStartTime");
        if (saleStart == null) saleStart = Timestamp.valueOf(LocalDateTime.now().minusMinutes(1));
        Timestamp lockTime = optionalTimeValue(payload, "lockTime");
        if (lockTime == null) lockTime = Timestamp.valueOf(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
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
                values (?, '电影开售', '电影开售', ?, ?, 'QUANTITY', 'QUANTITY', 0, 0, 0, 1, 1, 4, 4, 1, 1, 'NOT_STARTED', now(), now(), 0)
                """, sessionId, saleStart, lockTime);
    }

    private List<Map<String, Object>> cinemaHalls(Long venueId) {
        return jdbcTemplate.query("""
                select id, venue_id venueId, name, sort_order sortOrder, status
                from cinema_hall where venue_id=? and deleted=0 order by sort_order, id
                """, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("venueId", rs.getLong("venueId"));
            row.put("name", rs.getString("name"));
            row.put("sortOrder", rs.getInt("sortOrder"));
            row.put("status", rs.getString("status"));
            return row;
        }, venueId);
    }

    private void syncCinemaHalls(Long venueId, int hallCount) {
        int count = Math.max(1, hallCount);
        jdbcTemplate.update("update cinema_hall set deleted=1, status='DISABLED', updated_at=now() where venue_id=?", venueId);
        for (int i = 1; i <= count; i++) {
            String name = i + "号厅";
            List<Long> ids = jdbcTemplate.queryForList("select id from cinema_hall where venue_id=? and name=? order by id limit 1", Long.class, venueId, name);
            if (ids.isEmpty()) {
                jdbcTemplate.update("""
                        insert into cinema_hall (venue_id, name, sort_order, status, created_at, updated_at, deleted)
                        values (?, ?, ?, 'ENABLED', now(), now(), 0)
                        """, venueId, name, i);
            } else {
                jdbcTemplate.update("update cinema_hall set sort_order=?, status='ENABLED', deleted=0, updated_at=now() where id=?", i, ids.get(0));
            }
        }
    }

    private int countCinemaSeats(Long venueId) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from seat where venue_id=? and deleted=0 and status='AVAILABLE'", Integer.class, venueId);
        return count == null ? 0 : count;
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
                select ps.id, ps.venue_id venueId, v.city_name city, v.name cinemaName,
                       date_format(ps.start_time, '%Y-%m-%d %H:%i:%s') startTime,
                       date_format(ps.sale_start_time, '%Y-%m-%d %H:%i:%s') saleStartTime,
                       date_format(ps.lock_time, '%Y-%m-%d %H:%i:%s') lockTime,
                       coalesce(ps.hall_name, ps.session_name) hallName, ps.purchase_mode purchaseMode,
                       coalesce(min(tl.price), 0) price, coalesce(max(tl.released_stock), 0) stock
                from performance_session ps
                left join venue v on v.id=ps.venue_id
                left join ticket_level tl on tl.session_id=ps.id and tl.deleted=0
                where ps.movie_id=? and ps.deleted=0
                group by ps.id, ps.venue_id, v.city_name, v.name, ps.start_time, ps.sale_start_time, ps.lock_time, ps.hall_name, ps.session_name, ps.purchase_mode
                order by ps.start_time, ps.id
                """, (rs, rowNum) -> new SessionOption(rs.getLong("id"), rs.getString("startTime"), rs.getString("saleStartTime"),
                rs.getString("lockTime"), rs.getString("hallName"), rs.getString("purchaseMode"),
                rs.getLong("venueId"), rs.getString("city"), rs.getString("cinemaName"), rs.getBigDecimal("price"), rs.getInt("stock")), movieId);
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

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.parseLong(String.valueOf(value));
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

    private Timestamp optionalTimeValue(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        String value = String.valueOf(raw).trim();
        if (value.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}$")) value += ":00";
        return Timestamp.valueOf(LocalDateTime.parse(value, FORMATTER));
    }
}
