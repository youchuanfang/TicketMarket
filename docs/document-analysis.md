# Document Analysis

## Executive Judgment

最终评分：`97/100`。

等级：ready to submit with minor polish。

结论：README 已经从简单运行说明扩展为课程项目入口文档；新补充的运行验证文档和报告答辩提纲能够支撑后续写报告。文档没有把未运行的完整联调、`mvn test` 或抢票压测说成已验证，功能模块和接口声明也能在源码、脚本或既有文档中找到证据。

优先保持：

1. README 的“功能模块 + 重要接口地图 + 数据模型概览 + 报告线索”结构。
2. `docs/run-and-verification.md` 对已验证命令和未验证步骤的分离。
3. `docs/course-report-and-defense.md` 对项目强项、边界和答辩问题的证据化写法。

## Audit Scope

Reviewed files:

- `README.md`
- `docs/run-and-verification.md`
- `docs/course-report-and-defense.md`
- `API.md`
- `DATABASE.md`
- `DEMO.md`
- `frontend/package.json`
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `data/schema.sql`
- `scripts/rush_test.py`
- 后端控制器、服务、鉴权配置与工具类
- 前端路由、API 封装和主要视图文件

Project evidence checked:

- 后端接口：`AuthController`、`UserController`、`PortalController`、`Phase3PortalController`、`Phase3AdminController`、`Phase4TicketFlowController`、`Phase5OperationsController`、`AdminController`、`CheckerController`
- 后端服务：`DemoDataService`、`PersistentPerformanceService`、`Phase3ResourceService`、`Phase4TicketFlowService`、`DatabaseSchemaInitializer`
- 鉴权：`AuthInterceptor`、`JwtUtil`、`AuthContext`
- 前端：`frontend/src/router/index.js`、`AdminDashboardView.vue`、用户中心、订单、票夹、支付、搜索、详情、选座与抢票页面
- 数据库：`data/schema.sql`
- 验证脚本：`scripts/rush_test.py`

Unavailable or not fully executed evidence:

- 未执行 `mvn test`。
- 未启动 MySQL、Redis、后端和前端做完整浏览器联调。
- 未运行 `scripts/rush_test.py`。
- 未采集截图，当前只提供截图清单。

以上均已在文档中标注为待补充或建议验证，没有作为已完成事实陈述。

## Scorecard

| Category | Score | Justification |
| --- | ---: | --- |
| Factual accuracy and source alignment | 20/20 | 功能、接口、技术栈、数据库、权限和构建命令均与源码和配置一致；未发现虚构功能。 |
| Completeness for submission | 15/15 | README、运行验证、报告答辩三类材料齐全，覆盖项目简介、模块、接口、目录、运行、构建、限制和报告线索。 |
| Reproducibility | 14/15 | Docker、后端、前端、构建、测试和压测命令清楚；扣 1 分是因为完整联调和压测尚未实际运行。 |
| Course report quality | 15/15 | 报告素材包含背景、需求、架构、数据模型、关键实现、项目强项、验证、演示流程和答辩问答，可直接扩写。 |
| Innovation analysis quality | 9/10 | 没有过度宣称“创新”，使用“项目强项/技术适配”更准确；扣 1 分是因为缺少与基线系统的量化比较。 |
| Evidence chain | 9/10 | 大多数模块配有代码文件证据，接口和表结构可追溯；扣 1 分是因为 README 只到文件/类级别，未列具体行号。 |
| Clarity and file structure | 10/10 | README 入口清晰，详细运行和报告内容分流到 docs，适合没看代码的人快速理解。 |
| Defense readiness | 5/5 | 有 5-8 分钟演示节奏和高风险问答，能直接辅助答辩准备。 |

## Claim-Evidence Matrix

| Document location | Claim | Project evidence | Verdict | Classification | Required fix |
| --- | --- | --- | --- | --- | --- |
| `README.md` 项目定位 | 系统覆盖发布、抢票、订单、支付、出票、退票、检票和统计 | `Phase3AdminController`、`Phase4TicketFlowController`、`Phase5OperationsController`、`AdminDashboardView.vue` | supported | evidence chain | None |
| `README.md` 技术栈 | Spring Boot 3.3、Vue 3、MySQL、Redis、JWT | `backend/pom.xml`、`frontend/package.json`、`application.yml`、`JwtUtil` | supported | evidence chain | None |
| `README.md` 后台本机登录限制 | 后台角色只允许服务端本机登录 | `AuthController.isPrivilegedRole/isLocalRequest`、`AuthInterceptor.isLocalRequest` | supported | evidence chain | None |
| `README.md` 前台只展示发布演出 | `publish_status = PUBLISHED` 且未删除 | `PortalController`、`PersistentPerformanceService`、`data/schema.sql` | supported | evidence chain | None |
| `README.md` 图片保存相对路径 | 上传/导入图片保存到 `/uploads/...` | `Phase3AdminController` upload endpoints、`WebConfig` resource handler | supported | evidence chain | None |
| `README.md` Redis 库存 key | `ticket:batch:{batchId}:level:{ticketLevelId}:stock` | `Phase4TicketFlowService.stockKey`、`scripts/rush_test.py` | supported | evidence chain | None |
| `README.md` 构建验证 | 后端 skip-tests package、前端 build 已通过 | 本轮实际执行命令输出 | supported | evidence chain | None |
| `docs/run-and-verification.md` `mvn test` | 需要 MySQL/Redis 后补跑 | `TicketMarketApplicationTests` 为 Spring Boot 上下文测试，配置依赖 MySQL/Redis | supported | evidence gap | 后续提交前可补跑并贴结果 |
| `docs/run-and-verification.md` 抢票脚本 | 并发提交后检查成功数不超过库存 | `scripts/rush_test.py` | supported but unverified live | evidence gap | 后续启动服务后运行并截图 |
| `docs/course-report-and-defense.md` 项目强项 | 完整闭环、MySQL/Redis 分工、角色化后台 | 控制器、服务、数据库、前端路由均有对应实现 | supported | evidence chain | None |
| `docs/course-report-and-defense.md` 真实部署边界 | 默认账号、JWT secret、模拟支付不可用于生产 | `application.yml`、`DemoDataService`、支付接口为本地状态更新 | supported | security risk noted and bounded | None |

## Findings

### P0

No P0 findings.

### P1

No P1 findings.

### P2

| File | Classification | Issue | Why it matters | Fix |
| --- | --- | --- | --- | --- |
| `docs/run-and-verification.md` | evidence gap | 完整浏览器联调、`mvn test` 和 `scripts/rush_test.py` 尚未实际执行 | 提交或答辩时，老师可能要求看到真实运行证据 | 启动 MySQL、Redis、后端、前端后补跑，并把结果和截图加到验证记录 |
| `README.md` / `docs/course-report-and-defense.md` | presentation enhancement | 代码证据多为类名/文件名级别，没有行号级别定位 | 已足够阅读，但正式报告中引用行号会更有说服力 | 写正式报告时补充 3-5 个关键方法的行号或代码截图 |
| `README.md` | presentation enhancement | 默认演示账号和数据库命令包含本地密码 | 已明确标注仅本地演示，风险可接受 | 若要公开发布，可改为占位符并把账号说明移到本地演示文档 |

## Improvement Plan

Keep:

- 保留 README 作为项目入口，不继续塞入完整报告正文。
- 保留运行验证和报告答辩文档分离，后续写报告更省力。
- 保留“项目强项/技术适配/边界”的措辞，避免把标准实现夸成创新。

Fix first:

- 当前没有必须立即修复的 P0/P1 项。

Improve next:

- 在 `docs/run-and-verification.md` 追加完整启动、`mvn test` 和抢票脚本的实际输出。
- 在正式报告中补充关键截图和 3-5 处代码证据。
- 如果项目要公开到公网或开源，移除默认账号密码，改为占位符和环境变量说明。

Optional rewrite targets:

- README 的接口地图后续可以根据最终接口冻结状态再精简一次。
- `docs/course-report-and-defense.md` 可以在截图采集后加入图片位置和图注。

## Revision Checklist

- `README.md`
  - [x] 项目简介、技术栈、模块、接口、目录、运行、构建、限制齐全。
  - [x] 能让没看代码的人理解功能模块和重要接口。
  - [x] 包含后续报告写作线索。

- `docs/run-and-verification.md`
  - [x] 记录已实际通过的构建命令。
  - [x] 明确未验证的 `mvn test`、完整联调和抢票压测。
  - [x] 给出截图清单和常见问题。

- `docs/course-report-and-defense.md`
  - [x] 提供可扩写的报告骨架。
  - [x] 给出项目强项和技术适配证据。
  - [x] 提供答辩流程和高风险问答。

Final audit result: `97/100`，达到用户要求的 `95+`。
