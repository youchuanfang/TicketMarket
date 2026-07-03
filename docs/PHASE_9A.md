# Phase 9A：本地原创素材

本阶段新增 `scripts/generate_local_assets.py`，全部素材由本地 SVG 程序生成，不依赖外网。

生成结果：

- 首页轮播图：6 张，`uploads/banners/`
- 演出海报：12 张，`uploads/posters/performance/`
- 电影海报：6 张，`uploads/posters/movie/`
- 详情横幅：8 张，`uploads/detail/`

同步路径：

- 后端静态资源：`/uploads/**`
- 前端开发资源：`frontend/public/uploads/`
- 兼容旧路径：`frontend/public/posters/`

验证：

- 生成脚本执行成功
- `mvn -q -DskipTests package`
- `npm run build`

