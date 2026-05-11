package com.fatimagames.app.feature.games.match3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.match3.domain.CellContent
import com.fatimagames.app.feature.games.match3.domain.Match3Engine
import com.fatimagames.app.feature.games.match3.domain.SwapResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Match3UiState(
    val board: List<List<CellContent>> = emptyList(),
    val score: Long = 0,
    val stage: Int = 1,
    val selected: Pair<Int, Int>? = null,
    val lastSwapInvalid: Boolean = false,
    val cascadeLevels: Int = 0,
)

@HiltViewModel
class Match3ViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
) : ViewModel() {
    private var engine = Match3Engine()
    private val startTime = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(Match3UiState())
    val uiState: StateFlow<Match3UiState> = _uiState.asStateFlow()

    init { publish() }

    fun onCellTap(r: Int, c: Int) {
        val current = _uiState.value.selected
        if (current == null) {
            _uiState.value = _uiState.value.copy(selected = r to c, lastSwapInvalid = false)
            return
        }
        if (current == (r to c)) {
            _uiState.value = _uiState.value.copy(selected = null)
            return
        }
        val res = engine.trySwap(current.first, current.second, r, c)
        when (res) {
            is SwapResult.Invalid -> {
                _uiState.value = _uiState.value.copy(selected = r to c, lastSwapInvalid = false)
            }
            is SwapResult.NoMatch -> {
                _uiState.value = _uiState.value.copy(selected = null, lastSwapInvalid = true)
            }
            is SwapResult.Matched -> {
                _uiState.value = _uiState.value.copy(
                    selected = null,
                    lastSwapInvalid = false,
                    cascadeLevels = res.cascadeLevels,
                )
                if (engine.isStageComplete()) {
                    finishStageAndAdvance()
                }
            }
        }
        publish()
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
