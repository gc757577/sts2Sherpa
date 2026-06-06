package com.gchan.sts2_sherpa.logic

import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SilentCard

data class RecommendationResult(
    val recommendedCard: SilentCard,
    val recommendedScore: CandidateScore,
    val candidateScores: List<CandidateScore>,
    val deckAnalysis: DeckAnalysis,
)

data class CandidateScore(
    val card: SilentCard,
    val score: Int,
    val reasons: List<String>,
)

data class DeckAnalysis(
    val deckSize: Int,
    val tagCounts: Map<String, Int>,
)

object RecommendationEngine {
    fun recommend(
        currentDeck: List<DeckCard>,
        candidates: List<SilentCard>,
    ): RecommendationResult? {
        if (candidates.size != 3) return null

        val deckAnalysis = analyzeDeck(currentDeck)
        val scores = candidates.map { candidate ->
            scoreCandidate(candidate, deckAnalysis)
        }
        val recommendedScore = scores.maxWithOrNull(
            compareBy<CandidateScore> { it.score }
                .thenBy { tierScore(it.card.beginnerTier) }
                .thenBy { it.card.nameKo.ifBlank { it.card.id } },
        ) ?: return null

        return RecommendationResult(
            recommendedCard = recommendedScore.card,
            recommendedScore = recommendedScore,
            candidateScores = scores.sortedByDescending { it.score },
            deckAnalysis = deckAnalysis,
        )
    }

    fun analyzeDeck(currentDeck: List<DeckCard>): DeckAnalysis {
        val tagCounts = mutableMapOf<String, Int>()
        currentDeck.forEach { deckCard ->
            deckCard.card.allTags.forEach { tag ->
                tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + deckCard.count
            }
        }

        return DeckAnalysis(
            deckSize = currentDeck.sumOf { it.count },
            tagCounts = tagCounts,
        )
    }

    private fun scoreCandidate(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
    ): CandidateScore {
        var score = tierScore(candidate.beginnerTier)
        val reasons = mutableListOf(tierReason(candidate.beginnerTier))
        val tags = candidate.allTags.toSet()
        val deckSize = deckAnalysis.deckSize
        val tagCounts = deckAnalysis.tagCounts

        if (deckSize <= 18 && tags.hasAny("attack", "damage")) {
            score += 10
            reasons += "초반에는 적을 빠르게 처치할 공격 카드가 중요합니다."
        }

        if (tagCounts.countOf("aoe") == 0 && "aoe" in tags) {
            score += 15
            reasons += "현재 덱에 광역기가 부족해 여러 적을 상대할 카드 가치가 높습니다."
        }

        if (tagCounts.countOf("poison") >= 2 && "poison" in tags) {
            score += 15
            reasons += "현재 덱에 중독 카드가 있어 중독 시너지를 키우기 좋습니다."
        }

        if (tagCounts.countOf("shiv") >= 2 && "shiv" in tags) {
            score += 15
            reasons += "현재 덱에 단도 카드가 있어 단도 시너지를 강화할 수 있습니다."
        }

        if (deckSize >= 22 && "draw" in tags) {
            score += 12
            reasons += "덱이 커질수록 핵심 카드를 찾기 어려워 드로우 카드 가치가 올라갑니다."
        }

        if (deckSize >= 22 && "energy" in tags) {
            score += 10
            reasons += "덱이 커졌을 때 에너지 보조 카드는 여러 카드를 쓰기 쉽게 해줍니다."
        }

        if (deckSize >= 18 && "power" in tags) {
            score += 8
            reasons += "파워 카드는 한 번 사용하면 전투 전체에 영향을 줘 장기전에서 좋습니다."
        }

        if (tagCounts.countOf("block") <= 5 && "block" in tags) {
            score += 10
            reasons += "현재 덱의 방어 카드가 적어 방어 보강 가치가 있습니다."
        }

        if (candidate.beginnerTier.equals("D", ignoreCase = true) && deckSize >= 18) {
            score -= 15
            reasons += "덱이 어느 정도 커진 뒤에는 초보자에게 어려운 D티어 카드는 신중히 고르는 편이 좋습니다."
        }

        if (
            deckSize >= 25 &&
            "damage" in tags &&
            !tags.hasAny("draw", "energy", "power", "poison", "shiv", "aoe")
        ) {
            score -= 10
            reasons += "덱이 커진 뒤에는 단순 공격 카드보다 시너지나 순환 카드 가치가 높습니다."
        }

        return CandidateScore(
            card = candidate,
            score = score,
            reasons = reasons.take(4),
        )
    }

    private fun tierScore(tier: String): Int =
        when (tier.uppercase()) {
            "S" -> 50
            "A" -> 40
            "B" -> 30
            "C" -> 15
            "D" -> 0
            else -> 10
        }

    private fun tierReason(tier: String): String =
        when (tier.uppercase()) {
            "S" -> "티어 S 카드라 초보자에게 매우 강한 선택입니다."
            "A" -> "티어 A 카드라 초보자에게 안정적인 선택입니다."
            "B" -> "티어 B 카드라 무난하게 고를 수 있는 선택입니다."
            "C" -> "티어 C 카드라 현재 덱 상황에 따라 선택 가치가 달라집니다."
            "D" -> "티어 D 카드라 초보자에게는 다루기 어려울 수 있습니다."
            else -> "티어 정보가 부족해 기본 점수로 평가했습니다."
        }

    private fun Map<String, Int>.countOf(tag: String): Int = getOrDefault(tag, 0)

    private fun Set<String>.hasAny(vararg tags: String): Boolean = tags.any { it in this }
}
