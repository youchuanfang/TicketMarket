# Course Report And Defense

本文档用于后续撰写课程报告和准备 5-8 分钟答辩。它不是正式报告全文，而是可直接扩写的结构化素材。

## 1. 背景与目的

真实票务平台通常包含演出发布、场馆座位、票档批次、库存扣减、订单支付、出票、退票和检票等多个环节。TicketMarket 的目标是在本地课程项目中还原这条核心业务链路，使前台用户、后台管理员和检票员都能完成可演示的闭环操作。

报告中可以强调：本项目不是接入真实票务平台，而是用本地数据和模拟支付构建一个可复现、可讲解、可扩展的票务业务系统。

## 2. 需求分析

### 普通用户

- 浏览首页、分类、搜索和演出/电影详情。
- 注册登录、实名认证、维护观演人。
- 根据场次和票档进行预约、抢票或选座。
- 创建订单、模拟支付、查看电子票。
- 申请退票、查看站内消息。

### 管理员/票务管理员

- 管理演出档案、详情块、图片、发布状态。
- 管理场馆、区域、座位模板、场次、票档。
- 配置开售批次、初始化 Redis 库存、维护库存池。
- 审核退票、查看统计报表、操作日志和风控日志。

### 检票员

- 登录后台检票入口。
- 根据票号或入场码核验电子票。
- 对重复核验、已退票、无效票给出拦截结果。

## 3. 总体架构

系统采用前后端分离结构：

- 前端：Vue 3 + Vite，负责页面展示、路由权限、表单交互和后台管理工作台。
- 后端：Spring Boot，提供 REST API、JWT 鉴权、业务流程处理和统一返回格式。
- MySQL：保存最终业务数据，包括用户、演出、座位、批次、订单、电子票、退票、检票记录。
- Redis：用于抢票阶段的实时库存扣减，降低高频库存判断对 MySQL 的压力。

建议报告配图：

- 一张系统架构图：浏览器、Vue 前端、Spring Boot 后端、MySQL、Redis。
- 一张业务流程图：后台发布、前台购票、支付出票、退票、检票。

## 4. 数据库设计

数据库名为 `ticket_market`，初始化脚本为 `data/schema.sql`。主要表可按业务域说明：

| 业务域 | 表 | 说明 |
| --- | --- | --- |
| 用户权限 | `user`、`role`、`user_role`、`real_name_auth`、`viewer` | 登录身份、角色、实名和观演人 |
| 门户内容 | `city`、`performance_category`、`banner`、`home_section`、`movie` | 首页、分类、城市、电影 |
| 演出发布 | `performance`、`performance_detail_block`、`performance_artist` | 演出主档案和详情页内容 |
| 场馆座位 | `venue`、`venue_area`、`seat`、`session_seat` | 场馆、区域、座位和场次座位状态 |
| 批次库存 | `performance_session`、`ticket_level`、`sale_batch`、`stock_pool` | 场次、票档、开售批次和库存池 |
| 交易出票 | `rush_request`、`ticket_order`、`order_item`、`payment_record`、`e_ticket` | 抢票、订单、支付和电子票 |
| 售后检票 | `refund_apply`、`refund_record`、`checkin_record` | 退票申请、处理和核验 |
| 运营辅助 | `message`、`announcement`、`operation_log`、`risk_log`、`blacklist` | 通知、公告、日志和风控 |

报告重点可以放在三组关系：

- `performance -> performance_session -> ticket_level -> sale_batch`：从演出到售卖批次。
- `venue -> venue_area -> seat -> session_seat`：从场馆模板到具体场次座位状态。
- `rush_request -> ticket_order -> payment_record -> e_ticket -> checkin_record`：从抢票到入场核验。

## 5. 关键实现

### JWT 与角色权限

登录成功后后端生成 JWT，Token 中包含用户 ID、用户名和角色。`AuthInterceptor` 根据请求路径判断是否需要登录，并限制后台接口和检票接口的角色范围。后台角色登录还额外限制来源必须是服务端本机，避免局域网演示时后台账号被其他设备直接登录。

代码证据：

- `backend/src/main/java/com/ticketmarket/controller/AuthController.java`
- `backend/src/main/java/com/ticketmarket/config/AuthInterceptor.java`
- `backend/src/main/java/com/ticketmarket/util/JwtUtil.java`
- `frontend/src/router/index.js`

### 演出发布与前台过滤

后台可以创建、编辑、发布、下架演出，并维护详情块。前台门户查询时只展示 `publish_status = PUBLISHED` 且未删除的演出，从而把草稿、下架和后台编辑态与用户浏览态隔离。

代码证据：

- `Phase3AdminController`
- `PersistentPerformanceService`
- `PortalController`
- `SearchView.vue`

### 场馆座位与票档批次

管理员可以维护场馆、区域、座位模板、场次和票档。开售批次用于控制具体售卖时间、锁票时间、库存释放数量和 Redis 初始化。体育场模板支持中心舞台、内场和看台区块，适合演唱会类场景展示。

代码证据：

- `Phase3ResourceService`
- `Phase3AdminController`
- `AdminDashboardView.vue`
- `data/schema.sql`

### Redis 抢票库存

抢票阶段使用 Redis key `ticket:batch:{batchId}:level:{ticketLevelId}:stock` 保存批次票档库存。抢票请求会检查批次、票档、用户重复成功记录和库存，成功后生成待支付订单。这个设计可以在报告中作为“高频库存扣减与最终业务状态分离”的项目强项说明。

代码证据：

- `Phase4TicketFlowService.submitRush`
- `Phase4TicketFlowService.ensureStock`
- `Phase4TicketFlowService.createPendingOrder`
- `scripts/rush_test.py`

### 支付、出票、退票和检票闭环

系统使用模拟支付接口完成订单支付，并生成电子票。用户可以申请退票，管理员审核后更新订单、电子票和退款记录。检票员核验电子票时会记录检票结果，并拦截重复核验。

代码证据：

- `Phase4TicketFlowService.pay`
- `Phase4TicketFlowService.issueTickets`
- `Phase4TicketFlowService.applyRefund`
- `Phase4TicketFlowService.approveRefund`
- `Phase4TicketFlowService.verifyTicket`

## 6. 项目强项与技术适配

### 项目强项：完整票务闭环

本项目覆盖从后台发布到前台购票、支付出票、退票和检票的完整链路。相比只做 CRUD 的课程项目，它更容易在演示中体现业务状态流转。

演示方式：按 `DEMO.md` 的 20 步流程录屏，展示同一张票从购买到检票再到重复核验拦截。

边界：支付、短信、邮箱和真实票务平台均为模拟或未接入。

### 项目强项：MySQL 与 Redis 分工明确

MySQL 负责持久化最终业务状态，Redis 负责抢票阶段高频库存判断。这个设计符合票务系统中“库存快速扣减 + 订单状态落库”的技术需求。

演示方式：后台初始化 Redis 库存，运行 `scripts/rush_test.py`，观察成功数量不超过库存。

边界：当前仍是课程演示实现，不是生产级分布式队列或秒杀架构。

### 技术适配：角色化后台与本机登录限制

管理员、票务管理员和检票员共用后台入口，但菜单和接口权限不同。后台角色限制本机登录，适合局域网课堂演示中“用户端可多人访问、后台端只由演示电脑操作”的场景。

演示方式：本机登录后台，局域网设备注册普通用户购票。

边界：真实部署需要更完整的账号安全策略、审计和密码策略。

### 技术适配：发布状态过滤与相对资源路径

后台保存图片相对路径，前台按当前后端主机加载 `/uploads/**`，避免把 `localhost` 写死到数据库中。演出发布状态控制前台可见性，适合演示管理员从草稿到发布的过程。

演示方式：后台导入图片并发布新演出，再到前台搜索验证。

边界：当前上传资源是本地文件系统存储，未接入对象存储。

## 7. 测试与验证

已验证：

- 后端构建：`cd backend && mvn -q -DskipTests package` 通过。
- 前端构建：`cd frontend && npm run build` 通过。

建议补充验证：

- MySQL、Redis、后端、前端完整启动截图。
- Swagger 或浏览器访问核心接口截图。
- 普通用户购票、支付、电子票、退票截图。
- 检票员核验成功与重复核验拦截图。
- `scripts/rush_test.py` 输出截图，证明并发抢票成功数量不超过库存。

## 8. 演示流程

5-8 分钟答辩建议节奏：

1. 30 秒：说明项目背景和三类角色。
2. 60 秒：展示系统架构和数据表分组。
3. 90 秒：管理员发布演出、场馆座位、场次票档、初始化库存。
4. 90 秒：普通用户搜索演出、选座/抢票、模拟支付、查看电子票。
5. 60 秒：检票员核验电子票，演示重复核验拦截。
6. 60 秒：退票审核、站内消息、统计报表和日志。
7. 30 秒：总结项目强项、边界和后续改进。

## 9. 高风险答辩问题

### Q1：为什么同时使用 MySQL 和 Redis？

MySQL 适合保存最终业务状态，例如订单、电子票、退票和检票记录。Redis 适合抢票阶段的高频库存扣减。本项目把库存快速判断放在 Redis，把订单和票据状态落到 MySQL，便于兼顾演示中的响应速度和数据可追溯性。

### Q2：如何防止后台草稿被普通用户看到？

后台演出有 `publish_status` 字段，前台查询只展示 `PUBLISHED` 且未逻辑删除的记录。草稿和下架记录仍可在后台维护，但不会进入前台列表和搜索结果。

### Q3：抢票超卖如何验证？

项目提供 `scripts/rush_test.py`，脚本会设置固定库存并发提交多个抢票请求，最后检查成功数量不超过初始库存且 Redis 剩余库存不为负。报告中可以展示脚本输出作为验证证据。

### Q4：为什么说支付是模拟支付？

系统没有接入支付宝、微信或银行卡接口。支付接口只在本地系统内更新支付记录、订单状态并生成电子票，适合课程演示，但不具备真实支付能力。

### Q5：默认账号和 JWT secret 是否安全？

它们只适合本地演示。真实部署时应改为环境变量或外部配置，替换默认密码和密钥，并补充密码复杂度、登录风控和审计策略。

### Q6：如果 Redis 丢失库存怎么办？

后台提供批次库存初始化入口，可以根据 MySQL 中的批次和票档数据重新写入 Redis 库存。报告中可以说明 MySQL 是业务状态基准，Redis 是抢票阶段缓存。

## 10. 限制与未来工作

- 当前支付是模拟实现，未来可接入沙箱支付网关。
- 当前前端构建包较大，未来可用路由懒加载和依赖分包优化。
- 当前高并发能力是课程级验证，未来可引入消息队列、Lua 脚本、幂等令牌和更完整的库存流水。
- 当前上传文件保存在本地目录，未来可接入对象存储并增加图片审核。
- 当前通知是站内消息，未来可接入短信、邮箱或 WebSocket 实时推送。

## 11. 报告素材索引

- README 项目概览：`README.md`
- 接口说明：`API.md`
- 数据库说明：`DATABASE.md`
- 演示流程：`DEMO.md`
- 运行验证：`docs/run-and-verification.md`
- 阶段记录：`docs/PHASE_*.md`
