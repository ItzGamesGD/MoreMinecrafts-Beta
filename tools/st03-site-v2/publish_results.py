"""Keep compact, complete numerical evidence readable through GitHub Checks as well as artifacts."""
import csv
import json
import math
import os
from pathlib import Path
import subprocess
import sys


def distribution(values):
    a = sorted(values)
    return {'n': len(a), **{f'p{p:02}': a[max(0, math.ceil(len(a)*p/100)-1)] if a else None for p in (5,25,50,75,95)}, 'max': max(a) if a else None}


def enrich(folder, data):
    path = folder / 'candidates.csv'
    populations = {}
    if path.exists():
        rows = list(csv.DictReader(path.open()))
        for s in range(3):
            for b in (70, 80):
                for w in (0, 5):
                    eligible = [r for r in rows if int(r['center_set']) <= s
                        and int(r[f'good_s{s}'])*100 >= b*(int(r['biome_n'])-int(r['edge']))
                        and int(r['height_n']) > 0 and int(r['water'])*100 <= w*int(r['height_n'])]
                    populations[f'S{s}_B{b}_W{w}'] = {
                        'raw': distribution([int(r['max'])-int(r['min']) for r in eligible]),
                        'p05_p95': distribution([int(r['p95'])-int(r['p05']) for r in eligible]),
                    }
    legacy = folder / 'legacy-height.csv'
    if legacy.exists():
        rows = list(csv.DictReader(legacy.open()))
        data['legacy_height_population'] = {
            'raw': distribution([int(r['max'])-int(r['min']) for r in rows]),
            'p05_p95': distribution([int(r['p95'])-int(r['p05']) for r in rows]),
        }
    data['height_populations'] = populations
    for baseline in data.get('baselines', []):
        baseline.pop('starts', None)  # complete start coordinates remain in the artifact
    data.pop('legacy_resolution_pairs', None)  # paired resolutions remain in the artifact
    data.pop('configs', None)  # configuration IDs + limits fully specify the table
    return data


def post(payload):
    result = subprocess.run(['gh', 'api', '-X', 'POST', f"repos/{os.environ['GITHUB_REPOSITORY']}/check-runs", '--input', '-'],
        input=json.dumps(payload), text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr[-1000:])
    return json.loads(result.stdout)['id']


if __name__ == '__main__':
    folder = Path(sys.argv[1])
    p = folder / 'summary.json'
    if not p.exists():
        p = folder / 'measurement.json'
    data = enrich(folder, json.loads(p.read_text())) if p.exists() else {'status':'FAIL', 'reason':'No probe JSON'}
    data['head_sha'] = os.environ['GITHUB_SHA']
    data['run_id'] = os.environ['GITHUB_RUN_ID']
    text = json.dumps(data, ensure_ascii=False, separators=(',', ':'))
    # Chunk without truncation, so even unusually large diagnostic payloads are retrievable.
    chunks = [text[i:i+50000] for i in range(0,len(text),50000)]
    for i, chunk in enumerate(chunks):
        post({'name':f"st03-v2-results-{sys.argv[2]}-{i+1}", 'head_sha':os.environ['GITHUB_SHA'],
              'status':'completed', 'conclusion':'success' if data.get('status')=='PASS' else 'failure',
              'output':{'title':f"ST03 V2 seed {sys.argv[2]} part {i+1}/{len(chunks)}",
                        'summary':f"Run {os.environ['GITHUB_RUN_ID']} — measurement {data.get('status')} — natural placement is reported separately",
                        'text':chunk}})
