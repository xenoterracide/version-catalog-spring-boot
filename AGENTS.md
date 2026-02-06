<!--
SPDX-FileCopyrightText: Copyright © 2026 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-4.0
-->

- use commit-or-pr-message skill for commit messages
- `yarn test` must pass if `*.kts`, `*.java`, or `checkstyle/*.xml` has changed
- Keep Gradle lockfiles current:
  - `yarn ug` (fast) or `yarn ug:scan` (with build scan)
- Use `yarn merge` (Make: `make merge`) for the end-to-end merge/PR workflow
