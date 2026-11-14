"""Builds labelled contact sheets (one per background) of the Cloud Slime captures. Usage: contact_sheet.py <dir>"""
import os
import sys
from PIL import Image, ImageDraw

out = sys.argv[1]
sizes = ["large", "medium", "small"]
views = ["front", "side", "back", "left", "top", "three-quarter"]
for bg in ("bright", "dark"):
    sheet = Image.new("RGB", (384 * len(views), 216 * len(sizes)), (0, 0, 0))
    for r, size in enumerate(sizes):
        for c, view in enumerate(views):
            path = os.path.join(out, f"{bg}-{size}-{view}.png")
            if not os.path.exists(path):
                continue
            tile = Image.open(path).convert("RGB").resize((384, 216))
            ImageDraw.Draw(tile).text((6, 6), f"{bg} {size} {view}", fill=(255, 255, 0))
            sheet.paste(tile, (c * 384, r * 216))
    sheet.save(os.path.join(out, f"contact-sheet-{bg}.png"))
