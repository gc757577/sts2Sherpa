package com.gchan.sts2_sherpa

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gchan.sts2_sherpa.ui.MainScreen
import com.gchan.sts2_sherpa.ui.MainViewModel
import com.gchan.sts2_sherpa.ui.theme.Sts2_SherpaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            Sts2_SherpaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CardsApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardsApp(
    modifier: Modifier = Modifier,
) {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    MainScreen(
        uiState = uiState,
        onRewardSlotClick = viewModel::openCardPicker,
        onRewardCardClick = viewModel::addRewardCardToDeck,
        onCardPicked = viewModel::selectCardForSlot,
        onDismissCardPicker = viewModel::closeCardPicker,
        onDeckClick = viewModel::openDeckDialog,
        onDismissDeck = viewModel::closeDeckDialog,
        onSkipClick = viewModel::clearRewardSlots,
        onRemoveDeckCard = viewModel::removeCardFromDeck,
        onResetDeck = viewModel::resetToStartingDeck,
        onOpenCurrentDeckAddCardPicker = viewModel::openCurrentDeckAddCardPicker,
        onDismissCurrentDeckAddCardPicker = viewModel::closeCurrentDeckAddCardPicker,
        onCurrentDeckCardPicked = viewModel::addPickedCardToCurrentDeck,
        onOpenCurrentDeckSave = viewModel::openCurrentDeckSaveDialog,
        onSaveCurrentDeckBuild = viewModel::saveCurrentDeckAsBuild,
        onDismissCurrentDeckSave = viewModel::closeCurrentDeckSaveDialog,
        onDeleteSavedBuild = viewModel::deleteSavedBuild,
        onBuildMessageShown = viewModel::consumeBuildMessage,
        onOpenLabDeckAddCardPicker = viewModel::openLabDeckAddCardPicker,
        onDismissLabDeckAddCardPicker = viewModel::closeLabDeckAddCardPicker,
        onLabDeckCardPicked = viewModel::addPickedCardToLabDeck,
        onRemoveLabDeckCard = viewModel::removeCardFromLabDeck,
        onResetLabDeck = viewModel::resetLabDeckToStartingDeck,
        onClearLabDeck = viewModel::clearLabDeck,
        onSaveLabDeckBuild = viewModel::saveLabDeckAsBuild,
        onSaveCustomBuild = viewModel::saveCustomBuild,
        onImageSelected = viewModel::recognizeCardsFromImage,
        onOcrMessageShown = viewModel::consumeOcrMessage,
        onOcrResultSlotClick = viewModel::openOcrResultCardPicker,
        onOcrResultCardPicked = viewModel::selectPendingRecognizedCard,
        onDismissOcrResultCardPicker = viewModel::closeOcrResultCardPicker,
        onConfirmOcrResult = viewModel::confirmRecognizedCards,
        onCancelOcrResult = viewModel::cancelRecognizedCards,
        onRetryLoad = viewModel::loadCards,
        onCompleteOnboarding = viewModel::completeOnboarding,
        modifier = modifier,
    )
}
