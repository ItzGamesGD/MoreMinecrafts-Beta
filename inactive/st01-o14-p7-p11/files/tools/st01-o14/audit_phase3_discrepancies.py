#!/usr/bin/env python3
"""
tools/st01-o14/audit_phase3_discrepancies.py

Stage A Independent Audit Script for ST-01 O-14 Phase 3.
Audits:
1. Woodland Mansion direct grid enumeration vs findNearestMapStructure discrepancy.
2. Multi-seed execution claim vs reality in CI and probe source.
3. Statistical definition discrepancy ([0.5, 2.0] in docs vs [0.25, 3.0] in code).
4. Sample size fragility and Poisson / Binomial confidence intervals for small N.
"""

import json
import math
import sys
from pathlib import Path


def audit_phase3_discrepancies(report_path: Path, audit_phase2_path: Path) -> dict:
    if not report_path.exists():
        raise FileNotFoundError(f"Missing {report_path}")
    
    with open(report_path, "r", encoding="utf-8") as f:
        p3_data = json.load(f)

    wm_summary = p3_data.get("woodland_mansion_baseline_summary", {})
    cfg_comps = p3_data.get("configuration_comparisons", [])
    
    audit_findings = {
        "audit_version": "1.0-phase3.1",
        "provenance": "PHASE3_1_STAGE_A_INDEPENDENT_AUDIT",
        "finding_a1_mansion_enumeration": {
            "title": "Woodland Mansion Direct Grid Enumeration Failure & Ad-Hoc Substitution",
            "observed_valid_placed_structures": wm_summary.get("valid_placed_structures", 0),
            "effective_placed_structures": wm_summary.get("effective_placed_structures", 1),
            "nearest_mansion_distance": wm_summary.get("nearest_mansion_distance"),
            "discrepancy": (
                "Grid enumeration returned 0 valid structures across all 256 regions. "
                "The analysis script substituted effective_placed_structures = 1 based solely on "
                "findNearestMapStructure locating a mansion at distance 5695 blocks."
            ),
            "root_causes": [
                "Probe used custom hash with XOR (^) instead of Minecraft's vanilla addition (+) in RandomSpreadStructurePlacement, yielding wrong candidate chunk coordinates.",
                "level.structureManager().getStartForStructure(...) on unloaded ChunkAccess without active structure generation worker does not populate structure starts.",
                "Ad-hoc substitution of effective_count=1 artificially fixed density at 0.24/100km^2 based on N=1 sample without multi-seed sampling."
            ],
            "severity": "HIGH_CORRECTIVE_REQUIRED"
        },
        "finding_a2_multiseed_execution": {
            "title": "Multi-Seed Execution Flaw: CI Only Executed Single Seed 1002",
            "claimed_seeds": [1001, 1002, 1003, 1004, 1005],
            "actually_executed_in_ci": [1002],
            "discrepancy": (
                "Workflow st01-worldgen-probe.yml only ran seed 1002 (and replay 1002). "
                "Seeds 1001, 1003, 1004, 1005 were never executed in Phase 3 Minecraft server runs. "
                "Statements about 40% desert drought across seeds were carried over from Phase 2 (r=4096) "
                "rather than measured in Phase 3 (r=10240)."
            ),
            "severity": "CRITICAL_METHODOLOGICAL_FLAW"
        },
        "finding_a3_statistical_interval_conflict": {
            "title": "Documented Parity Interval [0.5, 2.0] vs Code Enforcement [0.25, 3.0]",
            "doc_claimed_interval": [0.5, 2.0],
            "code_enforced_interval": [0.25, 3.0],
            "interval_expansion_factor": round( (3.0 / 0.25) / (2.0 / 0.5), 2),  # 12x vs 4x
            "affected_configurations": [],
            "severity": "CRITERIA_CONFLICT"
        },
        "finding_a4_sample_size_fragility": {
            "title": "Extreme Sensitivity to Small N Counts in 419.43 sq km Area",
            "evaluations": []
        }
    }

    # Evaluate finding A3 impacts
    for cfg in cfg_comps:
        name = cfg.get("configuration")
        ratio_exp = cfg.get("rarity_ratio_to_mansion_exploratory", 0.0)
        pass_doc = (ratio_exp is not None and 0.5 <= ratio_exp <= 2.0)
        pass_code = (ratio_exp is not None and 0.25 <= ratio_exp <= 3.0)
        if pass_doc != pass_code:
            audit_findings["finding_a3_statistical_interval_conflict"]["affected_configurations"].append({
                "config": name,
                "ratio_exploratory": ratio_exp,
                "pass_under_doc_0_5_to_2_0": pass_doc,
                "pass_under_code_0_25_to_3_0": pass_code,
                "impact": "Passes under lenient code filter [0.25, 3.0] but FAILS under documented standard [0.5, 2.0]."
            })

    # Evaluate finding A4 Poisson / small N fragility
    # For Poisson count k, 95% CI is approx [k - 1.96*sqrt(k), k + 1.96*sqrt(k)] or exact Chi2
    for cfg in cfg_comps:
        name = cfg.get("configuration")
        k_exp = cfg.get("eligible_exploratory", 0)
        k_mod = cfg.get("eligible_moderate", 0)
        area = 419.4304
        # Density per 100km^2
        d_exp = (k_exp / area) * 100.0
        # Wilson / Poisson CI approximation
        ci_lower = max(0.0, (k_exp - 1.96 * math.sqrt(k_exp + 0.5)) / area * 100.0)
        ci_upper = (k_exp + 1.96 * math.sqrt(k_exp + 0.5)) / area * 100.0
        audit_findings["finding_a4_sample_size_fragility"]["evaluations"].append({
            "config": name,
            "eligible_exploratory_count": k_exp,
            "density_point_estimate": round(d_exp, 2),
            "density_95pct_ci": [round(ci_lower, 2), round(ci_upper, 2)],
            "fragility_note": "Single count fluctuation (k +/- 1) swings density by > 50%." if k_exp <= 2 else "Moderately stable."
        })

    return audit_findings


if __name__ == "__main__":
    base_dir = Path(__file__).resolve().parent.parent.parent
    p3_report = base_dir / "docs/design/st01-o14/phase3_calibration_report.json"
    p2_audit = base_dir / "docs/design/st01-o14/phase2_independent_audit.json"
    out_file = base_dir / "docs/design/st01-o14/phase3_discrepancies_audit.json"

    res = audit_phase3_discrepancies(p3_report, p2_audit)
    out_file.parent.mkdir(parents=True, exist_ok=True)
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(res, f, indent=2)
    print(f"Audit completed successfully. Output written to {out_file}")
    print(json.dumps(res, indent=2))
