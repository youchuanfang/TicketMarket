# TicketMarket 票务演示系统

TicketMarket 是一个基于 Spring Boot + Vue 3 的课程项目，用于演示“演出发布 - 场馆座位 - 开售批次 - 抢票/预约 - 订单支付 - 出票 - 退票 - 检票 - 运营统计”的完整票务业务链路。系统面向本机或局域网课程演示，不接入真实票务平台、真实支付、短信或邮箱服务。

## 项目定位

- 前台用户可以浏览演出/电影、搜索筛选、实名与观演人管理、选座或抢票、模拟支付、查看订单和电子票、申请退票、接收站内消息。
- 后台管理员/票务管理员可以维护演出、场馆、区域、座位模板、场次、票档、开售批次、库存池、退票审核、统计报表、操作日志和风控日志。
- 检票员可以在后台检票入口核验电子票，并拦截重复核验、无效票和已退票。
- MySQL 保存用户、演出、场馆、场次、票档、订单、支付、电子票、退票、检票等业务状态；Redis 用于抢票阶段的实时库存扣减。

## 技术栈

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| 前端 | Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios、ECharts | 单页应用、权限路由、后台管理界面、统计图表 |
| 后端 | Spring Boot 3.3、Spring MVC、Validation、MyBatis-Plus、Knife4j、JWT | REST API、统一返回、鉴权拦截、接口文档 |
| 数据 | MySQL 8、Redis | MySQL 持久化业务数据，Redis 维护抢票库存 key |
| 工具 | Maven、npm、Docker、Python 脚本 | 构建、运行容器、抢票并发验证脚本 |

## 功能模块

### 1. 认证与权限

- 登录、注册、当前用户恢复、退出登录。
- JWT 保存用户身份与角色，后端按接口前缀校验权限。
- 角色包括 `ADMIN`、`MANAGER`、`CHECKER` 和普通用户；后台角色只允许从服务端本机登录。
- 主要代码：`AuthController`、`AuthInterceptor`、`JwtUtil`、`frontend/src/router/index.js`。

### 2. 门户浏览与搜索

- 首页聚合轮播、分类、热门演出、即将开售、正在售票、热门城市、热门场馆和电影。
- 搜索页支持关键词、城市、分类、售卖状态筛选。
- 演出详情展示海报、详情块、场次、票档、服务标签、购票/退票/入场说明。
- 前台只展示 `publish_status = PUBLISHED` 且未逻辑删除的演出。
- 主要代码：`PortalController`、`Phase3PortalController`、`HomeView.vue`、`SearchView.vue`、`PerformanceDetailView.vue`。

### 3. 后台发布与资源管理

- 演出档案：新增、编辑、发布、下架、删除、详情块排版。
- 图片资源：支持上传文件，也支持导入本机图片到 `uploads/admin/`，数据库只保存 `/uploads/...` 相对路径。
- 场馆与座位：维护场馆、区域、座位模板，支持剧场/体育场/影院等模板化座位图。
- 场次与票档：维护场次时间、售卖时间、锁票时间、票档价格、区域、库存。
- 开售批次与库存池：支持批次初始化 Redis 库存、库存池补充、释放到批次。
- 主要代码：`Phase3AdminController`、`Phase3ResourceService`、`PersistentPerformanceService`、`AdminDashboardView.vue`。

### 4. 抢票、选座与订单

- 未开售时可提交预约提醒；正在售卖时可进入抢票或图形化选座。
- 抢票接口会根据批次、票档、用户、库存和重复成功记录生成请求结果。
- 选座流程包含锁座、释放锁座、创建订单、模拟支付、出票。
- Redis 库存 key 格式：`ticket:batch:{batchId}:level:{ticketLevelId}:stock`。
- 主要代码：`Phase4TicketFlowController`、`Phase4TicketFlowService`、`PurchaseView.vue`、`SessionSeatView.vue`、`RushQueueView.vue`、`PaymentView.vue`。

### 5. 退票、检票与运营

- 用户可以对订单申请退票，管理员审核通过或驳回。
- 支付后生成电子票；检票员根据票号/入场码核验，重复核验会被拦截。
- 系统记录站内消息、操作日志、风控日志，并提供运营概览、销售、抢票、检票、退票统计。
- 主要代码：`Phase5OperationsController`、`Phase4TicketFlowService`、`TicketsView.vue`、`AdminDashboardView.vue`。

### 6. 数据初始化与持久化

- `data/schema.sql` 定义核心表结构。
- `DatabaseSchemaInitializer` 在后端启动时创建/补齐部分表字段，便于旧库升级。
- `DemoDataService` 提供演示账号、电影和初始化演出素材。
- `PersistentPerformanceService`、`Phase3ResourceService` 在业务表为空时导入演示数据，已有管理员数据时不会清空或覆盖。

## 重要接口地图

后端基础地址为 `http://localhost:8080/api`。前端未配置 `VITE_API_BASE_URL` 时，会按当前访问主机推导为 `协议://当前主机:8080/api`。

### 认证与用户

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 登录并返回 JWT |
| `POST` | `/api/auth/register` | 注册普通用户 |
| `GET` | `/api/auth/me` | 恢复当前登录用户 |
| `GET` | `/api/user/me` | 查询个人资料 |
| `POST` | `/api/user/real-name` | 实名认证 |
| `GET/POST` | `/api/user/viewers` | 查询/新增观演人 |
| `PUT/DELETE` | `/api/user/viewers/{viewerId}` | 修改/删除观演人 |

### 门户与演出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/portal/home` | 首页聚合数据 |
| `GET` | `/api/portal/categories` | 分类列表 |
| `GET` | `/api/portal/search` | 演出搜索筛选 |
| `GET` | `/api/portal/performances/{id}` | 演出详情 |
| `GET` | `/api/portal/movies/{id}` | 电影详情 |
| `GET` | `/api/portal/performances/{performanceId}/sessions` | 演出场次 |
| `GET` | `/api/portal/sessions/{sessionId}/ticket-levels` | 场次票档 |
| `GET` | `/api/portal/sessions/{sessionId}/seats` | 场次座位 |
| `GET` | `/api/portal/sessions/{sessionId}/sale-status` | 场次售卖状态 |

### 后台管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/admin/dashboard` | 后台概览 |
| `GET/POST` | `/api/admin/performances` | 演出列表/新增 |
| `PUT` | `/api/admin/performances/{id}` | 编辑演出 |
| `PUT` | `/api/admin/performances/{id}/publish` | 发布演出 |
| `PUT` | `/api/admin/performances/{id}/offline` | 下架演出 |
| `GET/POST` | `/api/admin/performances/{id}/detail-blocks` | 详情块列表/新增 |
| `POST` | `/api/admin/upload/image` | 上传图片文件 |
| `POST` | `/api/admin/upload/local-image` | 导入本机图片 |
| `GET/POST` | `/api/admin/venues` | 场馆列表/新增 |
| `POST` | `/api/admin/venues/{venueId}/seats/generate` | 生成座位模板 |
| `GET/POST` | `/api/admin/sale-batches` | 开售批次列表/新增 |
| `POST` | `/api/admin/sale-batches/{id}/init-redis-stock` | 初始化 Redis 库存 |
| `GET` | `/api/admin/stock-pool` | 库存池 |

### 抢票、订单、支付、检票

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/reservations` | 未开售预约提醒 |
| `POST` | `/api/rush/submit` | 提交抢票 |
| `GET` | `/api/rush/{requestId}` | 查询抢票请求 |
| `GET` | `/api/rush/{requestId}/result` | 查询抢票结果 |
| `POST` | `/api/seats/lock` | 锁定座位 |
| `POST` | `/api/seats/release` | 释放座位 |
| `POST` | `/api/orders/create` | 创建订单 |
| `POST` | `/api/orders/{id}/cancel` | 取消订单 |
| `POST` | `/api/payment/{orderId}/pay` | 模拟支付 |
| `GET` | `/api/user/orders` | 我的订单 |
| `GET` | `/api/user/tickets` | 我的电子票 |
| `POST` | `/api/user/orders/{orderId}/refund` | 申请退票 |
| `POST` | `/api/admin/refunds/{id}/approve` | 通过退票 |
| `POST` | `/api/admin/refunds/{id}/reject` | 驳回退票 |
| `POST` | `/api/checker/tickets/verify` | 检票核验 |

完整接口补充见 [API.md](API.md)，数据表说明见 [DATABASE.md](DATABASE.md)。

## 目录结构

```text
TicketMarket/
├─ backend/                 # Spring Boot 后端
│  ├─ src/main/java/com/ticketmarket/
│  │  ├─ controller/        # REST API 控制器
│  │  ├─ service/           # 业务流程、初始化、持久化服务
│  │  ├─ config/            # CORS、鉴权拦截、上下文
│  │  ├─ dto/               # 请求/响应 DTO
│  │  ├─ model/             # 领域模型
│  │  └─ util/              # JWT、密码工具
│  └─ src/main/resources/application.yml
├─ frontend/                # Vue 3 前端
│  ├─ src/api/              # Axios API 封装
│  ├─ src/router/           # 路由与权限跳转
│  ├─ src/stores/           # Pinia 用户状态
│  └─ src/views/            # 前台、用户中心、后台页面
├─ data/schema.sql          # MySQL 初始化脚本
├─ scripts/rush_test.py     # 本地抢票并发验证脚本
├─ uploads/                 # 演示图片资源与后台上传目录
├─ docs/                    # 阶段记录、运行验证、报告/答辩材料
├─ API.md                   # 接口说明
├─ DATABASE.md              # 数据库说明
└─ DEMO.md                  # 演示流程
```

## 环境要求

- Windows 或兼容 Docker、Java、Node.js 的开发环境
- Java 17
- Maven 3.8+
- Node.js 18+
- Docker Desktop
- MySQL 8.x，默认容器名 `mysql-ticket`
- Redis，默认容器名 `redis-ticket`

默认后端配置位于 `backend/src/main/resources/application.yml`。其中数据库账号、JWT secret 和演示账号只适合本地课程演示；如果部署到真实环境，需要改为环境变量或外部配置，并替换默认密钥和密码。

## 快速启动

### 1. 启动 MySQL

```bash
docker run -d --name mysql-ticket -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ticket_market mysql:8.0
docker exec -i mysql-ticket mysql -uroot -proot ticket_market < data/schema.sql
```

如果容器已经存在，使用：

```bash
docker start mysql-ticket
```

### 2. 启动 Redis

```bash
docker run -d --name redis-ticket -p 6379:6379 redis:latest
docker exec -it redis-ticket redis-cli ping
```

返回 `PONG` 表示 Redis 可用。如果容器已经存在，使用：

```bash
docker start redis-ticket
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8080`，接口文档入口为 `http://localhost:8080/swagger-ui.html`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认访问地址为 `http://localhost:5173`。

## 构建与验证记录

已在本项目当前工作区执行并通过：

```bash
cd backend
mvn -q -DskipTests package
```

```bash
cd frontend
npm run build
```

前端构建成功，同时 Vite 输出了大 chunk 警告和第三方依赖注释提示；这些属于构建优化提示，不影响当前课程演示包产物生成。`mvn test`、完整本地联调、`scripts/rush_test.py` 并发抢票脚本需要在 MySQL、Redis、后端服务均启动后再运行验证。

更完整的复现步骤、验证记录和截图清单见 [docs/run-and-verification.md](docs/run-and-verification.md)。

## 演示账号

以下账号来自本地演示数据，仅用于课程演示：

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 系统管理员 | `admin` | `admin123` | 仅允许服务端本机登录后台 |
| 票务管理员 | `manager` | `manager123` | 仅允许服务端本机登录后台 |
| 检票员 | `checker` | `checker123` | 可进入检票管理 |

普通用户可以通过注册入口创建。真实部署时不要使用这些默认账号和密码。

## 数据模型概览

核心表分为八组：

- 用户与权限：`user`、`role`、`user_role`、`real_name_auth`、`viewer`。
- 门户基础数据：`city`、`performance_category`、`banner`、`home_section`、`movie`。
- 演出发布：`performance`、`performance_detail_block`、`performance_artist`。
- 场馆座位：`venue`、`venue_area`、`seat`、`session_seat`。
- 场次票档批次：`performance_session`、`ticket_level`、`sale_batch`、`sale_batch_ticket_level`、`stock_pool`。
- 抢票订单：`rush_request`、`ticket_order`、`order_item`、`payment_record`、`e_ticket`。
- 退票检票：`refund_apply`、`refund_record`、`checkin_record`。
- 运营辅助：`message`、`announcement`、`operation_log`、`risk_log`、`blacklist`、`search_history`、`hot_search`。

报告写作时可以把“MySQL 负责最终一致业务状态，Redis 负责高频库存扣减”作为系统设计重点展开。

## 报告与答辩写作线索

后续写课程报告时，建议按下面顺序组织：

1. 项目背景：真实票务流程复杂，课程项目用本地演示系统还原核心业务闭环。
2. 需求分析：普通用户、管理员/票务管理员、检票员三类角色分别有什么操作。
3. 总体架构：Vue 前端、Spring Boot 后端、MySQL 持久化、Redis 实时库存。
4. 数据库设计：用户、演出、场馆座位、场次票档、批次库存、订单支付、电子票、退票检票。
5. 核心流程：后台发布演出、前台查询购票、抢票扣库存、支付出票、退票回补、检票核验。
6. 关键实现：JWT 权限、后台本机登录限制、发布状态过滤、Redis 库存 key、重复抢票/重复检票拦截。
7. 测试验证：后端构建、前端构建、接口联调、演示脚本、并发抢票脚本。
8. 项目价值与不足：完整流程、持久化发布、局域网演示适配；不足是未接入真实支付、未做生产级分布式队列和外部通知。

可直接展开的报告/答辩提纲见 [docs/course-report-and-defense.md](docs/course-report-and-defense.md)。

## 已知边界

- 系统只用于课程演示，不保证生产级安全、支付合规、风控能力或高并发能力。
- 默认账号、数据库密码和 JWT secret 是本地演示配置，不能用于公网部署。
- 模拟支付不会调用真实支付平台；短信、邮箱、外部票务平台均未接入。
- Redis 库存需要 Redis 服务可用；并发抢票脚本依赖后端、MySQL、Redis 都已启动。
- 当前前端构建产物较大，后续可以用路由懒加载和手动分包优化。

## 相关文档

- [API.md](API.md)：接口路径、请求说明和权限说明。
- [DATABASE.md](DATABASE.md)：数据库表结构和持久化策略。
- [DEMO.md](DEMO.md)：课堂演示流程。
- [docs/run-and-verification.md](docs/run-and-verification.md)：运行、构建、验证和截图清单。
- [docs/course-report-and-defense.md](docs/course-report-and-defense.md)：课程报告与答辩提纲。
