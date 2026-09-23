# Stage 6 material implementation

Stage 6 uses only native blocks, tags, loot tables, recipes, configured features, and placed features.
`adamantium_debris` was selected over `adamantium_ore`: it drops itself without Fortune and uses an
air-discarding Nether placement, preserving Ancient-Debris-like semantics without copying its code.

Titanium is iron-tier mineable. Tungsten, Mythril, Etherium, and Adamantium require diamond-tier
pickaxes; this makes Tungsten the transition material and prevents the rare families from being
harvested early. Mythril and Etherium use `DropExperienceBlock` with diamond's 3-7 XP range. Their
loot remains direct-drop and Fortune-aware, while Titanium and Tungsten use raw materials.

The adjustable placement values are summarized by `MaterialProgression`; corresponding data-pack
JSON remains authoritative at runtime. Titanium/Tungsten use frequent iron-like veins, Mythril and
Etherium use one six-block attempt per chunk, and Adamantium uses one three-block, air-discarded
Nether attempt. Five reversible 9:1 storage-block recipes provide a vanilla-native material sink
without prematurely defining equipment recipes. All gameplay checks remain LIVE PENDING.
