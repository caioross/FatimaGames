package com.fatimagames.app.feature.games.mahjong

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.automirrored.outlined.Undo
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
import com.fatimagames.app.core.ui.GameBackGuard
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.TutorialContent
import com.fatimagames.app.core.ui.TutorialFirstTime
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

    val hasProgress = state.tiles.any { it.removed } && !state.completed

    GameBackGuard(hasProgress = hasProgress, onConfirmedExit = onBack) {
        Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
            GameTopBar(
                title = "Mahjong",
                subtitle = "${state.remaining} peças",
                onBackClick = { if (hasProgress) onBack() else onBack() },
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
            MahjongBoard(state = state, onTileTap = viewModel::onTileTap)
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
                ActionButton(label = "Desfazer", icon = Icons.AutoMirrored.Outlined.Undo, onClick = viewModel::onUndo)
                ActionButton(label = "Dica", icon = Icons.Outlined.Lightbulb, onClick = viewModel::onHint)
                ActionButton(label = "Embaralhar", icon = Icons.Outlined.Shuffle, onClick = viewModel::onReshuffle)
            }
        }
        TutorialFirstTime(name = "mahjong", steps = TutorialContent.mahjong)
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

/**
 * Mahjong board com sizing dinâmico (FIX M01/M02):
 * - Calcula bounding box em "unidades de half-cell"
 * - Determina tileW que faz tudo caber em maxWidth e maxHeight
 * - Centraliza o conjunto
 */
@Composable
private fun MahjongBoard(state: MahjongUiState, onTileTap: (Int) -> Unit) {
    val active = state.tiles.filter { !it.removed }
    if (active.isEmpty()) return

    val minCol = active.minOf { it.col }
    val maxCol = active.maxOf { it.col }
    val minRow = active.minOf { it.row }
    val maxRow = active.maxOf { it.row }
    val maxLayer = active.maxOf { it.layer }
    // Cada tile ocupa 2 half-cells de largura
    val halfCellsWide = (maxCol - minCol) + 2  // +2 porque tile precisa de 2 half-cells
    // Cada linha = 1 tile altura
    val tilesHigh = (maxRow - minRow) + 1
    // Layers superiores se deslocam isometrically
    val layerOffsetTilesW = maxLayer * 0.08f  // 8% de tileW por camada
    val layerOffsetTilesH = maxLayer * 0.08f

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val maxW = maxWidth
        val maxH = maxHeight

        // Resolve tileW para caber em ambas as dimensões
        // Largura usada: (halfCellsWide / 2) * tileW + layerOffsetTilesW * tileW
        // Altura usada: tilesHigh * tileH + layerOffsetTilesH * tileH
        // Com tileH = tileW * 1.32 (proporção tradicional)
        val tileAspect = 1.32f
        val depthRatio = 0.1f

        val tilesWideEffective = (halfCellsWide / 2f) + layerOffsetTilesW + depthRatio
        val tilesHighEffective = tilesHigh + layerOffsetTilesH + depthRatio

        val tileWByWidth = maxW / tilesWideEffective
        val tileWByHeight = (maxH / tilesHighEffective) / tileAspect
        val tileW = listOf(tileWByWidth, tileWByHeight, 60.dp).minOrNull() ?: 32.dp
        val tileH = tileW * tileAspect
        val tileDepth = tileW * depthRatio
        val layerOffsetX = tileW * 0.08f
        val layerOffsetY = tileH * 0.08f

        // Largura/altura efetivas para centralização
        val boardW = tileW * (halfCellsWide / 2f) + layerOffsetX * maxLayer + tileDepth
        val boardH = tileH * tilesHigh + layerOffsetY * maxLayer + tileDepth

        Box(
            modifier = Modifier.size(width = boardW, height = boardH),
        ) {
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

                val x = ((tile.col - minCol).toFloat() / 2f) * tileW.value - tile.layer * layerOffsetX.value
                val y = (tile.row - minRow).toFloat() * tileH.value - tile.layer * layerOffsetY.value

                Box(
                    modifier = Modifier
                        .offset(x.dp, y.dp)
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            // FIX V206: scale a partir do centro, não top-left
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                        }
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
