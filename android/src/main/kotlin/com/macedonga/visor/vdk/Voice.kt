package com.macedonga.visor.vdk

import com.macedonga.visor.vdk.protocol.Methods
import com.macedonga.visor.vdk.protocol.Notice
import com.macedonga.visor.vdk.protocol.NoticeRemoval
import com.macedonga.visor.vdk.protocol.Scope
import com.macedonga.visor.vdk.protocol.ToolAnswer
import com.macedonga.visor.vdk.protocol.ToolCall
import com.macedonga.visor.vdk.protocol.ToolSpec
import com.macedonga.visor.vdk.protocol.ToolsOffer
import com.macedonga.visor.vdk.protocol.Topics
import com.macedonga.visor.vdk.protocol.VoiceOffer
import com.macedonga.visor.vdk.protocol.VoiceReply
import com.macedonga.visor.vdk.protocol.VoiceRequest
import kotlinx.coroutines.flow.Flow

/** The wearer's questions that name the app, answered in the glasses' own voice. Needs [Scope.VOICE]. */
public class Voice internal constructor(private val visor: Visor) {
    /**
     * The questions that hold one of [phrases], while collected: "Hey Meta, ask the garage to close" comes here for
     * the phrase "the garage", and the assistant's model does not answer it. Answer each with [reply] within about
     * 20 seconds.
     */
    public fun requests(phrases: List<String>): Flow<VoiceRequest> =
        visor.subscribe(Topics.VOICE_REQUESTS, VoiceOffer(phrases))

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
}

/** Notifications on the Meta Ray-Ban Display. Needs [Scope.NOTIFY]; glasses without a display show none. */
public class Notify internal constructor(private val visor: Visor) {
    /** Shows a notification, or replaces the app's one with the same [id]. */
    public suspend fun post(id: String, title: String, text: String): Unit =
        visor.call(Methods.NOTIFY_POST, Notice(id, title, text))

    public suspend fun remove(id: String): Unit = visor.call(Methods.NOTIFY_REMOVE, NoticeRemoval(id))
}
