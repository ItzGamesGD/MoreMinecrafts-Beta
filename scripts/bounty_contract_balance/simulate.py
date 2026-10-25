"""Deterministic BC-02 sensitivity model. All market/effort numbers are hypotheses."""
from __future__ import annotations
import json,random,re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs/bounty_contract/BC-02_SIMULATION_RESULTS.md'
SEED=20261022
N=10000
TIERS={'beginner':((2,4),(5,10),(80,17,2.8,.2)),'intermediate':((6,10),(12,24),(60,30,9,1)),'advanced':((12,20),(25,50),(40,40,17,3))}
RARITY={'common':(16,48),'high-grade':(8,24),'rare':(2,8),'legendary':(1,2)}
SPECIAL=(20,45,30,5)
# Emerald-equivalent reward unit prices are assumed, not verified gameplay values.
VALUES={'low':(.02,.08,.4,2),'mid':(.08,.3,2,12),'high':(.2,1,8,48)}

def catalog_ids(filename):
    return set(re.findall(r'`((?:moreminecrafts|minecraft):[a-z0-9_]+)`', (ROOT/'docs/bounty_contract'/filename).read_text()))

MOBS=catalog_ids('BC-01_MOB_CATALOG.md')
ITEMS=catalog_ids('BC-01_DELIVERY_CATALOG.md')
STRUCTURES=catalog_ids('BC-01_EXPLORATION_CATALOG.md')
assert len(MOBS)==40
assert abs(sum(SPECIAL)-100)<1e-9
assert all(abs(sum(v[2])-100)<1e-9 for v in TIERS.values())

SCENARIOS=[
 ('beginner combat','beginner','combat','moreminecrafts:swamp_zombie',12,0,12),
 ('intermediate combat','intermediate','combat','moreminecrafts:bandit_archer',30,0,45),
 ('advanced combat','advanced','combat','moreminecrafts:lesser_magma_cube',60,0,120),
 ('common delivery','beginner','delivery','minecraft:wheat',24,.04,8),
 ('rare delivery','advanced','delivery','minecraft:diamond',8,3,80),
 ('MM material delivery','advanced','delivery','moreminecrafts:adamantium_ingot',8,12,160),
 ('exploration','intermediate','exploration','minecraft:mineshaft',1,0,35),
 ('special trial','advanced','special','minecraft:trial_chambers',1,0,120),
]

def validate():
    for _,_,kind,target,*_ in SCENARIOS:
        pool={'combat':MOBS,'delivery':ITEMS,'exploration':STRUCTURES,'special':STRUCTURES}[kind]
        if target not in pool: raise ValueError(f'unverified target {target}')

def sample(tier, special, value_case, rng):
    (plo,phi),(rlo,rhi),weights=TIERS[tier]
    if special: weights=SPECIAL
    price=rng.randint(plo,phi); payout=rng.randint(rlo,rhi)
    rarity=rng.choices(tuple(RARITY),weights=weights)[0]
    q=rng.randint(*RARITY[rarity]); unit=VALUES[value_case][tuple(RARITY).index(rarity)]
    return price,payout,rarity,q,q*unit

def run(value_case='mid'):
    validate();rng=random.Random(SEED);out=[]
    for label,tier,kind,target,count,unit_cost,effort in SCENARIOS:
        records=[];tail=0
        for _ in range(N):
            price,payout,rarity,q,reward=sample(tier,kind=='special',value_case,rng)
            tail+=rarity=='legendary'
            # cost is opportunity cost, not an emerald payment to the contract UI
            net=payout-price-count*unit_cost+reward
            records.append(net)
        records.sort()
        out.append({'scenario':label,'target':target,'tier':tier,'n':N,'effort_units_assumed':effort,'material_cost_assumed':round(count*unit_cost,2),'net_mean':round(sum(records)/N,2),'net_p05':round(records[int(N*.05)],2),'net_p95':round(records[int(N*.95)],2),'profit_per_effort':round(sum(records)/N/effort,3),'legendary_rate':round(tail/N,4)})
    return out

def stacking():
    # One shared kill credits every compatible contract; all purchases and payouts remain distinct.
    return [{'contracts':n,'shared_kills':12,'price_mean':3*n,'payout_mean':7.5*n,'emerald_net_mean':4.5*n,'emerald_net_per_kill':round(4.5*n/12,3)} for n in (1,2,5,10)]

def duplicates():
    return {'requirements':[8,16],'kills_to_finish':16,'counters_after_8':[8,8],'counters_after_16':[8,16]}

def delivery_competition():
    inventory=32;requirements=[32,32];completed=[]
    for q in requirements:
        if inventory>=q:inventory-=q;completed.append(True)
        else:completed.append(False)
    return {'starting_inventory':32,'requirements':requirements,'completed_in_order':completed,'remaining_inventory':inventory}

def render():
    cases={k:run(k) for k in VALUES}
    mid=cases['mid']
    s=['# BC-02 Simulation Results','','Reproduce: `python scripts/bounty_contract_balance/simulate.py`; test: `python -m unittest discover -s scripts/bounty_contract_balance -p "test_*.py"`. Python standard library only. Seed `20261022`; 10,000 independent contracts per scenario and sensitivity case. Script overwrites this Markdown deterministically.','','**Model inputs:** Target IDs are read from the BC-01 catalogs and checked for membership. Price/payout, rarity and quantity bands come from approved authority. Sampled price/payout are uniform *within* approved bands solely for sensitivity; real effort adjustment is OPEN. Reward unit emerald equivalents are assumptions: low `(0.02, 0.08, 0.4, 2)`, mid `(0.08, 0.3, 2, 12)`, high `(0.2, 1, 8, 48)` for common/high-grade/rare/legendary. They are not verified prices or reward-pool contents. Effort units and delivery opportunity costs are scenario hypotheses, not observed gameplay facts.','','| Scenario / BC-01 ID | Tier | Cost assumption | Effort assumption | Mid net mean | Mid p05–p95 | Low–high mean | Mid net / effort | Legendary sampled |','| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |']
    for i,row in enumerate(mid):
        low=cases['low'][i];high=cases['high'][i]
        s.append(f"| {row['scenario']} `{row['target']}` | {row['tier']} | {row['material_cost_assumed']} | {row['effort_units_assumed']} | {row['net_mean']} | {row['net_p05']}–{row['net_p95']} | {low['net_mean']}–{high['net_mean']} | {row['profit_per_effort']} | {row['legendary_rate']:.2%} |")
    s+=['','Net = emerald payout − purchase price − assumed material opportunity cost + assumed extra reward value. Material cost is a hypothetical conversion to emerald equivalents. Negative net means economic loss under that assumption. Completion effort is a normalized hypothesis; no measured spawn, catch or travel rates were available.','','## Compatible shared combat stacking','','| Contracts | Shared qualifying kills | Mean purchase | Mean emerald payout | Mean emerald net | Net / kill |','| ---: | ---: | ---: | ---: | ---: | ---: |']
    for x in stacking():s.append(f"| {x['contracts']} | {x['shared_kills']} | {x['price_mean']} | {x['payout_mean']} | {x['emerald_net_mean']} | {x['emerald_net_per_kill']} |")
    s+=['','One kill advances all matching accepted contracts, including both entries in one contract. Duplicate requirements 8 and 16 require 16 kills total, with counters `(8,8)` after 8 and `(8,16)` after 16. For two delivery contracts each requiring 32 of an item, starting inventory 32 completes the first and leaves the second incomplete with inventory 0. It must acquire another 32.','', '## Reward distribution and tail','','Approved rarity totals: Beginner 100%, Intermediate 100%, Advanced 100%, Special 100%. Approved quantity bands: Common 16–48, High-grade 8–24, Rare 2–8, Legendary 1–2. Sampled legendary frequencies appear above; binomial sampling error is approximately 0.14 percentage points at 0.2% and 0.43 points at 5%. A legendary reward worth 48 emerald equivalents per unit in the high sensitivity case can add 48–96 to one contract, versus a 2–20 emerald purchase. No real reward item is asserted to have this value.','','## Limits','','Special trial availability and direct special acquisition probability are not validated; the special scenario is conditional on obtaining a special contract and uses only a vanilla trial chamber target. Structure entry detection, completion times, market values, stock refresh, vendor encounter rate, and actual chosen reward items are UNKNOWN. No MM structure or unregistered item/entity enters any scenario.','']
    OUT.write_text('\n'.join(s))
    return cases

if __name__=='__main__':
    render();print(f'PASS: {len(SCENARIOS)} scenarios x 3 sensitivity cases x {N} samples; {OUT}')
