# Security Policy

## Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.3.x   | :white_check_mark: |
| < 1.3   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Elite Warboard, please report it responsibly:

1. **Do not** open a public GitHub issue for security vulnerabilities.
2. Open a [GitHub Security Advisory](https://github.com/mirooz/elite-dashboard/security/advisories/new) (recommended), or contact the maintainers privately if you prefer.
3. Include a clear description of the vulnerability, steps to reproduce, and potential impact.
4. Allow a reasonable time for a fix before any public disclosure.

We will acknowledge your report and keep you informed of the progress. We appreciate your help in keeping Elite Warboard and its users safe.

## What we consider in scope

- Remote code execution or privilege escalation in the application
- Data exposure (e.g., journal files, API keys, or user data handled by the app)
- Issues in dependencies that directly affect the security of the application

## Out of scope

- Issues in third-party APIs (EdTools, Ardent, SiriusCorp, Spansh, Canonn Bioforge) unless they are caused by misuse in our client code
- General best-practice recommendations without a concrete vulnerability
