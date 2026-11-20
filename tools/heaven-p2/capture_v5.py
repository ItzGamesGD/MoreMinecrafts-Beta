"""Re-run the committed V5 layout while recording every placed instance (exact voxels,
order, base) so P2 can convert per instance.  The design code is not modified: Layout.place
and Layout.canopy_drapes are wrapped only for the duration of the run."""
import os, sys, zlib
import numpy as np
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'heaven-prototype'))
import build_prototype as bp            # noqa: E402  (sets design import paths / cwd)
import veg_layout as vl, veg_models as vm   # noqa: E402

V5_CRC = 3288601534   # fine-grid crc32 recorded by the vegetation round (results.json) and P1 manifest
R = vm.R


def capture():
    rec = []
    orig_place, orig_drapes = vl.Layout.place, vl.Layout.canopy_drapes

    def place(self, cat, species, model, bx, bz, H=None, hard=0, soft=0, by=None, extra=None):
        byr = (self.s.top[bx, bz] + 1) if by is None else by
        before = self.fine.copy()
        out = orig_place(self, cat, species, model, bx, bz, H, hard, soft, by, extra)
        idx = np.argwhere((before == 0) & (self.fine > 0))         # voxels this instance actually wrote
        rec.append(dict(cat=cat, species=species, bx=bx, by=int(byr), bz=bz, H=H, model=dict(model),
                        voxels=idx, mats=self.fine[tuple(idx.T)].copy()))
        return out

    def drapes(self, n, wis=True, Lrange=(4, 10)):
        before = self.fine.copy()
        k = orig_drapes(self, n, wis, Lrange)
        idx = np.argwhere((before == 0) & (self.fine > 0))
        rec.append(dict(cat='drape', species='wisteria_canopy', voxels=idx, mats=self.fine[tuple(idx.T)].copy()))
        return k

    vl.Layout.place, vl.Layout.canopy_drapes = place, drapes
    try:
        s = vl.Surface(); L = vl.build('V5', s)
    finally:
        vl.Layout.place, vl.Layout.canopy_drapes = orig_place, orig_drapes
    crc = zlib.crc32(L.fine.tobytes())
    assert crc == V5_CRC, f'V5 layout drifted: {crc} != {V5_CRC}'
    return s, L, rec


def world_block(fine_idx):
    """fine voxel index (N,3) -> world block coords (template space)."""
    f = np.asarray(fine_idx)
    return np.stack([f[:, 0] // R, f[:, 1] // R + vl.YLO, f[:, 2] // R], 1)


if __name__ == '__main__':
    s, L, rec = capture()
    import collections
    print('instances', len(rec), collections.Counter(r['cat'] for r in rec))
    print('voxels total', sum(len(r['voxels']) for r in rec), '== fine', int((L.fine > 0).sum()))
