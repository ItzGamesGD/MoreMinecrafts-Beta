# bbmodel -> pose_mesh bake scripts

Offline scripts that bake a Blockbench `.bbmodel` into `assets/moreminecrafts/bbmodel/pose/<id>.json`
(quads: `v` item-space vertices, `uv`, `n`), rendered by the `moreminecrafts:pose_mesh` special item model.
Each script maps the model into the vanilla handheld item frame so the vanilla `item/handheld` display
(copied from the Minecraft client jar) can be used unchanged.

Requires Python 3 with numpy. Run with `python3 -I`.

- `bake_sword_frames.py <bbmodel> <anim|-> <frames> <grip name prefix> <out.json>`
  Swords. Blade axis -> sprite diagonal (1,1,0)/sqrt2, grip centre -> sprite pixel (2.5,12.5), blade length scaled to 17 px.
  `-` as animation = static pose (key ""); otherwise keys f0..f<N-1>.
- `bake_bow_poses.py <bbmodel> <out.json>`
  Bow pulling_0..2 poses into the vanilla bow sprite frame (chord (12,1)->(2,15), handle pixel (3.5,8.5)).
- `bake_tool.py <bbmodel> <grip name keys, comma separated> <out.json> [max_scale] [tip_dir "x,y,z"] [gui_rotation_out.json]`
  Generic tool/gadget. Grip centroid -> sprite handle pixel, far end along the principal axis = tip.
  Optional `tip_dir` sets the item-space direction of the tip (default: sprite diagonal); with `gui_rotation_out` it writes the
  GUI display rotation that restores the diagonal icon orientation.

Inputs (the bbmodels) are not stored in the repo.
