# Contributing to Elite Dangerous Warboard

Thank you for your interest in contributing to Elite Warboard. This document provides guidelines to help you get started.

## Code of Conduct

By participating in this project, you agree to uphold our [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

### Reporting Bugs

- Use the [Bug report](https://github.com/mirooz/elite-dashboard/issues/new?template=bug_report.md) issue template.
- Include your environment (OS, Java version, application version).
- Describe the steps to reproduce and the expected vs actual behavior.
- If relevant, attach journal excerpts or logs (redact personal data if needed).

### Suggesting Features or Changes

- Use the [Feature request](https://github.com/mirooz/elite-dashboard/issues/new?template=feature_request.md) issue template.
- Describe the use case and how it fits with the existing app (missions, mining, exploration).

### Pull Requests

1. **Fork** the repository and create a branch from `main` (or the current default branch).
2. **Build and test** locally:
   ```bash
   mvn clean install
   ```
   For the main app:
   ```bash
   cd elite-warboard-missions && mvn exec:java
   ```
3. Follow the existing code style (Java 17, same formatting as the rest of the project).
4. Keep commits focused and messages clear.
5. Open a Pull Request and fill in the [pull request template](.github/PULL_REQUEST_TEMPLATE.md).
6. Link any related issues (e.g. "Fixes #123").

## Development Setup

- **JDK 17** with **JavaFX 17** (e.g. [Liberica JDK 17 Full](https://bell-sw.com/pages/downloads/)).
- **Maven** 3.6+.
- Clone the repo and run `mvn clean install` from the root.

## Project Structure

- `elite-warboard-missions/` – main JavaFX application (dashboard, handlers, views).
- `elite-commons/` – shared utilities.
- `elite-clients/` – API clients (EdTools, Ardent, etc.).
- `journal-analyzer/` – journal analysis tool.
- `bioforge-biodatas/` – exobiology/bioforge data.

When adding features, put code in the appropriate module and follow existing patterns (handlers in `handlers/`, models in `model/`, views in `view/`, etc.).

## Translations

The UI is bilingual (English and French). When adding or changing user-facing strings:

- Update `elite-warboard-missions/src/main/resources/messages_en.properties`
- Update `elite-warboard-missions/src/main/resources/messages_fr.properties`

Use the same key in both files.

## Questions

If you have questions that are not bugs or feature requests, you can open a [GitHub Discussion](https://github.com/mirooz/elite-dashboard/discussions) (if enabled) or an issue with the "question" label.

Thank you for contributing.
