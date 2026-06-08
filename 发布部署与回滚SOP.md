# AI Assistant 发布部署与回滚 SOP

## 1. 文档定位

这份文档是标准操作手册。

适用场景：

- 你自己手动发版
- 录课时讲部署流程
- 线上发版后快速校验
- 出问题时快速回滚

## 2. 当前生产环境基线

- 服务器：`149.88.92.159`
- SSH 端口：`16328`
- 部署目录：`/opt/qa-assistant`
- 用户端：`http://149.88.92.159/ai-consumer/`
- 管理端：`http://149.88.92.159/ai-manager/`
- 后端健康检查：`http://149.88.92.159:19000/`

## 3. 发版前检查

### 3.1 代码侧

- 当前分支代码已确认
- 关键改动已提交
- 本地构建成功
- 前端打包成功
- 关键主链路已回归

### 3.2 环境侧

- 服务器可 SSH 登录
- Docker 正常
- 磁盘空间充足
- `.env` 配置完整
- 模型服务镜像可拉取

## 4. 当前生产编排文件

主编排文件：

- [`deploy/server/docker-compose.yml`](/Users/rengang/chuangye/osh-projects/osh-rag/osh-qa-assistant/deploy/server/docker-compose.yml)

生产后端配置：

- [`deploy/server/config/backend-prod-149.88.92.159.yml`](/Users/rengang/chuangye/osh-projects/osh-rag/osh-qa-assistant/deploy/server/config/backend-prod-149.88.92.159.yml)

Nginx 配置：

- [`deploy/server/nginx.149.88.92.159.conf`](/Users/rengang/chuangye/osh-projects/osh-rag/osh-qa-assistant/deploy/server/nginx.149.88.92.159.conf)

## 5. 自动发布流程

工作流文件：

- [deploy.yml](/Users/rengang/chuangye/osh-projects/osh-rag/osh-qa-assistant/.github/workflows/deploy.yml)

触发条件：

- 推送到 `main`
- 推送到 `release/**`
- 手工触发 `workflow_dispatch`

自动发布做的事：

1. 构建后端 jar
2. 构建管理端前端
3. 构建用户端前端
4. 通过 SSH 上传 jar、配置、前端 dist
5. 服务器执行 `docker compose up -d --remove-orphans`
6. 做后端健康检查

## 6. 手动发布 SOP

### 6.1 本地构建

后端：

```bash
mvn -f pom.xml -pl backend -am package -DskipTests
```

管理端前端：

```bash
cd frontend/ai-assistant-manager
npm ci --legacy-peer-deps
npm run build
```

用户端前端：

```bash
cd frontend/ai-assistant-consumer
npm ci --legacy-peer-deps
npm run build
```

### 6.2 上传产物

至少要上传：

- `backend/target/backend.jar`
- `frontend/ai-assistant-manager/dist`
- `frontend/ai-assistant-consumer/dist`
- `deploy/server/docker-compose.yml`
- `deploy/server/config/backend-prod-149.88.92.159.yml`
- `deploy/server/nginx.149.88.92.159.conf`

### 6.3 服务器执行

进入目录：

```bash
cd /opt/qa-assistant
```

重启服务：

```bash
docker compose up -d --remove-orphans
```

### 6.4 发版后验证

先看健康检查：

```bash
curl http://149.88.92.159:19000/
```

再看页面：

- `http://149.88.92.159/ai-manager/`
- `http://149.88.92.159/ai-consumer/`

再跑一遍核心链路：

- 管理端登录
- 用户端登录
- 创建或查看知识库
- 对话发消息

## 7. 容器级检查

查看容器：

```bash
docker ps
```

建议重点看：

- `qa-assistant-backend`
- `qa-assistant-nginx`
- `qa-assistant-embedding-api`
- `qa-assistant-rerank-api`
- `qa-assistant-qdrant`
- `qa-assistant-redis`
- `qa-assistant-mysql8`

## 8. 出问题时先看什么

### 8.1 页面打不开

先看：

- `qa-assistant-nginx`
- Nginx 配置
- 80 端口占用

### 8.2 后端健康检查不通

先看：

- `qa-assistant-backend`
- 后端日志
- MySQL / Redis / Qdrant / Embedding / Rerank 健康状态

### 8.3 聊天不可用

先看：

- SSE 接口是否能建连
- 后端日志里是否有鉴权异常
- Embedding 是否健康
- Rerank 是否超时或未启动

## 9. 标准回滚策略

### 9.1 什么情况下需要回滚

- 后端起不来
- 页面白屏
- 用户登录失败
- 核心对话链路异常
- 文件上传或知识库链路异常

### 9.2 最稳的回滚思路

回滚顺序建议：

1. 回滚 `backend.jar`
2. 回滚前端 `dist`
3. 如有必要再回滚配置文件

原因：

- 大多数问题不是数据库结构问题，就是代码或前端资源问题
- 优先回滚代码层最快

### 9.3 录课时建议这样讲回滚

真正的生产回滚，不是先去“猜哪有问题”，而是先把系统恢复到上一个稳定版本，再回头分析根因。

## 10. 发布后的标准验收单

- 后端 `UP`
- 管理端可登录
- 用户端可登录
- 知识库列表可查询
- 文件列表可查询
- 聊天可发消息
- SSE 可收到 `connected`
- 调用记录可查询
- OpenAPI 可正常返回

## 11. 最适合录课展示的部署讲法

如果你在录课里讲部署，建议按这个顺序：

1. 先讲架构图
2. 再讲 docker-compose
3. 再讲配置文件
4. 再讲 GitHub Actions
5. 再讲发版后验证
6. 最后讲回滚思路

这样不会把部署讲成一堆零散命令。
