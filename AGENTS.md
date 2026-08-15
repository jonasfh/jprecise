Agents and Contribution Notes for JPrecise

Purpose
- JPrecise is a technical proof-of-concept (POC) to investigate whether Android can provide a custom, fine-grained volume controller for Bluetooth headphones.

Target hardware and Android version
- Motorola Edge 50 Pro (XT2403-2)
- Target device software: Android 16 (as specified by stakeholders)

Motivation
- Android's native volume steps can be too coarse at very low listening levels. The POC explores finer-grained steps, custom overlays, and non-linear volume curves for extremely quiet listening.

Guidelines
- This is an experimental POC, not production software.
- Prefer Java (no Kotlin) and standard Android SDK APIs.
- Avoid unnecessary dependencies and frameworks; keep the project small and incremental.
- Verify Android behaviour experimentally on target hardware rather than assuming platform behavior.
- Make small, incremental changes with clear tests or manual verification steps.

Notes
- Keep work focused on exploration and measurement; suspend implementing full volume-control logic until experimental behaviors are understood.
