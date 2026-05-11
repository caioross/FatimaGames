package com.fatimagames.app.domain.model

enum class GameType(val key: String, val label: String) {
    JIGSAW("JIGSAW", "Quebra-cabeça"),
    MAHJONG("MAHJONG", "Mahjong"),
    MATCH3("MATCH3", "Combinar gemas"),
    COLOR_SORT("COLOR_SORT", "Organizar cores");

    companion object {
        fun fromKey(key: String): GameType? = entries.find { it.key == key }
    }
}
