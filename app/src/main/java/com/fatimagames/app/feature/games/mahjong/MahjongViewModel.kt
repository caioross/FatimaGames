package com.fatimagames.app.feature.games.mahjong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.GameStateRepository
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.mahjong.domain.MahjongEngine
import com.fatimagames.app.feature.games.mahjong.domain.MahjongLayoutBuilder
import com.fatimagames.app.feature.games.mahjong.domain.MahjongSnapshot
import com.fatimagames.app.feature.games.mahjong.domain.MahjongTile
import com.fatimagames.app.core.feedback.HapticController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class MahjongUiState(
    val tiles: List<MahjongTile> = emptyList(),
    val selectedId: Int? = null,
    val hintPair: Pair<Int, Int>? = null,
    val remaining: Int = 0,
    val elapsedMs: Long = 0,
    val completed: Boolean = false,
    val noMoves: Boolean = false,
)

@HiltViewModel
class MahjongViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
    private val stateRepo: GameStateRepository,
    private val haptic: HapticController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MahjongUiState())
    val uiState: StateFlow<MahjongUiState> = _uiState.asStateFlow()

    private var engine: MahjongEngine = MahjongEngine(MahjongLayoutBuilder.buildTurtleMVP())
    private var startTime = System.currentTimeMillis()
    private val json = Json { ignoreUnknownKeys = true }
    private var saveJob: Job? = null

    init {
        loadOrFresh()
    }

    private fun loadOrFresh() {
        viewModelScope.launch {
            val raw = stateRepo.loadSnapshot(GameType.MAHJONG)
            engine = if (raw != null) {
                runCatching {
                    val snap = json.decodeFromString<MahjongSnapshot>(raw)
                    startTime = System.currentTimeMillis() - snap.elapsedMs
                    MahjongEngine(snap.tiles)
                }.getOrElse {
                    MahjongEngine(MahjongLayoutBuilder.buildTurtleMVP())
                }
            } else {
                MahjongEngine(MahjongLayoutBuilder.buildTurtleMVP())
            }
            publish()
        }
    }

    fun onTileTap(id: Int) {
        val tile = engine.snapshot().firstOrNull { it.id == id && !it.removed } ?: return
        if (!engine.isFree(tile)) return
        val selected = _uiState.value.selectedId
        if (selected == null) {
            _uiState.value = _uiState.value.copy(selectedId = id, hintPair = null)
            return
        }
        if (selected == id) {
            _uiState.value = _uiState.value.copy(selectedId = null)
            return
        }
        val ok = engine.tryMatch(selected, id)
        if (ok) {
            haptic.snap()
            _uiState.value = _uiState.value.copy(selectedId = null)
            publish()
            scheduleSave()
            if (engine.isComplete()) {
                haptic.win()
                finishGame()
            }
        } else {
            haptic.error()
            _uiState.value = _uiState.value.copy(selectedId = id)
        }
    }

    fun onUndo() { engine.undo(); publish(); scheduleSave() }
    fun onHint() {
        val pair = engine.useHint()
        _uiState.value = _uiState.value.copy(hintPair = pair)
    }
    fun onReshuffle() { engine.reshuffle(); publish(); scheduleSave() }
    fun onRestart() {
        engine = MahjongEngine(MahjongLayoutBuilder.buildTurtleMVP())
        startTime = System.currentTimeMillis()
        _uiState.value = MahjongUiState()
        viewModelScope.launch { stateRepo.clearSnapshot(GameType.MAHJONG) }
        publish()
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            tiles = engine.snapshot(),
            remaining = engine.remainingCount(),
            completed = engine.isComplete(),
            elapsedMs = System.currentTimeMillis() - startTime,
            noMoves = !engine.isComplete() && engine.findHintPair() == null,
        )
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1000) // debounce
            val snap = MahjongSnapshot(
                tiles = engine.snapshot(),
                hintsUsed = engine.hintsUsed,
                undosUsed = engine.undosUsed,
                reshufflesUsed = engine.reshufflesUsed,
                elapsedMs = System.currentTimeMillis() - startTime,
            )
            stateRepo.saveSnapshot(GameType.MAHJONG, json.encodeToString(snap))
        }
    }

    private fun finishGame() {
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.MAHJONG,
                    score = computeScore(),
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = "TURTLE_MVP",
                    finishedAt = System.currentTimeMillis(),
                )
            )
            stateRepo.clearSnapshot(GameType.MAHJONG)
        }
    }

    private fun computeScore(): Long {
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
        val base = 10_000L
        val timePenalty = maxOf(0L, (elapsedSec - 600L)) * 5L
        val hintPenalty = engine.hintsUsed * 50L
        val undoPenalty = engine.undosUsed * 10L
        val reshufflePenalty = engine.reshufflesUsed * 100L
        return (base - timePenalty - hintPenalty - undoPenalty - reshufflePenalty).coerceAtLeast(0)
    }
}
