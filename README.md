# TicketMarket 票务演示系统

基于 Spring Boot + Vue 3 的课程项目，覆盖演出发布、场馆座位、开售批次、抢票、订单、支付、出票、退票和检票流程。系统只用于本机或局域网演示，不接入真实票务平台和真实支付。

## 环境要求

- Windows
- Java 17
- Maven 3.8+
- Node.js 18+
- Docker Desktop
- MySQL 8.x 容器：`mysql-ticket`
- Redis 容器：`redis-ticket`

## MySQL

课程项目使用 MySQL 持久化后台发布数据和票务状态。

默认配置：

- 容器名：`mysql-ticket`
- 数据库名：`ticket_market`
- 用户：`root`
- 密码：`root`
- 端口：`3306`

启动 MySQL：

```bash
docker run -d --name mysql-ticket -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ticket_market mysql:8.0
```

导入表结构：

```bash
docker exec -i mysql-ticket mysql -uroot -proot ticket_market < data/schema.sql
```

后端启动时也会执行轻量字段补齐，避免旧库缺少演出发布字段。

## Redis

```bash
docker run -d --name redis-ticket -p 6379:6379 redis:latest
docker exec -it redis-ticket redis-cli ping
```

返回 `PONG` 即可。Redis 用于抢票阶段的实时批次库存；MySQL 保存场次、批次、票档、订单和最终状态。后端重启后可根据 MySQL 批次和票档重新初始化 Redis 库存。

## 后端启动

```bash
cd backend
mvn spring-boot:run
```

构建：

```bash
cd backend
mvn -q -DskipTests package
```

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

构建：

```bash
cd frontend
npm run build
```

## 持久化范围

以下后台数据写入 MySQL，后端重启后仍然存在：

- 演出档案：标题、分类、城市、场馆、海报、价格、发布状态、购票须知等。
- 演出详情块：标题、段落、图片和排序。
- 场馆、区域、座位图。
- 场次、票档、开售批次、库存池。
- 订单、支付记录、电子票、退票申请、检票记录。

`DemoDataService` 仅保留账号、电影和初始化演示数据来源。启动时如果 MySQL 业务表为空，会导入演示数据；如果已有管理员数据，不会清空或覆盖。

## 图片上传

后台图片上传接口：

```text
POST /api/admin/upload/image
```

图片保存到仓库外层 `uploads/admin/` 目录，返回相对路径，例如：

```text
/uploads/admin/xxxxxxxx.jpg
```

演出保存时只写入相对路径，不写死 `localhost`，因此局域网访问时浏览器会按当前后端主机加载 `/uploads/**` 静态资源。

## 后台账号

后台账号只允许通过服务端本机 `localhost` 或 `127.0.0.1` 登录。

- `admin / admin123`
- `manager / manager123`
- `checker / checker123`

## 主要接口

- `GET /api/admin/performances`
- `POST /api/admin/performances`
- `PUT /api/admin/performances/{id}`
- `PUT /api/admin/performances/{id}/publish`
- `PUT /api/admin/performances/{id}/offline`
- `GET /api/admin/performances/{id}/detail-blocks`
- `POST /api/admin/performances/{id}/detail-blocks`
- `PUT /api/admin/performance-detail-blocks/{blockId}`
- `PUT /api/admin/performances/{id}/detail-blocks/reorder`
- `GET /api/portal/search`
- `GET /api/portal/performances/{id}`
- `GET /api/portal/performances/{id}/sessions`

前台只展示 `publish_status = PUBLISHED` 且未删除的演出；草稿和下架演出后台可见，前台不可见。
