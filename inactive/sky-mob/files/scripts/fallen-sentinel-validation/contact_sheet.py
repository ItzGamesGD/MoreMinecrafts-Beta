"""Builds labelled contact sheets (one per background) of the Fallen Sentinel captures. Usage: contact_sheet.py <dir>"""
import os
import sys
from PIL import Image, ImageDraw

out = sys.argv[1]
views = ["front", "left", "right", "back", "front45", "back45", "elevated", "helm-close", "halo-close",
         "wings-close", "lineup"]
cols = 4
for bg in ("bright", "dark"):
    rows = (len(views) + cols - 1) // cols
    sheet = Image.new("RGB", (480 * cols, 270 * rows), (0, 0, 0))
    for k, view in enumerate(views):
        path = os.path.join(out, f"{bg}-{view}.png")
        if not os.path.exists(path):
            continue
        tile = Image.open(path).convert("RGB").resize((480, 270))
        ImageDraw.Draw(tile).text((6, 6), f"{bg} {view}", fill=(255, 255, 0))
        sheet.paste(tile, ((k % cols) * 480, (k // cols) * 270))
    sheet.save(os.path.join(out, f"contact-sheet-{bg}.png"))
