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

Each `Transcript` has the `text`, the `language` it was heard in, and the `time` the utterance ended. Several apps
can listen at once, in one language: an app that asks for another language meanwhile fails with `busy`.

The stream ends with:

| Code | Reason | Why |
|---|---|---|
| `unsupported` | `no_recognizer` | Android 12, or a phone without speech recognition. |
| `unsupported` | `no_permission` | Visor may not record. The person allows the microphone in Visor's settings. |
| `not_connected` | | The glasses are not connected over Bluetooth. |
| `busy` | | Another app hears in another language. |
| `invalid_params` | | `language` is not a BCP 47 tag. |
| `failed` | `no_headset` | The glasses are no headset to the phone, or the recording stopped. |
| `failed` | `recognizer_failed` | The phone's recogniser stopped, for example for a language it lacks. |

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

While the app collects `tools`, the model can call them, alongside the person's own MCP servers. Each call comes
as a `ToolCall`; answer it with `answer` or `fail` within 15 seconds. The model reads the text and words its answer
to the wearer from it, so say what happened, not how to say it.

- **Names** start with a letter, then letters, digits, `_` or `-`, 40 at most. When two apps offer the same name,
  the second one's gets a `_` added for the model; the app still receives its own name.
- **Schemas** are JSON Schema objects. Without one, the tool takes no arguments.
- **Descriptions** are what the model goes by to choose a tool. Say what it does and when it applies.
- Visor's diary records each call with the app's name and whether it worked.

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
| Topic | `voice.transcripts` | `language` (optional) | `Transcript` |
| Topic | `assistant.tools` | `tools`: a list of `{name, description, schema}` | `ToolCall` |
| Method | `assistant.tools.answer` | `call`, `text`, `error` (default false) | `{}` |
| Method | `notify.post` | `id`, `title`, `text` | `{}` |
| Method | `notify.remove` | `id` | `{}` |

From your computer, the `visor` command can play the app's part:

```
visor watch voice.requests '{"phrases":["the garage"]}'
visor call voice.reply '{"request":"<id from the event>","text":"Closing the garage."}'
```
