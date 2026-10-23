"""Builds the catalyst-slot layout comparison sheet from the real-client anvil screenshots.

Usage: accessory_anvil_layout_comparison.py <screenshots-dir> <out-dir>
Rows = candidate layouts (CURRENT is the shipped one, no layout is approved); columns = GUI scales 2/3/4 for the
accessory +4 to +5 screen, then scale-3 +10 maximum and ordinary equipment. Crops are of the centered anvil area;
the original full-resolution screenshots are untouched.
"""
import glob
import os
import sys

from PIL import Image, ImageDraw, ImageFont

shots, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
VARIANTS = ["current", "inline_right", "left_of_input"]
COLUMNS = [("scale 2  accessory +4>+5", "scale-2-accessory-4-to-5"),
           ("scale 3  accessory +4>+5", "scale-3-accessory-4-to-5"),
           ("scale 4  accessory +4>+5", "scale-4-accessory-4-to-5"),
           ("scale 3  +10 maximum", "scale-3-accessory-10-maximum"),
           ("scale 3  ordinary equipment", "scale-3-ordinary-equipment")]
CROP = (400, 150, 1520, 930)   # 1120x780 around the centered panel, tabs and the slot right of the panel
TILE_W = 560
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
except OSError:
    font = ImageFont.load_default()


def find(variant, suffix):
    hits = glob.glob(os.path.join(shots, f"*_anvil-{variant}-{suffix}.png"))
    return hits[0] if hits else None


tile_h = round((CROP[3] - CROP[1]) * TILE_W / (CROP[2] - CROP[0]))
sheet = Image.new("RGB", (len(COLUMNS) * TILE_W, 44 + len(VARIANTS) * (tile_h + 30)), (32, 32, 32))
draw = ImageDraw.Draw(sheet)
draw.text((8, 10), "Anvil catalyst slot layout options - real client, TEST-ONLY catalyst icon, none approved", fill=(255, 255, 0), font=font)
missing = []
for row, variant in enumerate(VARIANTS):
    y0 = 44 + row * (tile_h + 30)
    for col, (title, suffix) in enumerate(COLUMNS):
        draw.text((col * TILE_W + 8, y0 + 4), f"{variant.upper()}  |  {title}", fill=(255, 255, 255), font=font)
        path = find(variant, suffix)
        if not path:
            missing.append(f"{variant}/{suffix}")
            continue
        tile = Image.open(path).convert("RGB").crop(CROP).resize((TILE_W, tile_h), Image.LANCZOS)
        sheet.paste(tile, (col * TILE_W, y0 + 30))
sheet.save(os.path.join(out, "anvil-catalyst-layout-comparison.png"))
print("missing:", missing)
sys.exit(1 if missing else 0)
