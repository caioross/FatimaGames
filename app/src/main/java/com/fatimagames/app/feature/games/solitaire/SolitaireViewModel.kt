package com.fatimagames.app.feature.games.solitaire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.solitaire.domain.Card
import com.fatimagames.app.feature.games.solitaire.domain.Pile
import com.fatimagames.app.feature.games.solitaire.domain.Selection
import com.fatimagames.app.feature.games.solitaire.domain.SolitaireEngine
import com.fatimagames.app.feature.games.solitaire.domain.SolitaireSnapshot
import com.fatimagames.app.domain.repository.GameStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SolitaireUiState(
    val tableau: List<List<Card>> = emptyList(),
    val foundations: List<List<Card>> = emptyList(),
    val stockCount: Int = 0,
    val waste: List<Card> = emptyList(),
    val selection: Selection? = null,
    val moves: Int = 0,
    val elapsedMs: Long = 0,
    val completed: Boolean = false,
)

@HiltViewModel
class SolitaireViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
    private val stateRepo: GameStateRepository,
) : ViewModel() {

    private var engine = SolitaireEngine()
    private var startTime = System.currentTimeMillis()
    private val json = Json { ignoreUnknownKeys = true }
    private var saveJob: Job? = null

    private val _uiState = MutableStateFlow(SolitaireUiState())
    val uiState: StateFlow<SolitaireUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val raw = stateRepo.loadSnapshot(GameType.SOLITAIRE)
            if (raw != null) {
                runCatching {
                    val snap = json.decodeFromString<SolitaireSnapshot>(raw)
                    engine.loadFromSnapshot(snap)
                }
            }
            publish()
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            stateRepo.saveSnapshot(GameType.SOLITAIRE, json.encodeToString(engine.toSnapshot()))
        }
    }

    fun onDrawClick() {
        engine.drawFromStock()
        clearSelection()
        publish()
    }

    fun onTableauCardTap(col: Int, indexInPile: Int) {
        val pile = Pile.Tableau(col)
        handleTapOnSource(pile, indexInPile)
    }

    fun onWasteTap() {
        handleTapOnSource(Pile.Waste, 0)
    }

    fun onFoundationTap(idx: Int) {
        handleTapOnSource(Pile.Foundation(idx), 0)
    }

    fun onEmptyTableauTap(col: Int) {
        val sel = _uiState.value.selection ?: return
        if (engine.tryMove(sel.pile, sel.fromIndex, Pile.Tableau(col))) {
            clearSelectionAndPublish()
        }
    }

    fun onEmptyFoundationTap(idx: Int) {
        val sel = _uiState.value.selection ?: return
        if (engine.tryMove(sel.pile, sel.fromIndex, Pile.Foundation(idx))) {
            clearSelectionAndPublish()
        }
    }

    private fun handleTapOnSource(pile: Pile, indexInPile: Int) {
        val sel = _uiState.value.selection
        if (sel == null) {
            // Primeira seleção
            _uiState.value = _uiState.value.copy(selection = Selection(pile, indexInPile))
            return
        }
        if (sel.pile == pile && sel.fromIndex == indexInPile) {
            // Toque na mesma coisa: deseleciona
            clearSelectionAndPublish()
            return
        }
        // Tentar mover sel -> (pile)
        if (engine.tryMove(sel.pile, sel.fromIndex, pile)) {
            clearSelectionAndPublish()
            if (engine.isWin()) finishGame()
            return
        }
        // Se não funcionou, troca a seleção (a nova vira a selecionada)
        _uiState.value = _uiState.value.copy(selection = Selection(pile, indexInPile))
    }

    fun onAutoToFoundation(pile: Pile) {
        if (engine.autoToFoundation(pile)) {
            clearSelectionAndPublish()
            if (engine.isWin()) finishGame()
        }
    }

    fun onRestart() {
        engine = SolitaireEngine()
        startTime = System.currentTimeMillis()
        clearSelection()
        viewModelScope.launch { stateRepo.clearSnapshot(GameType.SOLITAIRE) }
        publish()
    }

    private fun clearSelection() {
        _uiState.value = _uiState.value.copy(selection = null)
    }
    private fun clearSelectionAndPublish() {
        _uiState.value = _uiState.value.copy(selection = null)
        publish()
        scheduleSave()
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            tableau = engine.tableau.map { it.toList() },
            foundations = engine.foundations.map { it.toList() },
            stockCount = engine.stock.size,
            waste = engine.waste.toList(),
            moves = engine.moves,
            elapsedMs = System.currentTimeMillis() - startTime,
            completed = engine.isWin(),
        )
    }

    private fun finishGame() {
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.SOLITAIRE,
                    score = null,
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = "KLONDIKE",
                    finishedAt = System.currentTimeMillis(),
                )
            )
            stateRepo.clearSnapshot(GameType.SOLITAIRE)
        }
    }
}
