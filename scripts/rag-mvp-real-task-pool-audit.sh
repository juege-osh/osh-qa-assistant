#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FORMAL_FILE="${FORMAL_FILE:-$ROOT_DIR/scripts/rag-mvp-real-questions.json}"
CANDIDATE_FILE="${CANDIDATE_FILE:-$ROOT_DIR/scripts/rag-mvp-real-questions-candidate.json}"
REPORT_PATH="${REPORT_PATH:-}"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing command: $1" >&2
    exit 1
  fi
}

need_cmd python3

python3 - "$FORMAL_FILE" "$CANDIDATE_FILE" "$REPORT_PATH" <<'PY'
import json
import sys
from pathlib import Path

formal_file, candidate_file, report_path = sys.argv[1:]

formal_questions = json.loads(Path(formal_file).read_text(encoding="utf-8"))
candidate_questions = json.loads(Path(candidate_file).read_text(encoding="utf-8"))

formal_case_nos = [item.get("testCaseNo") for item in formal_questions]
candidate_case_nos = [item.get("testCaseNo") for item in candidate_questions]

formal_batches = [
    {
        "batchId": "2072237066467594242",
        "splitStrategy": "semantic",
        "itemCount": 18,
        "passCount": 18,
        "source": "18题正式批次"
    },
    {
        "batchId": "2072238664249954306",
        "splitStrategy": "token",
        "itemCount": 18,
        "passCount": 18,
        "source": "18题正式批次"
    },
    {
        "batchId": "2072272643297497089",
        "splitStrategy": "semantic",
        "itemCount": 18,
        "passCount": 17,
        "source": "18题正式批次最新完成批次"
    },
    {
        "batchId": "2072273432938139649",
        "splitStrategy": "token",
        "itemCount": 18,
        "passCount": 18,
        "source": "18题正式批次最新完成批次"
    },
]

candidate_evidence = [
    {
        "batchId": "2072271652196044802",
        "splitStrategy": "semantic",
        "scope": "RQ-16",
        "passCount": 1,
        "itemCount": 1,
    },
    {
        "batchId": "2072271818651193346",
        "splitStrategy": "token",
        "scope": "RQ-16",
        "passCount": 1,
        "itemCount": 1,
    },
    {
        "batchId": "2072270049048702978",
        "splitStrategy": "semantic",
        "scope": "RQ-20",
        "passCount": 1,
        "itemCount": 1,
    },
    {
        "batchId": "2072270190069592066",
        "splitStrategy": "token",
        "scope": "RQ-20",
        "passCount": 1,
        "itemCount": 1,
    },
]

upgraded_cases = [
    {
        "testCaseNo": "RQ-17",
        "evidence": [
            "2072227258677915650 semantic 1/1",
            "2072231220778971137 token 1/1",
        ],
        "status": "已并入正式题库"
    },
    {
        "testCaseNo": "RQ-18",
        "evidence": [
            "2072232422329626626 semantic 通过",
            "2072232858797289473 token 通过",
        ],
        "status": "已并入正式题库"
    },
]

current_conclusion = {
    "defaultCandidate": "token",
    "formalPoolJudgement": "完整18题正式题库下，历史上 semantic / token 都拿到过 18/18；但最新完成批次里已出现 `semantic 17/18 < token 18/18` 的新差异，当前阶段性默认候选继续偏向 token。",
    "candidatePoolJudgement": "RQ-16 / RQ-20 现在都已拿到 semantic 与 token 双侧单题通过证据，候选池剩余问题已不再是单题命中收口。",
    "notClosedReasons": [
        "RQ-16 / RQ-20 虽已双侧单题通过，但是否直接并入正式题库仍需看后续连续性。",
        "最新完整正式批次虽然已经能稳定创建与回查，但 `semantic 17/18` 的唯一失分点仍需决定是保留为真实差异，还是继续收口。",
    ]
}

task_pool = {
    "p0": [
        {
            "task": "围绕完整18题正式批次里 semantic 唯一失分点继续收口",
            "why": "最新完成批次已收敛为 `semantic 17/18 < token 18/18`，当前最具体的差异点是 `RQ-04` 模糊提问承接。"
        },
        {
            "task": "判断 RQ-16 / RQ-20 是否并入正式题库",
            "why": "两题都已拿到双侧单题通过证据，但还缺把候选题升级为正式样本的最终收口结论。"
        },
        {
            "task": "继续保留 token 为阶段性默认候选，但不对外宣布彻底定版",
            "why": "最新完整正式批次已再次给出 token 优势，但候选复杂题已被补平，当前不足以支持彻底定版。"
        },
    ],
    "p1": [
        {
            "task": "把 #73 的样本池与切分结论继续沉淀为稳定周节奏",
            "why": "当前闭环已形成，但还需要持续复跑和留痕，防止结论失真。"
        }
    ]
}

report_markdown = "\n".join([
    "# RAG MVP 真实问题池与任务池审计",
    "",
    "## 正式题库",
    f"- 当前正式题库文件：`{Path(formal_file).name}`",
    f"- 当前正式样本数：`{len(formal_questions)}`",
    f"- 题号：`{', '.join(formal_case_nos)}`",
    "",
    "## 候选题池",
    f"- 当前候选题库文件：`{Path(candidate_file).name}`",
    f"- 当前候选样本数：`{len(candidate_questions)}`",
    f"- 题号：`{', '.join(candidate_case_nos)}`",
    "",
    "## 关键正式批次证据",
    *[
        f"- `{item['splitStrategy']}`：`{item['batchId']}`，`{item['passCount']}/{item['itemCount']}`"
        for item in formal_batches
    ],
    "",
    "## 候选池关键证据",
    *[
        f"- `{item['scope']}` / `{item['splitStrategy']}`：`{item['batchId']}`，`{item['passCount']}/{item['itemCount']}`"
        for item in candidate_evidence
    ],
    "",
    "## 已升级进正式题库的候选题",
    *[
        f"- `{item['testCaseNo']}`：{item['status']}；证据：`{'；'.join(item['evidence'])}`"
        for item in upgraded_cases
    ],
    "",
    "## 当前阶段性结论",
    f"- 阶段性默认候选：`{current_conclusion['defaultCandidate']}`",
    f"- 正式题库结论：{current_conclusion['formalPoolJudgement']}",
    f"- 候选题结论：{current_conclusion['candidatePoolJudgement']}",
    *[f"- 未彻底收口原因：{reason}" for reason in current_conclusion["notClosedReasons"]],
    "",
    "## 当前任务池建议",
    *[f"- `P0` {item['task']}：{item['why']}" for item in task_pool["p0"]],
    *[f"- `P1` {item['task']}：{item['why']}" for item in task_pool["p1"]],
])

result = {
    "formalQuestionFile": str(Path(formal_file)),
    "formalQuestionCount": len(formal_questions),
    "formalCaseNos": formal_case_nos,
    "candidateQuestionFile": str(Path(candidate_file)),
    "candidateQuestionCount": len(candidate_questions),
    "candidateCaseNos": candidate_case_nos,
    "formalBatches": formal_batches,
    "candidateEvidence": candidate_evidence,
    "upgradedCases": upgraded_cases,
    "currentConclusion": current_conclusion,
    "taskPool": task_pool,
    "reportMarkdown": report_markdown,
}

if report_path:
    Path(report_path).write_text(report_markdown, encoding="utf-8")

print(json.dumps(result, ensure_ascii=False, indent=2))
PY
