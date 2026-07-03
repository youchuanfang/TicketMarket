# 第三阶段说明

已实现场馆、区域、座位模板、SVG 座位图、场次、票档、售票批次、Redis 库存初始化和库存池。

关键页面：`/admin/venue`、`/admin/seat-template`、`/admin/session`、`/admin/ticket-level`、`/admin/sale-batch`、`/admin/stock-pool`、`/session/:id/seats`。

Redis key：`ticket:batch:{batchId}:level:{ticketLevelId}:stock`。
