#!/usr/bin/env python3
"""Read-only validation of the XP Quest audit inventory against an exact Q1-C catalog."""
import argparse
import csv
import json
from collections import Counter
from pathlib import Path

def validate(catalog_path: Path, inventory_path: Path) -> None:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    with inventory_path.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))
    source = catalog["quests"]
    assert len(source) == len(rows) == 200
    by_id = {row["id"]: row for row in rows}
    assert len(by_id) == 200
    assert set(by_id) == {quest["id"] for quest in source}
    for quest in source:
        row = by_id[quest["id"]]
        for field, key in (
            ("tab", "tab"), ("tier", "tier"), ("status", "status"),
            ("tracker_kind", "kind")
        ):
            value = quest["tracker"][key] if field == "tracker_kind" else quest[key]
            assert row[field] == value, (quest["id"], field)
        assert row["registered"] == str(quest["registered"]).lower(), quest["id"]
        assert int(row["target"]) == quest["target"], quest["id"]
        assert int(row["raw_xp"]) == quest["rewardRawXp"], quest["id"]
        assert json.loads(row["tracker_json"]) == quest["tracker"], quest["id"]
    assert Counter(row["registered"] for row in rows) == {"true": 75, "false": 125}
    assert Counter(row["status"] for row in rows) == {"NOW": 163, "VERIFY": 17, "FUTURE": 20}
    assert sum(int(row["raw_xp"]) for row in rows) == 209100
    assert sum(int(row["raw_xp"]) for row in rows if row["status"] != "FUTURE") == 180000
    print("PASS: 200 unique IDs; 75/125 registration; exact fields; 209100/180000 XP")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", type=Path, help="Q1-C catalog.json at pinned source SHA")
    parser.add_argument("inventory", type=Path, help="published quest_inventory.csv")
    args = parser.parse_args()
    validate(args.catalog, args.inventory)
