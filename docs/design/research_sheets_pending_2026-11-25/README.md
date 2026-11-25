# Research sheet scenes — pending owner approval (2026-11-25)

Generated layout and background sheets for the six remaining Research disciplines
(ecology, world, combat, living, alchemy, production). Presentation only; no gameplay data changes.

These files are deliberately **outside** `src/main/resources`. The runtime loader reads any sheet placed there,
so they are activated only after owner approval, when they move into
`src/main/resources/assets/moreminecrafts/progression/research_sheets/` and
`src/main/resources/assets/moreminecrafts/textures/gui/research_tree/sheets/`.

- `layout/<discipline>.json`: scene format identical to the approved Resources scene (slots, zones, banners, routed edges).
- `sheets/sheet_research_<discipline>.png`: background sheet (no nodes, banners or text).
- `previews/preview_research_<discipline>.png`: composite preview with the approved kit. Node states are a sample
  (roots owned, nodes with all prerequisites owned available, rest locked). Real states come from the client capture.

Layout rule: one horizontal zone per branch; one row per node; columns follow prerequisite depth; connectors jog in
the empty gap just right of the parent. Every drawn connector is a real prerequisite and crosses no node.
