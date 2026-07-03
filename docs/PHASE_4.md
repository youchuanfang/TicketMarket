# 第四阶段说明

已实现购票主链路：购票选择、抢票排队、Redis 防超卖、选座锁座、订单确认、模拟支付、自动出票、我的订单和我的票夹。

关键接口：`/api/rush/submit`、`/api/seats/lock`、`/api/orders/create`、`/api/payment/{orderId}/pay`、`/api/user/orders`、`/api/user/tickets`。

支付仅为本地确认流程，不连接真实支付渠道。
