# Testing

Wearing the glasses for every change is slow. The VDK has two tools so you mostly do not have to:

- **The `vdk` command**, on your computer: talk to Visor on the phone, watch what your app does, and stand in
  for the band or the glasses.
- **`FakeVisor`**, in your tests: Visor inside your app's process, driven by your test code.

## The visor command

### Set up

1. Install Java 17 or later and Android's platform tools (adb). Android Studio brings both.
2. Download `vdk-<version>.zip` from the [releases](https://github.com/macedonga/visor-kit/releases), unzip it,
   and put its `bin` folder on your PATH. From a clone: `./gradlew :cli:installDist`, then use
   `tools/cli/build/install/vdk/bin/vdk`.
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
| `vdk status` | Visor's version, the glasses, the band, and the apps that use the VDK. |
| `vdk methods` | The methods and topics this Visor answers. |
| `vdk call METHOD [JSON]` | Runs any method and prints its answer. |
| `vdk watch TOPIC...` | Prints each event of one or more topics as it comes, until Ctrl+C. |
| `vdk trace` | Prints every call, answer and event between every app and Visor, with timings. |
| `vdk simulate TOPIC...` | While it runs, apps subscribed to these topics get only the events you type. |
| `vdk inject TOPIC EVENT` | Sends one made-up event to the apps subscribed to a topic. |
| `vdk apps` | The apps that use the VDK, their scopes, and whether they are connected. |
| `vdk grant PACKAGE SCOPE...` | Grants scopes without the consent screen. |
| `vdk revoke PACKAGE SCOPE...` | Takes scopes back. |

Add `--json` for JSON only, one value per line, to pipe into `jq` or a script.

### See what your app does

Run `vdk trace` in one terminal and use your app:

```
$ vdk trace
Tracing the VDK. Ctrl+C stops.
16:02:11.204  com.example.app  call        vdk.hello #1  {"protocol":1,"vdk":"0.1.0"}
16:02:11.209  com.example.app  result      vdk.hello #1 4 ms  {"protocol":1,"visor":"2.4.0",...}
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
$ vdk simulate band.gestures
Simulating ["band.gestures"]: apps get only what you type here. Ctrl+C or end of input stops.
index double_tap
middle hold
thumb up
```

For `band.gestures`, a line is `FINGER ACTION`, or `FINGER ACTION DERIVED` when the band's reading should differ
from the action. For any other topic, a line is the event's JSON:

```
$ vdk simulate glasses.status
{"paired":true,"link":"connected","battery":4,"worn":true}
{"paired":true,"link":"disconnected"}
```

With several topics, start each line with its topic. `vdk inject` sends a single event, simulated or not, to
the apps subscribed now:

```
vdk inject band.gestures index tap
vdk inject glasses.status '{"paired":true,"link":"connected","battery":15}'
```

Visor checks every event against the topic's type before it sends it, so a typo fails here, not in your app.
Made-up events go to apps only: the glasses, the band and Visor's own screens never see them.

### Script the glasses

`call` runs any method, which makes the command a quick way to script the glasses:

```
vdk call camera.photo '{"import":false}'
vdk call band.buzz '{"durationMs":120,"strength":128}'
vdk call display.prompter.show '{"text":"Two minutes left"}'
vdk --json watch band.gestures | jq -r .event.derived
```

## FakeVisor

`FakeVisor` (`com.macedonga.visor:vdk-testing`) is Visor in your process. It answers every method from state your
test sets, so tests run without a phone, glasses or band. It runs the same server code as Visor, so scopes,
streams and errors behave the same way.

```kotlin
@Test
fun warnsOnLowBattery() = runBlocking {
    val fake = FakeVisor()
    val visor = fake.connect()

    fake.glasses.battery = 4
    val screen = BatteryWarning(visor)
    screen.refresh()

    assertEquals("Charge the glasses", fake.display.prompter.value)
    assertEquals(listOf("vdk.hello", "display.prompter.show"), fake.calls)
    fake.close()
}
```

It runs as a plain JVM unit test: no emulator, no Robolectric. Calls answer from the fake's state at once, and
streams of state send it first, so a test needs no waiting. Events you send while nothing collects are dropped,
as with the real band: start collecting before you call `fake.band.gesture(...)`.

What the fake offers:

| | |
|---|---|
| `fake.granted` | The scopes the fake person granted; all at first. Taking one back ends the streams that need it. |
| `fake.glasses` | `paired`, `connected`, `display`, `model`, `battery`, `charging`, `worn`, `folded`: each change reaches `glasses.status`. `add(type)` puts a capture on the fake glasses. |
| `fake.band` | `paired`, `battery`, `charging`, `leftHanded`; `gesture(...)` sends a gesture; `buzzes` lists the buzzes asked for. |
| `fake.display` | `prompter`: the script on the teleprompter, null while closed. |
| `fake.calls` | The name of every method the app called, in order. |

Photos taken on the fake have `content://com.macedonga.visor.vdk.testing/captures/...` URIs, which open nothing.

## The sample app

`samples/android` in this repo is an app with a button for every capability. Build it with
`./gradlew :sample:installDebug`, and use it with `vdk trace` to see a whole exchange.
