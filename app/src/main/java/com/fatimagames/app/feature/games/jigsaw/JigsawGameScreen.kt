package com.fatimagames.app.feature.games.jigsaw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.WinOverlay
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawBoard
import com.fatimagames.app.feature.games.jigsaw.domain.PieceState

@Composable
fun JigsawGameScreen(
    photoId: Long,
    pieceCount: Int,
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: JigsawViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        GameTopBar(
            title = "Quebra-cabeça",
            subtitle = when (val s = state) {
                is JigsawUiState.Playing -> {
                    val total = s.board.pieces.size
                    val placed = total - distinctGroups(s.states)
                    "$placed / $total"
                }
                else -> null
            },
            onBackClick = onBack,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        viewModel.initialize(
                            photoId = photoId,
                            pieceCount = pieceCount,
                            viewportW = size.width,
                            viewportH = size.height,
                        )
                    },
            ) {
                when (val s = state) {
                    is JigsawUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Text(
                                "Preparando peças…",
                                color = theme.color.textSecondary,
                                style = typo.bodyLg,
                            )
                        }
                    }
                    is JigsawUiState.Playing -> {
                        JigsawCanvas(
                            board = s.board,
                            states = s.states,
                            onDrag = { id, dx, dy -> viewModel.onPieceDrag(id, dx, dy) },
                            onRelease = { id -> viewModel.onPieceReleased(id) },
                        )
                        if (s.completed) {
                            WinOverlay(
                                timeLabel = formatTime(s.elapsedMs),
                                secondaryLabel = "Peças",
                                secondaryValue = s.board.pieces.size.toString(),
                                onPlayAgain = onBack,
                                onHome = onHome,
                            )
                        }
                    }
                    is JigsawUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Text(
                                s.message,
                                color = theme.color.feedbackError,
                                style = typo.bodyLg,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JigsawCanvas(
    board: JigsawBoard,
    states: List<PieceState>,
    onDrag: (id: Int, dx: Float, dy: Float) -> Unit,
    onRelease: (id: Int) -> Unit,
) {
    var dragId by remember { mutableStateOf<Int?>(null) }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(board.seed) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Hit-test em ordem reversa (topo primeiro)
                        val hit = hitTest(states, board, offset)
                        dragId = hit
                    },
                    onDragEnd = {
                        dragId?.let { onRelease(it) }
                        dragId = null
                    },
                    onDragCancel = {
                        dragId?.let { onRelease(it) }
                        dragId = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragId?.let { onDrag(it, dragAmount.x, dragAmount.y) }
                    },
                )
            },
    ) {
        val byId = states.associateBy { it.pieceId }
        for (piece in board.pieces) {
            val s = byId[piece.id] ?: continue
            val bmp = board.pieceBitmaps[piece.id] ?: continue
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawBitmap(
                    bmp.asAndroidBitmap(),
                    s.xPx,
                    s.yPx,
                    null,
                )
            }
        }
    }
}

private fun hitTest(states: List<PieceState>, board: JigsawBoard, offset: Offset): Int? {
    val size = board.cellSizePx + 2 * board.knobInsetPx
    // procura em ordem reversa para pegar peça do topo
    for (s in states.asReversed()) {
        if (offset.x in s.xPx..(s.xPx + size) && offset.y in s.yPx..(s.yPx + size)) {
            return s.pieceId
        }
    }
    return null
}

private fun distinctGroups(states: List<PieceState>): Int =
    states.map { it.groupId }.toSet().size

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}
