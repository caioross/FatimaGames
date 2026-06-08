package com.fatimagames.app.feature.games.jigsaw

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameBackGuard
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.TutorialContent
import com.fatimagames.app.core.ui.TutorialFirstTime
import com.fatimagames.app.core.ui.WinOverlay
import com.fatimagames.app.feature.games.jigsaw.domain.JigsawBoard
import com.fatimagames.app.feature.games.jigsaw.domain.PieceState
import kotlin.math.abs

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

    val hasProgress = (state as? JigsawUiState.Playing)?.let { p ->
        !p.completed && distinctGroups(p.states) < p.board.pieces.size
    } ?: false

    // FIX J13: recycle bitmaps ao sair da tela para liberar memória
    DisposableEffect(Unit) {
        onDispose {
            val playing = state as? JigsawUiState.Playing
            playing?.board?.pieceBitmaps?.values?.forEach { bmp ->
                runCatching { bmp.asAndroidBitmap().recycle() }
            }
        }
    }

    GameBackGuard(hasProgress = hasProgress, onConfirmedExit = onBack) {
    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
        GameTopBar(
            title = "Quebra-cabeça",
            subtitle = when (val s = state) {
                is JigsawUiState.Playing -> {
                    val total = s.board.pieces.size
                    // FIX V112: contar peças que pertencem a grupos compostos (encaixadas)
                    val placed = s.states.groupBy { it.groupId }.values
                        .filter { it.size > 1 }
                        .sumOf { it.size }
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
                        InteractiveJigsawCanvas(
                            board = s.board,
                            states = s.states,
                            justSnappedIds = s.justSnappedPieceIds,
                            onDragPiece = { id, dx, dy -> viewModel.onPieceDrag(id, dx, dy) },
                            onRotatePiece = { id, deg -> viewModel.onPieceRotate(id, deg) },
                            onReleasePiece = { id -> viewModel.onPieceReleased(id) },
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
    TutorialFirstTime(name = "jigsaw", steps = TutorialContent.jigsaw)
    }
}

/**
 * Canvas com gestos avançados:
 * - 1 dedo arrastando peça → move peça
 * - 2 dedos com primeiro NÃO numa peça → zoom + pan do board
 * - 2 dedos com primeiro numa peça → rotaciona peça
 */
@Composable
private fun InteractiveJigsawCanvas(
    board: JigsawBoard,
    states: List<PieceState>,
    justSnappedIds: Set<Int>,
    onDragPiece: (id: Int, dx: Float, dy: Float) -> Unit,
    onRotatePiece: (id: Int, deg: Float) -> Unit,
    onReleasePiece: (id: Int) -> Unit,
) {
    // Estado do board (zoom + pan)
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Estado do drag corrente
    var dragId by remember { mutableStateOf<Int?>(null) }

    // Sempre lê o estado mais recente de states/board nos handlers
    val statesState = rememberUpdatedState(states)
    val boardState = rememberUpdatedState(board)

    // Pulse glow para peças recém-encaixadas
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = panX
                translationY = panY
            }
            .pointerInput(board.seed) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = true)
                    // Hit test no espaço LOCAL do canvas (já está corrigido pela transform)
                    val localOffset = firstDown.position
                    val hitId = hitTest(statesState.value, boardState.value, localOffset)
                    var currentId = hitId
                    dragId = currentId
                    var didSecondPointer = false
                    val pointerPositions = mutableMapOf<PointerId, Offset>()
                    pointerPositions[firstDown.id] = firstDown.position

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            // Final do gesto
                            currentId?.let { onReleasePiece(it) }
                            dragId = null
                            break
                        }

                        if (pressed.size >= 2) {
                            // Multi-finger: zoom + rotação
                            didSecondPointer = true
                            val zoom = event.calculateZoom()
                            val rotation = event.calculateRotation()
                            val pan = event.calculatePan()

                            if (currentId != null) {
                                // Pinça giratória sobre a peça → rotaciona a peça
                                if (abs(rotation) > 0.05f) {
                                    onRotatePiece(currentId!!, rotation)
                                }
                                // Aceitamos pequena translação + zoom acompanha
                                if (abs(pan.x) > 0.5f || abs(pan.y) > 0.5f) {
                                    onDragPiece(currentId!!, pan.x, pan.y)
                                }
                            } else {
                                // Sem peça → zoom + pan do board
                                if (zoom != 1f) {
                                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                                }
                                panX += pan.x
                                panY += pan.y
                            }
                            pressed.forEach { it.consume() }
                        } else {
                            // 1 finger: drag da peça (se houver)
                            val change = pressed.first()
                            val delta = change.positionChange()
                            if (currentId != null && !didSecondPointer) {
                                onDragPiece(currentId!!, delta.x, delta.y)
                                change.consume()
                            } else if (didSecondPointer) {
                                // Após interação de 2 dedos, voltou para 1 — encerrar
                                // (não deixa drag desalinhado)
                            }
                        }
                    }
                }
            },
    ) {
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
                // Aplicar rotação da peça (em torno do centro do bitmap)
                if (s.rotationDeg != 0f) {
                    val cx = s.xPx + bmp.width / 2f
                    val cy = s.yPx + bmp.height / 2f
                    nativeCanvas.rotate(s.rotationDeg, cx, cy)
                }

                if (isDragging) {
                    val shadowPaint = android.graphics.Paint().apply {
                        color = 0x66000000.toInt()
                        maskFilter = android.graphics.BlurMaskFilter(
                            14f,
                            android.graphics.BlurMaskFilter.Blur.NORMAL,
                        )
                    }
                    nativeCanvas.drawBitmap(
                        bmp.asAndroidBitmap().extractAlpha(),
                        s.xPx + 5f,
                        s.yPx + 10f,
                        shadowPaint,
                    )
                }

                nativeCanvas.drawBitmap(
                    bmp.asAndroidBitmap(),
                    s.xPx,
                    s.yPx,
                    null,
                )

                if (isJustSnapped && snapPulse.value > 0f) {
                    val glowPaint = android.graphics.Paint().apply {
                        color = 0xFFD4A24A.toInt()
                        alpha = (snapPulse.value * 130).toInt().coerceIn(0, 255)
                        maskFilter = android.graphics.BlurMaskFilter(
                            10f,
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

/**
 * Hit-test pelo path real (alpha do bitmap), não bbox.
 */
private fun hitTest(states: List<PieceState>, board: JigsawBoard, offset: Offset): Int? {
    val size = board.cellSizePx + 2 * board.knobInsetPx
    for (s in states.asReversed()) {
        val localX = (offset.x - s.xPx).toInt()
        val localY = (offset.y - s.yPx).toInt()
        if (localX !in 0 until size || localY !in 0 until size) continue
        val bmp = board.pieceBitmaps[s.pieceId]?.asAndroidBitmap() ?: continue
        if (localX >= bmp.width || localY >= bmp.height) continue
        val pixel = runCatching { bmp.getPixel(localX, localY) }.getOrNull() ?: continue
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha > 32) return s.pieceId
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
