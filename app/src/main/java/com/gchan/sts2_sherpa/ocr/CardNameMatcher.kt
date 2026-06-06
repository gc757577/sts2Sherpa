package com.gchan.sts2_sherpa.ocr

import com.gchan.sts2_sherpa.data.SilentCard

class CardNameMatcher {
    fun findMatchingCards(
        ocrText: String,
        cards: List<SilentCard>,
        maxResults: Int = 3,
    ): List<SilentCard> {
        val normalizedText = ocrText.normalizeForCardMatch()
        if (normalizedText.isBlank()) return emptyList()

        val exactMatches = cards.filter { card ->
            val normalizedName = card.matchName()
            normalizedName.isNotBlank() && normalizedText.contains(normalizedName)
        }

        val partialMatches = if (exactMatches.size < maxResults) {
            cards.filterNot { exactMatches.any { match -> match.id == it.id } }
                .filter { card ->
                    val normalizedName = card.matchName()
                    normalizedName.length >= PARTIAL_MATCH_MIN_LENGTH &&
                        normalizedName.windowed(
                            size = PARTIAL_MATCH_MIN_LENGTH,
                            step = 1,
                            partialWindows = false,
                        ).any { chunk -> normalizedText.contains(chunk) }
                }
        } else {
            emptyList()
        }

        return (exactMatches + partialMatches)
            .distinctBy { it.id }
            .take(maxResults)
    }

    private fun SilentCard.matchName(): String {
        val preferredName = normalizedName.ifBlank { nameKo }
        return preferredName.normalizeForCardMatch()
    }

    private fun String.normalizeForCardMatch(): String =
        filterNot { it.isWhitespace() }
            .replace(Regex("""[^\p{L}\p{N}]"""), "")
            .lowercase()

    private companion object {
        const val PARTIAL_MATCH_MIN_LENGTH = 2
    }
}
