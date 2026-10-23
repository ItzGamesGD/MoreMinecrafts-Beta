"""One manually triggered, read-only delegation to an OpenRouter free model.

live mode: one catalog read, then one chat request, and only if OPENROUTER_API_KEY is set.
mock mode: the same pipeline against canned responses, with no network and no key.

Writes result.json and summary.md to --out, appends the summary to
GITHUB_STEP_SUMMARY when set, and prints one FREE_MODEL_RESULT line for log retrieval.
"""

import argparse
import datetime
import json
import os
import sys

from openrouter_client import DelegationError, MODELS_URL, OpenRouterClient, parse_chat_response, select_model, urllib_transport

REQUEST_ID = "readonly-snippet-review-v1"
SYSTEM_PROMPT = "You are a read-only code reviewer. Analyze the snippet only. Do not write or suggest file edits."
SYNTHETIC_SNIPPET = """def average(values):
    total = 0
    for value in values:
        total += value
    return total / len(values)
"""
USER_PROMPT = (
    "Snippet (synthetic, not from any repository):\n```python\n"
    + SYNTHETIC_SNIPPET
    + "```\nReply in at most 120 words with: (1) a one-sentence summary, (2) up to three risks, "
    "(3) one question for the author."
)
OK_STATUSES = ("ok", "mock_ok", "blocked_no_key")
MOCK_MODEL = "mock/free-model:free"
MOCK_CATALOG = [{"id": MOCK_MODEL, "pricing": {"prompt": "0", "completion": "0"}}]
MOCK_COMPLETION = {
    "model": MOCK_MODEL,
    "choices": [
        {
            "message": {"role": "assistant", "content": "Summary: computes a mean. Risks: empty input; int overflow in other languages. Question: is empty input allowed?"},
            "finish_reason": "stop",
        }
    ],
    "usage": {"prompt_tokens": 90, "completion_tokens": 30, "total_tokens": 120},
}


def mock_transport(method, url, headers, body, timeout):
    if url == MODELS_URL:
        return {"data": MOCK_CATALOG}
    return MOCK_COMPLETION


def _new_record(mode):
    return {
        "request_id": REQUEST_ID,
        "mode": mode,
        "started_at": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "status": None,
        "model_requested": None,
        "model_served": None,
        "finish_reason": None,
        "latency_ms": None,
        "usage": None,
        "cost_reported": None,
        "response": None,
        "error": None,
    }


def delegate(api_key, transport, mode):
    """Runs catalog read, model selection and exactly one chat request. Never retries."""
    record = _new_record(mode)
    client = OpenRouterClient(api_key, transport)
    try:
        model = select_model(client.list_models())
        record["model_requested"] = model
        messages = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": USER_PROMPT},
        ]
        payload, latency_ms = client.chat_once(model, messages)
        record["latency_ms"] = latency_ms
        parsed = parse_chat_response(payload, model)
        record.update(
            model_served=parsed["model_served"],
            finish_reason=parsed["finish_reason"],
            usage=parsed["usage"],
            cost_reported=parsed["cost_reported"],
            response=parsed["content"][:1200],
            status="mock_ok" if mode == "mock" else "ok",
        )
    except DelegationError as error:
        record["status"] = f"error_{error.kind}"
        record["error"] = str(error)
    return record


def run(mode, out_dir, env, transport=None):
    if mode == "mock":
        record = delegate("mock-key", mock_transport, "mock")
    else:
        api_key = env.get("OPENROUTER_API_KEY", "").strip()
        if api_key:
            record = delegate(api_key, transport or urllib_transport, "live")
        else:
            record = _new_record("live")
            record["status"] = "blocked_no_key"
            record["error"] = "OPENROUTER_API_KEY secret is not set; no request was sent"
    write_outputs(record, out_dir, env)
    return record


def write_outputs(record, out_dir, env):
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, "result.json"), "w", encoding="utf-8") as handle:
        json.dump(record, handle, indent=2, sort_keys=True)
        handle.write("\n")

    summary = render_summary(record)
    with open(os.path.join(out_dir, "summary.md"), "w", encoding="utf-8") as handle:
        handle.write(summary)
    step_summary = env.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as handle:
            handle.write(summary)


def render_summary(record):
    usage = record["usage"] or {}
    rows = [
        ("request", record["request_id"]),
        ("mode", record["mode"]),
        ("model requested", record["model_requested"]),
        ("model served", record["model_served"]),
        ("latency (ms)", record["latency_ms"]),
        ("tokens prompt / completion / total", " / ".join(str(usage.get(key, "n/a")) for key in ("prompt_tokens", "completion_tokens", "total_tokens")) if record["usage"] else None),
        ("cost reported", record["cost_reported"]),
        ("error", record["error"]),
    ]
    lines = [f"# Free-model delegation: {record['status']}", "", "| Field | Value |", "| --- | --- |"]
    lines += [f"| {name} | {value if value is not None else 'n/a'} |" for name, value in rows]
    if record["response"]:
        lines += ["", "## Response", "", record["response"], ""]
    return "\n".join(lines) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description="Delegate one read-only analysis to an OpenRouter free model.")
    parser.add_argument("--mode", choices=("live", "mock"), default="mock")
    parser.add_argument("--out", required=True, help="directory for result.json and summary.md")
    args = parser.parse_args(argv)

    record = run(args.mode, args.out, os.environ)
    compact = {key: record[key] for key in ("status", "mode", "model_requested", "model_served", "latency_ms", "usage", "cost_reported", "error")}
    compact["response_excerpt"] = (record["response"] or "")[:300]
    print("FREE_MODEL_RESULT " + json.dumps(compact, sort_keys=True))
    return 0 if record["status"] in OK_STATUSES else 1


if __name__ == "__main__":
    sys.exit(main())
