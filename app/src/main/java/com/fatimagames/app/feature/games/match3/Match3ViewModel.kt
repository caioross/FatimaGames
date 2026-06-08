package com.fatimagames.app.feature.games.match3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.match3.domain.CellContent
import com.fatimagames.app.feature.games.match3.domain.Match3Engine
import com.fatimagames.app.feature.games.match3.domain.Match3Snapshot
import com.fatimagames.app.feature.games.match3.domain.SwapResult
import com.fatimagames.app.core.feedback.HapticController
import com.fatimagames.app.domain.repository.GameStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SwapAnimation(
    val r1: Int,
    val c1: Int,
    val r2: Int,
    val c2: Int,
    val willMatch: Boolean,
    val phase: Phase,
) {
    enum class Phase { OUTGOING, RETURNING }
}

data class FloatingScoreEvent(
    val id: Long,
    val text: String,
    val r: Int,
    val c: Int,
)

data class Match3UiState(
    val board: List<List<CellContent>> = emptyList(),
    val score: Long = 0,
    val stage: Int = 1,
    val selected: Pair<Int, Int>? = null,
    val swapping: SwapAnimation? = null,
    val cascadeLevels: Int = 0,
    val lastGain: Long = 0,
    val floaters: List<FloatingScoreEvent> = emptyList(),
    val animatingExplosion: Set<Pair<Int, Int>> = emptySet(),
    val inputLocked: Boolean = false,
)

@HiltViewModel
class Match3ViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
    private val stateRepo: GameStateRepository,
    private val haptic: HapticController,
) : ViewModel() {
    private var engine = Match3Engine()
    private val startTime = System.currentTimeMillis()
    private var nextFloaterId = 0L
    private val json = Json { ignoreUnknownKeys = true }
    private var saveJob: Job? = null

    private val _uiState = MutableStateFlow(Match3UiState())
    val uiState: StateFlow<Match3UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val raw = stateRepo.loadSnapshot(GameType.MATCH3)
            if (raw != null) {
                runCatching {
                    val snap = json.decodeFromString<Match3Snapshot>(raw)
                    engine.loadFromSnapshot(snap)
                }
            }
            publish()
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            stateRepo.saveSnapshot(GameType.MATCH3, json.encodeToString(engine.toSnapshot()))
        }
    }

    fun onCellTap(r: Int, c: Int) {
        if (_uiState.value.inputLocked) return
        val current = _uiState.value.selected
        if (current == null) {
            _uiState.value = _uiState.value.copy(selected = r to c)
            return
        }
        if (current == (r to c)) {
            _uiState.value = _uiState.value.copy(selected = null)
            return
        }
        // Verifica se é vizinha
        val adjacent = (current.first == r && kotlin.math.abs(current.second - c) == 1) ||
            (current.second == c && kotlin.math.abs(current.first - r) == 1)
        if (!adjacent) {
            // Substitui seleção
            _uiState.value = _uiState.value.copy(selected = r to c)
            return
        }
        // Swap adjacente: anima primeiro
        val willMatch = engine.peekSwapWouldMatch(current.first, current.second, r, c)
        viewModelScope.launch {
            performSwapWithAnimation(current.first, current.second, r, c, willMatch)
        }
    }

    private suspend fun performSwapWithAnimation(
        r1: Int, c1: Int, r2: Int, c2: Int, willMatch: Boolean,
    ) {
        // Fase 1: animação de "ida" — gemas trocam visualmente
        _uiState.value = _uiState.value.copy(
            inputLocked = true,
            selected = null,
            swapping = SwapAnimation(r1, c1, r2, c2, willMatch, SwapAnimation.Phase.OUTGOING),
        )
        delay(220)

        if (!willMatch) {
            haptic.error()
            _uiState.value = _uiState.value.copy(
                swapping = SwapAnimation(r1, c1, r2, c2, willMatch, SwapAnimation.Phase.RETURNING),
            )
            delay(220)
            _uiState.value = _uiState.value.copy(swapping = null, inputLocked = false)
            return
        }
        haptic.snap()

        // Fase 2: commit do swap (sem cascade ainda)
        engine.applySwapOnly(r1, c1, r2, c2)
        _uiState.value = _uiState.value.copy(swapping = null)
        publish()

        // Fase 3: cascade animado, step a step
        var cascadeLevel = 0
        var totalGained = 0L
        while (true) {
            val clear = engine.clearMatchesStep(cascadeLevel) ?: break

            // Mostra explosão (cells marcadas como Empty no engine, mas UI animou na transição)
            val centerR = clear.cells.map { it.first }.average().toInt()
            val centerC = clear.cells.map { it.second }.average().toInt()
            val floater = FloatingScoreEvent(
                id = nextFloaterId++,
                text = "+${clear.gained}",
                r = centerR, c = centerC,
            )
            _uiState.value = _uiState.value.copy(
                floaters = _uiState.value.floaters + floater,
                animatingExplosion = clear.cells,
                cascadeLevels = cascadeLevel,
            )
            publish()
            delay(280)

            // Aplica gravidade + spawn
            engine.applyGravityStep()
            _uiState.value = _uiState.value.copy(animatingExplosion = emptySet())
            publish()
            delay(320)

            totalGained += clear.gained
            cascadeLevel++

            // Limpa floater
            viewModelScope.launch {
                delay(700)
                _uiState.value = _uiState.value.copy(
                    floaters = _uiState.value.floaters.filter { it.id != floater.id },
                )
            }
            haptic.tick()
        }
        _uiState.value = _uiState.value.copy(lastGain = totalGained)
        scheduleSave()
        if (engine.isStageComplete()) {
            haptic.win()
            finishStageAndAdvance()
        }
        delay(120)
        _uiState.value = _uiState.value.copy(inputLocked = false)
    }

    private fun finishStageAndAdvance() {
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.MATCH3,
                    score = engine.score,
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = "STAGE_${engine.stage}",
                    finishedAt = System.currentTimeMillis(),
                )
            )
            engine.advanceStage()
            stateRepo.clearSnapshot(GameType.MATCH3)
            publish()
        }
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            board = engine.snapshot(),
            score = engine.score,
            stage = engine.stage,
        )
    }
}
