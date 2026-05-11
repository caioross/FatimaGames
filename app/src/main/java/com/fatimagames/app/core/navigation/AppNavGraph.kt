package com.fatimagames.app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.feature.games.colorsort.ColorSortScreen
import com.fatimagames.app.feature.games.jigsaw.JigsawGameScreen
import com.fatimagames.app.feature.games.jigsaw.JigsawSetupScreen
import com.fatimagames.app.feature.games.mahjong.MahjongScreen
import com.fatimagames.app.feature.games.match3.Match3Screen
import com.fatimagames.app.feature.home.HomeScreen
import com.fatimagames.app.feature.photolibrary.PhotoLibraryScreen
import com.fatimagames.app.feature.settings.SettingsScreen
import com.fatimagames.app.feature.stats.StatsScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    val theme = LocalAppTheme.current
    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas),
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onGameJigsaw = { nav.navigate(JigsawSetupRoute) },
                onGameMahjong = { nav.navigate(MahjongGameRoute) },
                onGameMatch3 = { nav.navigate(Match3GameRoute) },
                onGameColorSort = { nav.navigate(ColorSortGameRoute()) },
                onStatsClick = { nav.navigate(StatsRoute) },
                onSettingsClick = { nav.navigate(SettingsRoute) },
            )
        }
        composable<StatsRoute> {
            StatsScreen(onBack = { nav.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable<PhotoLibraryRoute> {
            PhotoLibraryScreen(
                onBack = { nav.popBackStack() },
                onPhotoSelected = { photoId ->
                    nav.navigate(JigsawGameRoute(photoId = photoId, pieceCount = 24))
                },
            )
        }
        composable<JigsawSetupRoute> {
            JigsawSetupScreen(
                onBack = { nav.popBackStack() },
                onLaunch = { photoId, pieceCount ->
                    nav.navigate(JigsawGameRoute(photoId, pieceCount))
                },
            )
        }
        composable<JigsawGameRoute> { backStackEntry ->
            val route: JigsawGameRoute = backStackEntry.toRoute()
            JigsawGameScreen(
                photoId = route.photoId,
                pieceCount = route.pieceCount,
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<MahjongGameRoute> {
            MahjongScreen(
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<Match3GameRoute> {
            Match3Screen(
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<ColorSortGameRoute> { backStackEntry ->
            val route: ColorSortGameRoute = backStackEntry.toRoute()
            ColorSortScreen(
                stage = route.stage,
                onBack = { nav.popBackStack() },
                onHome = { nav.popBackStack(HomeRoute, inclusive = false) },
                onNextStage = { next ->
                    nav.navigate(ColorSortGameRoute(stage = next)) {
                        popUpTo(HomeRoute) { inclusive = false }
                    }
                },
            )
        }
    }
}
