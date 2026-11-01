#!/usr/bin/env python3
"""G-4 preparation and integrity controls. No freeze/pin creation operation.

prepare writes a CANDIDATE inventory; verify never regenerates anything.
protected runs a future command only inside an already enforced read-only sandbox.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
BASE = 'tools/st05-p0-probe-01'
INVENTORY = BASE + '/freeze-inventory-candidate.json'
AUTHORITIES = [
    'AGENTS.md', 'docs/CONTENT_AUTHORITY_INDEX.md', 'docs/STRUCTURE_AUTHORITY_INDEX.md',
    'docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md',
    *['docs/design/' + name + '.md' for name in (
        'ST05_STAGE0_INTEGRATED_VALIDATION_PLAN', 'ST05_STAGE0_P0_ORACLE_MANIFEST',
        'ST05_STAGE0_P0_API_MAPPING', 'ST05_STAGE0_P0_FEASIBILITY_GAPS',
        'ST05_WORLDGEN_OWNER_DECISIONS_Q01_Q18_2026-10-30',
        'ST05_STAGE0_FIX_OWNER_DECISIONS_2026-10-31',
        'ST05_STAGE0_P0_PROBE_01_EXECUTION_SPEC', 'ST05_STAGE0_P0_PROBE_01_BINDING_PLAN',
        'ST05_STAGE0_P0_PROBE_01_G1G2_PREP')]]
EXECUTION = [BASE + '/' + n for n in (
    'inputs-v1.json', 'schedules-v1.json', 'controls-v1.json', 'runplan-v1.json',
    'expected-traces-v1.json', 'oracle/goldens-v1.json')]
EXECUTION += [BASE + '/invalid-inputs-v1/I%03d.json' % i for i in range(1, 30)]
PROVENANCE = [BASE + '/' + n for n in (
    'fixture-index-v1.tsv', 'materialize.py', 'verify_materialization.py',
    'CANDIDATE-DIGESTS.sha256', 'oracle/CANDIDATE-GOLDEN-DIGESTS.sha256',
    'oracle/oracle.py', 'oracle/negatives.py', 'oracle/tier_a.py', 'oracle/loader_rules.py',
    'oracle/test_corrections.py', 'freeze_integrity.py', 'test_freeze_integrity.py')]
PROVENANCE += AUTHORITIES


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def files(root):
    """Reject symlinks/special files, including directories linked outside scope."""
    found = set()
    for path in (root / BASE).rglob('*'):
        if path.is_symlink() or not (path.is_file() or path.is_dir()):
            raise ValueError('non-regular artifact: ' + str(path))
        if path.is_file():
            found.add(path.relative_to(root).as_posix())
    return found - {INVENTORY}


def prepare(root):
    if (root / BASE / "pins-v1.json").exists():
        raise ValueError("freeze creation and candidate preparation are separate")
    expected = set(EXECUTION + PROVENANCE)
    if files(root) != expected - set(AUTHORITIES):
        raise ValueError('tool inventory mismatch: ' + str(files(root) ^ (expected - set(AUTHORITIES))))
    entries = []
    for name in sorted(expected):
        path = root / name
        if path.is_symlink() or not path.is_file():
            raise ValueError('missing/nonregular inventory entry: ' + name)
        entries.append({'path': name, 'group': 'execution_reference' if name in EXECUTION else 'provenance_reproducibility',
                        'bytes': str(path.stat().st_size), 'sha256': sha(path)})
    data = {'schema': 'P01-FREEZE-INVENTORY-1', 'status': 'CANDIDATE',
            'coverage': 'Exact tool subtree plus explicitly listed authority files; inventory self-excluded and externally anchored at future G-4',
            'files': entries}
    (root / INVENTORY).write_text(json.dumps(data, indent=2, sort_keys=True) + '\n')


def verify(root, anchor=None):
    manifest = root / INVENTORY
    if manifest.is_symlink() or not manifest.is_file():
        raise ValueError('missing/nonregular inventory')
    if anchor is not None and sha(manifest) != anchor:
        raise ValueError('inventory anchor mismatch')
    data = json.loads(manifest.read_text())
    entries = data['files']
    names = [e['path'] for e in entries]
    if len(set(names)) != len(names) or set(names) != set(EXECUTION + PROVENANCE):
        raise ValueError('inventory differs from explicit contract')
    if files(root) != set(names) - set(AUTHORITIES):
        raise ValueError('missing/extra tool artifacts')
    for e in entries:
        p = root / e['path']
        if any(parent.is_symlink() for parent in [p, *p.parents]):
            raise ValueError('symlink: ' + e['path'])
        if not p.is_file() or str(p.stat().st_size) != e['bytes'] or sha(p) != e['sha256']:
            raise ValueError('missing/altered: ' + e['path'])
    return len(entries)


def require_ro(root):
    """Fail closed; never attempts a mount or permission downgrade."""
    status = dict(line.split(':', 1) for line in Path('/proc/self/status').read_text().splitlines() if ':' in line)
    for cap in ('CapEff', 'CapPrm', 'CapBnd'):
        if int(status[cap].strip(), 16) & (1 << 21):
            raise ValueError('NOT_ENFORCED: CAP_SYS_ADMIN ' + cap)
    root = root.resolve()
    # statvfs covers each inventoried artifact, and every nested directory.
    paths = [root, *(root / BASE).rglob('*'), *(root / p for p in AUTHORITIES)]
    for path in paths:
        if not os.statvfs(path).f_flag & os.ST_RDONLY:
            raise ValueError('NOT_ENFORCED: writable filesystem ' + str(path))
    def decode(value):
        for a, b in (('\\040', ' '), ('\\011', '\t'), ('\\012', '\n'), ('\\134', '\\')):
            value = value.replace(a, b)
        return value
    mounts = [line.split() for line in Path('/proc/self/mountinfo').read_text().splitlines()]
    containing = [m for m in mounts if root == Path(decode(m[4])) or Path(decode(m[4])) in root.parents]
    relevant = [max(containing, key=lambda m: len(m[4]))]
    relevant += [m for m in mounts if root in Path(decode(m[4])).parents]
    if any('ro' not in m[5].split(',') for m in relevant):
        raise ValueError('NOT_ENFORCED: writable enclosing/nested mount')


def protected(root, anchor, command):
    if not anchor or not command:
        raise ValueError('external inventory anchor and command required')
    require_ro(root)
    verify(root, anchor)
    try:
        return subprocess.run(command, check=False).returncode
    finally:
        require_ro(root)
        verify(root, anchor)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('operation', choices=['prepare', 'verify', 'protected'])
    ap.add_argument('--root', type=Path, default=ROOT)
    ap.add_argument('--anchor')
    argv = sys.argv[1:]
    split = argv.index('--') if '--' in argv else len(argv)
    command = argv[split + 1:]
    args = ap.parse_args(argv[:split])
    if args.operation == 'prepare':
        prepare(args.root)
    elif args.operation == 'verify':
        print('CANDIDATE inventory verified:', verify(args.root, args.anchor))
    else:
        return protected(args.root, args.anchor, command)
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, OSError, KeyError) as error:
        print('BLOCKED:', error, file=sys.stderr)
        sys.exit(3)
