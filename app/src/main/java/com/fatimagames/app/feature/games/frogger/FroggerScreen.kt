package com.fatimagames.app.feature.games.frogger

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import com.fatimagames.app.feature.games.frogger.domain.COLS
import com.fatimagames.app.feature.games.frogger.domain.FrogStatus
import com.fatimagames.app.feature.games.frogger.domain.LaneType
import com.fatimagames.app.feature.games.frogger.domain.ROWS

@Composable
fun FroggerScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: FroggerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        GameTopBar(
            title = "Sapo aventureiro",
            subtitle = "${state.lives} vidas · ${state.goalsReached}/5 travessias",
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
                .fillMaxWidth()
                .padding(horizontal = theme.spacing.md)
                .aspectRatio(COLS.toFloat() / ROWS.toFloat())
                .clip(RoundedCornerShape(theme.radii.md)),
        ) {
            FroggerCanvas(state = state)
            if (state.status != FrogStatus.ALIVE) {
                EndOverlay(
                    won = state.status == FrogStatus.WON,
                    score = state.score,
                    onRestart = viewModel::restart,
                    onHome = onHome,
                )
            }
        }

        Spacer(Modifier.height(theme.spacing.md))
        // D-pad
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = theme.spacing.lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DPadButton(Icons.Outlined.KeyboardArrowUp, viewModel::moveUp)
                Spacer(Modifier.height(4.dp))
                Row {
                    DPadButton(Icons.Outlined.KeyboardArrowLeft, viewModel::moveLeft)
                    Spacer(Modifier.size(72.dp))
                    DPadButton(Icons.Outlined.KeyboardArrowRight, viewModel::moveRight)
                }
                Spacer(Modifier.height(4.dp))
                DPadButton(Icons.Outlined.KeyboardArrowDown, viewModel::moveDown)
            }
        }
        Spacer(Modifier.height(theme.spacing.md))
    }
}

@Composable
private fun DPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(theme.color.primary.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = theme.color.textInverse, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun FroggerCanvas(state: FroggerUiState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cellW = w / COLS
        val cellH = h / ROWS

        // Fundo por lane
        for (lane in state.lanes) {
            val color = when (lane.type) {
                LaneType.SAFE -> if (lane.row == 0) Color(0xFF7A9B7E) else Color(0xFFE6DAC6)
                LaneType.CAR -> Color(0xFF2C2A26)
                LaneType.WATER -> Color(0xFF4A8AB8)
            }
            drawRect(
                color,
                Offset(0f, lane.row * cellH),
                Size(w, cellH),
            )
            // Linhas da rua
            if (lane.type == LaneType.CAR) {
                val dashY = lane.row * cellH + cellH * 0.5f
                var x = 0f
                while (x < w) {
                    drawRect(
                        Color.White,
                        Offset(x, dashY - 1.5f),
                        Size(cellW * 0.4f, 3f),
                    )
                    x += cellW
                }
            }
        }

        // Obstáculos
        for ((row, items) in state.obstaclePositions) {
            val lane = state.lanes[row]
            val color = if (lane.type == LaneType.CAR) Color(0xFFE24B4A) else Color(0xFF8B5A2B)
            for ((startCol, len) in items) {
                val x = startCol * cellW
                drawRect(
                    color,
                    Offset(x + 2f, row * cellH + 4f),
                    Size(cellW * len - 4f, cellH - 8f),
                )
                if (lane.type == LaneType.WATER) {
                    // Detalhe do tronco
                    drawLine(
                        Color(0xFF6B3F1F),
                        Offset(x + 4f, row * cellH + cellH / 2f),
                        Offset(x + cellW * len - 4f, row * cellH + cellH / 2f),
                        strokeWidth = 2f,
                    )
                }
            }
        }

        // Sapo
        val fx = state.frog.col * cellW + cellW / 2f
        val fy = state.frog.row * cellH + cellH / 2f
        val r = minOf(cellW, cellH) * 0.36f
        drawCircle(Color(0xFF1E6B3C), radius = r, center = Offset(fx, fy))
        drawCircle(Color(0xFF3E8C5B), radius = r * 0.7f, center = Offset(fx, fy))
        // Olhinhos
        drawCircle(Color.White, radius = r * 0.18f, center = Offset(fx - r * 0.35f, fy - r * 0.35f))
        drawCircle(Color.White, radius = r * 0.18f, center = Offset(fx + r * 0.35f, fy - r * 0.35f))
        drawCircle(Color.Black, radius = r * 0.08f, center = Offset(fx - r * 0.35f, fy - r * 0.35f))
        drawCircle(Color.Black, radius = r * 0.08f, center = Offset(fx + r * 0.35f, fy - r * 0.35f))
    }
}

@Composable
private fun EndOverlay(won: Boolean, score: Long, onRestart: () -> Unit, onHome: () -> Unit) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
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
            Text(if (won) "Você venceu!" else "Quase!", color = theme.color.textPrimary, style = typo.displayMd)
            Spacer(Modifier.height(theme.spacing.sm))
            Text("Pontos: %,d".format(score), color = theme.color.textSecondary, style = typo.bodyLg)
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
