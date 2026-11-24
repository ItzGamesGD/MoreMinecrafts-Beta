"""Resource estimates per vegetation species and per variant (ESTIMATES).

Assumed implementation for counting (see README for alternatives):
  * giant flowers  : hybrid = NBT multiblock structure assembled from a shared stem
                     block + shared giant-leaf block + per-form petal/centre blocks
                     whose block models carry sub-block (1/16) elements.
  * medium flowers : two-block-tall plant blocks (lower/upper half), cross/element models.
  * small flowers  : one cross-model plant block per colour (vanilla flower pattern).
  * trees          : NBT tree structures; leaves/logs reuse vanilla where it fits.
  * grass/bushes   : heaven short/tall grass (fixed colour, fixes the tint mismatch).
  * vines          : vanilla vine pattern (multiface) / weeping-vine pattern (head+body).
  * ground         : vanilla pink_petals reused; big-leaf plant is a new model block.
Counts are unique resources; shared parts are counted once per variant."""
import collections, numpy as np
import veg_models as vm

# resource units: (blocks, textures, models, nbt_structures)
SHARED = {
    'giant_stem':  (1, 2, 3, 0),   # straight / bent / joint models; side+top textures
    'giant_leaf':  (1, 1, 3, 0),   # base / mid / tip leaf models
}
SPECIES = {
    # giants: petal_out, petal_in(or accent), centre/stamen block; per-form NBT
    'giant:tulip':     dict(r=(3, 3, 5, 1), needs=['giant_stem', 'giant_leaf'], note='outer/inner petal + base'),
    'giant:lily':      dict(r=(4, 4, 6, 1), needs=['giant_stem', 'giant_leaf'], note='petal in/out, spotted petal, filament+anther'),
    'giant:daffodil':  dict(r=(3, 3, 6, 1), needs=['giant_stem', 'giant_leaf'], note='perianth, corona, rim'),
    'giant:peony':     dict(r=(3, 4, 6, 1), needs=['giant_stem', 'giant_leaf'], note='3 petal tones (ruffle models), sepal'),
    'giant:starlotus': dict(r=(4, 5, 7, 1), needs=['giant_stem', 'giant_leaf'], note='gradient petal x2, core, crystal, bud'),
    'medium:lavender':      dict(r=(1, 2, 2, 0)),
    'medium:bells':         dict(r=(1, 2, 2, 0)),
    'medium:phlox':         dict(r=(1, 2, 2, 0), needs=['cluster_base']),
    'medium:yellowcluster': dict(r=(1, 1, 0, 0), needs=['cluster_base'], note='recolour of phlox model'),
    'medium:hydrangea':     dict(r=(1, 2, 2, 0)),
    'tree:heaven':  dict(r=(1, 1, 1, 2), note='heaven leaves new; vanilla oak log; 2 size NBTs'),
    'tree:blossom': dict(r=(0, 0, 0, 2), note='vanilla cherry_leaves + log reused'),
    'tree:white':   dict(r=(1, 1, 1, 2), note='white blossom leaves new'),
    'grass:tuft_short': dict(r=(1, 1, 1, 0), note='heaven short grass (fixed colour)'),
    'grass:tuft_tall':  dict(r=(1, 2, 2, 0), note='heaven tall grass (double block)'),
    'bush:leafbush':   dict(r=(0, 0, 0, 0), note='vanilla azalea/leaves reused'),
    'bush:flowerbush': dict(r=(1, 2, 1, 0)),
    'ground:petals':   dict(r=(0, 0, 0, 0), note='vanilla pink_petals reused'),
    'ground:bigleaf':  dict(r=(1, 1, 1, 0)),
    'vine:vine':       dict(r=(1, 1, 1, 0), note='heaven vine (vanilla vine is foliage-tinted)'),
    'vine:wisteria':   dict(r=(2, 2, 2, 0), note='head + body blocks'),
    'vine:wisteria_canopy': dict(r=(0, 0, 0, 0), needs=['wisteria_block']),
}
SHARED.update({'cluster_base': (0, 1, 1, 0), 'wisteria_block': (2, 2, 2, 0)})
for c in vm.SMALL_COLORS:
    SPECIES['small:small_' + c[3:]] = dict(r=(1, 1, 1, 0))


def variant_cost(counts):
    """counts: {'cat:species': n} -> per-category totals and grand total."""
    used = [k for k, v in counts.items() if v > 0]
    shared = set(); per = collections.defaultdict(lambda: np.zeros(4, int))
    for k in used:
        sp = SPECIES.get(k)
        if sp is None:
            continue
        if k == 'vine:wisteria_canopy' and 'vine:wisteria' in used:
            pass
        else:
            per[k.split(':')[0]] += np.array(sp['r'])
        for n in sp.get('needs', []):
            if n == 'wisteria_block' and 'vine:wisteria' in used:
                continue
            if n not in shared:
                shared.add(n); per[k.split(':')[0]] += np.array(SHARED[n])
    tot = sum(per.values(), np.zeros(4, int))
    return {k: v.tolist() for k, v in per.items()}, tot.tolist()


def exposed_faces(M):
    s = set(M.keys()); n = 0
    for (x, y, z) in s:
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            if (x + d[0], y + d[1], z + d[2]) not in s:
                n += 1
    return n
