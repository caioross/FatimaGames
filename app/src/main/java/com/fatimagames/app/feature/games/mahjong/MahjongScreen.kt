package com.fatimagames.app.feature.games.mahjong

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.WinOverlay

@Composable
fun MahjongScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: MahjongViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        GameTopBar(
            title = "Mahjong",
            subtitle = "${state.remaining} peças",
            onBackClick = onBack,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(theme.spacing.md)
                .clip(RoundedCornerShape(theme.radii.lg))
                .background(theme.color.bgTubeWell),
        ) {
            MahjongBoard(
                state = state,
                onTileTap = viewModel::onTileTap,
            )

            if (state.completed) {
                WinOverlay(
                    timeLabel = formatTime(state.elapsedMs),
                    secondaryLabel = "Peças",
                    secondaryValue = state.tiles.size.toString(),
                    onPlayAgain = viewModel::onRestart,
                    onHome = onHome,
                )
            }
        }

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = theme.spacing.md, vertical = theme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(label = "Desfazer", icon = Icons.Outlined.Undo, onClick = viewModel::onUndo)
            ActionButton(label = "Dica", icon = Icons.Outlined.Lightbulb, onClick = viewModel::onHint)
            ActionButton(label = "Embaralhar", icon = Icons.Outlined.Shuffle, onClick = viewModel::onReshuffle)
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(theme.radii.md))
            .clickable(onClick = onClick)
            .padding(horizontal = theme.spacing.sm, vertical = theme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = theme.color.primaryPressed, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = theme.color.textSecondary, style = typo.labelMd)
    }
}

@Composable
private fun MahjongBoard(
    state: MahjongUiState,
    onTileTap: (Int) -> Unit,
) {
    val theme = LocalAppTheme.current
    val activeTiles = state.tiles.filter { !it.removed }
    if (activeTiles.isEmpty()) return

    // Determinar tamanho da peça baseado nas dimensões disponíveis (placeholder simples)
    val minCol = activeTiles.minOf { it.col }
    val maxCol = activeTiles.maxOf { it.col }
    val minRow = activeTiles.minOf { it.row }
    val maxRow = activeTiles.maxOf { it.row }
    val gridW = maxCol - minCol + 2
    val gridH = maxRow - minRow + 2

    // Tamanho em dp, fixo para MVP — cada half-cell ocupa 24.dp
    val halfCell = 24.dp

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(width = (gridW * 24).dp, height = (gridH * 32).dp)) {
            // Ordenar por camada para empilhamento visual
            activeTiles.sortedWith(compareBy({ it.layer }, { it.row }, { it.col })).forEach { tile ->
                val x = ((tile.col - minCol).toFloat() * 24f - tile.layer * 3f).dp
                val y = ((tile.row - minRow).toFloat() * 32f - tile.layer * 3f).dp
                val isFree = activeTiles.let { all ->
                    val above = all.any { t ->
                        t.layer == tile.layer + 1 && kotlin.math.abs(t.row - tile.row) <= 1 && kotlin.math.abs(t.col - tile.col) <= 1
                    }
                    if (above) false else {
                        val same = all.filter { it.layer == tile.layer && it.id != tile.id }
                        val left = same.any { it.row == tile.row && it.col == tile.col - 2 }
                        val right = same.any { it.row == tile.row && it.col == tile.col + 2 }
                        !(left && right)
                    }
                }
                val selected = state.selectedId == tile.id
                val hinted = state.hintPair?.let { it.first == tile.id || it.second == tile.id } == true

                val scale by animateFloatAsState(
                    targetValue = when {
                        selected -> 1.08f
                        hinted -> 1.05f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "tile-scale",
                )

                Box(
                    modifier = Modifier
                        .offset(x, y)
                        .size(width = 48.dp, height = 64.dp)
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                selected -> Color(0xFFFFE489)
                                !isFree -> Color(0xFFE8DCC0)
                                else -> Color(0xFFF4ECD6)
                            }
                        )
                        .border(
                            width = if (hinted) 2.dp else 1.dp,
                            color = if (hinted) theme.color.accentGold else Color(0xFF2C2A26),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable(enabled = isFree) { onTileTap(tile.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tile.face.displayChar,
                        color = Color(0xFF2C2A26),
                        fontSize = 14.sp,
                    )
                }
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
