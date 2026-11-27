#!/usr/bin/env python3
"""ST-01 P8-C2-A: export the owner-approved G0-R1 blueprint as Minecraft 26.3 structure templates.

Source of geometry: the committed, approved docs/design/st01-g0-r1/generated/blueprint.json (c26ea0f8). Nothing is
re-designed here. For each access route the exporter replays the ordered operations, overlays original D1 and applies
the 12-cell R1-01 exception exactly as the blueprint's operation_semantics prescribe, checks the result against the
approved closed_signature, and then splits the identical block array into three placement pieces:

  1. pyramid/st01/assembly_<route>.nbt  every operation-written cell outside the D1 box (81x72x81 frame)
  2. pyramid/royal_hall.nbt             original D1, referenced verbatim (never rewritten)
  3. pyramid/st01/d1_seam_exception.nbt the 12 approved air cells, placed last (4x1x3 frame)

Cells that no operation writes (open air above ground, outside the stepped shell) are structure void and are not
listed, so the asset never claims terrain outside the authored envelope. Python stdlib only; deterministic bytes.

    python3 tools/st01-p8c2a/export_assembly.py            # write assets + manifest
    python3 tools/st01-p8c2a/export_assembly.py --check    # fail if committed outputs differ
"""
import argparse, gzip, hashlib, io, json, struct, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BLUEPRINT = ROOT / 'docs/design/st01-g0-r1/generated/blueprint.json'
EXCEPTIONS = ROOT / 'docs/design/st01-g0-r1/generated/d1-exceptions.json'
D1 = ROOT / 'src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt'
ASSET_DIR = ROOT / 'src/main/resources/data/moreminecrafts/structure/pyramid/st01'
MANIFEST = ROOT / 'docs/design/st01-p8c2a/generated/assembly-manifest.json'

APPROVED_COMMIT = 'c26ea0f871e788d1d331b40151779d86590469f2'
BLUEPRINT_SHA = '3e7106aea15d8f033cbdba4728d90e7ee50875c0e80fc724ad1585510fd78f5a'
EXCEPTIONS_SHA = '9eeed14ca6809b12426f92adf910c8cf514ad52e8263682ada7f49f01f8bc849'
D1_SHA = '7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423'
DATA_VERSION = 5023
LO = (-40, -32, -40); SIZE = (81, 72, 81)
D1_ORIGIN = (-40, -24, 2); D1_SIZE = (39, 20, 39)
SEAM_ORIGIN = (-8, -24, 33); SEAM_SIZE = (4, 1, 3)
ROUTES = ('EAST', 'WEST')

sys.path.insert(0, str(ROOT / 'tools/st01-g0'))
from audit import nbt as read_nbt  # existing read-only G0 decoder (pinned by the blueprint's source hashes)


def sha(data): return hashlib.sha256(data).hexdigest()
def canonical(value): return json.dumps(value, sort_keys=True, separators=(',', ':'))


# ---------------------------------------------------------------- minimal deterministic NBT writer (big-endian)
def _s(text):
    raw = text.encode('utf-8'); return struct.pack('>H', len(raw)) + raw

def _payload(kind, value):
    if kind == 3: return struct.pack('>i', value)
    if kind == 8: return _s(value)
    if kind == 10:
        out = b''
        for name, (k, v) in value.items(): out += bytes([k]) + _s(name) + _payload(k, v)
        return out + b'\x00'
    if kind == 9:
        k, items = value
        return bytes([k]) + struct.pack('>i', len(items)) + b''.join(_payload(k, v) for v in items)
    raise ValueError(kind)

def state_tag(state):
    tag = {'id': (8, state['id'])}
    if state.get('properties'):
        tag['properties'] = (10, {k: (8, v) for k, v in sorted(state['properties'].items())})
    return tag

def entity_tag(payload):
    out = {}
    for key, value in payload.items():
        if not isinstance(value, str): raise ValueError('only string block-entity fields are exported: ' + key)
        out[key] = (8, value)
    return out

def template(size, cells):
    """cells: dict local(x,y,z) -> (state dict, entity payload or None). Sorted for byte determinism."""
    palette, lookup, blocks = [], {}, []
    for pos in sorted(cells):
        state, entity = cells[pos]
        key = canonical(state)
        if key not in lookup: lookup[key] = len(palette); palette.append(state_tag(state))
        entry = {'pos': (9, (3, list(pos))), 'state': (3, lookup[key])}
        if entity is not None: entry['nbt'] = (10, entity_tag(entity))
        blocks.append(entry)
    root = {'DataVersion': (3, DATA_VERSION), 'size': (9, (3, list(size))), 'palette': (9, (10, palette)),
            'blocks': (9, (10, blocks)), 'entities': (9, (0, []))}
    raw = b'\x0a' + _s('') + _payload(10, root)
    data = bytearray(gzip.compress(raw, compresslevel=9, mtime=0)); data[9] = 3  # OS byte pinned to Unix
    return bytes(data)


# ---------------------------------------------------------------- exact replay of the approved operation semantics
def replay(variant, d1):
    """Returns (cells, written) where cells maps authored pos -> state and written marks operation-written cells."""
    cells, written = {}, set()
    for op in variant['operations']:
        x0, x1, y0, y1, z0, z1 = op['box']
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    cells[(x, y, z)] = op['state']; written.add((x, y, z))
    d1_cells = {}
    for block in d1['blocks']:
        p = tuple(a + b for a, b in zip(block['pos'], D1_ORIGIN))
        d1_cells[p] = d1['palette'][block['state']]; cells[p] = d1_cells[p]
    return cells, written, d1_cells


def signature(variant, cells):
    """Blueprint closed_signature: sha256(canonical(palette) + uint16-LE grid in x,y,z order + canonical(entities))."""
    lookup = {canonical(s): i for i, s in enumerate(variant['palette'])}
    air = lookup[canonical({'id': 'minecraft:air'})]
    grid = bytearray()
    for x in range(LO[0], LO[0] + SIZE[0]):
        for y in range(LO[1], LO[1] + SIZE[1]):
            for z in range(LO[2], LO[2] + SIZE[2]):
                s = cells.get((x, y, z))
                grid += struct.pack('<H', air if s is None else lookup[canonical(s)])
    return sha(canonical(variant['palette']).encode() + bytes(grid) + canonical(variant['block_entities']).encode())


def in_d1(p): return all(D1_ORIGIN[i] <= p[i] < D1_ORIGIN[i] + D1_SIZE[i] for i in range(3))


def build():
    raw_bp = BLUEPRINT.read_bytes(); raw_ex = EXCEPTIONS.read_bytes(); raw_d1 = D1.read_bytes()
    assert sha(raw_bp) == BLUEPRINT_SHA, 'blueprint.json is not the approved c26ea0f8 file'
    assert sha(raw_ex) == EXCEPTIONS_SHA, 'd1-exceptions.json is not the approved c26ea0f8 file'
    assert sha(raw_d1) == D1_SHA, 'royal_hall.nbt is not the original D1'
    bp = json.loads(raw_bp); exceptions = json.loads(raw_ex); d1 = read_nbt(raw_d1)
    assert len(d1['blocks']) == D1_SIZE[0] * D1_SIZE[1] * D1_SIZE[2], 'D1 must own every cell of its box'
    seam = {tuple(e['assembly']) for e in exceptions}
    assert seam == {(x, -24, z) for x in range(-8, -4) for z in range(33, 36)}
    air = {'id': 'minecraft:air'}
    outputs, report = {}, {}
    seam_cells = {}
    for e in exceptions:
        assert e['post'] == air and e['original']['id'] != 'minecraft:air'
        p = tuple(e['assembly']); seam_cells[tuple(p[i] - SEAM_ORIGIN[i] for i in range(3))] = (air, None)
    outputs['d1_seam_exception.nbt'] = template(SEAM_SIZE, seam_cells)
    for route in ROUTES:
        variant = bp['variants'][route]
        assert variant['d1_overlay'] == {'path': str(D1.relative_to(ROOT)), 'origin': list(D1_ORIGIN), 'sha256': D1_SHA}
        assert sorted(map(tuple, variant['cover_cells'])) == sorted((x, -23, z) for x in range(-9, -5) for z in range(33, 36))
        cells, written, d1_cells = replay(variant, d1)
        for p in seam: cells[p] = air  # step 7: R1-01 exception
        assert signature(variant, cells) == variant['closed_signature'], route + ' replay differs from approved signature'
        entities = {tuple(e['pos']): e['nbt'] for e in variant['block_entities'] if not in_d1(tuple(e['pos']))}
        shell = {}
        for p in written:
            if in_d1(p): continue
            local = tuple(p[i] - LO[i] for i in range(3))
            shell[local] = (cells[p], entities.pop(p, None))
        assert not entities, 'block entity without block: %s' % entities
        # Every unlisted (void) cell of the frame lies above ground: the underground reservation is fully written.
        void = [(x, y, z) for x in range(-40, 41) for y in range(-32, 40) for z in range(-40, 41)
                if (x, y, z) not in written and not in_d1((x, y, z))]
        assert all(p[1] >= 0 for p in void), 'void cell below ground'
        name = 'assembly_%s.nbt' % route.lower()
        outputs[name] = template(SIZE, shell)
        counts = {}
        for state, _ in shell.values(): counts[state['id']] = counts.get(state['id'], 0) + 1
        report[route] = {'listed_cells': len(shell), 'void_cells': len(void), 'states': dict(sorted(counts.items())),
                         'block_entities': [[*(p[i] for i in range(3)), e['nbt']['id']] for e in variant['block_entities']
                                            for p in [tuple(e['pos'])] if not in_d1(p)],
                         'closed_signature': variant['closed_signature']}
    return bp, outputs, report


def manifest(bp, outputs, report):
    def asset(name): return {'resource': 'moreminecrafts:pyramid/st01/' + name[:-4], 'path': str((ASSET_DIR / name).relative_to(ROOT)),
                             'sha256': sha(outputs[name]), 'bytes': len(outputs[name])}
    rm = bp['runtime_metadata']
    pieces = lambda route: [
        {'order': 1, 'piece': 'ASSEMBLY', **asset('assembly_%s.nbt' % route.lower()), 'authored_min_corner': list(LO), 'size': list(SIZE),
         'rotation_pivot': [-LO[0], -LO[1], -LO[2]], 'content': 'all operation-written cells outside the D1 box; unwritten above-ground cells are structure void'},
        {'order': 2, 'piece': 'D1_ROYAL_HALL', 'resource': 'moreminecrafts:pyramid/royal_hall', 'path': str(D1.relative_to(ROOT)), 'sha256': D1_SHA,
         'bytes': D1.stat().st_size, 'authored_min_corner': list(D1_ORIGIN), 'size': list(D1_SIZE),
         'rotation_pivot': [-D1_ORIGIN[0], -D1_ORIGIN[1], -D1_ORIGIN[2]], 'content': 'original D1 bytes, verbatim; placed with St01RoyalChestPlacement.placementSettings'},
        {'order': 3, 'piece': 'D1_SEAM_EXCEPTION', **asset('d1_seam_exception.nbt'), 'authored_min_corner': list(SEAM_ORIGIN), 'size': list(SEAM_SIZE),
         'rotation_pivot': [-SEAM_ORIGIN[0], -SEAM_ORIGIN[1], -SEAM_ORIGIN[2]], 'content': 'R1-01: exactly 12 air cells, applied after D1'}]
    return {
        'status': 'P8-C2-A construction export of the APPROVED G0-R1 blueprint',
        'approved_blueprint': {'commit': APPROVED_COMMIT, 'blueprint_sha256': BLUEPRINT_SHA, 'd1_exceptions_sha256': EXCEPTIONS_SHA,
                               'approval_record': 'docs/design/st01-p8c2a/G0-R1-OWNER-APPROVAL-RECORD.md'},
        'exporter': {'path': 'tools/st01-p8c2a/export_assembly.py', 'sha256': sha(Path(__file__).read_bytes()),
                     'command': 'python3 tools/st01-p8c2a/export_assembly.py', 'data_version': DATA_VERSION,
                     'mm_sg': 'MM-SG exporter not used; MM in-repo generator conventions (lowercase id/properties palette, sorted positions, gzip mtime 0, OS byte 3). MM-SG 61a09eeb is referenced only through the G0-R1 26.3 block-state extract.'},
        'coordinate_contract': {'authored': '+X east, +Z south, +Y up; ground-centre origin; inclusive bounds',
                                'world': 'world(p) = groundOrigin + R(p); every piece: placementOrigin = groundOrigin + authored_min_corner, rotationPivot = -authored_min_corner, same Rotation, Mirror.NONE',
                                'orientation_to_rotation': {'SOUTH': 'NONE', 'WEST': 'CLOCKWISE_90', 'NORTH': 'CLOCKWISE_180', 'EAST': 'COUNTERCLOCKWISE_90'},
                                'orientation_meaning': 'direction the authored main entrance faces after rotation',
                                'access_route': 'independent EAST/WEST enum choosing the assembly piece; selection policy belongs to C2-B'},
        'runtime_metadata': {**rm, 'royal_chest_registration': 'St01RoyalChestPlacement.registerPlaced(level, d1PlacementOrigin, d1Settings) after all three pieces',
                             'completion_bounds_note': 'authored inclusive bounds; rotate with the same transform for a world box'},
        'pieces': {route: pieces(route) for route in ROUTES},
        'variants': {route: {**report[route], 'components': [{k: c[k] for k in ('id', 'kind', 'bounds', 'parent', 'placement_order', 'origin', 'assembly_offset')}
                                                             for c in bp['variants'][route]['components']],
                             'connections': bp['variants'][route]['connections']} for route in ROUTES},
    }


def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--check', action='store_true'); args = parser.parse_args()
    bp, outputs, report = build()
    text = json.dumps(manifest(bp, outputs, report), indent=1) + '\n'
    targets = {ASSET_DIR / n: d for n, d in outputs.items()}; targets[MANIFEST] = text.encode()
    if args.check:
        bad = [str(p.relative_to(ROOT)) for p, d in targets.items() if not p.is_file() or p.read_bytes() != d]
        assert sha(D1.read_bytes()) == D1_SHA
        if bad: sys.exit('committed outputs differ from the exporter: %s' % bad)
        print('PASS: committed assets and manifest match the exporter; D1 unchanged')
    else:
        for p, d in targets.items(): p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(d)
    for n, d in sorted(outputs.items()): print('%-24s %8d bytes sha256 %s' % (n, len(d), sha(d)))
    for r in ROUTES: print(r, 'listed', report[r]['listed_cells'], 'void', report[r]['void_cells'])


if __name__ == '__main__':
    main()
