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
    s = load(d, "sync.json")
    if s:
        if "namespaces" in s:
            ns = s["namespaces"]
            changed = [n for n in ns if n.get("action") not in ("noop",)]
            tot = lambda k: sum(n.get(k, 0) or 0 for n in ns)
            out.append(f"**Sync:** {s.get('status')} ({s.get('mode')}{', dry run' if s.get('dry_run') else ''}) — "
                       f"{len(ns)} namespaces, {len(changed)} changed, {len(s.get('retired', []))} retired, "
                       f"{len(s.get('errors', []))} errors; uploads {tot('uploads')}, copies {tot('copies')}, "
                       f"updates {tot('updates')}, moves {tot('moves')}, trashed {tot('trashed')}")
            if changed:
                out.append("<details><summary>Changed namespaces</summary>\n\n| ref | action | source | forced |\n|---|---|---|---|")
                out += [f"| `{n['ref']}` | {n.get('action')} | {n.get('source')} | {n.get('forced_update')} |" for n in changed[:200]]
                out.append("\n</details>")
            for e in s.get("errors", [])[:20]:
                out.append(f"- :x: `{e.get('ref')}`: {e.get('error')}")
        else:
            out.append(f"**Sync:** {s.get('result')} — {s.get('reason')}")
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
