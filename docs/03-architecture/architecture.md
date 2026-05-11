# Arquitetura — Fatima Games

**Versão**: 1.0 · Documento normativo para a engenharia.

---

## 1. Stack tecnológica

### 1.1 Linguagem e plataforma

- **Linguagem**: Kotlin 2.0+ (k2 compiler)
- **JDK target**: 17
- **Min SDK**: 26 (Android 8.0, agosto 2017)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

Justificativa do min SDK 26: cobre ~98% de devices Android ativos em 2026; libera APIs modernas (ImageDecoder, AdaptiveIcons, autosizing, etc.) sem polyfills.

### 1.2 UI

- **Jetpack Compose** + **Material 3** (com tema fortemente customizado — não usamos componentes padrão sem wrapper próprio)
- **Compose Compiler**: alinhado com Kotlin 2.0
- **Compose BOM** para versionamento consolidado

### 1.3 Bibliotecas selecionadas

| Categoria | Lib | Versão alvo | Razão |
|---|---|---|---|
| DI | Hilt | 2.51+ | Padrão Android; geração de código |
| Persistência | Room | 2.6+ | SQLite ergonômico, type-safe queries |
| Navegação | Navigation Compose | 2.8+ | Type-safe routes |
| Imagem | Coil | 2.7+ | Compose-native, light, performático |
| Coroutines | kotlinx-coroutines | 1.8+ | Concorrência idiomática |
| Serialização | kotlinx-serialization-json | 1.7+ | JSON para `payload_json` em Room |
| Testes | JUnit 4, Turbine, MockK | — | — |
| UI tests | Compose UI Test | — | — |
| Lint | ktlint, detekt | — | Estilo + qualidade |

Tudo gerenciado via **Version Catalog** (`gradle/libs.versions.toml`).

### 1.4 Build

- **Gradle 8.7+** com **Kotlin DSL**
- **AGP 8.5+**
- Build types: `debug`, `release`
- Product flavors: nenhum no MVP
- Signing: chave gerada localmente para distribuição APK; planejar move para Play App Signing na fase Play Store

## 2. Arquitetura da aplicação

### 2.1 Padrão

**MVVM + Clean Architecture leve**, com três camadas lógicas:

```
┌──────────────────────────────────────────────┐
│  ui/ — Compose, ViewModels, Navigation       │
│        (depende de domain/)                  │
├──────────────────────────────────────────────┤
│  domain/ — Use cases, entidades, regras      │
│        (puro Kotlin, sem Android)            │
├──────────────────────────────────────────────┤
│  data/ — Repositórios, Room, FS, datasources │
│        (depende de domain/)                  │
└──────────────────────────────────────────────┘
```

Dependências fluem **só para baixo**: `ui` → `domain` ← `data`. Domain não conhece data nem ui.

### 2.2 Por que MVVM + Clean (e não MVI completo)?

MVI completo (com sealed Intent/State/Effect explícitos) adiciona overhead que não compensa para um app deste porte e equipe. MVVM com `StateFlow`/`SharedFlow` resolve sem cerimônia. Onde uma tela exigir reactivity complexa (jigsaw, com gestos contínuos), localmente adotamos um state-machine pequeno via `Reducer` interno ao ViewModel.

### 2.3 Estrutura de pacotes

```
com.fatimagames.app
├── FatimaGamesApp.kt                          # @HiltAndroidApp
├── MainActivity.kt
├── core
│   ├── theme/                                 # Tokens, AppTheme(), Color, Typography
│   ├── ui/                                    # Componentes compartilhados (botões, cards)
│   ├── navigation/                            # NavGraph, rotas tipadas
│   ├── di/                                    # Modules Hilt globais
│   └── util/                                  # Extensions, helpers
├── data
│   ├── db/                                    # Room: AppDatabase, DAOs, Entities
│   ├── repository/                            # Implementações de Repository
│   ├── photo/                                 # PhotoStore (filesystem)
│   └── settings/                              # AppSettingsStore (DataStore)
├── domain
│   ├── model/                                 # Entidades puras (sem @ManagedBy room)
│   ├── repository/                            # Interfaces de Repository
│   └── usecase/                               # Use cases globais (StartGame, SaveRecord, …)
└── feature
    ├── home/
    │   ├── HomeRoute.kt
    │   ├── HomeViewModel.kt
    │   └── HomeScreen.kt
    ├── stats/
    ├── settings/
    ├── photolibrary/
    └── games/
        ├── jigsaw/
        │   ├── ui/
        │   │   ├── JigsawRoute.kt
        │   │   ├── JigsawViewModel.kt
        │   │   ├── JigsawScreen.kt
        │   │   └── components/                # PieceCanvas, BoardCanvas, etc.
        │   ├── domain/
        │   │   ├── model/                     # Piece, Edge, BoardState
        │   │   ├── slicer/                    # Algoritmo de recorte
        │   │   └── usecase/
        │   └── data/                          # Persistência específica do jigsaw
        ├── mahjong/
        ├── match3/
        └── colorsort/
```

### 2.4 Multi-módulo Gradle (futuro)

No MVP, **um único módulo `:app`** para simplicidade. Quando o app crescer (≥ 6 jogos ou 50k+ LOC), refatorar para multi-módulo:
- `:core:designsystem`, `:core:database`, `:core:domain`
- `:feature:home`, `:feature:games:jigsaw`, etc.

Decisão registrada como ADR-010 (futuro).

## 3. Modelo de dados

### 3.1 Schema Room

```kotlin
@Entity(tableName = "game_record")
data class GameRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val gameType: String,         // "JIGSAW", "MAHJONG", "MATCH3", "COLOR_SORT"
  val score: Long?,             // pontos (Match-3) ou null
  val durationMs: Long,
  val difficulty: String?,      // "12P", "MEDIUM", "STAGE_07", etc.
  val finishedAt: Long,         // epoch ms
  val payloadJson: String?      // detalhes específicos do jogo (serializados)
)

@Entity(tableName = "game_state")
data class GameState(
  @PrimaryKey val gameType: String,  // 1 estado salvo por jogo
  val snapshotJson: String,
  val updatedAt: Long
)

@Entity(tableName = "user_photo")
data class UserPhoto(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val filePath: String,         // absoluto em getFilesDir()
  val thumbnailPath: String,
  val label: String?,           // opcional, ex: "Aniversário Maria 2026"
  val createdAt: Long
)

@Entity(tableName = "app_setting")
data class AppSetting(
  @PrimaryKey val key: String,
  val value: String
)
```

### 3.2 DAOs

- `GameRecordDao` — insert, query by gameType ordenado por finishedAt desc, top N por score/duration
- `GameStateDao` — upsert, get by gameType, delete
- `UserPhotoDao` — list ordered by createdAt desc, delete by id
- `AppSettingDao` — kv get/set

### 3.3 Migrações

Versão 1 = schema acima. Toda mudança de schema exige `@AutoMigration` ou migration manual; mudanças destrutivas só com migration backup-friendly.

### 3.4 Estado de partida em andamento

Cada jogo serializa seu próprio `GameStateSnapshot` (data class no domain) como JSON usando kotlinx-serialization. O ViewModel grava no Room via Repository com debounce de 1 s.

Exemplo Jigsaw:
```kotlin
@Serializable
data class JigsawSnapshot(
  val photoId: Long,
  val pieceCount: Int,
  val seed: Long,                           // reprodutibilidade do corte
  val pieces: List<JigsawPieceSnapshot>,
  val elapsedMs: Long
)

@Serializable
data class JigsawPieceSnapshot(
  val pieceId: Int,
  val xPx: Float,
  val yPx: Float,
  val groupId: Int                          // peças encaixadas compartilham groupId
)
```

### 3.5 Armazenamento de fotos do usuário

- Fotos copiadas para diretório privado do app (`getFilesDir()/photos/`)
- Thumbnail (256×256, JPEG q=85) gerado no momento da importação, armazenado em `getFilesDir()/thumbs/`
- `UserPhoto.filePath` aponta para o arquivo full-res
- Backup do dispositivo: configurar `android:allowBackup="true"` com regras de inclusão para `photos/` e Room

## 4. Performance — diretrizes técnicas

### 4.1 Renderização de jogos

| Jogo | Estratégia |
|---|---|
| Jigsaw | Compose Canvas custom; cada peça é um `ImageBitmap` cacheado; offscreen do scroll usa lazy draw |
| Mahjong | Compose Canvas custom; pré-renderizar Bitmap por tipo de peça (42 bitmaps), reusar |
| Match-3 | Compose padrão com `Box` + `Modifier.offset` animado; gemas como `Image` com VectorDrawable |
| Color Sort | Compose Canvas para tubos e líquido animado |

### 4.2 Geração das peças do jigsaw (operação pesada)

- Executada em `Dispatchers.Default` com cancelamento cooperativo
- Progresso reportado via `Flow<JigsawGenProgress>` (porcentagem)
- Resultado é uma `List<JigsawPiece>` com `ImageBitmap` por peça
- Bitmaps em formato `ARGB_8888` (qualidade) com tamanho cap de ~512 px no maior lado por peça
- Total de memória de bitmaps capada em ~80 MB; se exceder, downscale automático da imagem-fonte

### 4.3 Memória

- `Coil` configurado com `crossfade(false)`, `memoryCache(maxSizePercent = 0.20)`, `diskCache` em `getCacheDir()/coil`
- Em jogos com muitos bitmaps, recycle agressivo ao sair da tela; LeakCanary em debug

### 4.4 Cold start

Target < 1500 ms. Estratégias:
- `App Startup` Library para inicializações
- Room init lazy
- Splash via `SplashScreen` API (Android 12+) com fallback
- Sem `Application.onCreate` pesado

### 4.5 Benchmarks

Suite **Macrobenchmark**:
- `coldStartup()` — abre app, mede até home renderizada
- `jigsawGen100()` — mede tempo de geração de 100 peças
- `mahjongInit()` — abertura da partida de Mahjong
- `match3Frame()` — frame timing durante 30 s de jogo

Roda no CI em device emulado (Pixel 6 API 33).

## 5. Concorrência

- UI roda em `Dispatchers.Main`
- Disco/DB em `Dispatchers.IO`
- CPU-bound (slicer, geração de fases Color Sort) em `Dispatchers.Default`
- `viewModelScope` para coroutines de tela
- `applicationScope` (CoroutineScope custom em singleton) para work que sobrevive a navegação (ex: salvamento de record final)
- Sem `GlobalScope`

## 6. Navegação

`Navigation Compose` com rotas tipadas via `@Serializable` objects (Compose Nav 2.8+):

```kotlin
@Serializable object HomeRoute
@Serializable object StatsRoute
@Serializable object SettingsRoute
@Serializable object PhotoLibraryRoute

@Serializable data class JigsawSetupRoute(val photoId: Long? = null)
@Serializable data class JigsawGameRoute(val photoId: Long, val pieceCount: Int)
@Serializable object MahjongGameRoute
@Serializable object Match3GameRoute
@Serializable data class ColorSortGameRoute(val stage: Int)
```

NavHost central em `core/navigation/AppNavGraph.kt`.

## 7. Injeção de dependência (Hilt)

Módulos:
- `DatabaseModule` — provê `AppDatabase` e DAOs
- `RepositoryModule` — `@Binds` interfaces de domain para implementações de data
- `DispatchersModule` — provê `@Named` dispatchers para testes
- `PhotoStoreModule`

Cada feature tem ViewModels com `@HiltViewModel` e construtor injetado.

## 8. Tratamento de erros

- `Result<T>` (Kotlin built-in) como retorno de use cases que podem falhar
- Logging via Timber (debug) + crash reporting **opcional** (não habilitado no MVP por privacidade)
- UI nunca crasha por exceção não-tratada em ViewModel; ela mostra um estado de erro com ação de recuperação

## 9. Testes

| Camada | Estratégia |
|---|---|
| `domain/` (lógica de jogo) | JUnit + MockK, cobertura mínima 80% |
| Repositórios | Robolectric ou Room in-memory |
| ViewModels | Turbine + MockK |
| UI | Compose UI Test para fluxos críticos (home → jogo → vitória) |
| Snapshot | Paparazzi para componentes do design system |
| Macrobenchmark | conforme seção 4.5 |

Detalhe completo em [05-engineering/standards.md](../05-engineering/standards.md#testes).

## 10. Configurações persistentes (DataStore)

Para preferências (modo escuro, escala de fonte, sons, vibração), usamos **Preferences DataStore** em vez de Room:
- Acesso por `Flow<Preferences>`
- Não-bloqueante por design
- Migration trivial do SharedPreferences se preciso

## 11. Permissions

| Permission | Quando | Justificativa exposta ao usuário |
|---|---|---|
| `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (≤ 32) | Importar foto da galeria | "Para você escolher uma foto sua." |
| `CAMERA` | Tirar foto | "Para tirar uma foto agora." |
| `VIBRATE` | Háptica | Solicitada implicitamente; sem prompt |

Sem permission de rede (`INTERNET` não declarado).
