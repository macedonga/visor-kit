# kit-fake: FakeVisor in plain Kotlin

The fake glasses, band and Display behind `FakeVisor`, with no Android in sight: for JVM tests, tools, and
`visor fake` on your computer. Android apps use [`kit-testing`](../testing), which adds the previews and the content
provider on top of this.

| Artifact | Needs |
|---|---|
| `com.macedonga.visor:kit-fake` | Java 17 or later |

```kotlin
dependencies {
    testImplementation("com.macedonga.visor:kit-fake:0.1.1")
}
```

## What it plays

| Part | What you can do with it |
|---|---|
| `fake.glasses` | Battery, charging, worn, folded, link and thermal; captures; touches and presses; motion, or DAT's motion CSV with `playMotion`. |
| `fake.band` | Paired, held and shared; gestures; motion on the wrist; the buzzes asked for. |
| `fake.display` | The teleprompter, the layout shown, clicks, back, the video's state, the Briefing cards and clicks in their details. |
| `fake.voice` | Questions the wearer asks, utterances for transcripts. |
| `fake.assistant` | Calls to the app's tools, conversation turns. |
| `fake.live` | The live camera, from samples you send or a recorded stream with `play`. |
| `fake.settings`, `fake.notify`, `fake.granted` | Settings, notifications, the scopes the fake person granted. |
| `fake.calls` | Every method the app called, in order. |

## Read more

[Testing](https://visor.macedonga.com/developers/docs/testing/).
