package com.fatimagames.app.feature.games.jigsaw

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.PrimaryButton
import com.fatimagames.app.core.ui.SecondaryButton
import com.fatimagames.app.core.ui.SimpleTopBar

private val DIFFICULTIES = listOf(12, 24, 48, 100)

@Composable
fun JigsawSetupScreen(
    onBack: () -> Unit,
    onLaunch: (photoId: Long, pieceCount: Int) -> Unit,
) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf(24) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        selectedPhotoUri = uri?.toString()
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas)) {
        SimpleTopBar(title = "Quebra-cabeça", onBackClick = onBack)

        Column(modifier = Modifier.padding(theme.spacing.lg)) {
            Text(
                "Escolha uma imagem",
                color = theme.color.textPrimary,
                style = typo.titleMd,
            )
            Spacer(Modifier.height(theme.spacing.md))

            // Photo preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(theme.radii.lg))
                    .background(theme.color.bgSurfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedPhotoUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = theme.color.textSecondary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(theme.spacing.xs))
                        Text(
                            "Selecione uma foto",
                            color = theme.color.textSecondary,
                            style = typo.bodyMd,
                        )
                    }
                } else {
                    Text(
                        "Foto pronta · $selectedPhotoUri",
                        color = theme.color.textPrimary,
                        style = typo.bodyMd,
                    )
                }
            }
            Spacer(Modifier.height(theme.spacing.md))
            SecondaryButton(
                text = "Escolher da galeria",
                onClick = {
                    pickImage.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(theme.spacing.lg))
            Text(
                "Dificuldade",
                color = theme.color.textPrimary,
                style = typo.titleMd,
            )
            Spacer(Modifier.height(theme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(theme.spacing.xs),
            ) {
                DIFFICULTIES.forEach { n ->
                    val isSel = selectedDifficulty == n
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(theme.radii.md))
                            .background(
                                if (isSel) theme.color.primary else theme.color.bgSurfaceMuted
                            )
                            .clickable { selectedDifficulty = n },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$n",
                            color = if (isSel) theme.color.textInverse else theme.color.textPrimary,
                            style = typo.labelLg,
                        )
                    }
                }
            }

            Spacer(Modifier.height(theme.spacing.xl))

            PrimaryButton(
                text = "Começar",
                onClick = {
                    // photoId temporário: 0 (representa "foto escolhida"); URI real seria
                    // armazenada antes de chegar aqui em produção. Por simplicidade do MVP,
                    // o JigsawGameScreen busca o URI via state global; para esta build, a
                    // tela de jogo demonstra o slicer com imagem padrão se photoId == 0.
                    onLaunch(0L, selectedDifficulty)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            )
        }
    }
}
