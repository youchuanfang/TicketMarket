# API 说明

后端基础路径：`http://localhost:8080/api`。前端未配置 `VITE_API_BASE_URL` 时，会根据当前访问主机自动推导为 `协议://当前主机:8080/api`。

统一返回格式：

```json
{
  "code": 0,
  "message": "OK",
  "data": {}
}
```

登录后请求需要携带：

```http
Authorization: Bearer <token>
```

权限返回码：

- `401`：未登录或 token 失效，前端跳转 `/login?redirect=原页面`
- `403`：token 有效但角色权限不足，前端跳转 `/403`

## 认证

`POST /api/auth/login`

```json
{
  "username": "普通用户账号",
  "password": "本机演示密码"
}
```

`POST /api/auth/register`

```json
{
  "username": "newuser",
  "password": "自定义密码",
  "nickname": "新用户"
}
```

`GET /api/auth/me`

返回当前登录用户信息，前端刷新页面时用它恢复登录状态。

`POST /api/auth/logout`

前端退出登录时调用；即使接口不可用，前端也会清除本地 token 和用户信息。

## 用户

`GET /api/user/me`

返回当前用户资料。

`POST /api/user/real-name`

```json
{
  "realName": "张三",
  "idCard": "330100199901011234"
}
```

`GET /api/user/viewers`

返回当前用户观演人。

`POST /api/user/viewers`

```json
{
  "name": "张三",
  "idCard": "330100199901011234",
  "phone": "13800138000"
}
```

`PUT /api/user/viewers/{viewerId}`

修改观演人。

`DELETE /api/user/viewers/{viewerId}`

删除观演人。

`PUT /api/user/viewers/{viewerId}/default`

设置默认观演人。

## 门户

`GET /api/portal/home`

返回首页轮播图、分类、热门推荐、即将开售、正在售票、票量动态、热门城市、热门场馆、电影。

`GET /api/portal/categories`

返回分类列表。

`GET /api/portal/search`

查询参数：

- `keyword`
- `city`
- `category`
- `status`

`GET /api/portal/performances/{id}`

返回演出详情、场次、票档、服务标签、规则说明。

`GET /api/portal/movies/{id}`

返回电影详情和放映场次。

## 后台

`GET /api/admin/dashboard`

需要 JWT，角色必须为 `ADMIN` 或 `MANAGER`。

## 检票

`GET /api/checker/dashboard`

需要 JWT，角色必须为 `CHECKER`、`ADMIN` 或 `MANAGER`。

## 前端权限路由

- 游客：`/`、`/category`、`/search`、`/performance/:id`、`/movie/:id`、`/login`
- 登录用户：`/user`、`/profile`、`/viewers`、`/orders`、`/tickets`、`/messages`、`/real-name`
- 后台：`/admin/**`
- 无权限页：`/403`

## 票务资源

- `GET /api/admin/venues`
- `POST /api/admin/venues`
- `GET /api/admin/venues/{venueId}/areas`
- `POST /api/admin/venues/{venueId}/seats/generate`
- `GET /api/admin/sessions`
- `GET /api/admin/sessions/{sessionId}/ticket-levels`
- `GET /api/admin/sale-batches`
- `POST /api/admin/sale-batches/{id}/init-redis-stock`
- `GET /api/admin/stock-pool`
- `GET /api/portal/performances/{performanceId}/sessions`
- `GET /api/portal/sessions/{sessionId}/ticket-levels`
- `GET /api/portal/sessions/{sessionId}/seats`
- `GET /api/portal/sessions/{sessionId}/active-batch`
- `GET /api/portal/sessions/{sessionId}/sale-status`

Redis 库存 key：

```text
ticket:batch:{batchId}:level:{ticketLevelId}:stock
```

## 抢票订单

- `POST /api/rush/submit`
- `POST /api/reservations`
- `GET /api/rush/{requestId}`
- `GET /api/rush/{requestId}/result`
- `POST /api/seats/lock`
- `POST /api/seats/release`
- `GET /api/seats/locks/{sessionId}`
- `POST /api/orders/create`
- `POST /api/orders/{id}/cancel`
- `GET /api/user/orders`
- `GET /api/user/orders/{id}`
- `POST /api/payment/{orderId}/pay`
- `GET /api/payment/{orderId}`
- `GET /api/user/tickets`
- `GET /api/user/tickets/{id}`

前台售卖状态：

- `RESERVABLE`：未开售，可提交预约抢票，不下单、不扣库存、不锁座
- `ON_SALE`：正在售卖，可提交抢票或进入选座
- `SOLD_OUT`：缺货中，按钮禁用
- `ENDED`：已结束
- `UNAVAILABLE`：暂不可售

## 运营核验

- `POST /api/user/orders/{orderId}/refund`
- `GET /api/user/refunds`
- `GET /api/admin/refunds`
- `POST /api/admin/refunds/{id}/approve`
- `POST /api/admin/refunds/{id}/reject`
- `POST /api/checker/tickets/verify`
- `GET /api/checker/checkins`
- `GET /api/admin/checkins`
- `GET /api/user/messages`
- `POST /api/user/messages/{id}/read`
- `GET /api/admin/statistics/overview`
- `GET /api/admin/operation-logs`
- `GET /api/admin/risk-logs`
