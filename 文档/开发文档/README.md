# 开发文档目录

本目录存放项目开发过程中遇到的问题、Bug 修复、调试技巧等文档。

## 文档分类

### 产品开发流程与模板

- [产品开发标准流程.md](./产品开发标准流程.md) - 规范需求调研、验收、原型、接口建模、测试上线、文档复盘的默认流程
- [需求方案与验收模板.md](./需求方案与验收模板.md) - 新功能需求、方案设计、验收与上线记录模板
- [2026-06-28_RAG_MVP测试问题集与效果记录.md](./2026-06-28_RAG_MVP测试问题集与效果记录.md) - RAG MVP 默认测试问题池、效果判断口径与记录模板
- [2026-06-28_RAG切分策略效果对比方案.md](./2026-06-28_RAG切分策略效果对比方案.md) - 定义切分实验、效果对比、记录方式与后续智能判断演进路径
- [2026-06-30_RAG_MVP剩余关键闭环与任务池.md](./2026-06-30_RAG_MVP剩余关键闭环与任务池.md) - 汇总当前主 goal 的已完成闭环、剩余任务与 `P0 / P1` 任务池建议
- [2026-07-01_RAG_MVP_ZenTao任务草案.md](./2026-07-01_RAG_MVP_ZenTao任务草案.md) - 把当前主 goal 的剩余事项整理成可直接落 ZenTao 的 `P0 / P1` 任务草案
- [2026-07-01_RAG_MVP_ZenTao执行正文模板.md](./2026-07-01_RAG_MVP_ZenTao执行正文模板.md) - 提供可直接复制到 ZenTao 的现有任务回填正文与新建任务正文模板
- [2026-07-01_RAG_MVP_ZenTao最终提交顺序清单.md](./2026-07-01_RAG_MVP_ZenTao最终提交顺序清单.md) - 把当前主 goal 落到 ZenTao 的推荐提交顺序收敛成最少歧义的操作清单
- [2026-07-01_RAG_MVP_P0P1任务池落地清单.md](./2026-07-01_RAG_MVP_P0P1任务池落地清单.md) - 把当前主 goal 收敛成可直接执行的 `P0 / P1` 任务池、回归样例与下一步动作
- `scripts/zentao-rag-audit.sh` - 只读审计当前 ZenTao 执行下与 `RAG MVP` 相关的任务，并给出“建议复用 / 建议新建”的初步映射
- `scripts/zentao-task-append-note.sh` - 安全追加 ZenTao 任务执行记录：先备份原描述，再追加新内容，最后回读尾部确认
- `scripts/qaenv-laughing-remote-audit.sh` - 在拿到 QA 凭据后只读收集远端 `.env`、docker 状态和 backend 告警相关日志，用于排查 `alertReadiness=90000`
- `scripts/rag-mvp-real-batch-compare.sh` - 当前已补“`runBatch` 请求超时后自动回查最新同名批次”的兜底，并允许 `previewSplit` 失败时只记 warning、不阻塞正式批次对比
- `scripts/rag-mvp-real-questions-candidate.json` - 真实问题候选扩题池，先用于筛选新样本，再决定是否并入正式题库基线

### Bug 修复文档
- [REDIS_TIMEOUT_DIAGNOSIS.md](./REDIS_TIMEOUT_DIAGNOSIS.md) - Redis 连接超时问题诊断
- [REDIS_CHECK_STEPS.md](./REDIS_CHECK_STEPS.md) - Redis 配置检查步骤
- [REDIS_PORT_FIX.md](./REDIS_PORT_FIX.md) - Redis 端口配置问题修复指南
- [FRONTEND_START_GUIDE.md](./FRONTEND_START_GUIDE.md) - 前端服务启动问题解决指南

### 调试与开发指南
- [断点调试路线图.md](./断点调试路线图.md) - IntelliJ IDEA 断点调试配置指南
- [接口调试速查手册.md](./接口调试速查手册.md) - 后端接口快速调试参考
- [接口联调示例命令集.md](./接口联调示例命令集.md) - 前后端联调常用命令

## 文档命名规范

新增开发文档时，请遵循以下命名规范：
- 使用清晰描述性的文件名
- 中文文档使用中文命名，英文文档使用英文命名
- Bug 修复文档建议格式：`问题描述_修复.md` 或 `PROBLEM_FIX.md`
- 示例：`Redis连接超时修复.md`、`SSE流式推送问题解决.md`

## 文档内容建议

每个 Bug 修复文档应包含：
1. **问题现象** - 具体的错误信息或异常行为
2. **问题原因** - 根本原因分析
3. **解决方案** - 详细的修复步骤
4. **验证方法** - 如何确认问题已解决
5. **相关代码** - 涉及的文件路径和代码片段（可选）

## 功能开发建议用法

后续涉及新功能、重要优化、较大重构时，建议默认按下面顺序推进：

1. 先阅读 [产品开发标准流程.md](./产品开发标准流程.md)
2. 复制 [需求方案与验收模板.md](./需求方案与验收模板.md) 作为当前需求文档
3. 先补齐需求问答、验收标准、轻量原型、接口设计、数据建模
4. 再进入开发、测试、上线和复盘

建议文件命名：

- `YYYY-MM-DD_功能名_需求与方案.md`
- `YYYY-MM-DD_功能名_测试与上线记录.md`
- `YYYY-MM-DD_功能名_复盘总结.md`

## 更新记录

- 2026-06-17: 初始化开发文档目录，整理现有文档
- 2026-06-27: 新增产品开发标准流程与需求方案模板，作为后续功能开发默认入口
- 2026-06-30: 新增 RAG MVP 剩余关键闭环与任务池文档，用于收敛当前主 goal 的真实状态
- 2026-07-01: 新增 RAG MVP ZenTao 任务草案文档，作为当前主 goal 的执行入口
- 2026-07-01: 新增 RAG MVP ZenTao 执行正文模板，提供现有任务回填与新建任务的可复制文本
- 2026-07-01: 新增 RAG MVP ZenTao 最终提交顺序清单，把当前落库顺序收敛成可直接照着执行的步骤
- 2026-07-01: 新增 RAG MVP P0/P1 任务池落地清单，把当前剩余项压成可直接执行的任务池
- 2026-07-01: 新增 `scripts/zentao-rag-audit.sh`，把 ZenTao 现有任务复用关系固化成可重复执行的只读审计脚本
- 2026-07-01: 新增 `scripts/zentao-task-append-note.sh`，把现有 ZenTao 任务的执行记录回填动作固化为安全追加脚本
- 2026-07-01: 新增 `scripts/qaenv-laughing-remote-audit.sh`，为共享 QA 告警 `90000` 排查准备远端只读审计入口
- 2026-07-01: 更新 `scripts/rag-mvp-real-batch-compare.sh`，允许切分预览失败时继续执行正式批次，避免 `previewSplit` 单点异常卡住主验收链路
- 2026-07-01: 补充运行态经验：本地替换 `.laughing-runtime/backend.jar` 后需要显式重建或重启容器，否则可能触发 `ZipException / NoClassDefFoundError` 这类热替换假故障
