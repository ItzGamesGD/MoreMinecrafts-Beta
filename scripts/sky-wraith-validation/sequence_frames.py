#!/usr/bin/env python3
"""Record the actual display as timestamped frames; no synthetic model poses."""
import base64
import csv
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw

scene, seconds = sys.argv[1], float(sys.argv[2])
out = Path('sky-wraith-results/sequences') / scene
out.mkdir(parents=True, exist_ok=True)
frames = []
start = time.monotonic()
while time.monotonic() - start < seconds:
    path = out / f'frame-{len(frames):04d}.png'
    before = time.time_ns()
    subprocess.run(['scrot', str(path)], check=True, stdout=subprocess.DEVNULL)
    after = time.time_ns()
    frames.append((path, before, after))
    target = start + len(frames) / 8
    time.sleep(max(0, target - time.monotonic()))
with (out / 'timestamps.csv').open('w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(('frame', 'capture_start_utc_ns', 'capture_end_utc_ns'))
    writer.writerows((p.name, a, b) for p,a,b in frames)
if len(frames) < 2:
    raise SystemExit(f'{scene}: fewer than two frames')
fps = (len(frames)-1) / ((frames[-1][1]-frames[0][1])/1e9)
subprocess.run(['ffmpeg','-loglevel','error','-y','-framerate',str(fps),'-i',str(out/'frame-%04d.png'),'-vf','scale=640:-2','-c:v','libx264','-pix_fmt','yuv420p',str(out/'preview.mp4')], check=True)
# A compact contact sheet in the Actions log permits visual inspection when artifact download is restricted.
indices = sorted({round(i*(len(frames)-1)/7) for i in range(8)})
sheet = Image.new('RGB', (1280, 2*385), '#202020')
draw = ImageDraw.Draw(sheet)
for j, index in enumerate(indices):
    im = Image.open(frames[index][0]).convert('RGB')
    im.thumbnail((320, 350))
    x,y=(j%4)*320,(j//4)*385
    sheet.paste(im,(x,y)); draw.text((x+5,y+352),f'{index}  +{(frames[index][1]-frames[0][1])/1e9:.2f}s', fill='white')
preview=out/'contact.jpg'; sheet.save(preview,quality=85)
print(f'SEQUENCE {scene} frames={len(frames)} start_utc={datetime.fromtimestamp(frames[0][1]/1e9,timezone.utc).isoformat()} fps_measured={fps:.3f}', flush=True)
print(f'SEQUENCE_PREVIEW_BEGIN {scene}', flush=True)
print(base64.encodebytes(preview.read_bytes()).decode(), end='', flush=True)
print(f'SEQUENCE_PREVIEW_END {scene}', flush=True)
