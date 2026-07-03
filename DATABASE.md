# 数据库设计说明

数据库名：`ticket_market`。初始化脚本位于 `data/schema.sql`。

## 设计原则

- 不使用 `order` 作为表名，订单表使用 `ticket_order`。
- 每张业务表包含 `id`、`created_at`、`updated_at`。
- 需要软删除的表增加 `deleted` 字段，`0` 表示正常，`1` 表示已删除。
- 关键状态字段使用明确枚举字符串，便于课程答辩时解释业务状态。
- 抢票并发阶段使用 Redis 保存当前批次库存，MySQL 保存最终订单和请求记录。

## 核心表分组

用户与权限：

- `user`
- `role`
- `user_role`
- `real_name_auth`
- `viewer`

门户内容：

- `city`
- `performance_category`
- `banner`
- `home_section`
- `home_section_item`
- `performance`
- `performance_artist`
- `movie`
- `movie_cast`

场馆与座位：

- `venue`
- `venue_area`
- `seat`
- `performance_session`
- `ticket_level`
- `session_seat`

售票批次和库存：

- `sale_batch`
- `sale_batch_ticket_level`
- `sale_batch_seat`
- `stock_pool`
- `rush_request`

订单、支付和电子票：

- `ticket_order`
- `order_item`
- `payment_record`
- `e_ticket`

退票与检票：

- `refund_apply`
- `refund_record`
- `checkin_record`

运营与风控：

- `reservation_remind`
- `favorite`
- `browse_history`
- `message`
- `announcement`
- `operation_log`
- `risk_log`
- `blacklist`
- `search_history`
- `hot_search`

## 主要枚举

购票模式 `sale_mode`：

- `SELECTABLE`：支持自主选座
- `AUTO_ALLOCATE`：不支持选座，系统自动分配座位
- `AREA_ONLY`：只选票档或区域
- `STANDING`：站席，无具体座位

抢票请求状态 `rush_request.status`：

- `WAITING`
- `PROCESSING`
- `SUCCESS`
- `FAILED`
- `SOLD_OUT`
- `DUPLICATE`
- `NOT_STARTED`
- `LOCKED`
- `LIMITED`
- `NO_AUTH`
- `NETWORK_UNKNOWN`

订单状态 `ticket_order.status`：

- `PENDING_PAYMENT`
- `PAID`
- `TICKET_ISSUED`
- `CANCELLED`
- `TIMEOUT_CLOSED`
- `REFUND_APPLYING`
- `REFUNDED`
- `CHECKED_IN`
- `FAILED`

电子票状态 `e_ticket.status`：

- `UNUSED`
- `CHECKED_IN`
- `REFUNDED`
- `INVALID`
- `EXPIRED`

座位状态 `session_seat.status`：

- `UNOPENED`
- `AVAILABLE`
- `LOCKED`
- `SOLD`
- `SELECTED`
- `UNAVAILABLE`
- `RECYCLED_AFTER_LOCK`

售票批次状态 `sale_batch.status`：

- `NOT_STARTED`
- `SELLING`
- `LOCKED`
- `CLOSED`

