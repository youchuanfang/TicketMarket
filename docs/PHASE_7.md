# 第七阶段说明

最终验收重点：

- 全站不显示开发者话术、默认账号密码或内部实现说明。
- 前台保持星票品牌风格，购票、订单、票夹、消息链路可操作。
- 后台保持左侧导航、顶部栏和内容区布局，按 ADMIN、MANAGER、CHECKER 显示菜单。
- Vite 使用 `--host 0.0.0.0`，`frontend/.env.local.example` 提供局域网 API 地址示例。
- README 包含 Redis、MySQL、后端、前端、默认账号、局域网和合规说明。

最终验证命令：

```bash
cd backend && mvn -q -DskipTests package
cd frontend && npm run build
docker exec redis-ticket redis-cli ping
python scripts/rush_test.py
```
