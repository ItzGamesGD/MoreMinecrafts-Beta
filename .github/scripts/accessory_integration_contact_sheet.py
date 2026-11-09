"""Builds a labeled contact sheet of every native screenshot (all inventory, recipe-book, backpack and anvil frames).

Usage: accessory_integration_contact_sheet.py <screenshots-dir> <out-dir>
Originals are untouched; the sheet is a 4-column grid with each tile labeled by its screenshot name.
"""
import glob
import os
import re
import sys

from PIL import Image, ImageDraw, ImageFont

shots, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
paths = sorted(glob.glob(os.path.join(shots, "*.png")))
if not paths:
    print("no screenshots found")
    sys.exit(0)

try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
except OSError:
    font = ImageFont.load_default()

COLS, TILE_W = 4, 640
tiles = []
for path in paths:
    img = Image.open(path).convert("RGB")
    h = round(img.height * TILE_W / img.width)
    tile = img.resize((TILE_W, h), Image.LANCZOS)
    label = re.sub(r"^\d+_", "", os.path.splitext(os.path.basename(path))[0])
    ImageDraw.Draw(tile).rectangle([0, 0, TILE_W, 26], fill=(0, 0, 0))
    ImageDraw.Draw(tile).text((6, 3), label, fill=(255, 255, 0), font=font)
    tiles.append(tile)

rows = (len(tiles) + COLS - 1) // COLS
row_h = max(t.height for t in tiles)
sheet = Image.new("RGB", (COLS * TILE_W, rows * row_h), (32, 32, 32))
for i, tile in enumerate(tiles):
    sheet.paste(tile, ((i % COLS) * TILE_W, (i // COLS) * row_h))
sheet.save(os.path.join(out, "integration-contact-sheet-all-screenshots.png"))
print(f"wrote contact sheet with {len(tiles)} screenshots")
