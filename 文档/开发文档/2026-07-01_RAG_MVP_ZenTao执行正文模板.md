# RAG MVP ZenTao 执行正文模板

## 1. 文档目的

这份文档只服务一件事：

- 把当前 `RAG MVP` 主 goal 的推进结果，整理成可直接复制到 ZenTao 的任务正文或执行记录

和任务草案文档的区别是：

- [2026-07-01_RAG_MVP_ZenTao任务草案.md](/Users/tianyi/devproj/osh-qa-assistant/文档/开发文档/2026-07-01_RAG_MVP_ZenTao任务草案.md) 负责说明“哪些任务要做、建议挂到哪里”
- 本文负责提供“直接可粘贴的正文”

## 2. 使用前建议

建议先跑：

```bash
bash scripts/zentao-rag-audit.sh
```

当前脚本会输出：

- 当前执行下的 `RAG MVP` 相关任务
- 建议复用的现有任务
- 建议新建的任务
- 建议回填的执行记录模板

如果要把正文安全追加到已有任务，而不是手工复制粘贴，建议再配合：

```bash
TASK_ID=73 NOTE_FILE=/tmp/zentao-note.md bash scripts/zentao-task-append-note.sh
```

这个脚本会：

- 先读取并备份原任务描述
- 再把新的执行记录追加到末尾
- 最后回读任务描述尾部，便于确认写入结果

## 3. 现有任务回填模板

### 3.1 回填 `#73 RAG-3-MVP测试问题集与效果记录闭环`

适用场景：

- 扩充真实业务问题集
- 固化 `token / semantic` 阶段性默认结论
- 把批次结果沉淀为 `P0 / P1` 任务池
- 记录更厚正式题库下的整批稳定性与可观测性进展

建议正文：

```text
2026-07-01 执行记录

- 已把真实问题集正式题库扩充到 18 条，题库文件：scripts/rag-mvp-real-questions.json
- 已完成一轮更厚正式题库下的批次执行链路改造：
  - 批次先创建
  - 题目逐条落库
  - 批次摘要实时显示 `已完成 x / 总数`
  - 中途中断时回填“已完成数 + 最近异常”
- 本地 laughing 当前已查询到两条新的 18 题正式批次：
  - 语义切分：2072237066467594242，18/18
  - Token切分：2072238664249954306，18/18
- 当前阶段性判断：
  - 更厚正式题库下“整批完全不可见”的问题在本地新包下已明显缓解
  - 当前更前排的剩余项不再是单道题，而是：
    - 整批稳定性 / 可观测性继续观察
    - `RQ-16 / RQ-20` 继续收口
    - 更厚正式题库下继续固化 `token / semantic` 阶段性结论
- 本轮还补了一条工程经验：
  - 若直接替换 `.laughing-runtime/backend.jar` 但未显式重启容器，可能触发 `publish experiment = 90000` 的假故障
  - 典型异常：`ZipException`、`NoClassDefFoundError: org/springframework/core/NestedCheckedException`
  - 容器重启后，同一最小 publish 请求已恢复 `code=200`

验证与证据：
- 文档：文档/开发文档/2026-07-01_RAG_MVP_P0P1任务池落地清单.md
- 草案：文档/开发文档/2026-07-01_RAG_MVP_ZenTao任务草案.md
- 脚本：
  - scripts/rag-mvp-real-batch-compare.sh
  - scripts/laughing-local-up.sh
```

### 3.2 回填 `#71 RAG-1-未命中知识与异常场景的体面反馈`

适用场景：

- 链路异常时的用户可见降级反馈
- `RQ-15` 之外的失败体面问题

建议正文：

```text
2026-07-01 执行记录

- 当前 RQ-15 最新单题失败里，`previewSplit 文件不存在` 已基本收敛，主要噪声已转为上游 `chat/completions 503`。
- 因此当前失败体面问题已不再是简单文案缺失，更接近“上游异常时如何继续给出可信、可理解反馈”的收口。
- 建议把失败体面类优化继续和 RQ-15、链路异常降级一起观察，而不是只看未命中固定话术。
```

### 3.3 回填 `#72 RAG-2-聊天回答的依据与参考来源展示`

适用场景：

- 回答可信度展示
- 来源展示与运营排查辅助

建议正文：

```text
2026-07-01 执行记录

- 公开结果页与验收过程已持续围绕“回答可信、是否有依据”展开。
- 当前 `#72` 更适合作为来源展示与运营排查辅助的承接位，配合 `#73` 的批次验收结果继续推进。
- 后续若公开应用能力进入单独验收，这里的来源展示能力也可作为复用基础。
```

## 4. 新建任务正文模板

### 4.1 新建 `P0-1 共享 QA 环境最小故障告警 alertReadiness=90000 排查与留痕`

建议标题：

- `RAG MVP：共享 QA 环境最小故障告警 alertReadiness=90000 排查与留痕`

建议正文：

```text
背景：
- 本地 laughing 运行态已验证 alertReadiness=READY、alertSelfCheck=SENT。
- 共享 QA 环境登录后访问 /consumer/ops/alertReadiness 仍返回 code=90000, msg=系统异常。
- 当前代码链路已核对，更像部署配置 / 运行态绑定异常，而不是接口实现本身。
- 本机 QA 部署源文件 .qaenv-laughing.env.local 当前已补齐最小故障告警所需 AI_ASSISTANT_ALERT_*。
- 使用 QAENV_RUNTIME_ENV_FILE=.qaenv-laughing.env.local bash scripts/qaenv-laughing-verify.sh 已明确看到：
  - 告警关键变量整组 set
  - alert_profile_status=configured
- 共享 SMTP 当前使用 465 + SSL，因此 laughing 部署 profile 还补上了：
  - AI_ASSISTANT_ALERT_SMTP_SSL_ENABLE
  - AI_ASSISTANT_ALERT_SMTP_SSL_TRUST
- 已补一个拿到凭据即可直跑的远端只读审计脚本：
  - bash scripts/qaenv-laughing-remote-audit.sh
- 当前远端仍可确认：
  - /opt/qa-assistant-laughing/.env 缺整组 AI_ASSISTANT_ALERT_*
  - /opt/qa-assistant-laughing/backend.jar 仍是 2026-06-25 旧包
  - backend 日志命中 No static resource consumer/ops/alertReadiness

目标：
- 明确 QA 环境 90000 根因，并补齐修复前后留痕。

建议执行动作：
1. 先把本机已就绪的 .qaenv-laughing.env.local 和最新 backend.jar 同步到共享 QA。
2. 执行 docker compose up -d --remove-orphans --force-recreate，确保 backend 容器重建。
3. 再运行 bash scripts/qaenv-laughing-remote-audit.sh，回查远端 .env、jar 时间/哈希和 backend 日志摘要。
4. 修复后重新验证 alertReadiness。
5. 如条件允许，再补一次 alertSelfCheck。

完成口径：
- 明确 90000 根因。
- 登录态 alertReadiness 返回非异常结果。
- 如条件允许，alertSelfCheck 也能走通。
- 留下修复前后结果与日志证据。
```

### 4.2 新建 `P1-1 公开应用能力：补一轮最小验收闭环并判断是否进入下一阶段`

建议标题：

- `公开应用能力：补一轮最小验收闭环并判断是否进入下一阶段`

建议正文：

```text
背景：
- 当前仓库里已有 AppPublishConfigDO、AppPublishConfigServiceImpl、PublicAppService、AppPublishSaveReq、PublicAppChatReq、PublicAppDetailVO 等代码。
- 目前已确认“公开配置保存 / 查询”这一层的实体、入参、Mapper 与服务实现存在。
- 也已确认公开访问侧的接口定义和访问 Req / VO 存在。
- 但在当前仓库检索下，仍未看到：
  - 明确的公开控制器入口
  - PublicAppServiceImpl
  - 前端公开页面入口
  - 前端公开发布配置 API
- 也还缺少像 RAG MVP 一样完整的运行态验收留痕。

目标：
- 判断当前公开应用能力是“可交付能力”还是“半成品能力”。

建议执行动作：
1. 核对接口入口是否完整存在。
2. 核对前端页面入口是否完整存在。
3. 核对是否存在真实访问留痕。
4. 输出“可交付 / 半成品 / 延后”的第一版结论。

完成口径：
- 补齐接口、页面、访问留痕三类证据。
- 形成可交付 / 半成品 / 延后的明确判断。
```

## 5. 当前建议

如果现在只想先把事情推进起来，建议顺序是：

1. 把 `#73` 回填一版执行记录
2. 新建 `P0-1` 共享 QA 告警排障任务
3. 继续把 `RQ-15` 和扩题证据挂回 `#73`
4. 最后再单独开 `P1-1` 公开应用能力验收任务
