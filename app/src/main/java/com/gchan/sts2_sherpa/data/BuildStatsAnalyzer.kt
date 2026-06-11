package com.gchan.sts2_sherpa.data

data class CardUsageStat(
    val card: SilentCard,
    val countBuilds: Int,
    val usageRate: Float,
    val rank: Int,
)

data class CardSynergyStat(
    val card: SilentCard,
    val coBuildCount: Int,
    val synergyRate: Float,
    val rank: Int,
)

object BuildStatsAnalyzer {
    private val startingCardIds = setOf(
        "STRIKE_SILENT",
        "DEFEND_SILENT",
        "NEUTRALIZE",
        "SURVIVOR",
    )

    fun calculateCardUsageStats(
        builds: List<SavedBuild>,
        allCards: List<SilentCard>,
    ): List<CardUsageStat> {
        if (builds.isEmpty()) return emptyList()

        val cardsById = allCards.associateBy { it.id }
        val countsByCardId = mutableMapOf<String, Int>()

        builds.forEach { build ->
            build.nonStartingCardIds().forEach { cardId ->
                countsByCardId[cardId] = countsByCardId.getOrDefault(cardId, 0) + 1
            }
        }

        return countsByCardId.entries
            .mapNotNull { (cardId, count) ->
                cardsById[cardId]?.let { card ->
                    CardUsageStat(
                        card = card,
                        countBuilds = count,
                        usageRate = count.toFloat() / builds.size.toFloat(),
                        rank = 0,
                    )
                }
            }
            .sortedWith(
                compareByDescending<CardUsageStat> { it.countBuilds }
                    .thenByDescending { it.usageRate }
                    .thenBy { it.card.statsDisplayName() },
            )
            .mapIndexed { index, stat -> stat.copy(rank = index + 1) }
    }

    fun calculateSynergyStatsForCard(
        targetCardId: String,
        builds: List<SavedBuild>,
        allCards: List<SilentCard>,
    ): List<CardSynergyStat> {
        if (targetCardId in startingCardIds) return emptyList()

        val cardsById = allCards.associateBy { it.id }
        val buildsWithTarget = builds
            .map { build -> build.nonStartingCardIds() }
            .filter { cardIds -> targetCardId in cardIds }

        if (buildsWithTarget.isEmpty()) return emptyList()

        val countsByCardId = mutableMapOf<String, Int>()
        buildsWithTarget.forEach { cardIds ->
            cardIds
                .filterNot { it == targetCardId }
                .forEach { cardId ->
                    countsByCardId[cardId] = countsByCardId.getOrDefault(cardId, 0) + 1
                }
        }

        return countsByCardId.entries
            .mapNotNull { (cardId, count) ->
                cardsById[cardId]?.let { card ->
                    CardSynergyStat(
                        card = card,
                        coBuildCount = count,
                        synergyRate = count.toFloat() / buildsWithTarget.size.toFloat(),
                        rank = 0,
                    )
                }
            }
            .sortedWith(
                compareByDescending<CardSynergyStat> { it.coBuildCount }
                    .thenByDescending { it.synergyRate }
                    .thenBy { it.card.statsDisplayName() },
            )
            .mapIndexed { index, stat -> stat.copy(rank = index + 1) }
    }

    private fun SavedBuild.nonStartingCardIds(): Set<String> =
        deck.asSequence()
            .filter { deckCard -> deckCard.count > 0 }
            .map { deckCard -> deckCard.card.id }
            .filterNot { cardId -> cardId in startingCardIds }
            .toSet()

    private fun SilentCard.statsDisplayName(): String =
        nameKo.ifBlank { normalizedName.ifBlank { id } }
}
