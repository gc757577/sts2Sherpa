package com.gchan.sts2_sherpa.logic

import com.gchan.sts2_sherpa.data.DeckCard
import kotlin.math.min

object BuildAnalyzer {
    fun directionLabel(deck: List<DeckCard>): String {
        val analysis = RecommendationEngine.analyzeDeck(deck)
        return listOf(
            "poison" to "중독 방향",
            "shiv" to "단도 방향",
            "sly" to "교활 방향",
        ).maxByOrNull { (tag, _) -> analysis.tagCounts.getOrDefault(tag, 0) }
            ?.takeIf { (tag, _) -> analysis.tagCounts.getOrDefault(tag, 0) >= 2 }
            ?.second ?: "방향 미정"
    }

    fun completionScore(deck: List<DeckCard>): Int {
        val analysis = RecommendationEngine.analyzeDeck(deck)
        var score = 30
        score += min(analysis.deckSize, 28)
        score += min(analysis.pickedAttackCount * 4, 16)
        score += min(analysis.pickedBlockCount * 4, 16)
        score += min(analysis.drawCount * 5, 15)
        score += min(analysis.energyCount * 5, 10)
        score += min(analysis.powerCount * 3, 9)
        if (analysis.majorSynergyTags.isNotEmpty()) score += 10
        if (analysis.missingRoles.size <= 1 && analysis.missingRoles.firstOrNull() == "큰 부족 요소 없음") score += 8
        return score.coerceIn(0, 100)
    }

    fun labCompletionScore(deck: List<DeckCard>): Int {
        val addedDeck = addedCardsOnly(deck)
        if (addedDeck.isEmpty()) return 0

        val analysis = RecommendationEngine.analyzeDeck(addedDeck)
        var score = 0
        score += min(analysis.deckSize * 6, 30)
        score += min(analysis.pickedAttackCount * 7, 18)
        score += min(analysis.pickedBlockCount * 7, 18)
        score += min(analysis.drawCount * 8, 16)
        score += min(analysis.energyCount * 8, 12)
        score += min(analysis.powerCount * 5, 15)
        if (analysis.majorSynergyTags.isNotEmpty()) score += 12
        return score.coerceIn(0, 100)
    }

    fun labDirectionLabel(deck: List<DeckCard>): String =
        directionLabel(addedCardsOnly(deck))

    fun addedCardsOnly(deck: List<DeckCard>): List<DeckCard> =
        deck.mapNotNull { deckCard ->
            val baselineCount = STARTING_DECK_COUNTS.getOrDefault(deckCard.card.id, 0)
            val addedCount = deckCard.count - baselineCount
            if (addedCount > 0) deckCard.copy(count = addedCount) else null
        }

    fun defaultPlayBuildName(directionLabel: String): String =
        when {
            directionLabel.contains("중독") -> "중독 플레이 덱"
            directionLabel.contains("단도") -> "단도 플레이 덱"
            directionLabel.contains("교활") -> "교활 플레이 덱"
            else -> "플레이 덱"
        }
}

private val STARTING_DECK_COUNTS = mapOf(
    "STRIKE_SILENT" to 5,
    "DEFEND_SILENT" to 5,
    "NEUTRALIZE" to 1,
    "SURVIVOR" to 1,
)
