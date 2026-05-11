package com.fatimagames.app.feature.games.match3.domain

import kotlin.random.Random

enum class GemType { RAIN, LEAF, BLOOM, SUN, MOON, EMBER }

/**
 * Conteúdo de uma célula. Pode ser gema normal, gema especial ou vazio.
 * Especiais detonam efeito ao serem incluídas em um match (mesma cor da gema base).
 */
sealed class CellContent {
    abstract val type: GemType?

    data class Normal(override val type: GemType) : CellContent()
    /** Limpa toda a linha quando ativada. */
    data class FlameH(override val type: GemType) : CellContent()
    /** Limpa toda a coluna. */
    data class FlameV(override val type: GemType) : CellContent()
    /** Limpa um 3x3 ao redor. */
    data class Bomb(override val type: GemType) : CellContent()

    /** "Vazio" só ocorre em transições; tabuleiro estável é sempre preenchido. */
    object Empty : CellContent() {
        override val type: GemType? = null
    }
}

const val BOARD_SIZE = 8

class Match3Engine(seed: Long = System.currentTimeMillis()) {
    private val rng = Random(seed)
    private val board: Array<Array<CellContent>> = Array(BOARD_SIZE) {
        Array(BOARD_SIZE) { CellContent.Empty as CellContent }
    }

    var score: Long = 0L; private set
    var stage: Int = 1; private set
    private val stageTargets = longArrayOf(0, 5_000, 12_000, 25_000, 50_000, 100_000)

    init { initialize() }

    private fun initialize() {
        for (r in 0 until BOARD_SIZE) for (c in 0 until BOARD_SIZE) {
            board[r][c] = CellContent.Normal(pickAvoidingMatch(r, c))
        }
    }

    private fun pickAvoidingMatch(r: Int, c: Int): GemType {
        val all = GemType.entries.toMutableSet()
        if (c >= 2) {
            val a = board[r][c - 1].type; val b = board[r][c - 2].type
            if (a != null && a == b) all.remove(a)
        }
        if (r >= 2) {
            val a = board[r - 1][c].type; val b = board[r - 2][c].type
            if (a != null && a == b) all.remove(a)
        }
        return all.random(rng)
    }

    fun snapshot(): List<List<CellContent>> = board.map { it.toList() }

    fun trySwap(r1: Int, c1: Int, r2: Int, c2: Int): SwapResult {
        if (!areAdjacent(r1, c1, r2, c2)) return SwapResult.Invalid
        val a = board[r1][c1]; val b = board[r2][c2]
        if (a is CellContent.Empty || b is CellContent.Empty) return SwapResult.Invalid
        board[r1][c1] = b; board[r2][c2] = a
        val matches = findAllMatches()
        if (matches.isEmpty()) {
            board[r1][c1] = a; board[r2][c2] = b
            return SwapResult.NoMatch
        }
        var cascadeLevel = 0
        var totalGained = 0L
        var currentMatches = matches
        while (currentMatches.isNotEmpty()) {
            val gained = removeAndScore(currentMatches, cascadeLevel)
            totalGained += gained
            applyGravity()
            currentMatches = findAllMatches()
            cascadeLevel++
        }
        score += totalGained
        return SwapResult.Matched(totalGained, cascadeLevel)
    }

    private fun areAdjacent(r1: Int, c1: Int, r2: Int, c2: Int): Boolean =
        (r1 == r2 && kotlin.math.abs(c1 - c2) == 1) ||
            (c1 == c2 && kotlin.math.abs(r1 - r2) == 1)

    /** Encontra runs ≥3 e marca células incluindo runs especiais. */
    private fun findAllMatches(): MatchResult {
        val cells = mutableSetOf<Pair<Int, Int>>()
        val runs = mutableListOf<Run>()

        // horizontais
        for (r in 0 until BOARD_SIZE) {
            var startC = 0
            for (c in 1..BOARD_SIZE) {
                val same = c < BOARD_SIZE &&
                    board[r][c].type != null &&
                    board[r][c].type == board[r][c - 1].type
                if (!same) {
                    val len = c - startC
                    if (len >= 3) {
                        val type = board[r][startC].type!!
                        runs.add(Run(orientation = RunOrientation.HORIZONTAL, row = r, col = startC, length = len, type = type))
                        for (k in 0 until len) cells.add(r to (startC + k))
                    }
                    startC = c
                }
            }
        }
        // verticais
        for (c in 0 until BOARD_SIZE) {
            var startR = 0
            for (r in 1..BOARD_SIZE) {
                val same = r < BOARD_SIZE &&
                    board[r][c].type != null &&
                    board[r][c].type == board[r - 1][c].type
                if (!same) {
                    val len = r - startR
                    if (len >= 3) {
                        val type = board[startR][c].type!!
                        runs.add(Run(orientation = RunOrientation.VERTICAL, row = startR, col = c, length = len, type = type))
                        for (k in 0 until len) cells.add((startR + k) to c)
                    }
                    startR = r
                }
            }
        }
        return MatchResult(cells = cells, runs = runs)
    }

    private fun removeAndScore(result: MatchResult, cascadeLevel: Int): Long {
        // Gerar gemas especiais conforme tamanho das runs
        val specialsToPlace = mutableListOf<Pair<Pair<Int, Int>, CellContent>>()
        for (run in result.runs) {
            if (run.length >= 4) {
                val midCol = if (run.orientation == RunOrientation.HORIZONTAL) run.col + run.length / 2 else run.col
                val midRow = if (run.orientation == RunOrientation.VERTICAL) run.row + run.length / 2 else run.row
                val special: CellContent = when {
                    run.length >= 5 -> CellContent.Bomb(run.type)
                    run.orientation == RunOrientation.HORIZONTAL -> CellContent.FlameH(run.type)
                    else -> CellContent.FlameV(run.type)
                }
                specialsToPlace.add((midRow to midCol) to special)
            }
        }

        // Expandir efeito de especiais incluídos no conjunto removido
        val finalCells = result.cells.toMutableSet()
        for ((r, c) in result.cells) {
            when (val cell = board[r][c]) {
                is CellContent.FlameH -> for (cc in 0 until BOARD_SIZE) finalCells.add(r to cc)
                is CellContent.FlameV -> for (rr in 0 until BOARD_SIZE) finalCells.add(rr to c)
                is CellContent.Bomb -> {
                    for (rr in (r - 1)..(r + 1)) for (cc in (c - 1)..(c + 1)) {
                        if (rr in 0 until BOARD_SIZE && cc in 0 until BOARD_SIZE) finalCells.add(rr to cc)
                    }
                }
                else -> {}
            }
        }

        var base = 0L
        for ((r, c) in finalCells) {
            board[r][c] = CellContent.Empty
            base += 10
        }
        for ((pos, content) in specialsToPlace) {
            if (pos.first in 0 until BOARD_SIZE && pos.second in 0 until BOARD_SIZE) {
                board[pos.first][pos.second] = content
            }
        }
        val multiplier = 1.0 + 0.5 * cascadeLevel
        return (base * multiplier).toLong()
    }

    private fun applyGravity() {
        for (c in 0 until BOARD_SIZE) {
            var write = BOARD_SIZE - 1
            for (r in BOARD_SIZE - 1 downTo 0) {
                if (board[r][c] !is CellContent.Empty) {
                    if (write != r) {
                        board[write][c] = board[r][c]
                        board[r][c] = CellContent.Empty
                    }
                    write--
                }
            }
            for (r in write downTo 0) {
                board[r][c] = CellContent.Normal(GemType.entries.random(rng))
            }
        }
    }

    fun isStageComplete(): Boolean {
        val target = stageTargets.getOrNull(stage) ?: return false
        return score >= target
    }

    fun advanceStage() { if (stage < stageTargets.size - 1) stage++ }
}

private enum class RunOrientation { HORIZONTAL, VERTICAL }
private data class Run(
    val orientation: RunOrientation,
    val row: Int,
    val col: Int,
    val length: Int,
    val type: GemType,
)
private data class MatchResult(val cells: Set<Pair<Int, Int>>, val runs: List<Run>) {
    fun isEmpty() = cells.isEmpty()
    fun isNotEmpty() = cells.isNotEmpty()
}

sealed interface SwapResult {
    data object Invalid : SwapResult
    data object NoMatch : SwapResult
    data class Matched(val pointsGained: Long, val cascadeLevels: Int) : SwapResult
}
