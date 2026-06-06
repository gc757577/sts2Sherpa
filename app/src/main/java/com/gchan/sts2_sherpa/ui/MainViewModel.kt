package com.gchan.sts2_sherpa.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gchan.sts2_sherpa.data.CardRepository
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SilentCard
import com.gchan.sts2_sherpa.logic.RecommendationEngine
import com.gchan.sts2_sherpa.logic.RecommendationResult
import com.gchan.sts2_sherpa.ocr.CardNameMatcher
import com.gchan.sts2_sherpa.ocr.OcrCardRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = true,
    val cards: List<SilentCard> = emptyList(),
    val selectedRewardCards: List<SilentCard?> = List(REWARD_SLOT_COUNT) { null },
    val currentDeck: List<DeckCard> = emptyList(),
    val recommendationResult: RecommendationResult? = null,
    val pickerSlotIndex: Int? = null,
    val isDeckDialogOpen: Boolean = false,
    val isRecognizing: Boolean = false,
    val pendingRecognizedCards: List<SilentCard?> = List(REWARD_SLOT_COUNT) { null },
    val isOcrResultDialogOpen: Boolean = false,
    val ocrPickerSlotIndex: Int? = null,
    val ocrRawText: String = "",
    val ocrMessage: String? = null,
    val errorMessage: String? = null,
) {
    val isRewardFull: Boolean
        get() = selectedRewardCards.all { it != null }

    val deckCardCount: Int
        get() = currentDeck.sumOf { it.count }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CardRepository(application.assets)
    private val ocrCardRecognizer = OcrCardRecognizer(application.applicationContext)
    private val cardNameMatcher = CardNameMatcher()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadCards()
    }

    fun loadCards() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.loadSilentCards() }
                .onSuccess { cards ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cards = cards,
                            currentDeck = it.currentDeck.ifEmpty { createInitialSilentDeck(cards) },
                            errorMessage = null,
                        ).withUpdatedRecommendation()
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cards = emptyList(),
                            errorMessage = throwable.message ?: "Failed to load cards",
                        )
                    }
                }
        }
    }

    fun openCardPicker(slotIndex: Int) {
        if (slotIndex !in 0 until REWARD_SLOT_COUNT) return
        _uiState.update { it.copy(pickerSlotIndex = slotIndex) }
    }

    fun closeCardPicker() {
        _uiState.update { it.copy(pickerSlotIndex = null) }
    }

    fun openDeckDialog() {
        _uiState.update { it.copy(isDeckDialogOpen = true) }
    }

    fun closeDeckDialog() {
        _uiState.update { it.copy(isDeckDialogOpen = false) }
    }

    fun selectCardForSlot(card: SilentCard) {
        _uiState.update { currentState ->
            val slotIndex = currentState.pickerSlotIndex ?: return@update currentState
            currentState.copy(
                selectedRewardCards = currentState.selectedRewardCards.mapIndexed { index, selectedCard ->
                    if (index == slotIndex) card else selectedCard
                },
                pickerSlotIndex = null,
            ).withUpdatedRecommendation()
        }
    }

    fun clearRewardSlots() {
        _uiState.update {
            it.copy(
                selectedRewardCards = List(REWARD_SLOT_COUNT) { null },
                pickerSlotIndex = null,
                recommendationResult = null,
            )
        }
    }

    fun addRewardCardToDeck(card: SilentCard) {
        _uiState.update { currentState ->
            currentState.copy(
                currentDeck = addCardToDeck(currentState.currentDeck, card),
                selectedRewardCards = List(REWARD_SLOT_COUNT) { null },
                pickerSlotIndex = null,
                recommendationResult = null,
            )
        }
    }

    fun recognizeCardsFromImage(uri: Uri) {
        val cards = _uiState.value.cards
        if (cards.isEmpty() || _uiState.value.isRecognizing) return

        _uiState.update { it.copy(isRecognizing = true, ocrMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ocrCardRecognizer.recognizeText(uri)
            }.onSuccess { recognizedText ->
                handleRecognizedText(recognizedText, cards)
            }.onFailure {
                updateOcrFailure()
            }
        }
    }

    fun recognizeCardsFromBitmap(bitmap: Bitmap) {
        val cards = _uiState.value.cards
        if (cards.isEmpty() || _uiState.value.isRecognizing) return

        _uiState.update { it.copy(isRecognizing = true, ocrMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ocrCardRecognizer.recognizeText(bitmap)
            }.onSuccess { recognizedText ->
                handleRecognizedText(recognizedText, cards)
            }.onFailure {
                updateOcrFailure()
            }
        }
    }

    private fun handleRecognizedText(
        recognizedText: String,
        cards: List<SilentCard>,
    ) {
        val recognizedCards = cardNameMatcher.findMatchingCards(recognizedText, cards)
        if (recognizedCards.isEmpty()) {
            _uiState.update {
                it.copy(
                    isRecognizing = false,
                    pendingRecognizedCards = List(REWARD_SLOT_COUNT) { null },
                    isOcrResultDialogOpen = false,
                    ocrPickerSlotIndex = null,
                    ocrRawText = recognizedText,
                    ocrMessage = "카드를 인식하지 못했습니다. 직접 선택해주세요.",
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isRecognizing = false,
                    pendingRecognizedCards = List(REWARD_SLOT_COUNT) { index ->
                        recognizedCards.getOrNull(index)
                    },
                    isOcrResultDialogOpen = true,
                    ocrPickerSlotIndex = null,
                    ocrRawText = recognizedText,
                    ocrMessage = null,
                )
            }
        }
    }

    private fun updateOcrFailure() {
        _uiState.update {
            it.copy(
                isRecognizing = false,
                ocrMessage = "카드를 인식하지 못했습니다. 직접 선택해주세요.",
            )
        }
    }

    fun applyRecognizedCards(cards: List<SilentCard>) {
        _uiState.update {
            it.copy(
                pendingRecognizedCards = List(REWARD_SLOT_COUNT) { index ->
                    cards.getOrNull(index)
                },
                isOcrResultDialogOpen = true,
                ocrPickerSlotIndex = null,
            )
        }
    }

    fun openOcrResultCardPicker(slotIndex: Int) {
        if (slotIndex !in 0 until REWARD_SLOT_COUNT) return
        _uiState.update { it.copy(ocrPickerSlotIndex = slotIndex) }
    }

    fun closeOcrResultCardPicker() {
        _uiState.update { it.copy(ocrPickerSlotIndex = null) }
    }

    fun selectPendingRecognizedCard(card: SilentCard) {
        _uiState.update { currentState ->
            val slotIndex = currentState.ocrPickerSlotIndex ?: return@update currentState
            currentState.copy(
                pendingRecognizedCards = currentState.pendingRecognizedCards.mapIndexed { index, pendingCard ->
                    if (index == slotIndex) card else pendingCard
                },
                ocrPickerSlotIndex = null,
            )
        }
    }

    fun confirmRecognizedCards() {
        _uiState.update { currentState ->
            currentState.copy(
                selectedRewardCards = List(REWARD_SLOT_COUNT) { index ->
                    currentState.pendingRecognizedCards.getOrNull(index)
                },
                pickerSlotIndex = null,
                pendingRecognizedCards = List(REWARD_SLOT_COUNT) { null },
                isOcrResultDialogOpen = false,
                ocrPickerSlotIndex = null,
                ocrRawText = "",
                ocrMessage = null,
            ).withUpdatedRecommendation()
        }
    }

    fun cancelRecognizedCards() {
        _uiState.update {
            it.copy(
                pendingRecognizedCards = List(REWARD_SLOT_COUNT) { null },
                isOcrResultDialogOpen = false,
                ocrPickerSlotIndex = null,
                ocrRawText = "",
            )
        }
    }

    fun consumeOcrMessage() {
        _uiState.update { it.copy(ocrMessage = null) }
    }

    private fun MainUiState.withUpdatedRecommendation(): MainUiState {
        val candidates = selectedRewardCards.filterNotNull()
        val recommendation = if (candidates.size == REWARD_SLOT_COUNT) {
            RecommendationEngine.recommend(
                currentDeck = currentDeck,
                candidates = candidates,
            )
        } else {
            null
        }

        return copy(recommendationResult = recommendation)
    }

    private fun createInitialSilentDeck(cards: List<SilentCard>): List<DeckCard> {
        val cardsById = cards.associateBy { it.id }
        return INITIAL_SILENT_DECK.mapNotNull { (cardId, count) ->
            cardsById[cardId]?.let { card -> DeckCard(card = card, count = count) }
        }
    }

    private fun addCardToDeck(deck: List<DeckCard>, card: SilentCard): List<DeckCard> {
        if (deck.none { it.card.id == card.id }) {
            return deck + DeckCard(card = card, count = 1)
        }

        return deck.map { deckCard ->
            if (deckCard.card.id == card.id) {
                deckCard.copy(count = deckCard.count + 1)
            } else {
                deckCard
            }
        }
    }
}

private const val REWARD_SLOT_COUNT = 3

private val INITIAL_SILENT_DECK = listOf(
    "STRIKE_SILENT" to 5,
    "DEFEND_SILENT" to 5,
    "NEUTRALIZE" to 1,
    "SURVIVOR" to 1,
)
