# 基于 Spring Boot + Vue 的综合票务抢票与电子票核验系统

本项目是课程设计实习演示系统，定位为自建演出票务平台。系统只用于本机和局域网演示，不爬取真实票务平台，不接入真实支付，不抓真实库存、真实订单或真实选座接口。

## 当前阶段

已完成第一阶段和第二阶段：

- 后端 Spring Boot 3 基础工程
- Redis、MySQL、MyBatis Plus、JWT、Knife4j、统一返回、全局异常、CORS 配置
- 用户注册、登录、JWT 鉴权、用户信息、实名状态、观演人管理接口
- 首页、分类、搜索、演出详情、电影详情基础接口
- 前端 Vue3 + Vite + Element Plus 门户页面
- 首页轮播、分类、热门推荐、即将开售、正在售票、回流票提示、电影入口
- 分类页、搜索页、演出详情页、电影详情页、登录注册页、个人中心、后台仪表盘入口
- MySQL 表结构初版、API/DATABASE/DEMO 文档初版

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

如果你的本地 MySQL 密码不是 `root`，请修改 `application.yml`。当前第二阶段接口使用后端内置演示数据，MySQL 未初始化时不影响门户接口开发；后续阶段会逐步切换到数据库表。

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
2. 复制 `frontend/.env.local.example` 为 `frontend/.env.local`。
3. 修改：

```bash
VITE_API_BASE_URL=http://192.168.1.23:8080
```

4. 后端启动在 `0.0.0.0:8080` 可被局域网前端访问，CORS 已允许常见局域网网段。
5. 演示设备浏览器访问：`http://192.168.1.23:5173`。

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

本项目不爬取大麦、猫眼、淘票票等真实票务平台；不访问需要登录的页面；不绕过验证码、签名、反爬、风控或隐藏接口；不接入真实微信/支付宝支付；不接入短信或邮箱；不做公网部署。

