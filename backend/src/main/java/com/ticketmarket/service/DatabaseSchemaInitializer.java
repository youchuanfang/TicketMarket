package com.ticketmarket.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("databaseSchemaInitializer")
public class DatabaseSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        statement("""
                create table if not exists performance (
                  id bigint primary key auto_increment,
                  title varchar(180) not null,
                  subtitle varchar(255),
                  category_id bigint default 0,
                  category_name varchar(80),
                  city_id bigint default 0,
                  city_name varchar(80),
                  venue_id bigint,
                  venue_name varchar(160),
                  address varchar(255),
                  poster_path varchar(500),
                  banner_path varchar(500),
                  detail_image_path varchar(500),
                  price_min decimal(10,2) default 0,
                  price_max decimal(10,2) default 0,
                  summary text,
                  introduction text,
                  detail_content longtext,
                  artist_intro text,
                  venue_intro text,
                  purchase_notice text,
                  refund_notice text,
                  entry_notice text,
                  service_tags varchar(500),
                  purchase_mode varchar(32) default 'SELECTABLE',
                  publish_status varchar(32) default 'DRAFT',
                  status varchar(32) default 'COMING_SOON',
                  start_time datetime,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists performance_detail_block (
                  id bigint primary key auto_increment,
                  performance_id bigint not null,
                  block_type varchar(32) not null,
                  title varchar(255),
                  content longtext,
                  image_path varchar(500),
                  sort_order int not null default 0,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0,
                  key idx_perf_block (performance_id, sort_order)
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists venue (
                  id bigint primary key auto_increment,
                  city_id bigint not null default 0,
                  city_name varchar(80),
                  name varchar(160) not null,
                  address varchar(255),
                  intro text,
                  description text,
                  venue_type varchar(32) not null default 'THEATER',
                  stage_label varchar(80) not null default '舞台',
                  capacity int not null default 0,
                  status varchar(32) not null default 'ENABLED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists venue_area (
                  id bigint primary key auto_increment,
                  venue_id bigint not null,
                  name varchar(120) not null,
                  area_type varchar(32) not null default 'SEATED',
                  default_ticket_level varchar(120),
                  sort_order int not null default 0,
                  color varchar(32) default '#d9303e',
                  status varchar(32) not null default 'ENABLED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists seat (
                  id bigint primary key auto_increment,
                  venue_id bigint not null,
                  area_id bigint not null,
                  row_no varchar(32) not null,
                  seat_no varchar(32) not null,
                  seat_label varchar(120),
                  x int not null,
                  y int not null,
                  is_aisle tinyint not null default 0,
                  is_disabled tinyint not null default 0,
                  status varchar(32) not null default 'AVAILABLE',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists performance_session (
                  id bigint primary key auto_increment,
                  performance_id bigint,
                  movie_id bigint,
                  venue_id bigint not null,
                  session_name varchar(180),
                  hall_name varchar(120),
                  sale_start_time datetime,
                  lock_time datetime,
                  entry_time datetime,
                  start_time datetime not null,
                  end_time datetime,
                  sale_mode varchar(32) not null default 'AUTO_ALLOCATE',
                  purchase_mode varchar(32) not null default 'AUTO_ALLOCATE',
                  status varchar(32) not null default 'SCHEDULED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists ticket_level (
                  id bigint primary key auto_increment,
                  session_id bigint not null,
                  name varchar(120) not null,
                  area_id bigint,
                  price decimal(10,2) not null,
                  total_stock int not null default 0,
                  released_stock int not null default 0,
                  unreleased_stock int not null default 0,
                  sold_stock int not null default 0,
                  locked_stock int not null default 0,
                  refunded_stock int not null default 0,
                  status varchar(32) not null default 'ENABLED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists session_seat (
                  id bigint primary key auto_increment,
                  session_id bigint not null,
                  seat_id bigint not null,
                  venue_id bigint,
                  area_id bigint,
                  ticket_level_id bigint,
                  batch_id bigint,
                  status varchar(32) not null default 'AVAILABLE',
                  lock_user_id bigint,
                  lock_order_no varchar(64),
                  locked_until datetime,
                  lock_expire_time datetime,
                  seat_label varchar(120),
                  x int,
                  y int,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  unique key uk_session_seat (session_id, seat_id)
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists sale_batch (
                  id bigint primary key auto_increment,
                  session_id bigint not null,
                  name varchar(120) not null,
                  batch_name varchar(120),
                  sale_start_time datetime not null,
                  lock_time datetime not null,
                  open_mode varchar(32) not null default 'COUNT',
                  release_type varchar(32),
                  open_stock int not null default 0,
                  release_quantity int not null default 0,
                  release_ratio int not null default 0,
                  allow_return_current_round tinyint not null default 1,
                  allow_return_during_sale tinyint not null default 1,
                  limit_per_user int not null default 2,
                  purchase_limit int not null default 2,
                  queue_enabled tinyint not null default 1,
                  enable_queue tinyint not null default 1,
                  status varchar(32) not null default 'NOT_STARTED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists sale_batch_ticket_level (
                  id bigint primary key auto_increment,
                  sale_batch_id bigint not null,
                  ticket_level_id bigint not null,
                  open_stock int not null default 0,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  unique key uk_batch_level (sale_batch_id, ticket_level_id)
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists sale_batch_seat (
                  id bigint primary key auto_increment,
                  sale_batch_id bigint not null,
                  session_seat_id bigint not null,
                  status varchar(32) not null default 'OPENED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  unique key uk_batch_seat (sale_batch_id, session_seat_id)
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists stock_pool (
                  id bigint primary key auto_increment,
                  session_id bigint not null,
                  ticket_level_id bigint,
                  session_seat_id bigint,
                  source_type varchar(32) not null,
                  status varchar(32) not null default 'WAITING_RELEASE',
                  available_for_next_batch tinyint not null default 1,
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists movie (
                  id bigint primary key auto_increment,
                  title varchar(180) not null,
                  genre varchar(120),
                  release_date date,
                  duration_minutes int,
                  director varchar(120),
                  actors varchar(500),
                  rating varchar(32),
                  summary text,
                  poster_path varchar(500),
                  home_recommended tinyint not null default 0,
                  home_sort int not null default 0,
                  status varchar(32) not null default 'PUBLISHED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  deleted tinyint not null default 0
                ) engine=InnoDB default charset=utf8mb4
                """);
        statement("""
                create table if not exists reservation_remind (
                  id bigint primary key auto_increment,
                  user_id bigint not null,
                  performance_id bigint,
                  session_id bigint,
                  batch_id bigint,
                  ticket_level_id bigint,
                  quantity int not null default 1,
                  viewer_ids varchar(500),
                  status varchar(32) not null default 'RESERVED',
                  created_at datetime not null default current_timestamp,
                  updated_at datetime not null default current_timestamp on update current_timestamp,
                  unique key uk_user_session_remind (user_id, session_id)
                ) engine=InnoDB default charset=utf8mb4
                """);

        addColumn("performance", "subtitle", "varchar(255)");
        addColumn("performance", "category_name", "varchar(80)");
        addColumn("performance", "city_name", "varchar(80)");
        addColumn("performance", "venue_name", "varchar(160)");
        addColumn("performance", "address", "varchar(255)");
        addColumn("performance", "banner_path", "varchar(500)");
        addColumn("performance", "detail_image_path", "varchar(500)");
        addColumn("performance", "price_min", "decimal(10,2) default 0");
        addColumn("performance", "price_max", "decimal(10,2) default 0");
        addColumn("performance", "introduction", "text");
        addColumn("performance", "detail_content", "longtext");
        addColumn("performance", "artist_intro", "text");
        addColumn("performance", "venue_intro", "text");
        addColumn("performance", "refund_notice", "text");
        addColumn("performance", "entry_notice", "text");
        addColumn("performance", "service_tags", "varchar(500)");
        addColumn("performance", "purchase_mode", "varchar(32) default 'SELECTABLE'");
        addColumn("performance", "publish_status", "varchar(32) default 'DRAFT'");
        addColumn("performance", "home_recommended", "tinyint not null default 0");
        addColumn("performance", "home_sort", "int not null default 0");
        addColumn("performance", "start_time", "datetime");
        addColumn("movie", "actors", "varchar(500)");
        addColumn("movie", "rating", "varchar(32)");
        addColumn("movie", "home_recommended", "tinyint not null default 0");
        addColumn("movie", "home_sort", "int not null default 0");
        addColumn("movie", "status", "varchar(32) not null default 'PUBLISHED'");
        addColumn("movie", "deleted", "tinyint not null default 0");
        addColumn("venue", "city_name", "varchar(80)");
        addColumn("venue", "description", "text");
        addColumn("venue", "venue_type", "varchar(32) not null default 'THEATER'");
        addColumn("venue", "stage_label", "varchar(80) not null default '舞台'");
        addColumn("venue", "capacity", "int not null default 0");
        addColumn("venue", "status", "varchar(32) not null default 'ENABLED'");
        addColumn("venue_area", "default_ticket_level", "varchar(120)");
        addColumn("venue_area", "color", "varchar(32) default '#d9303e'");
        addColumn("venue_area", "status", "varchar(32) not null default 'ENABLED'");
        addColumn("venue_area", "deleted", "tinyint not null default 0");
        addColumn("seat", "seat_label", "varchar(120)");
        addColumn("seat", "is_aisle", "tinyint not null default 0");
        addColumn("seat", "is_disabled", "tinyint not null default 0");
        addColumn("seat", "deleted", "tinyint not null default 0");
        addColumn("performance_session", "session_name", "varchar(180)");
        addColumn("performance_session", "sale_start_time", "datetime");
        addColumn("performance_session", "lock_time", "datetime");
        addColumn("performance_session", "entry_time", "datetime");
        addColumn("performance_session", "purchase_mode", "varchar(32) default 'AUTO_ALLOCATE'");
        addColumn("ticket_level", "released_stock", "int not null default 0");
        addColumn("ticket_level", "unreleased_stock", "int not null default 0");
        addColumn("ticket_level", "sold_stock", "int not null default 0");
        addColumn("ticket_level", "locked_stock", "int not null default 0");
        addColumn("ticket_level", "refunded_stock", "int not null default 0");
        addColumn("ticket_level", "status", "varchar(32) not null default 'ENABLED'");
        addColumn("session_seat", "venue_id", "bigint");
        addColumn("session_seat", "area_id", "bigint");
        addColumn("session_seat", "batch_id", "bigint");
        addColumn("session_seat", "lock_user_id", "bigint");
        addColumn("session_seat", "lock_expire_time", "datetime");
        addColumn("session_seat", "seat_label", "varchar(120)");
        addColumn("session_seat", "x", "int");
        addColumn("session_seat", "y", "int");
        addColumn("sale_batch", "batch_name", "varchar(120)");
        addColumn("sale_batch", "release_type", "varchar(32)");
        addColumn("sale_batch", "release_quantity", "int not null default 0");
        addColumn("sale_batch", "release_ratio", "int not null default 0");
        addColumn("sale_batch", "allow_return_during_sale", "tinyint not null default 1");
        addColumn("sale_batch", "purchase_limit", "int not null default 2");
        addColumn("sale_batch", "enable_queue", "tinyint not null default 1");
        addColumn("sale_batch", "deleted", "tinyint not null default 0");
        addColumn("stock_pool", "available_for_next_batch", "tinyint not null default 1");
        addColumn("reservation_remind", "batch_id", "bigint");
        addColumn("reservation_remind", "ticket_level_id", "bigint");
        addColumn("reservation_remind", "quantity", "int not null default 1");
        addColumn("reservation_remind", "viewer_ids", "varchar(500)");
    }

    private void addColumn(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = ? and column_name = ?
                """, Integer.class, table, column);
        if (count == null || count == 0) {
            statement("alter table " + table + " add column " + column + " " + definition);
        }
    }

    private void statement(String sql) {
        jdbcTemplate.execute(sql);
    }
}
