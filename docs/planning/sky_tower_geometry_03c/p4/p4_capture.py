#!/usr/bin/env python3
"""Capture REAL Minecraft 26.3 client frames (Xvfb + Vulkan llvmpipe, `import -window root`) from camera poses defined
relative to a section's tower axis. HUD hidden with F1, spectator mode. Not a schematic render.
usage: RCON_PASSWORD=.. DISPLAY=:77 python3 -I p4_capture.py <ax> <az> <wy> <label> <out_dir> [time]"""
import importlib.util, json, math, subprocess, sys, time
from pathlib import Path
HERE = Path(__file__).resolve().parent
s = importlib.util.spec_from_file_location('rcon', HERE.parents[3] / 'scripts/arena_validation/rcon.py'); rc = importlib.util.module_from_spec(s); s.loader.exec_module(rc)
ax, az, wy = float(sys.argv[1]), float(sys.argv[2]), float(sys.argv[3]); label, out = sys.argv[4], Path(sys.argv[5]); tod = sys.argv[6] if len(sys.argv) > 6 else 'noon'
only = sys.argv[7].split(',') if len(sys.argv) > 7 else None
out.mkdir(parents=True, exist_ok=True)
tops = {tuple(map(int, k.split(','))): v for k, v in json.loads((HERE.parent / 'p3/data/candidates/K2_8balc_A5relaxed_slab_land3/tread_tops_C1.json').read_text()).items()}
def T(deg, r=17.5):
    a = math.radians(deg); return tops[(1, round(r * math.cos(a)), round(r * math.sin(a)))] + wy
def P(r, deg, y): a = math.radians(deg); return (ax + r * math.cos(a), y, az + r * math.sin(a))
def look(c, t):
    dx, dy, dz = t[0] - c[0], t[1] - c[1], t[2] - c[2]
    return math.degrees(math.atan2(-dx, dz)), -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
def tang(r, deg, y, ahead=25, eye=1.62, rr=None): return P(r, deg, y + eye), P(rr or r, deg + ahead, y + eye)
C = {
 '01_overview_45deg': (P(46, 45, wy + 40), (ax, wy + 26, az)),
 '02_wall_and_stair': tang(16.4, 40, T(40), 30, 1.8, 18.5),
 '03_pillar_rhythm_along_stair': tang(18.6, 95, T(95), 35, 1.7, 19.5),
 '04_pillar_arch_joint': (P(31, 150, T(150) + 5), P(20.5, 150, T(150) + 4)),
 '05_curve_transition_diagonal': (P(27, 30, T(45) + 14), P(17.5, 50, T(50))),
 '06_balcony_entrance_B2-1': (P(17.2, 62, T(62) + 1.62), P(20.5, 74, T(74) + 0.8)),
 '07_from_balcony_B2-1': ((ax + 6.0, wy + 24.0 + 1.62, az + 21.6), P(17.5, 40, T(40))),
 '08_under_balcony_B2-1': ((ax + 6.0, wy + 24.0 - 6, az + 28.0), (ax + 5.0, wy + 23.0, az + 21.0)),
 '09_far_arcade_rhythm': (P(75, 100, wy + 30), (ax, wy + 28, az)),
 '10_player_view_walk': tang(17.5, 200, T(200), 22, 1.62),
}
sock = rc.connect(); n = [1]
def cmd(c):
    n[0] += 1; rc.send_packet(sock, n[0], 2, c); return rc.read_packet(sock)[2]
cmd('gamemode spectator P4Walker'); cmd(f'time set {tod}')
meta = {}
for name, (c, t) in C.items():
    if only and name not in only: continue
    yaw, pitch = look(c, t)
    cmd(f'tp P4Walker {c[0]:.3f} {c[1]:.3f} {c[2]:.3f} {yaw:.2f} {pitch:.2f}'); time.sleep(6)
    f = out / f'{label}_{name}_{tod}.png'
    subprocess.run(['import', '-window', 'root', str(f)], check=True)
    meta[f.name] = {'camera': [round(v, 3) for v in c], 'target': [round(v, 3) for v in t], 'yaw': round(yaw, 2), 'pitch': round(pitch, 2), 'time': tod,
                    'renderer': 'Minecraft 26.3 client, Vulkan llvmpipe (Mesa) under Xvfb, HUD hidden (F1)'}
    print(f.name, round(yaw), round(pitch), flush=True)
mp = out / 'captures.json'; old = json.loads(mp.read_text()) if mp.exists() else {}
old.update(meta); mp.write_text(json.dumps(old, indent=1) + '\n')
