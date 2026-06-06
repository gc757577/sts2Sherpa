package com.gchan.sts2_sherpa.data

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONObject

class CardRepository(
    private val assets: AssetManager,
) {
    fun loadSilentCards(): List<SilentCard> {
        val json = assets.open(SILENT_CARDS_PATH)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        return runCatching { parseJsonArray(json) }
            .getOrElse { parseDisplayFieldsFallback(json) }
    }

    private fun parseJsonArray(json: String): List<SilentCard> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            array.getJSONObject(index).toSilentCard()
        }
    }

    private fun JSONObject.toSilentCard(): SilentCard {
        val scalingJson = optJSONObject("scaling")
        return SilentCard(
            id = optString("id"),
            nameKo = optString("nameKo"),
            normalizedName = optString("normalizedName"),
            cost = optNullableInt("cost"),
            type = optString("type"),
            typeKo = optString("typeKo"),
            rarity = optString("rarity"),
            rarityKo = optString("rarityKo"),
            description = optString("description"),
            image = optString("image"),
            damage = optNullableInt("damage"),
            block = optNullableInt("block"),
            hitCount = optNullableInt("hitCount"),
            cardsDraw = optNullableInt("cardsDraw"),
            energyGain = optNullableInt("energyGain"),
            target = optString("target"),
            keywords = optStringList("keywords"),
            spawnsCards = optStringList("spawnsCards"),
            autoTags = optStringList("autoTags"),
            recommendTags = optStringList("recommendTags"),
            allTags = optStringList("allTags"),
            beginnerTier = optString("beginnerTier"),
            upgradeDependency = optString("upgradeDependency"),
            extraAction = optString("extraAction"),
            scaling = CardScaling(
                act1 = scalingJson?.optString("act1").orEmpty(),
                act2 = scalingJson?.optString("act2").orEmpty(),
                act3 = scalingJson?.optString("act3").orEmpty(),
            ),
            synergyKo = optStringList("synergyKo"),
            tagsKo = optStringList("tagsKo"),
            coreCardsKo = optStringList("coreCardsKo"),
        )
    }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name)) null else optInt(name)

    private fun JSONObject.optStringList(name: String): List<String> {
        val array = optJSONArray(name) ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
    }

    private fun parseDisplayFieldsFallback(json: String): List<SilentCard> {
        return json
            .removePrefix("[")
            .removeSuffix("]")
            .split(Regex("""(?m)^\s{2}},\s*\R\s{2}\{"""))
            .mapNotNull { rawObject ->
                val cardText = rawObject.trim()
                val id = cardText.stringValue("id")
                val image = cardText.stringValue("image")
                if (id.isBlank() || image.isBlank()) return@mapNotNull null

                SilentCard(
                    id = id,
                    nameKo = cardText.stringValue("nameKo").ifBlank { id },
                    normalizedName = cardText.stringValue("normalizedName"),
                    cost = cardText.intValue("cost"),
                    type = cardText.stringValue("type"),
                    typeKo = cardText.stringValue("typeKo"),
                    rarity = cardText.stringValue("rarity"),
                    rarityKo = cardText.stringValue("rarityKo"),
                    description = cardText.stringValue("description"),
                    image = image,
                    damage = cardText.intValue("damage"),
                    block = cardText.intValue("block"),
                    hitCount = cardText.intValue("hitCount"),
                    cardsDraw = cardText.intValue("cardsDraw"),
                    energyGain = cardText.intValue("energyGain"),
                    target = cardText.stringValue("target"),
                    keywords = cardText.stringArrayValue("keywords"),
                    spawnsCards = cardText.stringArrayValue("spawnsCards"),
                    autoTags = cardText.stringArrayValue("autoTags"),
                    recommendTags = cardText.stringArrayValue("recommendTags"),
                    allTags = cardText.stringArrayValue("allTags"),
                    beginnerTier = cardText.stringValue("beginnerTier"),
                    upgradeDependency = cardText.stringValue("upgradeDependency"),
                    extraAction = cardText.stringValue("extraAction"),
                    scaling = CardScaling(
                        act1 = cardText.stringValue("act1"),
                        act2 = cardText.stringValue("act2"),
                        act3 = cardText.stringValue("act3"),
                    ),
                    synergyKo = cardText.stringArrayValue("synergyKo"),
                    tagsKo = cardText.stringArrayValue("tagsKo"),
                    coreCardsKo = cardText.stringArrayValue("coreCardsKo"),
                )
            }
    }

    private fun String.stringValue(key: String): String {
        val line = lineSequence().firstOrNull { it.contains("\"$key\"") } ?: return ""
        return line
            .substringAfter(':', "")
            .trim()
            .removeSuffix(",")
            .trim()
            .removeSurrounding("\"")
            .trim()
    }

    private fun String.intValue(key: String): Int? {
        val value = stringValue(key)
        return value.takeUnless { it == "null" || it.isBlank() }?.toIntOrNull()
    }

    private fun String.stringArrayValue(key: String): List<String> {
        val match = Regex(
            pattern = """"$key"\s*:\s*\[(.*?)]""",
            options = setOf(RegexOption.DOT_MATCHES_ALL),
        ).find(this) ?: return emptyList()

        return Regex(""""([^"]*)"""")
            .findAll(match.groupValues[1])
            .map { it.groupValues[1] }
            .toList()
    }

    private companion object {
        const val SILENT_CARDS_PATH = "data/silent_cards_app.json"
    }
}
