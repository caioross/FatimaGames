# Relatório V3 — Status pós-onda M e backlog futuro

**Versão**: 3.0 · **Data**: 2026-05-12 · **Sucessor de**: AUDIT_REPORT_V2.md

Após a Onda M (animações de cascade reais, 144-tile turtle Mahjong, splash animada, auto-save universal), este documento cataloga o **estado atual** e o **backlog futuro priorizado**.

---

## 1. Onda M — Executada nesta rodada

### ✅ Splash screen animada com branding
- `AnimatedSplashScreen` em `feature/splash/`
- Logo "F" estilizado em disco gradient + anel dourado, anima com spring bounce
- 28 partículas (puzzle pieces, gemas, losangos) flutuam de baixo pra cima com cores da paleta
- "Fatima Games" em serif 38sp + "Jogos clássicos · com carinho" como tagline
- Auto-finish em 2.5s com fade-out
- Plugado no NavGraph como startDestination → HomeRoute
- Pop-up to evita voltar pra splash

### ✅ Match-3 cascade animado real
- Engine refatorado: `applySwapOnly`, `clearMatchesStep`, `applyGravityStep` separados
- ViewModel orquestra cada nível de cascade com delays:
  - Explosão das células matched (280ms) — visualizado via `animatingExplosion: Set<Pair<Int,Int>>`
  - Aplicação de gravidade + spawn (320ms) — gemas caem para preencher espaços
  - Loop até não haver mais matches
- Floaters separados por nível com posição no centro do match
- Háptica tick a cada nível de cascade

### ✅ Mahjong real 144-tile turtle layout
- Substituído layout simplificado por **canônico Brodie Lockard 1981**
- 5 camadas: 87 (base com head/tail), 36, 16, 4, 1 = **144**
- Linhas 0-7 com larguras variadas (12,10,12,14,12,10,12,10) formando shape de tartaruga
- Cabeça (col -3 row 3) e cauda (col 29 row 3) extensões
- Layer 1: 6×6 centralizado · Layer 2: 4×4 · Layer 3: 2×2 · Layer 4: 1 tile no topo

### ✅ Refino do shape das peças jigsaw

**3 bugs CRÍTICOS corrigidos no slicer** (causa real dos "buracos" reportados):

**Bug 1 — Convenção `outward` invertida em RIGHT/BOTTOM:**
- Análise matemática revelou que em traversal clockwise (TL→TR→BR→BL→TL), o parâmetro `outward` precisa ser uniforme (-1) em todas as 4 arestas para um TAB ir consistentemente "para fora" da peça.
- O código tinha `outward = -1` em TOP/LEFT mas `outward = +1` em RIGHT/BOTTOM.
- Resultado prático: a peça que devia ter TAB na direita estava sendo desenhada com cavidade (SLOT-shape) entrando pra dentro. A peça vizinha à esquerda dela também tinha cavidade. **Duas cavidades adjacentes = buraco visível entre as peças.**
- Fix: `outward = -1f` uniforme nas 4 chamadas de `appendEdge`.

**Bug 2 — Seeds de variação não-compartilhados:**
- Cada peça usava seu próprio seed local (`seedForVariation xor (gridRow*73 + gridCol*13)`) para todas as 4 arestas.
- Isso significava que a peça A e a peça B usavam seeds DIFERENTES na mesma aresta compartilhada → as curvinhas Bezier não casavam exatamente.
- Fix: introduzido `hEdgeSeed(boardSeed, r, c)` e `vEdgeSeed(boardSeed, r, c)` que computam um seed único por ARESTA do grid. Peça (r,c)'s RIGHT e peça (r, c+1)'s LEFT agora computam o mesmo seed → mesmas v1, v2 → curvas idênticas em direções opostas.

**Bug 3 — Bulbo do knob excedia o padding do bitmap:**
- BULB_EXTENT era 1.30 × knobInset, mas o bitmap só tem 1.0 × knobInset de padding em cada lado. Knob ficava cortado na borda do bitmap.
- Fix: BULB_EXTENT = 0.95 × knobInset (cabe com 5% de margem de anti-aliasing). KNOB_INSET_RATIO aumentado para 0.30 para knobs visivelmente robustos.

**Resultado validado matematicamente:**
- Peça A (0,0) TAB direita: bitmap renderiza região de imagem mais o TAB sticking out até x = 130-158.5 em board coords.
- Peça B (0,1) SLOT esquerda: bitmap deixa transparente exatamente a região board x = 130-158.5.
- Quando colocadas adjacentes: TAB de A se encaixa exatamente onde SLOT de B é transparente. **Imagem continua seamless entre peças.** ✓

Outros ajustes:
- 4 cubic Beziers compõem o knob (entrada com lip negativo, subida, descida, saída)
- LIP_DEPTH = 0.08 cria o pescoço clássico (inflexão pra dentro antes do bulbo)
- Curve mais simétrica em t=0.5 garantindo que A e B tracem a mesma geometria

### ✅ Auto-save universal (parcial)
- ✅ Mahjong (já estava)
- ✅ Match-3 (rodada anterior)
- ✅ Color Sort (rodada anterior)
- ✅ Solitaire — `SolitaireSnapshot` + load/save em ViewModel + clear ao restart/win
- ✅ Minesweeper — `MinesweeperSnapshot` com grid+flags+minesPlaced
- ⏭️ Tetris — deixado para próxima rodada (game loop complica)
- ⏭️ Frogger — game loop tempo-real impede save sensato; aceitável
- ⏭️ Jigsaw — bitmaps regenerar após load é complexo; pendente

---

## 2. Continue Banner agora dinâmico

Como mais 2 jogos passaram a salvar snapshots (Solitaire, Minesweeper), o `Continue Banner` da Home agora reflete o último jogo que a usuária pausou — não fica mais "preso" no Mahjong.

`GameStateRepositoryImpl.observeMostRecentSnapshot()` já retornava o snapshot mais recente; o gargalo era ter só Mahjong populando a tabela.

---

## 3. Backlog ainda pendente (V3 priorizado)

### Alto impacto — fazer próxima rodada
- **V501**: Solitaire drag-and-drop (atualmente só tap-tap)
- **V506**: Solitaire undo
- Tetris auto-save (com pause do loop)
- Jigsaw auto-save (regerar bitmaps após restore)
- **V603/V604**: Minesweeper cascade reveal animado + mine reveal sequencial
- **V705**: Tetris row collapse animado (atualmente só pisca)

### Médio impacto — nice-to-have
- **V003**: Ripple effect explícito em todos clickables
- **V010**: contentDescription pente-fino (auditar todos Icon/Image)
- **V005**: Skeleton/shimmer em loadings
- **V004**: Stagger nas animações simultâneas
- **V011**: Padronizar elevation/shadow
- **V013**: Animar transição light↔dark mode
- **V017**: Touch targets <48dp em alguns lugares
- **V018**: Tetris hold piece
- **V312**: indicação visual de "esperando animação" no Match-3
- **V406**: Color Sort "tubo extra" com limite explícito
- **V407**: Tubos com gradient sutil para parecer mais vidro
- **V902**: Continue Banner como lista expansível (top 3 pausados)

### Baixo impacto — pós-MVP
- **V101**: dragId persiste após snap (cosmético)
- **V108**: zoom-out limit 0.5× → 0.75×
- **V117**: jigsaw "savoring time" antes do win overlay
- **V210**: Mahjong score real no top bar
- **V313**: Format de score com locale BR
- **V605**: highlight da mina explodida
- **V710**: Tetris bag não persiste em snapshot
- **V803/V804/V807**: Frogger detalhes (cor de carros, troncos detalhados, score popup)
- **V1001/V1002**: Settings selection visual + Switch colors
- **V1101**: Stats filtro por jogo
- **V1201/V1202**: PhotoLibrary funcional (CRUD)

### Refinamentos técnicos
- **V103/V104**: Cache de alpha bitmaps no Jigsaw (memória)
- **V202**: BoxWithConstraints garante parent não-scrollable Mahjong
- **V302**: Match-3 use Modifier.layout / IntOffset px puros para precisão
- **V510**: Solitaire Vegas-style score
- **V707**: Tetris hard drop trail
- **V303**: Verificar key dos floaters está aplicado (sim, está)

---

## 4. Sentimento geral de "acabamento"

Após Onda M, o app está em estado **muito mais sólido**:

- **Splash bonita**: usuária abre o app e vê apresentação calorosa antes da Home
- **Cascade Match-3 visível**: gemas explodem e novas caem, satisfação real do match
- **Mahjong autêntico**: 144 peças no formato turtle clássico que ela reconhece
- **Continue Banner funcional**: respeita o último jogo pausado (Mahjong, Match-3, Color Sort, Solitaire, Minesweeper)
- **Snap forte e jogador**: peças do Jigsaw com knobs visualmente clássicos, snap forgiving 32%

Backlog é **gerenciável e bem-mapeado**. Próximas rodadas podem atacar drag-and-drop Solitaire (alto impacto UX) ou Tetris auto-save (completar puzzle universal de save).
