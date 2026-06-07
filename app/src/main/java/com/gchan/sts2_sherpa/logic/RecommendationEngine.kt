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
    val pickedAttackCount: Int,
    val pickedBlockCount: Int,
    val powerCount: Int,
    val drawCount: Int,
    val energyCount: Int,
    val footworkCount: Int,
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
        if (candidates.size != REWARD_CANDIDATE_COUNT) return null

        val deckAnalysis = analyzeDeck(currentDeck)
        val allCandidatesAreDTier = candidates.all { it.isTier("D") }
        val scores = candidates.map { candidate ->
            scoreCandidate(
                candidate = candidate,
                deckAnalysis = deckAnalysis,
                allCandidatesAreDTier = allCandidatesAreDTier,
            )
        }
        val sortedScores = scores.sortedByDescending { it.score }
        val recommendedScore = selectRecommendedScore(
            scores = scores,
            deckAnalysis = deckAnalysis,
        ) ?: return null

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
            pickedAttackCount = countPickedAttackCards(currentDeck),
            pickedBlockCount = countPickedBlockCards(currentDeck),
            powerCount = currentDeck.countTaggedCards("power"),
            drawCount = currentDeck.countTaggedCards("draw"),
            energyCount = currentDeck.countTaggedCards("energy"),
            footworkCount = currentDeck.countCardId("FOOTWORK"),
        )
    }

    private fun selectRecommendedScore(
        scores: List<CandidateScore>,
        deckAnalysis: DeckAnalysis,
    ): CandidateScore? {
        val highestScore = scores.reduceOrNull { best, current ->
            if (current.score > best.score) current else best
        } ?: return null

        if (
            deckAnalysis.pickedAttackCount >= 3 &&
            highestScore.card.isGeneralAttackAfterQuota(deckAnalysis)
        ) {
            val nonGeneralAttackScore = scores
                .filterNot { it.card.isGeneralAttackAfterQuota(deckAnalysis) }
                .reduceOrNull { best, current -> if (current.score > best.score) current else best }

            if (nonGeneralAttackScore != null) {
                return nonGeneralAttackScore
            }
        }

        return highestScore
    }

    private fun scoreCandidate(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        allCandidatesAreDTier: Boolean,
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

        fun addScore(label: String, value: Int, reason: String) {
            score += value
            reasons += reason
            breakdown += ScoreBreakdown(label = label, score = value, reason = reason)
        }

        applyEarlyGameRules(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            allCandidatesAreDTier = allCandidatesAreDTier,
            addScore = ::addScore,
        )
        applyExistingSynergyRules(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            addScore = ::addScore,
        )
        applyRatioRules(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            addScore = ::addScore,
        )
        applyFinisherRules(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            addScore = ::addScore,
        )
        applyFootworkRule(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            addScore = ::addScore,
        )
        applyPostAttackQuotaRules(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            addScore = ::addScore,
        )
        applyLateDeckRules(
            candidate = candidate,
            deckAnalysis = deckAnalysis,
            addScore = ::addScore,
        )

        return CandidateScore(
            card = candidate,
            score = score,
            reasons = reasons.distinct().take(6),
            scoreBreakdown = breakdown,
        )
    }

    private fun applyEarlyGameRules(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        allCandidatesAreDTier: Boolean,
        addScore: (String, Int, String) -> Unit,
    ) {
        val attackRole = isAttackRole(candidate)
        val blockRole = isBlockRole(candidate)
        val needsEarlyAttack = deckAnalysis.pickedAttackCount < 3
        val needsBasicBody = deckAnalysis.pickedAttackCount < 2 || deckAnalysis.pickedBlockCount < 2

        if (needsEarlyAttack && attackRole && !candidate.isTier("D")) {
            addScore(
                "초반 공격 카드 확보",
                35,
                if (isEarlyDamagePower(candidate)) {
                    "이 파워 카드는 지속 피해를 제공해 초반 공격 카드 역할을 대신할 수 있습니다."
                } else {
                    "초반에는 공격 카드 확보가 가장 중요해 공격 역할 카드를 우선합니다."
                },
            )
        }

        if (needsEarlyAttack && !attackRole) {
            addScore(
                "초반 공격 카드 부족 감점",
                -20,
                "첫 3장의 공격 카드가 채워지기 전에는 즉시 피해를 줄 수 없는 카드의 우선도가 낮습니다.",
            )
        }

        if (!allCandidatesAreDTier && candidate.isTier("D")) {
            addScore(
                "D티어 강한 감점",
                -40,
                "D티어 카드는 초반 보정 대상에서 제외하고, 다른 선택지가 있으면 추천하지 않습니다.",
            )
        }

        if (needsBasicBody) {
            val isAdrenaline = candidate.id == "ADRENALINE"
            if ("draw" in candidate.allTags) {
                addScore(
                    "초반 드로우 감점",
                    if (isAdrenaline) -5 else -20,
                    "초반에는 드로우를 해도 기본 타격/수비를 뽑을 확률이 높아 가치가 낮습니다.",
                )
            }
            if ("energy" in candidate.allTags) {
                addScore(
                    "초반 에너지 감점",
                    if (isAdrenaline) -5 else -20,
                    "공격과 방어 카드가 충분히 갖춰지기 전에는 에너지 보조보다 즉시 도움 되는 카드가 중요합니다.",
                )
            }
            if ("extra_action" in candidate.allTags && !attackRole && !blockRole) {
                addScore(
                    "초반 코스트 보조 감점",
                    -15,
                    "초반에는 코스트 보조보다 바로 피해를 주거나 막아주는 카드가 더 안정적입니다.",
                )
            }
            if (attackRole && candidate.damage != null && candidate.damage >= 8) {
                addScore(
                    "체급 좋은 공격 카드",
                    15,
                    "이 카드는 즉시 피해 수치가 좋아 초반 전투 안정성에 도움이 됩니다.",
                )
            }
            if (blockRole && candidate.block != null && candidate.block >= 7) {
                addScore(
                    "체급 좋은 방어 카드",
                    15,
                    "이 카드는 즉시 방어 수치가 좋아 초반 전투 안정성에 도움이 됩니다.",
                )
            }
        }
    }

    private fun applyExistingSynergyRules(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        addScore: (String, Int, String) -> Unit,
    ) {
        val tags = candidate.allTags.toSet()
        val tagCounts = deckAnalysis.tagCounts
        val synergyMultiplier = if (deckAnalysis.pickedAttackCount < 2 || deckAnalysis.pickedBlockCount < 2) {
            0.5
        } else {
            1.0
        }

        fun scaled(value: Int): Int = (value * synergyMultiplier).toInt()

        if (tagCounts.countOf("aoe") == 0 && "aoe" in tags) {
            addScore(
                "광역기 부족 보정",
                15,
                "현재 덱에 광역기가 부족해 여러 적을 상대할 카드 가치가 높습니다.",
            )
        }
        if (tagCounts.countOf("poison") >= 2 && "poison" in tags) {
            addScore(
                "중독 시너지 보정",
                scaled(15),
                "현재 덱에 중독 카드가 있어 중독 시너지를 키우기 좋습니다.",
            )
        }
        if (tagCounts.countOf("shiv") >= 2 && "shiv" in tags) {
            addScore(
                "단도 시너지 보정",
                scaled(15),
                "현재 덱에 단도 카드가 있어 단도 시너지를 강화할 수 있습니다.",
            )
        }
        if (deckAnalysis.deckSize >= 22 && "draw" in tags) {
            addScore(
                "드로우 보정",
                12,
                "덱이 커질수록 핵심 카드를 찾기 어려워 드로우 카드 가치가 올라갑니다.",
            )
        }
        if (deckAnalysis.deckSize >= 22 && "energy" in tags) {
            addScore(
                "에너지 보정",
                10,
                "덱이 커졌을 때 에너지 보조 카드는 여러 카드를 쓰기 쉽게 해줍니다.",
            )
        }
        if (deckAnalysis.deckSize >= 18 && "power" in tags) {
            addScore(
                "파워 보정",
                8,
                "파워 카드는 한 번 사용하면 전투 전체에 영향을 줘 장기전에서 좋습니다.",
            )
        }
        if (tagCounts.countOf("block") <= 5 && "block" in tags) {
            addScore(
                "방어 부족 보정",
                10,
                "현재 덱의 방어 카드가 적어 방어 보강 가치가 있습니다.",
            )
        }
        if (candidate.isTier("D") && deckAnalysis.deckSize >= 18) {
            addScore(
                "D티어 감점",
                -15,
                "덱이 어느 정도 커진 뒤에는 초보자에게 어려운 D티어 카드는 신중히 고르는 편이 좋습니다.",
            )
        }
        if (
            deckAnalysis.deckSize >= 25 &&
            "damage" in tags &&
            !tags.hasAny("draw", "energy", "power", "poison", "shiv", "aoe")
        ) {
            addScore(
                "큰 덱 단순 공격 감점",
                -10,
                "덱이 커진 뒤에는 단순 공격 카드보다 시너지나 순환 카드 가치가 높습니다.",
            )
        }
    }

    private fun applyRatioRules(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        addScore: (String, Int, String) -> Unit,
    ) {
        val tags = candidate.allTags.toSet()
        if (deckAnalysis.powerCount >= deckAnalysis.drawCount + 2) {
            if ("power" in tags) {
                addScore(
                    "파워/드로우/에너지 비율 보정",
                    -15,
                    "파워 카드가 많아져 첫 사이클이 약해질 수 있어 추가 파워의 가치를 낮게 봅니다.",
                )
            }
            if ("draw" in tags) {
                addScore(
                    "파워/드로우/에너지 비율 보정",
                    12,
                    "현재 덱은 파워에 비해 드로우가 부족해 순환 카드가 필요합니다.",
                )
            }
            if ("energy" in tags) {
                addScore(
                    "파워/드로우/에너지 비율 보정",
                    10,
                    "파워 카드가 많아질수록 드로우와 에너지 보조의 가치가 올라갑니다.",
                )
            }
        }
        if (deckAnalysis.drawCount >= deckAnalysis.powerCount + 3 && deckAnalysis.powerCount <= 1 && "power" in tags) {
            addScore(
                "파워/드로우/에너지 비율 보정",
                8,
                "드로우가 충분한 편이라 전투 전체 가치를 올리는 파워 카드도 활용하기 좋습니다.",
            )
        }
        if (deckAnalysis.energyCount == 0 && deckAnalysis.deckSize >= 22 && "energy" in tags) {
            addScore(
                "에너지 보조 부족 보정",
                10,
                "현재 덱에 에너지 보조가 없어 여러 카드를 쓰기 쉽게 해주는 카드 가치가 높습니다.",
            )
        }
        if (deckAnalysis.powerCount >= 3 && deckAnalysis.drawCount == 0) {
            if ("draw" in tags) {
                addScore(
                    "파워/드로우/에너지 비율 보정",
                    18,
                    "파워가 많은 덱인데 드로우가 없어 순환 카드가 특히 필요합니다.",
                )
            }
            if ("power" in tags) {
                addScore(
                    "파워 과다 감점",
                    -20,
                    "파워가 이미 많은데 드로우가 없어 추가 파워보다 순환 보강이 우선입니다.",
                )
            }
        }
    }

    private fun applyFinisherRules(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        addScore: (String, Int, String) -> Unit,
    ) {
        val synergyTag = getFinisherSynergyTag(candidate) ?: return
        if (deckAnalysis.deckSize < 20) {
            addScore(
                "피니셔 초반 감점",
                -25,
                "이 카드는 피니셔 성격이 강해 초반에는 조건을 맞추기 어려워 감점됩니다.",
            )
        } else if (deckAnalysis.tagCounts.countOf(synergyTag) >= 2) {
            addScore(
                "피니셔 후반 시너지 보정",
                25,
                "현재 덱에 관련 시너지가 충분해 피니셔 카드의 가치가 높아졌습니다.",
            )
        }
    }

    private fun applyFootworkRule(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        addScore: (String, Int, String) -> Unit,
    ) {
        if (candidate.id != "FOOTWORK") return

        if (deckAnalysis.footworkCount == 0) {
            addScore(
                "첫 발놀림 보정",
                100,
                "첫 발놀림은 사일런트의 방어 안정성을 크게 올려 거의 항상 좋은 선택입니다.",
            )
        }
    }

    private fun applyPostAttackQuotaRules(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        addScore: (String, Int, String) -> Unit,
    ) {
        if (deckAnalysis.pickedAttackCount < 3 || !isAttackRole(candidate)) return

        if (candidate.isTier("D")) {
            addScore(
                "D티어 공격 카드 제외",
                -100,
                "공격 카드가 이미 충분한 상태에서 D티어 공격 카드는 덱을 흐릴 가능성이 높습니다.",
            )
        }

        if (candidate.isGeneralAttackAfterQuota(deckAnalysis)) {
            addScore(
                "공격 카드 3장 확보 이후 일반 공격 감점",
                -60,
                "이미 공격 카드 3장이 확보되어 초보자 기준으로는 추가 일반 공격 카드의 가치가 낮습니다.",
            )
        }
    }

    private fun applyLateDeckRules(
        candidate: SilentCard,
        deckAnalysis: DeckAnalysis,
        addScore: (String, Int, String) -> Unit,
    ) {
        if (deckAnalysis.deckSize < 24 || isFinisher(candidate)) return

        val hasMajorSynergy = candidate.allTags.any { it in getMajorSynergyTags(deckAnalysis) }
        val isPremiumSynergyPick = candidate.beginnerTier.uppercase() in setOf("S", "A") && hasMajorSynergy
        if (isPremiumSynergyPick) return

        if (isAttackRole(candidate)) {
            addScore(
                "24장 이후 일반 공격 카드 감점",
                -25,
                "현재 덱이 24장 이상으로 커져 단순 공격 카드보다 파워, 드로우, 에너지, 피니셔 카드 가치가 높습니다.",
            )
        }
        if (isBlockRole(candidate)) {
            addScore(
                "24장 이후 일반 방어 카드 감점",
                -20,
                "덱이 커진 뒤에는 일반 방어 카드보다 핵심 카드를 빨리 찾거나 시너지를 완성하는 카드가 중요합니다.",
            )
        }
    }

    private fun shouldRecommendSkip(
        deckAnalysis: DeckAnalysis,
        candidateScores: List<CandidateScore>,
    ): List<String> {
        val deckSize = deckAnalysis.deckSize
        val candidates = candidateScores.map { it.card }
        val highestScore = candidateScores.maxOfOrNull { it.score } ?: return emptyList()
        val allLowTier = candidates.all { it.beginnerTier.uppercase() in setOf("C", "D") }
        val allDTier = candidates.all { it.isTier("D") }
        val hasMajorSynergyCandidate = candidates.any { it.hasMajorSynergy(deckAnalysis) }
        val hasMidDeckKeepTag = candidates.any {
            it.allTags.toSet().hasAny("aoe", "draw", "energy", "power", "poison", "shiv")
        }
        val hasLargeDeckKeepCard = candidates.any { it.isLateDeckKeepCard(deckAnalysis) }
        val generalAttackCandidates = candidates.filter { it.isGeneralAttackAfterQuota(deckAnalysis) }
        val hasPremiumMajorSynergyCandidate = candidates.any {
            it.beginnerTier.uppercase() in setOf("S", "A") && it.hasMajorSynergy(deckAnalysis)
        }
        val allDTierAttacks = candidates.all { it.isTier("D") && isAttackRole(it) }

        if (deckAnalysis.pickedAttackCount >= 3) {
            if (allDTierAttacks) {
                return listOf(
                    "공격 카드가 이미 3장 확보되어 일반 공격 카드를 더 넣을 필요가 낮습니다.",
                    "후보가 모두 D티어 공격 카드라 카드 추천보다 스킵이 안전합니다.",
                )
            }
            if (generalAttackCandidates.size == candidates.size && !hasPremiumMajorSynergyCandidate) {
                return listOf(
                    "공격 카드가 이미 3장 확보되어 이번 보상의 일반 공격 카드는 넘기는 편이 좋습니다.",
                    "초보자 기준으로 공격 카드를 더 넣기보다 덱 순환과 방어 안정성을 챙기는 편이 좋습니다.",
                )
            }
            if (
                generalAttackCandidates.size >= 2 &&
                !hasPremiumMajorSynergyCandidate &&
                (deckSize >= 24 || highestScore < 40)
            ) {
                return listOf(
                    "공격 카드가 이미 3장 확보되어 일반 공격 카드를 더 넣을 필요가 낮습니다.",
                    "이번 후보들은 덱의 다음 목표인 방어, 드로우, 파워, 에너지 보강에 크게 도움이 되지 않습니다.",
                )
            }
        }

        if (deckSize <= 18) return emptyList()

        if (allDTier && deckSize >= 18 && !hasMajorSynergyCandidate) {
            return listOf(
                "세 후보 모두 초보자 기준 D티어라 이번 보상은 넘기는 편이 안전합니다.",
                "후보 카드들이 현재 덱의 주요 시너지와 잘 맞지 않습니다.",
            )
        }

        if (deckSize in 19..23 && !hasMidDeckKeepTag) {
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

        if (deckSize >= 24) {
            if (highestScore < 40 && !hasLargeDeckKeepCard) {
                return listOf(
                    "현재 덱이 24장 이상으로 커져 불필요한 카드를 추가하면 핵심 카드가 늦게 나옵니다.",
                    "후반에는 단순 공격/방어 카드보다 파워, 드로우, 에너지, 피니셔 카드의 가치가 높습니다.",
                    "후보 카드들의 최고 점수가 낮아 이번 보상은 넘기는 편이 좋습니다.",
                )
            }
            if (allLowTier && !hasLargeDeckKeepCard) {
                return listOf(
                    "현재 덱이 24장 이상으로 커져 불필요한 카드를 추가하면 핵심 카드가 늦게 나옵니다.",
                    "후보 카드들이 모두 낮은 티어이고 후반 핵심 역할과 잘 맞지 않습니다.",
                )
            }
        }

        return emptyList()
    }

    private fun buildReadableReasons(
        card: SilentCard,
        scoreReasons: List<String>,
    ): List<String> {
        val tagReasons = card.allTags.toSet().cardTraitReasons()
        return (listOf(tierReason(card.beginnerTier)) + scoreReasons + tagReasons)
            .distinct()
            .take(4)
    }

    private fun isAttackRole(card: SilentCard): Boolean =
        card.type == "Attack" ||
            "damage" in card.allTags ||
            card.id in EXTRA_ATTACK_ROLE_CARD_IDS ||
            isEarlyDamagePower(card)

    private fun isBlockRole(card: SilentCard): Boolean =
        "block" in card.allTags ||
            (card.typeKo == "스킬" && card.block != null && card.block > 0)

    private fun isEarlyDamagePower(card: SilentCard): Boolean =
        card.id in EARLY_DAMAGE_POWER_CARD_IDS

    private fun isFinisher(card: SilentCard): Boolean =
        getFinisherSynergyTag(card) != null

    private fun getFinisherSynergyTag(card: SilentCard): String? =
        FINISHER_SYNERGY_TAGS[card.id]

    private fun countPickedAttackCards(deck: List<DeckCard>): Int =
        deck.sumOf { deckCard ->
            if (deckCard.card.id in STARTING_ATTACK_CARD_IDS) 0
            else if (isAttackRole(deckCard.card)) deckCard.count
            else 0
        }

    private fun countPickedBlockCards(deck: List<DeckCard>): Int =
        deck.sumOf { deckCard ->
            if (deckCard.card.id in STARTING_BLOCK_CARD_IDS) 0
            else if (isBlockRole(deckCard.card)) deckCard.count
            else 0
        }

    private fun getMajorSynergyTags(deckAnalysis: DeckAnalysis): Set<String> =
        deckAnalysis.majorSynergyTags

    private fun SilentCard.isLateDeckKeepCard(deckAnalysis: DeckAnalysis): Boolean {
        val tags = allTags.toSet()
        return tags.hasAny("power", "draw", "energy") ||
            isFinisher(this) ||
            hasMajorSynergy(deckAnalysis) ||
            (beginnerTier.uppercase() in setOf("S", "A") && hasMajorSynergy(deckAnalysis))
    }

    private fun SilentCard.isGeneralAttackAfterQuota(deckAnalysis: DeckAnalysis): Boolean {
        val tags = allTags.toSet()
        if (!isAttackRole(this)) return false
        if (isFinisher(this) || isEarlyDamagePower(this)) return false
        if (tags.hasAny("power", "draw", "energy")) return false
        if ("poison" in tags && deckAnalysis.tagCounts.countOf("poison") >= 2) return false
        if ("shiv" in tags && deckAnalysis.tagCounts.countOf("shiv") >= 2) return false
        if ("sly" in tags && deckAnalysis.tagCounts.countOf("sly") >= 2) return false
        return true
    }

    private fun SilentCard.hasMajorSynergy(deckAnalysis: DeckAnalysis): Boolean =
        allTags.any { it in getMajorSynergyTags(deckAnalysis) }

    private fun List<DeckCard>.countTaggedCards(tag: String): Int =
        sumOf { deckCard -> if (tag in deckCard.card.allTags) deckCard.count else 0 }

    private fun List<DeckCard>.countCardId(cardId: String): Int =
        sumOf { deckCard -> if (deckCard.card.id == cardId) deckCard.count else 0 }

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

    private fun SilentCard.isTier(tier: String): Boolean =
        beginnerTier.equals(tier, ignoreCase = true)

    private fun Map<String, Int>.countOf(tag: String): Int = getOrDefault(tag, 0)

    private fun Set<String>.hasAny(vararg tags: String): Boolean = tags.any { it in this }
}

private const val REWARD_CANDIDATE_COUNT = 3

private val STARTING_ATTACK_CARD_IDS = setOf(
    "STRIKE_SILENT",
    "NEUTRALIZE",
)

private val STARTING_BLOCK_CARD_IDS = setOf(
    "DEFEND_SILENT",
    "SURVIVOR",
)

private val EXTRA_ATTACK_ROLE_CARD_IDS = setOf(
    "DEADLY_POISON",
    "SNAKEBITE",
    "BLADE_DANCE",
    "BOUNCING_FLASK",
    "UP_MY_SLEEVE",
    "HAZE",
    "BLADE_OF_INK",
)

private val EARLY_DAMAGE_POWER_CARD_IDS = setOf(
    "NOXIOUS_FUMES",
    "INFINITE_BLADES",
)

private val FINISHER_SYNERGY_TAGS = mapOf(
    "MURDER" to "sly",
    "MEMENTO_MORI" to "sly",
    "KNIFE_TRAP" to "shiv",
    "FINISHER" to "shiv",
    "CORROSIVE_WAVE" to "sly",
)

private val MAJOR_SYNERGY_TAGS = setOf(
    "poison",
    "shiv",
    "sly",
    "weak",
    "dexterity",
    "retain",
)
