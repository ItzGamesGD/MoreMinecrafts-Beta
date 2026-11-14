#!/usr/bin/env python3
"""P8-C3-A geometric evidence: cut/fill maps (PNG) and centre cross-sections (SVG) from archived 81x81 height grids.
Usage: figures.py <results-dir-with-cov-*.json> <out-dir> seed:chunk[:label] ...
Colour: red = natural terrain above the plane (excavated; darker = deeper, scale 0..24),
blue = natural terrain below the plane (assembly body exposed; darker = deeper, scale 0..32), sand = within +-0."""
import base64, glob, json, os, struct, sys, zlib

N, HALF = 81, 40


def png(path, w, h, rows):
    raw = b''.join(b'\x00' + bytes(r) for r in rows)
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def colour(d):
    if d == 0: return (219, 203, 150)
    if d > 0:  # excavated
        t = min(1.0, d / 24.0); return (255, int(200 * (1 - t)), int(200 * (1 - t)))
    t = min(1.0, -d / 32.0); return (int(200 * (1 - t)), int(200 * (1 - t)), 255)


def load(results):
    out = {}
    for f in glob.glob(os.path.join(results, 'cov-*.json')):
        d = json.load(open(f))
        for c in d['candidates']:
            if 'grid_i16le_b64' in c: out[(str(d['seed']), c['chunk'])] = c
    return out


def main():
    results, outdir = sys.argv[1], sys.argv[2]; os.makedirs(outdir, exist_ok=True)
    cands = load(results); index = []
    for spec in sys.argv[3:]:
        seed, chunk, *label = spec.split(':')
        c = cands[(seed, chunk)]
        raw = base64.b64decode(c['grid_i16le_b64']); g = struct.unpack('<%dh' % (len(raw) // 2), raw)
        b = c['policies']['B_40_24_32']['median']; yf = b['yf']
        S = 4; rows = []
        # grid index = (dx+HALF)*N + (dz+HALF); image x = dx (east), image y = dz (south)
        for iz in range(N):
            row = []
            for ix in range(N):
                row += list(colour(g[ix * N + iz] - yf))
            row = [v for i in range(N) for v in row[i * 3:i * 3 + 3] * S]
            rows += [row] * S
        name = f"cutfill-{seed}-{chunk.replace(',', '_')}"
        png(os.path.join(outdir, name + '.png'), N * S, N * S, rows)
        # cross-sections through the centre (row along X at dz=0, column along Z at dx=0)
        def section(vals, title):
            sx = sy = 6  # equal horizontal and vertical scale: 6 px per block
            lo = min(min(vals), yf - 34); hi = max(max(vals), yf + 4)
            W, H = 81 * sx, (hi - lo) * sy + 30
            Y = lambda y: H - 10 - (y - lo) * sy
            pts = ' '.join(f"{i * sx},{Y(v):.1f}" for i, v in enumerate(vals))
            return (f'<g><text x="4" y="14" font-size="12">{title}</text>'
                    f'<rect x="0" y="{Y(yf):.1f}" width="{W}" height="{Y(yf - 32) - Y(yf):.1f}" fill="#d8b56a" fill-opacity="0.55" stroke="#8a6d2f"/>'
                    f'<line x1="0" x2="{W}" y1="{Y(yf):.1f}" y2="{Y(yf):.1f}" stroke="#000" stroke-dasharray="4 3"/>'
                    f'<polyline points="{pts}" fill="none" stroke="#2d6a2d" stroke-width="2"/>'
                    f'<text x="{W - 4}" y="{Y(yf) - 4:.1f}" font-size="11" text-anchor="end">plane Y0 = {yf}; body Y-32..Y0 (tan)</text></g>')
        # sections through the extreme column: the lowest point when the skirt dominates, else the highest point
        ext = min(range(N * N), key=lambda i: g[i]) if b['skirt'] >= b['cut'] else max(range(N * N), key=lambda i: g[i])
        ex, ez = ext // N, ext % N
        rowx = [g[ix * N + ez] for ix in range(N)]
        colz = [g[ex * N + iz] for iz in range(N)]
        def height(vals): return (max(max(vals), yf + 4) - min(min(vals), yf - 34)) * 6 + 30
        h1, h2 = height(rowx), height(colz)
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="486" height="{h1 + h2 + 20}" font-family="sans-serif">'
               f'<rect width="100%" height="100%" fill="#fff"/>'
               f'{section(rowx, f"seed {seed} chunk {chunk}: W-E section through the extreme column (natural surface green, 1 block = 6 px)")}'
               f'<g transform="translate(0,{h1 + 20})">{section(colz, "N-S section through the extreme column")}</g></svg>')
        open(os.path.join(outdir, name + '.svg'), 'w').write(svg)
        index.append(dict(seed=seed, chunk=chunk, label=':'.join(label), yf=yf, cut=b['cut'], skirt=b['skirt'], diff=c['diff'],
                          d_verdict=c['policies']['D_16_10_8']['median']['verdict'], png=name + '.png', svg=name + '.svg'))
    json.dump(index, open(os.path.join(outdir, 'figures.json'), 'w'), indent=1)
    print(json.dumps(index, indent=1))


if __name__ == '__main__':
    main()
