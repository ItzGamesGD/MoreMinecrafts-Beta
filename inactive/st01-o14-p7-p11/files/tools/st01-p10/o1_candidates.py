#!/usr/bin/env python3
"""ST-01 P10 O-1 support: for a set of candidate fallback times, what fraction of measured runs would have been opened by the golem's NORMAL arrival
(fallback not needed) and how long would players wait in the worst measured case. Inputs are the CSVs written by the P10 game tests.
Usage: python3 -I o1_candidates.py <data_dir> <out_md>"""
import sys, os, csv, statistics as st, math

CANDS = [300, 450, 600, 900, 1200, 1800]

def q(xs, p):
    xs = sorted(xs); return xs[max(0, math.ceil(p / 100 * len(xs)) - 1)] if xs else None

def rows(p): return list(csv.DictReader(open(p))) if os.path.exists(p) else []

def main(data, out):
    base = rows(os.path.join(data, "route_trials_base.csv")); flows = [r for r in rows(os.path.join(data, "flow_runs.csv")) if r["scenario"] == "sampled"]
    lines = ["| 후보 대체 개방 시간 | 초 | 순수 이동(9개 출발점 × 동/서 × 30회)에서 정상 도착이 먼저인 비율 | 가장 먼 출발점(NW 모서리·입구 복도)도 포함? | 따라가는 참가자 표본(시작 거리·루팅 지연·걸음 속도, 64회)에서 정상 도착이 먼저인 비율 |", "|---|---|---|---|---|"]
    arr = [int(r["t_r15"]) for r in base if r["outcome"] == "ARRIVED"]
    far = [int(r["t_r15"]) for r in base if r["outcome"] == "ARRIVED" and r["start"] in ("NW_CORNER", "CORRIDOR_OUT")]
    fl = [int(r["open_tick"]) for r in flows if r["final_state"] == "ENTRANCE_OPEN"]
    nfl = len(flows)
    for c in CANDS:
        a = sum(t < c for t in arr) / len(arr) * 100; f = sum(t < c for t in far) / len(far) * 100
        fo = sum(t < c for t in fl) / nfl * 100 if nfl else float("nan")
        lines.append(f"| {c} 틱 | {c/20:.0f} s | {a:.1f}% ({sum(t<c for t in arr)}/{len(arr)}) | {'예' if f == 100 else f'아니오 ({f:.0f}%)'} | {fo:.1f}% ({sum(t<c for t in fl)}/{nfl}) |")
    summary = f"\n순수 이동 도착 틱 분포(n={len(arr)}): min {min(arr)}, median {st.median(arr)}, p90 {q(arr,90)}, p95 {q(arr,95)}, p99 {q(arr,99)}, max {max(arr)}. 참가자 표본(n={len(fl)}) 개방 틱: min {min(fl)}, median {st.median(fl)}, p90 {q(fl,90)}, p95 {q(fl,95)}, max {max(fl)}.\n"
    open(out, "w").write("\n".join(lines) + "\n" + summary)
    print("\n".join(lines) + summary)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
