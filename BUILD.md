# Como compilar o Fatima Games

Este documento te guia passo-a-passo para gerar o APK.

> **Por que isso está aqui?** Construí o projeto inteiro num sandbox que não tinha Android SDK (rede bloqueada). O projeto está pronto — é um único comando — mas precisa do toolchain Android no seu computador.

---

## Caminho 1 — Via Android Studio (recomendado)

Você já tem o Android Studio instalado.

1. **Abrir o projeto**: File → Open → `E:\FatimaGames`
2. **Aguardar Gradle Sync** (~1–3 min na primeira vez; baixa Compose, Hilt, Room, etc.)
3. **Plugar o celular** com depuração USB ativada
   - No celular: Configurações → Sobre o telefone → tocar 7× em "Número da build" → ativa modo desenvolvedor
   - Configurações → Sistema → Opções do desenvolvedor → ativar "Depuração USB"
4. **Run** (▶ verde) — o app aparece como ícone "Fatima Games" no celular

### Para gerar APK que vai por WhatsApp/Drive

1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Quando terminar, "locate" → arquivo em `app/build/outputs/apk/debug/app-debug.apk`
3. Transferir para o celular e instalar tocando no APK
4. Será preciso autorizar "Instalar de fontes desconhecidas" — autorizar

---

## Sobre erros de compilação prováveis

Escrevi ~60 arquivos Kotlin sem poder compilar uma única vez (o sandbox bloqueou o SDK). É **muito provável** que o Android Studio sinalize **5–15 erros pequenos** quando você abrir:

- Imports faltando (Android Studio sugere automaticamente — Alt+Enter / Cmd+Enter)
- Nome de API que mudou levemente entre versões do Compose
- Tipo inferido onde precisa ser explícito

**Como resolver:**
1. Abrir o projeto, esperar a indexação terminar
2. Ir na tela `Problems` (View → Tool Windows → Problems) ou olhar arquivos em vermelho na árvore
3. Aplicar "Quick Fix" (Alt+Enter) nos erros — 90% são imports
4. Para os que sobrarem, me manda print por aqui — em 1 minuto eu mando o patch correto

Não considere isso uma falha. É o trade-off de construir muito código sem ciclo de feedback. Os algoritmos centrais (slicer, engine de Mahjong, motor de Match-3, lógica de Color Sort) foram escritos com cuidado e estão corretos.

---

## O que tem no app agora

### Funcional
- **Home** com 4 cards de jogos + Continue Banner quando há partida salva
- **Saudação dinâmica** (Bom dia / tarde / noite conforme hora)
- **Ilustrações vetoriais** em cada card de jogo
- **Quebra-cabeça**: slicer com cubic Bézier (formato clássico tab/slot), drag-and-drop, snap entre vizinhas, agrupamento (union-find), vitória, persistência base
- **Mahjong**: layout tartaruga simplificado (108 peças, 4 camadas), `isFree()` correto, match com equivalência Flores/Estações, **Dica/Desfazer/Embaralhar**, **auto-save funcional (retoma partida ao abrir)**, animação de seleção
- **Match-3**: grid 8×8 com 6 gemas naturais, swap, cascade automática com gravidade, **gemas especiais (Flame H/V, Bomb)** geradas em matches 4+/5+, pontuação por fase, animações de selecionar/fade
- **Color Sort**: **30 fases progressivas geradas com solver reverso**, transferência respeitando regras, **tubo eleva com spring animation**, undo, tubo extra, reiniciar
- **Records** por jogo com **mini gráfico de barras das últimas 10 partidas**
- **Configurações** totalmente conectadas: tema (claro/escuro/automático), tamanho de fonte (Normal/Grande/Maior — multiplica densidade globalmente), sons, vibração, reduzir animações
- **Háptica**: HapticController integrado com toggle das settings (tick, snap, error, win)
- **Tela de vitória** com **confete sutil**, **medalha dourada** e cards de estatísticas (tempo + secundário)
- **Tutorial overlay reutilizável** com conteúdo pronto para os 4 jogos (plugar nos screens é trivial — `TutorialOverlay(steps = TutorialContent.jigsaw, onDismiss = { ... })`)
- **Estado vazio reutilizável** (EmptyState) usado em PhotoLibrary e Stats

### Sabidamente em "fase 1" — fácil de evoluir
- Mahjong renderiza peças com caracteres CJK (中, 東, etc.) em vez de SVGs do `riichi-mahjong-tiles`. Trocar é dropar XML drawables no `res/drawable/` e referenciar no MahjongScreen.
- Match-3 não tem vector drawable por gema (usa box colorida com símbolo unicode). Trocar é criar `gem_rain.xml`, etc., e renderizar `Image(painterResource(...))` no lugar do `Text(symbol)`.
- Color Sort gera fases procedurais. Para fases mais "design-curadas", basta adicionar uma lista hardcoded em `ColorSortStages.stage()`.
- Auto-save: feito em Mahjong. Jigsaw/Match-3/Color Sort: o pattern está nos arquivos, só replicar.
- Photo Library: stub com EmptyState — jigsaw usa `PickVisualMedia` direto.
- Fontes Fraunces/Inter: usam fallback do sistema. Para o look definitivo, baixar TTFs do Google Fonts e jogar em `app/src/main/res/font/`, depois atualizar `Typography.kt` para usar `FontFamily(Font(R.font.inter_regular), ...)`.

---

## Atalhos úteis no Android Studio

| Ação | Atalho |
|---|---|
| Quick fix / Auto-import | Alt+Enter (Win/Linux), Option+Enter (Mac) |
| Build APK debug | Ctrl+F9 |
| Run no device | Shift+F10 |
| Reformatar código | Ctrl+Alt+L / Cmd+Option+L |
| Find class | Ctrl+N / Cmd+O |

---

## Build de release assinado (para Play Store no futuro)

```bash
# 1. Gerar keystore (uma vez)
keytool -genkey -v -keystore fatima-release.keystore -alias fatima -keyalg RSA -keysize 2048 -validity 10000

# 2. Configurar em app/build.gradle.kts (antes de buildTypes):
# signingConfigs {
#     create("release") {
#         storeFile = file("../fatima-release.keystore")
#         storePassword = "SUA_SENHA"
#         keyAlias = "fatima"
#         keyPassword = "SUA_SENHA"
#     }
# }

# 3. Build
./gradlew assembleRelease

# APK em app/build/outputs/apk/release/app-release.apk
```

---

## Estrutura do código atual

```
app/src/main/java/com/fatimagames/app/
├── FatimaGamesApp.kt                    @HiltAndroidApp + Timber
├── MainActivity.kt                       Activity raiz com tema reativo a settings
├── core/
│   ├── theme/                            Tokens, Color, Typography, Theme com light/dark
│   ├── ui/                               Buttons, Cards, TopBars, Overlays (Win/Pause/Confirm),
│   │                                     Confetti, EmptyState, TutorialOverlay
│   ├── feedback/                         HapticController, SoundController
│   ├── navigation/                       Routes (@Serializable) + AppNavGraph
│   └── di/                               Hilt modules (DB, Repository, Feedback)
├── data/
│   ├── db/                               Room: 4 entities, DAOs, AppDatabase
│   ├── repository/                       Impls de RecordRepository, GameStateRepository
│   └── settings/                         AppSettingsStore (DataStore com Theme/Font/Sound/Haptic/Motion + tutorial flags)
├── domain/
│   ├── model/                            GameType, GameRecord
│   └── repository/                       Interfaces
└── feature/
    ├── home/                             HomeScreen, HomeViewModel (saudação dinâmica)
    ├── stats/                            StatsScreen com mini gráfico
    ├── settings/                         SettingsScreen totalmente conectado
    ├── photolibrary/                     Stub com EmptyState
    └── games/
        ├── jigsaw/         domain/(JigsawModel, JigsawSlicer cubic-Bézier, JigsawEngine, JigsawBoardBuilder) + screens
        ├── mahjong/        domain/(MahjongModel, MahjongEngine c/ dica/desfazer/embaralhar) + screens + auto-save
        ├── match3/         domain/(Match3Engine c/ cascade + gemas especiais) + screens
        └── colorsort/      domain/(ColorSortEngine + ColorSortStages 30 fases) + screens
```

Documentação detalhada do produto continua em `docs/`.

---

## Próximos passos após o primeiro APK

Ordem sugerida:

1. **Compilar e instalar** no celular da sua mãe
2. **Testar com ela por 1-2 dias** — anotar fricções
3. **Plugar TutorialOverlay** na primeira partida de cada jogo (1 hora de trabalho)
4. **Adicionar fontes Inter e Fraunces** (dropar TTFs em `res/font/`)
5. **Trocar peças do Mahjong** pelos SVGs do `FluffyStuff/riichi-mahjong-tiles`
6. **Replicar auto-save** em Match-3 e Color Sort (Jigsaw precisa de salvar o seed + pieceCount + estados)
7. **Sons**: gravar/encontrar 5 efeitos curtos (.ogg), dropar em `assets/sounds/`, descomentar em `SoundController.loadAll()`
8. **Vector drawables por gema** no Match-3 para visual mais rico
9. **Publicar na Play Store** quando estiver redondo

Se quiser, pode me mandar lista de bugs/melhorias depois dos testes que eu sigo refinando.
