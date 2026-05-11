package com.fatimagames.app.feature.games.jigsaw.domain

import kotlin.math.abs

/**
 * Lógica de jogo do jigsaw separada da UI.
 * - Mantém estado das peças
 * - Faz snap entre peças vizinhas
 * - Detecta vitória
 */
class JigsawEngine(
    val board: JigsawBoard,
    initialStates: List<PieceState> = board.initialStates,
    val snapTolerancePx: Float = (board.cellSizePx * 0.18f),
) {
    private val statesById: MutableMap<Int, PieceState> =
        initialStates.associateBy { it.pieceId }.toMutableMap()

    val states: List<PieceState> get() = statesById.values.toList()

    fun stateOf(pieceId: Int): PieceState? = statesById[pieceId]

    fun definitionOf(pieceId: Int): PieceDefinition? =
        board.pieces.firstOrNull { it.id == pieceId }

    /** Move uma peça (e todas do grupo) por delta. Retorna lista de IDs movidos. */
    fun moveBy(pieceId: Int, dx: Float, dy: Float): List<Int> {
        val state = statesById[pieceId] ?: return emptyList()
        val groupId = state.groupId
        val moved = mutableListOf<Int>()
        for ((id, s) in statesById) {
            if (s.groupId == groupId) {
                statesById[id] = s.copy(xPx = s.xPx + dx, yPx = s.yPx + dy)
                moved.add(id)
            }
        }
        return moved
    }

    /**
     * Após soltar, tenta snap entre a peça (ou seu grupo) e vizinhas válidas.
     * Retorna `true` se algo encaixou.
     */
    fun trySnap(pieceId: Int): Boolean {
        val state = statesById[pieceId] ?: return false
        val def = definitionOf(pieceId) ?: return false
        val cell = board.cellSizePx.toFloat()
        val groupId = state.groupId

        // Para cada peça do mesmo grupo, procurar vizinhas em outros grupos
        val groupPieces = statesById.values.filter { it.groupId == groupId }
        for (gp in groupPieces) {
            val gpDef = definitionOf(gp.pieceId) ?: continue
            val neighbors = board.pieces.filter { other ->
                other.id != gpDef.id &&
                    (
                        (other.gridRow == gpDef.gridRow && abs(other.gridCol - gpDef.gridCol) == 1) ||
                            (other.gridCol == gpDef.gridCol && abs(other.gridRow - gpDef.gridRow) == 1)
                        )
            }
            for (nDef in neighbors) {
                val nState = statesById[nDef.id] ?: continue
                if (nState.groupId == groupId) continue
                val expectedDx = (nDef.gridCol - gpDef.gridCol) * cell
                val expectedDy = (nDef.gridRow - gpDef.gridRow) * cell
                val actualDx = nState.xPx - gp.xPx
                val actualDy = nState.yPx - gp.yPx
                if (abs(actualDx - expectedDx) < snapTolerancePx &&
                    abs(actualDy - expectedDy) < snapTolerancePx
                ) {
                    // Encaixar: alinhar e fundir grupos
                    val correction = expectedDx - actualDx to expectedDy - actualDy
                    val targetGroup = groupId
                    val sourceGroup = nState.groupId
                    for ((id, s) in statesById) {
                        if (s.groupId == sourceGroup) {
                            statesById[id] = s.copy(
                                xPx = s.xPx + correction.first,
                                yPx = s.yPx + correction.second,
                                groupId = targetGroup,
                            )
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    fun isComplete(): Boolean {
        val firstGroup = statesById.values.firstOrNull()?.groupId ?: return false
        return statesById.values.all { it.groupId == firstGroup }
    }
}
