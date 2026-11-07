# Visor kit

Build on Meta's glasses from your own app or your computer, through [Visor](https://github.com/macedonga/visor).
The kit reaches the glasses' status, camera and captures, the Meta Neural Band's gestures and vibration, and the
Ray-Ban Display. No Meta account, no review, no analytics.

## Requirements

- Visor on the phone, with the glasses set up and Developer mode on (Settings, Advanced).
- For an app: Android 8.0 or later. Visor itself needs Android 12.

## Add it

```kotlin
dependencies {
    implementation("com.macedonga.visor:kit:0.1.0")
    testImplementation("com.macedonga.visor:kit-testing:0.1.0")
}
```

## Use it

```kotlin
val visor = Visor.connect(context)

// Once: the person grants the scopes on Visor's consent screen.
val ask = registerForActivityResult(RequestScopes()) { held -> /* the scopes the app holds now */ }
ask.launch(setOf(Scope.CAMERA, Scope.BAND))

val photo = visor.camera.takePhoto()           // in the gallery: photo.uri
visor.band.gestures.collect { gesture ->       // the band's gestures come to the app while it collects
    if (gesture.derived == GestureAction.DOUBLE_TAP) visor.band.buzz()
}
visor.display.prompter.show("Slide 3: the numbers")
```

Every call fails with a `KitException` whose `code` says why: `scope_denied` (ask with `RequestScopes`),
`not_connected`, `unsupported` (for example the prompter on glasses without a display), `visor_too_old`, and so
on. While Visor restarts, calls wait and streams resume by themselves.

## Try it from your computer

The `visor` command talks to Visor on the phone through adb, over USB or wireless debugging. Get it from the
[releases](https://github.com/macedonga/visor-kit/releases) (it needs Java 17), or build it with
`./gradlew :cli:installDist`.

```
visor status                              # Visor, the glasses, the band, the apps that use the kit
visor call camera.photo                   # any method, by name, with JSON params
visor watch band.gestures glasses.status  # events as they come
visor trace                               # every call and event between your app and Visor
visor simulate band.gestures              # no band needed: type "index double_tap", your app gets it
visor inject glasses.status '{"paired":true,"link":"connected","battery":5}'
visor grant com.example.app camera band   # skip the consent screen while you develop
```

Only adb can reach Visor this way: Visor checks that each connection comes from the shell.

## Try it without glasses

`FakeVisor` runs Visor inside your app's process, for unit tests and for working without the phone:

```kotlin
val fake = FakeVisor()
val visor = fake.connect()
fake.glasses.battery = 12
fake.band.gesture(Finger.INDEX, GestureAction.TAP)
visor.display.prompter.show("Slide 3")
assertEquals("Slide 3", fake.display.prompter.value)
```

## Other languages

The `visor-kit` socket speaks JSON-RPC 2.0, one object per line. Every method in
[`protocol/api/openrpc.json`](protocol/api/openrpc.json) is a JSON-RPC method of the same name, and topics are
streamed with `kit.subscribe`. `adb forward tcp:7333 localabstract:visor-kit` makes it reachable on your computer.

## Compatibility

Released methods, fields and Kotlin APIs never go away or change: an app built against any kit release keeps
working with every later Visor, and the reverse. The build checks this against the last release; the rules are in
[CLAUDE.md](CLAUDE.md).

## License

Apache 2.0.
