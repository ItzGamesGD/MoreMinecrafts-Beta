"""Builds a labelled contact sheet of the Crystal Bug capture views. Usage: contact_sheet.py <results dir>"""
import os
import sys
from PIL import Image, ImageDraw

out = sys.argv[1]
views = ["front", "back", "left", "right", "top", "front-three-quarter", "back-three-quarter", "head-close",
         "crystals-close"]
tiles = [(v, Image.open(os.path.join(out, v + ".png")).convert("RGB").resize((640, 360))) for v in views
         if os.path.exists(os.path.join(out, v + ".png"))]
sheet = Image.new("RGB", (640 * 3, 360 * ((len(tiles) + 2) // 3)), (0, 0, 0))
for n, (name, tile) in enumerate(tiles):
    ImageDraw.Draw(tile).text((8, 8), name, fill=(255, 255, 0))
    sheet.paste(tile, ((n % 3) * 640, (n // 3) * 360))
sheet.save(os.path.join(out, "contact-sheet.png"))
