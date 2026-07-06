# Run And Verification

本文档记录 TicketMarket 的本地运行、构建验证、可复现命令和后续截图清单。内容用于支撑课程提交和 README 中的运行说明。

## Prerequisites

- Java 17
- Maven 3.8+
- Node.js 18+
- Docker Desktop
- MySQL 8.x 容器，建议名称 `mysql-ticket`
- Redis 容器，建议名称 `redis-ticket`

默认后端端口为 `8080`，默认前端端口为 `5173`。默认 MySQL 数据库为 `ticket_market`，默认 Redis 端口为 `6379`。

## Verified Environment

本轮文档整理中已实际执行并通过以下命令：

| 时间 | 命令 | 结果 |
| --- | --- | --- |
| 2026-07-06 | `cd backend && mvn test` | 通过，Spring Boot 上下文测试成功 |
| 2026-07-06 | `cd frontend && npm run build` | 通过，Vite 生成 `dist/` 产物 |

前端构建输出中出现两个说明：

- `node_modules/@vueuse/core` 中的 `/* #__PURE__ */` 注释位置无法被 Rollup 解释，构建时会移除该注释。
- 单个 JS chunk 超过 500 kB，后续可通过动态导入或 `manualChunks` 优化。

这两项是构建优化提示，不影响当前课程演示构建通过。

## Install And Run

### 1. MySQL

首次创建容器：

```bash
docker run -d --name mysql-ticket -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ticket_market mysql:8.0
```

导入初始化表结构：

```bash
docker exec -i mysql-ticket mysql -uroot -proot ticket_market < data/schema.sql
```

已有容器时启动：

```bash
docker start mysql-ticket
```

### 2. Redis

首次创建容器：

```bash
docker run -d --name redis-ticket -p 6379:6379 redis:latest
```

验证 Redis：

```bash
docker exec -it redis-ticket redis-cli ping
```

期望返回：

```text
PONG
```

已有容器时启动：

```bash
docker start redis-ticket
```

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

期望结果：

- 后端启动在 `http://localhost:8080`。
- Swagger UI 可访问 `http://localhost:8080/swagger-ui.html`。
- 首次启动时，数据库表为空则导入演示账号、演出、场馆、座位、场次、票档和批次数据。

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

期望结果：

- 前端启动在 `http://localhost:5173`。
- 未配置 `VITE_API_BASE_URL` 时，前端会按当前访问主机推导后端地址为 `协议://当前主机:8080/api`。

## Build Commands

后端构建：

```bash
cd backend
mvn test
```

前端构建：

```bash
cd frontend
npm run build
```

## Test And Verification Commands

### Backend Context Test

项目包含 `backend/src/test/java/com/ticketmarket/TicketMarketApplicationTests.java`，测试目标是 Spring Boot 上下文加载。

```bash
cd backend
mvn test
```

本轮已在当前工作区执行通过。该测试会读取 `application.yml` 并启动 Spring Boot 上下文，因此本地 MySQL 配置需要可用。

### Rush Stress Script

抢票并发脚本位于 `scripts/rush_test.py`。它会注册压测用户、初始化批次库存，并发提交抢票请求，最后检查成功数量不超过库存且剩余库存不为负。

运行前要求：

- MySQL 容器已启动并导入表结构。
- Redis 容器已启动。
- 后端已启动在 `http://localhost:8080`。
- 演示批次 `3001`、票档 `2001`、场次 `1001` 存在。

运行命令：

```bash
python scripts/rush_test.py
```

期望输出中 `passed` 为 `true`。

## Manual Verification Flow

建议按以下顺序完成课堂演示联调：

1. 访问 `http://localhost:5173`，确认首页、分类、搜索和演出详情正常展示。
2. 注册普通用户并登录，完成实名认证和观演人维护。
3. 使用本机浏览器登录 `admin / admin123`，进入后台运营概览。
4. 在“首页推荐”中选择热门推荐和分类推荐内容，回到首页确认轮播和卡片自动排版。
5. 在演出发布中新增或编辑已发布演出，维护多场次、多票档、富文本详情和退票时间节点，保存后关闭再打开确认仍存在。
6. 在场馆管理中确认只显示剧院和体育场馆；在电影院管理中确认电影院、影厅和座位图。
7. 普通用户进入演出详情，选择场次和票档，未开售时预约，开售后抢票或选座，确认限购 4 张且每张票需要观演人。
8. 普通用户进入电影详情，根据当前城市选择日期、电影院、放映时间和座位，确认电影不要求观演人且最多 4 座。
9. 抢票或选座成功后进入订单确认、模拟支付、电子票查看。
10. 检票员登录后台检票入口，核验电子票，再次核验同一票号验证重复入场拦截。
11. 用户申请退票，管理员审核通过，检查订单、电子票、座位状态和站内消息状态变化。
12. 查看统计报表、操作日志和风控日志。

## 2026-07-06 Feature Verification Notes

本轮代码层面已覆盖并构建通过：

- 后台演出编辑会重新读取单条完整详情，富文本详情、退票时间节点、场次和票档不再只停留在一次性表单。
- 电影排片后台只保留城市、电影院、影厅、放映时间和票价，购票库存由影厅座位状态决定。
- 电影选座不要求观演人，最多选择 4 个座位；演出购票仍要求每张票对应观演人。
- 场馆数据启动补齐服务会清理 `codex自测` 数据，并按已有城市补齐电影院、体育场馆和剧院。
- 前台城市选择只切换所在地，不再自动跳转到城市搜索页。

## Screenshot Checklist

截图不是代码质量证据，但适合作为课程报告和答辩展示材料。建议至少准备：

| 文件名建议 | 内容 | 用途 |
| --- | --- | --- |
| `screenshots/01-home.png` | 前台首页 | 展示门户聚合能力 |
| `screenshots/02-search.png` | 搜索筛选页 | 展示检索与分类 |
| `screenshots/03-performance-detail.png` | 演出详情、场次、票档 | 展示购票入口 |
| `screenshots/04-admin-performance.png` | 后台演出发布 | 展示管理员工作流 |
| `screenshots/05-seat-template.png` | 座位图或座位模板 | 展示场馆座位设计 |
| `screenshots/06-rush-queue.png` | 抢票等待/结果页 | 展示抢票流程 |
| `screenshots/07-payment-ticket.png` | 支付成功与电子票 | 展示出票结果 |
| `screenshots/08-checkin.png` | 检票核验 | 展示入场核验 |
| `screenshots/09-refund.png` | 退票申请和审核 | 展示售后流程 |
| `screenshots/10-statistics.png` | 统计报表 | 展示运营数据 |

## Common Problems

### MySQL 容器已存在

```bash
docker start mysql-ticket
```

如果需要重新导入表结构，先确认数据可以清空，再删除并重建容器。课程演示中不建议随意删除已有容器。

### Redis 无法连接

```bash
docker start redis-ticket
docker exec -it redis-ticket redis-cli ping
```

如果后端已启动，Redis 后启动后可重新触发后台“初始化库存”接口。

### 后台账号无法从局域网登录

这是设计行为。`ADMIN`、`MANAGER`、`CHECKER` 角色只能从服务端本机 `localhost` 或 `127.0.0.1` 登录，局域网设备用于普通用户购票演示。

### 前端接口地址不正确

检查是否设置了 `VITE_API_BASE_URL`。未设置时，前端会根据访问主机自动推导 `:8080/api`。

### 前端构建 chunk 过大

当前不影响演示。后续优化方向：

- 路由级懒加载后台管理页。
- 将 ECharts、Element Plus 等依赖拆分为独立 chunk。
- 使用 `build.rollupOptions.output.manualChunks`。

## Shutdown

停止前端和后端：在对应终端按 `Ctrl+C`。

停止容器：

```bash
docker stop redis-ticket
docker stop mysql-ticket
```

如果只是第二天继续演示，保留容器即可，避免丢失本地数据库数据。
