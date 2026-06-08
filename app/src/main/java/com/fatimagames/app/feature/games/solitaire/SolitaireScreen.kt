package com.fatimagames.app.feature.games.solitaire

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.GameBackGuard
import com.fatimagames.app.core.ui.GameTopBar
import com.fatimagames.app.core.ui.TutorialContent
import com.fatimagames.app.core.ui.TutorialFirstTime
import com.fatimagames.app.core.ui.WinOverlay
import com.fatimagames.app.feature.games.solitaire.domain.Pile
import com.fatimagames.app.feature.games.solitaire.domain.Selection
import com.fatimagames.app.feature.games.solitaire.ui.PlayingCard

@Composable
fun SolitaireScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    viewModel: SolitaireViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    val hasProgress = state.moves > 0 && !state.completed
    GameBackGuard(hasProgress = hasProgress, onConfirmedExit = onBack) {
    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
        GameTopBar(
            title = "Paciência",
            subtitle = "${state.moves} jogadas",
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
                .padding(theme.spacing.sm)
                .clip(RoundedCornerShape(theme.radii.lg))
                .background(theme.color.bgTubeWell)
                .padding(theme.spacing.sm),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            ) {
                TopRow(state = state, vm = viewModel)
                Spacer(Modifier.height(theme.spacing.md))
                TableauRow(state = state, vm = viewModel)
            }
            if (state.completed) {
                WinOverlay(
                    timeLabel = formatTime(state.elapsedMs),
                    secondaryLabel = "Jogadas",
                    secondaryValue = state.moves.toString(),
                    onPlayAgain = viewModel::onRestart,
                    onHome = onHome,
                )
            }
        }
    }
    TutorialFirstTime(name = "solitaire", steps = TutorialContent.solitaire)
    }
}

@Composable
private fun TopRow(state: SolitaireUiState, vm: SolitaireViewModel) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Stock + Waste
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.clickable { vm.onDrawClick() }) {
                if (state.stockCount > 0) {
                    PlayingCard(
                        card = com.fatimagames.app.feature.games.solitaire.domain.Card(
                            com.fatimagames.app.feature.games.solitaire.domain.Suit.CLUBS,
                            com.fatimagames.app.feature.games.solitaire.domain.Rank.ACE,
                            faceUp = false,
                        ),
                    )
                } else {
                    PlayingCard(card = null, placeholder = true)
                }
            }
            Box(modifier = Modifier.clickable(enabled = state.waste.isNotEmpty()) { vm.onWasteTap() }) {
                val top = state.waste.lastOrNull()
                if (top != null) {
                    PlayingCard(
                        card = top,
                        selected = state.selection?.pile is Pile.Waste,
                    )
                } else {
                    PlayingCard(card = null, placeholder = true)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // Foundations
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.foundations.forEachIndexed { idx, pile ->
                Box(modifier = Modifier.clickable {
                    if (pile.isEmpty()) vm.onEmptyFoundationTap(idx) else vm.onFoundationTap(idx)
                }) {
                    val top = pile.lastOrNull()
                    val selected = (state.selection?.pile as? Pile.Foundation)?.index == idx
                    if (top != null) {
                        PlayingCard(card = top, selected = selected)
                    } else {
                        PlayingCard(card = null, placeholder = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableauRow(state: SolitaireUiState, vm: SolitaireViewModel) {
    val theme = LocalAppTheme.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // FIX V502: cardW responsivo (7 colunas + 6 gaps de 4dp)
        val totalGap = 6 * 4
        val cardW = ((maxWidth.value - totalGap) / 7f).coerceIn(36f, 60f).dp
        val cardH = cardW * 1.42f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.tableau.forEachIndexed { col, pile ->
                TableauColumn(
                    col = col,
                    cards = pile,
                    selection = state.selection,
                    cardW = cardW,
                    cardH = cardH,
                    onEmpty = { vm.onEmptyTableauTap(col) },
                    onCard = { idx -> vm.onTableauCardTap(col, idx) },
                )
            }
        }
    }
}

@Composable
private fun TableauColumn(
    col: Int,
    cards: List<com.fatimagames.app.feature.games.solitaire.domain.Card>,
    selection: Selection?,
    cardW: androidx.compose.ui.unit.Dp = 46.dp,
    cardH: androidx.compose.ui.unit.Dp = 66.dp,
    onEmpty: () -> Unit,
    onCard: (Int) -> Unit,
) {
    val verticalGap = (cardH.value * 0.30f).dp
    val selectedFromIndex = (selection?.pile as? Pile.Tableau)
        ?.takeIf { it.index == col }
        ?.let { selection.fromIndex }

    Box(
        modifier = Modifier
            .width(cardW)
            .height(cardH + verticalGap * (cards.size.coerceAtLeast(1) - 1).coerceAtLeast(0))
            .clickable(enabled = cards.isEmpty()) { onEmpty() },
    ) {
        if (cards.isEmpty()) {
            PlayingCard(card = null, width = cardW, height = cardH, placeholder = true)
        } else {
            cards.forEachIndexed { idx, card ->
                Box(
                    modifier = Modifier
                        .offset(y = verticalGap * idx)
                        .clickable { onCard(idx) },
                ) {
                    val isSelected = selectedFromIndex != null && idx >= selectedFromIndex
                    PlayingCard(
                        card = card,
                        width = cardW,
                        height = cardH,
                        selected = isSelected,
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
