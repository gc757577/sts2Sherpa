package com.gchan.sts2_sherpa.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gchan.sts2_sherpa.data.CardRepository
import com.gchan.sts2_sherpa.data.BuildRepository
import com.gchan.sts2_sherpa.data.DeckCard
import com.gchan.sts2_sherpa.data.SavedBuild
import com.gchan.sts2_sherpa.data.SilentCard
import com.gchan.sts2_sherpa.logic.BuildAnalyzer
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
    val isCurrentDeckAddPickerOpen: Boolean = false,
    val labDeck: List<DeckCard> = emptyList(),
    val isLabDeckAddPickerOpen: Boolean = false,
    val ocrRawText: String = "",
    val ocrMessage: String? = null,
    val savedBuilds: List<SavedBuild> = emptyList(),
    val isSaveCurrentBuildDialogOpen: Boolean = false,
    val currentBuildDefaultName: String = "",
    val buildMessage: String? = null,
    val errorMessage: String? = null,
) {
    val isRewardFull: Boolean
        get() = selectedRewardCards.all { it != null }

    val deckCardCount: Int
        get() = currentDeck.sumOf { it.count }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CardRepository(application.assets)
    private val buildRepository = BuildRepository(application.applicationContext)
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
                            labDeck = it.labDeck.ifEmpty { createInitialSilentDeck(cards) },
                            savedBuilds = buildRepository.loadSavedBuilds(cards),
                            errorMessage = null,
                        ).withUpdatedRecommendation()
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cards = emptyList(),
                            currentDeck = emptyList(),
                            recommendationResult = null,
                            errorMessage = "카드 데이터를 불러오지 못했습니다.",
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

    fun removeCardFromDeck(cardId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                currentDeck = removeOneCardFromDeck(currentState.currentDeck, cardId),
            ).withUpdatedRecommendation()
        }
    }

    fun openCurrentDeckAddCardPicker() {
        _uiState.update { it.copy(isCurrentDeckAddPickerOpen = true) }
    }

    fun closeCurrentDeckAddCardPicker() {
        _uiState.update { it.copy(isCurrentDeckAddPickerOpen = false) }
    }

    fun addPickedCardToCurrentDeck(card: SilentCard) {
        _uiState.update { currentState ->
            currentState.copy(
                currentDeck = addCardToDeck(currentState.currentDeck, card),
                isCurrentDeckAddPickerOpen = false,
            ).withUpdatedRecommendation()
        }
    }

    fun resetToStartingDeck() {
        _uiState.update { currentState ->
            currentState.copy(
                currentDeck = createInitialSilentDeck(currentState.cards),
            ).withUpdatedRecommendation()
        }
    }

    fun openLabDeckAddCardPicker() {
        _uiState.update { it.copy(isLabDeckAddPickerOpen = true) }
    }

    fun closeLabDeckAddCardPicker() {
        _uiState.update { it.copy(isLabDeckAddPickerOpen = false) }
    }

    fun addPickedCardToLabDeck(card: SilentCard) {
        _uiState.update { currentState ->
            currentState.copy(
                labDeck = addCardToDeck(currentState.labDeck, card),
                isLabDeckAddPickerOpen = false,
            )
        }
    }

    fun removeCardFromLabDeck(cardId: String) {
        _uiState.update { currentState ->
            currentState.copy(labDeck = removeOneCardFromDeck(currentState.labDeck, cardId))
        }
    }

    fun resetLabDeckToStartingDeck() {
        _uiState.update { currentState ->
            currentState.copy(labDeck = createInitialSilentDeck(currentState.cards))
        }
    }

    fun clearLabDeck() {
        _uiState.update { it.copy(labDeck = emptyList()) }
    }

    fun saveLabDeckAsBuild(name: String, description: String) {
        saveDeckAsBuild(
            deck = _uiState.value.labDeck,
            name = name,
            description = description,
            source = "덱 실험실",
            successMessage = "실험 덱이 빌드 모음에 저장되었습니다.",
        )
    }

    fun openCurrentDeckSaveDialog() {
        _uiState.update { currentState ->
            if (currentState.currentDeck.isEmpty()) {
                currentState.copy(buildMessage = "덱이 비어 있어 저장할 수 없습니다.")
            } else {
                val directionLabel = BuildAnalyzer.directionLabel(currentState.currentDeck)
                currentState.copy(
                    isSaveCurrentBuildDialogOpen = true,
                    currentBuildDefaultName = BuildAnalyzer.defaultPlayBuildName(directionLabel),
                    buildMessage = null,
                )
            }
        }
    }

    fun closeCurrentDeckSaveDialog() {
        _uiState.update {
            it.copy(
                isSaveCurrentBuildDialogOpen = false,
                currentBuildDefaultName = "",
            )
        }
    }

    fun saveCurrentDeckAsBuild(name: String, description: String) {
        saveDeckAsBuild(
            deck = _uiState.value.currentDeck,
            name = name,
            description = description,
            source = "추천 플레이",
            successMessage = "현재 덱이 빌드 모음에 저장되었습니다.",
            closeCurrentDeckDialog = true,
        )
    }

    fun saveCustomBuild(name: String, description: String, deck: List<DeckCard>) {
        saveDeckAsBuild(
            deck = deck,
            name = name,
            description = description,
            source = "직접 생성",
            successMessage = "빌드가 저장되었습니다.",
        )
    }

    fun deleteSavedBuild(buildId: String) {
        _uiState.update { currentState ->
            val nextBuilds = currentState.savedBuilds.filterNot { it.id == buildId }
            buildRepository.saveSavedBuilds(nextBuilds)
            currentState.copy(
                savedBuilds = nextBuilds,
                buildMessage = "저장된 빌드를 삭제했습니다.",
            )
        }
    }

    fun recognizeCardsFromImage(uri: Uri) {
        val cards = _uiState.value.cards
        if (_uiState.value.isRecognizing) return
        if (cards.isEmpty()) {
            _uiState.update {
                it.copy(ocrMessage = "카드 데이터가 없어 인식을 실행할 수 없습니다.")
            }
            return
        }

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
        if (_uiState.value.isRecognizing) return
        if (cards.isEmpty()) {
            _uiState.update {
                it.copy(ocrMessage = "카드 데이터가 없어 인식을 실행할 수 없습니다.")
            }
            return
        }

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
                ocrMessage = "이미지를 인식하는 중 문제가 발생했습니다.",
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

    fun consumeBuildMessage() {
        _uiState.update { it.copy(buildMessage = null) }
    }

    private fun saveDeckAsBuild(
        deck: List<DeckCard>,
        name: String,
        description: String,
        source: String,
        successMessage: String,
        closeCurrentDeckDialog: Boolean = false,
    ) {
        _uiState.update { currentState ->
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@update currentState.copy(buildMessage = "빌드 이름을 입력해주세요.")
            }
            if (deck.isEmpty()) {
                return@update currentState.copy(buildMessage = "덱이 비어 있어 저장할 수 없습니다.")
            }

            val directionLabel = BuildAnalyzer.directionLabel(deck)
            val now = System.currentTimeMillis()
            val savedBuild = SavedBuild(
                id = "build_$now",
                name = trimmedName,
                description = description.trim(),
                deck = deck,
                totalCardCount = deck.sumOf { it.count },
                completionScore = BuildAnalyzer.completionScore(deck),
                directionLabel = directionLabel,
                source = source,
                createdAt = now,
            )
            val nextBuilds = listOf(savedBuild) + currentState.savedBuilds
            buildRepository.saveSavedBuilds(nextBuilds)
            currentState.copy(
                savedBuilds = nextBuilds,
                isSaveCurrentBuildDialogOpen = if (closeCurrentDeckDialog) false else currentState.isSaveCurrentBuildDialogOpen,
                currentBuildDefaultName = if (closeCurrentDeckDialog) "" else currentState.currentBuildDefaultName,
                buildMessage = successMessage,
            )
        }
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

    private fun removeOneCardFromDeck(deck: List<DeckCard>, cardId: String): List<DeckCard> =
        deck.mapNotNull { deckCard ->
            if (deckCard.card.id == cardId) {
                val nextCount = deckCard.count - 1
                if (nextCount > 0) deckCard.copy(count = nextCount) else null
            } else {
                deckCard
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
