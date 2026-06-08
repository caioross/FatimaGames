package com.fatimagames.app.core.navigation

import kotlinx.serialization.Serializable

@Serializable object SplashRoute
@Serializable object HomeRoute
@Serializable object StatsRoute
@Serializable object SettingsRoute
@Serializable object PhotoLibraryRoute

@Serializable object JigsawSetupRoute
@Serializable data class JigsawGameRoute(val photoId: Long, val pieceCount: Int)

@Serializable object MahjongGameRoute
@Serializable object Match3GameRoute
@Serializable data class ColorSortGameRoute(val stage: Int = 1)

@Serializable object SolitaireRoute
@Serializable object MinesweeperRoute
@Serializable object TetrisRoute
@Serializable object FroggerRoute
