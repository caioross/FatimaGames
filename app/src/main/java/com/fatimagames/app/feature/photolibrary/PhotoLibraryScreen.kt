package com.fatimagames.app.feature.photolibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.ui.EmptyState
import com.fatimagames.app.core.ui.SimpleTopBar

@Composable
fun PhotoLibraryScreen(
    onBack: () -> Unit,
    onPhotoSelected: (photoId: Long) -> Unit,
) {
    val theme = LocalAppTheme.current
    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
        SimpleTopBar(title = "Minhas fotos", onBackClick = onBack)
        EmptyState(
            title = "Sem fotos por aqui",
            description = "Você pode escolher uma foto direto da galeria quando começar um quebra-cabeça.",
            icon = Icons.Outlined.PhotoLibrary,
        )
    }
}
