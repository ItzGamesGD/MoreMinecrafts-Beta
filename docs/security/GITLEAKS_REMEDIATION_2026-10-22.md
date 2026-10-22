# Gitleaks historical finding remediation

## Scope and scan method

This report covers the two findings blocking Draft PR #156. The PR was checked through GitHub: it remains open and Draft, and its head is `725159edfacac8d2cbdeef4c6cbd9030839ea2e7`, matching the supplied SHA.

The repository has no separate `SECURITY.md`, `.gitleaks.toml`, or `.gitleaksignore` at the audited head. The security scan policy is defined in `.github/workflows/pre-public-security-scan.yml`.

The workflow checks out full history, fetches all remote branches, tags, and pull request head refs, then runs Gitleaks 8.30.1 with `--all --full-history`. The pinned Linux archive checksum is `551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb`. No custom Gitleaks configuration was active before this change.

The local reproduction fetched 104 remote branch refs, zero tags, and 155 pull request head refs, covering 1,356 reachable commits. The unconfigured scan exited 1 with the two findings listed below. Raw Gitleaks JSON and scanner logs were kept outside the repository and deleted after extracting metadata.

## Finding classifications

| Finding | Rule | Historical source | Classification | Exposure and residual risk |
| --- | --- | --- | --- | --- |
| A | `generic-api-key` | `scripts/free_model_delegation/test_delegation.py`, `aa376713aedcd7fecd769af39e18f8532b9e147d`, line 32 | **Synthetic test fixture.** The 28-character value has mixed case, digits, and punctuation; its structure has a provider-shaped test prefix with explicit test/secret markers followed by an eight-character uppercase suffix. This classification is based on the value structure and usage, not the variable name. | The historical fixture resembles a credential more than necessary, but the test supplies it only through `FakeTransport`; the error-redaction case patches the HTTP transport, and the no-key live-path test rejects every attempted call. No real external authentication request is made by these tests. The fixture remains in reachable public history; no evidence indicates a usable credential, so rotation is not indicated. |
| B | `generic-api-key` | `docs/TERRITORY_REGISTRATION_FABRIC_SPEC.md`, `a1e0aeb0ab8ba90419b714a42502cbe8db9d770c`, line 15 | **Non-secret natural-language text.** The 19-character match is mixed case and includes a slash. It is a prose compound in the opening technical paragraph, not a URL, code literal, hash, credential, or token. | The document describes a shelved territory design. The match is explanatory prose and is not used by code or infrastructure. No credential exposure or rotation is indicated. The prose remains in reachable branch and PR history. |

### Evidence and root cause

Both findings are produced by the default `generic-api-key` heuristic while scanning full history. Finding A is in an offline Python test suite: request construction is exercised with an injected fake transport, all outbound-call paths are mocked or refused, and the fixture is used to check header construction and redaction. Its provider-shaped prefix is unnecessary, but the test file is absent from PR #156's tree, so changing or reintroducing it in this security PR would expand scope. The historical finding is therefore allowlisted by its exact fingerprint.

Finding B is a natural-language phrase in a Markdown paragraph comparing technical code listings. It is not a token mechanism, and the nearby source link is a documentation reference rather than an authentication request or runtime dependency. Editing the technical sentence would add no security value; the exact historical finding is allowlisted.

Finding A's commit is reachable from PR head 141 and the `experiment/cloud-free-model-delegation-phase0` branch. Finding B's commit is reachable from PR heads 132, 133, 134, 136, 137, and 138 and from territory design branches. Neither commit is reachable from a tag. The scan sees these refs because the workflow intentionally scans all remote history.

## Remediation and coverage

`.gitleaksignore` adds one native Gitleaks fingerprint per verified false positive. Each fingerprint pins the exact commit, path, rule, and line. No rule, directory, branch, or global exclusions were added. The workflow, scanner version, exit behavior, and reference coverage are unchanged. A changed line or unrelated `generic-api-key` finding does not match either exception.

The controlled negative check uses an unrelated synthetic API-key fixture with the same pinned Gitleaks version and this ignore file. It must still produce a `generic-api-key` finding; its literal is not stored in this report or repository.

## Validation record

- Baseline full-history scan before the change: **FAIL**, Gitleaks 8.30.1, 1,356 reachable commits, two findings.
- Full-history scan after the exact-fingerprint ignore file: **PASS**, exit 0 with zero findings over the fetched ref set, including the remediation commit.
- Controlled unrelated synthetic finding: **PASS**, one `generic-api-key` finding remained detectable.
- Relevant historical Python test suite: **PASS**, all 23 tests.
- Remote GitHub Actions result: **pending**; local validation alone is not a remote CI pass.

No history was rewritten. No other branch, workflow, gameplay system, or PR #156 branch was changed. The residual risk is limited to the two documented fingerprints; all other findings remain subject to the unchanged scanner rules.
