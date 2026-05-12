package com.fatimagames.app.feature.games.jigsaw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.WinOverlay
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawBoard
import com.fatimagames.app.feature.games.jigsaw.domain.PieceState
import kotlinx.coroutines.launch

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
                    .background(theme.color.bgTubeWell)
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
                            justSnappedIds = s.justSnappedPieceIds,
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
    justSnappedIds: Set<Int>,
    onDrag: (id: Int, dx: Float, dy: Float) -> Unit,
    onRelease: (id: Int) -> Unit,
) {
    var dragId by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Animação de "pulse" para peças que acabaram de encaixar
    val snapPulse = remember { Animatable(0f) }
    LaunchedEffect(justSnappedIds.hashCode()) {
        if (justSnappedIds.isNotEmpty()) {
            snapPulse.snapTo(1f)
            snapPulse.animateTo(0f, animationSpec = tween(durationMillis = 350))
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(board.seed) {
                detectDragGestures(
                    onDragStart = { offset ->
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
        // Para arrastar grupo no topo, ordenamos: peças sem grupo > peças do grupo arrastado por último
        val byId = states.associateBy { it.pieceId }
        val draggingGroupId = dragId?.let { byId[it]?.groupId }
        val orderedPieces = board.pieces.sortedBy { piece ->
            val s = byId[piece.id]
            when {
                s == null -> -1
                s.groupId == draggingGroupId -> 1
                else -> 0
            }
        }

        for (piece in orderedPieces) {
            val s = byId[piece.id] ?: continue
            val bmp = board.pieceBitmaps[piece.id] ?: continue
            val isDragging = piece.id == dragId || (draggingGroupId != null && s.groupId == draggingGroupId)
            val isJustSnapped = piece.id in justSnappedIds

            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.save()

                if (isDragging) {
                    // Sombra abaixo da peça arrastada
                    val shadowPaint = android.graphics.Paint().apply {
                        color = 0x55000000.toInt()
                        maskFilter = android.graphics.BlurMaskFilter(
                            12f,
                            android.graphics.BlurMaskFilter.Blur.NORMAL,
                        )
                    }
                    nativeCanvas.drawBitmap(
                        bmp.asAndroidBitmap().extractAlpha(),
                        s.xPx + 4f,
                        s.yPx + 8f,
                        shadowPaint,
                    )
                }

                nativeCanvas.drawBitmap(
                    bmp.asAndroidBitmap(),
                    s.xPx,
                    s.yPx,
                    null,
                )

                // Glow ao acabar de encaixar
                if (isJustSnapped && snapPulse.value > 0f) {
                    val glowPaint = android.graphics.Paint().apply {
                        color = 0xFFD4A24A.toInt()
                        alpha = (snapPulse.value * 110).toInt()
                        maskFilter = android.graphics.BlurMaskFilter(
                            8f,
                            android.graphics.BlurMaskFilter.Blur.OUTER,
                        )
                    }
                    nativeCanvas.drawBitmap(
                        bmp.asAndroidBitmap().extractAlpha(),
                        s.xPx,
                        s.yPx,
                        glowPaint,
                    )
                }

                nativeCanvas.restore()
            }
        }
    }
}

private fun hitTest(states: List<PieceState>, board: JigsawBoard, offset: Offset): Int? {
    val size = board.cellSizePx + 2 * board.knobInsetPx
    // Em ordem reversa pega peça do topo
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
