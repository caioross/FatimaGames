package com.fatimagames.app.feature.games.colorsort

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.colorsort.domain.ColorSortEngine
import com.fatimagames.app.feature.games.colorsort.domain.ColorSortStages
import com.fatimagames.app.feature.games.colorsort.domain.Tube
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ColorSortUiState(
    val tubes: List<Tube> = emptyList(),
    val selectedTube: Int? = null,
    val stage: Int = 1,
    val moves: Int = 0,
    val elapsedMs: Long = 0,
    val completed: Boolean = false,
)

@HiltViewModel
class ColorSortViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var engine = ColorSortEngine(ColorSortStages.stage(1))
    private var stage = 1
    private val startTime = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(ColorSortUiState())
    val uiState: StateFlow<ColorSortUiState> = _uiState.asStateFlow()

    init { publish() }

    fun initialize(stage: Int) {
        if (this.stage != stage) {
            this.stage = stage
            engine = ColorSortEngine(ColorSortStages.stage(stage))
            publish()
        }
    }

    fun onTubeTap(idx: Int) {
        val selected = _uiState.value.selectedTube
        if (selected == null) {
            _uiState.value = _uiState.value.copy(selectedTube = idx)
            return
        }
        if (selected == idx) {
            _uiState.value = _uiState.value.copy(selectedTube = null)
            return
        }
        val moved = engine.transfer(selected, idx)
        if (moved && engine.isWin()) {
            finishStage()
        }
        _uiState.value = _uiState.value.copy(selectedTube = null)
        publish()
    }

    fun onUndo() {
        engine.undo()
        publish()
    }

    fun onAddTube() {
        engine.addEmptyTube()
        publish()
    }

    fun onRestart() {
        engine = ColorSortEngine(ColorSortStages.stage(stage))
        _uiState.value = ColorSortUiState(stage = stage)
        publish()
    }

    private fun finishStage() {
        viewModelScope.launch {
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.COLOR_SORT,
                    score = null,
                    durationMs = System.currentTimeMillis() - startTime,
                    difficulty = "STAGE_$stage",
                    finishedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun publish() {
        _uiState.value = _uiState.value.copy(
            tubes = engine.snapshot(),
            stage = stage,
            moves = engine.movesMade,
            completed = engine.isWin(),
            elapsedMs = System.currentTimeMillis() - startTime,
        )
    }
}
