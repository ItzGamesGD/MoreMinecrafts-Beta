"""P3.11: per-theme apply / revert rehearsal on a COPY of the live worldgen JSONs (production pools never touched).
Checks: changed-file lists, other theme untouched, idempotent apply/revert, dry-run, refusal on a tampered pool, mixed order, SHA-256 == worldgen_baseline.sha256."""
import hashlib, os, shutil, subprocess, sys, tempfile
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
BASE = {l.split()[1]: l.split()[0] for l in open(f'{ROOT}/tools/heaven-p38/worldgen_baseline.sha256') if l.strip()}
tmp = tempfile.mkdtemp(); rel = 'src/main/resources/data/moreminecrafts'
for sub in ('worldgen', 'tags/worldgen'):
    shutil.copytree(f'{ROOT}/{rel}/{sub}', f'{tmp}/{rel}/{sub}')
env = dict(os.environ, HEAVEN_P38_LIVE=f'{tmp}/{rel}/worldgen/template_pool/sky_islands')
sha = lambda p: hashlib.sha256(open(p, 'rb').read()).hexdigest()
def snap(): return {k: sha(f'{tmp}/{k}') for k in BASE}
def run(*a, ok=True):
    r = subprocess.run([sys.executable, f'{ROOT}/tools/heaven-p38/pending_pools.py', *a], env=env, capture_output=True, text=True)
    assert (r.returncode == 0) == ok, (a, r.stdout, r.stderr); return r
def diff(a, b): return sorted(k.split('sky_islands/')[1] for k in a if a[k] != b[k])
pool = lambda t: sorted(f'{t}/{p}.json' for p in ('satellite', 'start', 'tower_top'))
fails = []
def check(name, cond, detail=''): print(('PASS ' if cond else 'FAIL ') + name, detail); cond or fails.append(name)
orig = snap(); check('copy == baseline (58 files)', orig == {k: v for k, v in BASE.items()})
check('usage error without theme', run('apply', ok=False).returncode != 0)
check('bad theme refused', run('apply', 'snowy', ok=False).returncode != 0)
s0 = snap(); run('apply', 'forest', '--dry-run'); check('dry-run changes nothing', snap() == s0)
for sel, exp in (('meadow', pool('meadow')), ('forest', pool('forest')), ('all', pool('forest') + pool('meadow'))):
    run('apply', sel); a1 = snap(); check(f'apply {sel}: changed files', diff(orig, a1) == sorted(exp), str(diff(orig, a1)))
    run('apply', sel); check(f'apply {sel} twice: byte-identical', snap() == a1)
    run('revert', sel); r1 = snap(); check(f'revert {sel}: back to baseline', r1 == orig)
    run('revert', sel); check(f'revert {sel} twice: still baseline', snap() == orig)
run('apply', 'meadow'); run('apply', 'forest'); run('revert', 'meadow')
check('mixed: meadow reverted, forest stays applied', diff(orig, snap()) == pool('forest'), str(diff(orig, snap())))
run('revert', 'forest'); check('mixed: all reverted == baseline', snap() == orig)
t = f'{tmp}/{rel}/worldgen/template_pool/sky_islands/forest/start.json'; open(t, 'a').write(' ')
s0 = snap(); r = run('apply', 'all', ok=False); check('tampered pool: apply refused, nothing changed (even for meadow)', snap() == s0 and 'REFUSED' in (r.stderr + r.stdout))
r = run('revert', 'all', ok=False); check('tampered pool: revert refused, nothing changed', snap() == s0)
shutil.rmtree(tmp)
print('production tree == baseline:', all(sha(f'{ROOT}/{k}') == v for k, v in BASE.items()))
sys.exit(1 if fails else 0)
