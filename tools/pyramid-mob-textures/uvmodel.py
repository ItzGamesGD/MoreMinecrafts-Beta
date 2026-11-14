"""Vanilla 26.3 model UV layouts used by the pyramid mob textures, and a small preview renderer.

Box definitions were read from the 26.3 client classes (HumanoidModel.createMesh, SkeletonModel,
BabyZombieModel) and LayerDefinitions (zombie/husk 64x64, skeleton 64x32, baby zombie/husk 64x64).
Face orientation follows ModelPart.Cube: side strip order right(-X), front(-Z), left(+X), back(+Z);
top/bottom faces have columns -X..+X and rows back(+Z)..front(-Z).
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field
from PIL import Image, ImageDraw


@dataclass(frozen=True)
class Box:
    part: str
    u: int
    v: int
    w: int
    h: int
    d: int
    origin: tuple  # box min corner in part space (model units, y down, front = -z)
    pivot: tuple
    inflate: float = 0.0
    mirror: bool = False
    layer: str = "base"  # "base" or "outer" (hat / head overlay)
    uv_owner: bool = True  # False when the box reuses another box's UV (mirrored limbs)


@dataclass
class Model:
    name: str
    tex_w: int
    tex_h: int
    boxes: list = field(default_factory=list)
    zombie_arms: bool = False


def adult_humanoid(zombie_arms: bool) -> Model:
    m = Model("adult_humanoid", 64, 64, zombie_arms=zombie_arms)
    m.boxes = [
        Box("head", 0, 0, 8, 8, 8, (-4, -8, -4), (0, 0, 0)),
        Box("head", 32, 0, 8, 8, 8, (-4, -8, -4), (0, 0, 0), inflate=0.5, layer="outer"),
        Box("body", 16, 16, 8, 12, 4, (-4, 0, -2), (0, 0, 0)),
        Box("right_arm", 40, 16, 4, 12, 4, (-3, -2, -2), (-5, 2, 0)),
        Box("left_arm", 40, 16, 4, 12, 4, (-1, -2, -2), (5, 2, 0), mirror=True, uv_owner=False),
        Box("right_leg", 0, 16, 4, 12, 4, (-2, 0, -2), (-1.9, 12, 0)),
        Box("left_leg", 0, 16, 4, 12, 4, (-2, 0, -2), (1.9, 12, 0), mirror=True, uv_owner=False),
    ]
    return m


def baby_zombie() -> Model:
    m = Model("baby_zombie", 64, 64, zombie_arms=True)
    m.boxes = [
        Box("body", 16, 16, 4, 5, 2, (-2, -2.5, -1), (0, 17.5, 0)),
        Box("head", 3, 3, 6, 6, 6, (-3, -6.25, -3), (0, 15.25, 0)),
        Box("head", 35, 3, 6, 6, 6, (-3, -6.15, -3), (0, 15.25, 0), inflate=0.25, layer="outer"),
        Box("right_arm", 36, 16, 2, 5, 2, (-1, -0.5, -1), (-3, 15.5, 0)),
        Box("left_arm", 28, 16, 2, 5, 2, (-1, -0.5, -1), (3, 15.5, 0)),
        Box("right_leg", 8, 16, 2, 4, 2, (-1, 0, -1), (-1, 20, 0)),
        Box("left_leg", 0, 16, 2, 4, 2, (-1, 0, -1), (1, 20, 0)),
    ]
    return m


def skeleton() -> Model:
    m = Model("skeleton", 64, 32)
    m.boxes = [
        Box("head", 0, 0, 8, 8, 8, (-4, -8, -4), (0, 0, 0)),
        Box("head", 32, 0, 8, 8, 8, (-4, -8, -4), (0, 0, 0), inflate=0.5, layer="outer"),
        Box("body", 16, 16, 8, 12, 4, (-4, 0, -2), (0, 0, 0)),
        Box("right_arm", 40, 16, 2, 12, 2, (-1, -2, -1), (-5, 2, 0)),
        Box("left_arm", 40, 16, 2, 12, 2, (-1, -2, -1), (5, 2, 0), mirror=True, uv_owner=False),
        Box("right_leg", 0, 16, 2, 12, 2, (-1, 0, -1), (-2, 12, 0)),
        Box("left_leg", 0, 16, 2, 12, 2, (-1, 0, -1), (2, 12, 0), mirror=True, uv_owner=False),
    ]
    return m


@dataclass(frozen=True)
class Texel:
    """One texel of one face, with coordinates painters can use."""
    box: Box
    face: str  # right, front, left, back, top, bottom
    i: int  # column inside the face
    j: int  # row inside the face
    fw: int  # face width
    fh: int  # face height
    s: int  # for side faces: position around the strip (0..perimeter-1); else -1
    px: int  # texture pixel
    py: int

    @property
    def perimeter(self) -> int:
        return 2 * (self.box.w + self.box.d)

    @property
    def side(self) -> bool:
        return self.face in ("right", "front", "left", "back")


def face_rects(b: Box):
    """(face, u0, v0, width, height) for each face of a box."""
    u, v, w, h, d = b.u, b.v, b.w, b.h, b.d
    return [
        ("top", u + d, v, w, d),
        ("bottom", u + d + w, v, w, d),
        ("right", u, v + d, d, h),
        ("front", u + d, v + d, w, h),
        ("left", u + d + w, v + d, d, h),
        ("back", u + 2 * d + w, v + d, w, h),
    ]


def texels(b: Box):
    offsets = {"right": 0, "front": b.d, "left": b.d + b.w, "back": 2 * b.d + b.w}
    for face, u0, v0, fw, fh in face_rects(b):
        for j in range(fh):
            for i in range(fw):
                s = offsets[face] + i if face in offsets else -1
                yield Texel(b, face, i, j, fw, fh, s, u0 + i, v0 + j)


def uv_mask(model: Model, layer: str | None = None):
    """Set of texture pixels covered by owned boxes (optionally one layer)."""
    out = set()
    for b in model.boxes:
        if not b.uv_owner or (layer and b.layer != layer):
            continue
        for t in texels(b):
            out.add((t.px, t.py))
    return out


def paint(model: Model, painter) -> Image.Image:
    img = Image.new("RGBA", (model.tex_w, model.tex_h), (0, 0, 0, 0))
    px = img.load()
    for b in model.boxes:
        if not b.uv_owner:
            continue
        for t in texels(b):
            c = painter(t)
            if c is None:
                continue
            if len(c) == 3:
                c = (*c, 255)
            px[t.px, t.py] = tuple(int(max(0, min(255, round(x)))) for x in c)
    return img


# ---------------------------------------------------------------- preview renderer

def _face_corners(b: Box):
    g = b.inflate
    x0, y0, z0 = (o - g for o in b.origin)
    x1, y1, z1 = b.origin[0] + b.w + g, b.origin[1] + b.h + g, b.origin[2] + b.d + g
    # each face: origin corner (texel column 0, row 0), column axis end, row axis end
    return {
        "front": ((x0, y0, z0), (x1, y0, z0), (x0, y1, z0)),
        "right": ((x0, y0, z1), (x0, y0, z0), (x0, y1, z1)),
        "left": ((x1, y0, z0), (x1, y0, z1), (x1, y1, z0)),
        "back": ((x1, y0, z1), (x0, y0, z1), (x1, y1, z1)),
        "top": ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
        "bottom": ((x0, y1, z1), (x1, y1, z1), (x0, y1, z0)),
    }, (x0 + x1) / 2


_NORMALS = {"front": (0, 0, -1), "back": (0, 0, 1), "right": (-1, 0, 0), "left": (1, 0, 0),
            "top": (0, -1, 0), "bottom": (0, 1, 0)}


def _rot_x(p, a):
    x, y, z = p
    c, s = math.cos(a), math.sin(a)
    return (x, y * c - z * s, y * s + z * c)


def render(model: Model, tex: Image.Image, yaw_deg=25.0, pitch_deg=12.0, scale=10, size=(420, 520),
           background=(48, 44, 40, 255), arm_pitch=None) -> Image.Image:
    """Orthographic preview using the same per-face UV orientation as ModelPart.Cube."""
    tp = tex.convert("RGBA").load()
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    quads = []
    for b in model.boxes:
        corners, cx = _face_corners(b)
        rot = 0.0
        if "arm" in b.part and model.zombie_arms:
            rot = -math.pi / 2 if arm_pitch is None else arm_pitch
        rects = {f: (u0, v0, fw, fh) for f, u0, v0, fw, fh in face_rects(b)}
        for face, (o, cu, cv) in corners.items():
            u0, v0, fw, fh = rects[face]
            n = _NORMALS[face]
            if b.mirror:
                o, cu, cv = ((2 * cx - p[0], p[1], p[2]) for p in (o, cu, cv))
                n = (-n[0], n[1], n[2])

            def world(p):
                q = _rot_x(p, rot)
                return (q[0] + b.pivot[0], q[1] + b.pivot[1], q[2] + b.pivot[2])

            nn = _rot_x(n, rot)
            for j in range(fh):
                for i in range(fw):
                    col = tp[u0 + i, v0 + j]
                    if col[3] < 128:
                        continue
                    pts = []
                    for di, dj in ((0, 0), (1, 0), (1, 1), (0, 1)):
                        a, bb = (i + di) / fw, (j + dj) / fh
                        p = tuple(o[k] + (cu[k] - o[k]) * a + (cv[k] - o[k]) * bb for k in range(3))
                        pts.append(world(p))
                    quads.append((pts, col, nn, b.layer))
    def view(p):
        x, y, z = p
        c, s = math.cos(yaw), math.sin(yaw)
        x, z = x * c - z * s, x * s + z * c
        c, s = math.cos(pitch), math.sin(pitch)
        y, z = y * c - z * s, y * s + z * c
        return x, y, z
    img = Image.new("RGBA", size, background)
    dr = ImageDraw.Draw(img)
    items = []
    for pts, col, n, layer in quads:
        vp = [view(p) for p in pts]
        vn = view(n)
        if vn[2] > 1e-6:  # facing away from the camera (camera looks toward +z)
            continue
        depth = sum(p[2] for p in vp) / 4 - (0.01 if layer == "outer" else 0)
        light = 0.62 + 0.38 * max(0.0, -vn[2]) + (0.12 if n[1] < -0.5 else 0) - (0.2 if n[1] > 0.5 else 0)
        items.append((depth, vp, col, light))
    items.sort(key=lambda t: -t[0])
    xs = [p[0] for _, vp, _, _ in items for p in vp]
    ys = [p[1] for _, vp, _, _ in items for p in vp]
    ox = size[0] / 2 - (min(xs) + max(xs)) / 2 * scale
    oy = size[1] / 2 - (min(ys) + max(ys)) / 2 * scale
    for _, vp, col, light in items:
        poly = [(ox + p[0] * scale, oy + p[1] * scale) for p in vp]
        c = tuple(int(min(255, col[k] * light)) for k in range(3)) + (255,)
        dr.polygon(poly, fill=c)
    return img
