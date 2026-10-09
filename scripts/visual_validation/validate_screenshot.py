#!/usr/bin/env python3
"""Validate the captured Minecraft PNG without declaring scene semantics verified."""
import json
import math
import statistics
import sys
from pathlib import Path

from PIL import Image, ImageStat

image_path = Path(sys.argv[1] if len(sys.argv) > 1 else "visual-validation-results/visual-world.png")
result_path = Path(sys.argv[2] if len(sys.argv) > 2 else "visual-validation-results/validation.json")
readiness_log = Path("visual-validation-results/client.log")
capture_source_path = Path("visual-validation-results/capture-source.txt")
if not image_path.is_file():
    raise SystemExit(f"Screenshot missing: {image_path}")
if not readiness_log.is_file() or "VISUAL_VALIDATION_WORLD_READY" not in readiness_log.read_text(errors="replace"):
    raise SystemExit("Test-world readiness signal missing from the client log.")

with Image.open(image_path) as source:
    source.verify()
with Image.open(image_path) as source:
    image = source.convert("RGB")
    width, height = image.size
    colors = image.getcolors(maxcolors=1_000_001)
    distinct_colors = len(colors) if colors is not None else 1_000_001
    stats = ImageStat.Stat(image)
    channel_stdev = stats.stddev
    mean_stdev = statistics.mean(channel_stdev)
    center = image.crop((width // 4, height // 4, 3 * width // 4, 3 * height // 4))
    center_colors = center.getcolors(maxcolors=1_000_001)
    center_distinct_colors = len(center_colors) if center_colors is not None else 1_000_001

checks = {
    "file_exists": image_path.is_file(),
    "png_decodes": True,
    "dimensions_1280x720": [width, height] == [1280, 720],
    "not_uniform_or_blank": distinct_colors >= 128 and mean_stdev >= 8.0,
    "scene_region_has_visual_detail": center_distinct_colors >= 64,
    "world_readiness_signal": True,
    "capture_source": capture_source_path.read_text().strip() if capture_source_path.is_file() else "unknown",
    "visual_content": "NEEDS_MANUAL_REVIEW",
    "distinct_colors": distinct_colors,
    "center_distinct_colors": center_distinct_colors,
    "mean_channel_standard_deviation": round(mean_stdev, 3),
    "screenshot": image_path.name,
}
result_path.write_text(json.dumps(checks, indent=2) + "\n")
for key, value in checks.items():
    print(f"{key}: {value}")
if not all((checks["file_exists"], checks["png_decodes"], checks["dimensions_1280x720"],
            checks["not_uniform_or_blank"], checks["scene_region_has_visual_detail"],
            checks["world_readiness_signal"])):
    raise SystemExit("Screenshot structural validation failed.")
