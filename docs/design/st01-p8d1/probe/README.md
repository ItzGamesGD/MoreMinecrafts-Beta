# Read-only locator feasibility probe (evidence only)

`St01LocateProbe.java.txt` is the probe source (kept as text; **it is not compiled or registered in this branch**). It was run
locally once, uncommitted, through the existing `runSt01C2BProbe` Gradle run configuration with seed 1004, radius parameter 20, on the
P8-C2-C branch, with the entrypoint temporarily listed in the gametest `fabric.mod.json`. `locate-seed1004-radius20.json` is its raw output.
It changes no production behavior and was removed from the tree afterwards.

Limits: one seed, one machine (the cloud container, not the CI runners), one cold run per origin (no JIT warm-up control, no repeats of
the cold call), radius 20 only (radius 100, the vanilla explorer-map value, was **not** measured), ground truth enumerated only inside ±10,240 blocks.
