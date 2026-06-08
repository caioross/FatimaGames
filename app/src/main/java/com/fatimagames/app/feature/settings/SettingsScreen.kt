package com.fatimagames.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatimagames.app.core.theme.LocalAppTheme
import com.fatimagames.app.core.theme.LocalAppTypography
import com.fatimagames.app.core.ui.SimpleTopBar
import com.fatimagames.app.data.settings.AppSettings
import com.fatimagames.app.data.settings.AppSettingsStore
import com.fatimagames.app.data.settings.FontScale
import com.fatimagames.app.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: AppSettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.flow.collect { s -> _state.update { s } }
        }
    }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { store.setTheme(mode) } }
    fun setFontScale(scale: FontScale) { viewModelScope.launch { store.setFontScale(scale) } }
    fun setSounds(enabled: Boolean) { viewModelScope.launch { store.setSounds(enabled) } }
    fun setHaptics(enabled: Boolean) { viewModelScope.launch { store.setHaptics(enabled) } }
    fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { store.setReduceMotion(enabled) } }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current

    Column(modifier = Modifier.fillMaxSize().background(theme.color.bgCanvas).systemBarsPadding()) {
        SimpleTopBar(title = "Configurações", onBackClick = onBack)

        Column(modifier = Modifier.padding(theme.spacing.md)) {
            SectionTitle("Tema")
            ThemeRow(
                label = "Claro",
                selected = state.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setTheme(ThemeMode.LIGHT) }
            )
            ThemeRow(
                label = "Escuro",
                selected = state.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setTheme(ThemeMode.DARK) }
            )
            ThemeRow(
                label = "Automático",
                selected = state.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setTheme(ThemeMode.SYSTEM) }
            )

            Spacer(Modifier.height(theme.spacing.md))
            SectionTitle("Tamanho da fonte")
            FontScale.values().forEach { fs ->
                ThemeRow(
                    label = when (fs) {
                        FontScale.NORMAL -> "Normal"
                        FontScale.LARGE -> "Grande"
                        FontScale.XLARGE -> "Maior"
                    },
                    selected = state.fontScale == fs,
                    onClick = { viewModel.setFontScale(fs) }
                )
            }

            Spacer(Modifier.height(theme.spacing.md))
            SectionTitle("Outros")
            ToggleRow("Sons", state.soundsEnabled) { viewModel.setSounds(it) }
            ToggleRow("Vibração", state.hapticsEnabled) { viewModel.setHaptics(it) }
            ToggleRow("Reduzir animações", state.reduceMotion) { viewModel.setReduceMotion(it) }

            Spacer(Modifier.height(theme.spacing.lg))
            SectionTitle("Sobre")
            Column(modifier = Modifier.padding(horizontal = theme.spacing.md, vertical = theme.spacing.sm)) {
                Text(
                    "Fatima Games",
                    color = theme.color.textPrimary,
                    style = typo.titleSm,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Versão 1.0.0 · 8 jogos clássicos",
                    color = theme.color.textSecondary,
                    style = typo.labelMd,
                )
                Spacer(Modifier.height(theme.spacing.xs))
                Text(
                    "Feito com carinho. Sem anúncios, sem coleta de dados. Funciona 100% offline.",
                    color = theme.color.textSecondary,
                    style = typo.bodyMd,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Text(
        title.uppercase(),
        color = theme.color.textSecondary,
        style = typo.labelMd,
        modifier = Modifier.padding(vertical = theme.spacing.xs),
    )
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(theme.radii.md))
            .background(if (selected) theme.color.bgSurfaceMuted else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = theme.spacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = theme.color.textPrimary, style = typo.bodyLg)
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val theme = LocalAppTheme.current
    val typo = LocalAppTypography.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = theme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = theme.color.textPrimary, style = typo.bodyLg, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
