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
    }

    private void cleanupSelfTestRows() {
        Set<Long> performanceIds = new LinkedHashSet<>(ids("""
                select id from performance
                where deleted=0 and lower(concat_ws(' ', title, subtitle, summary, introduction, detail_content,
                  artist_intro, venue_intro, purchase_notice, refund_notice, entry_notice)) like '%codex%'
                """));
        performanceIds.addAll(ids("""
                select distinct performance_id from performance_detail_block
                where deleted=0 and lower(concat_ws(' ', title, content, image_path)) like '%codex%'
                """));

        Set<Long> movieIds = new LinkedHashSet<>(ids("""
                select id from movie
                where deleted=0 and lower(concat_ws(' ', title, genre, director, actors, summary, poster_path)) like '%codex%'
                """));

        Set<Long> venueIds = new LinkedHashSet<>(ids("""
                select id from venue
                where deleted=0 and lower(concat_ws(' ', name, address, intro, description)) like '%codex%'
                """));

        Set<Long> sessionIds = new LinkedHashSet<>();
        addAll(sessionIds, "select id from performance_session where deleted=0 and performance_id in (%s)", performanceIds);
        addAll(sessionIds, "select id from performance_session where deleted=0 and movie_id in (%s)", movieIds);
        addAll(sessionIds, "select id from performance_session where deleted=0 and venue_id in (%s)", venueIds);
        sessionIds.addAll(ids("""
                select id from performance_session
                where deleted=0 and lower(concat_ws(' ', session_name, hall_name)) like '%codex%'
                """));
        sessionIds.addAll(ids("""
                select distinct session_id from ticket_level
                where deleted=0 and lower(name) like '%codex%'
                """));

        Set<Long> ticketLevelIds = new LinkedHashSet<>();
        addAll(ticketLevelIds, "select id from ticket_level where deleted=0 and session_id in (%s)", sessionIds);
        ticketLevelIds.addAll(ids("select id from ticket_level where deleted=0 and lower(name) like '%codex%'"));

        Set<Long> batchIds = new LinkedHashSet<>();
        addAll(batchIds, "select id from sale_batch where deleted=0 and session_id in (%s)", sessionIds);

        Set<Long> orderIds = new LinkedHashSet<>();
        addAll(orderIds, "select id from ticket_order where deleted=0 and session_id in (%s)", sessionIds);
        addAll(orderIds, "select distinct order_id from order_item where ticket_level_id in (%s)", ticketLevelIds);
        addAll(orderIds, "select distinct order_id from e_ticket where session_id in (%s)", sessionIds);
        addAll(orderIds, "select distinct order_id from e_ticket where ticket_level_id in (%s)", ticketLevelIds);
        orderIds.addAll(ids("""
                select distinct oi.order_id
                from order_item oi
                left join ticket_level tl on tl.id=oi.ticket_level_id and tl.deleted=0
                where oi.ticket_level_id is not null and tl.id is null
                """));
        orderIds.addAll(ids("""
                select distinct et.order_id
                from e_ticket et
                left join ticket_level tl on tl.id=et.ticket_level_id and tl.deleted=0
                where et.ticket_level_id is not null and tl.id is null
                """));

        Set<Long> ticketIds = new LinkedHashSet<>();
        addAll(ticketIds, "select id from e_ticket where order_id in (%s)", orderIds);
        addAll(ticketIds, "select id from e_ticket where session_id in (%s)", sessionIds);
        addAll(ticketIds, "select id from e_ticket where ticket_level_id in (%s)", ticketLevelIds);

        Set<Long> refundApplyIds = new LinkedHashSet<>();
        addAll(refundApplyIds, "select id from refund_apply where order_id in (%s)", orderIds);

        deleteIn("delete from sale_batch_seat where sale_batch_id in (%s)", batchIds);
        deleteIn("delete from sale_batch_ticket_level where sale_batch_id in (%s)", batchIds);
        deleteIn("delete from sale_batch_ticket_level where ticket_level_id in (%s)", ticketLevelIds);
        deleteIn("delete from checkin_record where ticket_id in (%s)", ticketIds);
        deleteIn("delete from checkin_record where session_id in (%s)", sessionIds);
        deleteIn("delete from refund_record where refund_apply_id in (%s)", refundApplyIds);
        deleteIn("delete from refund_record where order_id in (%s)", orderIds);
        deleteIn("delete from refund_apply where id in (%s)", refundApplyIds);
        deleteIn("delete from payment_record where order_id in (%s)", orderIds);
        deleteIn("delete from e_ticket where id in (%s)", ticketIds);
        deleteIn("delete from order_item where order_id in (%s)", orderIds);
        deleteIn("delete from order_item where ticket_level_id in (%s)", ticketLevelIds);
        deleteIn("delete from stock_pool where session_id in (%s)", sessionIds);
        deleteIn("delete from stock_pool where ticket_level_id in (%s)", ticketLevelIds);
        deleteIn("delete from session_seat where session_id in (%s)", sessionIds);
        deleteIn("delete from reservation_remind where session_id in (%s)", sessionIds);
        deleteIn("delete from reservation_remind where performance_id in (%s)", performanceIds);
        deleteIn("delete from reservation_remind where ticket_level_id in (%s)", ticketLevelIds);
        deleteHomepageRecommendations("PERFORMANCE", performanceIds);
        deleteHomepageRecommendations("MOVIE", movieIds);

        updateIn("update ticket_order set deleted=1, status='CANCELLED', updated_at=now() where id in (%s)", orderIds);
        updateIn("update sale_batch set deleted=1, status='CANCELLED', updated_at=now() where id in (%s)", batchIds);
        updateIn("update ticket_level set deleted=1, status='DISABLED', updated_at=now() where id in (%s)", ticketLevelIds);
        updateIn("update performance_session set deleted=1, status='CANCELLED', updated_at=now() where id in (%s)", sessionIds);
        updateIn("update performance_detail_block set deleted=1, updated_at=now() where performance_id in (%s)", performanceIds);
        jdbcTemplate.update("""
                update performance_detail_block
                set deleted=1, updated_at=now()
                where deleted=0 and lower(concat_ws(' ', title, content, image_path)) like '%codex%'
                """);
        updateIn("update performance set deleted=1, publish_status='OFFLINE', updated_at=now() where id in (%s)", performanceIds);
        updateIn("update movie set deleted=1, status='OFFLINE', updated_at=now() where id in (%s)", movieIds);
        updateIn("update venue_area set deleted=1, status='DISABLED', updated_at=now() where venue_id in (%s)", venueIds);
        updateIn("update seat set deleted=1, status='DISABLED', updated_at=now() where venue_id in (%s)", venueIds);
        updateIn("update cinema_hall set deleted=1, status='DISABLED', updated_at=now() where venue_id in (%s)", venueIds);
        updateIn("update venue set deleted=1, status='DISABLED', updated_at=now() where id in (%s)", venueIds);
    }

    private List<Long> ids(String sql) {
        return jdbcTemplate.queryForList(sql, Long.class);
    }

    private void addAll(Set<Long> target, String sqlTemplate, Set<Long> source) {
        if (source.isEmpty()) return;
        target.addAll(ids(sqlTemplate.formatted(placeholders(source)), source));
    }

    private List<Long> ids(String sql, Set<Long> args) {
        return jdbcTemplate.queryForList(sql, Long.class, args.toArray());
    }

    private void updateIn(String sqlTemplate, Set<Long> ids) {
        executeIn(sqlTemplate, ids);
    }

    private void deleteIn(String sqlTemplate, Set<Long> ids) {
        executeIn(sqlTemplate, ids);
    }

    private void deleteHomepageRecommendations(String targetType, Set<Long> targetIds) {
        if (targetIds.isEmpty()) return;
        List<Object> args = new ArrayList<>();
        args.add(targetType);
        args.addAll(targetIds);
        jdbcTemplate.update(
                "delete from homepage_recommendation where target_type=? and target_id in (" + placeholders(targetIds) + ")",
                args.toArray()
        );
    }

    private void executeIn(String sqlTemplate, Set<Long> ids) {
        if (ids.isEmpty()) return;
        jdbcTemplate.update(sqlTemplate.formatted(placeholders(ids)), ids.toArray());
    }

    private String placeholders(Set<Long> ids) {
        return String.join(",", ids.stream().map(id -> "?").toList());
    }
}
