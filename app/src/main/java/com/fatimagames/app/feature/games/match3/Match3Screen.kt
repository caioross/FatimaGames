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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fatimagames.app.feature.games.match3.domain.CellContent
import com.fatimagames.app.feature.games.match3.ui.Gem

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
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        row.forEachIndexed { c, cell ->
                            val selected = state.selected == (r to c)
                            GemCell(
                                cell = cell,
                                selected = selected,
                                onClick = { viewModel.onCellTap(r, c) },
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }
        }

        Spacer(Modifier.height(theme.spacing.sm))
        Text(
            "Toque numa gema, depois numa vizinha. 3+ iguais somam pontos.",
            color = theme.color.textSecondary,
            style = typo.labelMd,
            modifier = Modifier.padding(horizontal = theme.spacing.lg, vertical = theme.spacing.xs),
        )
    }
}

@Composable
private fun GemCell(
    cell: CellContent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAppTheme.current
    val isVisible = cell !is CellContent.Empty

    val scale by animateFloatAsState(
        targetValue = when {
            !isVisible -> 0f
            selected -> 1.12f
            else -> 1f
        },
        animationSpec = if (selected) spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ) else tween(durationMillis = 160),
        label = "gem-scale",
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "gem-alpha",
    )

    Box(
        modifier = modifier
            .padding(1.dp)
            .graphicsLayer {
                scaleX = scale; scaleY = scale; this.alpha = alpha
            }
            .clip(RoundedCornerShape(8.dp))
            .background(theme.color.bgSurface.copy(alpha = 0.35f))
            .then(
                if (selected) Modifier.border(2.dp, theme.color.accentGold, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cell !is CellContent.Empty) {
            Gem(cell = cell, modifier = Modifier.fillMaxSize().padding(2.dp))
        }
    }
}
