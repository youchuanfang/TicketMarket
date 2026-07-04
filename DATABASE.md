# 数据库设计说明

数据库名：`ticket_market`。初始化脚本：`data/schema.sql`。

## 演出发布相关表

### performance

保存管理员发布的演出主档案。前台只读取 `publish_status = PUBLISHED` 且 `deleted = 0` 的记录。

关键字段：

- `title` / `subtitle`
- `category_id` / `category_name`
- `city_id` / `city_name`
- `venue_id` / `venue_name` / `address`
- `poster_path` / `banner_path` / `detail_image_path`
- `price_min` / `price_max`
- `summary` / `introduction` / `detail_content`
- `artist_intro` / `venue_intro`
- `purchase_notice` / `refund_notice` / `entry_notice`
- `service_tags`
- `purchase_mode`
- `publish_status`: `DRAFT`、`PUBLISHED`、`OFFLINE`
- `status`: 前台售卖展示状态，例如 `COMING_SOON`、`ON_SALE`、`LOCKED`
- `start_time`
- `created_at` / `updated_at` / `deleted`

### performance_detail_block

保存详情页排版块。前台按 `sort_order` 渲染。

字段：

- `performance_id`
- `block_type`: `TITLE`、`PARAGRAPH`、`IMAGE`
- `title`
- `content`
- `image_path`
- `sort_order`
- `created_at` / `updated_at` / `deleted`

## 场馆与座位

- `venue`: 场馆名称、城市、地址、容量、介绍、启用状态。
- `venue_area`: 区域名称、类型、默认票档、颜色和排序。
- `seat`: 座位坐标、排号、座号、座位标签、禁用状态。
- `session_seat`: 某个场次下的座位状态，包含 `AVAILABLE`、`LOCKED`、`SOLD`、`DISABLED` 等。

## 场次、票档、批次

### performance_session

持久化演出场次：

- `performance_id`
- `venue_id`
- `session_name`
- `sale_start_time`
- `lock_time`
- `entry_time`
- `start_time`
- `end_time`
- `purchase_mode`
- `status`

### ticket_level

持久化票档和库存：

- `session_id`
- `area_id`
- `price`
- `total_stock`
- `released_stock`
- `unreleased_stock`
- `sold_stock`
- `locked_stock`
- `refunded_stock`
- `status`

### sale_batch

持久化开售批次：

- `session_id`
- `batch_name`
- `sale_start_time`
- `lock_time`
- `release_type`
- `release_quantity`
- `release_ratio`
- `purchase_limit`
- `enable_queue`
- `status`: `NOT_STARTED`、`SELLING`、`LOCKED`、`CLOSED`

后端启动或查询时会根据 `sale_start_time` 和 `lock_time` 自动刷新批次状态：到开售时间后从 `NOT_STARTED` 变为 `SELLING`，到锁票时间后从 `SELLING` 变为 `LOCKED`。Redis 库存可通过后台“初始化库存”重新从 MySQL 票档和批次计算写入。

## 订单、支付、出票、退票、检票

- `ticket_order`: 订单主表，保存订单状态和金额。
- `order_item`: 订单明细，关联票档、座位和观演人。
- `payment_record`: 模拟支付记录。
- `e_ticket`: 电子票，保存票号、二维码内容和票状态。
- `refund_apply`: 退票申请。
- `refund_record`: 退票处理记录。
- `checkin_record`: 检票记录。

订单状态：

- `PENDING_PAYMENT`
- `PAID`
- `TICKET_ISSUED`
- `CANCELLED`
- `REFUND_APPLYING`
- `REFUNDED`
- `CHECKED_IN`

电子票状态：

- `UNUSED`
- `CHECKED_IN`
- `REFUNDED`
- `INVALID`

## 初始化策略

`DemoDataService` 不再作为后台新增数据的数据源。它只提供演示账号、电影和初始演出素材。应用启动时：

1. `DatabaseSchemaInitializer` 创建或补齐 MySQL 表字段。
2. `PersistentPerformanceService` 检查 `performance` 是否为空，为空才导入演示演出。
3. `Phase3ResourceService` 检查场馆和场次相关表是否为空，为空才导入演示场馆、座位、场次、票档和批次。
4. 如果表内已有管理员创建的数据，启动流程不会清空或覆盖。
