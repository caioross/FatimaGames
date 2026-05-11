package com.fatimagames.app.feature.games.match3

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.feature.games.match3.domain.CellContent
import com.fatimagames.app.feature.games.match3.domain.GemType

@Composable
fun Match3Screen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: Match3ViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        GameTopBar(
            title = "Combinar gemas",
            subtitle = "Fase ${state.stage}",
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

        Box(
            modifier = Modifier
                .padding(theme.spacing.md)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(theme.radii.lg))
                .background(theme.color.bgTubeWell)
                .padding(theme.spacing.xs),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.board.forEachIndexed { r, row ->
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        row.forEachIndexed { c, cell ->
                            val type = cell.type
                            val isSpecial = cell !is CellContent.Normal && cell !is CellContent.Empty
                            val selected = state.selected == (r to c)
                            val scale by animateFloatAsState(
                                targetValue = if (selected) 1.10f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                                label = "gem-scale",
                            )
                            val alpha by animateFloatAsState(
                                targetValue = if (type == null) 0f else 1f,
                                animationSpec = tween(durationMillis = 220),
                                label = "gem-alpha",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(1.dp)
                                    .graphicsLayer {
                                        scaleX = scale; scaleY = scale
                                        this.alpha = alpha
                                    }
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(gemColor(type, theme))
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.dp, theme.color.accentGold, RoundedCornerShape(6.dp))
                                        } else if (isSpecial) {
                                            Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp))
                                        } else Modifier
                                    )
                                    .clickable { viewModel.onCellTap(r, c) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    specialSymbol(cell) ?: gemSymbol(type),
                                    color = Color.White,
                                    style = typo.titleMd,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

private fun gemColor(type: GemType?, theme: com.fatimagames.app.core.theme.AppTheme): Color = when (type) {
    GemType.RAIN -> Color(0xFF4A8AB8)
    GemType.LEAF -> theme.color.feedbackSuccess
    GemType.BLOOM -> Color(0xFFC95B85)
    GemType.SUN -> theme.color.accentGold
    GemType.MOON -> theme.color.accentPlum
    GemType.EMBER -> theme.color.accentTerracotta
    null -> Color.Transparent
}

private fun gemSymbol(type: GemType?): String = when (type) {
    GemType.RAIN -> "◇"
    GemType.LEAF -> "❀"
    GemType.BLOOM -> "✿"
    GemType.SUN -> "✦"
    GemType.MOON -> "☾"
    GemType.EMBER -> "▲"
    null -> ""
}

private fun specialSymbol(cell: CellContent): String? = when (cell) {
    is CellContent.FlameH -> "⟷"
    is CellContent.FlameV -> "⟷"
    is CellContent.Bomb -> "✺"
    else -> null
}
