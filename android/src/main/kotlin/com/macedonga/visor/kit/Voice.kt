package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.AssistantTurn
import com.macedonga.visor.kit.protocol.HistoryQuery
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Notice
import com.macedonga.visor.kit.protocol.NoticeRemoval
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.ToolAnswer
import com.macedonga.visor.kit.protocol.ToolCall
import com.macedonga.visor.kit.protocol.ToolSpec
import com.macedonga.visor.kit.protocol.ToolsOffer
import com.macedonga.visor.kit.protocol.Topics
import com.macedonga.visor.kit.protocol.Transcript
import com.macedonga.visor.kit.protocol.TranscriptRequest
import com.macedonga.visor.kit.protocol.VoiceOffer
import com.macedonga.visor.kit.protocol.VoiceReply
import com.macedonga.visor.kit.protocol.VoiceRequest
import kotlinx.coroutines.flow.Flow

/**
 * The wearer's questions that name the app, answered in the glasses' own voice, with [Scope.VOICE]; and what the
 * glasses hear, as text, with [Scope.MICROPHONE].
 */
public class Voice internal constructor(private val visor: Visor) {
    /**
     * The questions that hold one of [phrases], while collected: "Hey Meta, ask the garage to close" comes here for
     * the phrase "the garage", and the assistant's model does not answer it. Answer each with [reply] within about
     * 20 seconds.
     */
    public fun requests(phrases: List<String>): Flow<VoiceRequest> =
        visor.subscribe(Topics.VOICE_REQUESTS, VoiceOffer(phrases))

    /**
     * What the glasses' microphones hear, as text, one finished utterance at a time, while collected. Visor records
     * only while it may use the microphone: while your app, or Visor, is on screen. Needs [Scope.MICROPHONE].
     */
    public fun transcripts(request: TranscriptRequest): Flow<Transcript> = visor.subscribe(Topics.VOICE_TRANSCRIPTS, request)

    /** [transcripts] in the phone's language. */
    public fun transcripts(): Flow<Transcript> = transcripts(TranscriptRequest {})

    /** The glasses say [text] to the wearer, and the turn ends. */
    public suspend fun reply(request: VoiceRequest, text: String): Unit = reply(request, text, listen = false)

    /** The glasses say [text] to the wearer; with [listen], they keep the microphone open for a follow-up. */
    public suspend fun reply(request: VoiceRequest, text: String, listen: Boolean): Unit =
        visor.call(Methods.VOICE_REPLY, VoiceReply(request.id, text, listen))
}

/** Tools for the assistant's model, which it calls while it answers the wearer. Needs [Scope.ASSISTANT]. */
public class Assistant internal constructor(private val visor: Visor) {
    /**
     * Offers the model [tools] while collected, and brings each call of one. Answer each with [answer] or [fail]
     * within 15 seconds; the model reads the text.
     */
    public fun tools(tools: List<ToolSpec>): Flow<ToolCall> = visor.subscribe(Topics.ASSISTANT_TOOLS, ToolsOffer(tools))

    public suspend fun answer(call: ToolCall, text: String): Unit =
        visor.call(Methods.ASSISTANT_TOOLS_ANSWER, ToolAnswer(call.id, text, error = false))

    /** The tool failed; [message] tells the model why. */
    public suspend fun fail(call: ToolCall, message: String): Unit =
        visor.call(Methods.ASSISTANT_TOOLS_ANSWER, ToolAnswer(call.id, message, error = true))

    /** Each question asked of the assistant with its answer, as it is answered. Needs [Scope.CONVERSATIONS]. */
    public val turns: Flow<AssistantTurn>
        get() = visor.subscribe(Topics.ASSISTANT_TURNS, Unit)

    /** The last [HistoryQuery.DEFAULT_LIMIT] turns kept on the phone, newest first. Needs [Scope.CONVERSATIONS]. */
    public suspend fun history(): List<AssistantTurn> = visor.call(Methods.ASSISTANT_HISTORY, HistoryQuery())

    /** At most [limit] turns asked after [since], in milliseconds since the epoch, newest first. */
    public suspend fun history(since: Long, limit: Int): List<AssistantTurn> =
        visor.call(Methods.ASSISTANT_HISTORY, HistoryQuery(since, limit))
}

/** Notifications on the Meta Ray-Ban Display. Needs [Scope.NOTIFY]; glasses without a display show none. */
public class Notify internal constructor(private val visor: Visor) {
    /** Shows a notification, or replaces the app's one with the same [id]. */
    public suspend fun post(id: String, title: String, text: String): Unit =
        visor.call(Methods.NOTIFY_POST, Notice(id, title, text))

    public suspend fun remove(id: String): Unit = visor.call(Methods.NOTIFY_REMOVE, NoticeRemoval(id))
}
