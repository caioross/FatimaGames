package com.fatimagames.app.feature.games.mahjong

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.WinOverlay
import com.fatimagames.app.feature.games.mahjong.domain.MahjongTile
import com.fatimagames.app.feature.games.mahjong.ui.MahjongTile as MahjongTileVisual
import com.fatimagames.app.feature.games.mahjong.ui.TileVisualState

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
            rightContent = {
                Text(
                    formatTime(state.elapsedMs),
                    color = theme.color.textPrimary,
                    style = typo.numericMd,
                    modifier = Modifier.padding(end = theme.spacing.md),
                )
            },
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
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
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
private fun MahjongBoard(state: MahjongUiState, onTileTap: (Int) -> Unit) {
    val active = state.tiles.filter { !it.removed }
    if (active.isEmpty()) return

    val minCol = active.minOf { it.col }
    val maxCol = active.maxOf { it.col }
    val minRow = active.minOf { it.row }
    val maxRow = active.maxOf { it.row }
    val halfCol = (maxCol - minCol + 2)
    val rowsSpan = (maxRow - minRow + 2)

    // Tamanho fixo de tile para MVP
    val tileW = 44.dp
    val tileH = 58.dp
    val tileDepth = 5.dp
    val gridStepX = 22.dp   // half-cell horizontal
    val gridStepY = 30.dp   // step vertical
    val layerOffset = 4.dp  // deslocamento isométrico por camada

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box {
            // Ordenar por (layer, row, col) para empilhamento visual correto
            active.sortedWith(compareBy({ it.layer }, { it.row }, { it.col })).forEach { tile ->
                val isFree = computeIsFree(tile, active)
                val selected = state.selectedId == tile.id
                val hinted = state.hintPair?.let { it.first == tile.id || it.second == tile.id } == true

                val visualState = when {
                    selected -> TileVisualState.SELECTED
                    hinted -> TileVisualState.HINTED
                    !isFree -> TileVisualState.BLOCKED
                    else -> TileVisualState.FREE
                }

                val scale by animateFloatAsState(
                    targetValue = when (visualState) {
                        TileVisualState.SELECTED -> 1.08f
                        TileVisualState.HINTED -> 1.04f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "tile-scale",
                )

                val x = ((tile.col - minCol).toFloat() * gridStepX.value - tile.layer * layerOffset.value).dp
                val y = ((tile.row - minRow).toFloat() * gridStepY.value - tile.layer * layerOffset.value).dp

                Box(
                    modifier = Modifier
                        .offset(x, y)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clickable(enabled = isFree) { onTileTap(tile.id) },
                ) {
                    MahjongTileVisual(
                        face = tile.face,
                        state = visualState,
                        width = tileW,
                        height = tileH,
                        depth = tileDepth,
                    )
                }
            }
        }
    }
}

private fun computeIsFree(tile: MahjongTile, all: List<MahjongTile>): Boolean {
    val above = all.any { t ->
        t.layer == tile.layer + 1 &&
            kotlin.math.abs(t.row - tile.row) <= 1 &&
            kotlin.math.abs(t.col - tile.col) <= 1
    }
    if (above) return false
    val same = all.filter { it.layer == tile.layer && it.id != tile.id }
    val left = same.any { it.row == tile.row && it.col == tile.col - 2 }
    val right = same.any { it.row == tile.row && it.col == tile.col + 2 }
    return !(left && right)
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}
