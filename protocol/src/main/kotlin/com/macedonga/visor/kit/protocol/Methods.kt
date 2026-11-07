package com.macedonga.visor.kit.protocol

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Every method Visor answers. A method without params takes [Unit], which is `{}` on the wire, and a method with
 * nothing to return answers [Unit].
 */
public object Methods {
    /** Opens a session: the app says what it speaks, Visor says what it answers and what the app may do. */
    public val HELLO: Method<Hello, Welcome> = Method(
        "kit.hello", null, 1,
        "Starts a session and returns what Visor answers and which scopes the app holds.",
        Hello.serializer(), Welcome.serializer(),
    )

    public val GLASSES_STATUS: Method<Unit, GlassesStatus> = Method(
        "glasses.status", Scope.STATE, 1,
        "Returns the glasses' link, model, battery, and whether they are worn, folded or recording.",
        Unit.serializer(), GlassesStatus.serializer(),
    )

    /** Every pair set up in Visor, the active one marked. */
    public val GLASSES_PAIRS: Method<Unit, List<GlassesPair>> = Method(
        "glasses.pairs", Scope.PAIRS, 1,
        "Lists every pair of glasses set up in Visor, with its name, model, looks and last battery.",
        Unit.serializer(), ListSerializer(GlassesPair.serializer()),
    )

    /**
     * Makes [PairSelection.serial] the active pair: Visor lets go of the pair connected now and connects to it, as when
     * the person picks it on Visor's home. Answers once Visor has switched, before the new pair is connected. Fails
     * with [ErrorCode.INVALID_PARAMS] and reason `not_found` for a serial Visor has not set up, and with
     * [ErrorCode.BUSY] while a Live view or an import holds the glasses.
     */
    public val GLASSES_SELECT: Method<PairSelection, GlassesPair> = Method(
        "glasses.select", Scope.PAIRS, 1,
        "Makes another pair of glasses the active one, the pair every other method acts on.",
        PairSelection.serializer(), GlassesPair.serializer(),
    )

    /**
     * Fails with [ErrorCode.NOT_CONNECTED], [ErrorCode.BUSY] while a video records, [ErrorCode.TIMEOUT] when the
     * glasses do not take it, and the reasons of [CAPTURES_IMPORT] when the import fails.
     */
    public val CAMERA_PHOTO: Method<PhotoRequest, Capture> = Method(
        "camera.photo", Scope.CAMERA, 1,
        "Takes a photo with the glasses and, unless asked not to, imports it into the phone's gallery.",
        PhotoRequest.serializer(), Capture.serializer(),
    )

    /** Answers once the glasses say they record. */
    public val CAMERA_VIDEO_START: Method<VideoRequest, Unit> = Method(
        "camera.video.start", Scope.CAMERA, 1,
        "Starts a video or a hyperlapse on the glasses.",
        VideoRequest.serializer(), Unit.serializer(),
    )

    /** Answers once the glasses say they stopped, or at once when no video runs. */
    public val CAMERA_VIDEO_STOP: Method<Unit, Unit> = Method(
        "camera.video.stop", Scope.CAMERA, 1,
        "Stops the video the glasses record.",
        Unit.serializer(), Unit.serializer(),
    )

    /**
     * Takes a photo with the camera that [Feeds.CAMERA_LIVE] streams, while it streams, at the glasses' full photo
     * size. Fails with [ErrorCode.BUSY] when the Live is not running, and [ErrorCode.TIMEOUT] when no photo comes.
     */
    public val CAMERA_LIVE_PHOTO: Method<Unit, LivePhoto> = Method(
        "camera.live.photo", Scope.CAMERA, 1,
        "Takes a photo while the camera is live, without stopping the stream.",
        Unit.serializer(), LivePhoto.serializer(),
    )

    public val CAPTURES_LIST: Method<Unit, List<Capture>> = Method(
        "captures.list", Scope.CAMERA, 1,
        "Lists the photos and videos on the glasses that the phone does not have yet.",
        Unit.serializer(), ListSerializer(Capture.serializer()),
    )

    /**
     * Answers with the capture once it is in the gallery, at once when it already is. A failed import has one of
     * these as its [Failure.reason]: `not_found` (no capture with that id), `wifi_off`, `wifi_stuck` (only turning
     * Wi-Fi off and on helps), `wifi_failed`, `glasses_did_not_join`, `no_wifi_session`, `not_allowed`, `cut_off`
     * (the glasses went away halfway), `damaged`, `not_saved` (the gallery refused it) or `failed`.
     */
    public val CAPTURES_IMPORT: Method<ImportRequest, Capture> = Method(
        "captures.import", Scope.CAMERA, 1,
        "Copies a photo or video from the glasses into the phone's gallery over Wi-Fi Direct.",
        ImportRequest.serializer(), Capture.serializer(),
    )

    public val BAND_STATUS: Method<Unit, BandStatus> = Method(
        "band.status", Scope.BAND, 1,
        "Returns the Neural Band's link, battery and wrist.",
        Unit.serializer(), BandStatus.serializer(),
    )

    /**
     * Borrows the band for the moment it takes. Fails with [ErrorCode.UNSUPPORTED] when no band is paired, and with
     * the reason `not_set_up` when only the Display holds it ([BandStatus.held] false).
     */
    public val BAND_BUZZ: Method<BuzzRequest, Unit> = Method(
        "band.buzz", Scope.BAND, 1,
        "Vibrates the Neural Band.",
        BuzzRequest.serializer(), Unit.serializer(),
    )

    /** A second show replaces the text on the Display. Fails with [ErrorCode.UNSUPPORTED] on glasses without one. */
    public val DISPLAY_PROMPTER_SHOW: Method<PrompterRequest, Unit> = Method(
        "display.prompter.show", Scope.DISPLAY, 1,
        "Shows a script on the Display's teleprompter.",
        PrompterRequest.serializer(), Unit.serializer(),
    )

    public val DISPLAY_PROMPTER_CLOSE: Method<Unit, Unit> = Method(
        "display.prompter.close", Scope.DISPLAY, 1,
        "Closes the Display's teleprompter.",
        Unit.serializer(), Unit.serializer(),
    )

    /**
     * Shows a layout on the Display, as a page of Visor's that fills the screen, and replaces the one shown before. The
     * page opens when the first layout comes, and later ones update it in place. Fails with [ErrorCode.UNSUPPORTED] on
     * glasses without a display.
     */
    public val DISPLAY_LAYOUT_SHOW: Method<Layout, Unit> = Method(
        "display.layout.show", Scope.DISPLAY, 1,
        "Shows a layout of text, images and buttons on the Display.",
        Layout.serializer(), Unit.serializer(),
    )

    /** Empties the page, and stops serving it. The wearer leaves it as any other app. */
    public val DISPLAY_LAYOUT_CLOSE: Method<Unit, Unit> = Method(
        "display.layout.close", Scope.DISPLAY, 1,
        "Empties the layout page on the Display.",
        Unit.serializer(), Unit.serializer(),
    )

    /**
     * Answers a [VoiceRequest]: the glasses say [VoiceReply.text] in their own voice. Fails with
     * [ErrorCode.INVALID_PARAMS] and reason `expired` when the question was answered already or the glasses gave up
     * on it: they wait about 20 seconds.
     */
    public val VOICE_REPLY: Method<VoiceReply, Unit> = Method(
        "voice.reply", Scope.VOICE, 1,
        "Answers a question the wearer asked the app, in the glasses' own voice.",
        VoiceReply.serializer(), Unit.serializer(),
    )

    /** Answers a [ToolCall]. Fails like [VOICE_REPLY] once the model stopped waiting, after 15 seconds. */
    public val ASSISTANT_TOOLS_ANSWER: Method<ToolAnswer, Unit> = Method(
        "assistant.tools.answer", Scope.ASSISTANT, 1,
        "Answers the assistant's call of one of the app's tools.",
        ToolAnswer.serializer(), Unit.serializer(),
    )

    /** Fails with [ErrorCode.UNSUPPORTED] on glasses without a display, which show no notifications of apps. */
    public val NOTIFY_POST: Method<Notice, Unit> = Method(
        "notify.post", Scope.NOTIFY, 1,
        "Shows a notification on the Display, or replaces the app's one with the same id.",
        Notice.serializer(), Unit.serializer(),
    )

    public val NOTIFY_REMOVE: Method<NoticeRemoval, Unit> = Method(
        "notify.remove", Scope.NOTIFY, 1,
        "Takes one of the app's notifications away from the Display.",
        NoticeRemoval.serializer(), Unit.serializer(),
    )

    /**
     * Fails with [ErrorCode.UNSUPPORTED] while Developer mode is off in Visor, and with [ErrorCode.REFUSED] for the
     * services Visor keeps to itself: identity and ownership (36), the link's authentication (54 and 79), updates (29).
     */
    public val RAW_SEND: Method<RawSend, Unit> = Method(
        "raw.send", Scope.RAW, 1,
        "Sends one message to a service of the glasses, as it is.",
        RawSend.serializer(), Unit.serializer(),
    )

    /** Every method, in the order of the reference. */
    public val all: List<Method<*, *>> = listOf(
        HELLO,
        GLASSES_STATUS, GLASSES_PAIRS, GLASSES_SELECT,
        CAMERA_PHOTO, CAMERA_VIDEO_START, CAMERA_VIDEO_STOP, CAMERA_LIVE_PHOTO,
        CAPTURES_LIST, CAPTURES_IMPORT,
        BAND_STATUS, BAND_BUZZ,
        DISPLAY_PROMPTER_SHOW, DISPLAY_PROMPTER_CLOSE, DISPLAY_LAYOUT_SHOW, DISPLAY_LAYOUT_CLOSE,
        VOICE_REPLY,
        ASSISTANT_TOOLS_ANSWER,
        NOTIFY_POST, NOTIFY_REMOVE,
        RAW_SEND,
    )
}

/** Every topic Visor streams. A topic without params takes [Unit]. */
public object Topics {
    /** Sends the status at once, then each change. */
    public val GLASSES_STATUS: Topic<Unit, GlassesStatus> = Topic(
        "glasses.status", Scope.STATE, 1,
        "The glasses' status now, then each change.",
        Unit.serializer(), GlassesStatus.serializer(),
    )

    /** Sends the list at once, then each time a pair is added, renamed, removed or made active. */
    public val GLASSES_PAIRS: Topic<Unit, List<GlassesPair>> = Topic(
        "glasses.pairs", Scope.PAIRS, 1,
        "Every pair of glasses set up in Visor, now and on each change.",
        Unit.serializer(), ListSerializer(GlassesPair.serializer()),
    )

    /**
     * Each touch of the touchpad and each press of a button, as the glasses report it to the phone. The glasses still
     * act on it as they always do: a tap still pauses the music.
     */
    public val GLASSES_INPUTS: Topic<Unit, GlassesInput> = Topic(
        "glasses.inputs", Scope.INPUTS, 1,
        "Each touch of the glasses' touchpad and each press of their buttons.",
        Unit.serializer(), GlassesInput.serializer(),
    )

    /** Sends each capture once. After Visor restarts, it sends again the ones still waiting. */
    public val CAPTURES_AVAILABLE: Topic<Unit, Capture> = Topic(
        "captures.available", Scope.CAMERA, 1,
        "Each new photo or video on the glasses that the phone does not have yet.",
        Unit.serializer(), Capture.serializer(),
    )

    /** Covers every import, also those Visor starts on its own. */
    public val CAPTURES_PROGRESS: Topic<Unit, CaptureProgress> = Topic(
        "captures.progress", Scope.CAMERA, 1,
        "Each import's progress, at most once per whole percent.",
        Unit.serializer(), CaptureProgress.serializer(),
    )

    /** Covers every import, also those Visor starts on its own. */
    public val CAPTURES_IMPORTED: Topic<Unit, Capture> = Topic(
        "captures.imported", Scope.CAMERA, 1,
        "Each capture that reaches the phone's gallery.",
        Unit.serializer(), Capture.serializer(),
    )

    public val CAPTURES_FAILED: Topic<Unit, CaptureFailure> = Topic(
        "captures.failed", Scope.CAMERA, 1,
        "Each import that did not finish.",
        Unit.serializer(), CaptureFailure.serializer(),
    )

    /** Sends the status at once, then each change. */
    public val BAND_STATUS: Topic<Unit, BandStatus> = Topic(
        "band.status", Scope.BAND, 1,
        "The Neural Band's status now, then each change.",
        Unit.serializer(), BandStatus.serializer(),
    )

    /** The clicks on the app's own layout, while it shows. Only the app that showed the layout gets them. */
    public val DISPLAY_LAYOUT_CLICKS: Topic<Unit, LayoutClick> = Topic(
        "display.layout.clicks", Scope.DISPLAY, 1,
        "Each click on a button or box of the layout the app shows.",
        Unit.serializer(), LayoutClick.serializer(),
    )

    /**
     * While any app subscribes, the phone borrows the band: its gestures come here and no longer reach the Display.
     * The band goes back to the Display when the last subscriber leaves. Ends with [ErrorCode.UNSUPPORTED] when no
     * band is paired, and with the reason `not_set_up` when only the Display holds it ([BandStatus.held] false).
     */
    public val BAND_GESTURES: Topic<Unit, Gesture> = Topic(
        "band.gestures", Scope.BAND, 1,
        "Every gesture the Neural Band reads, while the phone borrows it from the Display.",
        Unit.serializer(), Gesture.serializer(),
    )

    /**
     * While the app subscribes, the wearer's questions that hold one of its [VoiceOffer.phrases] come to it instead of
     * the assistant's model. Each needs a [Methods.VOICE_REPLY] within about 20 seconds, or the glasses tell the
     * wearer the app did not answer. Ends with [ErrorCode.INVALID_PARAMS] when no phrase has a word in it.
     */
    public val VOICE_REQUESTS: Topic<VoiceOffer, VoiceRequest> = Topic(
        "voice.requests", Scope.VOICE, 1,
        "The wearer's questions that name one of the app's phrases, for the app to answer.",
        VoiceOffer.serializer(), VoiceRequest.serializer(),
    )

    /**
     * What the glasses' microphones hear, as text, one utterance at a time, while subscribed. Visor records the glasses
     * as a Bluetooth headset, never the phone, and the phone's speech recogniser makes the text. Meanwhile the glasses
     * act as a call headset, so music pauses. Visor records only while it may use the microphone: while an app that
     * subscribes is on screen, or Visor is. Ends with [ErrorCode.UNSUPPORTED] and the reason `no_recognizer` (Android
     * 12 or a phone without speech recognition) or `no_permission` (Visor may not record), [ErrorCode.NOT_CONNECTED],
     * [ErrorCode.BUSY] while another app hears in another language, or [ErrorCode.FAILED] with `no_headset` or
     * `recognizer_failed`.
     */
    public val VOICE_TRANSCRIPTS: Topic<TranscriptRequest, Transcript> = Topic(
        "voice.transcripts", Scope.MICROPHONE, 1,
        "What the glasses' microphones hear, as text, one utterance at a time.",
        TranscriptRequest.serializer(), Transcript.serializer(),
    )

    /**
     * While the app subscribes, the assistant's model may use its [ToolsOffer.tools], and each call comes here. Each
     * needs a [Methods.ASSISTANT_TOOLS_ANSWER] within 15 seconds. Ends with [ErrorCode.INVALID_PARAMS] when a tool's
     * name or schema does not fit.
     */
    public val ASSISTANT_TOOLS: Topic<ToolsOffer, ToolCall> = Topic(
        "assistant.tools", Scope.ASSISTANT, 1,
        "Offers the assistant's model the app's tools, and brings each call of one.",
        ToolsOffer.serializer(), ToolCall.serializer(),
    )

    /** Ends with the same failures as [Methods.RAW_SEND]. */
    public val RAW_MESSAGES: Topic<RawFilter, RawMessage> = Topic(
        "raw.messages", Scope.RAW, 1,
        "Every message one service of the glasses sends, as it is.",
        RawFilter.serializer(), RawMessage.serializer(),
    )

    /** Every topic, in the order of the reference. */
    public val all: List<Topic<*, *>> = listOf(
        GLASSES_STATUS, GLASSES_PAIRS, GLASSES_INPUTS,
        CAPTURES_AVAILABLE, CAPTURES_PROGRESS, CAPTURES_IMPORTED, CAPTURES_FAILED,
        BAND_STATUS, BAND_GESTURES,
        DISPLAY_LAYOUT_CLICKS,
        VOICE_REQUESTS, VOICE_TRANSCRIPTS,
        ASSISTANT_TOOLS,
        RAW_MESSAGES,
    )
}
