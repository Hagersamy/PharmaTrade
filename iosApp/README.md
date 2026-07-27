# iOS app shell — set up on your Mac

This project's `:shared` Gradle module already declares `iosArm64()` and
`iosSimulatorArm64()` targets and builds a `Shared.framework` (see
`shared/build.gradle.kts`). Kotlin/Native's iOS targets can only be compiled
and linked on macOS with Xcode installed — that step can't be done from this
Windows machine, so the Xcode project itself needs to be created on your Mac.

Xcode's project file format is a hand-editable-but-fragile plist; generating
one from outside Xcode risks a broken project, so these are the steps to run
directly in Xcode instead:

1. Copy (or `git pull`) this whole repository onto your Mac.
2. Verify the shared framework builds standalone first:
   ```
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```
   Output framework: `shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`
3. In Xcode: **File > New > Project > iOS > App**. Save it into this
   `iosApp/` directory (product name `iosApp`, interface: SwiftUI).
4. Add a **Run Script** build phase (before "Compile Sources") that invokes
   Gradle to build and embed the framework — this is the standard
   Kotlin Multiplatform + Xcode integration script:
   ```sh
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
5. Add `SEARCH_PATHS`/framework linking as prompted by that task's output, or
   use the JetBrains **Kotlin Multiplatform** Xcode plugin, which wires this
   automatically when you add a KMP module reference to an Xcode project.
6. Once `Shared` is importable in Swift (`import Shared`), the placeholder
   entry point looks like:
   ```swift
   import SwiftUI
   import Shared

   @main
   struct iosAppApp: App {
       var body: some Scene {
           WindowGroup { Text("PharmaTrade — shared module linked") }
       }
   }
   ```

This step is a checkpoint for **Stage 5** of the migration plan (wiring up
the iOS app shell) — right now `:shared` only contains domain/data code with
no UI yet, so there's nothing to render beyond a placeholder. Compose
Multiplatform UI gets added to `:shared` in Stage 4, at which point step 6
above swaps to hosting a `ComposeUIViewController` instead of plain SwiftUI.
