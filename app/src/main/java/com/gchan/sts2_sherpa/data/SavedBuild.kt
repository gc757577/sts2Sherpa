package com.gchan.sts2_sherpa.data

data class SavedBuild(
    val id: String,
    val name: String,
    val description: String,
    val deck: List<DeckCard>,
    val totalCardCount: Int,
    val completionScore: Int,
    val directionLabel: String,
    val source: String,
    val createdAt: Long,
)

