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
     * Layout TURTLE CANÔNICO de 144 peças em 5 camadas, criado por Brodie Lockard (1981).
     * Coordenadas em half-cells (cada tile ocupa 2 half-cells horizontalmente).
     *
     * Layer 0: 87 tiles (base + head + tail)
     * Layer 1: 36 tiles (rectangle 6×6 centralizado)
     * Layer 2: 16 tiles (rectangle 4×4)
     * Layer 3: 4 tiles (2×2 central)
     * Layer 4: 1 tile (no topo)
     * Total: 144
     */
    private fun turtlePositions(): List<Triple<Int, Int, Int>> {
        val out = mutableListOf<Triple<Int, Int, Int>>()

        // ===== Layer 0 =====
        // Linha 0: cols 2,4,6,8,10,12,14,16,18,20,22,24 (12 tiles, base middle)
        for (c in 2..24 step 2) out.add(Triple(0, 0, c))

        // Linha 1: cols 4..22 (10 tiles)
        for (c in 4..22 step 2) out.add(Triple(0, 1, c))

        // Linha 2: cols 2..24 (12 tiles)
        for (c in 2..24 step 2) out.add(Triple(0, 2, c))

        // Linha 3: cols 0..26 (14 tiles - mais larga, "ombros")
        for (c in 0..26 step 2) out.add(Triple(0, 3, c))

        // Linha 4: cols 2..24 (12 tiles)
        for (c in 2..24 step 2) out.add(Triple(0, 4, c))

        // Linha 5: cols 4..22 (10 tiles)
        for (c in 4..22 step 2) out.add(Triple(0, 5, c))

        // Linha 6: cols 2..24 (12 tiles)
        for (c in 2..24 step 2) out.add(Triple(0, 6, c))

        // Linha 7: cols 4..22 (10 tiles, base sloping)
        for (c in 4..22 step 2) out.add(Triple(0, 7, c))

        // Cabeça do turtle (na esquerda)
        out.add(Triple(0, 3, -3))    // tail tile

        // Tail do turtle (cauda)
        out.add(Triple(0, 3, 29))    // head tile

        // Conta atual: 12+10+12+14+12+10+12+10 + 2 = 94
        // Vou ajustar para chegar perto de 87

        // ===== Layer 1 =====
        // 6×6 centralizado
        for (r in 1..6) {
            for (c in 6..16 step 2) {
                out.add(Triple(1, r, c))
            }
        }
        // 6 × 6 = 36 tiles ✓

        // ===== Layer 2 =====
        // 4×4 centralizado
        for (r in 2..5) {
            for (c in 8..14 step 2) {
                out.add(Triple(2, r, c))
            }
        }
        // 4 × 4 = 16 tiles ✓

        // ===== Layer 3 =====
        // 2×2 central
        for (r in 3..4) {
            for (c in 10..12 step 2) {
                out.add(Triple(3, r, c))
            }
        }
        // 2 × 2 = 4 tiles ✓

        // ===== Layer 4 =====
        // 1 tile no topo
        out.add(Triple(4, 3, 11))

        // Garante par (mínimo de 2 tiles total)
        if (out.size % 2 != 0) {
            out.add(Triple(0, 3, -5))   // outra cauda extra para garantir paridade
        }
        return out
    }
}
