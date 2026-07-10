# 开放 API 对接手册

## 1. 适用场景

这份文档用于说明外部系统如何接入本项目的通用问答能力。

适合以下场景：

- 企业内部系统接一个知识问答入口
- 第三方前端页面直接调用问答接口
- 自动化脚本用 `curl` 或服务端程序发起问答请求

## 2. 当前可用性判断

当前项目已经提供开放接口：

- `POST /consumer/api/chat`

这条接口的特点是：

- 返回 `SSE` 流式结果
- 不依赖用户登录 `token`
- 依赖 `appKey + appId + chatId` 做调用身份和会话归属校验

注意：

- 接口“能不能被调用”和“能不能返回有效答案”是两回事
- 前者取决于后端是否启动、`appKey/appId` 是否正确
- 后者还取决于：
  - 应用是否绑定知识库
  - 文档是否已经上传并入库
  - 模型 API Key 是否有效
  - 向量库和 embedding 能力是否可用

## 3. 接入前置条件

外部调用前，至少要准备好这些信息：

1. 一个已注册的用户
2. 该用户名下的一个应用 `appId`
3. 该用户的 `appKey`
4. 应用已绑定知识库
5. 知识库里已经上传并处理过文档

其中：

- `appKey` 在“用户中心”查看
- `appId` 在“应用列表”查看

## 4. 标准调用流程

推荐按这个顺序走：

1. 注册或登录用户
2. 创建知识库
3. 上传文档
4. 创建应用
5. 绑定知识库到应用
6. 在用户中心拿到 `appKey`
7. 外部系统调用 `POST /consumer/api/chat`
8. 读取 SSE 流结果

如果只想验证接口链路，最小流程是：

1. 已有用户
2. 已有应用
3. 已拿到 `appKey`
4. 直接发起开放 API 请求

## 5. 接口说明

### 5.1 请求地址

```text
POST /consumer/api/chat
```

### 5.2 请求头

```http
Content-Type: application/json
Accept: text/event-stream
```

### 5.3 请求体

```json
{
  "appId": 1,
  "appKey": "user-app-key",
  "chatId": "session-001",
  "userInput": "请总结知识库内容"
}
```

字段说明：

- `appId`：应用 ID，必填
- `appKey`：用户中心分配的应用调用凭证，必填
- `chatId`：调用方自行生成的唯一会话标识，必填
- `userInput`：用户输入的问题，必填

## 6. curl 示例

```bash
BASE_URL=http://127.0.0.1:9000

curl -N -X POST "$BASE_URL/consumer/api/chat" \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{
    "appId": 1,
    "appKey": "替换成真实 appKey",
    "chatId": "demo-session-001",
    "userInput": "请总结知识库内容"
  }'
```

## 7. 返回方式

返回为 `SSE` 流。

常见表现：

- 建连成功后会持续收到流式数据
- 结束时通常会看到 `[DONE]` 或流结束

如果要在服务端接入，建议：

- 后端程序使用支持流式响应的 HTTP 客户端
- 前端页面使用 `EventSource` 或兼容 `fetch stream` 的方式消费

## 8. 常见错误

### 8.1 `appKey无效`

说明：

- `appKey` 不存在

### 8.2 `appKey与应用不匹配`

说明：

- `appId` 对应的应用不属于这个 `appKey` 对应的用户

### 8.3 `应用不存在`

说明：

- `appId` 不存在

### 8.4 接口可调用但没有有效回答

重点排查：

- 模型 API Key 是否有效
- 知识库是否绑定到应用
- 文档是否已上传成功
- Qdrant / embedding / rerank 是否可用

## 9. 现有参考文档

仓库里已有相关资料，但比较分散：

- [接口联调示例命令集.md](/Users/tianyi/devproj/osh-qa-assistant/接口联调示例命令集.md)
- [接口调试速查手册.md](/Users/tianyi/devproj/osh-qa-assistant/接口调试速查手册.md)
- [frontend/ai-assistant-web/src/views/doc/Doc.vue](/Users/tianyi/devproj/osh-qa-assistant/frontend/ai-assistant-web/src/views/doc/Doc.vue)

这份文档更适合作为对外接入的单点入口。
