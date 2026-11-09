# Testing

Wearing the glasses for every change is slow. The VDK has three tools so you mostly do not have to:

- **The `visor` command**, on your computer: talk to Visor on the phone, watch what your app does, and stand in
  for the band or the glasses.
- **`FakeVisor`**, in your tests: Visor inside your app's process, driven by your test code.
- **`visor fake`**, on your computer: FakeVisor behind the same doors Visor opens on a phone, for scripts, web
  pages, MCP clients and the `visor` command, with no phone at all.

## The visor command

### Set up

1. Install Java 17 or later and Android's platform tools (adb). Android Studio brings both.
2. Download `visor-<version>.zip` from the [releases](https://github.com/macedonga/visor-kit/releases), unzip it,
   and put its `bin` folder on your PATH. From a clone: `./gradlew :cli:installDist`, then use
   `tools/cli/build/install/visor/bin/visor`.
3. Connect the phone with adb, over USB or wireless debugging: `adb devices` must list it. With more than one
   device, pick one with `visor -s SERIAL ...`.
4. In Visor, turn on Developer mode, in Settings, Advanced settings, Tools. It opens the socket the command uses;
   apps do not need it.

`visor` finds adb through `ANDROID_HOME` or the PATH, forwards a free port to Visor's socket, and removes the
forward when it ends. Only adb can reach the socket: Visor checks that each connection comes from adb's shell
user.

### Commands

| Command | What it does |
|---|---|
| `visor status` | Visor's version, the glasses, the band, and the apps that use the VDK. |
| `visor methods` | The methods and topics this Visor answers. |
| `visor call METHOD [JSON]` | Runs any method and prints its answer. |
| `visor watch TOPIC...` | Prints each event of one or more topics as it comes, until Ctrl+C. |
| `visor live FILE [JSON]` | Saves the camera live as raw HEVC or H.264, the sound beside it as `FILE.aac`, until Ctrl+C. |
| `visor live-photo FILE` | Takes a photo while the camera is live and saves the HEIC. |
| `visor trace` | Prints every call, answer and event between every app and Visor, with timings. |
| `visor simulate TOPIC...` | While it runs, apps subscribed to these topics get only the events you type. |
| `visor inject TOPIC EVENT` | Sends one made-up event to the apps subscribed to a topic. |
| `visor record FILE [TOPIC...]` | Saves each event of the topics to a file, until Ctrl+C. |
| `visor replay FILE [--loop]` | Plays a recording into the apps at its own pace. |
| `visor apps` | The apps that use the VDK, their scopes, and whether they are connected. |
| `visor grant PACKAGE SCOPE...` | Grants scopes without the consent screen. |
| `visor revoke PACKAGE SCOPE...` | Takes scopes back. |
| `visor fake` | Runs FakeVisor on this computer. See [Without a phone](#without-a-phone-visor-fake). |

Add `--json` for JSON only, one value per line, to pipe into `jq` or a script.

### Watch the camera

`visor live` films with the glasses until Ctrl+C. Write to a file, or to standard output for a player:

```
visor live - | ffplay -fflags nobuffer -
visor live walk.hevc '{"quality":"full_hd","frameRate":30,"audio":true}'
ffmpeg -i walk.hevc -i walk.hevc.aac -c copy walk.mp4
```

In PowerShell 5, pipe through `cmd /c` or save to a file: PowerShell's pipe changes binary data.

### See what your app does

Run `visor trace` in one terminal and use your app:

```
$ visor trace
Tracing the VDK. Ctrl+C stops.
16:02:11.204  com.example.app  call        kit.hello #1  {"protocol":1,"kit":"0.1.0"}
16:02:11.209  com.example.app  result      kit.hello #1 4 ms  {"protocol":1,"visor":"2.4.0",...}
16:02:11.240  com.example.app  subscribe   band.gestures #2
16:02:13.811  com.example.app  event       band.gestures #2  {"finger":"index","action":"tap","derived":"double_tap",...}
16:02:15.002  com.example.app  call        camera.photo #3  {"import":true}
16:02:18.517  com.example.app  result      camera.photo #3 3515 ms  {"id":"...","type":"photo","uri":"content://..."}
```

### Stand in for the band or the glasses

`simulate` takes the real source out of the topics you name: apps subscribed to them get only what you type, and
the band is not borrowed. It ends when you press Ctrl+C or close the input, and then the real source comes back
without your app noticing.

```
$ visor simulate band.gestures
Simulating ["band.gestures"]: apps get only what you type here. Ctrl+C or end of input stops.
index double_tap
middle hold
thumb up
```

For `band.gestures`, a line is `FINGER ACTION`, or `FINGER ACTION DERIVED` when the band's reading should differ
from the action. For `glasses.inputs`, a line is `SOURCE ACTION`, such as `touchpad swipe_forward`. For any other
topic, a line is the event's JSON:

```
$ visor simulate glasses.status
{"paired":true,"link":"connected","battery":4,"worn":true}
{"paired":true,"link":"disconnected"}
```

With several topics, start each line with its topic. `visor inject` sends a single event, simulated or not, to
the apps subscribed now:

```
visor inject band.gestures index tap
visor inject glasses.status '{"paired":true,"link":"connected","battery":15}'
```

Visor checks every event against the topic's type before it sends it, so a typo fails here, not in your app.
Made-up events go to apps only: the glasses, the band and Visor's own screens never see them.

### Script the glasses

`call` runs any method, which makes the command a quick way to script the glasses:

```
visor call camera.photo '{"import":false}'
visor call band.buzz '{"durationMs":120,"strength":128}'
visor call display.prompter.show '{"text":"Two minutes left"}'
visor --json watch band.gestures | jq -r .event.derived
```

### Record once, replay anywhere

`record` saves what the wearer does, with the state it happens in, so you can play it back while you work on
your app, on the phone or on `visor fake`:

```
visor record walk.jsonl                     # glasses.status, glasses.inputs, band.status, band.gestures
visor record taps.jsonl band.gestures       # only these topics
visor replay walk.jsonl                     # into the apps on the phone
visor --fake replay walk.jsonl --loop       # into visor fake, over and over
```

In a JVM test, `FakeVisor.replay` plays a recording into your app the same way, paced or all at once:

```kotlin
fake.replay(File("src/test/resources/walk.jsonl").reader())          // at its recorded pace
fake.replay(File("src/test/resources/taps.jsonl").reader(), paced = false)
```

Each line is one event, `{"t": 1520, "topic": "band.gestures", "event": {...}}`, with `t` in milliseconds from
the start of the recording. While a replay runs, its topics are simulated: apps get the recorded events, at
their recorded times, and nothing from the real glasses or band. The events keep the times they were recorded
with. A recording is plain JSON, so a test can read it too, or you can write one by hand.

## FakeVisor

`FakeVisor` (`com.macedonga.visor:kit-testing`) is Visor in your process. It answers every method from state your
test sets, so tests run without a phone, glasses or band. It runs the same server code as Visor, so scopes,
streams and errors behave the same way.

```kotlin
import com.macedonga.visor.kit.testing.FakeVisor
import com.macedonga.visor.kit.testing.connect

@Test
fun warnsOnLowBattery() = runBlocking {
    val fake = FakeVisor()
    val visor = fake.connect()

    fake.glasses.battery = 4
    val screen = BatteryWarning(visor)
    screen.refresh()

    assertEquals("Charge the glasses", fake.display.prompter.value)
    assertEquals(listOf("kit.hello", "display.prompter.show"), fake.calls)
    fake.close()
}
```

It runs as a plain JVM unit test: no emulator, no Robolectric. Calls answer from the fake's state at once, and
streams of state send it first, so a test needs no waiting. Events you send while nothing collects are dropped,
as with the real band: start collecting before you call `fake.band.gesture(...)`.

What the fake offers:

| Part of the fake | What it offers |
|---|---|
| `fake.granted` | The scopes the fake person granted; all at first. Taking one back ends the streams that need it. |
| `fake.glasses` | `paired`, `connected`, `display`, `model`, `battery`, `charging`, `worn`, `folded`, `thermal`: each change reaches `glasses.status`. `add(type)` puts a capture on the fake glasses, and `add(type, bytes, mime)` one with those bytes. `input(source, action)` sends a touch or a press. |
| `fake.band` | `paired`, `battery`, `charging`, `leftHanded`, `held`, `shared`; `gesture(...)` sends a gesture; `motion(...)` sends a reading for `band.motion` once `motionRate` is set; `buzzes` lists the buzzes asked for. |
| `fake.display` | `prompter`: the script on the teleprompter, null while closed; `layout`: the layout shown; `click(id)` taps one of its elements; `back()` goes back; `video(state, error)` moves the video along; `cards`: the app's Briefing cards; `clickCard(card, id)` clicks in a card's detail. |
| `fake.notify` | `shown`: the app's notifications on the Display, by id. |
| `fake.settings` | `all`: five sample settings by key; `change(key, value)` changes one as the wearer would. |
| `fake.assistant` turns | `turn(question, answer)` adds a turn the app gets on `assistant.turns`; `history` lists them. |
| `fake.assistant` | `tools` the app offers; `call(tool, arguments)` calls one as the model would and returns the answer. |
| `fake.voice` | `ask(question)` asks as the wearer would and returns the app's reply; `say(text)` is an utterance the glasses hear, for `voice.transcripts`; `listening` counts the apps that collect transcripts. |
| `fake.live` | `watching` and `config` while the app collects `camera.live`; `sendVideoConfig`, `sendVideo`, `sendAudioConfig`, `sendAudio` send samples; `stop()` ends it as the person would; `photos` counts live photos, and `photo(bytes, mime)` sets what they hold. |
| `fake.calls` | The name of every method the app called, in order. |

In an app with `kit-testing`, the fake's imported captures and live photos open with the app's `ContentResolver`,
as Visor's do: their `content://` URIs point at a provider that `kit-testing` adds to the app. Without bytes given,
each holds a 1x1 PNG. In a plain JVM test the URIs open nothing.

The fake films nothing: your test sends each live sample. Wait until the app watches, or the sample is lost:

```kotlin
fake.live.watching.first { it }
fake.live.sendVideoConfig(parameterSets)
fake.live.sendVideo(keyframe, ptsUs = 0, key = true)
```

`FakeVisor` itself is plain Kotlin, in `com.macedonga.visor:kit-fake`: a JVM test or tool can drive it without
Android. `kit-testing` adds `connect()`, which needs the Android library.

### Recorded feeds

DAT's mock plays a video file as the camera and a CSV as the motion sensors; FakeVisor does both:

```kotlin
launch { File("walk.csv").reader().use { fake.glasses.playMotion(it, loop = true) } }
launch { File("street.h265").inputStream().use { fake.live.play(it, "video/hevc", 504, 896, 24, loop = true) } }
```

The CSV is DAT's: no header, 14 numbers a row, the time in nanoseconds, the accelerometer, gyroscope and
magnetometer as x, y, z, and the orientation as w, x, y, z. The camera takes an Annex-B H.264 or H.265 stream;
ffmpeg makes one from any clip without re-encoding: `ffmpeg -i clip.mp4 -c:v copy -bsf:v hevc_mp4toannexb
street.h265` (`h264_mp4toannexb` for H.264). It starts once an app watches the live camera.

## Without a phone: visor fake

`visor fake` runs FakeVisor on your computer, behind the doors Visor opens on a phone:

- JSON-RPC over WebSocket on `ws://127.0.0.1:7333/rpc`, as Visor's [network door](network.md), with no token;
- MCP on `http://127.0.0.1:7333/mcp`, for an assistant on your computer;
- the `visor` command, with `--fake` in place of `-s SERIAL`;
- the Display simulator on `http://127.0.0.1:7333/display`.

Every scope is granted. Its console plays the glasses, and prints what apps do to them:

```
$ visor fake
FakeVisor on ws://127.0.0.1:7333/rpc and http://127.0.0.1:7333/mcp; visor --fake on port 7334. Type help.
The Display simulator: http://127.0.0.1:7333/display
00:23:47  Display: the prompter shows "Hello from the fake"
00:23:47  Band: a buzz of 400 ms
gesture index double_tap
battery 12
ask what is on the calendar today
00:24:02  Voice: the glasses say "Two meetings, the first at ten."
```

| Type | What happens |
|---|---|
| `battery N` | The glasses' battery, 0 to 100. |
| `charging`, `worn`, `folded`, `connected`, `display` with `on` or `off` | That part of `glasses.status`. |
| `thermal LEVEL` | `normal`, `light`, `moderate`, `severe` or `critical`. |
| `input SOURCE ACTION` | A touch or a press for `glasses.inputs`, such as `touchpad tap`. |
| `motion AX AY AZ` | An accelerometer reading in m/s² for `glasses.motion`, such as `motion 0 9.8 0`. |
| `motion-csv FILE` | DAT's 14-column motion CSV, played over and over at the pace of its timestamps. |
| `camera FILE WIDTHxHEIGHT FPS`, `camera off` | A recorded `.h264` or `.h265` stream as the live camera, looped. |
| `gesture FINGER ACTION` | A band gesture for `band.gestures`, such as `index double_tap`. |
| `band-shared on`, `band-shared off` | The band shared with the phone, as the person sets it in Visor. |
| `band-motion AX AY AZ` | An accelerometer reading on the wrist for `band.motion`, which needs the band shared. |
| `photo`, `video` | A new capture on the glasses. |
| `ask QUESTION` | The wearer asks; the app whose phrase the question holds answers. |
| `say TEXT` | The glasses hear TEXT, for `voice.transcripts`. |
| `tool NAME [JSON]` | The assistant calls an app's tool. |
| `turn QUESTION / ANSWER` | The wearer asks the assistant and it answers, for `assistant.turns`. |
| `click ID` | A tap on an element of the layout on the Display. |
| `back` | The wearer goes back: the layout goes. |
| `video STATE [ERROR]` | The layout's video moves to `playing`, `paused`, `ended`, or `failed` with an error. |
| `grant SCOPE...`, `revoke SCOPE...` | The scopes the fake person granted. |

From another terminal, the command works on the fake as on a phone: `visor --fake trace`, `visor --fake watch
band.gestures`, `visor --fake call display.prompter.show '{"text":"Hi"}'`. The Python client has
`visor_kit.connect_fake()` and the TypeScript one `Visor.connectFake()`. `--port N` moves the doors to N and the
command's port to N + 1; `--lan` opens the doors to the local network.

### The Display simulator

Open `http://127.0.0.1:7333/display` while `visor fake` runs. It draws the layout an app shows on the fake Display
at 600 by 600, by the same rules Visor uses to send it to the glasses, and it plays the wearer:

- the arrow keys move the focus as the touchpad and the band do, Enter selects, Backspace or Esc goes back; a
  click on an element selects it too. The app gets the clicks and the `back` as from the glasses, and each move on
  `glasses.inputs` as `nav_*`, `select` and `back` from the touchpad;
- the Display adds light, so its black is clear: the page draws it over a background (black, a room, a street in
  daylight, a night street, or your webcam) with a brightness slider;
- a checkbox makes it dim and sleep after 20 and 25 seconds, as the Display does;
- a video plays, and its pauses, end and failures reach the app on `display.video`;
- Screenshot and Record save the display as a PNG or a WebM (Chrome crops them to the display);
- Screen switches to the Briefing: the app's cards in the panel's material, one under another. Enter or a click opens
  a card's detail, its pages swipe with the arrows, its buttons click as on the glasses (the app gets them on
  `display.cards.clicks`, and an element's `toast` shows), and Backspace closes it.

DAT's icons are drawn with the nearest Material Symbol; the Briefing glyphs and the weather illustrations come from
Meta's own icon CDN, as the Display fetches them. Check a layout on the glasses before you ship it.

`visor --fake card card.json` puts a card on the fake Briefing from a file, as `visor card` does on a phone, and
`visor --fake call METHOD @params.json` takes any method's params from a file. With `visor fake --port 7400`, give
the same port to the command: `visor --fake=7400 status`.

In the app itself, `fake.display.preview(context)` from `kit-testing` is a view with the same renderer: it draws
what the fake Display shows and clicks what you tap, as DAT's `createPreviewView()` does.
`fake.display.briefingPreview(context)` draws the Briefing with the app's cards: a tap opens a card's detail, a tap
there clicks, and a double tap goes back.

## Web apps on the Display

`Visor.connectGlasses()` works only in the Display's browser. Elsewhere, connect the same page to `visor fake` with
`Visor.connectFake()`, and pick with `onGlasses()`: see [Web apps on the Display](web-apps.md).

## The samples

- `samples/android` is an app with a button for every capability. Build it with `./gradlew :sample:installDebug`,
  and use it with `visor trace` to see a whole exchange.
- `samples/python` holds small scripts on the Python client: a teleprompter pager turned by the band, a journal of
  the assistant's answers, and the glasses' settings.
- `samples/web` is a glasses console in one page, on the network door, with no build step.
