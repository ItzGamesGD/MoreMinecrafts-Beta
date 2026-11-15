"""Tomb Guardian model definition, UV packing and a small preview renderer.

The hierarchy, bone names and pivots (PartPose offsets) of every vanilla Warden bone are kept exactly as in
WardenModel.createBodyLayer (26.3): bone, body, right/left_ribcage, head, right/left_tendril, right/left_arm,
right/left_leg. Only the cubes are reshaped into a sandstone colossus, and decorative child parts are added
(they never replace a vanilla bone), so every vanilla Warden keyframe animation still drives the model.

Face orientation follows ModelPart.Cube (same convention as tools/pyramid-mob-textures/uvmodel.py): side strip
order right(-X), front(-Z), left(+X), back(+Z); top/bottom faces have columns -X..+X and rows back..front.
Model space: +Y is down, the front of the model is -Z.
"""
from __future__ import annotations

import math
from dataclasses import dataclass, field

from PIL import Image, ImageDraw

TEX_W = 256
TEX_H = 256


@dataclass
class Part:
    name: str
    parent: str | None
    pose: tuple  # PartPose.offset(x, y, z) relative to the parent part
    box: tuple | None  # (x, y, z, w, h, d) cube in part space, or None
    vanilla: bool = False
    u: int = 0
    v: int = 0


# Vanilla Warden poses and (reshaped) cubes. Extra parts use pose (0, 0, 0) and never carry animation names.
PARTS: list[Part] = [
    Part("bone", None, (0, 24, 0), None, vanilla=True),
    Part("body", "bone", (0, -21, 0), (-9, -13, -4, 18, 21, 11), vanilla=True),
    Part("right_ribcage", "body", (-7, -2, -4), (-2, -11, -0.1, 9, 21, 0), vanilla=True),
    Part("left_ribcage", "body", (7, -2, -4), (-7, -11, -0.1, 9, 21, 0), vanilla=True),
    Part("head", "body", (0, -13, 0), (-8, -16, -5, 16, 16, 10), vanilla=True),
    Part("right_tendril", "head", (-8, -12, 0), (-4, -4, -5, 4, 18, 10), vanilla=True),   # nemes lappet
    Part("left_tendril", "head", (8, -12, 0), (0, -4, -5, 4, 18, 10), vanilla=True),      # nemes lappet
    Part("right_arm", "body", (-13, -13, 1), (-4, 0, -4, 8, 28, 8), vanilla=True),
    Part("left_arm", "body", (13, -13, 1), (-4, 0, -4, 8, 28, 8), vanilla=True),
    Part("right_leg", "bone", (-5.9, -13, 0), (-4, 0, -4, 8, 13, 8), vanilla=True),
    Part("left_leg", "bone", (5.9, -13, 0), (-4, 0, -4, 8, 13, 8), vanilla=True),
    # torso decoration
    Part("collar", "body", (0, 0, 0), (-10, -14, -5, 20, 5, 13)),
    Part("chest_inlay_right", "body", (0, 0, 0), (-7, -9, -6, 5, 3, 2)),
    Part("chest_inlay_left", "body", (0, 0, 0), (2, -9, -6, 5, 3, 2)),
    Part("belt", "body", (0, 0, 0), (-9.5, 3, -4.5, 19, 4, 12)),
    Part("kilt", "body", (0, 0, 0), (-5, 7, -5, 10, 8, 2)),
    # nemes headdress, face and beard
    Part("nemes_cap", "head", (0, 0, 0), (-9, -17, -6, 18, 3, 12)),
    Part("brow_band", "head", (0, 0, 0), (-9.5, -14, -6.5, 19, 3, 13)),
    Part("uraeus", "head", (0, 0, 0), (-1, -19, -7.5, 2, 6, 2)),
    Part("nose", "head", (0, 0, 0), (-1.5, -8, -7, 3, 5, 2)),
    Part("beard", "head", (0, 0, 0), (-2, -1, -8, 4, 7, 3)),
    Part("nemes_back", "head", (0, 0, 0), (-8, -16, 5, 16, 20, 4)),
    # arms: gold and lapis pauldron, red sandstone upper band, lapis and gold bracer, fist
    Part("right_pauldron", "right_arm", (0, 0, 0), (-5.5, -3, -5.5, 11, 6, 11)),
    Part("right_upper_band", "right_arm", (0, 0, 0), (-4.5, 7, -4.5, 9, 5, 9)),
    Part("right_bracer", "right_arm", (0, 0, 0), (-5, 17, -5, 10, 5, 10)),
    Part("right_fist", "right_arm", (0, 0, 0), (-5, 22, -5, 10, 9, 10)),
    Part("left_pauldron", "left_arm", (0, 0, 0), (-5.5, -3, -5.5, 11, 6, 11)),
    Part("left_upper_band", "left_arm", (0, 0, 0), (-4.5, 7, -4.5, 9, 5, 9)),
    Part("left_bracer", "left_arm", (0, 0, 0), (-5, 17, -5, 10, 5, 10)),
    Part("left_fist", "left_arm", (0, 0, 0), (-5, 22, -5, 10, 9, 10)),
    # feet
    Part("right_foot", "right_leg", (0, 0, 0), (-4.5, 9, -6, 9, 4, 11)),
    Part("left_foot", "left_leg", (0, 0, 0), (-4.5, 9, -6, 9, 4, 11)),
]

BY_NAME = {p.name: p for p in PARTS}

VANILLA_BONES = ("bone", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg",
                 "right_ribcage", "left_ribcage", "right_tendril", "left_tendril")


def rect_size(p: Part) -> tuple[int, int]:
    _, _, _, w, h, d = p.box
    return 2 * d + 2 * w, d + h


def pack() -> None:
    """Shelf-pack every cube's UV strip into the atlas (deterministic, 1px padding)."""
    boxed = [p for p in PARTS if p.box]
    boxed.sort(key=lambda p: (-rect_size(p)[1], -rect_size(p)[0], p.name))
    x = y = shelf = 0
    for p in boxed:
        w, h = rect_size(p)
        if x + w > TEX_W:
            x, y, shelf = 0, y + shelf + 1, 0
        p.u, p.v = x, y
        x += w + 1
        shelf = max(shelf, h)
    if y + shelf > TEX_H:
        raise SystemExit(f"atlas overflow: needs {y + shelf} rows of {TEX_H}")


pack()


def world_offset(p: Part) -> tuple:
    x = y = z = 0.0
    q = p
    while q is not None:
        x += q.pose[0]
        y += q.pose[1]
        z += q.pose[2]
        q = BY_NAME[q.parent] if q.parent else None
    return x, y, z


# ---------------------------------------------------------------- texels

@dataclass(frozen=True)
class Texel:
    part: Part
    face: str
    i: int
    j: int
    fw: int
    fh: int
    px: int
    py: int
    pos: tuple  # centre of the texel in part space (model units)

    @property
    def side(self) -> bool:
        return self.face in ("right", "front", "left", "back")


def face_rects(p: Part):
    _, _, _, w, h, d = p.box
    u, v = p.u, p.v
    return [
        ("top", u + d, v, w, d),
        ("bottom", u + d + w, v, w, d),
        ("right", u, v + d, d, h),
        ("front", u + d, v + d, w, h),
        ("left", u + d + w, v + d, d, h),
        ("back", u + 2 * d + w, v + d, w, h),
    ]


def face_corners(p: Part):
    x0, y0, z0, w, h, d = p.box
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    return {
        "front": ((x0, y0, z0), (x1, y0, z0), (x0, y1, z0)),
        "right": ((x0, y0, z1), (x0, y0, z0), (x0, y1, z1)),
        "left": ((x1, y0, z0), (x1, y0, z1), (x1, y1, z0)),
        "back": ((x1, y0, z1), (x0, y0, z1), (x1, y1, z1)),
        "top": ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
        "bottom": ((x0, y1, z1), (x1, y1, z1), (x0, y1, z0)),
    }


def texels(p: Part):
    if not p.box:
        return
    corners = face_corners(p)
    for face, u0, v0, fw, fh in face_rects(p):
        if fw <= 0 or fh <= 0:
            continue
        o, cu, cv = corners[face]
        for j in range(fh):
            for i in range(fw):
                a, b = (i + 0.5) / fw, (j + 0.5) / fh
                pos = tuple(o[k] + (cu[k] - o[k]) * a + (cv[k] - o[k]) * b for k in range(3))
                yield Texel(p, face, i, j, fw, fh, u0 + i, v0 + j, pos)


def paint(painter) -> Image.Image:
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    px = img.load()
    for p in PARTS:
        for t in texels(p):
            c = painter(t)
            if c is None:
                continue
            if len(c) == 3:
                c = (*c, 255)
            px[t.px, t.py] = tuple(int(max(0, min(255, round(x)))) for x in c)
    return img


# ---------------------------------------------------------------- preview

_NORMALS = {"front": (0, 0, -1), "back": (0, 0, 1), "right": (-1, 0, 0), "left": (1, 0, 0),
            "top": (0, -1, 0), "bottom": (0, 1, 0)}


def render(tex: Image.Image, glow: Image.Image | None = None, yaw_deg=0.0, pitch_deg=8.0, scale=8,
           size=(560, 620), background=(46, 42, 38, 255)) -> Image.Image:
    """Orthographic software preview (no lighting model beyond a face tint), rest pose, no animation."""
    tp = tex.convert("RGBA").load()
    gp = glow.convert("RGBA").load() if glow else None
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    quads = []
    for part in PARTS:
        if not part.box:
            continue
        ox, oy, oz = world_offset(part)
        corners = face_corners(part)
        rects = {f: (u0, v0, fw, fh) for f, u0, v0, fw, fh in face_rects(part)}
        for face, (o, cu, cv) in corners.items():
            u0, v0, fw, fh = rects[face]
            if fw <= 0 or fh <= 0:
                continue
            n = _NORMALS[face]
            for j in range(fh):
                for i in range(fw):
                    col = tp[u0 + i, v0 + j]
                    if col[3] < 128:
                        continue
                    g = gp[u0 + i, v0 + j] if gp else (0, 0, 0, 0)
                    pts = []
                    for di, dj in ((0, 0), (1, 0), (1, 1), (0, 1)):
                        a, b = (i + di) / fw, (j + dj) / fh
                        q = tuple(o[k] + (cu[k] - o[k]) * a + (cv[k] - o[k]) * b for k in range(3))
                        pts.append((q[0] + ox, q[1] + oy, q[2] + oz))
                    quads.append((pts, col, g, n))

    def view(p):
        x, y, z = p
        c, s = math.cos(yaw), math.sin(yaw)
        x, z = x * c - z * s, x * s + z * c
        c, s = math.cos(pitch), math.sin(pitch)
        y, z = y * c - z * s, y * s + z * c
        return x, y, z

    items = []
    for pts, col, g, n in quads:
        vp = [view(p) for p in pts]
        vn = view(n)
        if vn[2] > 1e-6:
            continue
        depth = sum(p[2] for p in vp) / 4
        light = 0.62 + 0.38 * max(0.0, -vn[2]) + (0.12 if n[1] < -0.5 else 0) - (0.2 if n[1] > 0.5 else 0)
        items.append((depth, vp, col, g, light))
    items.sort(key=lambda t: -t[0])
    xs = [p[0] for _, vp, _, _, _ in items for p in vp]
    ys = [p[1] for _, vp, _, _, _ in items for p in vp]
    ox = size[0] / 2 - (min(xs) + max(xs)) / 2 * scale
    oy = size[1] / 2 - (min(ys) + max(ys)) / 2 * scale
    img = Image.new("RGBA", size, background)
    dr = ImageDraw.Draw(img)
    for _, vp, col, g, light in items:
        poly = [(ox + p[0] * scale, oy + p[1] * scale) for p in vp]
        c = [min(255, col[k] * light) for k in range(3)]
        if g[3] > 0:  # emissive pixels ignore shading and add their colour
            c = [min(255, c[k] * (1 - g[3] / 255) + g[k] * (g[3] / 255) * 1.0) for k in range(3)]
        dr.polygon(poly, fill=tuple(int(x) for x in c) + (255,))
    return img
