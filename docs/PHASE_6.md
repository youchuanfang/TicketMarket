# 第六阶段说明

已补齐演示数据说明、CSV 样例、并发压测脚本和项目文档。

数据文件：

- `data/sample_performances.csv`
- `data/sample_movies.csv`
- `data/schema.sql`

压测脚本：

```bash
python scripts/rush_test.py
```

脚本会把 `ticket:batch:3001:level:2001:stock` 设置为 5，并发提交 20 个抢票请求，验证成功数不超过库存且 Redis 库存不为负。
