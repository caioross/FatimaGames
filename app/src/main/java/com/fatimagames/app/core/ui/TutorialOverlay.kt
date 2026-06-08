package com.fatimagames.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography

data class TutorialStep(
    val title: String,
    val body: String,
)

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    var index by remember { mutableIntStateOf(0) }
    val current = steps[index]
    val isLast = index == steps.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(theme.radii.xl))
                .background(theme.color.bgSurface)
                .padding(theme.spacing.lg),
        ) {
            // Indicadores de passo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steps.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (i == index) theme.color.primary
                                else theme.color.bgSurfaceMuted
                            ),
                    )
                }
            }
            Spacer(Modifier.height(theme.spacing.md))
            Text(current.title, color = theme.color.textPrimary, style = typo.titleLg)
            Spacer(Modifier.height(theme.spacing.xs))
            Text(current.body, color = theme.color.textSecondary, style = typo.bodyLg)
            Spacer(Modifier.height(theme.spacing.lg))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.spacing.sm)) {
                TertiaryButton(
                    text = "Pular",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = if (isLast) "Começar" else "Próximo",
                    onClick = {
                        if (isLast) onDismiss() else index++
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

object TutorialContent {
    val jigsaw = listOf(
        TutorialStep("Bem-vinda ao quebra-cabeça", "As peças começam soltas na 'mesa'. Você escolhe a foto e quantas peças quer."),
        TutorialStep("Arraste para encaixar", "Toque e arraste uma peça. Quando ela chegar perto da posição certa, ela 'cola' sozinha."),
        TutorialStep("Sem pressa", "Pode pausar a qualquer momento — seu progresso fica salvo."),
    )
    val mahjong = listOf(
        TutorialStep("Mahjong solitário", "Remova todas as peças em pares iguais."),
        TutorialStep("Peças livres", "Uma peça é 'livre' quando não tem peça em cima E tem ao menos um lado livre."),
        TutorialStep("Quando travar", "Use 'Dica' para sugestões, 'Desfazer' para voltar, ou 'Embaralhar'."),
    )
    val match3 = listOf(
        TutorialStep("Combine 3 ou mais", "Toque em uma gema, depois numa vizinha. Se formar 3 iguais em linha, somam pontos."),
        TutorialStep("Combos especiais", "Combinar 4 ou 5 em linha cria gemas especiais que limpam linha, coluna ou área."),
        TutorialStep("Sem limite", "Não tem pressa nem fim — jogue no seu ritmo."),
    )
    val colorSort = listOf(
        TutorialStep("Organize os tubos", "O objetivo é deixar cada tubo com uma só cor."),
        TutorialStep("Como mover", "Toque num tubo (ele sobe), depois no destino. A cor do topo passa se o destino tiver mesma cor ou estiver vazio."),
        TutorialStep("Ajudas", "Pode desfazer quantas vezes quiser ou pedir um tubo extra."),
    )
    val solitaire = listOf(
        TutorialStep("Paciência clássica", "Mova todas as cartas para as 4 pilhas de cima (foundations), em ordem do Ás ao Rei, por naipe."),
        TutorialStep("Como mover", "Toque numa carta para selecionar. Toque na pilha de destino. No tableau, alterna cores e desce em ordem (Rei até Ás)."),
        TutorialStep("Stock", "Toque na pilha do canto para virar a próxima carta. Quando acabar, toque de novo para reciclar."),
    )
    val minesweeper = listOf(
        TutorialStep("Encontre as minas", "Toque numa célula para revelar. Números indicam quantas minas estão ao redor."),
        TutorialStep("Marcar bandeira", "Mantenha pressionado para colocar uma bandeira numa suspeita. Ou use o botão 'Modo bandeira'."),
        TutorialStep("A primeira é segura", "Sua primeira jogada nunca cai numa mina — você pode começar tranquila."),
    )
    val tetris = listOf(
        TutorialStep("Empilhe peças", "As peças caem do topo. Complete uma linha inteira para limpá-la e ganhar pontos."),
        TutorialStep("Controles", "Use os botões abaixo: ←→ move, girar gira, descer acelera, ▼ derruba até o chão."),
        TutorialStep("Atalhos", "Toque na tela = girar. Arraste para mover. Duplo toque = derrubar imediatamente."),
    )
    val frogger = listOf(
        TutorialStep("Atravesse com cuidado", "Use o D-pad para mover o sapo. Cuidado com os carros!"),
        TutorialStep("Rio", "Pra atravessar o rio, pule em cima dos troncos. Cair na água = perde uma vida."),
        TutorialStep("Objetivo", "Complete 5 travessias até o topo para vencer. Você tem 3 vidas."),
    )
}
