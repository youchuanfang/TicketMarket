# API 说明

后端基础路径：`http://localhost:8080`。

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
  "username": "user01",
  "password": "user123"
}
```

`POST /api/auth/register`

```json
{
  "username": "newuser",
  "password": "user123",
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

返回首页轮播图、分类、热门推荐、即将开售、正在售票、回流票、热门城市、热门场馆、电影。

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

## 后续阶段接口规划

第三阶段：

- 场馆 CRUD
- 区域 CRUD
- 座位模板生成
- 座位图查询
- 场次、票档、售票批次管理

第四阶段：

- 抢票请求提交
- 抢票结果轮询
- Redis 库存预扣
- 锁座
- 创建订单
- 取消订单
- 模拟支付
- 出票

第五阶段：

- 退票申请
- 退票审核
- 回流票入池
- 检票核验
- 站内信
- 统计报表
- 操作日志
- 风控日志
