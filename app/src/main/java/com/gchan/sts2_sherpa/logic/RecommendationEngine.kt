package com.gchan.sts2_sherpa.logic

import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SilentCard

enum class RecommendationAction {
    PICK_CARD,
    SKIP,
}

data class RecommendationResult(
    val action: RecommendationAction,
    val recommendedCard: SilentCard?,
    val recommendedScore: CandidateScore?,
    val candidateScores: List<CandidateScore>,
    val deckAnalysis: DeckAnalysis,
    val reasons: List<String>,
)

data class CandidateScore(
    val card: SilentCard,
    val score: Int,
    val reasons: List<String>,
    val scoreBreakdown: List<ScoreBreakdown>,
)

data class ScoreBreakdown(
    val label: String,
    val score: Int,
    val reason: String,
)

data class DeckAnalysis(
    val deckSize: Int,
    val tagCounts: Map<String, Int>,
) {
    val majorSynergyTags: Set<String>
        get() = MAJOR_SYNERGY_TAGS.filter { tagCounts.getOrDefault(it, 0) >= 2 }.toSet()

    val topTags: List<Pair<String, Int>>
        get() = tagCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(3)
            .map { it.key to it.value }

    val missingRoles: List<String>
        get() = buildList {
            if (tagCounts.getOrDefault("aoe", 0) == 0) add("광역기 부족")
            if (tagCounts.getOrDefault("draw", 0) <= 1 && deckSize >= 18) add("드로우 부족")
            if (tagCounts.getOrDefault("block", 0) <= 5) add("방어 부족")
            if (tagCounts.getOrDefault("energy", 0) == 0 && deckSize >= 22) add("에너지 보조 부족")
            if (tagCounts.getOrDefault("power", 0) == 0 && deckSize >= 18) add("파워 카드 부족")
        }.ifEmpty { listOf("큰 부족 요소 없음") }
}

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
        val sortedScores = scores.sortedByDescending { it.score }
        val recommendedScore = scores.reduceOrNull { best, current ->
            if (current.score > best.score) current else best
        } ?: return null

        val skipReasons = shouldRecommendSkip(
            deckAnalysis = deckAnalysis,
            candidateScores = scores,
        )

        return if (skipReasons.isNotEmpty()) {
            RecommendationResult(
                action = RecommendationAction.SKIP,
                recommendedCard = null,
                recommendedScore = null,
                candidateScores = sortedScores,
                deckAnalysis = deckAnalysis,
                reasons = skipReasons,
            )
        } else {
            RecommendationResult(
                action = RecommendationAction.PICK_CARD,
                recommendedCard = recommendedScore.card,
                recommendedScore = recommendedScore,
                candidateScores = sortedScores,
                deckAnalysis = deckAnalysis,
                reasons = buildReadableReasons(recommendedScore.card, recommendedScore.reasons),
            )
        }
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

    private fun shouldRecommendSkip(
        deckAnalysis: DeckAnalysis,
        candidateScores: List<CandidateScore>,
    ): List<String> {
        val deckSize = deckAnalysis.deckSize
        if (deckSize <= 18) return emptyList()

        val candidates = candidateScores.map { it.card }
        val highestScore = candidateScores.maxOf { it.score }
        val allLowTier = candidates.all { it.beginnerTier.uppercase() in setOf("C", "D") }
        val allDTier = candidates.all { it.beginnerTier.equals("D", ignoreCase = true) }
        val hasMidDeckKeepTag = candidates.any {
            it.allTags.toSet().hasAny("aoe", "draw", "energy", "power", "poison", "shiv")
        }
        val hasLargeDeckKeepTag = candidates.any {
            it.allTags.toSet().hasAny("draw", "energy", "power")
        }
        val hasMajorSynergyCandidate = candidates.any { candidate ->
            candidate.allTags.any { it in deckAnalysis.majorSynergyTags }
        }

        if (allDTier && deckSize >= 18 && !hasMajorSynergyCandidate) {
            return listOf(
                "세 후보 모두 초보자 기준 D티어라 이번 보상은 넘기는 편이 안전합니다.",
                "후보 카드들이 현재 덱의 주요 시너지와 잘 맞지 않습니다.",
            )
        }

        if (deckSize in 19..24 && !hasMidDeckKeepTag) {
            if (highestScore < 25) {
                return listOf(
                    "후보 카드들의 최고 점수가 낮아 덱에 넣을 가치가 크지 않습니다.",
                    "덱이 어느 정도 갖춰진 뒤에는 애매한 카드를 줄이는 편이 안정적입니다.",
                )
            }
            if (allLowTier) {
                return listOf(
                    "세 후보 모두 초보자 기준 낮은 티어라 이번 보상은 넘기는 편이 안전합니다.",
                    "후보 카드들이 현재 덱의 주요 시너지와 잘 맞지 않습니다.",
                )
            }
        }

        if (deckSize >= 25 && !hasLargeDeckKeepTag) {
            if (highestScore < 35) {
                return listOf(
                    "현재 덱이 25장 이상으로 커졌습니다.",
                    "애매한 카드를 추가하면 핵심 카드를 뽑기 어려워집니다.",
                    "최고 점수가 낮아 덱에 넣을 가치가 낮습니다.",
                )
            }
            if (allLowTier && !hasMajorSynergyCandidate) {
                return listOf(
                    "현재 덱이 25장 이상으로 커졌습니다.",
                    "후보 카드들이 현재 주요 시너지와 맞지 않습니다.",
                    "세 후보 모두 초보자 기준 낮은 티어라 이번 보상은 넘기는 편이 안전합니다.",
                )
            }
        }

        return emptyList()
    }

    private fun scoreCandidate(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
    ): CandidateScore {
        var score = tierScore(candidate.beginnerTier)
        val reasons = mutableListOf(tierReason(candidate.beginnerTier))
        val breakdown = mutableListOf(
            ScoreBreakdown(
                label = "기본 티어 점수",
                score = score,
                reason = tierReason(candidate.beginnerTier),
            ),
        )
        val tags = candidate.allTags.toSet()
        val deckSize = deckAnalysis.deckSize
        val tagCounts = deckAnalysis.tagCounts

        fun addScore(label: String, value: Int, reason: String) {
            score += value
            reasons += reason
            breakdown += ScoreBreakdown(label = label, score = value, reason = reason)
        }

        if (deckSize <= 18 && tags.hasAny("attack", "damage")) {
            addScore(
                label = "초반 공격 보정",
                value = 10,
                reason = "초반에는 적을 빠르게 처치할 공격 카드가 중요합니다.",
            )
        }

        if (tagCounts.countOf("aoe") == 0 && "aoe" in tags) {
            addScore(
                label = "광역기 부족 보정",
                value = 15,
                reason = "현재 덱에 광역기가 부족해 여러 적을 상대할 카드 가치가 높습니다.",
            )
        }

        if (tagCounts.countOf("poison") >= 2 && "poison" in tags) {
            addScore(
                label = "중독 시너지 보정",
                value = 15,
                reason = "현재 덱에 중독 카드가 있어 중독 시너지를 키우기 좋습니다.",
            )
        }

        if (tagCounts.countOf("shiv") >= 2 && "shiv" in tags) {
            addScore(
                label = "단도 시너지 보정",
                value = 15,
                reason = "현재 덱에 단도 카드가 있어 단도 시너지를 강화할 수 있습니다.",
            )
        }

        if (deckSize >= 22 && "draw" in tags) {
            addScore(
                label = "드로우 보정",
                value = 12,
                reason = "덱이 커질수록 핵심 카드를 찾기 어려워 드로우 카드 가치가 올라갑니다.",
            )
        }

        if (deckSize >= 22 && "energy" in tags) {
            addScore(
                label = "에너지 보정",
                value = 10,
                reason = "덱이 커졌을 때 에너지 보조 카드는 여러 카드를 쓰기 쉽게 해줍니다.",
            )
        }

        if (deckSize >= 18 && "power" in tags) {
            addScore(
                label = "파워 보정",
                value = 8,
                reason = "파워 카드는 한 번 사용하면 전투 전체에 영향을 줘 장기전에서 좋습니다.",
            )
        }

        if (tagCounts.countOf("block") <= 5 && "block" in tags) {
            addScore(
                label = "방어 부족 보정",
                value = 10,
                reason = "현재 덱의 방어 카드가 적어 방어 보강 가치가 있습니다.",
            )
        }

        if (candidate.beginnerTier.equals("D", ignoreCase = true) && deckSize >= 18) {
            addScore(
                label = "D티어 감점",
                value = -15,
                reason = "덱이 어느 정도 커진 뒤에는 초보자에게 어려운 D티어 카드는 신중히 고르는 편이 좋습니다.",
            )
        }

        if (
            deckSize >= 25 &&
            "damage" in tags &&
            !tags.hasAny("draw", "energy", "power", "poison", "shiv", "aoe")
        ) {
            addScore(
                label = "큰 덱 단순 공격 감점",
                value = -10,
                reason = "덱이 커진 뒤에는 단순 공격 카드보다 시너지나 순환 카드 가치가 높습니다.",
            )
        }

        return CandidateScore(
            card = candidate,
            score = score,
            reasons = reasons.take(4),
            scoreBreakdown = breakdown,
        )
    }

    private fun buildReadableReasons(
        card: SilentCard,
        scoreReasons: List<String>,
    ): List<String> {
        val tagReasons = card.allTags.toSet().cardTraitReasons()
        return (listOf(tierReason(card.beginnerTier)) + tagReasons + scoreReasons)
            .distinct()
            .take(4)
    }

    private fun Set<String>.cardTraitReasons(): List<String> = buildList {
        if ("poison" in this@cardTraitReasons) add("중독 시너지를 강화합니다.")
        if ("shiv" in this@cardTraitReasons) add("단도 방향 덱과 잘 맞습니다.")
        if ("draw" in this@cardTraitReasons) add("핵심 카드를 더 빨리 찾는 데 도움을 줍니다.")
        if ("block" in this@cardTraitReasons) add("방어 안정성을 올려줍니다.")
        if ("aoe" in this@cardTraitReasons) add("여러 적을 상대하는 전투에 도움을 줍니다.")
        if ("power" in this@cardTraitReasons) add("전투가 길어질수록 가치가 커질 수 있습니다.")
        if ("energy" in this@cardTraitReasons) add("한 턴에 더 많은 카드를 사용할 수 있게 도와줍니다.")
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
            "S" -> "초보자 기준 매우 강한 카드입니다."
            "A" -> "초보자에게 안정적인 선택입니다."
            "B" -> "무난하게 사용할 수 있는 카드입니다."
            "C" -> "상황을 타는 카드라 현재 덱과의 궁합이 중요합니다."
            "D" -> "초보자에게는 다루기 어려운 카드입니다."
            else -> "티어 정보가 부족해 기본 점수로 평가했습니다."
        }

    private fun Map<String, Int>.countOf(tag: String): Int = getOrDefault(tag, 0)

    private fun Set<String>.hasAny(vararg tags: String): Boolean = tags.any { it in this }
}

private val MAJOR_SYNERGY_TAGS = setOf(
    "poison",
    "shiv",
    "sly",
    "draw",
    "block",
    "weak",
    "dexterity",
)
