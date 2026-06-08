# Relatório V2 — Auditoria UI/UX Profunda

**Versão**: 2.0 · **Data**: 2026-05-12 · **Sucessor de**: AUDIT_REPORT.md

Este V2 é um pente-fino real após segunda iteração. Cobre detalhes visuais, microinterações, edge cases, system insets, comportamento de Compose, conflitos de gesto, e o sentimento geral de "acabamento amador" vs "produto polido".

Total: **127 itens novos** organizados por categoria.

---

## 0. Padrões transversais (afetam MUITAS telas)

### V001 🔴 System insets ignorados em telas-raiz
`enableEdgeToEdge()` é chamado no MainActivity. Telas devem aplicar `Modifier.windowInsetsPadding(WindowInsets.systemBars)` na raiz para evitar:
- Top bar grudando no status bar (ou ficando atrás dele)
- Bottom controls cortados pela barra de gestos do Android

**Onde**: TODAS as 8 game screens, Home, Settings, Stats, PhotoLibrary.

**Fix**: criar `Modifier.fillMaxSize().systemBarsPadding()` no Column externo de cada Screen.

### V002 🔴 Hit-test no Jigsaw após zoom não dá refresh
Quando o usuário aplica pinch zoom, scale muda, o callback é fechado em `awaitEachGesture`. A captura inicial de `localOffset = firstDown.position` é correta APÓS a transform (Compose corrige). Mas o `dragId` setado no início persiste mesmo se zoom mudar. Edge case raro mas possível.

### V003 🟠 Ripple effect ausente em botões críticos
Vários `Box.clickable(...)` sem `indication = LocalIndication.current`. Visual feedback no toque é fraco em:
- GameCard (Home)
- ThemeRow / ToggleRow (Settings)
- ActionButton (todos os jogos)
- DPadButton (Frogger)

### V004 🟠 Animações simultâneas começam no mesmo momento
Ao spawnar gemas/peças/etc., todas iniciam scale=0 → 1 sincronizadamente. Visualmente robótico. Adicionar **stagger** (delay 30-50ms por elemento).

### V005 🟠 Falta loading state em transições
Navegar entre telas é instantâneo (default do Navigation Compose), mas algumas telas têm work async (foto carregando, gerando peças). Adicionar shimmer ou skeleton.

### V006 🟡 Tipografia inconsistente — alguns Text sem usar tokens
Vários `Text("...", fontSize = 14.sp)` literais em vez de `style = typo.labelMd`. Compromete consistência.

### V007 🟡 Strings hardcoded em PT-BR no Kotlin
Inúmeros literais. Aceitável agora, mas catalogado para futura i18n.

### V008 🟡 Padding lateral inconsistente
Algumas telas usam `theme.spacing.md` (16dp), outras `theme.spacing.lg` (24dp). Padronizar para `space.lg` lateral global.

### V009 🟡 Estados disabled visuais fracos
Buttons com `enabled = false` apenas reduzem opacity. Falta indicação clara (ex: cinza saturado).

### V010 🟡 contentDescription faltando
Auditar todos os `Icon(...)` e `Image(...)`. Pelo menos 30 instâncias sem.

### V011 🟡 Sombras inconsistentes
Cards usam border 0.5dp + bg, sem shadow. Outros usam elevation. Padronizar.

### V012 🟡 Snackbars/Toasts ausentes
Erros não recuperáveis ou avisos importantes (ex: foto muito grande) silenciosos.

### V013 🟡 Configuração de tema não tem transição animada
Trocar light↔dark é abrupto. Compose default não anima cores. Adicionar `animateColorAsState` no theme provider.

### V014 🟡 Splash screen muito breve / sem branding
Apenas mostra ícone genérico por 1.5s. Sem nome, sem versão.

### V015 🟡 Scaffold structure missing
Compose tem `Scaffold` que cuida de top bar + bottom bar + content. Estamos usando Column manual. Trade-off OK mas perdemos benefícios de inset/snackbar host.

---

## 1. Jigsaw — UI/UX pente fino

### V101 🔴 dragId persiste após snap automático
Quando uma peça encaixa via snap, `dragId` continua apontando para ela. Próximo `onDrag` move o grupo todo. OK se intencional, mas pode parecer "grudou no dedo após encaixar".

**Fix**: limpar `dragId` se a peça encaixou (groupId mudou).

### V102 🔴 Hit-test após múltiplas peças encaixadas
Quando uma peça pertence a um grupo grande, hit-testar nela com bbox + alpha verifica só essa peça individualmente. Mas o grupo deveria ser draggable como um todo. Atual: pega a peça do topo do grupo no hit, arrasta o grupo via `moveBy`. OK.

### V103 🟠 Sombra da peça arrastada usa extractAlpha()
`bmp.asAndroidBitmap().extractAlpha()` cria um novo bitmap a cada frame de drag. **Memory churn massivo**.

**Fix**: cachear o alpha-bitmap por piece.id e reusar.

### V104 🟠 Glow no encaixe usa mesma técnica → mesmo problema
**Fix**: cache de alpha bitmaps.

### V105 🟠 Inicial scatter sobrepondo no topo da tela
A função distribui em jittered grid mas o `gridCols` pode dar muitos cols pequenos. Peças no topo podem coincidir com top bar.

**Fix**: limit gridCols mínimo + garantir scatterAreaTop respeita status bar / top bar (sub. 96dp).

### V106 🟠 Sem indicador visual de área-alvo
Como o usuário sabe onde a imagem completa vai? Adicionar contorno tracejado (fantasma) da imagem-alvo.

### V107 🟠 Sem botão "ver imagem completa" como hint
Plugar um botão temporário (toggle) que mostra a foto-alvo opaca por trás.

### V108 🟠 Zoom-out limit 0.5× pode esconder peças
Em scale 0.5×, peças ficam tão pequenas que mal dá pra clicar. Permitir só até 0.75×.

### V109 🟠 Zoom-in limit 3× pode perder peças fora do viewport
Sem auto-pan ao tocar fora dos limites. Acceptable; usuário pode panar.

### V110 🟠 Rotação por pinça começa abruptamente
Mínimo de 0.05 rad de threshold. Mas ainda pode rotacionar acidentalmente quando o usuário só queria mover. Aumentar threshold para 0.1.

### V111 🟠 Snap só com rotation ≈ 0° é rígido
Talvez aceitar rotações múltiplas de 90°. Fora do MVP.

### V112 🟡 Subtítulo "X / Y peças" troca de número via `distinctGroups`
Isso conta GRUPOS, não peças encaixadas. Cada peça solta é seu próprio grupo. 24 peças total - 1 grupo gigante = 23 grupos → "1 encaixada". Errado matematicamente.

**Fix**: contar peças cujo groupId != pieceId (i.e., faz parte de grupo unificado).

### V113 🟡 Setup screen: foto preview mostra URI bruta como texto
Quando `selectedPhotoUri != null`, mostra `"Foto pronta · $selectedPhotoUri"` — URI gigante feio. Substituir por AsyncImage real (já feito numa versão, verificar se ficou).

### V114 🟡 Setup screen: difficulty grid uses `.weight(1f)` em Box que pode quebrar
Box dentro de Row com weight só funciona se Row tiver `.fillMaxWidth()`. Sim, está. OK.

### V115 🟡 Loading state ("Preparando peças…") sem progress indicator
Apenas texto. Adicionar CircularProgressIndicator.

### V116 🟡 Glow dourado pode ofuscar em peças claras
Cor fixa #D4A24A. Em peças de imagem branca/clara, glow pouco visível. Usar cor complementar do bitmap dominante (avançado, pós-MVP).

### V117 🟡 Win overlay aparece imediatamente quando última peça encaixa
Sem "savoring time". Esperar 600ms vendo a imagem completa antes do overlay.

### V118 🟡 Não há contagem regressiva nem peças totais visíveis em destaque
Apenas no subtítulo, pequeno. Adicionar HUD pequeno.

---

## 2. Mahjong — UI/UX pente fino

### V201 🔴 Tile sizing math usa `.dp.value` perdendo precisão
`((tile.col - minCol).toFloat() / 2f) * tileW.value - tile.layer * layerOffsetX.value` — extrai value como Float, depois aplica `.dp` no final. Funciona mas pode ter rounding errors em phones com density estranha.

**Fix**: manipular em pixels via `with(LocalDensity.current) { ... }` ou usar `Modifier.layout` para posicionar.

### V202 🔴 Bounding box pode ficar maior que viewport disponível
`BoxWithConstraints { maxW, maxH }` lê constraints. Calcula `tileW = listOf(byW, byH, 60.dp).min()`. Mas se constraints permitem que o board cresça > maxHeight (ex: scrollable container parent), `maxH = Infinity`. Falha.

**Fix**: garantir parent não-scrollable do BoxWithConstraints.

### V203 🟠 Tartaruga simplificada (108 tiles) pode ser injogável
Layout custom sem testes de balance. Algumas configurações de embaralho podem dar 0 movimentos no início.

**Fix**: gerar embaralho que garanta pelo menos 5 movimentos válidos iniciais (validate).

### V204 🟠 Selected + Hinted simultâneo: visual confuso
Se uma peça selecionada também é parte da hint pair, scale = 1.08 vs 1.04 — selected vence. Mas a borda dourada de hinted é mais larga. Pode ficar borrado.

**Fix**: priorizar selected, suprimir hint visual quando selecionado.

### V205 🟠 Tiles bloqueadas (não livres) ficam ligeiramente dim — discreto demais
Cor `IvoryFaceDim` muito próxima de FREE. Reforçar diferença OU adicionar overlay sutil.

### V206 🟠 Tile "scaling" no selected escala desde o canto, não centro
`graphicsLayer { scaleX = scale; scaleY = scale }` escala do origin (top-left por padrão). Resultado: tile cresce pra direita/baixo, parecendo "deslizar".

**Fix**: setar `transformOrigin = TransformOrigin(0.5f, 0.5f)`.

### V207 🟠 Animação de match (remover) instantânea
Tile vai pra `removed = true` no engine, recompose sem ela. Nenhum fade.

**Fix**: estado intermediário "dissolving" antes de remover.

### V208 🟡 Hint só pisca a primeira vez? — verificar
A função `useHint()` decrementa contador e retorna par. Mas a animação visual não é "piscar", é só border dourada constante até desselecionar.

**Fix**: borda dourada deve pulsar (alpha animado em loop) por 2-3 segundos, depois sumir.

### V209 🟡 Reset/restart sem animação
Tiles desaparecem e reaparecem na nova posição. Adicionar fade-out + fade-in.

### V210 🟡 Top bar score é tempo (`formatTime`) — não score real
Mahjong tem score interno (penalidades). Mostrar no win overlay, não no top bar.

### V211 🟡 Tiles depth uses `tileDepth = tileW * depthRatio` = ~6dp em phone normal
OK visualmente mas pode parecer cartoon. Ajustar para 4dp.

### V212 🟡 Dragão branco (moldura) sem desenho central
Apenas dois retângulos concêntricos. Tradicional Mahjong tem o naipe interno. OK pra MVP.

### V213 🟡 ActionButtons (Desfazer/Dica/Embaralhar) sem indicação de usos restantes
Engine tem `hintsUsed`, `undosUsed`, `reshufflesUsed`. UI deveria mostrar "Dica (3)" decrementando.

---

## 3. Match-3 — UI/UX pente fino

### V301 🔴 Swap animation: dx/dy podem grudar em valores stale
Quando swap inválido faz vai-e-volta, o `swapping` state é nullado depois do retorno. Mas `animateFloatAsState` pode estar com targetValue ainda inválido se recompose acontecer no meio.

**Fix**: usar `Animatable` em vez de `animateFloatAsState` quando o target depende de estado complexo.

### V302 🔴 GemCellAt usa `posR/posC * cellSize.value` mas position é fixa
Cada cell é posicionada via `offset(x, y)` calculado. Se cellSize mudar (ex: rotação), recompõe mas animação não interpola.

**Fix**: usar `Modifier.layout` ou seguir um `Modifier.offset { IntOffset(...) }` em px.

### V303 🟠 FloatingPoints animation triggera ao compor — não recompõe para novos floaters
Cada `for (f in state.floaters)` cria nova instância — animação roda 1x.
Mas se a lista mudar (floater removida), a key implícita do Compose pode reusar instância.

**Fix**: `for (f in state.floaters) { key(f.id) { FloatingPoints(...) } }`.

### V304 🟠 Cascade visual instantânea
Após swap match, gemas explodem + caem em 0ms — engine roda todo cascade em loop sem pausas. UI vê só estado final.

**Fix**: ViewModel deveria pausar entre níveis de cascade. Mostra explosão (200ms) → queda (300ms) → próximo nível. Refactor da Match3Engine pra retornar **série de transições**.

### V305 🟠 Selected gem visual ambiguo com gemas vizinhas
Border dourada 2.5dp + scale 1.12 pode "vazar" sobre gemas adjacentes.

**Fix**: adicionar pequena margem entre cells (já tem 1dp padding, aumentar para 2dp).

### V306 🟠 Gem renderer drawRaindrop, drawLeaf, etc. desenha sobre fundo cinza
GemCell tem `background = bgSurface.copy(alpha = 0.20)`. A gota azul sobre cinza claro tem contraste OK, mas a lua roxa sobre fundo claro pode ficar pálida.

**Fix**: background mais escuro/translúcido OU sem background (sólida vazia).

### V307 🟠 Sem indicador "objetivo" da fase
Stage 1 = atingir 5000 pts. Mas usuário não sabe. Mostrar progress bar "1234 / 5000".

### V308 🟠 Sem detecção de deadlock no UI
Engine não tem deadlock detection. Se board tem 0 swaps válidos, jogo trava. Adicionar auto-shuffle.

### V309 🟡 Score popup `+30`, `+60` etc. — color sempre gold
Faria sentido cor diferente por tamanho de match (3=gold, 4=terracotta, 5=plum).

### V310 🟡 Special gems (Flame, Bomb) com símbolo `⟷` `✺` pode não renderizar
Caracteres unicode dependem de fonte. Substituir por Canvas paths ou ícones.

### V311 🟡 Sem celebração ao concluir fase
Fase X atingida → stage++ silencioso. Adicionar overlay "Fase 2!" antes de continuar.

### V312 🟡 inputLocked durante animação esconde feedback visual de seleção
Usuário toca gema, fica "esperando" — porque inputLocked. Sem visual indicando "espera, animando…".

### V313 🟡 Top bar score: format `%,d` em locale BR usa ponto, não vírgula — verificar
`String.format("%,d", 12345)` em locale BR retorna "12.345". OK.

---

## 4. Color Sort — UI/UX pente fino

### V401 🔴 Tube tilt rotaciona em torno do centro
`rotationZ = tiltDeg` com pivô default = center. Visualmente parece o tubo flutuando inclinado, não derramando.

**Fix**: `graphicsLayer { rotationZ = tiltDeg; transformOrigin = TransformOrigin(0.85f, 0.5f) }` — pivô perto da boca direita.

### V402 🔴 Pour não mostra líquido caindo
Atualmente: tube tilta, depois engine.transfer() executa (visualmente: líquido teleporta).

**Fix**: animar uma "gota" colorida do bico da source ao bico da dest com arco parabólico.

### V403 🟠 Tubo selecionado lifts 18dp mas pode sobrepor o tubo acima na linha 2
FlowRow com vertical spacing 14dp. Lift -18dp → overlap de 4dp.

**Fix**: aumentar vertical spacing para 24dp.

### V404 🟠 Win overlay sem celebração específica de "tubos preenchidos"
Tubos sortados ficam estáticos enquanto WinOverlay aparece. Glow dourado nos tubos antes do overlay.

### V405 🟠 Undo não inverte animação
Faz reverso instantâneo (sem tilt). Inconsistente.

### V406 🟠 Add tube botão sempre habilitado
Mesmo após usar 1 (limite). Engine permite ilimitado, contradizendo spec.

**Fix**: tracker no engine + UI disabled.

### V407 🟡 GlassTube sem reflexo dinâmico
Highlight estático. Pode parecer pintura, não vidro. Adicionar leve gradient.

### V408 🟡 "Fase X · Y movimentos" — sem indicador de progresso (de 30 fases)
Adicionar "X / 30".

### V409 🟡 Action buttons label long em portuguese ("Tubo extra")
Pode truncar em phones estreitos. Verificar.

### V410 🟡 ColorSortViewModel.initialize() recarrega snapshot só na primeira chamada
Se mudar de stage (via onNextStage), `if (this.stage != stage)` reentra. Mas o snapshot carregado depende do stage — pode confundir. OK na prática.

---

## 5. Solitaire — UI/UX pente fino

### V501 🔴 Sem drag-and-drop (decisão pendente, dificulta UX)
Tap-tap é OK mas Solitaire físico é drag. Maioria espera.

### V502 🔴 Cards 46dp largura é pequeno demais
7 colunas × 46dp = 322dp. Phones de 360dp permitem 51dp. Cards seriam mais legíveis.

**Fix**: `cardW = (maxWidth - paddings) / 7` via BoxWithConstraints.

### V503 🟠 TableauColumn height = `cardH + verticalGap * (cards.size - 1)` mas pode ser negativo
`cards.size.coerceAtLeast(1) - 1).coerceAtLeast(0) = 0` quando size=0. OK.
Mas size=1 → height = 66dp. Single card. OK.
Size=13 → height = 66 + 18*12 = 282dp. Plus other rows = >450dp. Scroll vertical existe (good), mas top row pode rolar para fora também — quer manter top bar fixa.

**Fix**: separar Top Row da scrollable area.

### V504 🟠 Card.faceUp = false (back) tem desenho simples (♣ unicode)
Em algumas fontes ♣ fica branco-feio. Usar drawable vetorial.

### V505 🟠 Sem hover/lift quando carta selecionada
Apenas border dourado. Adicionar `offset(y = -8.dp)` para selected.

### V506 🟠 Sem undo
Klondike clássico tem undo. Engine não suporta atual. Adicionar history stack.

### V507 🟠 Sem auto-flip de carta no topo do tableau após mover sequência
Engine faz isso (`flipTopIfNeeded`), mas sem animação visual.

### V508 🟡 Stock vazio depois de reciclar mostra placeholder
Mas placeholder igual ao "Foundation empty" — confunde.

### V509 🟡 Foundations não diferenciam naipes vazios
Visualmente igual. Adicionar pequeno símbolo do naipe esperado (♠ vazio na primeira foundation, etc.).

### V510 🟡 Sem score
Klondike tem score Vegas, score standard. Adicionar simples (+10 por carta na foundation).

### V511 🟡 Time tracking sem início de partida visível
Começa no init, OK.

### V512 🟡 Sem botão restart no top bar
Apenas via WinOverlay. Adicionar menu kebab.

---

## 6. Minesweeper — UI/UX pente fino

### V601 🔴 Cells HARD (16×12) podem ficar pequenas demais em phones
Phone 360dp / 12 = 30dp por cell. Tocável mas apertado.

**Fix**: paisagem aceitável? OR scale cells differently OR limit HARD to tablets.

### V602 🟠 Long-press para flag não tem tooltip / discovery
Toggle de modo bandeira ajuda mas usuário precisa descobrir.

**Fix**: primeira partida abre tutorial (já feito) + dica permanente no top bar tipo "Mantenha pressionado para 🚩".

### V603 🟠 Cascade reveal instantâneo
Quando clica em 0-adjacent, todas as células revelam de uma só vez. Visualmente abrupto.

**Fix**: animação radial (cells próximas primeiro, depois mais distantes; ~15ms delay por anel).

### V604 🟠 Mine reveal final instantâneo
Quando perde, todas minas aparecem na hora. Animar sequencialmente.

### V605 🟠 Status `Lost(mineRow, mineCol)` guarda info, mas UI não destaca a mina explodida
**Fix**: célula da mina = vermelho mais escuro + animação.

### V606 🟡 Números 1-8 dependem de fonte do sistema
Em alguns devices ficam strange. Considerar Canvas paths.

### V607 🟡 Sem chord (double-click numa célula numerada com flags corretas revela vizinhos)
Avançado. Backlog.

### V608 🟡 Sem timer visível durante partida
Engine tracks elapsedMs. Mostrar no top bar (já tem `formatTime` mas confirme).

### V609 🟡 Botão "Reiniciar" não tem confirmação se houve progresso
Pode estar perdendo partida no meio acidentalmente.

**Fix**: usar `ConfirmExitDialog` (já existe).

---

## 7. Tetris — UI/UX pente fino

### V701 🔴 Single tap dispara rotate ANTES da swipe completar
`detectTapGestures(onTap = { rotate() })` + `detectDragGestures(...)` no mesmo Box. Conflito potencial: tap rápido seguido de drag = rotação + swipe.

**Fix**: usar APENAS detectDragGestures com handling manual:
- Se total movement < 8px = tap (rotate)
- Senão = swipe

### V702 🔴 Lock delay 500ms pode sentir "lento" — peças não travam quando o jogador queria
Trade-off: para iniciante, 500ms ajuda mover. Para avançado, irrita.

**Fix**: hard drop instantâneo (já existe), lock só após delay com aviso visual (peça começa a piscar quando próximo de travar).

### V703 🟠 Game-over overlay tem `widthIn(min=240dp, max=320dp)` — pode cortar em phones muito grandes
Aceitável; centralizado.

### V704 🟠 Ghost piece pode ficar sobreposta com current se ghostRow = curRow (peça já no chão)
Fix `if (ghostRow - cur.row >= 2)` evita isso. OK.

### V705 🟠 Line clear flash + queda de linhas acima — não há gravidade animada
Linhas piscam, depois lockPiece já executa o collapse no engine. Visual: linhas piscam, somem, board reorganizado instantâneamente.

**Fix**: animar a queda das linhas acima (200ms cada linha).

### V706 🟠 Score change não anima
Score pula de 100 → 200 instantaneamente. Adicionar `animateFloatAsState` ou similar para counting effect.

### V707 🟡 Hard drop não mostra trail visual
Peça aparece já travada. Adicionar streak transparente do start ao end position.

### V708 🟡 Level up sem celebração
Level transita silencioso.

### V709 🟡 Próxima peça preview 80dp × 28dp — apertada
Aumentar para 64dp × 64dp em layout vertical na sidebar (se houver).

### V710 🟡 Bag não persiste em snapshot
Auto-save não inclui o bag — após restore, ordem das próximas peças muda.

---

## 8. Frogger — UI/UX pente fino

### V801 🔴 Goal slots invisíveis no topo
Row 0 é safe, mas usuário não sabe que precisa chegar lá X vezes em posições específicas.

**Fix**: 5 lily pads visíveis na row 0, mudando para "✓" verde quando completados.

### V802 🔴 Sapo não rotaciona visualmente conforme direção
Move para cima/baixo/lados mas o sprite é fixo (corpo redondo). Falta indicador "olhando para cima".

**Fix**: setar rotação baseada na última direção de movimento.

### V803 🟠 Carros todos vermelhos
Visual monótono. Adicionar variedade (azul, amarelo, terracotta) — array de cores.

### V804 🟠 Logs todos marrons
Variar tons. Add water animation (linhas onduladas).

### V805 🟠 Frog landing parcial em tronco = "fora" e morre
Hit-test: `frog.colFloat + 0.4 >= start && frog.colFloat + 0.6 < start + len`. Se frog tá na borda exata, falha.

**Fix**: tolerância mais generosa OR check center em vez de canto.

### V806 🟠 Sem countdown / mensagem de "vidas restantes" claro
"3 vidas" texto pequeno no subtítulo. Adicionar 3 ❤️ visíveis.

### V807 🟠 Sem score popup ao alcançar topo (+100)
Plugar FloatingPoints.

### V808 🟡 D-pad 56dp pode ser pequeno demais para alguns
Trade-off entre footprint e tamanho. OK.

### V809 🟡 Sem trilha sonora ambiente (rio + cars)
Backlog.

### V810 🟡 Frog colFloat pode sair de bounds (logs carregando-o pra fora)
Engine detecta como "DEAD_WATER". Mas visualmente sapo aparece flutuando antes de morrer. Pequeno bug.

---

## 9. Home — UI/UX pente fino

### V901 🟠 8 cards em grid 2-col = 4 linhas = scroll
Em phones, ocupa muito espaço vertical. Considerar 3-col? Cards menores?

### V902 🟠 Continue Banner apenas para "most recent"
Se a usuária tem partidas pausadas em vários jogos, só vê o último.

**Fix**: lista expansível "Continuar de onde parou" com top 3.

### V903 🟡 Saudação dinâmica não considera nome do usuário
"Bom dia" generico. Adicionar config para nome ("Bom dia, Fátima").

### V904 🟡 Top bar sem padding superior para status bar
**Fix**: V001.

### V905 🟡 GameCard subtitle muito apagado (textSecondary)
Pouco contraste para olhos cansados.

---

## 10. Settings — UI/UX pente fino

### V1001 🟠 ThemeRow não tem indicador visual claro de seleção
Apenas background tint. Adicionar checkmark à direita.

### V1002 🟠 ToggleRow Switch padrão Material 3 — cor não combina com tema
**Fix**: configurar `SwitchColors` via parâmetro.

### V1003 🟡 Sem botão "Limpar dados" / "Resetar progresso"
Backlog mas útil.

### V1004 🟡 Versão hardcoded "1.0.0" — usar BuildConfig.VERSION_NAME
**Fix**.

### V1005 🟡 Sem link para política de privacidade (curta)
Mesmo offline, declarar "não coletamos nada" é importante.

---

## 11. Stats / Records — UI/UX pente fino

### V1101 🟠 Sem filtro por jogo
Lista todos misturados. Adicionar tabs ou chips.

### V1102 🟠 Mini graph sem eixos / labels
Barras coloridas mas usuário não sabe escala.

### V1103 🟡 EmptyState chato
"Sem partidas ainda" sem ilustração calorosa.

### V1104 🟡 Sem export de records
Backlog.

---

## 12. PhotoLibrary — UI/UX pente fino

### V1201 🟠 Stub atual diz "Em desenvolvimento" — quebra confiança
Mostra EmptyState bonito agora. Verificar texto.

### V1202 🟠 Sem fluxo real (importar, listar, deletar)
Aceitável para MVP, mas elimina o diferencial do Jigsaw com fotos próprias.

**Fix**: implementar UserPhotoStore + grid + delete + integração com Jigsaw setup.

---

## Plano de execução priorizado

Em ordem de impacto:

### Onda L (Urgente — bugs visíveis em todas as telas)
- V001: system insets em todas as 11 telas (Home, Stats, Settings, PhotoLibrary, 8 games, Setup)
- V112: contagem correta de "peças encaixadas" no Jigsaw
- V206: TransformOrigin centro em Mahjong scale
- V401: Color Sort tilt pivô correto
- V701: Tetris tap/swipe conflict
- V801/V802: Frogger goal pads + rotação do sapo

### Onda M (Animações críticas)
- V103/V104: Jigsaw bitmap cache (memory churn)
- V207/V209: Mahjong dissolve + restart animation
- V304: Match-3 cascade animado com pausas
- V402: Color Sort pour com gota
- V603/V604: Minesweeper reveal + mine animation
- V705/V706: Tetris row collapse + score counting

### Onda N (Polimento profundo)
- V003: Ripple em todos os clickables
- V010: contentDescription pente-fino
- V303: keys nos floaters
- V506: Solitaire undo
- V603/V604: Minesweeper polish

Tudo isso é executável incrementalmente.
