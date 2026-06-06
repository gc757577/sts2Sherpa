package com.gchan.sts2_sherpa.data

data class SilentCard(
    val id: String,
    val nameKo: String,
    val normalizedName: String,
    val cost: Int?,
    val type: String,
    val typeKo: String,
    val rarity: String,
    val rarityKo: String,
    val description: String,
    val image: String,
    val damage: Int?,
    val block: Int?,
    val hitCount: Int?,
    val cardsDraw: Int?,
    val energyGain: Int?,
    val target: String,
    val keywords: List<String>,
    val spawnsCards: List<String>,
    val autoTags: List<String>,
    val recommendTags: List<String>,
    val allTags: List<String>,
    val beginnerTier: String,
    val upgradeDependency: String,
    val extraAction: String,
    val scaling: CardScaling,
    val synergyKo: List<String>,
    val tagsKo: List<String>,
    val coreCardsKo: List<String>,
)

data class CardScaling(
    val act1: String,
    val act2: String,
    val act3: String,
)
