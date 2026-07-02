#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ZENTAO_EXECUTION_ID="${ZENTAO_EXECUTION_ID:-2}"
ZENTAO_MCP_FILE="${ZENTAO_MCP_FILE:-$ROOT_DIR/../osh-all/osh-backend/mcp.json}"
AUDIT_KEYWORDS="${AUDIT_KEYWORDS:-RAG,MVP,知识库,切分,验收,告警,公开应用}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd curl
need_cmd python3

TMP_DIR="$(mktemp -d)"
RESULT_FILE="$TMP_DIR/zentao-rag-audit.json"
trap 'rm -rf "$TMP_DIR"' EXIT

python3 - "$ZENTAO_EXECUTION_ID" "$ZENTAO_MCP_FILE" "$AUDIT_KEYWORDS" <<'PY' > "$RESULT_FILE"
import json
import os
import subprocess
import sys
from pathlib import Path

execution_id, mcp_file, raw_keywords = sys.argv[1:]
keywords = [item.strip() for item in raw_keywords.split(",") if item.strip()]

base_url = os.environ.get("ZENTAO_BASE_URL", "").strip()
token = os.environ.get("ZENTAO_TOKEN", "").strip()

if (not base_url or not token) and Path(mcp_file).exists():
    mcp = json.loads(Path(mcp_file).read_text(encoding="utf-8-sig"))
    server = (mcp.get("mcpServers") or {}).get("zentao") or {}
    headers = server.get("headers") or {}
    if not base_url:
        base_url = (server.get("apiBaseUrl") or "").strip()
    if not token:
        token = (headers.get("token") or "").strip()

if not base_url:
    base_url = "http://43.242.200.25:52001"

if not token:
    raise RuntimeError("missing ZENTAO token: set ZENTAO_TOKEN or provide a valid MCP config file")

resp = subprocess.run(
    [
        "curl",
        "-fsS",
        f"{base_url}/api.php/v1/executions/{execution_id}/tasks?page=1&limit=100",
        "-H",
        f"Token: {token}",
    ],
    capture_output=True,
    text=True,
    check=True,
)

payload = json.loads(resp.stdout)
tasks = payload.get("tasks") or []

def hit_keywords(task):
    haystack = "\n".join(
        [
            str(task.get("name") or ""),
            str(task.get("desc") or ""),
            str(task.get("keywords") or ""),
        ]
    )
    return [word for word in keywords if word in haystack]

filtered = []
for task in tasks:
    hits = hit_keywords(task)
    if not hits:
        continue
    filtered.append(
        {
            "id": task.get("id"),
            "name": task.get("name"),
            "status": task.get("status"),
            "pri": task.get("pri"),
            "deadline": task.get("deadline"),
            "assignedTo": (task.get("assignedTo") or {}).get("account"),
            "assignedToRealName": task.get("assignedToRealName"),
            "keywordHits": hits,
            "descPreview": (task.get("desc") or "").replace("\n", " ")[:220],
        }
    )

direct_goal_tasks = []
keyword_only_matches = []

direct_name_patterns = [
    "RAG MVP：共享 QA 环境最小故障告警 alertReadiness=90000 排查与留痕",
    "公开应用能力：补一轮最小验收闭环并判断是否进入下一阶段",
    "RAG-1-未命中知识与异常场景的体面反馈",
    "RAG-2-聊天回答的依据与参考来源展示",
    "RAG-3-MVP测试问题集与效果记录闭环",
]

for task in filtered:
    name = task["name"] or ""
    if any(pattern in name for pattern in direct_name_patterns):
        direct_goal_tasks.append(task)
    else:
        keyword_only_matches.append(task)

reuse_candidates = []
new_candidates = []
note_templates = []
for task in direct_goal_tasks:
    name = task["name"] or ""
    if "共享 QA 环境最小故障告警 alertReadiness=90000 排查与留痕" in name:
        reuse_candidates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "suggestedFor": [
                    "P0-1 共享 QA 告警目标环境排障",
                    "远端 .env / 日志 / docker 状态采证",
                ],
            }
        )
    elif "公开应用能力：补一轮最小验收闭环并判断是否进入下一阶段" in name:
        reuse_candidates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "suggestedFor": [
                    "P1-1 公开应用能力最小验收闭环",
                    "接口 / 页面 / 访问留痕三类证据补齐",
                ],
            }
        )
    elif "MVP测试问题集与效果记录闭环" in name:
        reuse_candidates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "suggestedFor": [
                    "P0-2 扩充真实业务问题集",
                    "P0-3 固化 token / semantic 阶段性默认结论",
                    "P0-4 沉淀 P0 / P1 任务池",
                    "P0-5 观察完整批次稳定性",
                ],
            }
        )
        note_templates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "template": "\n".join(
                    [
                        "2026-07-01 执行记录",
                        "",
                        "- 已把真实问题集正式题库扩充到 18 条，题库文件：scripts/rag-mvp-real-questions.json。",
                        "- 已完成一轮更厚正式题库下的批次执行链路改造：批次先创建、题目逐条落库、批次摘要实时显示已完成数量，中途中断时回填最近异常。",
                        "- 本地 laughing 当前已查询到两条新的 18 题正式批次：",
                        "  - 语义切分：2072237066467594242，18/18",
                        "  - Token切分：2072238664249954306，18/18",
                        "- 当前阶段性判断：",
                        "  - 更厚正式题库下“整批完全不可见”的问题在本地新包下已明显缓解。",
                        "  - 当前更前排的剩余项不再是单道题，而是整批稳定性 / 可观测性继续观察，以及候选题池 `RQ-16 / RQ-20` 的继续收口。",
                        "- 当前最需要继续追的不是大面积掉分，而是：",
                        "  - 真实问题样本继续扩题",
                        "  - `token / semantic` 阶段性默认结论继续固化",
                        "  - 跑测结果沉淀为持续任务池，并继续保留完整批次证据",
                        "",
                        "验证与证据：",
                        "- 文档：文档/开发文档/2026-07-01_RAG_MVP_P0P1任务池落地清单.md",
                        "- 草案：文档/开发文档/2026-07-01_RAG_MVP_ZenTao任务草案.md",
                        "- 审计脚本：bash scripts/zentao-rag-audit.sh",
                    ]
                ),
            }
        )
    elif "未命中知识与异常场景的体面反馈" in name:
        reuse_candidates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "suggestedFor": [
                    "RQ-15 之外的失败体面问题",
                    "未命中知识 / 链路异常的用户可见反馈优化",
                ],
            }
        )
        note_templates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "template": "\n".join(
                    [
                        "2026-07-01 执行记录",
                        "",
                        "- 当前 RQ-15 最新单题失败里，`previewSplit 文件不存在` 已基本收敛，主要噪声已转为上游 `chat/completions 503`。",
                        "- 因此当前失败体面问题已不再是简单文案缺失，更接近“上游异常时如何继续给出可信、可理解反馈”的收口。",
                        "- 建议把失败体面类优化继续和 RQ-15、链路异常降级一起观察，而不是只看未命中固定话术。",
                    ]
                ),
            }
        )
    elif "依据与参考来源展示" in name:
        reuse_candidates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "suggestedFor": [
                    "回答可信度展示",
                    "来源展示与运营排查辅助",
                ],
            }
        )
        note_templates.append(
            {
                "taskId": task["id"],
                "taskName": name,
                "template": "\n".join(
                    [
                        "2026-07-01 执行记录",
                        "",
                        "- 公开结果页与验收过程已持续围绕“回答可信、是否有依据”展开。",
                        "- 当前 `#72` 更适合作为来源展示与运营排查辅助的承接位，配合 `#73` 的批次验收结果继续推进。",
                        "- 后续若公开应用能力进入单独验收，这里的来源展示能力也可作为复用基础。",
                    ]
                ),
            }
        )

if not any("共享 QA 环境最小故障告警 alertReadiness=90000 排查与留痕" in (task["name"] or "") for task in direct_goal_tasks):
    new_candidates.append(
        {
            "title": "RAG MVP：共享 QA 环境最小故障告警 alertReadiness=90000 排查与留痕",
            "reason": "现有执行任务中未看到直接承接 QA 告警运行态异常的任务。",
        }
    )

if not any("公开应用能力：补一轮最小验收闭环并判断是否进入下一阶段" in (task["name"] or "") for task in direct_goal_tasks):
    new_candidates.append(
        {
            "title": "公开应用能力：补一轮最小验收闭环并判断是否进入下一阶段",
            "reason": "现有执行任务中未看到直接承接公开应用验收闭环的任务。",
        }
    )

print(
    json.dumps(
        {
            "executionId": int(execution_id),
            "source": {
                "baseUrl": base_url,
                "tokenSource": "env" if os.environ.get("ZENTAO_TOKEN") else ("mcp" if Path(mcp_file).exists() else "unknown"),
                "mcpFileUsed": str(Path(mcp_file)) if Path(mcp_file).exists() else None,
            },
            "keywordFilter": keywords,
            "totalTasksInExecution": len(tasks),
            "matchedTasks": filtered,
            "directGoalTasks": direct_goal_tasks,
            "keywordOnlyMatches": keyword_only_matches,
            "suggestedReuse": reuse_candidates,
            "suggestedTaskNotes": note_templates,
            "suggestedNewTasks": new_candidates,
        },
        ensure_ascii=False,
        indent=2,
    )
)
PY

cat "$RESULT_FILE"
