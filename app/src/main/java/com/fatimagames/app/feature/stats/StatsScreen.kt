package com.fatimagames.app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.SimpleTopBar
import com.fatimagames.app.core.ui.StatCard
import com.fatimagames.app.domain.model.GameRecord
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val records: List<GameRecord> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val recordRepo: RecordRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            recordRepo.observeAllRecent(50).collect { list ->
                _state.update { it.copy(records = list) }
            }
        }
    }
}

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
        SimpleTopBar(title = "Records", onBackClick = onBack)
        if (state.records.isEmpty()) {
            EmptyStats()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(theme.spacing.md)) {
                items(GameType.entries) { gameType ->
                    val byGame = state.records.filter { it.gameType == gameType }
                    if (byGame.isNotEmpty()) {
                        GameStatsCard(gameType = gameType, records = byGame)
                        Spacer(Modifier.height(theme.spacing.md))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStats() {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Column(
        modifier = Modifier.fillMaxSize().padding(theme.spacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Sem partidas ainda.",
            color = theme.color.textPrimary,
            style = typo.titleMd,
        )
        Spacer(Modifier.height(theme.spacing.xs))
        Text(
            "Comece a jogar e seus records aparecem aqui — você vai ver sua evolução com o tempo.",
            color = theme.color.textSecondary,
            style = typo.bodyMd,
        )
    }
}

@Composable
private fun GameStatsCard(gameType: GameType, records: List<GameRecord>) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    val bestTimeMs = records.minOf { it.durationMs }
    val bestScore = records.mapNotNull { it.score }.maxOrNull()
    val count = records.size
    val avgTimeMs = records.sumOf { it.durationMs } / count

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.radii.lg))
            .background(theme.color.bgSurface)
            .padding(theme.spacing.md),
    ) {
        Text(gameType.label, color = theme.color.textPrimary, style = typo.titleMd)
        Spacer(Modifier.height(theme.spacing.sm))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.spacing.xs)) {
            StatCard(label = "Melhor tempo", value = formatTime(bestTimeMs), modifier = Modifier.weight(1f))
            StatCard(label = "Partidas", value = count.toString(), modifier = Modifier.weight(1f))
            if (bestScore != null) {
                StatCard(label = "Melhor pontos", value = "%,d".format(bestScore), modifier = Modifier.weight(1f))
            } else {
                StatCard(label = "Tempo médio", value = formatTime(avgTimeMs), modifier = Modifier.weight(1f))
            }
        }

        // Mini gráfico — últimas 10 partidas
        Spacer(Modifier.height(theme.spacing.md))
        Text("Suas últimas partidas", color = theme.color.textSecondary, style = typo.labelMd)
        Spacer(Modifier.height(theme.spacing.xs))
        MiniBarChart(values = records.take(10).reversed().map { it.durationMs.toFloat() })
    }
}

@Composable
private fun MiniBarChart(values: List<Float>) {
    val theme = LocalAppTheme.current
    if (values.isEmpty()) return
    val maxV = values.max()
    val minV = values.min()
    val range = (maxV - minV).coerceAtLeast(1f)
    Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
        val barW = size.width / (values.size * 1.5f)
        val gap = barW * 0.5f
        for ((i, v) in values.withIndex()) {
            // menor tempo = barra menor (representando melhor); invertido para "subir" significar melhora
            val normalized = 1f - ((v - minV) / range)  // 0..1, maior = melhor
            val h = (size.height * (0.2f + normalized * 0.8f))
            val x = i * (barW + gap) + gap / 2f
            drawRoundRect(
                color = if (i == values.size - 1) theme.color.accentGold else theme.color.primary,
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 3f, barW / 3f),
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}
