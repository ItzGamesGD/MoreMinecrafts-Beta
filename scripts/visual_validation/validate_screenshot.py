"""Validate a native Minecraft PNG and the client capture lifecycle."""
import json
import statistics
import sys
from pathlib import Path

from PIL import Image, ImageStat

image_path = Path(sys.argv[1] if len(sys.argv) > 1 else "visual-validation-results/visual-world.png")
result_path = Path(sys.argv[2] if len(sys.argv) > 2 else "visual-validation-results/validation.json")
log_path = Path("visual-validation-results/client.log")
capture_source_path = Path("visual-validation-results/capture-source.txt")

if not image_path.is_file():
    raise SystemExit(f"Screenshot missing: {image_path}")
if not log_path.is_file():
    raise SystemExit("Client log missing.")
log = log_path.read_text(errors="replace")
required_markers = (
    "VISUAL_VALIDATION_PLAYER_JOINED",
    "VISUAL_VALIDATION_WORLD_READY",
    "VISUAL_VALIDATION_FRAME_READY",
    "VISUAL_CAPTURE_REQUESTED",
    "VISUAL_CAPTURE_WRITTEN",
)
missing = [marker for marker in required_markers if marker not in log]
if missing:
    raise SystemExit(f"Client capture lifecycle markers missing: {', '.join(missing)}")
if "VISUAL_CAPTURE_FAILED" in log:
    raise SystemExit("Client also emitted VISUAL_CAPTURE_FAILED.")
if not capture_source_path.is_file() or "Minecraft Screenshot.grab" not in capture_source_path.read_text():
    raise SystemExit("Screenshot was not produced through the native Minecraft capture API.")

with Image.open(image_path) as source:
    source.verify()
with Image.open(image_path) as source:
    image = source.convert("RGB")
    width, height = image.size
    colors = image.getcolors(maxcolors=1_000_001)
    distinct_colors = len(colors) if colors is not None else 1_000_001
    mean_stdev = statistics.mean(ImageStat.Stat(image).stddev)
    center = image.crop((width // 4, height // 4, 3 * width // 4, 3 * height // 4))
    center_colors = center.getcolors(maxcolors=1_000_001)
    center_distinct_colors = len(center_colors) if center_colors is not None else 1_000_001

checks = {
    "file_exists": image_path.is_file(),
    "png_decodes": True,
    "dimensions_1280x720": [width, height] == [1280, 720],
    "non_uniform_with_meaningful_variation": distinct_colors >= 128 and mean_stdev >= 8.0,
    "center_region_has_visual_detail": center_distinct_colors >= 64,
    "client_world_and_capture_lifecycle": True,
    "native_capture_api": "Minecraft Screenshot.grab from mainRenderTarget",
    "visual_content": "NEEDS_MANUAL_REVIEW",
    "distinct_colors": distinct_colors,
    "center_distinct_colors": center_distinct_colors,
    "mean_channel_standard_deviation": round(mean_stdev, 3),
    "screenshot": image_path.name,
}
result_path.write_text(json.dumps(checks, indent=2) + "\n")
for key, value in checks.items():
    print(f"{key}: {value}")
if not all((
    checks["file_exists"],
    checks["png_decodes"],
    checks["dimensions_1280x720"],
    checks["non_uniform_with_meaningful_variation"],
    checks["center_region_has_visual_detail"],
    checks["client_world_and_capture_lifecycle"],
)):
    raise SystemExit("Screenshot structural validation failed; semantic scene content requires manual review.")
