import hashlib
import json
import sys
import unittest

HERE = __import__("pathlib").Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import tower_composite as composite
import generate as generator

STYLES = ("snowy", "meadow", "forest", "badlands", "desert", "rocky")


class TowerCompositeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.approved = composite.TOWER.read_bytes()
        # Rebuild the uncomposited island source in memory; the checked-in tower.nbt
        # files are the generated outputs and must not be fed back into compose().
        cls.source = {style: generator.build_style_islands(style)["tower"].to_nbt()
                      for style in STYLES}
        c2_path = composite.ROOT / "docs/planning/sky_tower_integration_02/phase_c2_candidates.json"
        cls.candidates = json.loads(c2_path.read_text())["candidates"]
        cls.outputs = {}
        for style in STYLES:
            raw, details = composite.compose(style, cls.source[style], cls.approved, composite.load_candidate(style))
            cls.outputs[style] = raw, details, composite.load(raw)

    def test_approved_template_is_read_only_and_matches_approval_hash(self):
        self.assertEqual(composite.APPROVED_SHA256, hashlib.sha256(self.approved).hexdigest())
        self.assertEqual(self.approved, composite.TOWER.read_bytes())

    def test_all_six_composites_are_deterministic_and_preserve_weighted_root_connector(self):
        for style in STYLES:
            with self.subTest(style=style):
                first, details, nbt = self.outputs[style]
                second, _ = composite.compose(style, self.source[style], self.approved, composite.load_candidate(style))
                self.assertEqual(first, second)
                self.assertEqual(tuple(map(int, nbt["size"])), composite.COMPOSITE_SIZE)
                rows = composite.block_rows(nbt)
                jigsaws = [(pos, nbt_data) for pos, (state, nbt_data) in rows.items()
                           if str(state["id"]) == "minecraft:jigsaw"]
                self.assertEqual(len(jigsaws), 1)
                self.assertEqual(jigsaws[0][0], (36, 16, 24))
                self.assertEqual(str(jigsaws[0][1]["pool"]), "minecraft:empty")
                self.assertTrue(details["terminated_legacy_socket"])
                self.assertEqual(details["tower_origin_from_piece"], (0, 17, 0))
                self.assertEqual(details["tower_bounds_from_connector"], [[-36, 1, -24], [13, 80, 24]])
                self.assertTrue(all(0 <= pos[i] < composite.COMPOSITE_SIZE[i] for pos in rows for i in range(3)))
                pool = json.loads((composite.DATA / f"worldgen/template_pool/sky_islands/{style}/start.json").read_text())
                selected = {entry["element"]["location"].rsplit("/", 1)[-1]: entry["weight"] for entry in pool["elements"]}
                self.assertEqual(selected["tower"], 3)

    def test_composite_preserves_all_approved_trial_spawners_and_vaults(self):
        source_tower = composite.block_rows(composite.load(self.approved))
        expected = {name: {(pos[0], pos[1] + 17, pos[2]): value
                           for pos, value in source_tower.items() if str(value[0]["id"]) == name}
                    for name in ("minecraft:trial_spawner", "minecraft:vault")}
        for style in STYLES:
            with self.subTest(style=style):
                _, _, nbt = self.outputs[style]
                rows = composite.block_rows(nbt)
                for block, anchors in expected.items():
                    got = {pos: value for pos, value in rows.items() if str(value[0]["id"]) == block}
                    self.assertEqual(got, anchors)
                self.assertEqual(len(expected["minecraft:trial_spawner"]), 16)
                self.assertEqual(len(expected["minecraft:vault"]), 8)

    def test_existing_candidate_collision_counts_match_independent_c2_dataset(self):
        for style in STYLES:
            with self.subTest(style=style):
                c2 = next(row for row in self.candidates if row["id"] == f"{style}_dx-12_dz0_r0_w3")
                _, details, _ = self.outputs[style]
                self.assertEqual(details["tower_overwrite_voxel_count"], len(c2["tower_overwrites_parent"]))

    def test_start_pool_weights_and_generation_policies_remain_unchanged(self):
        expected = {"plain_small_a": 3, "plain_small_b": 3, "plain_medium": 4, "ruins": 6, "tower": 3, "temple": 2}
        for style in STYLES:
            pool = json.loads((composite.DATA / f"worldgen/template_pool/sky_islands/{style}/start.json").read_text())
            weights = {entry["element"]["location"].rsplit("/", 1)[-1]: entry["weight"] for entry in pool["elements"]}
            self.assertEqual(weights, expected)


if __name__ == "__main__":
    unittest.main()
