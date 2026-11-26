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

Layout rule (revision 2026-11-25, horizontal): one horizontal lane per branch; columns follow prerequisite depth;
children of a discipline root sit in one row and connect along a bus line above the lane; other connectors jog in the
empty gap just right of the parent. Nodes are 64 GUI px (`nodeGuiSize`), titles are centred in each lane banner.

Known limitation: a few prerequisites cross lanes (for example a mob-hunt node that needs a node from another branch).
Those connectors take longer routes and may pass close to nodes. This needs owner review of the cross-lane
prerequisites or a layout exception before activation.
