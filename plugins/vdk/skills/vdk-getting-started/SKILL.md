---
name: vdk-getting-started
description: Add the VDK to an Android app, connect to Visor, ask for scopes and make a first call
---

# Getting started with the VDK

The VDK lets an Android app use Meta's glasses through Visor. The phone needs Visor, set up with the glasses.

1. Add `implementation("com.macedonga.visor:kit:<version>")`.
2. Connect: `val visor = Visor.connect(context)`. Check `Visor.isInstalled(context)` first; without Visor,
   asking for scopes throws `ActivityNotFoundException`.
3. Ask for the scopes the feature needs, once, from an activity:
   `registerForActivityResult(RequestScopes()) { held -> }` then `launch(setOf(Scope.CAMERA))`.
4. Call and collect: `visor.camera.takePhoto()`, `visor.glasses.status.collect { }`.

Read `getting-started` and `scopes-and-consent` with `read_vdk_doc` for the full steps.
