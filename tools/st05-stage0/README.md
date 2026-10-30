# ST-05 Stage 0 isolated probes

These are feasibility/counterexample probes, **not an ST-05 generator**. No arena
geometry, fit scoring, cavern segmentation or spacing policy is installed.
The Fabric initializer is in the gametest source set and inert unless
`mm.st05.out` is provided. Never enable it in an existing/player world: the
`write` phase intentionally leaves a two-chunk marker fixture incomplete.

## Reproduce

Use Java 25, Minecraft 26.3, Loader 0.19.5, Loom 1.17.21 and Fabric API
0.161.0+26.3 (the branch's unchanged dependency pins).

```sh
python3 tools/st05-stage0/model_probe.py
bash tools/st05-stage0/run_live.sh
```

`run_live.sh` creates fresh uniquely named worlds under `build/st05-stage0/run`
and retains all logs/JSON under the printed `build/st05-stage0-evidence-*` path.
It performs forward generation + first-write save/process halt, a second JVM
on the saved world, then reverse generation in a fresh same-seed world. A failed
assertion is a failed probe; the results must be inspected before reporting.
The runner does not delete worlds. It changes only the dedicated run directory's
server properties and EULA file. The test server uses offline mode and an
ephemeral port; do not expose it to other users.

In this managed environment, use the existing proxy and system trust store,
without disabling certificate verification:

```sh
export JAVA_HOME=/workspace/toolchains/jdk-25.0.4.1
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/workspace/gradle-home
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080"
bash tools/st05-stage0/run_live.sh
```

## Interpretation and limits

- The Python probe explores all six orders of a small complete graph, including
  concurrent independent canonicalization and a JSON round trip. It proves
  neither Minecraft concurrent generation nor disk-crash safety. Growing a
  bounded frontier changes canonical IDs; a late bridge merges components.
- Distance 100 and the A/B/C centers are **synthetic examples**, not gameplay
  settings. First-arrival spacing gives different answers by order. A possible
  explicit priority is lexicographic order of canonical IDs among successful
  rolls, but it is **unapproved and not adopted**. Selecting a policy and its
  roll/spacing denominator requires owner review.
- The real-server scan covers eight FULL chunks crossing x=512 (Anvil region
  boundary), x=480..543, z=0..31, y=-40..-10: 63,488 fixed samples. It hashes
  block states and lists six-neighbor air components. Components touching a
  sampled frontier have unknown continuation; **none are claimed Q09-qualified**.
  No finite scan alone proves globally unique cavern identities.
- The prewritten two-marker fixture plan tests only “plan before writes”. It
  contains no real collision, connection, occupancy or architecture plan.
  The deliberate save/halt after marker A tests whether ordinary block writes
  plus a complete plan alone provide cross-chunk atomicity. It does not prove
  atomicity impossible with a different protocol. Failed plan-file creation is
  detected before marker writes. A recoverable journal/visibility protocol has
  not been implemented or approved.
- elapsed time and heap are observations only; they never choose outputs. Heap
  is used heap at capture, not a peak or per-probe allocation measurement.
- A02 and A03 remain BLOCKER until the handoff's full requirements are proved.
  No Stage 1, worldgen integration, merge or checkpoint promotion is authorized.

The Draft PR is the result/decision record; `results/` contains raw observations.
