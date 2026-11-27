#!/usr/bin/env python3
"""C-4 static route check on the six committed composite tower.nbt files. Not Minecraft physics:
collision shapes are approximated (stairs/slabs are full steps, doors/ladders passable). It proves
topological continuity and headroom/support of a route, never a player walkthrough.
Usage: python3 -I phase_c4_walkability.py [out.json]"""
import gzip, io, json, re, sys, collections
from pathlib import Path
import nbtlib
ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "src/main/resources/data/moreminecrafts/structure/sky_islands"
STYLES = ["snowy", "meadow", "forest", "badlands", "desert", "rocky"]
FLOORS = [10, 21, 33, 47, 64]
OFF = (0, 17, 0)                        # approved tower local -> composite
CONNECTOR = (36, 16, 24)                # island connector in composite space (C-3 report)
PASS_NAMES = {"air", "cave_air", "void_air", "structure_void", "short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "snow", "vine",
              "ladder", "cobweb", "water", "light", "sweet_berry_bush", "pink_petals", "moss_carpet", "leaf_litter", "seagrass", "tall_seagrass"}
PASS_SUFFIX = ("_torch", "_wall_torch", "_button", "_sign", "_wall_sign", "_hanging_sign", "_door", "_carpet", "_rail", "_pressure_plate",
               "_sapling", "_mushroom", "_tulip", "_orchid", "_bluet", "_candle")
PASS_FLOWERS = {"dandelion", "poppy", "allium", "azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "lilac", "rose_bush",
                "peony", "sunflower", "torch", "lever", "rail", "tripwire", "tripwire_hook", "wither_rose"}
NOSTAND = re.compile(r"(_wall$|_fence$|_fence_gate$|lava|water|barrier|iron_bars|glass_pane|chain)")
def load(style):
    f = nbtlib.File.parse(io.BytesIO(gzip.decompress((DATA / style / "tower.nbt").read_bytes())))
    pal = [(s["id"], dict(s.get("properties", {}))) for s in f["palette"]]
    size = tuple(int(v) for v in f["size"]); g = {}
    for b in f["blocks"]:
        x, y, z = (int(v) for v in b["pos"]); g[(x, y, z)] = pal[int(b["state"])][0]
    return size, g
def passable(g, p):
    b = g.get(p, "minecraft:air").replace("minecraft:", "")
    if b.endswith(("_trapdoor", "_fence", "_fence_gate", "_wall")): return False
    return b in PASS_NAMES or b in PASS_FLOWERS or b.endswith(PASS_SUFFIX)
def standable(g, p):
    b = g.get(p, "minecraft:air")
    return not passable(g, p) and not NOSTAND.search(b)
def node_ok(g, p):
    x, y, z = p
    return standable(g, (x, y - 1, z)) and passable(g, (x, y, z)) and passable(g, (x, y + 1, z))
def neighbors(g, p, size):
    x, y, z = p
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, nz = x + dx, z + dz
        if not (0 <= nx < size[0] and 0 <= nz < size[2]): continue
        for dy in (0, 1, -1, -2, -3):
            q = (nx, y + dy, nz)
            if not node_ok(g, q): continue
            if dy == 1 and not passable(g, (x, y + 2, z)): continue          # headroom to jump
            if dy < 0 and not all(passable(g, (nx, y + k, nz)) for k in range(dy, 2)): continue
            yield q
            break
def bfs(g, size, starts):
    seen = set(starts); dq = collections.deque(starts)
    while dq:
        p = dq.popleft()
        for q in neighbors(g, p, size):
            if q not in seen: seen.add(q); dq.append(q)
    return seen
def run(style, barriers_active):
    size, g = load(style)
    wall = {}
    if barriers_active:
        for fl in range(4):
            for c in range(18): wall[(39 + c // 3, FLOORS[fl] + 1 + c % 3 + OFF[1], 27)] = "minecraft:barrier"
    gg = dict(g); gg.update(wall)
    starts = [(x, y, z) for x in range(CONNECTOR[0] - 6, CONNECTOR[0] + 7) for z in range(CONNECTOR[2] - 6, CONNECTOR[2] + 7)
              for y in range(CONNECTOR[1] - 3, CONNECTOR[1] + 4) if node_ok(gg, (x, y, z))]
    seen = bfs(gg, size, starts)
    res = {"style": style, "size": list(size), "barriers": barriers_active, "start_nodes": len(starts), "reachable_nodes": len(seen)}
    for fl in range(5):
        ey = FLOORS[fl] + 1 + OFF[1]
        near = [p for p in seen if p[1] == ey and abs(p[0] - 38) <= 8 and abs(p[2] - 24) <= 8]
        interior = [p for p in seen if p[1] == ey and 15 <= p[0] <= 33 and 15 <= p[2] <= 33]
        res[f"floor{fl}_entry_reachable"] = bool(near); res[f"floor{fl}_interior_nodes"] = len(interior)
    res["spawner_adjacent_reachable"] = sum(1 for fl in range(4) for sx in (17, 31) for sz in (17, 31)
        if any((sx + dx, FLOORS[fl] + 2 + OFF[1] - 1, sz + dz) in seen for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2)))
    res["top_y_reached"] = max((p[1] for p in seen), default=None)
    return res
if __name__ == "__main__":
    out = [run(s, b) for s in STYLES for b in (False, True)]
    for r in out: print(json.dumps(r))
    if len(sys.argv) > 1: Path(sys.argv[1]).write_text(json.dumps(out, indent=1) + "\n")
