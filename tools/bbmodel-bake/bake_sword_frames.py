# Bake bbmodel quads into the vanilla handheld frame.
# Vanilla iron sword sprite: blade runs from the handle (~pixel 2.5,12.5) toward the top-right corner, flat in the item XY plane.
# Mapping: bbmodel +y (blade) -> item (1,1,0)/sqrt2, bbmodel +x -> item (1,-1,0)/sqrt2, bbmodel +z -> item z.
# Geometry is scaled so the handle-to-tip length equals the vanilla sprite's (17 px), and the handle centre is placed at ANCHOR.
import json, sys, math
import numpy as np
from pathlib import Path
SRC, ANIM, NFRAMES, GRIP_PREFIX, OUT = Path(sys.argv[1]), sys.argv[2], int(sys.argv[3]), sys.argv[4], Path(sys.argv[5])
ANCHOR = np.array([2.5/16, 1-12.5/16, 0.5])
VANILLA_LEN_PX = 17.0
d = json.loads(SRC.read_text(encoding='utf-8'))
anim = None if ANIM == '-' else next(a for a in d['animations'] if a['name'] == ANIM)
L = anim['length'] if anim else 0
def rot(ax, ay, az):
    ax, ay, az = map(math.radians, (ax, ay, az))
    Rx = np.array([[1,0,0],[0,math.cos(ax),-math.sin(ax)],[0,math.sin(ax),math.cos(ax)]])
    Ry = np.array([[math.cos(ay),0,math.sin(ay)],[0,1,0],[-math.sin(ay),0,math.cos(ay)]])
    Rz = np.array([[math.cos(az),-math.sin(az),0],[math.sin(az),math.cos(az),0],[0,0,1]])
    return Rx @ Ry @ Rz
def sample(uuid, channel, t):
    if anim is None: return None
    an = anim['animators'].get(uuid)
    if not an: return None
    ks = sorted([k for k in an['keyframes'] if k['channel'] == channel], key=lambda k: k['time'])
    if not ks: return None
    v = lambda k: np.array([k['data_points'][0][c] for c in 'xyz'], float)
    if t <= ks[0]['time']: return v(ks[0])
    if t >= ks[-1]['time']: return v(ks[-1])
    for a, b in zip(ks, ks[1:]):
        if a['time'] <= t <= b['time']:
            f = (t - a['time']) / (b['time'] - a['time']) if b['time'] != a['time'] else 0
            return v(a) + (v(b) - v(a)) * f
groups, group_parent, elem_parent = {}, {}, {}
def walk(n, parent_gid):
    if isinstance(n, str):
        elem_parent[n] = parent_gid; return
    groups[n['uuid']] = n
    group_parent[n['uuid']] = parent_gid
    for c in n.get('children', []): walk(c, n['uuid'])
for n in d['outliner']: walk(n, None)
def chain_of(gid):
    out = []
    while gid is not None:
        out.append(gid); gid = group_parent.get(gid)
    return list(reversed(out))
def apply_group(gid, t, p):
    g = groups[gid]
    O = np.array(g.get('origin', [0,0,0]), float)
    r = sample(gid, 'rotation', t)
    if r is None: r = np.array(g.get('rotation', [0,0,0]), float)
    pos = sample(gid, 'position', t)
    if pos is None: pos = np.zeros(3)
    return rot(*r) @ (p - O) + O + pos
def elem_corners(e):
    f, to = np.array(e['from'], float), np.array(e['to'], float)
    cs = []
    for z in (f[2], to[2]):
        for y in (f[1], to[1]):
            for x in (f[0], to[0]): cs.append(np.array([x, y, z]))
    r = e.get('rotation', [0,0,0])
    if any(abs(v) > 1e-9 for v in r):
        O = np.array(e['origin'], float); R = rot(*r)
        cs = [R @ (c - O) + O for c in cs]
    return cs
elements = d['elements']
res = d['resolution']['width']
FACES = {'up':[(0,1,0),(0,1,1),(1,1,1),(1,1,0)],'down':[(0,0,1),(0,0,0),(1,0,0),(1,0,1)],
         'north':[(1,1,0),(1,0,0),(0,0,0),(0,1,0)],'south':[(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
         'west':[(0,1,0),(0,0,0),(0,0,1),(0,1,1)],'east':[(1,1,1),(1,0,1),(1,0,0),(1,1,0)]}
def frame_quads(t):
    quads = []
    for e in elements:
        cs = elem_corners(e)
        gid = elem_parent.get(e['uuid'])
        chain = chain_of(gid) if gid else []
        for g in chain:
            cs = [apply_group(g, t, c) for c in cs]
        for fname, picks in FACES.items():
            face = e.get('faces', {}).get(fname)
            uv = face.get('uv') if isinstance(face, dict) else None
            if not uv: continue
            pts = np.array([cs[p[0]+2*p[1]+4*p[2]] for p in picks])
            quads.append((pts, uv, e['name']))
    return quads
# grip centre and blade length measured on the rest pose (frame 0 time)
rest = frame_quads(0)
grip = [c for pts, _, name in rest for c in pts if name.startswith(GRIP_PREFIX)]
GP = np.mean(grip, axis=0)
tip_y = max(c[1] for pts, _, _ in rest for c in pts)
S_SCALE = VANILLA_LEN_PX / (tip_y - GP[1])
M = np.array([[math.sqrt(0.5), math.sqrt(0.5), 0],[-math.sqrt(0.5), math.sqrt(0.5), 0],[0,0,1]])
# M maps bbmodel (x,y,z) -> item basis: column j is the image of basis vector j
M = np.array([[math.sqrt(0.5), math.sqrt(0.5), 0],[-math.sqrt(0.5), math.sqrt(0.5), 0],[0,0,1]]).T.T
def to_item(p):
    return M @ ((p - GP) / 16.0 * S_SCALE) + ANCHOR
frames = {}
n_frames = NFRAMES
for fi in range(n_frames):
    t = L * fi / n_frames if anim else 0
    quads = []
    for pts, (x1,y1,x2,y2), _ in frame_quads(t):
        uvs = [[x1/res,y1/res],[x1/res,y2/res],[x2/res,y2/res],[x2/res,y1/res]]
        v = [to_item(p).round(5).tolist() for p in pts]
        n = np.cross(pts[1]-pts[0], pts[2]-pts[1]); n = n/(np.linalg.norm(n) or 1)
        nn = (M @ n).round(4).tolist()
        quads.append({'v': v, 'uv': [[round(a,5),round(b,5)] for a,b in uvs], 'n': nn})
    frames[('' if not anim and n_frames == 1 else f'f{fi}')] = quads
assert all(np.isfinite(np.array(q['v'])).all() for fr in frames.values() for q in fr)
OUT.write_text(json.dumps({'poses': frames}, separators=(',',':')), encoding='utf-8')
print('grip', GP.round(2).tolist(), 'tip_y', round(tip_y,2), 'scale', round(S_SCALE,4), 'poses', list(frames.keys())[:3], len(frames))
