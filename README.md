# 基于 Spring Boot + Vue 的综合票务抢票与电子票核验系统

系统是课程设计实习演示系统，定位为自建演出票务平台。系统只用于本机和局域网演示，不爬取真实票务平台，不接入真实支付，不抓真实库存、真实订单或真实选座接口。

## 当前能力

系统已覆盖课程演示所需的完整票务链路：

- 用户登录、实名状态、观演人管理、个人中心、订单、票夹、站内信
- 首页、分类、搜索、演出详情、电影详情、场次、票档、图形化选座
- 场馆、区域、座位模板、场次、票档、售票批次、库存池
- 抢票提交、Redis 防超卖、选座锁座、订单、模拟支付、自动出票
- 退票申请与审核、库存回流或入池、检票核验、统计、操作日志、风控日志
- ADMIN / MANAGER / CHECKER / USER 分角色权限控制

## 环境要求

- Windows
- Java 17
- Maven 3.8+
- Node.js 18+
- MySQL 8.x
- Docker Desktop
- Redis 容器：`redis-ticket`

## Redis 启动与检查

README 保留指定 Redis 启动命令：

```bash
docker run -d --name redis-ticket -p 6379:6379 redis:latest
```

README 保留指定 Redis 检查命令：

```bash
docker exec -it redis-ticket redis-cli ping
```

返回 `PONG` 即可。后端配置为：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## MySQL 初始化

默认数据库名：`ticket_market`。

```bash
mysql -uroot -p < data/schema.sql
```

默认 `backend/src/main/resources/application.yml` 使用：

```yaml
spring.datasource.url: jdbc:mysql://localhost:3306/ticket_market
spring.datasource.username: root
spring.datasource.password: root
```

如果你的本地 MySQL 密码不是 `root`，请修改 `application.yml`。当前演示数据以内置数据为主，MySQL 表结构用于课程设计说明和后续持久化扩展。

可选 Docker MySQL：

```bash
docker run -d --name mysql-ticket -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ticket_market mysql:8.0
```

## 后端启动

```bash
cd backend
mvn spring-boot:run
```

接口文档：

- Knife4j: `http://localhost:8080/doc.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

Vite 已配置：

```js
server: {
  host: '0.0.0.0',
  port: 5173
}
```

## 局域网访问方法

1. 查询 Windows 局域网 IP，例如 `192.168.1.23`。
2. 前端默认会根据当前访问地址自动推导后端地址：

```bash
http://当前访问主机:8080/api
```

3. 如需手动指定，可在 `frontend/.env.local` 配置完整 API 地址，例如：

```bash
VITE_API_BASE_URL=http://192.168.1.23:8080/api
```

4. 后端启动在 `0.0.0.0:8080` 可被局域网前端访问，CORS 已允许常见局域网网段。
5. 演示设备浏览器访问：`http://192.168.1.23:5173`。
6. 局域网其他设备仅作为普通用户访问门户、详情、购票、订单和票夹；后台账号只允许在服务器本机通过 `localhost` 或 `127.0.0.1` 登录，`/api/admin/**` 与 `/api/checker/**` 在非本机 Host 下会被拒绝。

## 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 系统管理员 | admin | admin123 |
| 票务管理员 | manager | manager123 |
| 检票员 | checker | checker123 |
| 普通用户 | user01 | user123 |
| 普通用户 | user02 | user123 |

## 数据来源说明

- 演出、电影、场馆、场次、票档均为课程演示样例数据。
- `data/sample_performances.csv` 和 `data/sample_movies.csv` 提供后续导入模板。
- 当前海报使用前端本地 SVG 占位图。
- 电影数据后续可扩展 TMDB 官方 API；没有 API Key 时继续使用本地样例。
- 不从真实票务平台下载图片、库存、订单或选座数据。

## 合规说明

系统不爬取大麦、猫眼、淘票票等真实票务平台；不访问需要登录的页面；不绕过验证码、签名、反爬、风控或隐藏接口；不接入真实微信/支付宝支付；不接入短信或邮箱；不做公网部署。
