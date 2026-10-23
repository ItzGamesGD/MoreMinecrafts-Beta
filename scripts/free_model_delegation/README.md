# Free-model delegation (Phase 0 experiment)

A manually triggered GitHub Actions workflow that sends one read-only analysis
request to an OpenRouter **free** model and records the result. It is an
experiment only: it does not touch gameplay code, production workflows, or `main`.

Files:

- `openrouter_client.py`: stdlib-only OpenRouter client. Eligible models are `:free` ids with zero catalog pricing. Each run sends one chat request, with no retries and no fallback model list.
- `run_delegation.py`: the CLI. `--mode mock` runs offline. `--mode live` sends one request if `OPENROUTER_API_KEY` is set, and reports `blocked_no_key` if not.
- `test_delegation.py`: mocked unit tests. No network and no key needed.
- `../../.github/workflows/free-model-delegation.yml`: the manual workflow.

## Owner setup (mobile-friendly, about 5 minutes)

Do this in a mobile browser in desktop-site mode if the GitHub app does not show these screens.

1. Create an OpenRouter account at openrouter.ai and create an API key. Use only free models. Do **not** add credits or enable any paid model. The client refuses paid or unknown-price models, but the account should still have no balance.
2. In GitHub, open the repository **Settings → Secrets and variables → Actions → New repository secret**. Name it exactly `OPENROUTER_API_KEY` and paste the key. Never paste the key into chat, a PR, or a file.
3. Open **Actions → Free model delegation (experiment) → Run workflow**. Pick the branch `experiment/cloud-free-model-delegation-phase0`, and choose `mode`:
   - `mock`: offline check of the pipeline. No key needed, no request sent.
   - `live`: one request to one free model.
4. When the run finishes, open the run page. The job summary shows status, model, latency, token usage, and the response. The `free-model-delegation-result` artifact holds `result.json` and `summary.md` for 7 days.

Notes:

- Only people with write access can run the workflow. Anyone who can push to a branch can change the workflow file on that branch, so the secret is effectively limited to repository writers. Do not merge or dispatch untrusted branches.
- `workflow_dispatch` only works for workflow files that exist on the default branch. The workflow must be merged to `main` before it can be dispatched from this branch. Merging is an owner decision and is not part of this PR.

## Local mock run

```bash
python3 -m unittest discover -s scripts/free_model_delegation -p "test_*.py" -v
python3 scripts/free_model_delegation/run_delegation.py --mode mock --out /tmp/fmd-mock
```

## Cost guard

- One chat request per run. No retries and no automatic escalation.
- Only `:free` models with zero catalog prices are eligible.
- The request names one model. There is no `models` or fallback list.
- A response that names a different model, or reports a non-zero `usage.cost`, is recorded as `error_cost_guard`.
- The request contains only a synthetic 5-line Python snippet defined in `run_delegation.py`. It contains no repository content.
