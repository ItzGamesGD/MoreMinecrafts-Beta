#!/usr/bin/env python3
"""LIVE movement test with a REAL Minecraft 26.3 client (no bots): the client holds W (never jump/sprint), the
controller only re-aims the player with the server `rotate` command toward a point a few degrees ahead on the
lane circle, and samples the authoritative server position (`data get entity`). Player is in adventure mode.
usage: RCON_PASSWORD=.. DISPLAY=:77 python3 -I p4_walk.py <scenario.json> <out.json>
scenario: {"name","start":[x,y,z],"waypoints":[[x,z],...] (world), "goal":[x,y,z,tol], "lane":[rmin,rmax] or null, "timeout":s}"""
import importlib.util, json, math, re, subprocess, sys, time
from pathlib import Path
HERE = Path(__file__).resolve().parent
s = importlib.util.spec_from_file_location('rcon', HERE.parents[3] / 'scripts/arena_validation/rcon.py'); rc = importlib.util.module_from_spec(s); s.loader.exec_module(rc)
AX = tuple(json.loads(Path(sys.argv[1]).read_text()).get('axis', (999.5, 1000.5)))   # tower axis in world coords
sc = json.loads(Path(sys.argv[1]).read_text())
sock = rc.connect(); n = [1]
def cmd(c):
    n[0] += 1; rc.send_packet(sock, n[0], 2, c); return rc.read_packet(sock)[2]
def pos():
    t = cmd('data get entity P4Walker Pos'); return tuple(float(v) for v in re.findall(r'(-?[\d.]+)d', t))
def key(action): subprocess.run(['xdotool', action, 'w'], check=True)
cmd('gamemode adventure P4Walker'); cmd('effect clear P4Walker')
x, y, z = sc['start']; cmd(f'tp P4Walker {x} {y} {z}'); time.sleep(2.5)
wps = sc['waypoints']; k = 0; trace = []; t0 = time.time(); last_prog = (t0, 0); result = 'TIMEOUT'
key('keydown')
try:
    while time.time() - t0 < sc.get('timeout', 120):
        p = pos(); trace.append([round(time.time() - t0, 2), *[round(v, 3) for v in p]])
        while k < len(wps) - 1 and math.hypot(wps[k][0] - p[0], wps[k][1] - p[2]) < 1.2: k += 1
        if k > last_prog[1]: last_prog = (time.time(), k)
        g = sc['goal']
        if math.hypot(g[0] - p[0], g[2] - p[2]) < g[3] and abs(g[1] - p[1]) < 0.3: result = 'REACHED'; break
        if p[1] < sc['start'][1] - 3: result = 'FELL'; break
        if time.time() - last_prog[0] > 6: result = 'STUCK'; break
        tx, tz = wps[k]; yaw = math.degrees(math.atan2(-(tx - p[0]), tz - p[2]))
        cmd(f'rotate P4Walker {yaw:.2f} 10'); time.sleep(0.15)
finally:
    key('keyup')
settle = sc.get('settle', 0)
rad = [math.hypot(t[1] - AX[0], t[3] - AX[1]) for t in trace if t[0] >= settle]
ys = [t[2] for t in trace]
lane = sc.get('lane')
res = {'name': sc['name'], 'result': result, 'samples': len(trace), 'seconds': round(time.time() - t0, 1), 'waypoints_passed': k, 'waypoints': len(wps),
       'y_start': ys[0] if ys else None, 'y_end': ys[-1] if ys else None, 'max_y_gain_between_samples': round(max([b - a for a, b in zip(ys, ys[1:])] or [0]), 3),
       'radius_min': round(min(rad), 3) if rad else None, 'radius_max': round(max(rad), 3) if rad else None,
       'lane': lane, 'steer_radius': sc.get('steer_radius'), 'lane_check_from_s': settle, 'stayed_in_lane': (all(lane[0] - 0.05 <= r < lane[1] + 0.05 for r in rad) if lane else None), 'jump_key_used': False,
       'mode': 'adventure; W held; no jump/sprint; steering by server rotate only', 'trace': trace}
Path(sys.argv[2]).write_text(json.dumps(res) + '\n'); print({k_: v for k_, v in res.items() if k_ != 'trace'})
