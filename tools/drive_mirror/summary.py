"""Render workflow reports as a GitHub job summary (counts only; no Drive ids)."""
import json
import os
import sys


def load(d, name):
    p = os.path.join(d, name)
    try:
        return json.load(open(p))
    except Exception:
        return None


def main(d):
    out = ["## Drive mirror"]
    pre = load(d, "preflight.json")
    if pre:
        out.append(f"**Preflight:** {pre.get('result')} — " + ", ".join(
            f"{k}: {'ok' if v.get('ok') else 'FAILED'}" for k, v in pre.get("checks", {}).items()))
        if pre.get("reason"):
            out.append(f"> {pre['reason']}")
    g = load(d, "secgate.json")
    if g:
        c = g.get("counts", {})
        out.append(f"**Security gate:** {g.get('verdict')} — findings {c.get('findings')}, approved {c.get('approved')}, "
                   f"proposed (awaiting owner) {c.get('proposed_awaiting_owner')}, unreviewed {c.get('unreviewed')}, "
                   f"refs quarantined {c.get('refs_quarantined')}")
        for f in g.get("blocking_findings", [])[:20]:
            out.append(f"- :lock: `{f['rule']}` in `{f['file']}:{f['line']}` (commit `{f['commit'][:8]}`, {f['status']})")
        for r in list(g.get("refs", {}))[:50]:
            out.append(f"- :no_entry: quarantined, not uploaded: `{r}`")
    s = load(d, "sync.json")
    if s:
        if "namespaces" in s:
            ns = s["namespaces"]
            changed = [n for n in ns if n.get("action") not in ("noop",)]
            tot = lambda k: sum(n.get(k, 0) or 0 for n in ns)
            gate = s.get("gate") or {}
            if gate.get("mode") == "approved-snapshot":
                out.append(f"**Source snapshot:** frozen at {gate.get('snapshot_created_at')} (digest `{str(gate.get('snapshot_digest'))[:12]}`, "
                           f"scan binding {gate.get('scan_binding')}); only commits the security gate examined were uploaded")
            elif gate:
                out.append(f":warning: **UNGATED run** ({gate.get('mode')}): no security gate applied to this sync")
            if s.get("mode_reason"):
                out.append(f"_Run upgraded to reconcile: {s['mode_reason']}_")
            out.append(f"**Sync:** {s.get('status')} ({s.get('mode')}{', dry run' if s.get('dry_run') else ''}) — "
                       f"{len(ns)} namespaces, {len(changed)} changed, {len(s.get('retired', []))} retired, "
                       f"{len(s.get('errors', []))} errors; uploads {tot('uploads')}, copies {tot('copies')}, "
                       f"updates {tot('updates')}, moves {tot('moves')}, trashed {tot('trashed')}")
            if changed:
                out.append("<details><summary>Changed namespaces</summary>\n\n| ref | action | source | forced |\n|---|---|---|---|")
                out += [f"| `{n['ref']}` | {n.get('action')} | {n.get('source')} | {n.get('forced_update')} |" for n in changed[:200]]
                out.append("\n</details>")
            for r, why in (s.get("retire_deferred") or {}).items():
                out.append(f"- :warning: `{r}` not retired: {why}")
            for e in s.get("errors", [])[:20]:
                out.append(f"- :x: `{e.get('ref')}`: {e.get('error')}")
        else:
            out.append(f"**Sync:** {s.get('result')} — {s.get('reason')}")
            if s.get("result") == "GATE_ERROR":
                out.append("> Gate/sync mismatch: nothing was uploaded. See RUNBOOK 'Recognising a gate/sync mismatch'.")
    v = load(d, "verify.json")
    if v:
        g = v.get("global", {})
        out.append(f"**Verify:** {v.get('result')} — namespaces {g.get('found_namespaces')}/{g.get('expected_namespaces')}, "
                   f"files {g.get('files_verified')}/{g.get('files_expected')}, deep downloads {g.get('deep_downloads')}")
        if g.get("problems"):
            out.append(f"- global problems: `{json.dumps(g['problems'])}`")
        for r in v.get("failing_namespaces", [])[:30]:
            out.append(f"- :x: `{r}`: `{json.dumps(v['namespaces'][r]['problems'])}`")
    st = load(d, "status.json")
    if st:
        out.append(f"**Freshness:** {st.get('result')} — last complete run {st.get('hours_since_last_complete_run')} h ago; "
                   f"in progress {len(st.get('in_progress', []))}, behind {len(st.get('behind', []))}, "
                   f"missing {len(st.get('missing', []))}")
        for i in st.get("issues", []):
            out.append(f"- :warning: {i}")
    print("\n\n".join(out))


if __name__ == "__main__":
    main(sys.argv[1])
