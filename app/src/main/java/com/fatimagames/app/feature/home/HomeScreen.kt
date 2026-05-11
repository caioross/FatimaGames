package com.fatimagames.app.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatimagames.app.R
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.ContinueBanner
import com.fatimagames.app.core.ui.GameCard
import com.fatimagames.app.core.ui.HomeTopBar
import com.fatimagames.app.domain.model.GameType

@Composable
fun HomeScreen(
    onGameJigsaw: () -> Unit,
    onGameMahjong: () -> Unit,
    onGameMatch3: () -> Unit,
    onGameColorSort: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        greeting = state.weekdayLabel,
        continueGame = state.continueGame,
        onGameJigsaw = onGameJigsaw,
        onGameMahjong = onGameMahjong,
        onGameMatch3 = onGameMatch3,
        onGameColorSort = onGameColorSort,
        onStatsClick = onStatsClick,
        onSettingsClick = onSettingsClick,
        onContinueClick = {
            when (state.continueGame?.gameType) {
                GameType.JIGSAW -> onGameJigsaw()
                GameType.MAHJONG -> onGameMahjong()
                GameType.MATCH3 -> onGameMatch3()
                GameType.COLOR_SORT -> onGameColorSort()
                null -> {}
            }
        },
    )
}

@Composable
private fun HomeContent(
    greeting: String,
    continueGame: ContinueGameInfo?,
    onGameJigsaw: () -> Unit,
    onGameMahjong: () -> Unit,
    onGameMatch3: () -> Unit,
    onGameColorSort: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        HomeTopBar(
            greeting = greeting,
            onRecordsClick = onStatsClick,
            onSettingsClick = onSettingsClick,
        )

        Column(modifier = Modifier.padding(horizontal = theme.spacing.lg)) {
            continueGame?.let {
                ContinueBanner(
                    title = "Continuar",
                    subtitle = it.title,
                    onClick = onContinueClick,
                    icon = Icons.Outlined.PlayArrow,
                )
                Spacer(Modifier.height(theme.spacing.md))
            }

            Text(
                "Escolha um jogo",
                color = theme.color.textSecondary,
                style = typo.labelMd,
                modifier = Modifier.padding(start = theme.spacing.xxs, bottom = theme.spacing.sm),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = theme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(theme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(theme.spacing.sm),
            ) {
                item {
                    GameCard(
                        title = "Quebra-cabeça",
                        subtitle = "Suas fotos",
                        onClick = onGameJigsaw,
                        illustration = { Illustration(R.drawable.illust_jigsaw) },
                    )
                }
                item {
                    GameCard(
                        title = "Mahjong",
                        subtitle = "Pares iguais",
                        onClick = onGameMahjong,
                        illustration = { Illustration(R.drawable.illust_mahjong) },
                    )
                }
                item {
                    GameCard(
                        title = "Combinar gemas",
                        subtitle = "3 iguais ou mais",
                        onClick = onGameMatch3,
                        illustration = { Illustration(R.drawable.illust_match3) },
                    )
                }
                item {
                    GameCard(
                        title = "Organizar cores",
                        subtitle = "Tubos coloridos",
                        onClick = onGameColorSort,
                        illustration = { Illustration(R.drawable.illust_colorsort) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Illustration(@androidx.annotation.DrawableRes resId: Int) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier.size(96.dp),
        contentScale = ContentScale.Fit,
    )
}
