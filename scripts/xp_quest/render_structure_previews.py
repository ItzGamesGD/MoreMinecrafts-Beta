#!/usr/bin/env python3
"""Offline design previews of the 200-quest structure proposal.

Reads the proposal mapping and the layout written by QuestTreeStressTest
(build/xp-quest-structure/real-layout.json), so node positions come from the real layout code.
These are OFFLINE previews, not native Minecraft captures.
"""
import json
import pathlib
from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
STRUCTURE = ROOT / "docs/design/xp-quest/structure"
LAYOUT = ROOT / "build/xp-quest-structure/real-layout.json"
OUT = STRUCTURE / "previews"
FONT = "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"

BG = (30, 28, 25)
LANE = (44, 40, 34)
GOLD = (214, 178, 84)
GREY = (120, 112, 98)
TEXT = (232, 226, 210)
DIM = (150, 144, 132)
EDGE = (226, 196, 110)
ELEMENT_EDGE = (110, 102, 88)
STATUS_STYLE = {"NOW": "solid", "VERIFY": "ring", "FUTURE": "dim"}


def font(size):
    return ImageFont.truetype(FONT, size)


def fit(d, text, f, maxw):
    if d.textlength(text, font=f) <= maxw:
        return text
    while text and d.textlength(text + "…", font=f) > maxw:
        text = text[:-1]
    return text + "…"


def load():
    mapping = json.loads((STRUCTURE / "quest_structure_mapping.json").read_text(encoding="utf-8"))
    layout = json.loads(LAYOUT.read_text(encoding="utf-8"))
    by_id = {q["quest_id"]: q for q in mapping["quests"]}
    chapters = {c["id"]: c for c in mapping["chapters"]}
    sub_titles = {s["id"]: s["title"] for c in mapping["chapters"] for s in c["sub_branches"]}
    edges = json.loads((STRUCTURE / "edge_ledger.json").read_text(encoding="utf-8"))["edges"]
    return mapping, layout, by_id, chapters, sub_titles, edges


def draw_node(d, x, y, status, registered, scale):
    s = 13 * scale
    fill = (176, 150, 80) if status == "NOW" else (90, 84, 74)
    if status == "FUTURE":
        fill = (62, 58, 52)
    d.rectangle([x - s, y - s, x + s, y + s], fill=fill, outline=(20, 18, 15), width=max(1, int(scale)))
    if status == "VERIFY":
        d.rectangle([x - s - 2, y - s - 2, x + s + 2, y + s + 2], outline=(200, 160, 60), width=max(1, int(scale)))
    if registered:
        d.rectangle([x - 3 * scale, y - 3 * scale, x + 3 * scale, y + 3 * scale], fill=(255, 226, 140))


def render(lanes, nodes, edges_drawn, by_id, chapters, sub_titles, scale, crop=None, labels=True,
           lane_filter=None, title=None, path=None):
    width, height = 684, 3116
    if crop:
        cx0, cy0, cx1, cy1 = crop
    else:
        cx0, cy0, cx1, cy1 = 0, 0, width, height
    header = 26 if title else 0
    img = Image.new("RGB", (int((cx1 - cx0) * scale) + 1, int((cy1 - cy0) * scale) + 1 + header), BG)
    d = ImageDraw.Draw(img)
    fx = font(max(10, int(14 * scale)))
    fs = font(max(8, int(11 * scale)))

    def sx(v):
        return int((v - cx0) * scale)

    def sy(v):
        return int((v - cy0) * scale) + header

    for lane in lanes:
        if lane_filter and lane["chapter"] not in lane_filter:
            continue
        top, bottom = lane["top"], lane["top"] + lane["height"]
        if bottom < cy0 or top > cy1:
            continue
        d.rectangle([sx(0), sy(top), sx(width), sy(bottom)], fill=LANE)
        d.rectangle([sx(0), sy(top), sx(128), sy(bottom)], fill=(24, 21, 18))
        d.rectangle([sx(127), sy(top), sx(128), sy(bottom)], fill=GOLD)
        if labels:
            ch = chapters[lane["chapter"]]
            maxw = int(116 * scale)
            d.text((sx(10), sy(top + 10)), fit(d, ch["title"]["en"], fx, maxw), font=fx, fill=GOLD)
            d.text((sx(10), sy(top + 10 + int(18 * scale) + 4)), fit(d, ch["title"]["ko"], fx, maxw), font=fx, fill=TEXT)
            d.text((sx(10), sy(top + 10 + int(40 * scale) + 6)), f'{lane["count"]} quests', font=fs, fill=DIM)

    for parent, child, kind in edges_drawn:
        p, c = nodes.get(parent), nodes.get(child)
        if not p or not c:
            continue
        color = EDGE if kind == "suggested_progression" else (120, 170, 200)
        mx = (p["x"] + c["x"]) / 2
        d.line([(sx(p["x"]), sy(p["y"])), (sx(mx), sy(p["y"])), (sx(mx), sy(c["y"])), (sx(c["x"]), sy(c["y"]))],
               fill=color, width=max(1, int(scale)))

    seen_subs = set()
    for n in sorted(nodes.values(), key=lambda v: v["order"]):
        if cx0 - 30 <= n["x"] <= cx1 + 30 and cy0 - 30 <= n["y"] <= cy1 + 30:
            q = by_id[n["id"]]
            draw_node(d, sx(n["x"]), sy(n["y"]), q["status"], q["registered"], scale)
        if labels and n["sub"] not in seen_subs and lane_filter is not None and cx0 <= n["x"] <= cx1:
            seen_subs.add(n["sub"])
            cap_y = sy(n["y"] - 34)
            if cap_y >= header + 4:
                d.text((sx(n["x"] - 26), cap_y), fit(d, sub_titles[n["sub"]]["en"], fs, int(150 * scale)), font=fs, fill=DIM)
    if title:
        d.text((8, 6), title, font=font(max(11, int(13 * scale))), fill=TEXT)
        d.line([(0, header - 1), (img.width, header - 1)], fill=(70, 64, 54))
    d.text((8, img.height - int(16 * scale)),
           "OFFLINE PREVIEW (not native). Square: NOW. Ringed: VERIFY. Dim: FUTURE. Bright centre: registered.",
           font=fs, fill=DIM)
    img.save(path)
    return img.size


def main():
    mapping, layout, by_id, chapters, sub_titles, edges = load()
    OUT.mkdir(parents=True, exist_ok=True)
    order = {q["quest_id"]: q["presentation_order"] for q in mapping["quests"]}
    subs = {q["quest_id"]: q["sub_branch"] for q in mapping["quests"]}
    nodes = {n["id"]: {"id": n["id"], "x": n["x"], "y": n["y"], "chapter": n["chapter"],
                       "sub": subs[n["id"]], "order": order[n["id"]]} for n in layout["nodes"]}
    lanes = layout["lanes"]
    drawn = [(e["parent"], e["child"], e["kind"]) for e in edges if e["enforcement"] == "presentation-only"]

    sizes = {}
    sizes["overview"] = render(lanes, nodes, drawn, by_id, chapters, sub_titles, 1.0,
                               title="200-quest structure: all 15 chapters (offline, layout from the stress test)",
                               path=OUT / "01-overview-all-chapters.png")
    exploration = [ln for ln in lanes if ln["chapter"] == "exploration"]
    ex = exploration[0]
    sizes["dense"] = render(lanes, nodes, drawn, by_id, chapters, sub_titles, 1.4,
                            crop=(0, ex["top"] - 10, 684, ex["top"] + ex["height"] + 10),
                            lane_filter={"exploration"}, title="Densest chapter: Exploration, 24 quests",
                            path=OUT / "02-dense-exploration.png")
    eco = [ln for ln in lanes if ln["chapter"] == "ecology"][0]
    sizes["sparse"] = render(lanes, nodes, drawn, by_id, chapters, sub_titles, 1.6,
                             crop=(0, eco["top"] - 10, 684, eco["top"] + eco["height"] + 10),
                             lane_filter={"ecology"}, title="Sparsest chapter: Ecology slice, 3 quests (preserved)",
                             path=OUT / "03-sparse-ecology.png")
    cross_lanes = {"crafting", "exploration"}
    tops = [ln["top"] for ln in lanes if ln["chapter"] in cross_lanes]
    bottoms = [ln["top"] + ln["height"] for ln in lanes if ln["chapter"] in cross_lanes]
    sizes["cross"] = render(lanes, nodes, drawn, by_id, chapters, sub_titles, 1.2,
                            crop=(0, min(tops) - 10, 684, max(bottoms) + 10),
                            lane_filter=cross_lanes,
                            title="Cross-chapter presentation link: Crafting parachute to Exploration parachute landing",
                            path=OUT / "04-cross-chapter-links.png")
    zoom_id = "xpq.cmb.bestiary_25"
    zn = nodes[zoom_id]
    sizes["zoom"] = render(lanes, nodes, drawn, by_id, chapters, sub_titles, 3.0,
                           crop=(zn["x"] - 90, zn["y"] - 70, zn["x"] + 150, zn["y"] + 110),
                           lane_filter={"ecology"}, title="Zoomed-in node (3x): Bestiary 25 with its position in the lane",
                           path=OUT / "05-zoomed-node.png")
    print(json.dumps({k: list(v) for k, v in sizes.items()}))


if __name__ == "__main__":
    main()
