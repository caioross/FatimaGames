package com.fatimagames.app.feature.games.minesweeper.domain

import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
data class CellSnap(val mine: Boolean, val revealed: Boolean, val flagged: Boolean, val adj: Int)

@Serializable
data class MinesweeperSnapshot(
    val difficulty: String,
    val grid: List<List<CellSnap>>,
    val flagsUsed: Int,
    val minesPlaced: Boolean,
)

data class Cell(
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val adjacentMines: Int = 0,
)

enum class Difficulty(val rows: Int, val cols: Int, val mines: Int, val label: String) {
    EASY(9, 9, 10, "Fácil"),
    MEDIUM(12, 10, 20, "Médio"),
    HARD(16, 12, 40, "Difícil"),
}

sealed interface GameStatus {
    data object Playing : GameStatus
    data object Won : GameStatus
    data class Lost(val mineRow: Int, val mineCol: Int) : GameStatus
}

class MinesweeperEngine(val difficulty: Difficulty, val seed: Long = System.currentTimeMillis()) {

    private val rng = Random(seed)
    private val grid: Array<Array<Cell>> = Array(difficulty.rows) {
        Array(difficulty.cols) { Cell() }
    }
    private var minesPlaced = false
    var status: GameStatus = GameStatus.Playing; private set
    var flagsUsed: Int = 0; private set

    val rows: Int get() = difficulty.rows
    val cols: Int get() = difficulty.cols
    val totalMines: Int get() = difficulty.mines
    val flagsRemaining: Int get() = totalMines - flagsUsed

    fun snapshot(): List<List<Cell>> = grid.map { it.toList() }

    fun toSnapshot(): MinesweeperSnapshot = MinesweeperSnapshot(
        difficulty = difficulty.name,
        grid = grid.map { row -> row.map { CellSnap(it.isMine, it.isRevealed, it.isFlagged, it.adjacentMines) } },
        flagsUsed = flagsUsed,
        minesPlaced = minesPlaced,
    )

    fun loadFromSnapshot(snap: MinesweeperSnapshot) {
        for (r in 0 until rows) for (c in 0 until cols) {
            val sc = snap.grid.getOrNull(r)?.getOrNull(c) ?: continue
            grid[r][c] = Cell(sc.mine, sc.revealed, sc.flagged, sc.adj)
        }
        flagsUsed = snap.flagsUsed
        minesPlaced = snap.minesPlaced
    }

    fun tap(r: Int, c: Int): Boolean {
        if (status !is GameStatus.Playing) return false
        val cell = grid[r][c]
        if (cell.isFlagged || cell.isRevealed) return false
        if (!minesPlaced) {
            placeMines(safeR = r, safeC = c)
            minesPlaced = true
        }
        if (grid[r][c].isMine) {
            // Revela todas as minas
            for (i in 0 until rows) for (j in 0 until cols) {
                if (grid[i][j].isMine) grid[i][j] = grid[i][j].copy(isRevealed = true)
            }
            status = GameStatus.Lost(r, c)
            return true
        }
        floodReveal(r, c)
        checkWin()
        return true
    }

    fun toggleFlag(r: Int, c: Int): Boolean {
        if (status !is GameStatus.Playing) return false
        val cell = grid[r][c]
        if (cell.isRevealed) return false
        if (cell.isFlagged) {
            grid[r][c] = cell.copy(isFlagged = false)
            flagsUsed--
        } else {
            if (flagsUsed >= totalMines) return false
            grid[r][c] = cell.copy(isFlagged = true)
            flagsUsed++
        }
        return true
    }

    private fun placeMines(safeR: Int, safeC: Int) {
        val safeZone = mutableSetOf<Pair<Int, Int>>()
        for (dr in -1..1) for (dc in -1..1) {
            val r = safeR + dr; val c = safeC + dc
            if (r in 0 until rows && c in 0 until cols) safeZone.add(r to c)
        }
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until rows) for (j in 0 until cols) {
            if ((i to j) !in safeZone) candidates.add(i to j)
        }
        candidates.shuffle(rng)
        for ((r, c) in candidates.take(totalMines)) {
            grid[r][c] = grid[r][c].copy(isMine = true)
        }
        // Calcular adjacencies
        for (i in 0 until rows) for (j in 0 until cols) {
            if (grid[i][j].isMine) continue
            var count = 0
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val ni = i + dr; val nj = j + dc
                if (ni in 0 until rows && nj in 0 until cols && grid[ni][nj].isMine) count++
            }
            grid[i][j] = grid[i][j].copy(adjacentMines = count)
        }
    }

    private fun floodReveal(r: Int, c: Int) {
        val queue: ArrayDeque<Pair<Int, Int>> = ArrayDeque()
        queue.add(r to c)
        while (queue.isNotEmpty()) {
            val (i, j) = queue.removeFirst()
            val cell = grid[i][j]
            if (cell.isRevealed || cell.isFlagged || cell.isMine) continue
            grid[i][j] = cell.copy(isRevealed = true)
            if (cell.adjacentMines == 0) {
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val ni = i + dr; val nj = j + dc
                    if (ni in 0 until rows && nj in 0 until cols) {
                        queue.add(ni to nj)
                    }
                }
            }
        }
    }

    private fun checkWin() {
        for (i in 0 until rows) for (j in 0 until cols) {
            val cell = grid[i][j]
            if (!cell.isMine && !cell.isRevealed) return
        }
        status = GameStatus.Won
    }
}
