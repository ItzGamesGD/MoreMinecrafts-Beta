#!/usr/bin/env python3
"""Static validation of the generated probe pack (no Minecraft needed). Run: python3 -I tools/worldgen-probe/selftest.py"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import nbt  # noqa: E402

BUILD = os.path.join(HERE, "build_probe_pack.py")


def read(path):
    with open(path, "rb") as f:
        return f.read()


def build(out, **kw):
    args = [sys.executable, "-I", BUILD, out]
    for k, v in kw.items():
        args += [f"--{k}", v]
    subprocess.run(args, check=True)


def load_template(root, name):
    t = nbt.to_py(nbt.loads(read(os.path.join(root, "data/moreminecrafts/structure/probe", name + ".nbt"))))
    pal = t["palette"]
    blocks = {tuple(b["pos"]): (pal[b["state"]], b.get("nbt")) for b in t["blocks"]}
    return t["size"], blocks


class ProbePackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.built = os.path.join(cls.tmp.name, "built")
        cls.bare = os.path.join(cls.tmp.name, "bare")
        build(cls.built, adaptation="encapsulate", shell="built")
        build(cls.bare, adaptation="none", shell="bare")

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def jigsaws(self, blocks):
        return {pos: nbtag for pos, (st, nbtag) in blocks.items() if nbtag and nbtag.get("id") == "minecraft:jigsaw"}

    def test_pool_templates_exist_and_targets_resolve(self):
        root = self.built
        pools = {}
        pdir = os.path.join(root, "data/moreminecrafts/worldgen/template_pool/probe")
        for f in os.listdir(pdir):
            pools[f"moreminecrafts:probe/{f[:-5]}"] = [e["element"]["location"] for e in json.loads(read(os.path.join(pdir, f)))["elements"]]
        names_by_template = {}
        for pool, locs in pools.items():
            for loc in locs:
                short = loc.split("probe/")[1]
                size, blocks = load_template(root, short)
                names_by_template[loc] = {j["name"] for j in self.jigsaws(blocks).values()}
        for tpl in ("central_chamber", "corridor_a", "corridor_b", "combat_room", "end_cap"):
            _, blocks = load_template(root, tpl)
            for pos, j in self.jigsaws(blocks).items():
                pool, target = j["pool"], j["target"]
                if pool == "minecraft:empty":
                    continue
                if pool.startswith("minecraft:"):
                    continue  # vanilla pool: resolved by the game; generation success proves it
                self.assertIn(pool, pools, f"{tpl}{pos} references missing pool {pool}")
                self.assertTrue(any(target in names_by_template[loc] for loc in pools[pool]),
                                f"{tpl}{pos}: no template in {pool} has a jigsaw named {target}")

    def test_doorway_cross_sections_match(self):
        sizes = {t: load_template(self.built, t)[0] for t in ("central_chamber", "corridor_a", "combat_room", "end_cap")}
        # every door is 3 wide x 4 high and centred on the template's x axis; corridor interior is 3x4
        for t, (sx, sy, sz) in sizes.items():
            self.assertEqual(sx % 2, 1, f"{t} width must be odd so the door is centred")
        csx, csy, _ = sizes["corridor_a"]
        self.assertEqual((csx - 2, csy - 2), (3, 4))

    def test_built_shell_is_closed_except_doors(self):
        for tpl in ("central_chamber", "corridor_a", "corridor_b", "combat_room", "end_cap"):
            (sx, sy, sz), blocks = load_template(self.built, tpl)
            holes = [pos for pos, (st, _) in blocks.items()
                     if (pos[0] in (0, sx - 1) or pos[1] in (0, sy - 1) or pos[2] in (0, sz - 1))
                     and st["id"] in ("minecraft:air", "minecraft:jigsaw")]
            for x, y, z in holes:
                self.assertIn(z, (0, sz - 1), f"{tpl}: opening at {(x, y, z)} is not on a door face")
                self.assertTrue(1 <= y <= 4, f"{tpl}: opening at {(x, y, z)} outside the 3x4 door")

    def test_bare_has_no_shell_blocks(self):
        for tpl in ("central_chamber", "combat_room"):
            _, blocks = load_template(self.bare, tpl)
            self.assertFalse(any(st["id"] == "minecraft:tuff_bricks" for st, _ in blocks.values()))

    def test_variants_differ_only_in_terrain_adaptation(self):
        with tempfile.TemporaryDirectory() as d:
            a, b = os.path.join(d, "a"), os.path.join(d, "b")
            build(a, adaptation="encapsulate")
            build(b, adaptation="none")
            for dp, _, fs in os.walk(a):
                for f in fs:
                    pa = os.path.join(dp, f)
                    pb = os.path.join(b, os.path.relpath(pa, a))
                    ba, bb = read(pa), read(pb)
                    if f == "probe_kingdom.json" and "structure_set" not in pa:
                        ja, jb = json.loads(ba), json.loads(bb)
                        self.assertEqual(ja.pop("terrain_adaptation"), "encapsulate")
                        self.assertEqual(jb.pop("terrain_adaptation"), "none")
                        self.assertEqual(ja, jb)
                    else:
                        self.assertEqual(ba, bb, pa)

    def test_byte_reproducible(self):
        with tempfile.TemporaryDirectory() as d:
            a, b = os.path.join(d, "a"), os.path.join(d, "b")
            build(a, adaptation="encapsulate")
            build(b, adaptation="encapsulate")
            for dp, _, fs in os.walk(a):
                for f in fs:
                    pa = os.path.join(dp, f)
                    self.assertEqual(read(pa), read(os.path.join(b, os.path.relpath(pa, a))))


if __name__ == "__main__":
    unittest.main(verbosity=2)
