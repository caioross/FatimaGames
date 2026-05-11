package com.fatimagames.app.data.repository

import com.fatimagames.app.data.db.GameStateDao
import com.fatimagames.app.data.db.GameStateEntity
import com.fatimagames.app.domain.model.GameType
import com.fatimagames.app.domain.repository.GameStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameStateRepositoryImpl @Inject constructor(
    private val dao: GameStateDao,
) : GameStateRepository {

    override suspend fun saveSnapshot(type: GameType, snapshotJson: String) {
        dao.upsert(GameStateEntity(gameType = type.key, snapshotJson = snapshotJson, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun loadSnapshot(type: GameType): String? =
        dao.getByGame(type.key)?.snapshotJson

    override suspend fun clearSnapshot(type: GameType) {
        dao.clearByGame(type.key)
    }

    override fun observeMostRecentSnapshot(): Flow<Pair<GameType, Long>?> =
        dao.observeMostRecent().map { entity ->
            entity?.let { GameType.fromKey(it.gameType)?.let { gt -> gt to it.updatedAt } }
        }
}
