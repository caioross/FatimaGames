package com.fatimagames.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Intercepta o gesto/botão de voltar quando há progresso. Pede confirmação.
 *
 * Uso: embrulhar a tela de jogo. Passar `hasProgress = true` quando há partida em curso.
 */
@Composable
fun GameBackGuard(
    hasProgress: Boolean,
    onConfirmedExit: () -> Unit,
    confirmTitle: String = "Sair da partida?",
    confirmMessage: String = "Seu progresso fica salvo. Você pode voltar a qualquer momento.",
    content: @Composable () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = hasProgress) {
        showConfirm = true
    }

    content()

    if (showConfirm) {
        ConfirmExitDialog(
            title = confirmTitle,
            message = confirmMessage,
            onConfirm = {
                showConfirm = false
                onConfirmedExit()
            },
            onDismiss = { showConfirm = false },
        )
    }
}
