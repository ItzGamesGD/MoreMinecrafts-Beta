#!/usr/bin/env python3
"""Labeled recipe-book OPEN vs CLOSED comparison for GUI scales 2/3/4, cut from the real-client screenshots.

usage: accessory_ui_contact_sheet.py <screenshots-dir> <out-dir>
Writes ui-contact-sheet.png (full frames at 50%) and ui-contact-sheet-zoom.png (native-pixel crops of the GUI area).
Exits non-zero when an expected screenshot is missing.
"""
import glob
import os
import sys

from PIL import Image, ImageDraw, ImageFont

shots, out = sys.argv[1:3]
os.makedirs(out, exist_ok=True)


def font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


def find(name):
    hits = glob.glob(os.path.join(shots, f"*_{name}.png"))
    return hits[0] if hits else None


missing = []
cells = []
for scale in (2, 3, 4):
    for state in ("closed", "open"):
        path = find(f"ui-scale-{scale}-recipe-{state}")
        if not path:
            missing.append(f"ui-scale-{scale}-recipe-{state}")
        cells.append((scale, state, path))

W, H, PAD, LAB = 960, 540, 16, 40
sheet = Image.new("RGB", (2 * W + 3 * PAD, 3 * (H + LAB + PAD) + PAD + 50), (40, 40, 46))
d = ImageDraw.Draw(sheet)
d.text((PAD, 12), "Accessory inventory - recipe book CLOSED (left) vs OPEN (right), GUI scales 2 / 3 / 4 (real client, 1920x1080, 50%)",
       font=font(22), fill=(240, 240, 240))
zoom = Image.new("RGB", (2 * 1000 + 3 * PAD, 3 * (700 + LAB + PAD) + PAD + 50), (40, 40, 46))
dz = ImageDraw.Draw(zoom)
dz.text((PAD, 12), "Native-pixel crops of the inventory area (no resampling), scales 2 / 3 / 4, CLOSED left / OPEN right",
        font=font(22), fill=(240, 240, 240))
for i, (scale, state, path) in enumerate(cells):
    row, col = divmod(i, 2)
    x = PAD + col * (W + PAD)
    y = 50 + row * (H + LAB + PAD)
    label = f"GUI scale {scale} - recipe book {state.upper()}"
    d.text((x, y), label, font=font(20), fill=(255, 255, 255))
    zx = PAD + col * (1000 + PAD)
    zy = 50 + row * (700 + LAB + PAD)
    dz.text((zx, zy), label, font=font(20), fill=(255, 255, 255))
    if path:
        img = Image.open(path).convert("RGB")
        sheet.paste(img.resize((W, H), Image.LANCZOS), (x, y + LAB))
        zoom.paste(img.crop((460, 190, 1460, 890)), (zx, zy + LAB))
sheet.save(os.path.join(out, "ui-contact-sheet.png"))
zoom.save(os.path.join(out, "ui-contact-sheet-zoom.png"))
if missing:
    print("MISSING:", missing)
    sys.exit(1)
print("contact sheets written")
