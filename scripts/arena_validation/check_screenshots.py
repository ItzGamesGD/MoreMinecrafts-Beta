#!/usr/bin/env python3
"""Reject empty/broken native captures; human/model vision review remains mandatory."""
from pathlib import Path
from PIL import Image, ImageStat

for index in (0, 1):
    path = Path(f"arena-validation-results/arena-camera-{index}.png")
    with Image.open(path) as image:
        assert image.size == (1280, 720), (path, image.size)
        image = image.convert("RGB")
        assert max(ImageStat.Stat(image).stddev) > 10, f"Blank capture: {path}"
        assert len(image.resize((160, 90)).getcolors(14400) or []) > 100, f"Insufficient image detail: {path}"
    print(f"NATIVE_SCREENSHOT_VALIDATED {path}; visual inspection still required")
