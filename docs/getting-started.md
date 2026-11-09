# Getting started

This guide takes an Android app from nothing to a photo taken with the glasses.

## 1. Prepare the phone

Install [Visor](https://github.com/macedonga/visor) and set up the glasses in it. Nothing else: your app asks the
person for what it needs, on Visor's consent screen.

## 2. Add the library

The VDK needs Android 8.0 (API 26) or later in your app. Visor itself runs on Android 12 and later.

```kotlin
dependencies {
    implementation("com.macedonga.visor:kit:0.1.0")
    testImplementation("com.macedonga.visor:kit-testing:0.1.0")
}
```

The library's manifest declares Visor's package in `<queries>`, so your app can see Visor on Android 11 and later
without anything in your own manifest.

## 3. Connect

```kotlin
if (!Visor.isInstalled(context)) {
    // Point the person to Visor's releases page.
    return
}
val visor = try {
    Visor.connect(context)
} catch (e: KitException) {
    // e.code is VISOR_TOO_OLD, or another code: see Errors.
    return
}
```

`connect` suspends until Visor answers. Keep one `Visor` for as long as you use the glasses, and `close()` it
when you are done. The [connection](connection.md) page explains what happens when Visor restarts.

## 4. Ask for scopes

Each capability needs a scope. Ask for the ones you need with `RequestScopes`, which opens Visor's consent screen
over your app:

```kotlin
val ask = registerForActivityResult(RequestScopes()) { held ->
    // held: every scope the app holds now. It can be fewer than asked for.
}
ask.launch(setOf(Scope.STATE, Scope.CAMERA))
```

Register the launcher before the activity starts, as for any activity result. If your app already holds every
scope it asks for, the screen does not appear and the result comes at once. `visor.scopes()` returns the scopes
the app holds at any time.

## 5. Use the glasses

```kotlin
lifecycleScope.launch {
    visor.glasses.status.collect { status ->
        batteryView.text = status.battery?.let { "$it%" } ?: "-"
    }
}

lifecycleScope.launch {
    try {
        val photo = visor.camera.takePhoto()
        imageView.setImageURI(Uri.parse(photo.uri))
    } catch (e: KitException) {
        when (e.code) {
            ErrorCode.SCOPE_DENIED -> ask.launch(setOf(e.scope!!))
            ErrorCode.NOT_CONNECTED -> showMessage("Put the glasses on.")
            else -> showMessage(e.message)
        }
    }
}
```

`takePhoto()` returns once the photo is in the phone's gallery. Visor grants your app read access to its URI, so
your app needs no media permission to show it.

## 6. Try it without the glasses

While you build, these tools save you from wearing the glasses:

- The `visor` command on your computer shows what your app sends and receives, and can stand in for the band or
  the glasses: `visor simulate band.gestures`, then type `index double_tap`.
- `visor record` saves what the wearer does once, and `visor replay` plays it back as often as you like.
- `FakeVisor` runs Visor inside your tests.
- `visor fake` runs a fake Visor on your computer, for scripts, web pages and MCP clients, with no phone at all.

All of them are in [Testing](testing.md).

## Next

- [Scopes and consent](scopes-and-consent.md)
- [Errors](errors.md): what each code means and what to do about it
- The capability pages: [Glasses](glasses.md), [Camera and captures](camera-and-captures.md),
  [Neural Band](neural-band.md), [Display](display.md), [Briefing cards](briefing-cards.md),
  [Voice and the assistant](voice-and-assistant.md), [Settings](settings.md)
- Building a web app for the Display instead: [Web apps on the Display](web-apps.md)
