#!/usr/bin/env python3
"""Builds accessory visual evidence: a labeled texture contact sheet, native-size slot crops cut from the
real-client screenshots, and a missing-texture (magenta) scan. Exits non-zero on any failed check.

usage: accessory_visual_evidence.py <repo-root> <screenshots-dir> <client-log> <out-dir>
"""
import glob
import json
import os
import re
import sys
from collections import Counter

from PIL import Image, ImageDraw, ImageFont

root, shots, log_path, out = sys.argv[1:5]
os.makedirs(os.path.join(out, "crops"), exist_ok=True)
failures = []


def font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


def magenta(px):
    r, g, b = px[:3]
    return r >= 200 and b >= 200 and g <= 60


def magenta_count(img):
    img = img.convert("RGB")
    return sum(1 for px in img.getdata() if magenta(px))


src = open(os.path.join(root, "src/main/java/dev/itzgamesgd/moreminecrafts/accessory/AccessoryDefinition.java")).read()
ids = re.findall(r'(?m)^\s+[A-Z_]+\("([a-z_]+)",\s*"item\.moreminecrafts\.\1"', src)
assert len(ids) == 12, ids
lang = json.load(open(os.path.join(root, "src/main/resources/assets/moreminecrafts/lang/en_us.json")))
names = {i: lang.get("item.moreminecrafts." + i, i) for i in ids}
tex_dir = os.path.join(root, "src/main/resources/assets/moreminecrafts/textures/item")

# ---------------------------------------------------------------- contact sheet
SCALE, PAD, COLS = 8, 24, 4
tile = 32 * SCALE
label_h = 64
rows = (len(ids) + COLS - 1) // COLS
title_h = 70
W = COLS * (tile + PAD) + PAD
H = title_h + rows * (tile + label_h + PAD) + PAD
sheet = Image.new("RGBA", (W, H), (58, 58, 66, 255))
d = ImageDraw.Draw(sheet)
d.text((PAD, 18), "MoreMinecrafts accessory textures - 12 items, native 32x32, nearest-neighbor x8",
       font=font(26), fill=(240, 240, 240, 255))
for n, i in enumerate(ids):
    im = Image.open(os.path.join(tex_dir, i + ".png")).convert("RGBA")
    if im.size != (32, 32):
        failures.append(f"{i}: texture is {im.size}, expected 32x32")
    if magenta_count(im) > 0:
        failures.append(f"{i}: texture contains missing-texture magenta")
    x = PAD + (n % COLS) * (tile + PAD)
    y = title_h + (n // COLS) * (tile + label_h + PAD)
    cell = Image.new("RGBA", (tile, tile), (139, 139, 139, 255))
    cell.alpha_composite(im.resize((tile, tile), Image.NEAREST))
    sheet.alpha_composite(cell, (x, y))
    d.text((x, y + tile + 6), f"{n + 1:02d}  {names[i]}", font=font(22), fill=(255, 255, 255, 255))
    d.text((x, y + tile + 34), f"moreminecrafts:{i}", font=font(15), fill=(190, 190, 200, 255))
sheet.convert("RGB").save(os.path.join(out, "accessory-contact-sheet.png"))

# ------------------------------------------------------------ client log slots
slots = []
for line in open(log_path, errors="replace"):
    m = re.search(r"VISUAL_SLOT name=(\S+) menuSlot=(\d+) scale=(\d+) x=(-?\d+) y=(-?\d+) size=(\d+) item=moreminecrafts:(\w+)", line)
    if m:
        slots.append(dict(name=m[1], menu=int(m[2]), scale=int(m[3]), x=int(m[4]), y=int(m[5]), size=int(m[6]), item=m[7]))
layout_ok = [l.strip() for l in open(log_path, errors="replace") if "VISUAL_LAYOUT_OK" in l]


def shot(name):
    hits = glob.glob(os.path.join(shots, f"*_{name}.png"))
    return hits[0] if hits else None


report = {"layout_assertions_logged": len(layout_ok), "screenshots": {}, "icons": {}}
crops_by_scale = {2: {}, 3: {}, 4: {}}
for s in slots:
    if s["item"] not in ids:
        continue
    path = shot(s["name"])
    if not path:
        failures.append(f"screenshot missing for {s['name']}")
        continue
    img = Image.open(path).convert("RGB")
    crop = img.crop((s["x"], s["y"], s["x"] + s["size"], s["y"] + s["size"]))
    cdir = os.path.join(out, "crops", s["name"])
    os.makedirs(cdir, exist_ok=True)
    crop.save(os.path.join(cdir, f"{s['menu']:02d}_{s['item']}.png"))
    bg = Counter(crop.getdata()).most_common(1)[0][0]
    inked = sum(1 for px in crop.getdata() if sum(abs(a - b) for a, b in zip(px, bg)) > 24)
    mg = magenta_count(crop)
    key = f"{s['name']}/{s['item']}"
    report["icons"][key] = {"inked_fraction": round(inked / (s["size"] ** 2), 3), "magenta_pixels": mg}
    if mg:
        failures.append(f"{key}: missing-texture magenta pixels in rendered slot")
    if inked / (s["size"] ** 2) < 0.12:
        if s["name"].endswith("recipe-open"):
            # The recipe-book panel is drawn over the left part of the inventory (see report); not a texture fault.
            report["icons"][key]["covered_by_recipe_book"] = True
        else:
            failures.append(f"{key}: rendered slot looks empty (inked {inked / (s['size'] ** 2):.2f})")
    if s["name"].endswith("recipe-closed") and s["menu"] >= 9 and s["menu"] <= 20:
        crops_by_scale[s["scale"]][s["item"]] = crop

for scale, crops in crops_by_scale.items():
    missing = [i for i in ids if i not in crops]
    if missing:
        failures.append(f"scale {scale}: icons not rendered in real inventory slots: {missing}")
        continue
    cell = 16 * scale
    cols = 6
    pad = 14
    lab = 40
    sw = cols * (cell + pad) + pad
    sh = 54 + 2 * (cell + lab + pad) + pad
    sh_img = Image.new("RGB", (sw, sh), (58, 58, 66))
    dd = ImageDraw.Draw(sh_img)
    dd.text((pad, 14), f"Real-client inventory slots, GUI scale {scale} ({cell}px per slot icon, no resampling)",
            font=font(20), fill=(240, 240, 240))
    for n, i in enumerate(ids):
        x = pad + (n % cols) * (cell + pad)
        y = 54 + (n // cols) * (cell + lab + pad)
        sh_img.paste(crops[i], (x, y))
        dd.text((x, y + cell + 4), i, font=font(11), fill=(230, 230, 230))
    sh_img.save(os.path.join(out, f"native-size-scale-{scale}.png"))
    big = sh_img.resize((sw * 3, sh * 3), Image.NEAREST)
    big.save(os.path.join(out, f"native-size-scale-{scale}-x3.png"))

for path in sorted(glob.glob(os.path.join(shots, "*.png"))):
    n = magenta_count(Image.open(path))
    report["screenshots"][os.path.basename(path)] = {"magenta_pixels": n}
    if n:
        failures.append(f"{os.path.basename(path)}: {n} magenta pixels in full screenshot")

expected = ["visual-scale-%d-recipe-%s" % (s, st) for s in (2, 3, 4) for st in ("closed", "open")]
expected += ["visual-equipped-round-%d" % r for r in range(6)] + ["visual-backpack-scale-%d" % s for s in (2, 3, 4)]
for e in expected:
    if not shot(e):
        failures.append(f"expected screenshot not produced: {e}")
equipped = {s["item"] for s in slots if s["name"].startswith("visual-equipped-round") and s["menu"] >= 47}
if set(ids) - equipped - {i for i in ids}:
    pass
missing_equipped = [i for i in ids if i not in equipped]
if missing_equipped:
    failures.append(f"never shown in an equipped accessory slot: {missing_equipped}")
if len(layout_ok) < 12:
    failures.append(f"only {len(layout_ok)} VISUAL_LAYOUT_OK assertions logged (expected >= 12)")

report["failures"] = failures
json.dump(report, open(os.path.join(out, "report.json"), "w"), indent=2)
print(json.dumps({"failures": failures, "layout_ok": len(layout_ok), "icons_checked": len(report["icons"])}, indent=2))
sys.exit(1 if failures else 0)
