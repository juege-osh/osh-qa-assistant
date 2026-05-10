# 无敌 RAG 项目完整实战教程

副标题：基于 `ai-assistant` 真实项目源码，从 0 到本地跑通、源码吃透、断点调试、部署上线、课程包装的全链路教程

---

## 1. 这门课到底要带你做成什么

这不是一份只讲概念的 RAG 文档，而是一份可以直接拿去做课、做训练营、做项目复盘的真项目教程。

本教程使用的真实项目目录：

- `ai-assistant`

项目根路径：

- `./ai-assistant`

如果你后续要讲 OSH 集成版，可以把下面这个项目当作进阶章节补充：

- `../osh-backend/assistant-service`

但本教程主线只围绕 `ai-assistant`，因为它结构更清晰，更适合作为教学母版。

学完这套内容，学员至少要吃透下面 8 件事：

1. 知道一个完整 RAG 项目为什么不能只靠“大模型接口 + Prompt”。
2. 能分清 MySQL、Redis、Qdrant 在项目里的职责边界。
3. 能亲手把文档上传、切块、向量化、入向量库整个流程跑通。
4. 能把聊天请求从前端一路跟到后端、检索、重排、上下文增强、流式返回。
5. 能用断点把关键变量看明白，而不是只会看接口返回。
6. 能把项目在本地跑起来，并且做基础自测。
7. 能把项目打包上线到服务器。
8. 能基于这套项目继续扩展出企业知识库助手、客服机器人、内部 SOP 助手等商业化场景。

---

## 2. 先建立正确认知：这个项目到底是不是 RAG

答案是：是，而且链路比较完整。

这个项目已经包含了一个典型 RAG 系统的关键环节：

1. 文档上传
2. 文档解析
3. 文本切块
4. 向量化
5. 向量库存储
6. 相似度检索
7. 重排序
8. 上下文增强
9. 大模型生成
10. SSE 流式返回
11. 会话记忆
12. 调用记录追踪

你可以把整个系统理解成“两条主线”：

- 入库线：把资料变成“可被检索的知识”
- 问答线：把用户问题变成“基于知识库的回答”

如果学员只记住这一句话，这门课就已经成功一半了：

> RAG 不是“问模型”，而是“先把知识整理进系统，再带着上下文去问模型”。

---

## 3. 项目全景图

### 3.1 项目模块结构

当前仓库是一个“单后端统一启动 + 双前端独立运行”的结构。

```text
ai-assistant
├── backend                         # 统一后端启动入口
├── common                          # 公共能力：上传、鉴权、Redis、异常、工具类
├── consumer                        # 用户侧业务：知识库、文档、聊天、开放 API
├── manager                         # 管理侧业务：管理员、用户、应用、知识库、调用记录
├── frontend
│   ├── ai-assistant-consumer       # 用户端前端
│   └── ai-assistant-manager        # 管理端前端
├── deploy                          # 部署示例
├── scripts                         # 冒烟测试脚本
├── start-local.sh                  # 本地一键启动
└── 文档
    ├── 1.表结构.sql
    ├── 2.初始化数据.sql
    └── upload
```

### 3.2 统一后端入口

统一后端入口类：

- `backend/src/main/java/com/osh/ai/assistant/backend/BackendApp.java`

这个类非常关键，因为它把 `manager`、`consumer`、`common` 三个模块统一装配进了一个 Spring Boot 进程。

这意味着现在不是两个 jar 分别启动，而是一个统一 jar：

- `backend/target/backend.jar`

### 3.3 前后端访问地址

本地开发默认端口：

1. 后端：`9000`
2. 管理端前端：`8001`
3. 用户端前端：`9001`

本地访问：

1. 管理端前端：`http://127.0.0.1:8001/`
2. 用户端前端：`http://127.0.0.1:9001/`
3. 健康检查：`http://127.0.0.1:9000/`
4. 管理端接口前缀：`http://127.0.0.1:9000/manager/`
5. 用户端接口前缀：`http://127.0.0.1:9000/consumer/`

### 3.4 用一句大白话理解这套系统

你可以把它理解成：

- `manager` 是后台老板端
- `consumer` 是用户工作台
- `common` 是公共零件仓
- `backend` 是总开关
- `MySQL` 存业务数据
- `Redis` 存验证码和缓存
- `Qdrant` 存向量
- `DashScope` 负责大模型、Embedding、Rerank

---

## 4. 课程建议讲法：先讲角色分工，再讲技术链路

这一段你完全可以直接拿去当第一节课开场。

### 4.1 MySQL 负责什么

MySQL 不存向量，它主要存业务结构化数据：

1. 用户
2. 管理员
3. 应用
4. 知识库
5. 上传文件记录
6. 聊天会话
7. 聊天消息
8. 调用记录

也就是说，MySQL 解决的是“谁、什么时候、做了什么、属于谁”的问题。

### 4.2 Redis 负责什么

Redis 在本项目里最直观的作用是验证码缓存。

对应代码：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/UserServiceImpl.java`
- `manager` 侧也有类似登录流程

当用户点“获取验证码”时，图片和答案不是存在数据库，而是短期存在 Redis。

### 4.3 Qdrant 负责什么

Qdrant 是向量数据库，它不关心“这个文件是谁上传的”，它关心的是：

1. 一段文本的向量是什么
2. 哪些文本和当前问题最相似

所以 Qdrant 解决的是“找相近内容”的问题。

### 4.4 大模型平台负责什么

本项目接的是阿里百炼 DashScope，主要用了三类能力：

1. Chat 模型：回答问题
2. Embedding 模型：把文本转成向量
3. Rerank 模型：给召回结果重新排序

这一点非常适合拿来给学员讲“RAG 不是只有一个模型”。

---

## 5. 本项目包含的 RAG 核心知识点

你可以把本项目里的 RAG 知识点整理成下面这张脑图式口播逻辑：

### 5.1 文档处理

1. 上传原始文件
2. 读取文件内容
3. 按 token 切块
4. 给 chunk 打元数据
5. 写入向量库

### 5.2 检索增强

1. 用户提问
2. 向量相似度召回
3. 按知识库过滤
4. 重排序
5. 把结果塞回 Prompt
6. 再交给模型回答

### 5.3 多轮对话

1. 每个会话有自己的 `chatId`
2. 后端把 `conversationId` 传给 `ChatMemory`
3. 所以前后轮会保留上下文

### 5.4 流式输出

1. 前端先建立 SSE 连接
2. 后端一边拿模型流式响应，一边向前端推送
3. 前端逐段拼接回答
4. 收到 `[DONE]` 表示本轮结束

---

## 6. 先把项目跑起来：环境准备

这一节必须讲得极其落地，因为小白最容易死在环境。

### 6.1 必装软件

1. JDK 17
2. Maven 3.9+
3. Node.js 18+
4. npm
5. MySQL 8
6. Redis 6+
7. Docker
8. Qdrant

### 6.2 为什么必须 JDK 17

项目父工程 `pom.xml` 指定了：

```xml
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>
```

所以如果你用 Java 8 去运行，就会报典型错误：

```text
UnsupportedClassVersionError
class file version 61.0
Java Runtime only recognizes class file versions up to 52.0
```

对应关系：

1. `61.0` = Java 17
2. `52.0` = Java 8

这也是很多学员最容易踩的第一个坑。

### 6.3 本地依赖关系图

```text
前端 consumer 9001  ----\
                         \
                          -> 后端 backend 9000 -> MySQL
前端 manager  8001  ----/                       -> Redis
                                                 -> Qdrant
                                                 -> DashScope
```

---

## 7. 数据库初始化

### 7.1 建库建表脚本

脚本位置：

- `文档/1.表结构.sql`

### 7.2 初始化管理员脚本

脚本位置：

- `文档/2.初始化数据.sql`

这个脚本会初始化一个管理员账号：

1. 用户名：`admin`
2. 密码：`123456`

### 7.3 建议的本地执行步骤

1. 新建数据库：`ai_assistant`
2. 执行 `1.表结构.sql`
3. 执行 `2.初始化数据.sql`
4. 确认数据库里有 `manager` 表的默认管理员

如果你用 Navicat、DataGrip、DBeaver 都可以。

---

## 8. 配置文件怎么理解

统一后端主配置文件：

- `backend/src/main/resources/application.yml`

这个配置已经改造成了“环境变量优先”的模式，适合教学和部署。

### 8.1 重点配置项

```yaml
spring:
  datasource:
    url: jdbc:mysql://${AI_ASSISTANT_DB_HOST:YOUR_DB_HOST}:${AI_ASSISTANT_DB_PORT:3306}/${AI_ASSISTANT_DB_NAME:ai_assistant}
    username: ${AI_ASSISTANT_DB_USERNAME:root}
    password: ${AI_ASSISTANT_DB_PASSWORD:YOUR_DB_PASSWORD}
  data:
    redis:
      host: ${AI_ASSISTANT_REDIS_HOST:YOUR_REDIS_HOST}
      port: ${AI_ASSISTANT_REDIS_PORT:6379}
  ai:
    vectorstore:
      qdrant:
        host: ${AI_ASSISTANT_QDRANT_HOST:YOUR_QDRANT_HOST}
        port: ${AI_ASSISTANT_QDRANT_PORT:6334}
    dashscope:
      api-key: ${AI_ASSISTANT_DASHSCOPE_API_KEY:YOUR_DASHSCOPE_API_KEY}

upload:
  static-dir: ${AI_ASSISTANT_UPLOAD_DIR:/absolute/path/to/upload}
```

### 8.2 这一段你要给学员强调什么

重点强调 4 件事：

1. MySQL、Redis、Qdrant、DashScope 缺一个都可能启动不完整。
2. `Qdrant` 当前项目默认端口用的是 `6334`。
3. 上传目录 `upload.static-dir` 一定要有写权限。
4. `DashScope API Key` 不要写死进公开课示例仓库。

---

## 9. Qdrant 怎么启动

本项目当前配置更适合直接使用 Docker 起一个 Qdrant。

### 9.1 本地启动示例

```bash
docker run -p 6333:6333 -p 6334:6334 \
  --name qdrant6334 \
  -v $(pwd)/qdrant:/qdrant/storage \
  -d qdrant/qdrant:v1.16
```

### 9.2 为什么要映射两个端口

1. `6333` 常用于 HTTP
2. `6334` 常用于 gRPC

而当前项目配置里实际连接的是：

- `6334`

所以如果学员只映射了 `6333`，项目就可能连不上。

### 9.3 启动后怎么验证

可以先验证容器是否起来：

```bash
docker ps
```

也可以试试：

```bash
curl http://127.0.0.1:6333/collections
```

---

## 10. 本地一键启动项目

项目已经提供了一键脚本：

- `start-local.sh`

### 10.1 它会做哪些事情

这个脚本很适合在课程里讲“工程化意识”。

它会自动做下面几件事：

1. 检测 JDK 17
2. 如果 `backend.jar` 不存在就自动构建
3. 检查 `9000`、`8001`、`9001` 端口是否占用
4. 启动统一后端
5. 启动管理端前端
6. 启动用户端前端

### 10.2 启动命令

```bash
cd ai-assistant
./start-local.sh
```

### 10.3 成功后访问地址

1. `http://127.0.0.1:8001/`
2. `http://127.0.0.1:9001/`
3. `http://127.0.0.1:9000/`

### 10.4 停止命令

```bash
./stop-local.sh
```

### 10.5 日志位置

```text
.local-runlogs/backend.log
.local-runlogs/manager-ui.log
.local-runlogs/consumer-ui.log
```

---

## 11. 本地启动后第一轮自测

不要一启动就点页面乱试。正确顺序是先做最小验证。

### 11.1 验证后端健康检查

接口代码：

- `common/src/main/java/com/osh/ai/assistant/common/controller/HealthController.java`

核心逻辑非常简单：

```java
@GetMapping("/")
public Result<String> health(){
    return Result.buildSuccess("UP");
}
```

验证命令：

```bash
curl http://127.0.0.1:9000/
```

如果能返回成功，说明统一后端至少已经起来了。

### 11.2 跑冒烟测试脚本

脚本位置：

- `scripts/smoke-test.sh`

执行：

```bash
./scripts/smoke-test.sh http://127.0.0.1:9000
```

这个脚本会做这些事情：

1. 健康检查
2. manager 获取验证码
3. 从 Redis 直接读验证码值
4. manager 登录
5. consumer 获取验证码
6. consumer 登录
7. 如果 consumer 用户不存在则自动注册
8. 调用受保护接口验证鉴权是否生效

### 11.3 这一步为什么很适合教学

因为它能告诉学员：

1. 不是只能点页面测试
2. 后端是可以先脱离前端做自检的
3. Redis 验证码、登录、鉴权、业务接口可以串成一套自动验证链路

---

## 12. 先讲表结构，再讲业务链路

很多人讲 RAG 只讲向量库，这是不够的。

一个能商用的 RAG 系统，至少要把下面几张表讲清楚：

### 12.1 `user`

用户表，保存普通用户账号信息，还包含：

- `app_key`

这个字段很关键，因为开放接口 `/api/chat` 会用它识别调用方。

### 12.2 `app`

应用表，表示一个问答应用实例。

关键字段：

1. `app_name`
2. `app_desc`
3. `out_lib_enable`
4. `lib_id`

其中：

- `out_lib_enable = 1` 表示宽松模式
- `out_lib_enable = 0` 表示严格模式

### 12.3 `knowledge_lib`

知识库表，一个应用通常会绑定一个知识库。

### 12.4 `upload_file`

这是教学里的重点表。

它不仅存文件名，还存：

1. `store_path`
2. `char_count`
3. `recall_count`
4. `status`
5. `doc_ids`

尤其是 `doc_ids`，它把 MySQL 业务记录和 Qdrant 里的向量文档关联起来了。

### 12.5 `chat`

会话表，表示一组多轮对话。

### 12.6 `chat_message`

聊天消息表，分别记录用户消息和 AI 回复。

### 12.7 `invoke_record` 和 `invoke_record_detail`

这是很有项目味道的设计。

很多 demo 项目根本不做这一层，但这个项目做了。

它们用于记录：

1. 一次调用是否成功
2. 请求耗时
3. 模型名
4. token 消耗
5. 用户问题
6. AI 回答
7. 失败原因

这对后续做监控、报表、计费、定位问题非常有价值。

---

## 13. 第一条主线：文档是怎么“入库”的

这是整门课第一个最关键的大章节。

### 13.1 先记住：上传不是一步，而是两步

本项目里，“把文档加到知识库”不是一个动作，而是两个动作：

1. 先把原始文件传到文件系统
2. 再把这个文件做解析、切块、向量化并写入向量库

这是一个非常适合给学员建立工程思维的点。

---

## 14. 第一步：原始文件上传到本地存储

统一后端对外上传入口：

1. `backend/src/main/java/com/osh/ai/assistant/backend/controller/ConsumerStorageController.java`
2. `backend/src/main/java/com/osh/ai/assistant/backend/controller/ManagerStorageController.java`

用户端常用接口：

- `POST /consumer/storage/uploadFile`

管理端常用接口：

- `POST /manager/storage/uploadFile`

实际落地的公共实现类：

- `common/src/main/java/com/osh/ai/assistant/common/controller/StorageController.java`
- `common/src/main/java/com/osh/ai/assistant/common/service/impl/StorageServiceImpl.java`

### 14.1 控制器入口

```java
@PostMapping("/uploadFile")
public Result<UploadResultVO> uploadFile(@Validated UploadFileReq uploadFileReq) {
    return Result.buildSuccess(storageService.uploadFile(uploadFileReq));
}
```

### 14.2 真实存储逻辑

```java
public UploadResultVO uploadFile(UploadFileReq uploadFileReq) {
    MultipartFile multipartFile = uploadFileReq.getFile();
    check(multipartFile);
    String originalFilename = multipartFile.getOriginalFilename();
    String module = uploadFileReq.getModule();
    byte[] bytes = multipartFile.getBytes();
    String relativePath = storeFile(originalFilename, module, bytes);
    UploadResultVO vo = new UploadResultVO();
    vo.setOriginalFilename(originalFilename);
    vo.setRelativePath(relativePath);
    vo.setSize(multipartFile.getSize());
    return vo;
}
```

### 14.3 这一步做了什么

1. 校验文件是否为空
2. 读取原始文件名
3. 读取文件字节数组
4. 拼接一个新的存储路径
5. 写到 `upload.static-dir` 对应目录
6. 返回相对路径 `relativePath`

### 14.4 新文件名为什么不是原名

看 `StorageServiceImpl` 的 `spliceRelativePath4File`：

```java
String patternDate = DateUtil.format(LocalDateTime.now(), CommonConstants.UPLOAD_DATE_FORMAT);
String baseName = IdUtil.fastSimpleUUID();
String suffix = "." + FilenameUtils.getExtension(originalFilename);
String newFileName = baseName + suffix;
```

目的有 3 个：

1. 避免重名覆盖
2. 按日期分目录便于管理
3. 保留后缀方便后续解析

最终路径形态类似：

```text
resources/module/20260503/uuid.pdf
```

---

## 15. 第二步：把文件真正入知识库

这一步才是 RAG 真正开始的地方。

接口入口：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/controller/UploadFileController.java`

接口：

- `POST /consumer/uploadFile/add`

请求体对象：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/bean/req/uploadfile/UploadFileAddReq.java`

关键字段：

1. `libId`
2. `storePath`
3. `originalFileName`

### 15.1 核心代码

```java
public void add(UploadFileAddReq addReq) {
    UploadFileDO entity = ConvertUtil.convert(addReq, UploadFileDO.class);
    entity.setFileName(addReq.getOriginalFileName());
    entity.setStatus(UploadFileStatusEnum.ENABLED.getCode());
    StoreResultDTO storeResultDTO = storage.store(addReq.getStorePath(), addReq.getLibId());
    entity.setCharCount(storeResultDTO.getCharCount());
    entity.setDocIds(JSONUtil.toJsonStr(storeResultDTO.getDocIds()));
    save(entity);
}
```

### 15.2 这一段一定要给学员讲透

这段代码说明：

1. 文件上传成功，不代表已经变成知识库
2. 真正的知识化动作发生在 `storage.store(...)`
3. 向量库返回的 `docIds` 会被反写进 MySQL
4. 后续删除、禁用、统计召回次数，都依赖这个关联

---

## 16. 第三步：文件解析、切块、入向量库

接口里的 `storage.store(...)` 实际实现类是：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/elt/VectorStorage.java`

### 16.1 先读文件

文件读取器：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/elt/reader/TikaDocReader.java`

核心代码：

```java
public List<Document> read(String storePath) {
    String absPath = uploadProperties.getStaticDir() + storePath;
    File file = new File(absPath);
    if (!file.isFile() || !file.exists()) {
        throw new BizEx("文件不存在");
    }
    TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(new FileSystemResource(file));
    List<Document> documents = tikaDocumentReader.read();
    if (CollUtil.isEmpty(documents)) {
        throw new BizEx("无法读取文件");
    }
    return documents;
}
```

### 16.2 再切块

`VectorStorage.store` 里的核心逻辑：

```java
List<Document> documents = docReader.read(storePath);
TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
documents = tokenTextSplitter.split(documents);
```

这说明当前项目已经内置了切块逻辑。

默认使用 `TokenTextSplitter`，代码注释写得很明确：

- 每个 chunk 默认 size 大约 800

### 16.3 再打元数据

```java
for (Document document : documents) {
    document.getMetadata().put(CommonConstants.LIB_ID_STR_KEY, String.valueOf(libId));
}
```

这一步极其关键。

为什么？

因为后面检索时不是全库乱搜，而是按 `libId` 过滤。

也就是说：

- 向量库里可以存很多知识库的 chunk
- 但搜索时只会搜当前应用绑定的知识库

### 16.4 最后写入向量库

```java
vectorStore.add(documents);
```

### 16.5 同时保存 `docIds` 和字符数

```java
docIds.add(document.getId());
charCount += text.length();
```

最后返回：

1. `charCount`
2. `docIds`

### 16.6 用一句话总结入库线

可以把这条链路口播成：

> 上传只是把文件放进仓库，入库才是把文件变成能被检索的知识。

---

## 17. 入库链路的完整时序图

```text
前端上传文件
  -> /consumer/storage/uploadFile
  -> StorageServiceImpl 把文件写入 upload 目录
  -> 返回 relativePath

前端提交“添加文档”
  -> /consumer/uploadFile/add
  -> UploadFileServiceImpl.add
  -> VectorStorage.store
  -> TikaDocReader.read
  -> TokenTextSplitter.split
  -> document.metadata.libId = 当前知识库ID
  -> vectorStore.add
  -> docIds / charCount 写入 upload_file 表
```

---

## 18. 第二条主线：聊天问答是怎么跑起来的

这一节是整个课程第二个最关键的大章节。

建议你在正式授课时反复强调：

> RAG 的价值不在“模型会说话”，而在“模型回答前先检索知识”。

---

## 19. 前端聊天页是怎么工作的

用户聊天页：

- `frontend/ai-assistant-consumer/src/views/personal/chat/Chat.vue`

用户侧聊天路由：

- `frontend/ai-assistant-consumer/src/router/index.ts`

对应路由是：

- `/personal/chat`

### 19.1 页面做了两件事

1. 先通过 SSE 建立流式连接
2. 再通过普通 POST 请求发起提问

### 19.2 前端建立 SSE 连接

```ts
evtSource = new EventSource(`${BASE_URL}/chat/connect?chatId=${activeChatId.value}`)
```

### 19.3 前端发送问题

```ts
chatApi({
  chatId: activeChatId.value,
  userInput: txt
})
```

### 19.4 前端接收流式内容

```ts
evtSource.onmessage = e => {
  if (e.data === '[DONE]') {
    streamingMsgIndex = -1
    return
  }
  if (streamingMsgIndex === -1) {
    pageData.messages.push({ typeDesc: 'assistant', message: '', html: '' })
    streamingMsgIndex = pageData.messages.length - 1
  }
  const target = pageData.messages[streamingMsgIndex]
  target.message += e.data
  target.html = render(target)
}
```

### 19.5 这段代码要教会学员什么

1. SSE 连接和提问请求是两回事
2. 后端每吐出一小段文本，前端就拼接一次
3. `[DONE]` 是服务端主动约定的结束标记

---

## 20. 后端聊天入口

后端聊天控制器：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/controller/ChatController.java`

关键接口有两个：

1. `GET /consumer/chat/connect`
2. `POST /consumer/chat/chat`

### 20.1 建立 SSE 连接

```java
@GetMapping("/connect")
public SseEmitter connect(@RequestParam("chatId") Long chatId) {
    return chatService.connect(chatId);
}
```

### 20.2 发起聊天

```java
@PostMapping(value = "/chat")
public Result<Void> chat(@RequestBody @Validated ChatReq chatReq) {
    chatService.chat(chatReq);
    return Result.buildSuccessMsg("处理完成");
}
```

---

## 21. 聊天服务层做了什么

核心类：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/ChatServiceImpl.java`

### 21.1 建立会话级 SSE 连接

```java
public SseEmitter connect(Long chatId) {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.put(chatId, emitter);
    emitter.onCompletion(() -> emitters.remove(chatId));
    emitter.onTimeout(() -> emitters.remove(chatId));
    emitter.onError(e -> emitters.remove(chatId));
    return emitter;
}
```

这意味着：

1. 每个 `chatId` 对应一个 `SseEmitter`
2. 聊天结束或异常后会清理掉

### 21.2 发起问答

```java
public void chat(ChatReq chatReq) {
    SseEmitter sseEmitter = emitters.get(chatReq.getChatId());
    if (sseEmitter == null) {
        throw new BizEx("无效会话");
    }
    AppDO app = obtainApp(chatReq.getChatId());
    ChatDTO chatDTO = ConvertUtil.convert(chatReq, ChatDTO.class);
    chatDTO.setConversationId(String.valueOf(chatReq.getChatId()));
    UserDO crtUser = userService.getById(UserContext.getUserId());
    chatDTO.setAppKey(crtUser.getAppKey());
    InvokeRecordBuilder builder = invokeManager.initInvokeRecordBuild(chatDTO, crtUser, app);
    chatMessageService.addUserMessage(chatReq);
    executorService.execute(() -> {
        String assistantMessage = aiChatService.doChat(sseEmitter, app, builder);
        chatMessageService.addAssistantMessage(chatReq.getChatId(), assistantMessage);
    });
}
```

### 21.3 这里面有 5 个关键动作

1. 根据 `chatId` 找到对应 SSE 通道
2. 找到这个会话绑定的应用 `app`
3. 把当前会话 ID 作为 `conversationId`
4. 先保存用户问题
5. 异步调用大模型，并在完成后保存 AI 完整回复

这就是一个很标准的“持久化 + 异步流式输出”设计。

---

## 22. 真正的 RAG 核心：AiChatServiceImpl

最重要的类：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/AiChatServiceImpl.java`

如果你整门课只能精讲一个类，那就是它。

### 22.1 这个类负责什么

它负责把下面这些东西串起来：

1. 检索
2. 重排
3. Prompt 组装
4. 会话记忆
5. 模型流式输出
6. 调用记录落库

---

## 23. RAG 检索逻辑到底发生在哪里

关键方法：

- `AiChatServiceImpl.obtainDocuments`

核心代码：

```java
SearchRequest searchRequest = SearchRequest.builder()
    .query(userInput)
    .topK(CommonConstants.TOP_K)
    .filterExpression(CommonConstants.LIB_ID_STR_KEY + " == '" + libId +"'")
    .build();
List<Document> documents = vectorStore.similaritySearch(searchRequest);
documents = doRerank(userInput, documents);
```

### 23.1 这段代码必须拆开讲

#### 第一步：相似度召回

```java
vectorStore.similaritySearch(searchRequest)
```

意思是：

- 用当前问题去 Qdrant 找相似文本块

#### 第二步：召回条数

`CommonConstants.TOP_K` 当前值是：

```java
public static final Integer TOP_K = 200;
```

很多人第一次看到会觉得大，其实这只是“候选召回数”，不是最终答案使用的块数。

#### 第三步：知识库过滤

```java
filterExpression("libId == '当前知识库ID'")
```

这一步保证不会把别的知识库的内容召回来。

#### 第四步：重排序

```java
documents = doRerank(userInput, documents);
```

说明这个项目不是“召回完就直接喂模型”，而是又做了一层更细的排序。

---

## 24. Rerank 是怎么做的

关键方法：

- `AiChatServiceImpl.doRerank`

核心逻辑：

```java
var rerankRequest = new RerankRequest(userInput, documents);
RerankResponse response = rerankModel.call(rerankRequest);
return response.getResults()
    .stream()
    .filter(doc -> doc != null && doc.getScore() >= ConsumerConstants.MIN_SCORE)
    .sorted(Comparator.comparingDouble(DocumentWithScore::getScore).reversed())
    .map(DocumentWithScore::getOutput)
    .collect(Collectors.toList());
```

当前最小分数阈值：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/constants/ConsumerConstants.java`

```java
public static final Double MIN_SCORE = 0.3D;
```

### 24.1 这段很适合给学员讲的几个点

1. RAG 的效果常常不是卡在模型，而是卡在召回质量。
2. `topK` 大，不代表最终质量高。
3. Rerank 的存在，就是为了把“像是相关”变成“更可能真的相关”。
4. `MIN_SCORE` 太高会漏掉答案，太低会引入噪声。

---

## 25. 检索结果是怎么塞进 Prompt 的

关键类：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/advisor/QueryAugmenterAdvisor.java`

核心逻辑：

```java
Query originalQuery = Query.builder()
    .text(chatClientRequest.prompt().getUserMessage().getText())
    .history(chatClientRequest.prompt().getInstructions())
    .build();

QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder().build();
Query augmentedQuery = queryAugmenter.augment(originalQuery, documents);

return chatClientRequest
    .mutate()
    .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery.text()))
    .build();
```

### 25.1 这一步到底在干嘛

把它翻译成人话就是：

1. 原来用户只问了一句“这个产品如何部署？”
2. 系统先去知识库里找到了几段相关文档
3. 再把这些文档拼进用户问题上下文
4. 最后把增强后的问题交给模型

这就是“检索增强生成”里“增强”的核心动作。

---

## 26. 严格模式和宽松模式

这个项目有一个非常适合教学的设计：

- 超出知识库后，是否允许模型自由回答

关键代码在 `AiChatServiceImpl.obtainResponse`。

```java
if (CollUtil.isEmpty(documents)) {
    if (YesNoEnum.Y.getCode().equals(app.getOutLibEnable())) {
        chatClientRequestSpec.advisors(advisorSpec ->
            advisorSpec.param(ChatMemory.CONVERSATION_ID, chatDTO.getConversationId()));
    } else {
        Generation generation = new Generation(new AssistantMessage(NO_ANSWER));
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        return Flux.just(chatResponse);
    }
}
```

固定拒答文案是：

```text
问题超出知识库范围，我无法回答。
```

### 26.1 宽松模式

当 `out_lib_enable = 1`：

1. 没查到知识库内容
2. 仍然允许模型继续回答

### 26.2 严格模式

当 `out_lib_enable = 0`：

1. 没查到知识库内容
2. 不让模型自由发挥
3. 直接返回拒答文案

### 26.3 这段设计特别适合做课堂案例

你可以现场演示两个 app：

1. 一个开宽松模式
2. 一个开严格模式

然后问同一个知识库外问题，学员能立刻看懂“RAG 控制边界”的价值。

---

## 27. 系统 Prompt 是怎么控制回答行为的

关键方法：

- `AiChatServiceImpl.getSystemPrompt`

系统提示词模板：

```java
private static final PromptTemplate SYSTEM_PROMPT_TEMPLATE = new PromptTemplate("""
			遵循下面的要求:
			{rule}
			## 限制
			### 所有的回答只能使用中文。
			""");
```

### 27.1 这一段适合给学员讲的重点

1. Prompt 不是瞎写的，它承载了业务边界。
2. 严格模式和宽松模式，最终会体现在 Prompt 和流程控制上。
3. “所有回答只能使用中文”也是系统规则的一部分。

---

## 28. 会话记忆是怎么实现的

### 28.1 ChatClient 默认带了两个 Advisor

配置类：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/config/BizConfig.java`

关键代码：

```java
return chatClientBuilder
    .defaultAdvisors(
        new SimpleLoggerAdvisor(),
        PromptChatMemoryAdvisor.builder(chatMemory).build()
    )
    .build();
```

### 28.2 会话 ID 从哪里来

普通用户工作台聊天：

```java
chatDTO.setConversationId(String.valueOf(chatReq.getChatId()));
```

开放 API 聊天：

```java
chatDTO.setConversationId(chatReq.getAppId() + "_" + chatReq.getChatId());
```

### 28.3 为什么开放 API 要拼 `appId_chatId`

因为对外接口的 `chatId` 是调用方自己传的字符串。

如果不和 `appId` 组合，不同应用可能会撞会话。

这就是很典型的“多租户下会话隔离”思维。

---

## 29. 流式返回是怎么从后端推给前端的

核心方法：

- `AiChatServiceImpl.processNext`
- `AiChatServiceImpl.sendData`

```java
private void processNext(SseEmitter sseEmitter, ChatResponse response, StringBuilder retSb,
                         AtomicReference<ChatResponse> lastResponseRef) {
    lastResponseRef.set(response);
    String text = response.getResult().getOutput().getText();
    if (StrUtil.isBlank(text)) {
        return;
    }
    retSb.append(text);
    sendData(sseEmitter, text);
}
```

结束时会发送：

```java
sendData(sseEmitter, ConsumerConstants.STREAM_END);
```

而 `STREAM_END` 就是：

```java
public static final String STREAM_END = "[DONE]";
```

### 29.1 这一段适合课堂讲解的话术

可以这样解释：

1. 模型不是一次性把全部答案吐出来
2. 而是一段一段返回
3. 后端每收到一段，就推给浏览器
4. 浏览器不断拼接
5. 最后收到 `[DONE]` 才真正结束

---

## 30. 聊天记录为什么能回看

用户问题和 AI 回答最终都会落表：

1. 用户消息先保存
2. AI 完整回复在流式结束后保存

历史消息接口：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/controller/ChatMessageController.java`

接口：

- `GET /consumer/chatMessage/listHistory?chatId=xxx`

所以这不是一个“临时聊天页面”，而是有持久化会话能力的。

---

## 31. 对外开放 API 是怎么做的

控制器：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/controller/ApiController.java`

接口：

- `POST /consumer/api/chat`

请求对象：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/bean/req/api/ApiChatReq.java`

字段有：

1. `appKey`
2. `userInput`
3. `appId`
4. `chatId`

### 31.1 这个设计适合怎么讲

你可以告诉学员：

- 用户工作台聊天是“登录态内部接口”
- `/api/chat` 是“给外部系统集成的开放接口”

这意味着未来可以接：

1. 企业官网客服
2. 企业微信机器人
3. 微信小程序
4. CRM 系统
5. OA 系统

---

## 32. 登录鉴权链路

不要忽略这一块，因为真正上线后，很多问题都不是 RAG 本身，而是鉴权没过。

### 32.1 鉴权拦截器

核心类：

- `common/src/main/java/com/osh/ai/assistant/common/interceptor/AuthorizationInterceptor.java`

关键逻辑：

```java
String tokenToVerify = request.getHeader(HttpHeaders.AUTHORIZATION);
if (StrUtil.isBlank(tokenToVerify)) {
    throw new BizEx(CodeEnum.AUTH_ERR);
}
DecodedJWT decodedJWT = JwtUtil.verify(tokenToVerify);
TokenDTO tokenDTO = JwtUtil.parse(decodedJWT, TokenDTO.class);
UserContext.set(tokenDTO);
```

### 32.2 这里要强调一个细节

这个项目当前是直接把 token 放进 `Authorization` 请求头，并不是标准的：

```text
Bearer xxxxxx
```

而是直接：

```text
Authorization: xxxxxx
```

很多人联调失败，就是因为默认按 Bearer 发了。

### 32.3 白名单接口

统一后端白名单在：

- `backend/src/main/resources/application.yml`

典型白名单包括：

1. `/`
2. `/manager/manager/getCode`
3. `/manager/manager/login`
4. `/consumer/user/getCode`
5. `/consumer/user/login`
6. `/consumer/user/register`
7. `/consumer/api/chat`
8. `/consumer/chat/connect`

### 32.4 验证码逻辑

核心类：

- `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/UserServiceImpl.java`

重点方法：

1. `getCode()`
2. `login()`
3. `validateCode()`

验证码会：

1. 生成图片
2. 生成 `captchaId`
3. 把答案放到 Redis
4. 登录时按 `captchaId` 去 Redis 校验

---

## 33. 这门课一定要重点教：断点调试

很多学员学了 RAG，最后还是停留在“会运行，不会排错”。  
所以你这门课如果想卖得更硬核，断点调试必须讲透。

下面我按“最推荐的断点顺序”给你列出来。

---

## 34. 断点调试总原则

建议统一使用 IntelliJ IDEA 调试后端，Chrome DevTools 调试前端。

### 34.1 后端调试建议

1. 不要先打 20 个断点
2. 一次只盯一条链路
3. 每次只看 3 类变量

这 3 类变量是：

1. 入参有没有到
2. 中间结果有没有变
3. 输出是不是你预期的

### 34.2 前端调试建议

1. 先看 Network
2. 再看 EventSource 是否建立
3. 再看消息拼接逻辑

---

## 35. 断点一：文件上传成功了吗

推荐断点位置：

1. `StorageServiceImpl.uploadFile`
2. `StorageServiceImpl.storeFile`
3. `StorageServiceImpl.doStoreFile`

对应文件：

- `common/src/main/java/com/osh/ai/assistant/common/service/impl/StorageServiceImpl.java`

### 35.1 你要让学员重点看哪些变量

1. `originalFilename`
2. `module`
3. `relativePath`
4. `localAbsPath`
5. `bytes.length`

### 35.2 正常现象

1. `relativePath` 类似 `resources/xxx/20260503/uuid.pdf`
2. `localAbsPath` 指向真实磁盘路径
3. 目标目录自动创建
4. 文件成功写入磁盘

### 35.3 常见问题

1. 上传目录没权限
2. `upload.static-dir` 配错
3. 文件为空

---

## 36. 断点二：文档真的被解析了吗

推荐断点位置：

1. `UploadFileServiceImpl.add`
2. `TikaDocReader.read`
3. `VectorStorage.store`

### 36.1 要重点看哪些变量

在 `UploadFileServiceImpl.add` 看：

1. `addReq.getStorePath()`
2. `addReq.getLibId()`

在 `TikaDocReader.read` 看：

1. `absPath`
2. `documents`

在 `VectorStorage.store` 看：

1. `documents.size()`
2. 切块前文本长度
3. 切块后 `documents.size()`
4. `docIds`
5. `charCount`

### 36.2 这一段能讲什么

这一步特别适合演示：

1. 一个 PDF 不是天然就能检索
2. 它先被读成文本
3. 再从“大文档”变成“很多小 chunk”

---

## 37. 断点三：向量库到底写进去了吗

推荐断点位置：

1. `VectorStorage.store` 的 `vectorStore.add(documents)`

### 37.1 断点时重点看

1. `documents` 里每个元素的 `id`
2. `metadata.libId`
3. `text`

### 37.2 这一步要强调什么

如果 `vectorStore.add` 之前 `documents` 就是空的，那问题不在向量库，在文档解析。  
如果 `documents` 有值但写入失败，那问题才更可能在 Qdrant 连接。

这就是断点调试的价值：

- 先分层定位，再精确排障。

---

## 38. 断点四：聊天请求有没有真正进入 RAG 主流程

推荐断点位置：

1. `ChatServiceImpl.chat`
2. `AiChatServiceImpl.doChat`
3. `AiChatServiceImpl.obtainResponse`

### 38.1 重点变量

在 `ChatServiceImpl.chat` 看：

1. `chatReq.getChatId()`
2. `chatReq.getUserInput()`
3. `app`
4. `chatDTO.getConversationId()`

在 `AiChatServiceImpl.obtainResponse` 看：

1. `documents`
2. `app.getOutLibEnable()`
3. `chatClientRequestSpec`

### 38.2 这一节适合怎么教

你可以让学员先问一个明确存在于知识库的问题，再问一个知识库外的问题。  
这样他们能看见：

1. 有召回时走增强问答
2. 没召回时走严格/宽松分支

---

## 39. 断点五：检索和重排结果到底是什么

推荐断点位置：

1. `AiChatServiceImpl.obtainDocuments`
2. `AiChatServiceImpl.doRerank`

### 39.1 重点看哪些变量

在 `obtainDocuments` 看：

1. `searchRequest`
2. `libId`
3. `documents` 重排前数量

在 `doRerank` 看：

1. 每个 `score`
2. 过滤后剩余数量
3. 最终保留下来的文本块

### 39.2 这一节非常适合讲“为什么回答不准”

如果学员问“模型回答不对，是不是模型太差”，你就让他先看这里：

1. 有没有召回到相关 chunk
2. 召回到的 chunk 排名对不对
3. 分数阈值是不是把正确内容过滤掉了

---

## 40. 断点六：上下文增强后的 Prompt 到底长什么样

推荐断点位置：

1. `QueryAugmenterAdvisor.before`

### 40.1 重点看哪些变量

1. `originalQuery.text()`
2. `documents`
3. `augmentedQuery.text()`

### 40.2 这是教学爆点

这一断点非常适合课堂演示。  
因为很多人一直听别人说“RAG 会把知识注入 Prompt”，但从来没亲眼看过。

你在这个断点里直接展开变量，学员就会瞬间理解：

- 原来增强前后 Prompt 真的不一样

---

## 41. 断点七：为什么前端能一边生成一边显示

推荐断点位置：

1. `AiChatServiceImpl.processNext`
2. 前端 `Chat.vue` 里的 `evtSource.onmessage`

### 41.1 后端看什么

1. `text`
2. `retSb`

### 41.2 前端看什么

1. `e.data`
2. `streamingMsgIndex`
3. `target.message`

### 41.3 课堂建议

你可以打开浏览器 DevTools 的 Sources 面板，在 `Chat.vue` 编译后的源码打断点。  
让学员直观看到：

1. 每次 SSE 推来一段文本
2. 页面消息就立刻变长一点

---

## 42. 断点八：登录鉴权问题到底出在哪

推荐断点位置：

1. `UserServiceImpl.getCode`
2. `UserServiceImpl.login`
3. `AuthorizationInterceptor.preHandle`

### 42.1 重点变量

在 `getCode` 看：

1. `captchaId`
2. `capText`

在 `login` 看：

1. `username`
2. `one.getPwd()`
3. `token`

在 `preHandle` 看：

1. 请求路径
2. `Authorization` 请求头
3. 解析后的 `tokenDTO`

### 42.2 适合讲的坑

1. 验证码过期
2. token 没带
3. token 带成 Bearer 格式
4. 请求路径不在白名单

---

## 43. 建议你在课上专门做的 6 个断点实验

这部分你可以直接拿去当实战作业。

### 实验 1：上传一个 PDF，观察文件路径生成规则

目标：

- 看清 `relativePath` 和磁盘路径的关系

### 实验 2：上传后立刻进入 `TikaDocReader.read`

目标：

- 看清“文件”如何变成 `Document`

### 实验 3：观察切块前后 `documents.size()` 的变化

目标：

- 理解为什么 RAG 要切块

### 实验 4：提一个知识库内问题，观察召回结果

目标：

- 看清 `similaritySearch` + `rerank`

### 实验 5：切换严格模式和宽松模式

目标：

- 理解 RAG 的边界控制

### 实验 6：在前端观察 SSE 消息拼接

目标：

- 理解流式问答体验是如何实现的

---

## 44. 接口清单总览

这部分非常适合做课件最后的索引表。

### 44.1 公共能力

1. `GET /`  
   健康检查

2. `POST /manager/storage/uploadFile`  
   管理端文件上传

3. `POST /consumer/storage/uploadFile`  
   用户端文件上传

### 44.2 manager 管理端

1. `GET /manager/manager/getCode`
2. `POST /manager/manager/login`
3. `GET /manager/manager/queryById`
4. `POST /manager/manager/modifyById`
5. `POST /manager/manager/updatePwd`
6. `POST /manager/manager/add`
7. `POST /manager/manager/queryPage`
8. `GET /manager/manager/deleteById`
9. `POST /manager/app/queryPage`
10. `POST /manager/knowledgeLib/queryPage`
11. `POST /manager/uploadFile/queryPage`
12. `POST /manager/user/queryPage`
13. `GET /manager/user/queryById`
14. `POST /manager/invokeRecord/queryPage`
15. `POST /manager/invokeRecord/queryOverview`

### 44.3 consumer 用户端

1. `GET /consumer/user/getCode`
2. `POST /consumer/user/login`
3. `POST /consumer/user/register`
4. `POST /consumer/user/modifyById`
5. `POST /consumer/user/updatePwd`
6. `GET /consumer/user/queryById`
7. `POST /consumer/app/add`
8. `POST /consumer/app/queryPage`
9. `GET /consumer/app/deleteById`
10. `GET /consumer/app/queryById`
11. `POST /consumer/app/modifyById`
12. `GET /consumer/app/checkChatCondition`
13. `GET /consumer/app/unBindLib`
14. `POST /consumer/app/bindLib`
15. `POST /consumer/knowledgeLib/add`
16. `POST /consumer/knowledgeLib/queryPage`
17. `GET /consumer/knowledgeLib/deleteById`
18. `GET /consumer/knowledgeLib/queryById`
19. `POST /consumer/knowledgeLib/modifyById`
20. `GET /consumer/knowledgeLib/listAvailableLib`
21. `POST /consumer/uploadFile/add`
22. `POST /consumer/uploadFile/queryPage`
23. `GET /consumer/uploadFile/deleteById`
24. `POST /consumer/uploadFile/updateStatus`
25. `POST /consumer/chat/add`
26. `POST /consumer/chat/rename`
27. `GET /consumer/chat/listRecent`
28. `GET /consumer/chat/connect`
29. `POST /consumer/chat/chat`
30. `GET /consumer/chat/deleteById`
31. `GET /consumer/chatMessage/listHistory`
32. `POST /consumer/api/chat`
33. `POST /consumer/invokeRecord/queryPage`
34. `POST /consumer/invokeRecord/queryOverview`

---

## 45. 前端页面结构怎么讲

### 45.1 用户端前端

路由文件：

- `frontend/ai-assistant-consumer/src/router/index.ts`

核心页面：

1. `views/Login.vue`
2. `views/Register.vue`
3. `views/personal/user/UserCenter.vue`
4. `views/personal/app/AppManage.vue`
5. `views/personal/knowledgelib/KnowledgeLibManage.vue`
6. `views/personal/uploadfile/UploadFileManage.vue`
7. `views/personal/uploadfile/AddFile.vue`
8. `views/personal/chat/Chat.vue`
9. `views/personal/invokerecord/InvokeRecordManage.vue`

### 45.2 管理端前端

路由文件：

- `frontend/ai-assistant-manager/src/router/index.ts`

核心页面：

1. `views/Login.vue`
2. `views/manager/ManagerManage.vue`
3. `views/user/UserManage.vue`
4. `views/app/AppManage.vue`
5. `views/knowledgelib/KnowledgeLibManage.vue`
6. `views/uploadfile/UploadFileManage.vue`
7. `views/invokerecord/InvokeRecordManage.vue`

### 45.3 如果你拿去讲课，页面顺序建议这样讲

1. 先讲注册登录
2. 再讲知识库
3. 再讲上传文档
4. 再讲应用绑定知识库
5. 再讲聊天调试
6. 最后讲调用记录和管理后台

这样逻辑最顺。

---

## 46. 本项目的工程化亮点

如果你是卖课，这些点要多强调，因为它们会提升项目价值感。

### 46.1 统一后端入口

不是 `manager.jar + consumer.jar` 双进程乱飞，而是统一为：

- `backend.jar`

### 46.2 双前端分离

用户端和管理端职责明确。

### 46.3 自动化冒烟测试

不是纯手工点页面，而是有：

- `scripts/smoke-test.sh`

### 46.4 调用记录落库

方便后续做：

1. 统计
2. 审计
3. 监控
4. 成本分析

### 46.5 严格模式 / 宽松模式

适合拿来讲企业知识问答边界控制。

### 46.6 SSE 流式返回

用户体验更接近真实 AI 产品。

---

## 47. 本地部署完整步骤

这一段建议你后续录成单独一节“环境搭建 + 本地跑通”。

### 47.1 第一步：准备数据库

1. 创建 MySQL 数据库 `ai_assistant`
2. 执行 `文档/1.表结构.sql`
3. 执行 `文档/2.初始化数据.sql`

### 47.2 第二步：准备 Redis

确保 Redis 能正常访问。

### 47.3 第三步：启动 Qdrant

```bash
docker run -p 6333:6333 -p 6334:6334 \
  --name qdrant6334 \
  -v $(pwd)/qdrant:/qdrant/storage \
  -d qdrant/qdrant:v1.16
```

### 47.4 第四步：配置环境变量

示例：

```bash
export AI_ASSISTANT_DB_HOST=127.0.0.1
export AI_ASSISTANT_DB_PORT=3306
export AI_ASSISTANT_DB_NAME=ai_assistant
export AI_ASSISTANT_DB_USERNAME=root
export AI_ASSISTANT_DB_PASSWORD=YOUR_DB_PASSWORD

export AI_ASSISTANT_REDIS_HOST=127.0.0.1
export AI_ASSISTANT_REDIS_PORT=6379

export AI_ASSISTANT_QDRANT_HOST=127.0.0.1
export AI_ASSISTANT_QDRANT_PORT=6334

export AI_ASSISTANT_DASHSCOPE_API_KEY=YOUR_DASHSCOPE_API_KEY
export AI_ASSISTANT_UPLOAD_DIR=$(pwd)/文档/upload
```

### 47.5 第五步：启动项目

```bash
./start-local.sh
```

### 47.6 第六步：验证

```bash
curl http://127.0.0.1:9000/
./scripts/smoke-test.sh http://127.0.0.1:9000
```

### 47.7 第七步：页面访问

1. 管理端：`http://127.0.0.1:8001/`
2. 用户端：`http://127.0.0.1:9001/`

---

## 48. 线上部署总方案

当前项目的推荐生产形态是：

1. 后端统一 jar 一个进程
2. 管理端和用户端前端分别打成静态资源
3. Nginx 做统一入口
4. MySQL、Redis、Qdrant 可以独立部署

### 48.1 后端部署文件

1. `deploy/server/docker-compose.yml`
2. `deploy/server/config/backend-prod.yml`
3. `deploy/server/nginx.ai-assistant.conf`

### 48.2 生产架构图

```text
用户浏览器
   -> Nginx
      -> /ai-manager      静态资源
      -> /ai-consumer     静态资源
      -> /manager/**      代理到 backend.jar
      -> /consumer/**     代理到 backend.jar

backend.jar
   -> MySQL
   -> Redis
   -> Qdrant
   -> DashScope
```

---

## 49. 线上部署详细步骤

### 49.1 构建统一后端

在项目根目录执行：

```bash
mvn -f pom.xml -pl backend -am package -DskipTests
```

产物：

- `backend/target/backend.jar`

### 49.2 构建前端静态资源

管理端：

```bash
cd frontend/ai-assistant-manager
npm install
npm run build
```

用户端：

```bash
cd frontend/ai-assistant-consumer
npm install
npm run build
```

### 49.3 服务器建议目录

```text
/opt/ai-assistant
├── backend.jar
├── docker-compose.yml
├── config
│   └── backend-prod.yml
├── upload
└── logs
```

### 49.4 生产配置示例

示例文件：

- `deploy/server/config/backend-prod.yml`

建议改成你课程里更安全的占位符版本：

```yaml
server:
  port: 9000

spring:
  datasource:
    url: jdbc:mysql://YOUR_DB_HOST:3306/ai_assistant?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
    username: YOUR_DB_USER
    password: YOUR_DB_PASSWORD
  data:
    redis:
      host: YOUR_REDIS_HOST
      port: 6379
  ai:
    vectorstore:
      qdrant:
        collection-name: assistant_knowledge_lib
        host: YOUR_QDRANT_HOST
        port: 6334
        initialize-schema: true

upload:
  static-dir: /opt/ai-assistant/upload
```

### 49.5 Docker Compose 启动后端

参考文件：

- `deploy/server/docker-compose.yml`

核心思路：

1. 使用 `eclipse-temurin:17-jre`
2. 挂载 `backend.jar`
3. 挂载生产配置
4. 挂载上传目录
5. 暴露宿主机 `19000 -> 容器 9000`

启动：

```bash
docker compose up -d
```

### 49.6 配置 Nginx

参考文件：

- `deploy/server/nginx.ai-assistant.conf`

重点代理规则：

1. `/ai-manager/` -> 管理端静态资源
2. `/ai-consumer/` -> 用户端静态资源
3. `/manager/` -> 后端 manager 接口
4. `/consumer/` -> 后端 consumer 接口
5. `/consumer/chat/connect` -> SSE 专门关闭缓冲

### 49.7 为什么 SSE 要单独配置

因为流式接口如果被 Nginx 缓冲，前端就不能实时收到内容。

所以这个配置非常关键：

1. `proxy_buffering off;`
2. `proxy_cache off;`
3. `proxy_read_timeout 3600s;`

---

## 50. 上线后的最小验收流程

### 50.1 服务级验收

1. 后端健康检查成功
2. manager 登录成功
3. consumer 注册登录成功
4. 文件上传成功
5. 文档入库成功
6. 能问出知识库内问题
7. 严格模式下库外问题能拒答
8. 宽松模式下库外问题能自由回答

### 50.2 数据级验收

1. `upload_file` 有记录
2. `chat` 有记录
3. `chat_message` 有记录
4. `invoke_record` 有记录
5. `invoke_record_detail` 有记录

### 50.3 向量级验收

1. Qdrant collection 已创建
2. 入库后可以检索到对应 chunk

---

## 51. 常见问题排查手册

这部分很适合做成课程最后的“救命手册”。

### 51.1 报错：`UnsupportedClassVersionError`

原因：

- 你在用低版本 Java 跑 JDK 17 编译出来的 jar

解决：

1. 安装 JDK 17
2. 设置 `JAVA_HOME`
3. 重新启动

### 51.2 页面打不开，但前端端口启动了

排查顺序：

1. 先看 `npm run dev` 是否正常
2. 再看 `BASE_URL` 是否指向正确后端
3. 再看浏览器控制台和 Network

### 51.3 登录报认证失败

排查：

1. 有没有先拿验证码
2. 验证码是否过期
3. `Authorization` 请求头是否正确
4. token 是否被拦截器成功解析

### 51.4 上传成功但问答没有效果

这类问题最常见。

排查顺序：

1. 文件是否只是上传了，但没执行 `/consumer/uploadFile/add`
2. `TikaDocReader.read` 是否读到内容
3. `vectorStore.add` 是否成功
4. `docIds` 是否写进 `upload_file`
5. `status` 是否是启用
6. 应用是否绑定了正确的知识库

### 51.5 Qdrant 连不上

排查：

1. 容器是否启动
2. 端口是否映射了 `6334`
3. `AI_ASSISTANT_QDRANT_HOST` 是否正确
4. 防火墙是否放行

### 51.6 明明有知识，但总返回“超出知识库范围”

排查：

1. 检索是否真的召回到 chunk
2. `libId` 过滤是否正确
3. Rerank 分数阈值是否过高
4. 应用是否处于严格模式

### 51.7 前端一直转圈，不显示回答

排查：

1. SSE 连接是否成功
2. Nginx 是否关闭了 SSE 缓冲
3. 后端 `processNext` 有没有收到流式内容
4. 前端 `evtSource.onmessage` 是否触发

### 51.8 文档解析失败

排查：

1. 文件是否真实存在
2. 文件格式是否被 Tika 支持
3. 文件内容是否为空或损坏

---

## 52. 你可以带学员进一步优化哪些地方

这部分很适合做进阶章节，也适合做续费课程。

### 52.1 优化切块策略

当前项目使用：

- `TokenTextSplitter`

可以进一步扩展：

1. 按段落切
2. 按标题层级切
3. 加 overlap
4. 对表格、代码、FAQ 使用不同切块策略

### 52.2 优化召回质量

可以继续优化：

1. 调整 `topK`
2. 调整 `MIN_SCORE`
3. 多路召回
4. Hybrid Search
5. query rewrite

### 52.3 优化多轮对话

当前已经用了 `ChatMemory`，还可以继续做：

1. 更细的记忆窗口
2. 长会话摘要
3. 记忆裁剪

### 52.4 优化可观测性

当前已有调用记录，后续还可以增加：

1. 检索结果日志
2. 模型耗时分段统计
3. chunk 命中可视化
4. 成本监控

### 52.5 优化文件存储

当前是本地磁盘，后续可以改造成：

1. MinIO
2. OSS
3. COS
4. S3

### 52.6 优化安全能力

后续可以做：

1. 更严格的 token 机制
2. API 调用签名
3. 速率限制
4. 敏感词和越权控制

---

## 53. 课程包装建议：如何拆成一门能卖的课

下面这份章节结构，你可以直接拿去做课程目录。

### 第 1 章：RAG 到底是什么，为什么不是“套个大模型接口”

目标：

- 建立正确认知

### 第 2 章：真实项目全景拆解

目标：

- 看懂 `backend + common + consumer + manager + frontend`

### 第 3 章：环境搭建与本地跑通

目标：

- 跑起 MySQL、Redis、Qdrant、DashScope、前后端

### 第 4 章：数据库设计与业务模型

目标：

- 看懂 `user/app/knowledge_lib/upload_file/chat/chat_message`

### 第 5 章：文件上传与本地存储

目标：

- 理解“上传”和“入库”是两回事

### 第 6 章：文档解析、切块、向量化入库

目标：

- 看懂 Tika + TokenTextSplitter + VectorStore

### 第 7 章：聊天主链路与 SSE 流式返回

目标：

- 从 `Chat.vue` 一路跟到后端

### 第 8 章：检索、Rerank、Prompt 增强

目标：

- 真正吃透 RAG 核心

### 第 9 章：严格模式与宽松模式

目标：

- 理解企业知识问答的边界控制

### 第 10 章：登录鉴权与开放 API

目标：

- 学会做商用接口形态

### 第 11 章：断点调试实战

目标：

- 会定位入库、检索、流式输出、鉴权问题

### 第 12 章：本地联调、自测与冒烟脚本

目标：

- 学会最小化回归验证

### 第 13 章：生产部署与 Nginx 代理

目标：

- 把项目真正上线

### 第 14 章：常见故障排查

目标：

- 提升项目交付能力

### 第 15 章：RAG 项目优化与商业化扩展

目标：

- 从能跑到能卖、能交付、能扩展

---

## 54. 每章都可以配的课后作业

### 作业 1

自己搭建 MySQL、Redis、Qdrant，并成功跑通项目。

### 作业 2

上传 3 个不同类型文档，观察 `upload_file` 表变化。

### 作业 3

用断点看清切块前后 `documents.size()` 的变化。

### 作业 4

同一问题分别在严格模式和宽松模式下测试。

### 作业 5

自己调用 `/consumer/api/chat` 写一个外部小 demo。

### 作业 6

把项目部署到一台自己的 Linux 服务器。

---

## 55. 一条最适合课堂演示的完整实操路径

如果你录课，我建议你按下面顺序现场演示。

1. 启动 MySQL、Redis、Qdrant
2. 执行 SQL 初始化
3. 填好环境变量
4. `./start-local.sh`
5. 打开 manager 和 consumer 页面
6. 注册一个普通用户
7. 新建知识库
8. 上传一个 PDF
9. 执行文档入库
10. 新建应用并绑定知识库
11. 新建会话
12. 提问一个知识库内问题
13. 提问一个知识库外问题
14. 切换严格/宽松模式对比结果
15. 在 IDEA 里打断点演示检索与重排
16. 看 `invoke_record` 和 `chat_message` 表
17. 最后演示 Docker + Nginx 上线

这条路径基本把课程价值全部串起来了。

---

## 56. 这套项目最值得反复讲的 10 个源码点

如果你准备做精讲版源码课，这 10 个点一定要讲透。

1. `BackendApp`  
   为什么能统一多个模块到一个进程

2. `StorageServiceImpl.uploadFile`  
   原始文件存储入口

3. `UploadFileServiceImpl.add`  
   为什么“上传”和“入库”分两步

4. `TikaDocReader.read`  
   文件如何读成文档对象

5. `VectorStorage.store`  
   切块、元数据、向量入库

6. `ChatServiceImpl.chat`  
   聊天业务编排入口

7. `AiChatServiceImpl.obtainDocuments`  
   检索和过滤核心

8. `AiChatServiceImpl.doRerank`  
   召回结果质量提升核心

9. `QueryAugmenterAdvisor.before`  
   上下文增强的真实发生点

10. `AuthorizationInterceptor.preHandle`  
    鉴权链路入口

---

## 57. 最后一段总结：把这个项目讲透，你就真的掌握 RAG 了

这套 `ai-assistant` 项目不是一个玩具 demo，它已经具备了比较完整的真实业务形态：

1. 有双端前端
2. 有统一后端
3. 有数据库
4. 有 Redis
5. 有向量库
6. 有开放 API
7. 有调用记录
8. 有严格 / 宽松控制
9. 有流式输出
10. 有基础工程化能力

如果学员能把这套项目做到下面这个程度，就不是“知道 RAG”，而是“会做 RAG 项目”：

1. 能本地跑通
2. 能独立排错
3. 能解释两条主线
4. 能打断点看明白
5. 能独立部署上线

而这，也正是这门课最应该卖的价值。

---

## 58. 附录 A：建议的课堂口播金句

这些话你可以直接拿去上课时用。

1. RAG 不是让模型变聪明，而是让模型说话前先查资料。
2. 上传文件不是入库，入库是把文件变成可检索知识。
3. MySQL 管业务，Qdrant 管相似度，Redis 管短期缓存。
4. 回答不准，先别骂模型，先看有没有召回到正确 chunk。
5. 真正会做项目的人，一定会打断点，不会只看接口结果。
6. 严格模式不是更笨，而是更安全。
7. 流式输出本质上不是魔法，就是 SSE 一段一段往前推。

---

## 59. 附录 B：课程发布前自检清单

你在卖课前，建议自己按这份清单过一遍。

1. 本地是否能完整跑通
2. SQL 脚本是否可直接执行
3. 所有配置示例是否已替换成占位符
4. Docker 启动步骤是否真实可复现
5. Qdrant 端口说明是否清晰
6. manager 默认账号是否说明清楚
7. 普通用户注册流程是否完整演示
8. 上传到入库两步是否讲清楚
9. 严格 / 宽松模式是否做过对比实验
10. 至少 6 个断点实验是否准备好
11. SSE 流式演示是否准备好
12. 线上部署是否至少自己演练过一次

---

## 60. 附录 C：本教程重点源码索引

### 后端核心

1. `backend/src/main/java/com/osh/ai/assistant/backend/BackendApp.java`
2. `backend/src/main/resources/application.yml`
3. `common/src/main/java/com/osh/ai/assistant/common/controller/HealthController.java`
4. `common/src/main/java/com/osh/ai/assistant/common/service/impl/StorageServiceImpl.java`
5. `common/src/main/java/com/osh/ai/assistant/common/interceptor/AuthorizationInterceptor.java`
6. `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/UploadFileServiceImpl.java`
7. `consumer/src/main/java/com/osh/ai/assistant/consumer/elt/reader/TikaDocReader.java`
8. `consumer/src/main/java/com/osh/ai/assistant/consumer/elt/VectorStorage.java`
9. `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/ChatServiceImpl.java`
10. `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/AiChatServiceImpl.java`
11. `consumer/src/main/java/com/osh/ai/assistant/consumer/advisor/QueryAugmenterAdvisor.java`
12. `consumer/src/main/java/com/osh/ai/assistant/consumer/config/BizConfig.java`
13. `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/UserServiceImpl.java`
14. `consumer/src/main/java/com/osh/ai/assistant/consumer/service/impl/ApiServiceImpl.java`

### 前端核心

1. `frontend/ai-assistant-consumer/src/router/index.ts`
2. `frontend/ai-assistant-consumer/src/views/personal/chat/Chat.vue`
3. `frontend/ai-assistant-manager/src/router/index.ts`

### 启动与部署

1. `start-local.sh`
2. `scripts/smoke-test.sh`
3. `deploy/server/docker-compose.yml`
4. `deploy/server/config/backend-prod.yml`
5. `deploy/server/nginx.ai-assistant.conf`
6. `部署与运维详细说明.md`

---

文档版本说明：

1. 本教程基于当前仓库真实源码整理
2. 课程中的账号、数据库密码、API Key、服务器 IP 等敏感信息，发布时请全部替换成你自己的占位符示例
3. 如果后续你把该项目进一步整合进 OSH，可把“统一后端集成版”作为进阶章节追加
