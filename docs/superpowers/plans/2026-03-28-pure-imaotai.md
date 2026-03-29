# Pure iMaotai Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove every DaMai-specific behavior and leave the app as an iMaotai-only helper with iMaotai-facing naming and copy.

**Architecture:** Keep the existing Android app structure, but collapse the dual-mode branching into a single iMaotai flow. Rename the core app/service/config resources to iMaotai-facing names while preserving the current package name so the refactor stays low-risk.

**Tech Stack:** Android Views, Kotlin, Android AccessibilityService, Gradle Groovy build scripts, JUnit 4

---

### Task 1: Lock The New State Model With A Unit Test

**Files:**
- Modify: `app/build.gradle`
- Create: `app/src/test/java/com/rookie/damaihelper/UserManagerTest.kt`
- Modify: `app/src/main/java/com/rookie/damaihelper/UserManager.kt`

- [ ] Write the failing test for a single `UserManager.keyword` state.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests com.rookie.damaihelper.UserManagerTest` and confirm it fails.
- [ ] Implement the simplified `UserManager` API.
- [ ] Re-run the focused unit test and confirm it passes.

### Task 2: Collapse The UI To iMaotai Only

**Files:**
- Modify: `app/src/main/java/com/rookie/damaihelper/MainActivity.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Remove mode-switch controls and bind the page to one keyword.
- [ ] Launch only iMaotai from the start action.
- [ ] Update labels, hints, and tips to iMaotai-only copy.

### Task 3: Rename And Trim The Accessibility Service

**Files:**
- Create: `app/src/main/java/com/rookie/damaihelper/IMaotaiHelperService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/imaotai_helper_service.xml`
- Modify: `app/src/main/java/com/rookie/damaihelper/Extensions.kt`

- [ ] Remove DaMai event handling and keep only the iMaotai flow.
- [ ] Rename application-facing service references to `IMaotaiHelperService`.
- [ ] Limit package visibility and accessibility package filters to `com.moutai.mall`.

### Task 4: Rename Supporting App Resources

**Files:**
- Create: `app/src/main/java/com/rookie/damaihelper/IMaotaiApp.kt`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `README.md`
- Modify: `settings.gradle`

- [ ] Rename app-facing names to iMaotai wording while keeping the existing package name.
- [ ] Rewrite README usage instructions for the iMaotai-only flow.

### Task 5: Final Verification

**Files:**
- Modify: none

- [ ] Run `./gradlew :app:testDebugUnitTest --tests com.rookie.damaihelper.UserManagerTest`.
- [ ] Run `./gradlew :app:assembleDebug`.
- [ ] Search `app/src/main` for stale DaMai references and confirm only the package name remains.
