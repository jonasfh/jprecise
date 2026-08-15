# Agents and Contribution Guide for JPrecise

This document is the canonical entry point and rulebook for AI coding assistants (Gemini, Antigravity, etc.) and human contributors working in this repository.

---

## 1. Mandatory Agent Workflow & Git Commit Policy

- **Commit Upon Completion:** When a coherent task, fix, or feature is completed and verified, the AI agent **MUST proactively create a Git commit**. Do not leave working, verified changes uncommitted unless explicitly instructed by the user.
- **Verification Before Commit:** Every commit must be verified:
  - Run `./gradlew assembleDebug` (and unit tests if applicable) before creating a commit.
  - Fix any build warnings, compile errors, or lint failures discovered during verification immediately before committing.
- **Atomic and Relevant Commits:**
  - Stage only relevant modified/new files (`git add <files>`).
  - Write concise, descriptive commit messages describing the *what* and *why*.
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
