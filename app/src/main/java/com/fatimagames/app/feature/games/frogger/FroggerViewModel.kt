package com.fatimagames.app.feature.games.frogger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.frogger.domain.FrogStatus
import com.fatimagames.app.feature.games.frogger.domain.Frog
import com.fatimagames.app.feature.games.frogger.domain.FroggerEngine
import com.fatimagames.app.feature.games.frogger.domain.Lane
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FroggerUiState(
    val frog: Frog = Frog(12, 5),
    val lanes: List<Lane> = emptyList(),
    val obstaclePositions: Map<Int, List<Pair<Float, Int>>> = emptyMap(),
    val lives: Int = 3,
    val score: Long = 0,
    val status: FrogStatus = FrogStatus.ALIVE,
    val goalsReached: Int = 0,
)

@HiltViewModel
class FroggerViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
) : ViewModel() {

    private var engine = FroggerEngine()
    private var loopJob: Job? = null
    private val startTime = System.currentTimeMillis()
    private var finished = false

    private val _uiState = MutableStateFlow(FroggerUiState())
    val uiState: StateFlow<FroggerUiState> = _uiState.asStateFlow()

    init {
        publish()
        startLoop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            val dt = 50L
            while (true) {
                delay(dt)
                engine.update(dt / 1000f)
                publish()
                if (engine.status != FrogStatus.ALIVE) {
                    saveRecord()
                    break
                }
            }
        }
    }

    fun moveUp() { engine.moveUp(); publish() }
    fun moveDown() { engine.moveDown(); publish() }
    fun moveLeft() { engine.moveLeft(); publish() }
    fun moveRight() { engine.moveRight(); publish() }

    fun restart() {
        loopJob?.cancel()
        finished = false
        engine = FroggerEngine()
        publish()
        startLoop()
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            frog = engine.frog,
            lanes = engine.lanes,
            obstaclePositions = engine.obstaclePositions(),
            lives = engine.lives,
            score = engine.score,
            status = engine.status,
            goalsReached = engine.goalsReached,
        )
    }

    private fun saveRecord() {
        if (finished) return
        finished = true
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.FROGGER,
                    score = engine.score,
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = "CLASSIC",
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
