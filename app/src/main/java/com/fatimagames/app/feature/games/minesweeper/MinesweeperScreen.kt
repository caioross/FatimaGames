package com.fatimagames.app.feature.games.minesweeper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameBackGuard
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.TutorialContent
import com.fatimagames.app.core.ui.TutorialFirstTime
import com.fatimagames.app.core.ui.WinOverlay
import com.fatimagames.app.feature.games.minesweeper.domain.Cell
import com.fatimagames.app.feature.games.minesweeper.domain.Difficulty
import com.fatimagames.app.feature.games.minesweeper.domain.GameStatus

private val NumberColors = mapOf(
    1 to Color(0xFF1D5FA5),
    2 to Color(0xFF1E6B3C),
    3 to Color(0xFFB8351F),
    4 to Color(0xFF3C3489),
    5 to Color(0xFF712B13),
    6 to Color(0xFF0F6E56),
    7 to Color(0xFF2C2A26),
    8 to Color(0xFF6B665E),
)

@Composable
fun MinesweeperScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: MinesweeperViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    val hasProgress = state.grid.any { row -> row.any { it.isRevealed || it.isFlagged } } &&
        state.status is com.fatimagames.app.feature.games.minesweeper.domain.GameStatus.Playing
    GameBackGuard(hasProgress = hasProgress, onConfirmedExit = onBack) {
    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
        GameTopBar(
            title = "Campo Minado",
            subtitle = "${state.difficulty.label} · ${state.flagsRemaining}🚩",
            onBackClick = onBack,
            rightContent = {
                Text(
                    formatTime(state.elapsedMs),
                    color = theme.color.textPrimary,
                    style = typo.numericMd,
                    modifier = Modifier.padding(end = theme.spacing.md),
                )
            },
        )

        // Seletor de dificuldade
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.xs),
        ) {
            Difficulty.entries.forEach { d ->
                val isSel = state.difficulty == d
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(theme.radii.md))
                        .background(if (isSel) theme.color.primary else theme.color.bgSurfaceMuted)
                        .clickable { viewModel.setDifficulty(d) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        d.label,
                        color = if (isSel) theme.color.textInverse else theme.color.textPrimary,
                        style = typo.labelMd,
                    )
                }
            }
        }

        Spacer(Modifier.height(theme.spacing.sm))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(theme.spacing.md)
                .clip(RoundedCornerShape(theme.radii.lg))
                .background(theme.color.bgTubeWell)
                .padding(theme.spacing.xs),
        ) {
            MinesweeperGrid(state = state, viewModel = viewModel)
            if (state.status is GameStatus.Won) {
                WinOverlay(
                    timeLabel = formatTime(state.elapsedMs),
                    secondaryLabel = "Dificuldade",
                    secondaryValue = state.difficulty.label,
                    onPlayAgain = viewModel::onRestart,
                    onHome = onHome,
                )
            }
            if (state.status is GameStatus.Lost) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
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
                        Text("Que pena!", color = theme.color.textPrimary, style = typo.displayMd)
                        Spacer(Modifier.height(theme.spacing.sm))
                        Text(
                            "Tente de novo — você consegue!",
                            color = theme.color.textSecondary,
                            style = typo.bodyLg,
                        )
                        Spacer(Modifier.height(theme.spacing.lg))
                        com.fatimagames.app.core.ui.PrimaryButton(
                            text = "Jogar de novo",
                            onClick = viewModel::onRestart,
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
        }

        // Toggle flag mode
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.spacing.md, vertical = theme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                label = if (state.flagMode) "Modo bandeira" else "Modo toque",
                icon = Icons.Outlined.Flag,
                highlighted = state.flagMode,
                onClick = viewModel::toggleFlagMode,
            )
            ActionButton(
                label = "Reiniciar",
                icon = Icons.Outlined.Refresh,
                onClick = viewModel::onRestart,
            )
        }
    }
    TutorialFirstTime(name = "minesweeper", steps = TutorialContent.minesweeper)
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(theme.radii.md))
            .background(if (highlighted) theme.color.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = theme.spacing.md, vertical = theme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (highlighted) theme.color.primary else theme.color.primaryPressed,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = theme.color.textSecondary, style = typo.labelMd)
    }
}

@Composable
private fun MinesweeperGrid(state: MinesweeperUiState, viewModel: MinesweeperViewModel) {
    val aspectRatio = state.cols.toFloat() / state.rows.toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            for (r in 0 until state.rows) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    for (c in 0 until state.cols) {
                        val cell = state.grid.getOrNull(r)?.getOrNull(c) ?: Cell()
                        MinesweeperCell(
                            cell = cell,
                            onTap = { viewModel.onCellTap(r, c) },
                            onLongPress = { viewModel.onCellLongPress(r, c) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinesweeperCell(
    cell: Cell,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAppTheme.current
    val bg = when {
        cell.isRevealed && cell.isMine -> Color(0xFFE24B4A)
        cell.isRevealed -> theme.color.bgSurface
        else -> theme.color.bgSurfaceMuted
    }
    val border = when {
        cell.isRevealed -> theme.color.borderSubtle
        else -> theme.color.borderStrong
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(3.dp))
            .pointerInput(cell) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.isFlagged && !cell.isRevealed -> Text("🚩", fontSize = 14.sp)
            cell.isRevealed && cell.isMine -> Text("💣", fontSize = 14.sp)
            cell.isRevealed && cell.adjacentMines > 0 -> {
                Text(
                    cell.adjacentMines.toString(),
                    color = NumberColors[cell.adjacentMines] ?: Color.Black,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700),
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}
