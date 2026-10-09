# Security policy

## Reporting a vulnerability

Please report security problems privately through
[GitHub private vulnerability reporting](https://github.com/csantiagu/ci-security-scan/security/advisories/new),
not in a public issue. Include the workflow input that triggers it and what an attacker could do.

You should get a reply within a week.

## Scope

In scope: the workflows in `.github/workflows/`, for example a way to make a scan pass when it
should fail, to run code from a pull request with elevated permissions, or to leak a secret into logs.

Out of scope: vulnerabilities in the scanners themselves (OSV-Scanner, gitleaks, Semgrep, Trivy).
Report those to their projects. The `demo/vulnerable/` folder is vulnerable on purpose.
