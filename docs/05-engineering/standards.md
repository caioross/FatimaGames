# Padrões de Engenharia — Fatima Games

**Versão**: 1.0

---

## 1. Estilo de código Kotlin

- **ktlint** (configuração: `ktlint_official`) + **detekt**
- Identação 2 espaços (não 4) — alinhado com a configuração interna
- Trailing comma em parâmetros multi-linha
- Strings com `"""` triple-quote para SQL e JSON literal
- Sem comentários comentando o óbvio. Comentários explicam **porquê**, não **o quê**.
- KDoc em todo símbolo público de `core/` e `domain/`

## 2. Estrutura de arquivos

- Um arquivo por conceito principal (classe / objeto / função top-level)
- Nome do arquivo = nome do tipo principal
- Sub-tipos privados no mesmo arquivo se forem coesos

## 3. Naming

| Categoria | Convenção | Exemplo |
|---|---|---|
| Classes/Objects | `PascalCase` | `JigsawSlicer` |
| Funções, propriedades | `camelCase` | `generatePieces` |
| Constantes top-level | `UPPER_SNAKE` | `DEFAULT_SNAP_TOLERANCE_DP` |
| Composable | `PascalCase` | `GameCard` |
| ViewModels | `XxxViewModel` | `JigsawViewModel` |
| UI states | `XxxUiState` | `JigsawUiState` |
| Use cases | `VerbXxxUseCase` | `LoadJigsawSnapshotUseCase` |
| Repositórios (interface) | `XxxRepository` | `RecordRepository` |
| Repositórios (impl) | `XxxRepositoryImpl` | `RecordRepositoryImpl` |

## 4. Padrões de ViewModel

- ViewModel expõe **um único** `StateFlow<XxxUiState>` para o estado da tela
- Para eventos one-shot (toast, navegação, vitória) expõe `SharedFlow<XxxEvent>` (`replay=0`)
- Não expor `Flow` interno bruto; tudo passa por transformação `combine`/`map`
- Use cases injetados; ViewModel não chama Repository direto

```kotlin
@HiltViewModel
class JigsawViewModel @Inject constructor(
  private val loadSnapshot: LoadJigsawSnapshotUseCase,
  private val saveSnapshot: SaveJigsawSnapshotUseCase,
  private val finalizeRecord: SaveRecordUseCase,
  savedState: SavedStateHandle
) : ViewModel() {

  private val _uiState = MutableStateFlow(JigsawUiState.Loading)
  val uiState: StateFlow<JigsawUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<JigsawEvent>(replay = 0)
  val events: SharedFlow<JigsawEvent> = _events.asSharedFlow()

  // ...
}
```

## 5. Imutabilidade

- `data class` para estado
- `val` em propriedades, exceto onde há razão clara para `var`
- Operações em estado retornam novos objetos (não mutam in-place), salvo otimização explícita em hot paths

## 6. Threading

- UI em `Dispatchers.Main`
- DB/disco em `Dispatchers.IO`
- CPU intensivo em `Dispatchers.Default`
- ViewModels usam `viewModelScope`
- Use `withContext(Dispatchers.X)` em vez de `launch` quando o retorno é necessário

## 7. Tratamento de erros

- Use cases retornam `Result<T>` quando podem falhar de modo recuperável
- Exceções são para condições verdadeiramente excepcionais (programming errors)
- ViewModel converte `Result.failure` em `UiState.Error(...)` com mensagem amigável

## 8. Composables

- Sem lógica de negócio em composables — só rendering e dispatch de intents
- Parâmetros: `state`, `onAction: (Action) -> Unit`, `modifier: Modifier = Modifier`
- Toda recomposição quente (>5 vezes por interação) deve ser perfilada
- `remember { ... }` para cálculos não-triviais; `derivedStateOf` para dependências
- `LaunchedEffect(key)` com keys explícitas — nunca `Unit` sem justificativa
- `key` em listas (`LazyColumn`/`LazyRow`) sempre

## 9. Tema e tokens

- **Proibido** `Color(0xFF...)`, `dp(8)`, etc. soltos em componentes
- Tudo via `LocalAppTheme.current.color.x` / `.spacing.x` / `.typography.x`
- Adicionar novo token = adicionar em `theme/Tokens.kt` e documentar em `design-system.md`

## 10. Performance — checklist

Antes de mergear feature com renderização custom:

- [ ] Frame timing benchmark passou (alvo 16 ms p99)
- [ ] Sem alocações em hot path (perfilado com Allocation Tracker)
- [ ] Bitmaps em cache, não regenerados a cada draw
- [ ] `drawWithCache` em modificadores onde aplicável
- [ ] LeakCanary não detectou leaks em fluxo de entrar/sair da tela 5 vezes

## 11. Testes

### 11.1 Pirâmide

- **Unit** (rápido, isolado, sem Android): toda lógica de jogo (slicer, isFree, swap, transferTube). Alvo cobertura: 85%.
- **Integration** (Robolectric ou Room in-memory): repositórios, DAOs, migrations.
- **UI** (Compose UI Test): fluxos críticos.
- **Screenshot/Snapshot** (Paparazzi): componentes do design system.
- **Macrobenchmark**: ver `architecture.md §4.5`.

### 11.2 Organização

```
src/test/java/                  # unit (JVM)
src/androidTest/java/           # instrumentado
src/screenshotTest/java/        # Paparazzi
```

### 11.3 Convenções

- Nome de teste: `metodo_estado_resultadoEsperado()`
- Use `@DisplayName` ou comentário para casos não-óbvios
- 1 assert por teste idealmente; múltiplos OK se relacionados

### 11.4 Casos de teste obrigatórios por jogo

Cada spec de jogo lista seus casos obrigatórios. PR de jogo só mergeia se todos estão presentes e passando.

## 12. CI/CD (mínimo no MVP)

GitHub Actions ou similar com 3 jobs:

1. **lint**: `./gradlew ktlintCheck detekt`
2. **test**: `./gradlew testDebugUnitTest`
3. **build**: `./gradlew assembleRelease` (artefato APK como output)

Mais robusto pós-MVP: macrobenchmark em emulador, screenshot tests, deploy automático.

## 13. Versionamento

- Semver-ish: `MAJOR.MINOR.PATCH` (ex: `1.0.0`)
- `versionCode` = inteiro monotonicamente crescente, incrementa a cada release
- `versionName` exposto em "Sobre" nas configurações

## 14. ADRs (Architecture Decision Records)

Decisões importantes registradas em `docs/05-engineering/adrs/` (futuro). Templated.

### ADRs principais (resumo)

**ADR-001 · UI com Jetpack Compose + Material 3 customizado**
- Razão: Compose é o padrão atual; declarativo facilita motion e tema custom. Material 3 como base mas com wrapper interno.

**ADR-002 · Min SDK 26**
- Cobre 98%+ do mercado; libera ImageDecoder, AdaptiveIcons, autosizing TextView, etc.

**ADR-003 · Portrait apenas no MVP**
- Reduz superfície de UI em ~30% sem perda real (todos os jogos funcionam melhor em portrait nesse formato).

**ADR-004 · Room (não SQLDelight)**
- Familiar, integração com Compose via Flow, migrations claras. SQLDelight seria upside marginal aqui.

**ADR-005 · Hilt (não Koin)**
- Geração de código em compile time captura mais erros cedo; padrão Google.

**ADR-006 · Slicer próprio em Kotlin**
- Libs existentes ou são datadas (worldsproject 2013) ou são JS-only (headbreaker). Implementação nativa é factível e dá controle visual total.

**ADR-007 · Assets Mahjong baseados em `FluffyStuff/riichi-mahjong-tiles`**
- Domínio público, vetorial, conjunto completo. Economiza semanas de design sem risco legal.

**ADR-008 · Sem analytics no MVP**
- Privacidade > telemetria. Pós-MVP avaliar opt-in.

**ADR-009 · Sem trilha sonora ambiente no MVP**
- Adiciona complexidade de licenciamento e UX (controle de volume separado). Pode entrar pós-MVP.

**ADR-010 · App single-module no MVP**
- Multi-módulo é otimização prematura abaixo de ~50k LOC.

## 15. Lista de bibliotecas (versões alvo, maio 2026)

| Lib | Grupo | Versão | Licença |
|---|---|---|---|
| Kotlin | `org.jetbrains.kotlin:kotlin-stdlib` | 2.0.20 | Apache 2.0 |
| Compose BOM | `androidx.compose:compose-bom` | 2024.09.00 ou superior | Apache 2.0 |
| Material 3 | `androidx.compose.material3:material3` | (via BOM) | Apache 2.0 |
| Activity Compose | `androidx.activity:activity-compose` | 1.9+ | Apache 2.0 |
| Navigation Compose | `androidx.navigation:navigation-compose` | 2.8+ | Apache 2.0 |
| Hilt | `com.google.dagger:hilt-android` | 2.51+ | Apache 2.0 |
| Hilt Compose | `androidx.hilt:hilt-navigation-compose` | 1.2+ | Apache 2.0 |
| Room | `androidx.room:room-runtime/ktx/compiler` | 2.6+ | Apache 2.0 |
| DataStore | `androidx.datastore:datastore-preferences` | 1.1+ | Apache 2.0 |
| Coil | `io.coil-kt:coil-compose` | 2.7+ | Apache 2.0 |
| Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8+ | Apache 2.0 |
| Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7+ | Apache 2.0 |
| Splash Screen | `androidx.core:core-splashscreen` | 1.1+ | Apache 2.0 |
| Timber | `com.jakewharton.timber:timber` | 5.0+ | Apache 2.0 |
| LeakCanary (debug) | `com.squareup.leakcanary:leakcanary-android` | 2.14+ | Apache 2.0 |
| MockK | `io.mockk:mockk` | 1.13+ | Apache 2.0 |
| Turbine | `app.cash.turbine:turbine` | 1.1+ | Apache 2.0 |
| Paparazzi | `app.cash.paparazzi:paparazzi` | 1.3+ | Apache 2.0 |

Toda dependência passa por revisão antes de entrar — licença, manutenção ativa, tamanho do APK adicionado, justificativa.

## 16. Política de revisão de PR

- Todo PR precisa: descrição clara, screenshots/vídeo de UI quando aplicável, testes adicionados, lint passando
- Squash merge para histórico limpo
- Sem PRs grandes (>500 linhas) sem justificativa; quebrar em séries menores

## 17. Logs e debugging

- **Timber** em build debug com `Timber.DebugTree`
- Em release: árvore que descarta `VERBOSE` e `DEBUG`, mantém `INFO`+ em memória rolling (últimas 200 entradas) acessível em "Sobre → Diagnóstico" para troubleshooting com a usuária
- Nenhum log em release com dados pessoais
