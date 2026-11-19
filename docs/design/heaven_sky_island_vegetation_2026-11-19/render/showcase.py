"""Showcase scenes: models on a flat heaven-grass platform (prototype models,
not NBT terrain).  Used for the model catalogue, giant-flower detail sheet and
multiblock-resolution comparison."""
import numpy as np, collections
import raycast as rc, veg_models as vm, veg_render as vr

R = vm.R


def platform_scene(models, W_blocks, D_blocks=7, gap=None):
    """models: list of (Model, x_block_center).  Returns Scene."""
    Gt = np.zeros((W_blocks, 18, D_blocks), np.uint8)
    Gt[:, 0, :] = rc.KID['stone']; Gt[:, 1, :] = rc.KID['dirt']; Gt[:, 2, :] = rc.KID['grass_block']
    F = np.zeros((W_blocks * R, 15 * R, D_blocks * R), np.uint8)
    for M, xc in models:
        ox, oz = xc * R + 2, (D_blocks // 2) * R + 2
        for (x, y, z), m in M.items():
            fx, fy, fz = ox + x, y, oz + z
            if 0 <= fx < F.shape[0] and 0 <= fy < F.shape[1] and 0 <= fz < F.shape[2] and F[fx, fy, fz] == 0:
                F[fx, fy, fz] = vm.MAT_ID[m]
    return vr.Scene(Gt, F, (0, 3, 0))


def downsample(M, c, frac=0.25):
    """Rebuild a model at a coarser cell size c (fine voxels): a cell is filled when
    >= frac of it is occupied; colour = most common material.  c=4 -> full blocks
    (plain multiblock), c=2 -> half-block elements."""
    cells = collections.defaultdict(collections.Counter)
    for (x, y, z), m in M.items():
        cells[((x + 2) // c, y // c, (z + 2) // c)][m] += 1
    out = vm.Model(); filled = 0
    for (cx, cy, cz), cnt in cells.items():
        if sum(cnt.values()) >= frac * c ** 3:
            filled += 1; m = cnt.most_common(1)[0][0]
            for dx in range(c):
                for dy in range(c):
                    for dz in range(c):
                        out[(cx * c - 2 + dx, cy * c + dy, cz * c - 2 + dz)] = m
    return out, filled


def ruler(h_blocks):
    M = vm.Model()
    for y in range(h_blocks * R):
        for x in (0, 1):
            for z in (0, 1):
                M[(x - 1, y, z - 1)] = 'player' if (y // R) % 2 == 0 else 'wt_2'
    return M
