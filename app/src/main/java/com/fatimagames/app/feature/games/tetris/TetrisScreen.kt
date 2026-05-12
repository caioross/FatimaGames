package com.fatimagames.app.feature.games.tetris

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.feature.games.tetris.domain.BOARD_COLS
import com.fatimagames.app.feature.games.tetris.domain.BOARD_ROWS
import com.fatimagames.app.feature.games.tetris.domain.Tetromino

@Composable
fun TetrisScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: TetrisViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        GameTopBar(
            title = "Tetris",
            subtitle = "Nível ${state.level} · ${state.lines} linhas",
            onBackClick = onBack,
            rightContent = {
                Text(
                    "%,d".format(state.score),
                    color = theme.color.textPrimary,
                    style = typo.numericLg,
                    modifier = Modifier.padding(end = theme.spacing.md),
                )
            },
        )

        // Próxima peça preview
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Próxima:", color = theme.color.textSecondary, style = typo.labelMd)
            Spacer(Modifier.size(theme.spacing.sm))
            NextPiecePreview(piece = state.next)
        }

        Spacer(Modifier.height(theme.spacing.xs))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = theme.spacing.md)
                .aspectRatio(BOARD_COLS.toFloat() / BOARD_ROWS.toFloat())
                .clip(RoundedCornerShape(theme.radii.md))
                .background(Color(0xFF1C1A17)),
        ) {
            TetrisBoard(state = state)
            if (state.gameOver) {
                GameOverOverlay(score = state.score, onRestart = viewModel::restart, onHome = onHome)
            }
        }

        // Controles
        Spacer(Modifier.height(theme.spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.spacing.md),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(icon = Icons.Outlined.KeyboardArrowLeft, onClick = viewModel::moveLeft, label = "Esquerda")
            ControlButton(icon = Icons.Outlined.RotateRight, onClick = viewModel::rotate, label = "Girar")
            ControlButton(icon = Icons.Outlined.KeyboardArrowDown, onClick = viewModel::softDrop, label = "Descer")
            ControlButton(icon = Icons.Outlined.VerticalAlignBottom, onClick = viewModel::hardDrop, label = "Cair")
            ControlButton(icon = Icons.Outlined.KeyboardArrowRight, onClick = viewModel::moveRight, label = "Direita")
        }
        Spacer(Modifier.height(theme.spacing.sm))
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(theme.radii.md))
            .background(theme.color.bgSurfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = theme.spacing.sm, vertical = theme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = theme.color.primaryPressed, modifier = Modifier.size(36.dp))
        Text(label, color = theme.color.textSecondary, style = typo.labelMd)
    }
}

@Composable
private fun NextPiecePreview(piece: Tetromino?) {
    val theme = LocalAppTheme.current
    if (piece == null) return
    Canvas(modifier = Modifier.size(width = 80.dp, height = 32.dp)) {
        val color = Color(piece.color)
        val cellSize = (size.height / 2f)
        val cells = piece.blocks[0]
        val minR = cells.minOf { it.first }
        val minC = cells.minOf { it.second }
        for ((r, c) in cells) {
            drawRect(
                color = color,
                topLeft = Offset((c - minC) * cellSize, (r - minR) * cellSize),
                size = Size(cellSize - 1f, cellSize - 1f),
            )
        }
    }
}

@Composable
private fun TetrisBoard(state: TetrisUiState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cellW = w / BOARD_COLS
        val cellH = h / BOARD_ROWS

        // Grid sutil
        val gridColor = Color(0xFF2C2A26)
        for (i in 0..BOARD_ROWS) {
            drawLine(gridColor, Offset(0f, i * cellH), Offset(w, i * cellH), strokeWidth = 0.5f)
        }
        for (j in 0..BOARD_COLS) {
            drawLine(gridColor, Offset(j * cellW, 0f), Offset(j * cellW, h), strokeWidth = 0.5f)
        }

        // Peças assentadas
        for (r in 0 until BOARD_ROWS) for (c in 0 until BOARD_COLS) {
            val ord = state.grid.getOrNull(r)?.getOrNull(c) ?: -1
            if (ord >= 0) {
                val color = Color(Tetromino.entries[ord].color)
                drawCell(color, c * cellW, r * cellH, cellW, cellH)
            }
        }

        // Peça atual + ghost
        val cur = state.current
        if (cur != null) {
            // Ghost (sombra do drop)
            var ghostRow = cur.row
            while (true) {
                val ghostCells = cur.copy(row = ghostRow + 1).cells()
                val valid = ghostCells.all { (r, c) ->
                    r in 0 until BOARD_ROWS && c in 0 until BOARD_COLS &&
                        (state.grid.getOrNull(r)?.getOrNull(c) ?: -1) < 0
                }
                if (!valid) break
                ghostRow++
            }
            val ghostColor = Color(cur.type.color).copy(alpha = 0.25f)
            val ghostCells = cur.copy(row = ghostRow).cells()
            for ((r, c) in ghostCells) {
                if (r in 0 until BOARD_ROWS && c in 0 until BOARD_COLS) {
                    drawCell(ghostColor, c * cellW, r * cellH, cellW, cellH, ghost = true)
                }
            }
            // Peça real
            val color = Color(cur.type.color)
            for ((r, c) in cur.cells()) {
                if (r in 0 until BOARD_ROWS && c in 0 until BOARD_COLS) {
                    drawCell(color, c * cellW, r * cellH, cellW, cellH)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCell(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    ghost: Boolean = false,
) {
    if (ghost) {
        drawRect(color, Offset(x + 1f, y + 1f), Size(w - 2f, h - 2f))
        return
    }
    // Bevel falso
    drawRect(color, Offset(x + 1f, y + 1f), Size(w - 2f, h - 2f))
    drawRect(
        color.copy(alpha = 0.45f),
        Offset(x + 1f, y + 1f),
        Size(w - 2f, h * 0.18f),
    )
    drawRect(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(x + 1f, y + h * 0.82f),
        size = Size(w - 2f, h * 0.18f),
    )
}

@Composable
private fun GameOverOverlay(score: Long, onRestart: () -> Unit, onHome: () -> Unit) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(theme.radii.xl))
                .background(theme.color.bgSurface)
                .padding(theme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Game Over", color = theme.color.textPrimary, style = typo.displayMd)
            Spacer(Modifier.height(theme.spacing.sm))
            Text("Pontuação: %,d".format(score), color = theme.color.textSecondary, style = typo.bodyLg)
            Spacer(Modifier.height(theme.spacing.lg))
            com.fatimagames.app.core.ui.PrimaryButton(
                text = "Jogar de novo",
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(theme.spacing.sm))
            com.fatimagames.app.core.ui.TertiaryButton(
                text = "Voltar ao início",
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
