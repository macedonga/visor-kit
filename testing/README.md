# kit-testing: FakeVisor for your app's tests

`FakeVisor` runs Visor inside your app's process, so your tests and your previews need no phone, glasses or band.
It runs the same server code as Visor: scopes, errors and streams behave as they do on the glasses.

| Artifact | Needs |
|---|---|
| `com.macedonga.visor:kit-testing` | [`kit`](../android); Android or a plain JVM test |

```kotlin
dependencies {
    testImplementation("com.macedonga.visor:kit-testing:0.1.0")
}
```

## Use it

```kotlin
@Test fun warnsOnALowBattery() = runTest {
    val fake = FakeVisor()
    fake.glasses.battery = 4
    val visor = fake.connect()

    assertEquals(4, visor.glasses.status.first().battery)
    assertTrue(BatteryWarning(visor).shouldWarn())
}
```

Set the fake's state (`fake.glasses.battery = 4`, `fake.band.shared = true`), play the wearer
(`fake.band.gesture(...)`, `fake.voice.say(...)`, `fake.display.click(id)`), and check what your app did
(`fake.calls`, `fake.display.cards`, `fake.band.buzzes`). Start collecting a stream before you play its events: the
fake, like the glasses, does not keep them for later.

## What is inside

| Part | What it does |
|---|---|
| `FakeVisor.connect()` | A `Visor` on the fake, in your process. |
| `fake.display.preview(context)` | A view that draws the fake Display, as DAT's preview does; taps click. |
| `fake.display.briefingPreview(context)` | A view of the Briefing with your app's cards and their details. |
| A content provider | Opens the fake's captures and live photos with your app's `ContentResolver`. |

Everything else, the fake glasses, band, Display, voice and live camera, comes from [`kit-fake`](../fake).

## Read more

[Testing](../docs/testing.md): the fake's every part, recorded feeds, and the `visor` command.
