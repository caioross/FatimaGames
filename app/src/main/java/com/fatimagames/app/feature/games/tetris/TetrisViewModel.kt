package com.fatimagames.app.feature.games.tetris

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.tetris.domain.BOARD_COLS
import com.fatimagames.app.feature.games.tetris.domain.BOARD_ROWS
import com.fatimagames.app.feature.games.tetris.domain.FallingPiece
import com.fatimagames.app.feature.games.tetris.domain.Tetromino
import com.fatimagames.app.feature.games.tetris.domain.TetrisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TetrisUiState(
    val grid: List<List<Int>> = emptyList(),
    val current: FallingPiece? = null,
    val next: Tetromino? = null,
    val score: Long = 0L,
    val level: Int = 1,
    val lines: Int = 0,
    val gameOver: Boolean = false,
    val paused: Boolean = false,
)

@HiltViewModel
class TetrisViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
) : ViewModel() {

    private var engine = TetrisEngine()
    private var loopJob: Job? = null
    private val startTime = System.currentTimeMillis()
    private var finished = false

    private val _uiState = MutableStateFlow(TetrisUiState())
    val uiState: StateFlow<TetrisUiState> = _uiState.asStateFlow()

    init {
        engine.spawnNext()
        publish()
        startLoop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (true) {
                delay(engine.tickInterval())
                if (_uiState.value.paused) continue
                if (engine.gameOver) break
                if (engine.current == null) {
                    engine.spawnNext()
                } else {
                    engine.tick()
                    if (engine.current == null) engine.spawnNext()
                }
                publish()
                if (engine.gameOver) {
                    saveRecord()
                    break
                }
            }
        }
    }

    fun moveLeft() { if (!_uiState.value.paused) { engine.moveLeft(); publish() } }
    fun moveRight() { if (!_uiState.value.paused) { engine.moveRight(); publish() } }
    fun rotate() { if (!_uiState.value.paused) { engine.rotate(); publish() } }
    fun softDrop() {
        if (_uiState.value.paused) return
        engine.softDrop()
        if (engine.current == null) engine.spawnNext()
        publish()
    }
    fun hardDrop() {
        if (_uiState.value.paused) return
        engine.hardDrop()
        engine.spawnNext()
        publish()
        if (engine.gameOver) saveRecord()
    }

    fun togglePause() {
        _uiState.value = _uiState.value.copy(paused = !_uiState.value.paused)
    }

    fun restart() {
        loopJob?.cancel()
        finished = false
        engine = TetrisEngine()
        engine.spawnNext()
        publish()
        startLoop()
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            grid = engine.board.map { it.toList() },
            current = engine.current,
            next = engine.next,
            score = engine.score,
            level = engine.level,
            lines = engine.lines,
            gameOver = engine.gameOver,
        )
    }

    private fun saveRecord() {
        if (finished) return
        finished = true
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.TETRIS,
                    score = engine.score,
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = "LV${engine.level}",
                    finishedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }
}
