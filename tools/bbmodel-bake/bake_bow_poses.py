# Bake bbmodel quads into the vanilla handheld frame.
# Vanilla iron sword sprite: blade runs from the handle (~pixel 2.5,12.5) toward the top-right corner, flat in the item XY plane.
# Mapping: bbmodel +y (blade) -> item (1,1,0)/sqrt2, bbmodel +x -> item (1,-1,0)/sqrt2, bbmodel +z -> item z.
# Geometry is scaled so the handle-to-tip length equals the vanilla sprite's (17 px), and the handle centre is placed at ANCHOR.
import json, sys, math
import numpy as np
from pathlib import Path
SRC, OUT = Path(sys.argv[1]), Path(sys.argv[2])
POSES = ['pulling_0', 'pulling_1', 'pulling_2']
ANCHOR = np.array([3.5/16, 1-8.5/16, 0.5])  # vanilla bow handle pixel (3.5, 8.5)
VANILLA_LEN_PX = 17.2  # vanilla bow sprite chord: (12,1) to (2,15) px
d = json.loads(SRC.read_text(encoding='utf-8'))

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

LIMB_PREFIXES = ('고목 활대', '옥빛 활끝 고리', '시위 고정 홈', '고목 걸쇠', '윗 시위', '아랫 시위')
GRIP_PREFIXES = ('감아 두른 가죽', '고목 손잡이 심재')
def set_anim(name):
    global anim, L
    anim = next(a for a in d['animations'] if a['name'] == name)
    L = anim['length']
set_anim('pulling_0')
rest = frame_quads(0)
limb = np.array([c for pts, _, n in rest if n.startswith(LIMB_PREFIXES) for c in pts])
grip = np.array([c for pts, _, n in rest if n.startswith(GRIP_PREFIXES) for c in pts])
GP = grip.mean(axis=0)
mu = limb.mean(axis=0)
U, Sv, Vt = np.linalg.svd(limb - mu, full_matrices=False)
span, bulge_in_plane, normal = Vt[0], Vt[1], Vt[2]
proj = (limb - mu) @ span
top, bot = limb[np.argmax(proj)], limb[np.argmin(proj)]
if (top - bot) @ span < 0: span = -span
chord = top - bot
chord_len = float(np.linalg.norm(chord))
s_b = chord / chord_len
# bulge: component of (grip - chord midpoint) perpendicular to the span
mid = (top + bot) / 2
e = GP - mid; e = e - (e @ s_b) * s_b; e = e / np.linalg.norm(e)
n_b = np.cross(s_b, e); n_b = n_b / np.linalg.norm(n_b)
# sprite targets: chord direction (10,14)/|.| in item XY (top-right is +x,+y), bulge toward the wood at -x
u_i = np.array([10.0, 14.0, 0.0]); u_i /= np.linalg.norm(u_i)
p_i = np.array([-14.0, 10.0, 0.0]); p_i /= np.linalg.norm(p_i)  # perpendicular to u_i, toward -x
z_i = np.array([0.0, 0.0, 1.0])
Bb = np.column_stack([s_b, e, n_b]); Bi = np.column_stack([u_i, p_i, z_i])
M = Bi @ Bb.T
if np.linalg.det(M) < 0:
    z_i = -z_i; Bi = np.column_stack([u_i, p_i, z_i]); M = Bi @ Bb.T
S_SCALE = VANILLA_LEN_PX / chord_len
def to_item(p):
    return M @ ((p - GP) / 16.0 * S_SCALE) + ANCHOR
poses = {}
for name in POSES:
    set_anim(name)
    quads = []
    for pts, (x1,y1,x2,y2), _ in frame_quads(0):
        uvs = [[x1/res,y1/res],[x1/res,y2/res],[x2/res,y2/res],[x2/res,y1/res]]
        v = [to_item(p).round(5).tolist() for p in pts]
        n = np.cross(pts[1]-pts[0], pts[2]-pts[1]); n = n/(np.linalg.norm(n) or 1)
        quads.append({'v': v, 'uv': [[round(a,5),round(b,5)] for a,b in uvs], 'n': (M @ n).round(4).tolist()})
    poses[name] = quads
assert all(np.isfinite(np.array(q['v'])).all() for fr in poses.values() for q in fr)
OUT.write_text(json.dumps({'poses': poses}, separators=(',',':')), encoding='utf-8')
print('chord_px', round(chord_len,2), 'scale', round(S_SCALE,4), 'det', round(float(np.linalg.det(M)),3), 'span', s_b.round(3).tolist(), 'bulge', e.round(3).tolist(), 'grip', GP.round(2).tolist())
