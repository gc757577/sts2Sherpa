package com.gchan.sts2_sherpa.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BuildRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadSavedBuilds(cards: List<SilentCard>): List<SavedBuild> {
        val cardsById = cards.associateBy { it.id }
        val rawJson = preferences.getString(KEY_SAVED_BUILDS, null).orEmpty()
        if (rawJson.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(rawJson)
            List(array.length()) { index -> array.getJSONObject(index).toSavedBuild(cardsById) }
                .filter { it.deck.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    fun saveSavedBuilds(builds: List<SavedBuild>) {
        val array = JSONArray()
        builds.forEach { build -> array.put(build.toJson()) }
        preferences.edit().putString(KEY_SAVED_BUILDS, array.toString()).apply()
    }

    fun updateSavedBuild(
        builds: List<SavedBuild>,
        updatedBuild: SavedBuild,
    ): List<SavedBuild> {
        val nextBuilds = builds.map { build ->
            if (build.id == updatedBuild.id) updatedBuild else build
        }
        saveSavedBuilds(nextBuilds)
        return nextBuilds
    }

    private fun JSONObject.toSavedBuild(cardsById: Map<String, SilentCard>): SavedBuild {
        val deck = optJSONArray("deck").toDeckCards(cardsById)
        return SavedBuild(
            id = optString("id").ifBlank { "build_${optLong("createdAt", System.currentTimeMillis())}" },
            name = optString("name").ifBlank { "저장된 빌드" },
            description = optString("description"),
            deck = deck,
            totalCardCount = optInt("totalCardCount", deck.sumOf { it.count }),
            completionScore = optInt("completionScore", 0),
            directionLabel = optString("directionLabel").ifBlank { "방향 미정" },
            source = optString("source").ifBlank { "알 수 없음" },
            createdAt = optLong("createdAt", System.currentTimeMillis()),
        )
    }

    private fun JSONArray?.toDeckCards(cardsById: Map<String, SilentCard>): List<DeckCard> {
        if (this == null) return emptyList()
        return List(length()) { index -> optJSONObject(index) }
            .mapNotNull { item ->
                val cardId = item?.optString("cardId").orEmpty()
                val count = item?.optInt("count", 0) ?: 0
                val card = cardsById[cardId]
                if (card == null || count <= 0) null else DeckCard(card = card, count = count)
            }
    }

    private fun SavedBuild.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("description", description)
            .put("deck", JSONArray().also { array ->
                deck.forEach { deckCard ->
                    array.put(
                        JSONObject()
                            .put("cardId", deckCard.card.id)
                            .put("count", deckCard.count),
                    )
                }
            })
            .put("totalCardCount", totalCardCount)
            .put("completionScore", completionScore)
            .put("directionLabel", directionLabel)
            .put("source", source)
            .put("createdAt", createdAt)

    private companion object {
        const val PREFERENCES_NAME = "saved_builds"
        const val KEY_SAVED_BUILDS = "saved_builds_json"
    }
}
