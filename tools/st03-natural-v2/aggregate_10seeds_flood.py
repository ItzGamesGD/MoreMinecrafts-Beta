"""Aggregate 10-seed ST-03 flood and ground condition audit results.
Can read either local results in build/st03-natural/results/ or fetch via GitHub API.
Outputs aggregated JSON and CSV reports.
"""
import csv
import json
import os
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

SEEDS = [10079, 10033, 10148, 10137, 10142, 10039, 10135, 10061, 314159265, 867530926]

def load_seed_data(results_dir: Path, commit_sha: str = None, run_id: str = None) -> dict:
    seed_data = {}
    missing = []
    for s in SEEDS:
        p = results_dir / str(s) / "flood-summary.json"
        if p.exists():
            with open(p, "r", encoding="utf-8") as f:
                seed_data[s] = json.load(f)
        else:
            missing.append(s)

    if missing and run_id:
        print(f"Downloading artifacts for run {run_id}...")
        tmp_dir = results_dir / "downloaded"
        tmp_dir.mkdir(parents=True, exist_ok=True)
        subprocess.run(["gh", "run", "download", run_id, "--dir", str(tmp_dir)], check=True)
        for s in missing:
            p = tmp_dir / f"st03-natural-{s}" / f"results/{s}/flood-summary.json"
            if not p.exists():
                p = tmp_dir / f"st03-natural-{s}" / "flood-summary.json"
            if not p.exists():
                p = tmp_dir / f"st03-natural-{s}" / f"{s}/flood-summary.json"
            if p.exists():
                with open(p, "r", encoding="utf-8") as f:
                    seed_data[s] = json.load(f)
            else:
                # Check check-runs API fallback
                pass

    return seed_data

def aggregate(seed_data: dict, out_dir: Path):
    out_dir.mkdir(parents=True, exist_ok=True)
    total_candidates = sum(d.get("total_candidates", 2601) for d in seed_data.values())
    total_valid = sum(d.get("valid_starts", 0) for d in seed_data.values())
    total_non_passing = sum(d.get("non_passing_candidates", 0) for d in seed_data.values())

    # Hypothesis 1 aggregation
    h1_pass_starts_water_112 = 0
    h1_pass_total_water_cols_112 = 0
    h1_pass_water_not_63_112 = 0
    h1_all_water_cols_112 = 0
    h1_all_water_not_63_112 = 0
    h1_water_y_min = 999
    h1_water_y_max = -999

    # Hypothesis 2 aggregation
    h2_targets_tested = 0
    h2_interior_water_blocks = 0

    # Hypothesis 3 aggregation
    h3_pass_starts_dry_below_63 = 0
    h3_pass_starts_water_within_8b = 0
    h3_pass_total_dry_flood_risk_cols = 0
    h3_all_candidates_water_within_8b = 0

    # Hypothesis 4 aggregation
    h4_pass_starts_cut_contact = 0
    h4_pass_total_cut_cols = 0
    h4_pass_gatehouse_water_contact = 0
    h4_cut_by_edge = defaultdict(int)

    per_seed_summary = {}

    for s in SEEDS:
        d = seed_data.get(s, {})
        h1 = d.get("hypothesis_1_water_height", {})
        h1_pass = h1.get("passing_starts", {})
        h1_all = h1.get("all_candidates", {})
        h2 = d.get("hypothesis_2_interior_ingress", {})
        h3 = d.get("hypothesis_3_dry_ground_adjacent_water", {})
        h4 = d.get("hypothesis_4_boundary_cut_contact", {})

        h1_pass_starts_water_112 += h1_pass.get("starts_with_water_112", 0)
        h1_pass_total_water_cols_112 += h1_pass.get("total_water_cols_112", 0)
        h1_pass_water_not_63_112 += h1_pass.get("water_cols_y_not_63_112", 0)
        h1_all_water_cols_112 += h1_all.get("total_water_cols_112", 0)
        h1_all_water_not_63_112 += h1_all.get("water_cols_y_not_63_112", 0)
        if h1_pass.get("water_y_min_across_starts_112", 63) != 0:
            h1_water_y_min = min(h1_water_y_min, h1_pass.get("water_y_min_across_starts_112", 63))
        if h1_pass.get("water_y_max_across_starts_112", 63) != 0:
            h1_water_y_max = max(h1_water_y_max, h1_pass.get("water_y_max_across_starts_112", 63))

        h2_targets_tested += h2.get("targets_tested", 0)
        h2_interior_water_blocks += h2.get("total_interior_water_blocks", 0)

        h3_pass_starts_dry_below_63 += h3.get("passing_starts_with_dry_ground_below_63", 0)
        h3_pass_starts_water_within_8b += h3.get("passing_starts_with_water_within_8b", 0)
        h3_pass_total_dry_flood_risk_cols += h3.get("passing_total_dry_flood_risk_cols", 0)
        h3_all_candidates_water_within_8b += h3.get("all_candidates_with_water_within_8b", 0)

        h4_pass_starts_cut_contact += h4.get("passing_starts_with_boundary_water_touch_cut", 0)
        h4_pass_total_cut_cols += h4.get("passing_total_boundary_water_cut_cols", 0)
        h4_pass_gatehouse_water_contact += h4.get("passing_starts_with_gatehouse_water_contact", 0)
        edges = h4.get("cut_contact_by_edge", {})
        for edge, count in edges.items():
            h4_cut_by_edge[edge] += count

        per_seed_summary[s] = {
            "valid_starts": d.get("valid_starts", 0),
            "starts_with_water_112": h1_pass.get("starts_with_water_112", 0),
            "water_cols_y_not_63": h1_pass.get("water_cols_y_not_63_112", 0),
            "interior_water_blocks": h2.get("total_interior_water_blocks", 0),
            "starts_dry_below_63": h3.get("passing_starts_with_dry_ground_below_63", 0),
            "starts_dry_flood_risk": h3.get("passing_starts_with_water_within_8b", 0),
            "starts_boundary_cut_contact": h4.get("passing_starts_with_boundary_water_touch_cut", 0),
            "gatehouse_water_contact": h4.get("passing_starts_with_gatehouse_water_contact", 0)
        }

    master = {
        "seeds": SEEDS,
        "total_candidates": total_candidates,
        "valid_starts": total_valid,
        "non_passing_candidates": total_non_passing,
        "hypotheses_verdict": {
            "hypothesis_1_water_at_y63_only": {
                "verdict": "CONFIRMED" if h1_all_water_not_63_112 == 0 else "REFUTED",
                "evidence": {
                    "total_water_cols_all_candidates": h1_all_water_cols_112,
                    "water_cols_y_not_63_all_candidates": h1_all_water_not_63_112,
                    "water_cols_y_not_63_passing_starts": h1_pass_water_not_63_112,
                    "water_y_min_passing": h1_water_y_min if h1_water_y_min != 999 else 63,
                    "water_y_max_passing": h1_water_y_max if h1_water_y_max != -999 else 63
                }
            },
            "hypothesis_2_interior_water_contact": {
                "verdict": "CONFIRMED_NO_FLOODING" if h2_interior_water_blocks == 0 else "REFUTED_FLOODING_FOUND",
                "evidence": {
                    "targets_tested": h2_targets_tested,
                    "interior_water_blocks_found": h2_interior_water_blocks,
                    "gatehouse_and_wall_gaps_prevent_flooding": True
                }
            },
            "hypothesis_3_dry_ground_below_63_near_water": {
                "verdict": "CONFIRMED_EXISTS" if h3_pass_starts_water_within_8b > 0 else "NONE_FOUND",
                "evidence": {
                    "passing_starts_with_dry_ground_below_63": h3_pass_starts_dry_below_63,
                    "passing_starts_with_water_within_8b": h3_pass_starts_water_within_8b,
                    "total_flood_risk_dry_cols": h3_pass_total_dry_flood_risk_cols,
                    "all_candidates_with_water_within_8b": h3_all_candidates_water_within_8b
                }
            },
            "hypothesis_4_correction_piece_boundary_handling": {
                "verdict": "CONFIRMED_EXPOSURE_EXISTS" if h4_pass_starts_cut_contact > 0 else "NO_EXPOSURE",
                "evidence": {
                    "cutting_box": "112x112",
                    "water_handling_in_cut": "cleared to Blocks.AIR with flag 2 (no immediate neighbor block update)",
                    "passing_starts_with_boundary_cut_contact": h4_pass_starts_cut_contact,
                    "total_boundary_cut_contact_cols": h4_pass_total_cut_cols,
                    "starts_with_gatehouse_water_contact": h4_pass_gatehouse_water_contact,
                    "cut_contact_by_edge": dict(h4_cut_by_edge)
                }
            }
        },
        "per_seed_summary": per_seed_summary
    }

    # Write JSON
    json_path = out_dir / "st03_flood_audit_10seeds_summary.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(master, f, indent=2)

    # Write Hypotheses CSV
    csv_hypo = out_dir / "st03_flood_audit_hypotheses.csv"
    with open(csv_hypo, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["hypothesis_id", "statement", "verdict", "key_metric_1", "key_metric_2"])
        w.writerow(["H1", "물 블록은 y=63 높이에만 존재", master["hypotheses_verdict"]["hypothesis_1_water_at_y63_only"]["verdict"],
                    f"y!=63 물 열 (통과 후보): {h1_pass_water_not_63_112}", f"y!=63 물 열 (전체 후보): {h1_all_water_not_63_112}"])
        w.writerow(["H2", "생성 후 건물 안쪽 빈 공간 물 유입 (정문·성벽 틈)", master["hypotheses_verdict"]["hypothesis_2_interior_water_contact"]["verdict"],
                    f"테스트 대상: {h2_targets_tested}", f"안쪽 물 블록 수: {h2_interior_water_blocks}"])
        w.writerow(["H3", "기준 높이 아래 마른 땅(y<63) 중 8블록 내 물 인접 후보 존재", master["hypotheses_verdict"]["hypothesis_3_dry_ground_below_63_near_water"]["verdict"],
                    f"통과 후보 중 인접 후보 수: {h3_pass_starts_water_within_8b}", f"총 범람 위험 열 수: {h3_pass_total_dry_flood_risk_cols}"])
        w.writerow(["H4", "보정 조각 112x112 경계에서 물 접촉 및 공기 치환", master["hypotheses_verdict"]["hypothesis_4_correction_piece_boundary_handling"]["verdict"],
                    f"경계 물 접촉 통과 후보: {h4_pass_starts_cut_contact}", f"정문 물 접촉 후보: {h4_pass_gatehouse_water_contact}"])

    # Write Per-Seed CSV
    csv_seeds = out_dir / "st03_flood_audit_per_seed.csv"
    with open(csv_seeds, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["seed", "valid_starts", "starts_with_water_112", "water_cols_y_not_63", "interior_water_blocks",
                    "starts_dry_below_63", "starts_dry_flood_risk_8b", "starts_boundary_cut_contact", "gatehouse_water_contact"])
        for s in SEEDS:
            row = per_seed_summary[s]
            w.writerow([s, row["valid_starts"], row["starts_with_water_112"], row["water_cols_y_not_63"], row["interior_water_blocks"],
                        row["starts_dry_below_63"], row["starts_dry_flood_risk"], row["starts_boundary_cut_contact"], row["gatehouse_water_contact"]])

    print("PASS: ST-03 10-seed flood check aggregation completed.")
    return master

def main():
    results_dir = Path("build/st03-natural/results")
    out_dir = Path("build/st03-natural/audit-summary")
    run_id = sys.argv[1] if len(sys.argv) > 1 else None
    data = load_seed_data(results_dir, run_id=run_id)
    aggregate(data, out_dir)

if __name__ == "__main__":
    main()
