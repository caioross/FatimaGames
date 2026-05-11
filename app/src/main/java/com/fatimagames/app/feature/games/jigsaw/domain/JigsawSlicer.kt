package com.fatimagames.app.feature.games.jigsaw.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Slicer canônico do jigsaw.
 *
 * - Decide a grade (cols × rows) com células aproximadamente quadradas
 * - Atribui tipos de aresta (TAB/SLOT) garantindo encaixe entre vizinhas
 * - Constrói o Path da peça com curvas cubic Bézier formando o "knob"
 * - Recorta o bitmap para cada peça
 *
 * Inspirado em:
 *  - headbreaker (JS) — modelo Tab/Slot
 *  - Shamim Akhtar Unity tutorial — matemática Bézier
 *  - piecemaker (Python) — proporções padrão
 */
object JigsawSlicer {

    private const val KNOB_INSET_RATIO = 0.22f   // ~22% do cell size
    private const val EDGE_VARIATION = 0.06f     // ±6% de variação no knob

    fun chooseGrid(targetPieces: Int, imageAspect: Float): Pair<Int, Int> {
        var best: Pair<Int, Int>? = null
        var bestScore = Float.MAX_VALUE
        for (cols in 2..targetPieces) {
            val rows = (targetPieces.toFloat() / cols).toInt()
            if (rows < 2) continue
            val piecesActual = cols * rows
            val aspectError = abs(((cols.toFloat() / rows) / imageAspect) - 1f)
            val countError = abs(piecesActual - targetPieces).toFloat() / targetPieces
            val score = aspectError * 2f + countError
            if (score < bestScore) {
                bestScore = score
                best = cols to rows
            }
        }
        return best ?: (4 to 3)
    }

    fun assignEdges(rows: Int, cols: Int, seed: Long): List<PieceDefinition> {
        val rng = Random(seed)
        val edges = Array(rows) { Array(cols) { mutableMapOf<Side, EdgeType>() } }

        for (r in 0 until rows) for (c in 0 until cols) {
            edges[r][c][Side.TOP] = when {
                r == 0 -> EdgeType.FLAT
                else -> opposite(edges[r - 1][c][Side.BOTTOM]!!)
            }
            edges[r][c][Side.LEFT] = when {
                c == 0 -> EdgeType.FLAT
                else -> opposite(edges[r][c - 1][Side.RIGHT]!!)
            }
            edges[r][c][Side.BOTTOM] = when {
                r == rows - 1 -> EdgeType.FLAT
                rng.nextBoolean() -> EdgeType.TAB
                else -> EdgeType.SLOT
            }
            edges[r][c][Side.RIGHT] = when {
                c == cols - 1 -> EdgeType.FLAT
                rng.nextBoolean() -> EdgeType.TAB
                else -> EdgeType.SLOT
            }
        }

        val out = mutableListOf<PieceDefinition>()
        var id = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            out.add(
                PieceDefinition(
                    id = id++,
                    gridRow = r,
                    gridCol = c,
                    edges = edges[r][c].toMap(),
                )
            )
        }
        return out
    }

    private fun opposite(e: EdgeType): EdgeType = when (e) {
        EdgeType.TAB -> EdgeType.SLOT
        EdgeType.SLOT -> EdgeType.TAB
        EdgeType.FLAT -> EdgeType.FLAT
    }

    /**
     * Constrói o Path do contorno da peça em coordenadas locais do bitmap.
     * Origem do bitmap é (0,0); o miolo da peça começa em (knobInset, knobInset).
     */
    fun buildPiecePath(
        piece: PieceDefinition,
        cellSize: Float,
        knobInset: Float,
        seedForVariation: Long,
    ): Path {
        val rng = Random(seedForVariation xor (piece.gridRow * 73L + piece.gridCol * 13L))
        val path = Path()

        val left = knobInset
        val top = knobInset
        val right = knobInset + cellSize
        val bottom = knobInset + cellSize

        // Começa no canto superior-esquerdo do miolo
        path.moveTo(left, top)

        // TOP — esquerda-para-direita
        appendEdge(
            path,
            from = Offset(left, top),
            to = Offset(right, top),
            type = piece.edges[Side.TOP]!!,
            knobInset = knobInset,
            outward = -1f,
            rng = rng,
        )
        // RIGHT — cima-para-baixo
        appendEdge(
            path,
            from = Offset(right, top),
            to = Offset(right, bottom),
            type = piece.edges[Side.RIGHT]!!,
            knobInset = knobInset,
            outward = 1f,
            rng = rng,
        )
        // BOTTOM — direita-para-esquerda
        appendEdge(
            path,
            from = Offset(right, bottom),
            to = Offset(left, bottom),
            type = piece.edges[Side.BOTTOM]!!,
            knobInset = knobInset,
            outward = 1f,
            rng = rng,
        )
        // LEFT — baixo-para-cima
        appendEdge(
            path,
            from = Offset(left, bottom),
            to = Offset(left, top),
            type = piece.edges[Side.LEFT]!!,
            knobInset = knobInset,
            outward = -1f,
            rng = rng,
        )

        path.close()
        return path
    }

    /**
     * Acrescenta a curva de uma aresta ao Path.
     *
     * Para TAB/SLOT, monta 3 cubic Béziers (entrada, topo, saída) usando o
     * vetor normal à aresta para projetar o knob para fora ou para dentro.
     */
    private fun appendEdge(
        path: Path,
        from: Offset,
        to: Offset,
        type: EdgeType,
        knobInset: Float,
        outward: Float,
        rng: Random,
    ) {
        if (type == EdgeType.FLAT) {
            path.lineTo(to.x, to.y)
            return
        }
        val sign = if (type == EdgeType.TAB) outward else -outward
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = hypot(dx, dy)
        // Vetor normal (perpendicular) ao segmento, normalizado
        val nx = -dy / len
        val ny = dx / len

        // Variação aleatória sutil no formato do knob
        val v1 = 1f + (rng.nextFloat() - 0.5f) * EDGE_VARIATION
        val v2 = 1f + (rng.nextFloat() - 0.5f) * EDGE_VARIATION

        fun pt(t: Float, off: Float): Pair<Float, Float> {
            val px = from.x + dx * t + nx * off * sign
            val py = from.y + dy * t + ny * off * sign
            return px to py
        }

        val k = knobInset
        val (a1x, a1y) = pt(0.30f, 0f)
        val (a2x, a2y) = pt(0.35f, k * 0.40f * v1)
        val (a3x, a3y) = pt(0.40f, k * 0.80f * v1)

        path.cubicTo(a1x, a1y, a2x, a2y, a3x, a3y)

        val (b1x, b1y) = pt(0.45f, k * 1.15f * v2)
        val (b2x, b2y) = pt(0.55f, k * 1.15f * v2)
        val (b3x, b3y) = pt(0.60f, k * 0.80f * v2)

        path.cubicTo(b1x, b1y, b2x, b2y, b3x, b3y)

        val (c1x, c1y) = pt(0.65f, k * 0.40f * v1)
        val (c2x, c2y) = pt(0.70f, 0f)
        path.cubicTo(c1x, c1y, c2x, c2y, to.x, to.y)
    }

    /**
     * Recorta uma peça do bitmap-fonte usando o path como máscara.
     * O bitmap retornado tem dimensões (cellSize + 2*knobInset)².
     */
    fun renderPieceBitmap(
        sourceBitmap: Bitmap,
        piece: PieceDefinition,
        cellSize: Int,
        knobInset: Int,
        seedForVariation: Long,
    ): androidx.compose.ui.graphics.ImageBitmap {
        val w = cellSize + 2 * knobInset
        val h = cellSize + 2 * knobInset
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Build path in float coords
        val path = buildPiecePath(piece, cellSize.toFloat(), knobInset.toFloat(), seedForVariation)
        val androidPath = path.asAndroidPath()

        // Clip & draw the source region
        canvas.save()
        canvas.clipPath(androidPath)
        val srcX = piece.gridCol * cellSize - knobInset
        val srcY = piece.gridRow * cellSize - knobInset
        val srcRect = Rect(
            max(0, srcX),
            max(0, srcY),
            min(sourceBitmap.width, srcX + w),
            min(sourceBitmap.height, srcY + h),
        )
        val dstRect = Rect(
            if (srcX < 0) -srcX else 0,
            if (srcY < 0) -srcY else 0,
            if (srcX < 0) -srcX + srcRect.width() else srcRect.width(),
            if (srcY < 0) -srcY + srcRect.height() else srcRect.height(),
        )
        canvas.drawBitmap(sourceBitmap, srcRect, dstRect, null)
        canvas.restore()

        // Inner shadow / border outline
        val stroke = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = 0x55000000.toInt()
            isAntiAlias = true
        }
        canvas.drawPath(androidPath, stroke)

        return out.asImageBitmap()
    }

    fun knobInsetForCell(cellSize: Int): Int =
        (cellSize * KNOB_INSET_RATIO).toInt().coerceAtLeast(8)
}
