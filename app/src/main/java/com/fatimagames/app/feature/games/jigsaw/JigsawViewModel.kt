package com.fatimagames.app.feature.games.jigsaw

import android.content.Context
import android.graphics.BitmapFactory
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
import com.fatimagames.app.feature.games.jigsaw.domain.PieceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    ) : JigsawUiState
    data class Error(val message: String) : JigsawUiState
}

@HiltViewModel
class JigsawViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
                val opts = BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }
                runCatching {
                    context.assets.open("sample.jpg").use { BitmapFactory.decodeStream(it, null, opts) }
                }.getOrNull() ?: generatePlaceholder(800, 600)
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
        while (eng.trySnap(pieceId)) {
            // continua tentando até não haver mais snaps em cadeia
        }
        val done = eng.isComplete()
        publishPlaying(completed = done)
        if (done) finishGame()
    }

    private fun publishPlaying(completed: Boolean) {
        val eng = engine ?: return
        val cur = _uiState.value as? JigsawUiState.Playing ?: return
        _uiState.value = cur.copy(
            states = eng.states,
            elapsedMs = System.currentTimeMillis() - startTime,
            completed = completed,
        )
    }

    private fun finishGame() {
        val eng = engine ?: return
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

    private fun generatePlaceholder(w: Int, h: Int): android.graphics.Bitmap {
        // Gradiente de cores quentes/frias para fallback sem asset
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint()
        for (y in 0 until h) {
            val t = y.toFloat() / h
            val r = (0x7A + t * (0xC9 - 0x7A)).toInt()
            val g = (0x9B + t * (0x7B - 0x9B)).toInt()
            val b = (0x7E + t * (0x5C - 0x7E)).toInt()
            paint.color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            canvas.drawRect(0f, y.toFloat(), w.toFloat(), (y + 1).toFloat(), paint)
        }
        return bmp
    }
}
