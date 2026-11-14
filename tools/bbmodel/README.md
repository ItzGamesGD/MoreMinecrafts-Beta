# Blockbench → MoreMinecrafts entity model pipeline (POC)

`convert_bbmodel.py` turns a Blockbench "free"-format `.bbmodel` (per-face UV, bone rig) into the JSON consumed at runtime by
`client/crystalguardian/BoneModelLoader`, and extracts the embedded texture.

```
python3 tools/bbmodel/convert_bbmodel.py tools/bbmodel/source/Ancient_Crystal_Guardian.bbmodel \
  --model-out src/main/resources/assets/moreminecrafts/entity_models/ancient_crystal_guardian.json \
  --texture-out src/main/resources/assets/moreminecrafts/textures/entity/ancient_crystal_guardian/ancient_crystal_guardian.png
# add --check to verify committed outputs are up to date without writing

python3 tools/bbmodel/validate_converted_model.py <bbmodel> <model.json> <texture.png>   # independent re-check against the source
```

* `source/Ancient_Crystal_Guardian.bbmodel` is the untouched original (sha256 recorded inside the converted JSON).
* Conversion convention (documented in the script header): `x'=-x`, `y'=24-y`, `z'=z`, rotations `(-rx,-ry,rz)`, pivots parent-relative,
  explicit vertices with per-face pixel UVs and outward normals — so mirrored and non-box UV layouts are preserved exactly.
* The runtime builds plain vanilla `ModelPart`s: no GeckoLib or other dependency.
