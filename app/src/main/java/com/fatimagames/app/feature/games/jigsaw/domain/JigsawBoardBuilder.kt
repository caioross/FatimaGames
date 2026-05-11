package com.fatimagames.app.feature.games.jigsaw.domain

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.random.Random

/**
 * Constrói um [JigsawBoard] a partir de um bitmap e contagem-alvo de peças.
 * Operação pesada — chamar em Dispatchers.Default.
 */
object JigsawBoardBuilder {

    /**
     * @param sourceBitmap imagem original já com EXIF rotation aplicada
     * @param targetPieces 12, 24, 48, 100
     * @param viewportWidthPx largura da área onde as peças serão exibidas
     * @param viewportHeightPx altura idem
     * @param seed para reprodutibilidade
     */
    fun build(
        sourceBitmap: Bitmap,
        targetPieces: Int,
        viewportWidthPx: Int,
        viewportHeightPx: Int,
        seed: Long = System.currentTimeMillis(),
    ): JigsawBoard {
        val imageAspect = sourceBitmap.width.toFloat() / sourceBitmap.height
        val (cols, rows) = JigsawSlicer.chooseGrid(targetPieces, imageAspect)

        val maxBoardW = (viewportWidthPx * 0.7f).toInt()
        val maxBoardH = (viewportHeightPx * 0.55f).toInt()
        val cellByW = maxBoardW / cols
        val cellByH = maxBoardH / rows
        val cellSize = minOf(cellByW, cellByH).coerceAtLeast(60)
        val knobInset = JigsawSlicer.knobInsetForCell(cellSize)

        // Crop/resize source para colcols*cellSize × rows*cellSize
        val needW = cols * cellSize
        val needH = rows * cellSize
        val cropped = cropCenter(sourceBitmap, needW, needH)

        val pieces = JigsawSlicer.assignEdges(rows, cols, seed)

        val bitmaps = HashMap<Int, ImageBitmap>(pieces.size)
        for (piece in pieces) {
            bitmaps[piece.id] = JigsawSlicer.renderPieceBitmap(
                sourceBitmap = cropped,
                piece = piece,
                cellSize = cellSize,
                knobInset = knobInset,
                seedForVariation = seed,
            )
        }

        // Distribui peças soltas aleatoriamente nas margens
        val rng = Random(seed)
        val states = pieces.map { def ->
            val xRange = 0..(viewportWidthPx - cellSize - 2 * knobInset)
            val yRange = (maxBoardH + knobInset)..(viewportHeightPx - cellSize - 2 * knobInset).coerceAtLeast(maxBoardH + knobInset + 1)
            PieceState(
                pieceId = def.id,
                xPx = rng.nextInt(xRange.first, (xRange.last + 1).coerceAtLeast(xRange.first + 1)).toFloat(),
                yPx = rng.nextInt(yRange.first, (yRange.last + 1).coerceAtLeast(yRange.first + 1)).toFloat(),
                groupId = def.id,
            )
        }

        return JigsawBoard(
            rows = rows,
            cols = cols,
            imageWidthPx = needW,
            imageHeightPx = needH,
            cellSizePx = cellSize,
            knobInsetPx = knobInset,
            seed = seed,
            pieces = pieces,
            pieceBitmaps = bitmaps,
            initialStates = states,
        )
    }

    private fun cropCenter(src: Bitmap, w: Int, h: Int): Bitmap {
        val srcAspect = src.width.toFloat() / src.height
        val dstAspect = w.toFloat() / h

        val (cropW, cropH) = if (srcAspect > dstAspect) {
            (src.height * dstAspect).toInt() to src.height
        } else {
            src.width to (src.width / dstAspect).toInt()
        }
        val x = (src.width - cropW) / 2
        val y = (src.height - cropH) / 2
        val cropped = Bitmap.createBitmap(src, x, y, cropW, cropH)
        return if (cropped.width == w && cropped.height == h) {
            cropped
        } else {
            Bitmap.createScaledBitmap(cropped, w, h, true)
        }
    }
}
