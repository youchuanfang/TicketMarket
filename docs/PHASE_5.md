# 第五阶段说明

已实现退票申请、后台审核、库存回流或入池、检票核验、站内信、统计概览、操作日志和风控日志。

关键接口：`/api/user/orders/{orderId}/refund`、`/api/admin/refunds/{id}/approve`、`/api/checker/tickets/verify`、`/api/user/messages`、`/api/admin/statistics/overview`、`/api/admin/risk-logs`。

检票支持票号或入场码，已退票、已核验和无效票会给出明确结果。
