# Voice and the assistant

Visor answers "Hey Meta" with the person's own model. An app can take part in two ways: answer the questions that
name it, and give the model tools to use while it answers.

## Questions that name the app

Scope: `voice`. All glasses.

```kotlin
visor.voice.requests(listOf("the garage", "my lights")).collect { request ->
    val text = when {
        "close" in request.text -> garage.close().let { "Closing the garage." }
        else -> "I can open or close the garage."
    }
    visor.voice.reply(request, text)
}
```

While the app collects `requests`, every question that holds one of its phrases comes to the app instead of the
model: "Hey Meta, ask the garage to close" comes for "the garage". The glasses then say the reply in their own
voice, as they would the model's answer.

- **Matching.** A question matches a phrase when it holds all the phrase's words, in order and next to each
  other. Case, accents and punctuation do not count. When phrases of several apps match, the longest phrase wins.
  Pick phrases nobody says by chance: "the garage" rather than "open".
- **Time.** Reply within about 20 seconds; the glasses give up on a question after that. Without a reply, the
  glasses tell the wearer that the app did not answer.
- **Follow-ups.** `reply(request, text, listen = true)` keeps the microphone open after the reply, as after a
  question. The follow-up is matched like any question: it comes to the app again only if it names a phrase.
- **What else comes.** `request.intent` is what the glasses understood the question as, such as `IN:QUESTION`,
  when they said. `request.phrase` is the phrase that matched.

A `VoiceRequest`:

| Field | Type | Meaning |
|---|---|---|
| `id` | `String` | What `reply` answers. |
| `text` | `String` | The question, as the glasses heard it. |
| `phrase` | `String` | The app's phrase that matched. |
| `intent` | `String?` | The glasses' reading of the question. |

A reply that comes too late fails with `invalid_params` and reason `expired`.

### Started by voice

An app that is not running can be started by its phrase, as DAT's voice invocations start one. Declare an activity
with the VDK's voice action and a string array of phrases:

```xml
<activity android:name=".GarageActivity" android:exported="true">
    <intent-filter>
        <action android:name="com.macedonga.visor.kit.action.VOICE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <meta-data android:name="com.macedonga.visor.kit.VOICE_PHRASES" android:resource="@array/garage_phrases" />
</activity>
```

When a question holds one of these phrases and no running app holds it, Visor starts the activity with the
question. Read it with `VoiceLaunch.request(intent)` in `onCreate` and `onNewIntent`, and reply as above, within
about 20 seconds:

```kotlin
VoiceLaunch.request(intent)?.let { request ->
    lifecycleScope.launch { Visor.connect(this@GarageActivity).voice.reply(request, "The garage is closed.") }
}
```

Visor starts only apps the person granted the `voice` scope, so ask for it once, at setup. While the app collects
`requests`, its running offer answers instead and nothing is started. Visor reads the declared phrases again every
30 seconds, so a newly installed app is heard within half a minute.

## Saying something unasked

`speak` has the glasses say a line in their own voice now, with nobody asking: a timer that ran out, a door that
opened, a message worth hearing. It needs the `voice` scope.

```kotlin
visor.voice.speak("Your coffee is ready.")
```

Keep it short and rare: the wearer hears it over whatever they are doing. The text holds at most 500 characters.

> [!NOTE]
> Visor speaks through the glasses' data collection service, which starts for it and stops after a minute of quiet.
> It works on the Meta Ray-Ban Display.

## What the glasses hear

Scope: `microphone`. All glasses, on Android 13 or later.

```kotlin
visor.voice.transcripts(TranscriptRequest { language = "en-US" }).collect { transcript ->
    println(transcript.text)
}
```

`transcripts` turns what the glasses' microphones hear into text, one finished utterance at a time, while you
collect it. Visor records the glasses as a Bluetooth headset, never the phone's own microphone, and the phone's
speech recogniser makes the text: on the phone when it holds the language, otherwise as the phone's recogniser
does it. Without a `language`, Visor hears in the phone's language.

While transcripts run, the glasses act as a call headset, so music pauses as in a call. Android lets Visor record
only while it may use the microphone: while your app is on screen, or Visor is. The VDK lends Visor your app's
access when it binds, so keep collecting from a visible screen. From a computer, keep Visor on screen.

The Meta Ray-Ban Display is no Bluetooth headset to the phone. There the Display hears the wearer itself, with the
speech recognition of Meta's toolkit app, as DAT's speech does: in the Display's own language, which `language`
cannot change, and each transcript says. The toolkit app must be on the screen while it listens. If your app shows
no layout, the Display shows a Listening screen with your app's name, so the wearer knows who listens; going back on
the glasses ends the stream with `closed`.

Each `Transcript` has the `text`, the `language` it was heard in, the `time` the utterance ended, and the
recogniser's `confidence` from 0 to 1 when it gives one. Several apps can listen at once, in one language: an app
that asks for another language meanwhile fails with `busy`.

Ask for `partials` to get the text while the wearer speaks, as DAT gives it. A partial transcript has `partial`
true, and the next one replaces it; the finished utterance comes last with `partial` false:

```kotlin
visor.voice.transcripts(TranscriptRequest { partials = true }).collect { t ->
    if (t.partial) caption.text = t.text else log.append(t.text)
}
```

The stream ends with:

| Code | Reason | Why |
|---|---|---|
| `unsupported` | `no_recognizer` | Android 12, or a phone without speech recognition. |
| `unsupported` | `no_permission` | Visor may not record. The person allows the microphone in Visor's settings. |
| `not_connected` | | The glasses are not connected over Bluetooth. |
| `busy` | | Another app hears in another language. |
| `invalid_params` | | `language` is not a BCP 47 tag. |
| `failed` | `no_headset` | The glasses are no headset to the phone, or the recording stopped. |
| `failed` | `recognizer_failed` | The phone's recogniser stopped, for example for a language it lacks, or the Display's failed. |
| `closed` | `back` | On the Display, the wearer went back from the toolkit app. |

## Tools for the model

Scope: `assistant`. All glasses.

```kotlin
val tools = listOf(
    ToolSpec {
        name = "set_lights"
        description = "Turns the living room lights on or off."
        schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("on") { put("type", "boolean") } }
            putJsonArray("required") { add("on") }
        }
    },
)
visor.assistant.tools(tools).collect { call ->
    val on = call.arguments.jsonObject["on"]?.jsonPrimitive?.boolean
    if (on == null) visor.assistant.fail(call, "Say whether to turn them on or off.")
    else visor.assistant.answer(call, if (lights.set(on)) "Done." else "The lights did not answer.")
}
```

While the app collects `tools`, the model can call them, alongside the person's own MCP servers, the Display's
own tools and those of a [web app](web-apps.md#tools-for-the-assistant) open on it. Each call comes
as a `ToolCall`; answer it with `answer` or `fail` within 15 seconds. The model reads the text and words its answer
to the wearer from it, so say what happened, not how to say it.

- **Names** start with a letter, then letters, digits, `_` or `-`, 40 at most. When two apps offer the same name,
  the second one's gets a `_` added for the model; the app still receives its own name.
- **Schemas** are JSON Schema objects. Without one, the tool takes no arguments.
- **Descriptions** are what the model goes by to choose a tool. Say what it does and when it applies.
- Visor's diary records each call with the app's name and whether it worked.

## The conversations

Visor keeps what the wearer asks the assistant, and its answers, on the phone until the person deletes them. With
the `conversations` scope an app can read them, and get each new one as it is answered: a journal, a search over
everything asked, a sync to the person's own notes.

```kotlin
visor.assistant.turns.collect { turn -> println("${turn.question} -> ${turn.answer}") }
val lastWeek = visor.assistant.history(since = System.currentTimeMillis() - 7 * 86_400_000L, limit = 500)
```

An `AssistantTurn`:

| Field | Type | Meaning |
|---|---|---|
| `id` | `String` | Stable: a turn asked again comes again with the same id and the new answer. |
| `time` | `Long` | When it was asked, in milliseconds since the epoch. |
| `question`, `answer` | `String` | The answer is empty when the turn failed. |
| `failure` | `String` | Why it failed, for a log; empty when it did not. |
| `source` | `TurnSource` | `glasses`, `phone` (Visor's Meta AI page) or `live_ai`. |
| `conversation` | `String` | Turns asked close together share one, as do the turns of one Live AI session. |
| `photo` | `Boolean` | True when the glasses sent a photo with the question. The photo stays in Visor. |
| `tools` | `List<ToolUse>` | What the assistant did with the person's tools: `server`, `what`, `ok`. |

`history` returns the newest turns first, at most `limit` (1 to 500, 50 by default).

## Notifications on the Display

Scope: `notify`. The Meta Ray-Ban Display.

```kotlin
visor.notify.post("delivery", "Package", "Your delivery is at the door.")
visor.notify.remove("delivery")
```

A second post with the same id replaces the first. Glasses without a display show no notifications of apps, so
there `post` fails with `unsupported`.

## JSON-RPC

| | Name | Params | Answer |
|---|---|---|---|
| Topic | `voice.requests` | `phrases` | `VoiceRequest` |
| Method | `voice.reply` | `request`, `text`, `listen` (default false) | `{}` |
| Method | `voice.speak` | `text` | `{}` |
| Topic | `voice.transcripts` | `language`, `partials` (optional) | `Transcript`: `text`, `language`, `time`, `partial`, `confidence` |
| Topic | `assistant.tools` | `tools`: a list of `{name, description, schema}` | `ToolCall` |
| Method | `assistant.tools.answer` | `call`, `text`, `error` (default false) | `{}` |
| Topic | `assistant.turns` | none | `AssistantTurn` |
| Method | `assistant.history` | `since` (optional), `limit` (default 50) | a list of `AssistantTurn` |
| Method | `notify.post` | `id`, `title`, `text` | `{}` |
| Method | `notify.remove` | `id` | `{}` |

From your computer, the `visor` command can play the app's part:

```
visor watch voice.requests '{"phrases":["the garage"]}'
visor call voice.reply '{"request":"<id from the event>","text":"Closing the garage."}'
```
