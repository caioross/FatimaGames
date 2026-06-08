package com.fatimagames.app.feature.games.minesweeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.minesweeper.domain.Cell
import com.fatimagames.app.feature.games.minesweeper.domain.Difficulty
import com.fatimagames.app.feature.games.minesweeper.domain.GameStatus
import com.fatimagames.app.feature.games.minesweeper.domain.MinesweeperEngine
import com.fatimagames.app.feature.games.minesweeper.domain.MinesweeperSnapshot
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

data class MinesweeperUiState(
    val grid: List<List<Cell>> = emptyList(),
    val rows: Int = 9,
    val cols: Int = 9,
    val flagsRemaining: Int = 10,
    val flagMode: Boolean = false,
    val difficulty: Difficulty = Difficulty.EASY,
    val status: GameStatus = GameStatus.Playing,
    val elapsedMs: Long = 0,
)

@HiltViewModel
class MinesweeperViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
    private val stateRepo: GameStateRepository,
) : ViewModel() {

    private var engine = MinesweeperEngine(Difficulty.EASY)
    private var startTime = System.currentTimeMillis()
    private val json = Json { ignoreUnknownKeys = true }
    private var saveJob: Job? = null

    private val _uiState = MutableStateFlow(MinesweeperUiState())
    val uiState: StateFlow<MinesweeperUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val raw = stateRepo.loadSnapshot(GameType.MINESWEEPER)
            if (raw != null) {
                runCatching {
                    val snap = json.decodeFromString<MinesweeperSnapshot>(raw)
                    val diff = runCatching { Difficulty.valueOf(snap.difficulty) }.getOrNull() ?: Difficulty.EASY
                    engine = MinesweeperEngine(diff)
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
            stateRepo.saveSnapshot(GameType.MINESWEEPER, json.encodeToString(engine.toSnapshot()))
        }
    }

    fun setDifficulty(d: Difficulty) {
        engine = MinesweeperEngine(d)
        startTime = System.currentTimeMillis()
        _uiState.value = MinesweeperUiState(difficulty = d)
        viewModelScope.launch { stateRepo.clearSnapshot(GameType.MINESWEEPER) }
        publish()
    }

    fun onRestart() {
        engine = MinesweeperEngine(engine.difficulty)
        startTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(flagMode = false)
        viewModelScope.launch { stateRepo.clearSnapshot(GameType.MINESWEEPER) }
        publish()
    }

    fun toggleFlagMode() {
        _uiState.value = _uiState.value.copy(flagMode = !_uiState.value.flagMode)
    }

    fun onCellTap(r: Int, c: Int) {
        if (engine.status !is GameStatus.Playing) return
        if (_uiState.value.flagMode) engine.toggleFlag(r, c)
        else engine.tap(r, c)
        scheduleSave()
        publish()
        if (engine.status is GameStatus.Won) finishGame()
    }

    fun onCellLongPress(r: Int, c: Int) {
        if (engine.status !is GameStatus.Playing) return
        engine.toggleFlag(r, c)
        scheduleSave()
        publish()
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            grid = engine.snapshot(),
            rows = engine.rows,
            cols = engine.cols,
            flagsRemaining = engine.flagsRemaining,
            difficulty = engine.difficulty,
            status = engine.status,
            elapsedMs = System.currentTimeMillis() - startTime,
        )
    }

    private fun finishGame() {
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.MINESWEEPER,
                    score = null,
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = engine.difficulty.label,
                    finishedAt = System.currentTimeMillis(),
                )
            )
            stateRepo.clearSnapshot(GameType.MINESWEEPER)
        }
    }
}
