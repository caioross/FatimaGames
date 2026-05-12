package com.fatimagames.app.feature.games.jigsaw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.GameStateRepository
import com.fatimagames.app.domain.repository.RecordRepository
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawBoard
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawBoardBuilder
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawEngine
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawSession
import com.fatimagames.app.feature.games.jigsaw.domain.PhotoLoader
import com.fatimagames.app.feature.games.jigsaw.domain.PieceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface JigsawUiState {
    data object Loading : JigsawUiState
    data class Playing(
        val board: JigsawBoard,
        val states: List<PieceState>,
        val elapsedMs: Long,
        val completed: Boolean,
        val justSnappedPieceIds: Set<Int> = emptySet(),
    ) : JigsawUiState
    data class Error(val message: String) : JigsawUiState
}

@HiltViewModel
class JigsawViewModel @Inject constructor(
    private val photoLoader: PhotoLoader,
    private val session: JigsawSession,
    private val recordRepo: RecordRepository,
    private val stateRepo: GameStateRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow<JigsawUiState>(JigsawUiState.Loading)
    val uiState: StateFlow<JigsawUiState> = _uiState.asStateFlow()

    private var engine: JigsawEngine? = null
    private var startTime = 0L
    private var pieceCount: Int = 24

    fun initialize(photoId: Long, pieceCount: Int, viewportW: Int, viewportH: Int) {
        if (viewportW <= 0 || viewportH <= 0) return
        if (engine != null) return
        this.pieceCount = pieceCount
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                val uri = session.pendingPhotoUri
                photoLoader.loadFromUri(uri) ?: photoLoader.generateDefaultLandscape()
            }
            val board = withContext(Dispatchers.Default) {
                JigsawBoardBuilder.build(
                    sourceBitmap = bitmap,
                    targetPieces = pieceCount,
                    viewportWidthPx = viewportW,
                    viewportHeightPx = viewportH,
                )
            }
            engine = JigsawEngine(board)
            startTime = System.currentTimeMillis()
            _uiState.value = JigsawUiState.Playing(
                board = board,
                states = engine!!.states,
                elapsedMs = 0,
                completed = false,
            )
        }
    }

    fun onPieceDrag(pieceId: Int, dx: Float, dy: Float) {
        val eng = engine ?: return
        eng.moveBy(pieceId, dx, dy)
        publishPlaying(completed = false)
    }

    fun onPieceReleased(pieceId: Int) {
        val eng = engine ?: return
        val snappedIds = mutableSetOf<Int>()
        // Tenta snap em cadeia
        var didSnap = true
        while (didSnap) {
            didSnap = eng.trySnap(pieceId)
            if (didSnap) {
                // marcar todas as peças do grupo do pieceId como recém-encaixadas
                val groupId = eng.stateOf(pieceId)?.groupId
                if (groupId != null) {
                    eng.states.filter { it.groupId == groupId }.forEach { snappedIds.add(it.pieceId) }
                }
            }
        }
        val done = eng.isComplete()
        val cur = (_uiState.value as? JigsawUiState.Playing) ?: return
        _uiState.value = cur.copy(
            states = eng.states,
            elapsedMs = System.currentTimeMillis() - startTime,
            completed = done,
            justSnappedPieceIds = snappedIds,
        )
        if (done) finishGame()
    }

    private fun publishPlaying(completed: Boolean) {
        val eng = engine ?: return
        val cur = _uiState.value as? JigsawUiState.Playing ?: return
        _uiState.value = cur.copy(
            states = eng.states,
            elapsedMs = System.currentTimeMillis() - startTime,
            completed = completed,
            justSnappedPieceIds = emptySet(),
        )
    }

    private fun finishGame() {
        viewModelScope.launch {
            val duration = System.currentTimeMillis() - startTime
            recordRepo.save(
                GameRecord(
                    id = 0,
                    gameType = GameType.JIGSAW,
                    score = null,
                    durationMs = duration,
                    difficulty = "${pieceCount}P",
                    finishedAt = System.currentTimeMillis(),
                )
            )
            stateRepo.clearSnapshot(GameType.JIGSAW)
        }
    }
}
