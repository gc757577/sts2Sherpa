package com.gchan.sts2_sherpa.ocr

import android.util.Log
import com.gchan.sts2_sherpa.data.SilentCard
import kotlin.math.max
import kotlin.math.min

class CardNameMatcher {
    fun findMatchingCards(
        ocrText: String,
        cards: List<SilentCard>,
        maxResults: Int = 3,
    ): List<SilentCard> {
        val normalizedText = ocrText.normalizeForCardMatch()
        if (normalizedText.isBlank()) return emptyList()

        val exactMatches = cards.mapNotNull { card ->
            val bestName = card.matchNames().firstOrNull { normalizedName ->
                normalizedName.isNotBlank() && normalizedText.contains(normalizedName)
            } ?: return@mapNotNull null

            CardMatch(
                card = card,
                score = EXACT_MATCH_SCORE + bestName.length,
                similarity = 1.0,
                source = "exact",
            )
        }

        val fuzzyMatches = if (exactMatches.size < maxResults) {
            cards
                .filterNot { card -> exactMatches.any { it.card.id == card.id } }
                .mapNotNull { card -> findBestFuzzyMatch(card, normalizedText) }
        } else {
            emptyList()
        }

        return (exactMatches + fuzzyMatches)
            .sortedWith(compareByDescending<CardMatch> { it.score }.thenBy { it.card.id })
            .distinctBy { it.card.id }
            .take(maxResults)
            .also { matches ->
                matches.forEach { match ->
                    Log.d(
                        TAG,
                        "Matched card=${match.card.id}, name=${match.card.displayName()}, " +
                            "score=${"%.3f".format(match.score)}, " +
                            "similarity=${"%.3f".format(match.similarity)}, source=${match.source}",
                    )
                }
            }
            .map { it.card }
    }

    private fun findBestFuzzyMatch(
        card: SilentCard,
        normalizedText: String,
    ): CardMatch? {
        return card.matchNames()
            .filter { it.isNotBlank() }
            .mapNotNull { normalizedName ->
                val threshold = if (normalizedName.length <= SHORT_NAME_MAX_LENGTH) {
                    SHORT_NAME_SIMILARITY_THRESHOLD
                } else {
                    SIMILARITY_THRESHOLD
                }

                val similarity = findBestSimilarity(
                    normalizedName = normalizedName,
                    normalizedText = normalizedText,
                )

                if (similarity >= threshold) {
                    CardMatch(
                        card = card,
                        score = similarity + normalizedName.length / NAME_LENGTH_SCORE_DIVISOR,
                        similarity = similarity,
                        source = "fuzzy",
                    )
                } else {
                    null
                }
            }
            .maxByOrNull { it.score }
    }

    private fun findBestSimilarity(
        normalizedName: String,
        normalizedText: String,
    ): Double {
        val nameLength = normalizedName.length
        if (nameLength == 0 || normalizedText.isBlank()) return 0.0

        val minWindowSize = max(1, nameLength - WINDOW_LENGTH_TOLERANCE)
        val maxWindowSize = min(normalizedText.length, nameLength + WINDOW_LENGTH_TOLERANCE)

        var bestSimilarity = 0.0
        for (windowSize in minWindowSize..maxWindowSize) {
            if (windowSize > normalizedText.length) continue

            normalizedText.windowed(
                size = windowSize,
                step = 1,
                partialWindows = false,
            ).forEach { ocrChunk ->
                bestSimilarity = max(
                    bestSimilarity,
                    normalizedName.similarityTo(ocrChunk),
                )
            }
        }

        return bestSimilarity
    }

    private fun SilentCard.matchNames(): List<String> =
        listOf(normalizedName, nameKo)
            .map { it.normalizeForCardMatch() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun String.normalizeForCardMatch(): String =
        replace(Regex("""\s+"""), "")
            .replace(Regex("""[^가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]"""), "")
            .lowercase()

    private fun String.similarityTo(other: String): Double {
        val maxLength = max(length, other.length)
        if (maxLength == 0) return 1.0

        val distance = levenshteinDistance(other)
        return 1.0 - distance.toDouble() / maxLength.toDouble()
    }

    private fun String.levenshteinDistance(other: String): Int {
        if (this == other) return 0
        if (isEmpty()) return other.length
        if (other.isEmpty()) return length

        var previous = IntArray(other.length + 1) { it }
        var current = IntArray(other.length + 1)

        for (i in indices) {
            current[0] = i + 1
            for (j in other.indices) {
                val substitutionCost = if (this[i] == other[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + substitutionCost,
                )
            }
            val temp = previous
            previous = current
            current = temp
        }

        return previous[other.length]
    }

    private fun SilentCard.displayName(): String = nameKo.ifBlank { id }

    private data class CardMatch(
        val card: SilentCard,
        val score: Double,
        val similarity: Double,
        val source: String,
    )

    private companion object {
        const val TAG = "CardNameMatcher"
        const val EXACT_MATCH_SCORE = 10.0
        const val SIMILARITY_THRESHOLD = 0.75
        const val SHORT_NAME_SIMILARITY_THRESHOLD = 0.95
        const val SHORT_NAME_MAX_LENGTH = 2
        const val WINDOW_LENGTH_TOLERANCE = 2
        const val NAME_LENGTH_SCORE_DIVISOR = 100.0
    }
}
