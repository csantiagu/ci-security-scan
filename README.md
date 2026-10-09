# ci-security-scan

[![Self-test](https://github.com/csantiagu/ci-security-scan/actions/workflows/self-test.yml/badge.svg)](https://github.com/csantiagu/ci-security-scan/actions/workflows/self-test.yml)

A reusable GitHub Actions workflow that runs six security scans on every pull request and once a week,
using free open-source scanners, with one pass/fail check at the end.

```yaml
jobs:
  scan:
    uses: csantiagu/ci-security-scan/.github/workflows/scan.yml@v1
    with:
      maven-dir: backend
      npm-dir: frontend
      image-context: backend
```

## What it catches

| Scan | Tool | Catches | Counts as a finding |
| --- | --- | --- | --- |
| Maven dependencies | [OSV-Scanner](https://github.com/google/osv-scanner) | Known vulnerabilities in the libraries you ship | Any known vulnerability in the runtime classpath (test scope excluded) |
| npm dependencies | `npm audit` | Known vulnerabilities in production packages | `high` or above (configurable); dev dependencies ignored |
| Secrets | [gitleaks](https://github.com/gitleaks/gitleaks) | API keys, tokens and passwords committed to git | Any secret anywhere in the **full history** |
| Code | [Semgrep](https://github.com/semgrep/semgrep) | Injection, XSS, insecure crypto and similar patterns in source | `ERROR`-severity rules (configurable) |
| Container image | [Trivy](https://github.com/aquasecurity/trivy) | Vulnerable OS packages and libraries inside your built image | `HIGH`/`CRITICAL` that have a fix available |
| Config & IaC | Trivy | Misconfigurations in Dockerfiles, Compose, Kubernetes, Terraform | `HIGH`/`CRITICAL` |

Every scan is optional. Leave its input empty (or set it to `false`) to skip it.

## Why it is built this way

- **Every scanner is pinned by digest**, not by tag. A tag can be moved to point at a different image;
  a `sha256` digest cannot. Actions are pinned to commit SHAs for the same reason.
  [Renovate](renovate.json) keeps the pins current and waits 3 days after each release before proposing it.
- **Least privilege.** The scan only gets `contents: read`. Checkouts don't keep the token on disk.
  Inputs reach shell scripts through environment variables, never by direct `${{ }}` interpolation.
- **npm installs nothing.** `npm audit` reads the lockfile without running `npm install`, so no package
  install scripts run. The Maven and image scans *do* run your build (`./mvnw` and `docker build`), as any
  CI would.
- **Never call it from `pull_request_target`.** That trigger runs with your repository's privileges, and the
  Maven and image scans would execute a fork's code under them. Use `pull_request`, as the example does.
- **Nothing about your code leaves the runner.** Semgrep runs with `--metrics=off`; it downloads rules
  but sends nothing back.
- **One required check.** The scan jobs only fail when a scanner breaks. Findings are counted and passed to
  a final `Verdict` job, which writes a summary table and fails the run. Make that job a required status check.
- **A finding and a broken scanner look different.** The verdict says "scanner did not finish" for a tool or
  setup error, so a broken scan never passes silently.
- **Report-only mode for rollout.** Set `fail-on-findings: false` to see what fails before you enforce it.
- **Proven both ways.** The [self-test](.github/workflows/self-test.yml) runs the workflow on every change
  against two demo projects. [`demo/clean`](demo/clean) must pass every scan. [`demo/vulnerable`](demo/vulnerable)
  must be caught by every scan: it has Log4Shell, a vulnerable lodash, an SQL injection, a fake API key,
  an end-of-life base image and a container running as root.

## Usage

Copy [`examples/security-scan.yml`](examples/security-scan.yml) to `.github/workflows/` in your repo, set the
folders, and make `scan / Verdict` a required status check on your default branch.

For the strongest guarantee, pin the workflow to a release commit rather than a tag:

```yaml
uses: csantiagu/ci-security-scan/.github/workflows/scan.yml@<commit-sha> # v1.0.0
```

### Inputs

| Input | Default | Meaning |
| --- | --- | --- |
| `ref` | triggering commit | Git ref to scan, e.g. `develop` for a scheduled run |
| `fail-on-findings` | `true` | `false` reports findings without failing the run |
| `artifact-prefix` | `security-scan` | SARIF artifact prefix; change it if you call the workflow twice in one run |
| `maven-dir` | *(skip)* | Folder with the root `pom.xml`. Uses `./mvnw` when present |
| `java-version` | `21` | JDK used to resolve Maven dependencies |
| `osv-config` | `<maven-dir>/osv-scanner.toml` | OSV-Scanner accepted vulnerabilities |
| `npm-dir` | *(skip)* | Folder with `package.json` and `package-lock.json` |
| `node-version` | `24` | Node.js used for `npm audit` |
| `npm-audit-level` | `high` | Lowest severity counted: `low`, `moderate`, `high` or `critical` |
| `secrets-scan` | `true` | Scan the full git history with gitleaks |
| `gitleaks-config` | `.gitleaks.toml` | gitleaks config, used when present |
| `gitleaks-ignore` | `.gitleaksignore` | Reviewed gitleaks findings, used when present |
| `code-scan` | `true` | Scan source with Semgrep |
| `source-dir` | `.` | Folder covered by the Semgrep and Trivy config scans |
| `semgrep-config` | `p/default` | Space-separated [rule packs](https://semgrep.dev/explore), e.g. `p/java p/typescript p/owasp-top-ten` |
| `semgrep-severity` | `ERROR` | Lowest rule severity run: `INFO`, `WARNING` or `ERROR` |
| `image-context` | *(skip)* | Docker build context for the image scan |
| `image-dockerfile` | `<image-context>/Dockerfile` | Dockerfile for the image scan |
| `config-scan` | `true` | Scan Dockerfiles, Compose, Kubernetes and IaC with Trivy |
| `trivy-ignore` | `.trivyignore.yaml` | Accepted Trivy findings (image and config), used when present |

### Outputs

`maven-findings`, `npm-findings`, `secrets-findings`, `code-findings`, `image-findings` and
`config-findings`: the number of findings per scan, empty when that scan was skipped. Use them in a later job,
for example to post a comment or open an issue.

### Results in the Security tab

Add the `upload-sarif` job from the [example](examples/security-scan.yml) to send findings to GitHub code
scanning, where they show in the **Security** tab and as annotations on pull requests. It is a separate
workflow because it needs `security-events: write`, and the scan itself should not need that permission.
Code scanning is free for public repos; private repos need GitHub Code Security.

## Accepting a finding

Sometimes a finding is not exploitable in your project, or there is no upgrade yet. Record it instead of
turning the scan off, and **always give a reason**. The OSV and Trivy formats also take an expiry date: when it
passes, the finding fails the build again, so an exception cannot be forgotten. Use one.

| Scan | File | Example |
| --- | --- | --- |
| Maven | `osv-scanner.toml` next to `pom.xml` | [osv-scanner.toml](examples/accepted-findings/osv-scanner.toml) |
| Image and config | `.trivyignore.yaml` at the repo root | [.trivyignore.yaml](examples/accepted-findings/.trivyignore.yaml) |
| Secrets (one finding) | `.gitleaksignore` at the repo root | [.gitleaksignore](examples/accepted-findings/.gitleaksignore) |
| Secrets (a pattern) | `.gitleaks.toml` at the repo root | [.gitleaks.toml](examples/accepted-findings/.gitleaks.toml) |
| Code (paths) | `.semgrepignore` at the repo root | [.semgrepignore](examples/accepted-findings/.semgrepignore) |
| Code (one line) | a `// nosemgrep: <rule-id>` comment on the line | |
| npm | upgrade, or add an [`overrides`](https://docs.npmjs.com/cli/configuring-npm/package-json#overrides) entry in `package.json` | |

A real leaked secret must be **rotated**. Deleting it in a new commit leaves it in the history, which is
exactly what the secrets scan reads.

## Limitations

- Dependency scanning covers **Maven** and **npm**. The code, secrets, image and config scans work for any language.
- Runs on GitHub-hosted Linux runners (it needs Docker).
- The Semgrep and Trivy scans download rules and vulnerability databases at run time, so the runner needs internet access.

## License

[MIT](LICENSE)
