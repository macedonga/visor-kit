package com.macedonga.visor.vdk.server

import com.macedonga.visor.vdk.protocol.VdkInternal
import java.text.Normalizer

/**
 * How a question is matched to an app's voice phrases: a phrase matches when all its words appear in the question, in
 * order and next to each other. Words are compared in lower case, without accents or punctuation.
 */
@VdkInternal
public object Phrases {
    /** The words of [text] as they are compared. */
    public fun words(text: String): List<String> =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(MARKS, "")
            .split(NOT_WORD)
            .filter { it.isNotEmpty() }

    /** The index of the longest of [phrases] found in [question], or null when none is. */
    public fun best(question: List<String>, phrases: List<List<String>>): Int? =
        phrases.indices.filter { phrases[it].isNotEmpty() && contains(question, phrases[it]) }.maxByOrNull { phrases[it].size }

    private fun contains(question: List<String>, phrase: List<String>): Boolean =
        (0..question.size - phrase.size).any { start -> phrase.indices.all { question[start + it] == phrase[it] } }

    private val MARKS = Regex("\\p{M}+")
    private val NOT_WORD = Regex("[^\\p{L}\\p{N}]+")
}
