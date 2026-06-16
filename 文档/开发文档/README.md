# 开发文档目录

本目录存放项目开发过程中遇到的问题、Bug 修复、调试技巧等文档。

## 文档分类

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

## 更新记录

- 2026-06-17: 初始化开发文档目录，整理现有文档
