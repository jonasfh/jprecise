# Agents and Contribution Guide for JPrecise

This document is the canonical entry point and rulebook for AI coding assistants (Gemini, Antigravity, etc.) and human contributors working in this repository.

---

## 1. Mandatory Agent Workflow, GitHub Issues & Git Policy

- **GitHub Issue First:** For all non-trivial changes, create an issue in GitHub (via GitHub MCP / CLI) describing the objective and scope before starting work.
- **Implementation Plan in Issue Comments:** Before starting code implementation on an issue, the agent **MUST create a structured implementation plan and post it as a comment** directly on the GitHub issue.
- **Branching Workflow:**
  - Never work directly on `main` for non-trivial tasks.
  - Before creating a branch, ensure `main` is up-to-date with `origin` (`git fetch origin` / fast-forward pull).
  - Create and switch to a dedicated feature branch named `<issuenr>-<description>`, e.g., `8-low-level-volume-resolution`.
  - Standard practice is 1 branch per issue (multiple issues can be addressed in the same branch if closely related or needed).
- **Documenting Findings & Outcomes:**
  - Upon completing a spike, investigation, or feature, update `AGENTS.md` (and `README.md` where applicable) with important technical findings, architecture decisions, and platform limitations.
  - Post a completion summary comment on the GitHub issue detailing what was implemented and verified.
- **Commit Upon Completion:** When a coherent task, fix, or feature is completed and verified, the AI agent **MUST proactively create a Git commit**. Do not leave working, verified changes uncommitted unless explicitly instructed by the user.
- **Verification Before Commit (Local Testing Only):**
  - When actual code files are modified, run `./gradlew test assembleDebug` locally before creating a commit.
  - Verification builds/tests can be skipped if changes only affect non-code files (e.g., documentation, markdown files, etc.).
  - Fix any build warnings, compile errors, or lint failures discovered during verification immediately before committing.
  - **Do NOT poll or wait for remote GitHub Actions CI:** Run all tests locally. After pushing and opening a PR, report back immediately to the user without polling remote CI status (the developer checks CI before merge).
- **Atomic and Relevant Commits & Message Format:**
  - Stage only relevant modified/new files (`git add <files>`).
  - Write concise, descriptive commit messages describing the *what* and *why*.
  - **Mandatory Issue Prefix:** When a commit refers to a GitHub issue, the commit message **MUST always start with `(#<issue-nr>)`**, e.g., `(#8) Increase volume resolution at low level` or `(#4) Clarify commit message format in AGENTS.md`.
- **Pull Request Creation:**
  - Whenever a feature branch is pushed to GitHub, create a Pull Request (PR) targeting `main` with a clear description linking back to the relevant issue.
  - The PR remains open until the developer explicitly requests it to be merged/rebased, or performs the merge manually.
  - **No Merge on Failing CI:** Never merge or rebase a PR if GitHub Actions or CI checks have failed.
- **Devcontainer Environment:** All builds, tests, and CLI operations must run inside the devcontainer.

---

## 2. Project Purpose & Technical Context

- **Goal:** Technical proof-of-concept (POC) to investigate whether Android can provide a custom, ultra-fine-grained volume controller (especially at very low listening levels for sleep/quiet environments) for both device speakers and Bluetooth headphones.
- **Target Hardware:** Motorola Edge 50 Pro (XT2403-2)
- **Target Android Version:** Android 16 (API Level 36)
- **Primary Language:** Java (no Kotlin) and standard Android SDK APIs. Keep the project lightweight and avoid heavy third-party frameworks.
- **Exploratory Phase:** Keep work focused on exploration, measurement, and experimentation before designing complex UI or full control architectures.

---

## 3. Skills and Customizations

Antigravity and other agent runtimes discover customizations hierarchically in this workspace:

- **Rules:** `AGENTS.md` and `GEMINI.md` at workspace root define always-active project guidelines.
- **Skills Directory:** `.agents/skills/<skill-name>/SKILL.md` is used to define specialized runbooks, measurement scripts, or automated workflows.
- **Adding New Skills:** When defining repeatable multi-step procedures (e.g., automated volume benchmarks, log parsers, ADB test fixtures), create a new skill in `.agents/skills/<skill-name>/SKILL.md`.

---

## 4. Testing, Verification & Tooling

- **Canonical Build Command:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Install on Device via ADB:**
  ```bash
  ./gradlew installDebug
  # or
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- **Live Logging:**
  ```bash
  adb logcat -s JPrecise:V AndroidRuntime:E
  ```
- **Fix Verification Issues Immediately:** Never just report a broken build or lint error—diagnose and fix it before reporting back.

---

## 5. Devcontainer & Infrastructure Notes

- **Dockerfile Layering:** Heavy setup (Android SDK, platform-tools, Gradle) is cached in early Docker layers. User UID/GID alignment is done at build time.
- **Host Docker Build:**
  ```bash
  docker build -t jprecise-devcontainer .devcontainer \
      --build-arg USER_UID=$(id -u) --build-arg USER_GID=$(id -g)
  ```
- **Headless Build via Docker:**
  ```bash
  docker run --rm -v "$PWD":/workspace -w /workspace jprecise-devcontainer \
      bash -lc "./gradlew --no-daemon assembleDebug --stacktrace"
  ```

---

## 6. Android Volume & AccessibilityService Findings (POC #1)

- **Key Filtering Mechanism:**
  - `AccessibilityService` requires `android:canRequestFilterKeyEvents="true"` in XML and `AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS` in code.
  - Key events are delivered to `onKeyEvent(KeyEvent event)` before standard OS processing.
- **What We Can Observe:**
  - Physical volume button presses: `KEYCODE_VOLUME_UP` (24), `KEYCODE_VOLUME_DOWN` (25), `KEYCODE_VOLUME_MUTE` (164).
  - Key action (`ACTION_DOWN`, `ACTION_UP`) and repeat counts (long press detection).
  - Current audio stream state via `AudioManager` (`STREAM_MUSIC` volume level, min/max bounds, `isMusicActive()`).
- **What We Can Intercept (Consume):**
  - Returning `true` from `onKeyEvent()` consumes the key event, preventing Android from displaying the default volume HUD and preventing default volume step increments/decrements.
  - Returning `false` allows passive observation without blocking system behavior.
- **Motorola / Android 16 Behavior:** Moto devices running modern Android (API 33+) may mark sideloaded apps as "Restricted Settings" (requiring tapping 3-dots -> "Allow restricted settings" in App Info before enabling accessibility).

---

## 7. Low-Level Volume Resolution & Audio Architecture Findings (POC #2)

- **Android Audio Stack Layer Breakdown:**
  1. **App Layer (`AudioManager`):**
     - Only accepts integer indices (`0, 1, ... 15` for `STREAM_MUSIC`).
     - Standard Android provides no public API for fractional stream indices.
  2. **Framework Layer (`AudioService` / `AudioSystem`):**
     - Internally converts integer indices to decibel volume curves.
     - Lower-level methods (`AudioSystem.setStreamVolumeIndexAS`) are `@hide` and blocked by Android SELinux and hidden API enforcement for regular third-party apps.
  3. **Native DSP / Effects Layer (`android.media.audiofx`):**
     - Publicly accessible to regular applications via Android SDK.
     - `Equalizer` / `DynamicsProcessing` attached to audio session (Session 0 for output mix where supported, or application sessions).
     - Allows decibel adjustments in millibels (1 dB = 100 mB), providing continuous attenuation control.
  4. **Native PCM Mixer (`AudioFlinger` / `AudioTrack`):**
     - Operates on float gain scalars (`0.0f` to `1.0f`), providing 32-bit floating point output resolution.
- **The JPrecise Hybrid Multi-Resolution Engine:**
  - **The Low-Volume Problem:** On stock Android, the jump from level 0 (mute) to level 1 (audible) is jarringly large in quiet environments.
  - **The Solution:**
    - Hold the underlying `AudioManager` stream index at `1` (the lowest audible base).
    - Divide the range between 0 and 1 into **10 fine sub-steps (0.1, 0.2, ... 1.0)**.
    - Apply software attenuation from `-24.0 dB` (step 0.1) to `0.0 dB` (step 1.0) via `Equalizer` and float PCM gain.
    - Route intercepted physical volume keys to increment/decrement sub-steps (e.g. 0.05, 0.10, 0.25).
- **Physical Output Verification:**
  - `AudioBenchmarkFixture` generates a pure 440 Hz tone directly verifying that consecutive JPrecise positions produce distinct, measurable, and audible attenuation levels.


