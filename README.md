# Fatima Games

Aplicativo Android nativo de jogos casuais, projetado com foco em qualidade visual, calma e acessibilidade.

| | |
|---|---|
| **Plataforma** | Android (Kotlin, Jetpack Compose) |
| **Min SDK** | 26 (Android 8.0) · **Target SDK** 34 (Android 14) |
| **Distribuição** | APK direto → Google Play Store (futuro) |
| **Estado** | Implementação polida · pronto para compilar no Android Studio |
| **Como compilar** | [BUILD.md](BUILD.md) |

## Quick start

```bash
# No Android Studio: File → Open → E:\FatimaGames → Run (▶)
# Ou via terminal (precisa de JDK 17 + Android SDK):
./gradlew assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

Para erros pequenos que aparecerem ao abrir, use Alt+Enter (Quick Fix) e me manda print do que não resolver.

## O que tem dentro

### Polimento de experiência
- Saudação dinâmica (Bom dia / tarde / noite) na Home
- Ilustrações vetoriais customizadas em cada card de jogo
- **Tema (claro / escuro / automático)** aplicado em runtime via settings
- **Escala de fonte** multiplicada globalmente (Normal / Grande / Maior)
- **Háptica integrada** com toggle (tick, snap, erro, vitória)
- **Animações suaves** em todos os jogos (spring para tubos, scale para gemas e tiles)
- **Tela de vitória** com confete + medalha + cards de estatísticas
- **Tutorial overlay** com conteúdo pronto para os 4 jogos
- **Estado vazio** bonito reutilizável
- **Confirmação de saída** com dialog reutilizável

### Jogos
- **Jigsaw**: slicer com cubic Bézier (formato clássico tab/slot), drag/drop com snap entre vizinhas, agrupamento union-find, vitória
- **Mahjong**: layout tartaruga, isFree correto, match com equivalência Flores/Estações, Dica/Desfazer/Embaralhar, **auto-save funcional**
- **Match-3**: grid 8×8, swap, cascade gravidade, **gemas especiais** (Flame H/V, Bomb) geradas em matches 4+/5+, pontuação por fase
- **Color Sort**: **30 fases procedurais geradas com solver reverso** (sempre solucionáveis), transferência respeitando regras, undo, tubo extra

### Persistência
- **Room** com 4 entities (records, game state, fotos, settings)
- **DataStore** para configurações + flags de tutoriais vistos
- **Continue Banner** na home quando há partida salva
- **Stats** com mini gráfico de barras por jogo

### Acessibilidade
- Touch targets 44dp+ para circular buttons, 56-72dp para botões principais
- Textos mínimo 16sp, configurável até 1.30x
- Modo escuro 100% funcional
- Contraste AA mínimo

## Documentação

- [PRD](docs/01-product/PRD.md) · [Personas](docs/01-product/personas.md)
- [Design System](docs/02-design/design-system.md) · [UX & a11y](docs/02-design/ux-principles.md)
- [Arquitetura](docs/03-architecture/architecture.md)
- [Spec do Jigsaw](docs/04-games/jigsaw.md) · [Mahjong](docs/04-games/mahjong.md) · [Match-3](docs/04-games/match3.md) · [Color Sort](docs/04-games/color-sort.md)
- [Padrões + ADRs](docs/05-engineering/standards.md) · [Roadmap](docs/06-roadmap.md)

## Estrutura

```
FatimaGames/
├── README.md · BUILD.md · settings.gradle.kts · build.gradle.kts · gradle.properties
├── gradle/ (libs.versions.toml, wrapper)
├── gradlew · gradlew.bat
├── docs/                          # documentação do produto
└── app/
    ├── build.gradle.kts · proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/fatimagames/app/    # ~60 arquivos Kotlin
        └── res/                          # strings, colors, themes, drawables, icons
```

## Princípios não-negociáveis

1. Zero monetização agressiva — sem anúncios, sem IAP
2. Acessibilidade-first — touch targets generosos, contraste alto, fontes legíveis
3. Trabalho nunca se perde — auto-save em jogos longos
4. Visual coeso — uma paleta, dois fonts (placeholder), um sistema de motion
5. Performance > efeito — 60fps no device-alvo é regra
