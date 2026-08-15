
Agents and Contribution Notes for JPrecise

The development container is the canonical development environment.
Build, test and development commands should be executed inside the
container whenever possible.

Purpose
- JPrecise is a technical proof-of-concept (POC) to investigate whether Android can provide a custom, fine-grained volume controller for Bluetooth headphones.

Target hardware and Android version
- Motorola Edge 50 Pro (XT2403-2)
- Target device software: Android 16 (API level 36)

Motivation
- Android's native volume steps can be too coarse at very low listening levels. The POC explores finer-grained steps, custom overlays, and non-linear volume curves for extremely quiet listening.

Guidelines
- This is an experimental POC, not production software.
- Prefer Java (no Kotlin) and standard Android SDK APIs.
- Avoid unnecessary dependencies and frameworks; keep the project small and incremental.
- Verify Android behavior experimentally on target hardware rather than assuming platform behavior.
- Make small, incremental changes with clear tests or manual verification steps.

Testing and Verification
- Tests are important throughout the project. Unit tests should be added where appropriate.
- Integration tests and build/infrastructure verification are especially important during the early POC phase.
- A configuration should not be considered complete until it has actually been exercised and verified.
- Prefer small, incremental changes followed by build/test verification.
- AI agents should fix problems discovered during verification rather than merely reporting them.

Development practices
- Keep the project simple and avoid unnecessary frameworks or dependencies.
- Use Java and standard Android SDK APIs unless there is a compelling reason otherwise.
- Verify assumptions on real devices (the target is Android 16 / API 36).
- Make small, testable commits: commit when a coherent piece of work is completed and verified.
- Include only relevant files in commits, but make sure all relevant files are committed


Notes
- Keep work focused on exploration and measurement; suspend implementing full volume-control logic until experimental behaviors are understood.

Devcontainer / Dockerfile notes
- **Purpose:** The devcontainer Dockerfile was reorganized to improve Docker layer caching and speed up incremental rebuilds during development.
- **Key changes:** stable, expensive steps (system packages, Gradle, Android SDK and platform tools) are now early, cacheable layers; user creation and workspace ownership are done late so source changes do not invalidate heavy layers.
- **UID/GID alignment:** The Dockerfile accepts `USER_UID` and `USER_GID` build arguments so the `dev` user inside the container can match your host UID/GID to avoid file permission issues when mounting the workspace.
- **Build example:**

	```bash
	docker build -t jprecise-devcontainer .devcontainer \
		--build-arg USER_UID=$(id -u) --build-arg USER_GID=$(id -g)
	```

- **Run example (build inside container):**

	```bash
	docker run --rm -v "$PWD":/workspace -w /workspace jprecise-devcontainer \
		bash -lc "./gradlew --no-daemon assembleDebug --stacktrace"
	```

- **Rationale:** Keeping SDK and toolchain installation in cacheable layers reduces rebuild time; delaying workspace copy/user setup prevents frequent small changes (source edits) from invalidating the expensive setup layers.

