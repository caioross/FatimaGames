package com.fatimagames.app.feature.games.mahjong.domain

import kotlin.math.abs
import kotlin.random.Random

/**
 * Engine do Mahjong Solitaire.
 *
 * Layout simplificado para MVP: 4 camadas, 36 peças por camada (6×6),
 * com camadas superiores menores. Total ~108 peças (subset do conjunto
 * tradicional de 144, mantendo balanço de pares).
 */
class MahjongEngine(initialTiles: List<MahjongTile>) {
    private val tiles: MutableList<MahjongTile> = initialTiles.toMutableList()
    private val removalHistory: ArrayDeque<Pair<Int, Int>> = ArrayDeque()

    var hintsUsed: Int = 0; private set
    var undosUsed: Int = 0; private set
    var reshufflesUsed: Int = 0; private set

    fun snapshot(): List<MahjongTile> = tiles.toList()

    fun isFree(tile: MahjongTile): Boolean {
        if (tile.removed) return false
        // Camada acima cobrindo?
        val above = tiles.any { t ->
            !t.removed &&
                t.layer == tile.layer + 1 &&
                abs(t.row - tile.row) <= 1 &&
                abs(t.col - tile.col) <= 1
        }
        if (above) return false
        // Lados livres?
        val sameLayer = tiles.filter { it.layer == tile.layer && !it.removed && it.id != tile.id }
        val leftBlocked = sameLayer.any { it.row == tile.row && it.col == tile.col - 2 }
        val rightBlocked = sameLayer.any { it.row == tile.row && it.col == tile.col + 2 }
        return !(leftBlocked && rightBlocked)
    }

    fun matches(a: TileFace, b: TileFace): Boolean = a.familyId == b.familyId

    /** Tenta remover par. Retorna true se removeu. */
    fun tryMatch(idA: Int, idB: Int): Boolean {
        val a = tiles.find { it.id == idA && !it.removed } ?: return false
        val b = tiles.find { it.id == idB && !it.removed } ?: return false
        if (a.id == b.id) return false
        if (!isFree(a) || !isFree(b)) return false
        if (!matches(a.face, b.face)) return false
        tiles[tiles.indexOf(a)] = a.copy(removed = true)
        tiles[tiles.indexOf(b)] = b.copy(removed = true)
        removalHistory.addLast(idA to idB)
        return true
    }

    fun undo(): Boolean {
        val (idA, idB) = removalHistory.removeLastOrNull() ?: return false
        tiles.indexOfFirst { it.id == idA }.takeIf { it >= 0 }?.let {
            tiles[it] = tiles[it].copy(removed = false)
        }
        tiles.indexOfFirst { it.id == idB }.takeIf { it >= 0 }?.let {
            tiles[it] = tiles[it].copy(removed = false)
        }
        undosUsed++
        return true
    }

    /** Retorna um par válido se houver, para usar como dica. */
    fun findHintPair(): Pair<Int, Int>? {
        val active = tiles.filter { !it.removed && isFree(it) }
        for (i in active.indices) for (j in i + 1 until active.size) {
            if (matches(active[i].face, active[j].face)) {
                return active[i].id to active[j].id
            }
        }
        return null
    }

    fun useHint(): Pair<Int, Int>? {
        val pair = findHintPair() ?: return null
        hintsUsed++
        return pair
    }

    /** Embaralha desenhos das peças restantes mantendo o layout. */
    fun reshuffle(seed: Long = System.currentTimeMillis()) {
        reshufflesUsed++
        val active = tiles.filter { !it.removed }
        val faces = active.map { it.face }.toMutableList()
        faces.shuffle(Random(seed))
        var idx = 0
        for (i in tiles.indices) {
            if (!tiles[i].removed) {
                tiles[i] = tiles[i].copy(face = faces[idx++])
            }
        }
    }

    fun isComplete(): Boolean = tiles.all { it.removed }

    fun remainingCount(): Int = tiles.count { !it.removed }
}

/** Constrói um layout MVP em "tartaruga simplificada". */
object MahjongLayoutBuilder {

    fun buildTurtleMVP(seed: Long = System.currentTimeMillis()): List<MahjongTile> {
        val positions = turtlePositions()
        require(positions.size % 2 == 0) {
            "Mahjong layout must have an even number of positions, got ${positions.size}"
        }
        val pairs = positions.size / 2
        val faces = generateBalancedDeck(pairs)
        val pairedFaces = faces.flatMap { listOf(it, it) }.toMutableList()
        pairedFaces.shuffle(Random(seed))
        return positions.mapIndexed { idx, (layer, r, c) ->
            MahjongTile(id = idx, face = pairedFaces[idx], layer = layer, row = r, col = c)
        }
    }

    /** Gera `pairs` desenhos distintos (face) balanceados. */
    private fun generateBalancedDeck(pairs: Int): List<TileFace> {
        val all = mutableListOf<TileFace>()
        (1..9).forEach { all.add(TileFace.Bamboo(it)) }
        (1..9).forEach { all.add(TileFace.Character(it)) }
        (1..9).forEach { all.add(TileFace.Circle(it)) }
        listOf("東", "南", "西", "北").forEach { all.add(TileFace.Wind(it)) }
        listOf("RED", "GREEN", "WHITE").forEach { all.add(TileFace.Dragon(it)) }
        (1..4).forEach { all.add(TileFace.Flower(it)) }
        (1..4).forEach { all.add(TileFace.Season(it)) }
        // total = 9+9+9+4+3+4+4 = 42 faces; precisamos 54 pares
        val deck = mutableListOf<TileFace>()
        var i = 0
        while (deck.size < pairs) {
            deck.add(all[i % all.size])
            i++
        }
        return deck
    }

    /**
     * Posições (layer, row, col) usando half-cell coords.
     * Layout simplificado: base 6×9 = 54 peças, depois camadas progressivamente menores.
     */
    private fun turtlePositions(): List<Triple<Int, Int, Int>> {
        val out = mutableListOf<Triple<Int, Int, Int>>()

        // Layer 0: 6 rows × 9 cols
        for (r in 0 until 6) {
            for (c in 0 until 9) {
                out.add(Triple(0, r, c * 2))
            }
        }
        // Layer 1: 4 rows × 6 cols (centralizado)
        for (r in 1..4) {
            for (c in 0 until 6) {
                out.add(Triple(1, r, (c + 1) * 2 + 1))
            }
        }
        // Layer 2: 2 rows × 3 cols
        for (r in 2..3) {
            for (c in 0 until 3) {
                out.add(Triple(2, r, (c + 2) * 2 + 2))
            }
        }
        // Layer 3: 1 tile no topo
        out.add(Triple(3, 2, 8))
        out.add(Triple(3, 3, 8))
        return out
    }
}
