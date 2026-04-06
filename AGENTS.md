# AGENTS.md

## Project purpose

Build and maintain an honest Android AI call companion. The repository currently targets supported telecom-role integration plus a separate STT/TTS assistant flow. Do not present unsupported carrier-call audio automation as implemented.

## Quick start

```bash
export JAVA_HOME="/Users/heodongun/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home"
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## Install / run / test commands

- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Full local verification: `./gradlew testDebugUnitTest lintDebug assembleDebug`

## Default workflow

1. Inspect repository state and docs first.
2. Keep scope aligned with documented Android telecom limitations.
3. Implement the smallest honest slice.
4. Add or update tests.
5. Update README and `docs/` for meaningful changes.
6. Run build, tests, and lint.
7. Report manual QA status honestly.

## Done definition

- Requested code exists and matches the documented scope.
- Build passes.
- Unit tests pass.
- Lint passes.
- README and docs match the code.
- Manual QA is either executed on hardware or explicitly reported as blocked.

## Code style principles

- Prefer small, named classes over large files with mixed responsibilities.
- Keep telecom, assistant, and speech code separated by package.
- Avoid speculative abstractions.
- Do not hide unsupported behavior behind misleading names.

## File structure principles

- `assistant/`: backend token configuration and reply generation
- `speech/`: device STT/TTS integrations
- `telecom/`: Android Telecom services and policy
- `ui/theme/`: Compose theme only
- `docs/`: architecture, change logs, testing notes

## Documentation principles

- README must stay aligned with real repository behavior.
- Each meaningful change should update at least one `docs/changes/*` entry.
- Limitations belong in docs, not just in chat responses.

## Testing principles

- Add JVM tests for pure Kotlin logic first.
- Only claim device behavior after device execution.
- Treat telecom/media assertions as hardware-dependent unless proven.

## Branch / commit / PR rules

- Work on a feature branch, not `main`.
- Keep commits scoped by purpose.
- Do not create a PR unless a remote exists and the user asked for it.

## Sensitive and caution paths

- `assistant/`: never place long-lived provider secrets here.
- `telecom/`: avoid unsupported assumptions about carrier-call media.
- `README.md` and `docs/`: keep product claims conservative and accurate.

## Pre-work checklist

- Confirm scope from docs and prior verification.
- Confirm Android and JDK tooling availability.
- Confirm whether a device/emulator is available for manual QA.

## Post-work checklist

- Run build, unit tests, and lint.
- Update docs.
- Check git status.
- Report blockers and unverified areas explicitly.

## Never do this

- Do not claim autonomous carrier-call audio handling unless it is actually implemented and device-verified.
- Do not ship long-lived OpenAI/Codex secrets in the app.
- Do not report tests, PRs, or merges that did not happen.
