# File Claw - Development Rules & Guidelines
Last Updated: June 15, 2026

These rules MUST be strictly followed by any developer or AI assistant working on the File Claw project before and during any task.

## 1. Mandatory Context Gathering (Pre-Task)
Before making ANY changes or starting a new feature, you MUST read and understand the project context by reviewing the following files:
- `project-blueprint.md`
- `rules.md` (This file)
- `project-mindmap.md`
- `project-chart.md`
- `tech-specs.md`
- `changelogs.md`
- `backup-manager.md`
- `security-protocol.md`
- `testing-checklist.md`
- `known-issues.md`

This ensures full awareness of the app's architecture, dependencies, limitations, and current state. Never guess the structure without reading these first.

## 2. Source Code Backup Protocol (Task Initialization)
Before initiating any code changes for a new task:
1. Create a full backup of the current working source code.
2. Store the backup in a dedicated `/backup` folder at the root of the project.
3. Save the backup as a versioned ZIP file (e.g., `backup_v1.0.1.zip`).
4. **Context File**: Inside the backup folder (or zip), include a text file summarizing exactly what was completed up to this version.
5. **Never delete old backups.** Always accumulate new backups so that restoring to any previous state is easy.
6. **Log it**: Update `backup-manager.md` to reflect the newly created backup.
*(Note for AI Agents: If your environment restricts shell file-compression commands like `zip` or `tar`, instruct the user to use the platform's "Export as ZIP" feature, or simulate the backup logically via Git or copy commands if available).*

## 3. The Implementation Loop (Code -> Test -> Fix)
For any user feature request or bug fix, strictly follow this loop:
1. **Implement**: Make the requested code changes carefully.
2. **Test**: You MUST test the changes immediately. (e.g., run `compile_applet` and verify successful build).
3. **Fix**: If any problem, error, or build failure occurs, diagnose and fix it immediately.
4. **Iterate**: Repeat the Test/Fix loop continuously. Do NOT stop until the code works perfectly and the user gets the desired output.

## 4. Documentation Upkeep (Post-Task)
Documentation is a living entity. Whenever a change is made to the codebase (adding a feature, fixing a bug, updating a dependency):
- You MUST update all relevant documentation files (`project-blueprint.md`, `project-mindmap.md`, `project-chart.md`, etc.) immediately to reflect the new state.
- Never allow the code and the documentation to drift out of sync.

## 5. Strict Changelog Protocol
- Every single modification must be recorded in `changelogs.md`.
- Entries must be organized date-wise.
- Follow the exact formatting guidelines specified in the changelog document.

## 6. Missing Rules Added for Safety (AI Best Practices)
- **Read Before Edit (No Guessing)**: Always view the exact contents of a target file before attempting to write or edit it. Never assume you know the line numbers.
- **Micro-Verifications**: Don't wait until the end of a massive rewrite to test. Run build checks (`compile_applet`) after changing complex configurations or major files.
- **Architecture Strictness**: Always follow the MVVM + Repository pattern outlined in the blueprints. Do not bypass the Data layer for a quick UI hack.
