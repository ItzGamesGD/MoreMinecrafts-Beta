# ST-05-A — Q109 reconfirmation and independent feasibility check

Design documentation only. Source PR #212 before this amendment: `3c0415f82c74b0720da1cd6e73668b423a2a3398`; base Wave15 `c9f7a4bf748a3f688c4a3f8103bc16ba789d9b50`. [Current authority](ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md) remains the specification.

## Provenance and comparison

The owner reconfirmed the ST-05-A report and all 23 Q109 names/scores in the current ChatGPT conversation. There is no external GitHub or Drive original and no original execution script or raw log. This message is a user reconfirmation source, not historical repository evidence or independent validation of the previous report. The historical Q109 wording about a server-JAR/static-code audit does not establish that this task obtained or repeated that audit.

Comparison against the existing current-authority tables: **15/15 active and 8/8 inactive names and provisional scores match exactly**. No roster entry, score or activation gate was changed.

The owner-reported earlier results were: seeds 0/1/42/109, 1–4 participants, R1–R6, 96/96 valid budgets/eligibility, R3 16/16 exactly one focal elite, R5–R6 32/32 able to include elites, peak 12 alive, 85–100% fill, no need for 105%. These remain reported results whose original execution cannot be inspected.

## Newly executed check and limits

The independent Python script below constructs 96 deterministic **feasibility witnesses** from only the active roster. It uses integer half-up budget rounding, cumulative score eligibility, unlimited repeated selections, and dynamic-programming reachability to fill 100% of each budget. Seeded choices distinguish cases but do not reproduce the unavailable historical algorithm. No selection weights are approved or inferred.

For R3 it reserves Zombie (melee), Skeleton (ranged), and Breeze (special/focal elite), then admits only scores below 7. Those role labels are witness assumptions based on the intended roles, not newly verified entity AI or an approved fixed R3 lineup. R5–R6 reserve one Breeze solely to demonstrate elite inclusion feasibility; this adds no elite-density rule. Spawn accounting simulates one eligible kill at a time and refill at <=6, to a cap of 12; it does not test spatial spawning, AI, Q110 blockage timing, absence, chunk reloads or a runtime controller.

New results: **96/96** budget/eligibility checks; **16/16** R3 with exactly one score>=7 elite; **32/32** R5–R6 with at least one elite; peak **12**; fill **100–100%**; exceptional 105% unused. This corroborates feasibility, not historical composition identity, gameplay balance or live acceptance. Minecraft/server-JAR/AI/live checks: **NOT RUN** in this documentation task.

## Reproduce

Save the following block as a separate `reproduce.py` outside the repository and run Python 3.12: `python reproduce.py docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md > results.json`. It validates the two roster tables against the owner-reconfirmed values and prints every composition plus the SHA-256 of its input specification. The script and results are embedded here to keep this PR documentation-only and the check reviewable without temporary files. CPython's seeded choice behavior is part of this reproducibility boundary; different Python implementations/versions may produce different witnesses.

```python
import collections
import hashlib
import json
import math
from pathlib import Path
import random
import re
import sys

spec = Path(sys.argv[1])
text = spec.read_text()
active = dict(zip(['Zombie','Baby Zombie','Husk','Skeleton','Bogged','Stray','Breeze','Cave Zombie','Cave Skeleton','Bandit','Bandit Archer','Large Zombie','Lesser Wither Skeleton','Armored Skeleton','Bandit Warrior'], [3,2,3,4,4,5,7,3,3,4,4,6,6,7,7]))
inactive = dict(zip(['Spider','Cave Spider','Small Spider','Large Spider','Silverfish','Leap Zombie','Bomb Zombie','Mining Giant'], [3,2,2,5,1,4,4,9]))
a = text.split('### Active Q109 baseline roster')[1].split('### Conditional Q109 candidates')[0]
b = text.split('### Conditional Q109 candidates')[1].split('## Supplies')[0]
def table(section):
    return {name.strip(): int(score) for name, score in re.findall(r'\|\s*([^|\n]+?)\s*\|\s*(\d+)\s*(?=\|)', section)}
assert table(a) == active
assert table(b) == inactive
rows = []
for seed in (0,1,42,109):
    for players in range(1,5):
        for round_no in range(1,7):
            budget = (20 if round_no <= 2 else 32 if round_no <= 4 else 48)
            budget = (budget * (10 + 6*(players-1)) + 5)//10
            cap = 4 if round_no <= 2 else 7 if round_no <= 4 else 10
            pool = [name for name, score in active.items() if score <= cap]
            # Feasibility witnesses, not approved selection weights or runtime roles.
            chosen = ['Zombie','Skeleton','Breeze'] if round_no == 3 else ['Breeze'] if round_no >= 5 else []
            if round_no == 3:
                pool = [name for name in pool if active[name] < 7]
            remaining = budget - sum(active[name] for name in chosen)
            reachable = [True] + [False]*remaining
            for amount in range(1,remaining+1):
                reachable[amount] = any(active[name] <= amount and reachable[amount-active[name]] for name in pool)
            assert reachable[remaining]
            rng = random.Random(f'{seed}/{players}/{round_no}')
            while remaining:
                name = rng.choice([name for name in pool if active[name] <= remaining and reachable[remaining-active[name]]])
                chosen.append(name)
                remaining -= active[name]
            score = sum(active[name] for name in chosen)
            assert math.ceil(.85*budget) <= score <= budget
            assert all(active[name] <= cap for name in chosen)
            assert not set(chosen) & set(inactive)
            elite = sum(active[name] >= 7 for name in chosen)
            assert round_no != 3 or elite == 1
            assert round_no < 5 or elite >= 1
            pending, alive, peak = len(chosen), 0, 0
            while pending or alive:
                if pending and alive <= 6:
                    batch = min(pending,12-alive)
                    pending -= batch
                    alive += batch
                peak = max(peak,alive)
                assert alive <= 12
                if alive:
                    alive -= 1
            rows.append(dict(seed=seed,players=players,round=round_no,budget=budget,score=score,elite=elite,peak_alive=peak,composition=dict(sorted(collections.Counter(chosen).items()))))
assert len(rows) == 96
print(json.dumps(dict(spec_sha256=hashlib.sha256(spec.read_bytes()).hexdigest(), roster_matches=23, cases=len(rows),valid=96,r3_exactly_one_elite=sum(r['round']==3 and r['elite']==1 for r in rows),r5_r6_elite=sum(r['round']>=5 and r['elite']>=1 for r in rows),max_alive=max(r['peak_alive'] for r in rows),min_fill=min(r['score']/r['budget'] for r in rows),max_fill=max(r['score']/r['budget'] for r in rows),rows=rows),indent=2))
```

## Recorded independent allocations

Each composition is a multiset; this table does not prescribe spawn order.

| Seed | Players | Round | Budget = used | Elite count | Peak alive | Composition |
|---:|---:|---:|---:|---:|---:|---|
| 0 | 1 | R1 | 20 | 0 | 7 | Baby Zombie ×2, Cave Skeleton ×2, Husk ×2, Skeleton ×1 |
| 0 | 1 | R2 | 20 | 0 | 7 | Baby Zombie ×3, Bandit ×2, Cave Skeleton ×1, Zombie ×1 |
| 0 | 1 | R3 | 32 | 1 | 9 | Baby Zombie ×2, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×1, Skeleton ×1, Stray ×1, Zombie ×2 |
| 0 | 1 | R4 | 32 | 3 | 6 | Armored Skeleton ×1, Baby Zombie ×1, Bandit Archer ×1, Bandit Warrior ×2, Stray ×1 |
| 0 | 1 | R5 | 48 | 4 | 10 | Armored Skeleton ×1, Bandit Warrior ×1, Breeze ×2, Cave Skeleton ×1, Cave Zombie ×2, Husk ×2, Stray ×1 |
| 0 | 1 | R6 | 48 | 3 | 12 | Baby Zombie ×3, Bandit ×1, Bandit Warrior ×1, Breeze ×2, Husk ×1, Stray ×1, Zombie ×3 |
| 0 | 2 | R1 | 32 | 0 | 10 | Baby Zombie ×1, Bandit ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×2, Skeleton ×1, Zombie ×2 |
| 0 | 2 | R2 | 32 | 0 | 10 | Baby Zombie ×2, Bandit ×1, Bogged ×2, Cave Skeleton ×3, Skeleton ×1, Zombie ×1 |
| 0 | 2 | R3 | 51 | 1 | 12 | Bandit ×2, Bandit Archer ×2, Bogged ×1, Breeze ×1, Cave Zombie ×2, Husk ×1, Skeleton ×3, Zombie ×1 |
| 0 | 2 | R4 | 51 | 1 | 12 | Armored Skeleton ×1, Baby Zombie ×1, Bandit ×1, Bandit Archer ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Lesser Wither Skeleton ×1, Skeleton ×2, Stray ×1, Zombie ×2 |
| 0 | 2 | R5 | 77 | 3 | 12 | Armored Skeleton ×1, Baby Zombie ×2, Bandit Warrior ×1, Breeze ×1, Husk ×2, Large Zombie ×2, Lesser Wither Skeleton ×2, Skeleton ×1, Stray ×3, Zombie ×1 |
| 0 | 2 | R6 | 77 | 4 | 12 | Armored Skeleton ×1, Baby Zombie ×1, Bandit ×2, Bandit Warrior ×1, Bogged ×1, Breeze ×2, Cave Zombie ×4, Husk ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×2 |
| 0 | 3 | R1 | 44 | 0 | 12 | Baby Zombie ×1, Bandit ×3, Bandit Archer ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×2, Husk ×1, Skeleton ×1, Zombie ×2 |
| 0 | 3 | R2 | 44 | 0 | 12 | Bandit ×4, Bandit Archer ×2, Bogged ×1, Cave Skeleton ×2, Cave Zombie ×1, Husk ×1, Skeleton ×1 |
| 0 | 3 | R3 | 70 | 1 | 12 | Baby Zombie ×5, Bandit ×3, Bogged ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×2, Large Zombie ×1, Skeleton ×4, Zombie ×1 |
| 0 | 3 | R4 | 70 | 2 | 12 | Armored Skeleton ×1, Baby Zombie ×1, Bandit ×2, Bandit Archer ×1, Bandit Warrior ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Large Zombie ×2, Skeleton ×3, Stray ×1 |
| 0 | 3 | R5 | 106 | 3 | 12 | Baby Zombie ×2, Bandit ×1, Bandit Archer ×2, Bandit Warrior ×1, Bogged ×1, Breeze ×2, Cave Skeleton ×2, Cave Zombie ×1, Husk ×2, Large Zombie ×3, Skeleton ×2, Stray ×3, Zombie ×3 |
| 0 | 3 | R6 | 106 | 6 | 12 | Armored Skeleton ×3, Bandit Warrior ×1, Bogged ×1, Breeze ×2, Cave Skeleton ×2, Cave Zombie ×1, Husk ×1, Large Zombie ×4, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×1, Zombie ×3 |
| 0 | 4 | R1 | 56 | 0 | 12 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×3, Bogged ×1, Cave Skeleton ×5, Cave Zombie ×2, Skeleton ×1, Zombie ×3 |
| 0 | 4 | R2 | 56 | 0 | 12 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×4, Bogged ×1, Cave Skeleton ×4, Cave Zombie ×1, Husk ×1, Skeleton ×3 |
| 0 | 4 | R3 | 90 | 1 | 12 | Baby Zombie ×2, Bandit ×1, Bogged ×1, Breeze ×1, Cave Skeleton ×2, Cave Zombie ×2, Husk ×4, Large Zombie ×1, Lesser Wither Skeleton ×5, Skeleton ×2, Zombie ×1 |
| 0 | 4 | R4 | 90 | 5 | 12 | Armored Skeleton ×2, Bandit Archer ×1, Bogged ×4, Breeze ×3, Cave Skeleton ×2, Cave Zombie ×1, Husk ×1, Stray ×4, Zombie ×1 |
| 0 | 4 | R5 | 134 | 6 | 12 | Armored Skeleton ×2, Baby Zombie ×1, Bandit ×2, Bandit Archer ×2, Bandit Warrior ×2, Breeze ×2, Cave Skeleton ×2, Cave Zombie ×1, Husk ×2, Large Zombie ×3, Skeleton ×1, Stray ×5, Zombie ×4 |
| 0 | 4 | R6 | 134 | 7 | 12 | Armored Skeleton ×1, Baby Zombie ×1, Bandit ×1, Bandit Archer ×4, Bandit Warrior ×3, Bogged ×1, Breeze ×3, Cave Zombie ×3, Husk ×3, Large Zombie ×5, Lesser Wither Skeleton ×1, Stray ×1 |
| 1 | 1 | R1 | 20 | 0 | 6 | Bandit ×1, Bogged ×1, Cave Zombie ×1, Zombie ×3 |
| 1 | 1 | R2 | 20 | 0 | 6 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×2, Cave Zombie ×1, Zombie ×1 |
| 1 | 1 | R3 | 32 | 1 | 8 | Bandit Archer ×1, Bogged ×1, Breeze ×1, Cave Zombie ×1, Skeleton ×2, Zombie ×2 |
| 1 | 1 | R4 | 32 | 1 | 8 | Baby Zombie ×2, Bandit Archer ×1, Bandit Warrior ×1, Cave Zombie ×1, Husk ×1, Large Zombie ×1, Stray ×1 |
| 1 | 1 | R5 | 48 | 1 | 10 | Baby Zombie ×2, Breeze ×1, Cave Zombie ×1, Large Zombie ×2, Lesser Wither Skeleton ×2, Stray ×2 |
| 1 | 1 | R6 | 48 | 1 | 11 | Baby Zombie ×1, Bogged ×1, Breeze ×1, Cave Skeleton ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×3, Stray ×1, Zombie ×1 |
| 1 | 2 | R1 | 32 | 0 | 10 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×1, Cave Skeleton ×2, Husk ×2, Skeleton ×1, Zombie ×2 |
| 1 | 2 | R2 | 32 | 0 | 10 | Baby Zombie ×2, Bandit ×2, Bandit Archer ×2, Cave Zombie ×2, Zombie ×2 |
| 1 | 2 | R3 | 51 | 1 | 12 | Bandit ×2, Bandit Archer ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×2, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×2, Zombie ×1 |
| 1 | 2 | R4 | 51 | 2 | 11 | Armored Skeleton ×1, Bandit Archer ×2, Bandit Warrior ×1, Husk ×2, Lesser Wither Skeleton ×2, Stray ×1, Zombie ×2 |
| 1 | 2 | R5 | 77 | 3 | 12 | Armored Skeleton ×1, Baby Zombie ×4, Bandit ×1, Bandit Archer ×1, Breeze ×2, Cave Skeleton ×1, Cave Zombie ×1, Lesser Wither Skeleton ×4, Skeleton ×1, Zombie ×2 |
| 1 | 2 | R6 | 77 | 4 | 12 | Baby Zombie ×2, Bandit Archer ×3, Bandit Warrior ×1, Bogged ×3, Breeze ×3, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Skeleton ×1, Stray ×1, Zombie ×1 |
| 1 | 3 | R1 | 44 | 0 | 12 | Bandit ×2, Bandit Archer ×2, Cave Skeleton ×1, Cave Zombie ×3, Husk ×1, Skeleton ×1, Zombie ×3 |
| 1 | 3 | R2 | 44 | 0 | 12 | Baby Zombie ×2, Bandit Archer ×3, Cave Skeleton ×4, Husk ×1, Skeleton ×1, Zombie ×3 |
| 1 | 3 | R3 | 70 | 1 | 12 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×1, Bogged ×2, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×2, Large Zombie ×2, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×1, Zombie ×3 |
| 1 | 3 | R4 | 70 | 4 | 12 | Armored Skeleton ×1, Baby Zombie ×1, Bandit ×4, Bandit Warrior ×1, Bogged ×1, Breeze ×2, Lesser Wither Skeleton ×2, Stray ×1, Zombie ×1 |
| 1 | 3 | R5 | 106 | 6 | 12 | Armored Skeleton ×1, Bandit ×1, Bandit Archer ×2, Bandit Warrior ×1, Breeze ×4, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Large Zombie ×2, Lesser Wither Skeleton ×3, Skeleton ×2, Stray ×1 |
| 1 | 3 | R6 | 106 | 5 | 12 | Armored Skeleton ×1, Baby Zombie ×1, Bandit ×2, Bandit Warrior ×2, Bogged ×4, Breeze ×2, Cave Zombie ×1, Husk ×2, Large Zombie ×2, Lesser Wither Skeleton ×2, Skeleton ×3 |
| 1 | 4 | R1 | 56 | 0 | 12 | Baby Zombie ×2, Bandit ×4, Bandit Archer ×1, Bogged ×2, Cave Skeleton ×2, Cave Zombie ×4, Zombie ×2 |
| 1 | 4 | R2 | 56 | 0 | 12 | Baby Zombie ×2, Bandit ×1, Bandit Archer ×3, Bogged ×1, Cave Skeleton ×2, Cave Zombie ×2, Husk ×1, Skeleton ×2, Zombie ×3 |
| 1 | 4 | R3 | 90 | 1 | 12 | Baby Zombie ×2, Bandit ×1, Bandit Archer ×1, Bogged ×3, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×2, Lesser Wither Skeleton ×3, Skeleton ×2, Stray ×3, Zombie ×2 |
| 1 | 4 | R4 | 90 | 4 | 12 | Armored Skeleton ×2, Baby Zombie ×1, Bandit ×2, Bandit Archer ×2, Bandit Warrior ×2, Cave Skeleton ×2, Cave Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×2, Zombie ×5 |
| 1 | 4 | R5 | 134 | 5 | 12 | Armored Skeleton ×1, Baby Zombie ×3, Bandit ×2, Bandit Archer ×1, Bandit Warrior ×1, Bogged ×3, Breeze ×3, Cave Skeleton ×3, Husk ×3, Large Zombie ×3, Lesser Wither Skeleton ×1, Skeleton ×2, Stray ×2, Zombie ×3 |
| 1 | 4 | R6 | 134 | 10 | 12 | Armored Skeleton ×5, Baby Zombie ×1, Bandit ×2, Bandit Archer ×1, Bandit Warrior ×2, Breeze ×3, Cave Skeleton ×4, Husk ×1, Lesser Wither Skeleton ×1, Skeleton ×4, Stray ×2, Zombie ×1 |
| 42 | 1 | R1 | 20 | 0 | 6 | Baby Zombie ×1, Bandit ×1, Bogged ×1, Skeleton ×1, Zombie ×2 |
| 42 | 1 | R2 | 20 | 0 | 6 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×1, Bogged ×1, Cave Zombie ×1, Zombie ×1 |
| 42 | 1 | R3 | 32 | 1 | 7 | Bandit Archer ×1, Bogged ×1, Breeze ×1, Skeleton ×1, Stray ×2, Zombie ×1 |
| 42 | 1 | R4 | 32 | 0 | 7 | Bandit ×2, Cave Zombie ×1, Large Zombie ×1, Lesser Wither Skeleton ×2, Zombie ×1 |
| 42 | 1 | R5 | 48 | 2 | 11 | Baby Zombie ×1, Bandit ×1, Bogged ×1, Breeze ×2, Cave Skeleton ×1, Cave Zombie ×1, Large Zombie ×1, Skeleton ×3 |
| 42 | 1 | R6 | 48 | 4 | 10 | Armored Skeleton ×3, Baby Zombie ×1, Bandit ×1, Bogged ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×1, Skeleton ×1 |
| 42 | 2 | R1 | 32 | 0 | 10 | Baby Zombie ×2, Bandit ×2, Bogged ×2, Cave Skeleton ×1, Husk ×1, Zombie ×2 |
| 42 | 2 | R2 | 32 | 0 | 11 | Baby Zombie ×4, Bandit Archer ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×2, Skeleton ×1, Zombie ×1 |
| 42 | 2 | R3 | 51 | 1 | 12 | Baby Zombie ×1, Bogged ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×2, Zombie ×1 |
| 42 | 2 | R4 | 51 | 3 | 10 | Armored Skeleton ×1, Bandit ×1, Bandit Warrior ×1, Breeze ×1, Cave Zombie ×2, Large Zombie ×2, Stray ×1, Zombie ×1 |
| 42 | 2 | R5 | 77 | 2 | 12 | Baby Zombie ×2, Bandit ×2, Bandit Archer ×1, Breeze ×2, Cave Zombie ×3, Large Zombie ×3, Lesser Wither Skeleton ×2, Stray ×1, Zombie ×1 |
| 42 | 2 | R6 | 77 | 4 | 12 | Bandit ×4, Bandit Warrior ×2, Bogged ×1, Breeze ×2, Cave Zombie ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×2 |
| 42 | 3 | R1 | 44 | 0 | 12 | Bandit Archer ×1, Bogged ×3, Cave Skeleton ×2, Cave Zombie ×2, Husk ×1, Skeleton ×1, Zombie ×3 |
| 42 | 3 | R2 | 44 | 0 | 12 | Baby Zombie ×3, Bandit ×3, Bandit Archer ×1, Cave Zombie ×1, Husk ×2, Skeleton ×1, Zombie ×3 |
| 42 | 3 | R3 | 70 | 1 | 12 | Baby Zombie ×2, Bogged ×2, Breeze ×1, Cave Zombie ×2, Large Zombie ×2, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×4, Zombie ×1 |
| 42 | 3 | R4 | 70 | 6 | 12 | Armored Skeleton ×4, Baby Zombie ×3, Breeze ×2, Husk ×1, Skeleton ×2, Stray ×1, Zombie ×2 |
| 42 | 3 | R5 | 106 | 6 | 12 | Armored Skeleton ×1, Baby Zombie ×3, Bandit ×2, Bandit Archer ×2, Bandit Warrior ×1, Breeze ×4, Cave Zombie ×1, Husk ×2, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×2, Stray ×2, Zombie ×1 |
| 42 | 3 | R6 | 106 | 4 | 12 | Armored Skeleton ×3, Baby Zombie ×4, Bandit ×5, Bandit Archer ×1, Bogged ×2, Breeze ×1, Cave Zombie ×2, Husk ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×2, Zombie ×1 |
| 42 | 4 | R1 | 56 | 0 | 12 | Baby Zombie ×1, Bandit ×2, Bandit Archer ×1, Cave Skeleton ×2, Cave Zombie ×2, Husk ×6, Skeleton ×3 |
| 42 | 4 | R2 | 56 | 0 | 12 | Baby Zombie ×2, Bandit ×3, Bandit Archer ×1, Bogged ×2, Cave Skeleton ×2, Cave Zombie ×4, Skeleton ×1, Zombie ×2 |
| 42 | 4 | R3 | 90 | 1 | 12 | Baby Zombie ×2, Bandit ×2, Bandit Archer ×3, Breeze ×1, Cave Skeleton ×5, Cave Zombie ×2, Husk ×2, Large Zombie ×1, Lesser Wither Skeleton ×2, Skeleton ×2, Zombie ×2 |
| 42 | 4 | R4 | 90 | 3 | 12 | Armored Skeleton ×1, Baby Zombie ×5, Bandit ×1, Bandit Archer ×2, Bandit Warrior ×1, Bogged ×4, Breeze ×1, Cave Skeleton ×1, Husk ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×2, Stray ×1 |
| 42 | 4 | R5 | 134 | 7 | 12 | Armored Skeleton ×2, Baby Zombie ×2, Bandit ×2, Bandit Warrior ×3, Bogged ×3, Breeze ×2, Cave Skeleton ×3, Cave Zombie ×3, Husk ×2, Large Zombie ×2, Skeleton ×3, Stray ×2, Zombie ×1 |
| 42 | 4 | R6 | 134 | 3 | 12 | Baby Zombie ×1, Bandit ×3, Bandit Archer ×2, Bandit Warrior ×1, Bogged ×1, Breeze ×2, Cave Skeleton ×1, Cave Zombie ×1, Husk ×3, Large Zombie ×1, Lesser Wither Skeleton ×4, Skeleton ×2, Stray ×5, Zombie ×3 |
| 109 | 1 | R1 | 20 | 0 | 6 | Baby Zombie ×1, Bandit ×1, Bogged ×2, Cave Zombie ×1, Zombie ×1 |
| 109 | 1 | R2 | 20 | 0 | 6 | Baby Zombie ×1, Bandit ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×1, Skeleton ×1 |
| 109 | 1 | R3 | 32 | 1 | 9 | Baby Zombie ×2, Bandit Archer ×1, Bogged ×1, Breeze ×1, Cave Skeleton ×1, Husk ×1, Skeleton ×1, Zombie ×1 |
| 109 | 1 | R4 | 32 | 2 | 6 | Bogged ×1, Breeze ×2, Cave Skeleton ×1, Lesser Wither Skeleton ×1, Stray ×1 |
| 109 | 1 | R5 | 48 | 3 | 10 | Armored Skeleton ×2, Bandit ×1, Bogged ×2, Breeze ×1, Skeleton ×1, Stray ×1, Zombie ×2 |
| 109 | 1 | R6 | 48 | 3 | 11 | Baby Zombie ×1, Bandit ×2, Bandit Archer ×1, Breeze ×3, Cave Zombie ×1, Skeleton ×1, Zombie ×2 |
| 109 | 2 | R1 | 32 | 0 | 10 | Baby Zombie ×1, Bandit ×1, Bogged ×1, Cave Skeleton ×1, Husk ×4, Skeleton ×1, Zombie ×1 |
| 109 | 2 | R2 | 32 | 0 | 9 | Bandit ×2, Bandit Archer ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Skeleton ×2, Zombie ×1 |
| 109 | 2 | R3 | 51 | 1 | 12 | Bandit ×2, Bandit Archer ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×2, Husk ×1, Large Zombie ×1, Skeleton ×2, Zombie ×2 |
| 109 | 2 | R4 | 51 | 1 | 12 | Baby Zombie ×2, Bandit ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×1, Husk ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Stray ×3 |
| 109 | 2 | R5 | 77 | 4 | 12 | Armored Skeleton ×1, Baby Zombie ×2, Bandit ×1, Bandit Archer ×3, Bandit Warrior ×1, Breeze ×2, Cave Zombie ×1, Husk ×5, Stray ×1, Zombie ×2 |
| 109 | 2 | R6 | 77 | 2 | 12 | Bandit ×1, Bandit Warrior ×1, Breeze ×1, Cave Skeleton ×1, Cave Zombie ×2, Husk ×3, Large Zombie ×3, Lesser Wither Skeleton ×1, Skeleton ×1, Stray ×2, Zombie ×1 |
| 109 | 3 | R1 | 44 | 0 | 12 | Baby Zombie ×3, Bandit ×1, Bandit Archer ×1, Cave Zombie ×4, Husk ×2, Zombie ×4 |
| 109 | 3 | R2 | 44 | 0 | 12 | Baby Zombie ×2, Bandit Archer ×2, Bogged ×2, Cave Skeleton ×2, Cave Zombie ×3, Husk ×2, Zombie ×1 |
| 109 | 3 | R3 | 70 | 1 | 12 | Baby Zombie ×1, Bandit ×1, Bandit Archer ×2, Breeze ×1, Cave Skeleton ×1, Husk ×1, Large Zombie ×2, Lesser Wither Skeleton ×2, Skeleton ×2, Stray ×1, Zombie ×2 |
| 109 | 3 | R4 | 70 | 2 | 12 | Armored Skeleton ×1, Baby Zombie ×2, Bandit ×1, Bandit Archer ×1, Bandit Warrior ×1, Bogged ×3, Cave Skeleton ×2, Cave Zombie ×2, Husk ×1, Large Zombie ×1, Stray ×1, Zombie ×2 |
| 109 | 3 | R5 | 106 | 11 | 12 | Armored Skeleton ×3, Bandit ×4, Bandit Warrior ×2, Breeze ×6, Cave Zombie ×1, Husk ×1, Skeleton ×1, Zombie ×1 |
| 109 | 3 | R6 | 106 | 7 | 12 | Armored Skeleton ×2, Baby Zombie ×3, Bandit ×1, Bandit Warrior ×1, Breeze ×4, Cave Zombie ×2, Husk ×1, Large Zombie ×2, Stray ×4, Zombie ×2 |
| 109 | 4 | R1 | 56 | 0 | 12 | Baby Zombie ×2, Bandit ×2, Bandit Archer ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×2, Husk ×2, Skeleton ×3, Zombie ×3 |
| 109 | 4 | R2 | 56 | 0 | 12 | Baby Zombie ×3, Bogged ×2, Cave Skeleton ×3, Cave Zombie ×2, Husk ×1, Skeleton ×3, Zombie ×4 |
| 109 | 4 | R3 | 90 | 1 | 12 | Baby Zombie ×3, Bandit ×2, Bandit Archer ×1, Breeze ×1, Cave Skeleton ×2, Cave Zombie ×1, Husk ×3, Large Zombie ×1, Lesser Wither Skeleton ×2, Skeleton ×2, Stray ×3, Zombie ×2 |
| 109 | 4 | R4 | 90 | 3 | 12 | Armored Skeleton ×3, Baby Zombie ×1, Bandit ×2, Bandit Archer ×1, Bogged ×1, Cave Skeleton ×1, Cave Zombie ×4, Husk ×1, Large Zombie ×1, Lesser Wither Skeleton ×1, Skeleton ×2, Stray ×2, Zombie ×1 |
| 109 | 4 | R5 | 134 | 7 | 12 | Armored Skeleton ×3, Bandit ×1, Bandit Warrior ×2, Bogged ×3, Breeze ×2, Cave Zombie ×2, Husk ×3, Lesser Wither Skeleton ×4, Skeleton ×1, Stray ×4, Zombie ×2 |
| 109 | 4 | R6 | 134 | 8 | 12 | Armored Skeleton ×4, Bandit ×2, Bandit Archer ×1, Bogged ×1, Breeze ×4, Cave Skeleton ×2, Husk ×3, Large Zombie ×1, Lesser Wither Skeleton ×2, Skeleton ×2, Stray ×3, Zombie ×2 |
