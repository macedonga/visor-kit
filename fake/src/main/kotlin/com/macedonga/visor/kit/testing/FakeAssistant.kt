package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.AssistantTurn
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.HistoryQuery
import com.macedonga.visor.kit.protocol.Notice
import com.macedonga.visor.kit.protocol.ToolAnswer
import com.macedonga.visor.kit.protocol.ToolCall
import com.macedonga.visor.kit.protocol.ToolSpec
import com.macedonga.visor.kit.protocol.Transcript
import com.macedonga.visor.kit.protocol.TranscriptRequest
import com.macedonga.visor.kit.protocol.VoiceReply
import com.macedonga.visor.kit.protocol.VoiceRequest
import com.macedonga.visor.kit.server.Phrases
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject

/** The wearer's voice, for an app that answers questions through [com.macedonga.visor.kit.Voice]. */
public class FakeVoice internal constructor() {
    private class Offer(val phrases: List<String>, val words: List<List<String>>, val requests: SendChannel<VoiceRequest>)

    private val offers = CopyOnWriteArrayList<Offer>()
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<VoiceReply>>()
    private val ids = AtomicInteger()

    /**
     * Asks [question] as the wearer would, and returns the reply of the app whose phrase it holds. Null when no app's
     * phrase matches, or when the app does not reply within 5 seconds.
     */
    public suspend fun ask(question: String): VoiceReply? {
        val words = Phrases.words(question)
        val (offer, phrase) = offers.mapNotNull { o -> Phrases.best(words, o.words)?.let { o to it } }
            .maxByOrNull { (o, i) -> o.words[i].size } ?: return null
        val id = "question-${ids.incrementAndGet()}"
        val reply = CompletableDeferred<VoiceReply>()
        waiting[id] = reply
        try {
            offer.requests.trySend(VoiceRequest(id, question, offer.phrases[phrase]))
            return withTimeoutOrNull(WAIT_MS) { reply.await() }
        } finally {
            waiting.remove(id)
        }
    }

    internal fun offer(phrases: List<String>): Flow<VoiceRequest> = channelFlow {
        val offer = Offer(phrases, phrases.map(Phrases::words), channel)
        if (offer.words.all { it.isEmpty() }) throw KitException(ErrorCode.INVALID_PARAMS, "No phrase has a word in it.")
        offers += offer
        awaitClose { offers -= offer }
    }

    internal fun replied(reply: VoiceReply) {
        waiting[reply.request]?.complete(reply)
            ?: throw KitException(ErrorCode.INVALID_PARAMS, "No question ${reply.request} is waiting.", reason = "expired")
    }

    private val heard = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val _listening = MutableStateFlow(0)

    /** How many apps collect transcripts now. Wait for one before [say], as a transcript nobody hears is lost. */
    public val listening: StateFlow<Int> = _listening.asStateFlow()

    /** The glasses hear [text], as one utterance, and every app that collects transcripts gets it. */
    public fun say(text: String) {
        heard.tryEmit(text)
    }

    internal fun transcripts(request: TranscriptRequest): Flow<Transcript> {
        val language = request.language ?: Locale.getDefault().toLanguageTag()
        return heard.map { Transcript(it, language, System.currentTimeMillis()) }
            .onStart { _listening.update { it + 1 } }
            .onCompletion { _listening.update { it - 1 } }
    }
}

/** The assistant's model, for an app that offers tools through [com.macedonga.visor.kit.Assistant]. */
public class FakeAssistant internal constructor() {
    private class Offer(val tools: List<ToolSpec>, val calls: SendChannel<ToolCall>)

    private val offers = CopyOnWriteArrayList<Offer>()
    private val waiting = ConcurrentHashMap<String, CompletableDeferred<ToolAnswer>>()
    private val ids = AtomicInteger()

    /** Every tool the app offers now. */
    public val tools: List<ToolSpec> get() = offers.flatMap { it.tools }

    private val _history = MutableStateFlow<List<AssistantTurn>>(emptyList())

    /** Every turn [turn] made, oldest first. */
    public val history: StateFlow<List<AssistantTurn>> = _history.asStateFlow()
    private val answeredTurns = MutableSharedFlow<AssistantTurn>(extraBufferCapacity = 64)

    /** The wearer asks [question] and the assistant answers [answer]: apps that watch the turns get it. */
    public fun turn(question: String, answer: String): AssistantTurn {
        val turn = AssistantTurn("turn-${ids.incrementAndGet()}", System.currentTimeMillis(), question, answer, conversation = "fake")
        _history.update { it + turn }
        answeredTurns.tryEmit(turn)
        return turn
    }

    internal fun turns(): Flow<AssistantTurn> = answeredTurns

    internal fun history(query: HistoryQuery): List<AssistantTurn> {
        if (query.limit !in 1..HistoryQuery.MAX_LIMIT) {
            throw KitException(ErrorCode.INVALID_PARAMS, "The limit must be 1 to ${HistoryQuery.MAX_LIMIT}.")
        }
        val since = query.since
        return _history.value.asReversed().filter { since == null || it.time > since }.take(query.limit)
    }

    /**
     * Calls [tool] with [arguments] as the model would, and returns the app's answer. Null when no app offers the tool,
     * or when it does not answer within 5 seconds.
     */
    public suspend fun call(tool: String, arguments: JsonObject): ToolAnswer? {
        val offer = offers.firstOrNull { o -> o.tools.any { it.name == tool } } ?: return null
        val id = "call-${ids.incrementAndGet()}"
        val answer = CompletableDeferred<ToolAnswer>()
        waiting[id] = answer
        try {
            offer.calls.trySend(ToolCall(id, tool, arguments))
            return withTimeoutOrNull(WAIT_MS) { answer.await() }
        } finally {
            waiting.remove(id)
        }
    }

    internal fun offer(tools: List<ToolSpec>): Flow<ToolCall> = channelFlow {
        val offer = Offer(tools, channel)
        offers += offer
        awaitClose { offers -= offer }
    }

    internal fun answered(answer: ToolAnswer) {
        waiting[answer.call]?.complete(answer)
            ?: throw KitException(ErrorCode.INVALID_PARAMS, "No call ${answer.call} is waiting.", reason = "expired")
    }
}

/** The Display's notifications, for an app that posts them through [com.macedonga.visor.kit.Notify]. */
public class FakeNotify internal constructor() {
    private val _shown = MutableStateFlow<Map<String, Notice>>(emptyMap())

    /** The notifications on the fake Display, by their id. */
    public val shown: StateFlow<Map<String, Notice>> = _shown.asStateFlow()

    internal fun posted(notice: Notice) {
        _shown.value += notice.id to notice
    }

    internal fun removed(id: String) {
        _shown.value -= id
    }
}

private const val WAIT_MS = 5_000L
