package com.fatimagames.app.feature.games.jigsaw.domain

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

enum class EdgeType { FLAT, TAB, SLOT }

enum class Side { TOP, RIGHT, BOTTOM, LEFT }

/** Definição estática de uma peça (não muda durante a partida). */
data class PieceDefinition(
    val id: Int,
    val gridRow: Int,
    val gridCol: Int,
    val edges: Map<Side, EdgeType>,
)

/** Estado dinâmico de uma peça em jogo. */
data class PieceState(
    val pieceId: Int,
    val xPx: Float,
    val yPx: Float,
    val groupId: Int,
)

data class JigsawBoard(
    val rows: Int,
    val cols: Int,
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val cellSizePx: Int,
    val knobInsetPx: Int,
    val seed: Long,
    val pieces: List<PieceDefinition>,
    val pieceBitmaps: Map<Int, ImageBitmap>,
    val initialStates: List<PieceState>,
)

@Serializable
data class JigsawSnapshot(
    val photoId: Long,
    val pieceCount: Int,
    val seed: Long,
    val pieces: List<PieceSnap>,
    val elapsedMs: Long,
)

@Serializable
data class PieceSnap(
    val pieceId: Int,
    val xPx: Float,
    val yPx: Float,
    val groupId: Int,
)
