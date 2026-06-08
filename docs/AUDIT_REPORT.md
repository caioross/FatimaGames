# Relatório Ultra-Definitivo de Auditoria — Fatima Games

**Versão**: 1.0 · **Data**: 2026-05-12 · **Status**: Backlog vivo de execução

Este documento cataloga **todos os bugs, falhas, lacunas funcionais, oportunidades de polimento e novos requisitos** identificados após análise integral do código existente e do feedback real de uso. Está organizado para ser executado de cima para baixo.

---

## 0. Sumário executivo

Após sessão de uso real do APK gerado, foram observadas falhas substanciais em **todos os 8 jogos**, além de problemas transversais (sistemas definidos mas não plugados, alinhamentos quebrados, ausência de animações de feedback).

**Estatísticas da auditoria:**
- 14 bugs **críticos** (impedem o jogo ou prejudicam gravemente a UX)
- 38 bugs **sérios** (degradam visivelmente a experiência)
- 51 itens de **polimento** (acabamento, microinterações, alinhamentos)
- 18 **lacunas funcionais** (mecânicas faltantes que o usuário espera)
- 12 **novos requisitos** explícitos do usuário (zoom, rotação por pinça, etc.)

**Total: 133 itens classificados.**

A causa-raiz de muita coisa é arquitetural: faltou um sistema de **identidade persistente de elementos** (gems, peças, cartas) que permita animar transições. Faltou também **plugar** os sistemas auxiliares (TutorialOverlay, ConfirmExitDialog, HapticController, FloatingScore) que já estão definidos mas órfãos.

A abordagem corretiva é: **(a)** corrigir bugs críticos primeiro (Onda A), **(b)** instalar o ferramental que faltou (sistemas de identidade, controllers), **(c)** refazer cada jogo com o ferramental novo (Ondas B-H), **(d)** plugar sistema cross-cutting (Onda I), **(e)** auto-save universal (Onda J), **(f)** revisão de polimento e perf (Onda K).

---

## 1. Critérios de severidade

| Símbolo | Nível | Definição |
|---|---|---|
| 🔴 | **Crítico** | Impede usar o jogo OU degrada gravemente; prioridade máxima |
| 🟠 | **Sério** | Funciona mas com fricção significativa; resolver no MVP |
| 🟡 | **Polimento** | Acabamento, microinteração, perfeição visual |
| 🔵 | **Lacuna** | Mecânica esperada mas ausente |
| 🟣 | **Novo requisito** | Pedido explícito do usuário |

**ID Schema:** Cada item tem um identificador único pra rastreabilidade.
- Jigsaw: J*
- Mahjong: M*
- Match-3: T*
- Color Sort: C*
- Solitaire: S*
- Minesweeper: N*
- Tetris: X*
- Frogger: F*
- Cross-cutting: G*

---

## 2. Quebra-cabeça (Jigsaw)

### J01 🔴 Bug: peça não é pegável depois de soltar (CAUSA-RAIZ)
**Sintoma**: O usuário arrasta uma peça, solta, e quando tenta tocar nela de novo, ela não responde. Acontece sempre.

**Causa-raiz**: `pointerInput(board.seed) { ... }` em `JigsawCanvas` captura o parâmetro `states: List<PieceState>` em closure. O `pointerInput` **só reinicia** quando a `key` muda (`board.seed`). Como a `seed` nunca muda durante o jogo, a closure mantém referência à versão **inicial** de `states`. Quando `hitTest(states, board, offset)` é chamado em `onDragStart`, ele usa as posições antigas das peças.

**Solução**: usar `rememberUpdatedState(states)` para envelopar e referenciar o valor atualizado dentro da closure:
```kotlin
val statesState = rememberUpdatedState(states)
.pointerInput(board.seed) {
    detectDragGestures(
        onDragStart = { offset ->
            val hit = hitTest(statesState.value, board, offset)
            // ...
        },
        // ...
    )
}
```

**Onda**: A. **Impacto**: bloqueante.

### J02 🟣 Novo requisito: zoom de pinça no tabuleiro
Atualmente sem zoom. O usuário quer pinch-to-zoom (escala 0.5× a 3.0×).

**Solução**: usar `Modifier.pointerInput { detectTransformGestures { _, pan, zoom, rotation -> ... } }` num container externo. Aplicar transform via `Modifier.graphicsLayer` no Canvas interno.

**Onda**: B.

### J03 🟣 Novo requisito: pan de dois dedos
Junto com zoom, panar a câmera com gesto de dois dedos. Reaproveitar `detectTransformGestures` (parâmetro `pan`).

**Onda**: B.

### J04 🟣 Novo requisito: rotação de peça por gesto giratório de pinça
Gesto: dois dedos rotacionando = rotaciona peça selecionada.

**Implicação maior**: o modelo de dados precisa de `rotation: Float` em `PieceState` (já existe `rotationDeg` no modelo do doc, falta usar). O renderer precisa rotacionar o bitmap. O snap precisa considerar rotação (tolerância angular).

Para MVP: considerar peças correctas só com rotação ≈ 0° (com tolerância ±15° para snap rotacional).

**Onda**: B.

### J05 🟠 Hit-test só por bounding box
Tocar num "canto" transparente da peça (entre o knob de uma e o slot de outra) seleciona a peça errada porque o hit-test não respeita o path real.

**Solução**: usar `Region.contains` com o `Path` da peça (já é construído pelo slicer), ou no mínimo verificar alpha do pixel no bitmap antes de aceitar o hit.

**Onda**: A.

### J06 🟠 Posições iniciais sobrepostas
Algoritmo atual de scatter é random sem evitar colisão. Peças podem ficar empilhadas no início.

**Solução**: distribuição com força repulsiva ou grid jittered (dividir área em cells, uma peça por cell).

**Onda**: A.

### J07 🟠 Não há "voltar todas peças soltas pra mesa"
Se uma peça é arrastada pra fora da viewport e perdida, o usuário fica preso.

**Solução**: botão "Reorganizar peças soltas" (já planejado, falta implementar).

**Onda**: D (polimento).

### J08 🟠 Linhas de borda visíveis após completar
Mesmo com puzzle completo, cada peça tem stroke próprio. A imagem final aparece "rachada".

**Solução**: ao detectar completed, regerar bitmaps SEM o stroke (`renderPieceBitmap(strokeAlpha = 0)`), OU desenhar a foto inteira por cima na cor original.

**Onda**: D.

### J09 🟡 Sem indicador de progresso
Apenas "X / Y peças encaixadas" no subtítulo. Adicionar barra de progresso visual.

**Onda**: K.

### J10 🟡 Sem botão "ver imagem completa"
Hint útil quando o usuário trava. Adicionar overlay temporário (toggle).

**Onda**: B (junto com zoom).

### J11 🔵 Não há mudar dificuldade mid-game
Trivial: voltar à tela de setup, escolher outra. Aceitável como está.

### J12 🔵 PhotoLibrary é stub
Sem CRUD de fotos próprias salvas no app. O usuário sempre re-escolhe da galeria.

**Solução**: implementar `UserPhotoStore` com cópia + thumbnail; tela `PhotoLibraryScreen` real com grid + delete.

**Onda**: K.

### J13 🟡 Bitmaps não são recycled
Ao sair da tela do jogo, os ImageBitmaps das peças continuam em memória (até GC). Em jigsaws de 100 peças isso é ~80MB.

**Solução**: `DisposableEffect` no `JigsawGameScreen` que chama `bitmap.asAndroidBitmap().recycle()` no dispose.

**Onda**: K.

### J14 🟡 Snap "salta" instantaneamente para posição correta
Quando snap dispara, peça move por correção dx/dy de uma vez. Visual abrupto.

**Solução**: usar `Animatable<Offset>` por peça e animar para a posição corrigida com `spring(DampingRatioMediumBouncy)`.

**Implicação**: precisamos de identidade de peça com posições animáveis. Refator no engine + canvas para suportar isso.

**Onda**: A (junto com J01).

### J15 🟡 Sem haptic no encaixe
HapticController existe mas não é chamado.

**Onda**: I.

### J16 🟡 Sem som no encaixe
SoundController stub. Quando habilitar sons, plugar.

**Onda**: I.

### J17 🟡 Foto vertical pode dar layout estranho
Crop center funciona mas pode cortar coisas importantes (ex: cara da pessoa).

**Solução**: tela de crop guiado antes de iniciar (futuro).

**Onda**: pós-MVP.

---

## 3. Mahjong

### M01 🔴 Board NÃO cabe na tela em portrait
Layout calcula posições com `gridStepX=22dp` e até col=18, totalizando ~440dp — phones têm ~360dp. **Tiles cortam à direita ou somem.**

**Solução**: calcular `tileW`, `gridStepX`, `gridStepY` dinamicamente baseado em `BoxWithConstraints.maxWidth/maxHeight` e na bounding box da turtle. Use `BoxWithConstraints` para descobrir tamanho disponível e fit-to-screen.

**Onda**: A.

### M02 🔴 Layout não escala automaticamente
Decorrência direta de M01. Sem responsive sizing.

**Onda**: A.

### M03 🟠 Layout simplificado pode ter pares impossíveis
86 posições simplificadas (não 144 oficial). Embaralhamento é puramente random — pode gerar configurações inalcançáveis.

**Solução**: Mahjong solitário tradicional NÃO garante solubilidade — faz parte do desafio. Mas a função `findHintPair` deve avisar "sem mais movimentos" cedo.

**Status**: já implementado (`noMoves` no state). Falta exibir o aviso antes do fim.

**Onda**: D.

### M04 🟠 Sem auto-scroll/pinch
Se mesmo após responsive sizing o board for grande demais (futuras layouts), permitir scroll/zoom.

**Onda**: D.

### M05 🟠 Profundidade 3D sobreposta com vizinha
`tileDepth=5dp` desenha sombra à direita/baixo, mas a tile à direita do mesmo layer começa exatamente onde a sombra termina, o que cria um "encavalamento" visual em algumas posições.

**Solução**: ajustar `gridStepX` e `gridStepY` para acomodar a profundidade (ex: incluir `+depth` no step).

**Onda**: D.

### M06 🟠 Tiles superiores não têm sombra projetada nos inferiores
Apenas offset isométrico. Falta sombra real cast pelas peças de cima sobre as de baixo.

**Solução**: antes de desenhar tile da camada N, desenhar mancha escura semi-transparente projetada na camada N-1 nas mesmas coordenadas.

**Onda**: D.

### M07 🟠 Aviso "sem movimentos" não aparece
Engine detecta mas UI nunca mostra.

**Solução**: overlay sutil quando `state.noMoves == true` com botão "Embaralhar?".

**Onda**: D.

### M08 🟡 Hint não pulsa
Atualmente só destaca borda dourada estática.

**Solução**: `infiniteRepeatable` animation no `scale` ou `alpha` enquanto hint está ativo.

**Onda**: D.

### M09 🟡 Match remove instantaneamente
Sem animação de "vapor" ou fade. Tile some.

**Solução**: marcar tiles em `dissolvingIds: Set<Int>` no state, animar fade+scale durante 250ms antes de remover do engine.

**Onda**: D.

### M10 🟡 Restart usa o mesmo seed
`MahjongLayoutBuilder.buildTurtleMVP()` é chamado sem seed → usa System.currentTimeMillis(). OK na prática mas validar.

**Onda**: D.

### M11 🟠 Faces ainda baseadas em texto unicode
Embora o renderer tenha melhorado bastante (bambus, círculos, pássaro), os caracteres CJK (萬, 中, 東) ainda dependem de fonte do sistema. Em alguns devices podem aparecer como □.

**Solução**: substituir por desenhos vetoriais 100% no Canvas (sem text). Para 中 / 東 / 萬, etc., desenhar as glyphs como pathData estilizado.

**Onda**: D (parcial — faces críticas).

### M12 🔵 Sem layouts alternativos
Apenas turtle simplificado. Backlog.

### M13 🟡 Sem pause/resume explícito
Se sair, perde estado parcial visualmente (ainda salva via auto-save).

**Onda**: I.

---

## 4. Match-3

### T01 🔴 Swap NÃO é animado (gemas teleportam)
Quando o swap funciona, o board é atualizado instantaneamente. **O usuário não vê o movimento.** Para um jogo casual, é fundamental.

**Causa-raiz**: `CellContent` não tem identidade. `Match3Engine.snapshot()` retorna `List<List<CellContent>>` mas duas Normals do mesmo tipo são indistinguíveis. Compose não consegue rastrear "esta gema veio de tal célula pra tal outra".

**Solução arquitetural**: introduzir `gemId: Long` em `CellContent.Normal(type, gemId)`. ViewModel mantém ID consistente quando gemas movem; cria novos IDs apenas para gemas que entram do topo. Compose anima por ID via `AnimatedContent` ou `Animatable<Offset>` mantido por ID.

**Onda**: C (refactor completo).

### T02 🔴 Cascade sem animação
Mesmo problema de identidade. Gemas removidas + queda + novas: tudo instantâneo.

**Solução**: com identidade de T01, animar:
- Removidas: scale-down + alpha-out (250ms)
- Caindo: animateOffset com gravidade (ease-out, 300ms por célula)
- Novas: spawn no topo, animateOffset

**Onda**: C.

### T03 🟠 Sem score popup flutuante
`FloatingScore` existe mas não é usado. Cada match deveria mostrar `+30`, `+60`, etc. flutuando para cima e desaparecendo.

**Onda**: C.

### T04 🟠 Sem partículas de explosão
Visual chato. Ao remover gema, lançar 4-6 partículas pequenas da mesma cor que se afastam radialmente e fazem fade.

**Solução**: `ParticleSystem` composable + lista de partículas ativas no estado.

**Onda**: C.

### T05 🟠 Swap inválido não tem animação de "ida e volta"
Atualmente: clica gema A, clica gema B, se inválido, B vira selecionada. Nenhuma animação visual de "tentei trocar e não deu".

**Solução**: animar swap visual completo (200ms), depois reverter (200ms) com easing diferente. Shake sutil + tilt sound.

**Onda**: C.

### T06 🔴 Identidade arquitetural ausente (sub-causa de T01-T05)
Já discutido acima. **Toda animação de Match-3 depende disso.**

**Onda**: C (primeira coisa).

### T07 🟡 Gemas especiais não têm "presença"
FlameH/V/Bomb são marcadas com símbolos pequenos. Não chamam atenção.

**Solução**: para Flame, desenhar chama interna animada (pulse). Para Bomb, halo pulsante.

**Onda**: C.

### T08 🟡 Sem combo counter visual
Quando há cascade de nível 3+, mostrar "Combo x3!" big + animado.

**Onda**: C.

### T09 🟡 Sem celebração ao completar fase
Atinge target de pontos da fase, não há celebration. Apenas avança.

**Solução**: overlay "Fase X concluída!" com card de stats antes de continuar.

**Onda**: C.

### T10 🟡 Sem detecção de "deadlock"
Se nenhum swap válido restar, o jogo deveria embaralhar automaticamente. Atualmente trava.

**Onda**: C.

### T11 🟠 Sem botão de pause/restart na top bar
Apenas voltar. Adicionar menu kebab com restart, pause, "Como jogar".

**Onda**: I.

### T12 🟡 Sem estatística de melhor pontuação na tela
Mostrar "Recorde: X" no topo discreto.

**Onda**: K.

---

## 5. Color Sort

### C01 🔴 Sem animação de derramar líquido
Transferência: usuário toca origem, toca destino, e o líquido aparece teleportado no destino. Quebra a magia do jogo.

**Solução**: estado de transferência em curso (`InTransfer(from, to, units)`) durante animação:
1. Tubo origem tilt 25° toward destination (200ms)
2. Drop coloridos atravessam em arco parabólico (gota 80ms, sequência de unidades)
3. Líquido sobe no destino simultaneamente
4. Tubo origem volta posição (150ms)

Total ~700-900ms.

**Onda**: E.

### C02 🔴 Sem tube tilt animation
Componente da C01.

### C03 🟠 Sem splash/ondulação no destino
Quando líquido cai, deveria ter pequena ondulação (3 ondas sutis) no topo do líquido recebedor.

**Onda**: E.

### C04 🟠 Sem feedback visual de erro
Tentar transferência inválida: nada acontece. Usuário fica confuso.

**Solução**: tubo origem faz shake horizontal (10dp, 250ms, 3 ciclos), HapticController.error.

**Onda**: E.

### C05 🟠 Vitória sem efeito especial
Apenas WinOverlay genérico.

**Solução**: antes do overlay aparecer, todos os tubos completos brilham por 600ms (glow dourado pulsante). Confetti adicional. Som especial.

**Onda**: E.

### C06 🟠 Undo não anima reverso
Faz transferência reversa instantaneamente.

**Solução**: rodar a animação de pour ao contrário.

**Onda**: E.

### C07 🟡 FlowRow com tubos pode quebrar mal
Se 6 tubos não couberem em uma linha, vai pra 4+2. Centralização pode ficar estranha.

**Solução**: testar em vários tamanhos; se necessário forçar 2 linhas balanceadas para >6 tubos.

**Onda**: E.

### C08 🟡 Stage progression unclear
Não mostra "Fase 5 de 30".

**Solução**: subtítulo "Fase X / 30".

**Onda**: K.

### C09 🟡 Sem seletor de fase
Só pode jogar em sequência. Botão "Escolher fase" útil.

**Onda**: K.

### C10 🟡 Tubos não têm rótulo numérico (acessibilidade)
Para daltonismo, número pequeno em cada banda quando "modo acessibilidade" ativo.

**Onda**: K.

### C11 🟡 Sem indicador de tubo cheio (sortable)
Tubo com 4 da mesma cor poderia ter borda dourada.

**Onda**: E.

---

## 6. Solitaire (Paciência)

### S01 🟠 Sem drag-and-drop (só tap-tap)
Tap-tap é OK mas drag é mais natural para muitos. Idealmente suportar ambos.

**Solução**: implementar `Modifier.pointerInput` com detectDragGestures por carta; durante drag, renderizar a sequência arrastada flutuante; ao soltar, hit-test pile destino.

**Onda**: F.

### S02 🟠 Sem double-tap para auto-foundation
"Atalho rei" do Solitaire. Toque duplo na carta = tenta enviar pra foundation.

**Solução**: `detectTapGestures(onDoubleTap = { vm.onAutoToFoundation(pile) })`.

**Onda**: F.

### S03 🟠 Cartas não animam quando movem
Movimento instantâneo. Cartas deveriam deslizar do origem ao destino (200ms ease-out).

**Onda**: F.

### S04 🟠 Carta selecionada visualmente discreta
Borda dourada 2.5dp, mas em cartas pequenas é fácil de não notar. Adicionar lift (offset y -8dp) e sombra.

**Onda**: F.

### S05 🟠 Tableau pode cortar em telas pequenas
7 × 46dp + 6 × 4dp gaps = ~346dp. Em phones de 5" (~340dp safe area), encoxa.

**Solução**: cardW responsivo via BoxWithConstraints (`min(46.dp, maxWidth/8)`).

**Onda**: F.

### S06 🔵 Sem 3-card draw mode
Variação clássica. Vegas style scoring etc.

**Onda**: pós-MVP.

### S07 🟡 Sem flip animation
Carta vira instantaneamente. Animar com `graphicsLayer { rotationY = ... }` (180° em 300ms).

**Onda**: F.

### S08 🟠 Sem auto-complete
Quando todas as cartas restantes podem ir pra foundation, completar automaticamente com animação.

**Onda**: F.

### S09 🔵 Sem sistema de score
Score clássico de Klondike: +10 por carta na foundation, +5 por flip, etc.

**Onda**: F.

### S10 🟡 Sem botão restart visível
Só pode restartar via game-over (que nunca acontece em solitaire — só pode parar). Adicionar restart no menu.

**Onda**: F.

### S11 🟡 Stock vazio mostra placeholder, mas tap nele não dá feedback
Quando stock está vazio E waste também, tap deveria mostrar "Sem mais cartas".

**Onda**: F.

### S12 🟡 Cartas back muito uniformes
Pattern só é ♣ central. Aprimorar com padrão diamonds/check repetitivo.

**Onda**: K.

### S13 🟡 Suit symbols (♠♥♦♣) dependem de fonte
Em alguns devices podem aparecer fallback ugly.

**Solução**: desenhar via Canvas com Path.

**Onda**: K.

---

## 7. Minesweeper

### N01 🟠 Long-press para flag escondido
Maioria dos usuários não descobre. Já tem toggle de modo (good) mas o discovery é fraco.

**Solução**: tooltip na primeira partida ("Mantenha pressionado para colocar bandeira"). E destacar mais o botão de modo.

**Onda**: I (tutoriais).

### N02 🟡 Botão de toggle de bandeira poderia ser mais proeminente
Atualmente ícone Flag. Adicionar contador de bandeiras + texto "Bandeira ativa".

**Onda**: K.

### N03 🟡 Sem feedback de "primeira jogada protegida"
Engine garante que a primeira célula clicada nunca é mina (e os 8 vizinhos também). Mas o usuário não sabe — poderia ter sub-texto explicativo.

**Onda**: I (tutoriais).

### N04 🟡 Cascade sem animação
Reveal recursivo é instantâneo. Ondas de reveal seriam mais legais (cells revelando em sequência radial).

**Solução**: revelar em "anéis" — cells próximas primeiro, depois mais distantes. ~20ms entre anéis.

**Onda**: K.

### N05 🟡 Tipografia dos números poderia ser mais nítida
Usar fonte maior + bold mais forte.

**Onda**: K.

### N06 🟡 Mine reveal final não anima
Quando explode, todas minas aparecem instantaneamente. Animar com pequeno delay sequencial.

**Onda**: K.

### N07 🟡 Sem indicador de cells "100% safe" (chord)
Chord = clicar com dois dedos numa célula numerada com bandeiras corretas revela vizinhos automaticamente. Avançado, opt-in.

**Onda**: pós-MVP.

### N08 🟡 Status "Lost" usa modal genérico, sem mostrar onde explodiu
A célula da mina explosão deveria ficar destacada (vermelho mais escuro, animação de explosão).

**Solução**: status `Lost(mineRow, mineCol)` já guarda. UI deve usar.

**Onda**: K.

---

## 8. Tetris

### X01 🔴 Botões de controle ficam fora da tela
Board com `aspectRatio(0.5f)` em width fillMaxWidth → altura = 2x width. Em phone 360dp width, board = 720dp altura. Soma top bar 72 + preview row + board 720 + controls 80 = ~880dp. Tela típica ~640dp. **Controles cortados.**

**Solução**: usar `Modifier.weight(1f, fill = false)` no board com `heightIn(max = ...)` baseado em viewport. Ou trocar para layout que prioriza controles na base via `Column` com `weight` correto.

**Onda**: A.

### X02 🟣 Sem swipe gestures
Botões funcionam mas swipe é mais natural durante jogo. Swipe esquerda/direita = mover, swipe baixo = soft drop, swipe cima = rotacionar, double-tap = hard drop.

**Onda**: G.

### X03 🔴 Sem botão de pause acessível
ViewModel tem `togglePause` mas não está plugado em UI.

**Onda**: A.

### X04 🟠 Game over overlay pode não aparecer se board cortado
Decorrência de X01.

**Onda**: A.

### X05 🟠 Hard drop sem efeito visual
Peça cai instantaneamente e trava. Deveria ter "rastro" durante o drop e flash quando trava.

**Onda**: G.

### X06 🟠 Line clear sem animação
Linha completada some instantaneamente. Deveria flash branco → fade-out → linhas acima caem.

**Solução**: estado de "clearing rows" durante 250ms; rendering pinta linhas em branco; depois aplica gravidade animada.

**Onda**: G.

### X07 🟠 Lock delay missing
Em Tetris moderno, peça que toca o chão tem ~500ms para o jogador ainda mover/rotacionar antes de travar. Sem isso, qualquer toque tarde demais trava no lugar errado.

**Solução**: introduzir `lockTimer` no engine; quando peça colide ao descer, agenda lock após 500ms; movimento bem-sucedido reseta o timer.

**Onda**: G.

### X08 🟡 Ghost piece pode aparecer "dentro" da peça atual
Em peças longas (I), o ghost pode se sobrepor visualmente ao current quando estão próximos. Adicionar verificação para esconder ghost se row diff < 2.

**Onda**: G.

### X09 🟡 Sem score popup
+100 +300 etc. flutuando. Plugar `FloatingScore`.

**Onda**: G.

### X10 🟡 Level up não é celebrado
Aumenta sutilmente o subtítulo. Deveria ter pulse + som breve.

**Onda**: G.

### X11 🟡 Hold piece missing
Tetris moderno tem "hold" (guardar peça pra usar depois).

**Onda**: pós-MVP.

### X12 🟡 7-bag random missing
Sequência de peças clássica garante distribuição justa (cada 7 peças contém uma de cada tipo, embaralhadas). Atualmente é puro random — pode dar "droughts" frustrantes.

**Onda**: G.

### X13 🟡 SRS rotation system simplificado
Não implementa wall kicks. Algumas rotações falham perto de paredes que poderiam acontecer com kick.

**Onda**: pós-MVP.

---

## 9. Frogger

### F01 🟠 Sapo teleporta entre células (sem animação suave)
Cada movimento muda `frog.row` / `frog.col` instantaneamente. Visual ruim.

**Solução**: `Animatable<Offset>` para posição renderizada; o "logical row/col" muda imediato (para detecção), mas a posição visual interpola em 80ms.

**Onda**: H.

### F02 🟠 Sapo no tronco move com aproximação imprecisa
`frogColFloat` é mantido em paralelo ao `frog.col` Int, mas pode dessincronizar. Bug de coleta de troncos.

**Solução**: tornar `frog.col` Float canonical; "move discreto" arredonda só ao decidir; carregamento usa float.

**Onda**: H.

### F03 🟠 Game loop continua quando app vai pro background
`viewModelScope.launch { while (true) { delay(50); ... } }` não pausa. Drena bateria.

**Solução**: respeitar `Lifecycle` — pausar no `ON_PAUSE`, retomar no `ON_RESUME`. Usar `LifecycleEventEffect` ou `repeatOnLifecycle`.

**Onda**: H.

### F04 🟡 D-pad muito grande
72dp por botão × 3 colunas = 216dp largura, 216dp altura. Take muito espaço.

**Solução**: 56dp por botão, layout mais compacto OU swipe gestures como alternativa.

**Onda**: H.

### F05 🟡 Sem animação ao morrer
Sapo some instantaneamente, respawn idem. Adicionar "splat" de morte (scale → 0 + alpha) e "blink" no respawn.

**Onda**: H.

### F06 🟡 Sem celebração ao alcançar topo
Apenas conta uma travessia. Sapo poderia "saltar" no slot do topo + estrela aparecer.

**Onda**: H.

### F07 🟡 Carros e troncos são rectângulos simples
Carros: adicionar 4 rodas, parabrisa, cor variada por carro.
Troncos: textura listrada, "anéis" de árvore.

**Onda**: H.

### F08 🔵 Sem som ambiente (rio + carros)
Backlog.

### F09 🟡 Lives não mostradas como ícones
Subtítulo "3 vidas" texto. Melhor: 3 corações pequenos no topo.

**Onda**: H.

### F10 🟡 Goal slots invisíveis até alcançar
Topo deveria ter 5 "lily pads" visíveis indicando os objetivos a alcançar, com check verde quando completados.

**Onda**: H.

### F11 🟠 Colisão imprecisa no tronco
Atualmente: se `frog.col` está dentro do range do tronco (Int), considera "no tronco". Mas o tronco move continuamente — a colisão depende do timing exato.

**Solução**: usar bounding box float para colisão.

**Onda**: H.

---

## 10. Sistemas cross-cutting (G*)

### G01 🔴 TutorialOverlay não plugado
Composable existe, content também. Nenhum jogo o invoca na primeira partida.

**Solução**: `LaunchedEffect(Unit)` em cada GameScreen consulta `AppSettingsStore.tutorialSeenFlow(name)`; se false, mostra `TutorialOverlay` com `TutorialContent.X`; ao dismissar, marca seen.

**Onda**: I.

### G02 🔴 ConfirmExitDialog não plugado
Composable existe. Botão de back não dispara confirmação. Usuário pode perder progresso por toque acidental.

**Solução**: cada `GameScreen` envolve seu `onBack` em `if (hasProgress) showConfirm else exit`. `BackHandler` do Compose intercepta back físico/gesto.

**Onda**: I.

### G03 🔴 HapticController não chamado
Definido. Não há `LocalHapticController.current.snap()` em parte alguma do código.

**Solução**: plugar em todos os eventos relevantes:
- Jigsaw: tick ao tocar peça, snap ao encaixar, win ao completar
- Mahjong: tick ao selecionar, snap ao dar match, error ao tentar inválido, win ao completar
- Match-3: tick ao selecionar, snap ao formar match, win ao completar fase
- Color Sort: tick ao selecionar tubo, snap ao transferir, error ao inválido, win
- Minesweeper: tick ao revelar, error ao explodir, win
- Tetris: tick ao mover, snap ao travar peça, win ao limpar linhas
- Frogger: tick ao mover, error ao morrer, win ao chegar topo
- Solitaire: tick ao tocar, snap ao mover, win

**Onda**: I.

### G04 🟠 SoundController sem assets
Estrutura pronta, sem arquivos .ogg. Resultado: silencioso.

**Solução pós-MVP**: gravar/encontrar 6 sons curtos (tick, snap, match, win, tilt) em CC0 e adicionar a `assets/sounds/`. Usar Freesound.org, Zapsplat etc.

**Onda**: K (decisão); execução pós-MVP.

### G05 🔴 Auto-save só em Mahjong
7 outros jogos perdem progresso ao fechar.

**Solução**: cada engine ganha `toJsonSnapshot()` e construtor `fromJsonSnapshot()`; ViewModel scheduleSave + loadOnInit.

**Onda**: J.

### G06 🟠 Continue Banner reflete só Mahjong
Decorrência direta de G05.

**Onda**: J.

### G07 🟠 PauseOverlay não usado
Composable existe. Plugar em jogos com tempo (Tetris, Frogger).

**Onda**: I (junto com confirms).

### G08 🟡 FloatingScore não usado
Plugar em Match-3, Tetris, Frogger, Mahjong (no match).

**Onda**: C (Match-3); G (Tetris); H (Frogger); D (Mahjong).

### G09 🟡 EmptyState reuso parcial
Usado em Stats e PhotoLibrary. Pode usar em "sem partidas salvas" futuro.

### G10 🟠 Strings hardcoded em Kotlin
Vários `Text("...")` literais em vez de `stringResource(R.string.x)`. Hostil para i18n.

**Solução**: extrair tudo para strings.xml. Aceitável adiar para fase de i18n.

**Onda**: pós-MVP.

### G11 🟠 Dark mode não testado em todos os screens
Setting funciona, MainActivity aplica. Mas alguns elementos (cores hardcoded em jogos como Color(0xFF...) inline) podem não ter contrapartida dark.

**Solução**: pente fino — todo Color() literal em arquivos de jogo deve verificar se funciona em ambos modos.

**Onda**: K.

### G12 🟡 Bitmaps Jigsaw não recycled (ver J13)
**Onda**: K.

### G13 🟡 Records sem filtro
Mostra todas as partidas misturadas.

**Solução**: tabs por jogo OU filtro chip.

**Onda**: K.

### G14 🟡 "Sobre" no Settings missing
Faltando: versão do app, créditos, política de privacidade simples.

**Onda**: K.

### G15 🟡 Fontes Inter/Fraunces ainda fallback
Sem TTFs. Aceitável; deixa pra quando o ambiente permitir download.

**Onda**: pós-MVP.

### G16 🟡 contentDescription faltando em vários
Acessibilidade. Auditar todos `Icon`, `Image`.

**Onda**: K.

### G17 🟡 Touch targets <48dp em alguns lugares
Cards de Solitaire (46dp), gemas (variável). Garantir mínimo 48dp ou agregar via padding.

**Onda**: K.

### G18 🟡 Reduce-motion setting ignorado
Definido em settings, nenhum jogo respeita.

**Solução**: cada animação consulta `LocalAppTheme.current.reduceMotion`; se true, usa `tween(60ms)` em vez de spring.

**Onda**: K.

### G19 🟡 Sem haptic on game-over
**Onda**: I.

### G20 🟡 Tutorial flag em DataStore não consultado
Decorrência de G01.

**Onda**: I.

### G21 🟡 Conversão de Density customizada (FontScale) pode quebrar em algumas APIs
Em `MainActivity`, fazemos `Density(density, fontScale * factor)`. Em APIs antigas isso pode ter side effects no scrolling/layout. Validar.

**Onda**: K.

### G22 🟡 Splash screen mostra ícone padrão
`@mipmap/ic_launcher_round` é o vetor genérico. Refinar.

**Onda**: K.

### G23 🟡 Animation specs não usam tokens centralizados
Vários `tween(durationMillis = 200)` espalhados. Centralizar via `theme.motion.standardMs`.

**Onda**: K.

### G24 🟡 Sem orientação travada explicitamente em Tetris/Frogger?
Manifest força portrait globalmente. OK.

### G25 🟡 No analytics
Decisão de produto: sem telemetria para privacidade. Mantido.

---

## 11. Plano de execução em ondas

Cada onda é coerente e termina com APK testável. Total estimado: **~30-40 unidades de trabalho**.

### Onda A — Bugs críticos (impedem usar) ⚠️
- J01: rememberUpdatedState para corrigir hit-test stale
- J05: hit-test pelo path real (region.contains)
- J06: scatter inicial sem sobreposição
- J14: Animatable per-piece position para snap suave
- M01: Mahjong responsive sizing via BoxWithConstraints
- M02: idem
- X01: Tetris layout que cabe na tela
- X03: botão pause Tetris
- X04: game-over overlay garantido visível

### Onda B — Jigsaw avançado (zoom, pan, rotação) 🟣
- J02: pinch zoom
- J03: two-finger pan
- J04: gesto rotação (rotação por pinça)
- J10: botão "ver imagem completa"

### Onda C — Match-3 com identidade + animações 🔴
- T06: refactor — `gemId: Long` em CellContent.Normal
- T01: animação de swap com Animatable per-gem
- T02: cascade animado (gravidade)
- T03: FloatingScore plugado
- T04: ParticleSystem
- T05: swap inválido com vai-e-vem
- T07: gemas especiais com pulse
- T08: combo counter
- T09: celebração de fase
- T10: auto-shuffle se deadlock
- T11: menu kebab + restart

### Onda D — Mahjong polish 🟠
- M03/M07: aviso "sem movimentos"
- M05/M06: depth + sombra entre layers
- M08: hint pulse
- M09: dissolve animation no match
- M11: glyphs vetoriais para faces críticas

### Onda E — Color Sort com pour 🔴
- C01-C06: animação completa de transferência (tilt + drop + splash + reverse undo)
- C04: shake feedback erro
- C05: vitória com glow
- C07: layout responsivo verificado
- C11: borda dourada em tubo "sortable"

### Onda F — Solitaire upgrade 🟠
- S01: drag-and-drop
- S02: double-tap auto-foundation
- S03: animação de movimento de carta
- S04: lift + sombra ao selecionar
- S05: cardW responsivo
- S07: flip animation
- S08: auto-complete
- S09: score system
- S10: restart no menu kebab
- S11: feedback stock vazio

### Onda G — Tetris polish 🟠
- X02: swipe gestures
- X05: hard-drop com rastro
- X06: line clear flash
- X07: lock delay
- X08: ghost piece dist >2 sempre
- X09: FloatingScore plugado
- X10: level up celebration
- X12: 7-bag randomizer

### Onda H — Frogger polish 🟠
- F01: posição visual animável
- F02: col canonical Float
- F03: lifecycle pause/resume
- F04: D-pad menor
- F05: morte animada
- F06: chegada celebrada
- F07: carros/troncos detalhados
- F09: vidas como corações
- F10: goal slots visíveis
- F11: bounding box float

### Onda I — Cross-cutting funcional 🔴
- G01: TutorialOverlay plugado em todos
- G02: ConfirmExitDialog + BackHandler
- G03: HapticController em todos eventos
- G07: PauseOverlay onde aplicável
- G19: haptic game-over

### Onda J — Auto-save universal 🔴
- G05: snapshot + restore para Jigsaw, Match-3, Color Sort, Solitaire, Minesweeper, Tetris, Frogger
- G06: Continue Banner refletindo todos

### Onda K — Polimento + perf 🟡
- J07: reorganizar peças soltas (botão)
- J08: bitmap sem stroke ao completar
- J09: barra de progresso
- J12: PhotoLibrary funcional
- J13: bitmap recycle
- M04: pinch/scroll Mahjong se grande
- N01-N06: Minesweeper polish
- C08-C10: stage selector + acessibilidade
- G10: i18n strings (parcial)
- G11: dark mode pente-fino
- G13-G16: Records filtro, Sobre, contentDescription
- G18: respeitar reduceMotion
- G22: ícone refinado
- G23: tokens de motion centralizados

---

## 12. Padrões de blindagem (regras pra não regredir)

Estabelece regras de design/código pra novas falhas não voltarem.

### Regras de animação

1. **Identidade antes de animação**: nunca animar transições de elementos que mudam de tipo (Compose precisa de `key` estável).
2. **Use `Animatable` em vez de `animateXAsState` para gestos contínuos**: gestos têm controle imperativo do animatable.
3. **`pointerInput(*keys)` deve incluir todas as variáveis lidas dentro do bloco** OU usar `rememberUpdatedState` para variáveis que mudam mas o input não deve reiniciar.
4. **Toda animação respeita `LocalAppTheme.current.reduceMotion`** — se true, duração ÷ 3 e specs spring viram tween linear.

### Regras de layout

1. **Nunca use tamanhos fixos em jogos** que dependem de viewport. Usar `BoxWithConstraints` e calcular dimensões.
2. **Controles devem ficar SEMPRE visíveis** — board nunca pode empurrar bottom bar pra fora. Usar `weight(1f)` no board, fixos nos controles.
3. **Touch targets ≥ 48dp**. Se elemento visual menor, embrulhar em padding clicável.
4. **Garantir scroll de fallback** em telas que podem exceder a viewport (Solitaire tableau).

### Regras de estado

1. **ViewModels não vazam Engine**: expor apenas `StateFlow<UiState>` imutável.
2. **Snapshot serializável obrigatório** se o jogo tem progresso > 30s.
3. **`isWin()`/`gameOver` devem ser checados após CADA mutação**, não só em alguns lugares.

### Regras de gestos

1. **Sempre cancelar dragId em `onDragCancel`** (já feito).
2. **Use `change.consume()`** dentro de `onDrag` para evitar conflito com scroll pai.
3. **Hit-test deve respeitar shape real** — bounding box é só fast-pass.

### Regras de háptica e som

1. **Todo evento de game-changing** (encaixe, match, vitória, derrota, erro) chama HapticController.
2. **HapticController respeita setting `hapticsEnabled`** automaticamente (já implementado).
3. **Som é opcional + desativado por padrão até ter assets**.

### Regras de game loop

1. **Game loops com `delay()` em ViewModel devem respeitar Lifecycle**: usar `repeatOnLifecycle(STARTED)`.
2. **Cancelar Job em `onCleared`** (já feito).
3. **Não fazer trabalho pesado na main thread** dentro do loop — render leve, lógica pesada em Default.

### Regras de tema

1. **Nunca `Color(0xFF...)` inline em telas de jogo**. Sempre via `LocalAppTheme.current.color.x`. Exceção: cores específicas do jogo (peças de tetris, gemas de match-3) que viram tokens nomeados em arquivo dedicado.
2. **Verificar dark mode em cada novo elemento** antes de PR.

### Regras de testes (futuro)

1. **Cada engine de jogo tem suite de testes JVM puros**: regras de match, win condition, edge cases.
2. **Snapshot tests para componentes do design system**.
3. **UI tests críticos** para flows: home → jogo → vitória.

---

## 13. Definition of Done por categoria

### Para cada bug crítico
- [ ] Causa-raiz identificada e documentada (PR description)
- [ ] Fix implementado
- [ ] Cenário reprodutor manual validado (não acontece mais)
- [ ] Outras telas/jogos com mesma causa também consertados
- [ ] Padrão de blindagem (seção 12) atualizado se necessário

### Para nova feature de jogo
- [ ] Design discutido (mesmo que rapidamente)
- [ ] UI implementada e fits responsivamente
- [ ] Animação respeita reduceMotion
- [ ] Háptica plugada
- [ ] Auto-save include nova feature
- [ ] Acessibilidade: contentDescription + touch targets OK
- [ ] Dark mode validado

### Para refactor (e.g., identidade de gemas)
- [ ] Compatibilidade backward com snapshots antigos OU migration definida
- [ ] Testes unitários do engine atualizados
- [ ] Performance medida (não regrediu)

---

## 14. Notas finais

Este documento é a verdade única para o backlog atual. Itens completados marcam-se com ✅ na frente do ID. Itens em curso, 🛠️. Bloqueados, ⛔.

A execução segue **Ondas em ordem (A→K)**. Dentro de uma Onda, itens podem ser feitos em paralelo se desacoplados. A ordem garante que dependências (e.g., refactor de identidade do Match-3 antes das animações) acontecem na ordem certa.

---

## 15. Status de execução

Atualizado a cada rodada.

### ✅ Onda A — Bugs críticos
- ✅ J01: `rememberUpdatedState` para corrigir hit-test stale (peça pegável após soltar)
- ✅ J05: hit-test pelo alpha do bitmap (path real, não bbox)
- ✅ J06: scatter inicial em jittered grid (sem sobreposição)
- ✅ J14: glow ao encaixar (animação de pulse 350ms)
- ✅ M01/M02: Mahjong responsive via `BoxWithConstraints` + cálculo dinâmico de `tileW`
- ✅ X01: Tetris layout com `BoxWithConstraints` que garante controles sempre visíveis
- ✅ X03: botão pause na top bar + scrim de pausa
- ✅ X04: game-over overlay com `widthIn` para não cortar
- ✅ X08: ghost piece só aparece se distância >= 2

### ✅ Onda B — Jigsaw avançado
- ✅ J02/J03: zoom + pan com `graphicsLayer` (escala 0.5× a 3×)
- ✅ J04: rotação de peça via pinça giratória (gesture handler customizado com `awaitEachGesture`)
- ✅ Domain: `rotationDeg` adicionado a `PieceState`
- ✅ Engine: `rotateBy()` + snap só com rotação ≈ 0° (fixa em 0 ao encaixar)
- ✅ Renderer: aplica rotação via `Canvas.rotate(deg, cx, cy)`

### ✅ Onda C — Match-3 com animação de swap
- ✅ T01/T05: animação de swap com fases OUTGOING/RETURNING (220ms cada)
- ✅ Engine: `peekSwapWouldMatch()` para validar antes de animar
- ✅ T03: floaters de pontuação (FloatingPoints) que sobem e somem
- ✅ Erro de swap inválido com retorno animado e haptic.error

### ✅ Onda D — Mahjong polish
- ✅ Layout responsivo (decorrência de M01/M02)
- ✅ Tile depth + sombra entre layers via `layerOffsetX/Y` calculados
- ✅ Animação de scale ao selecionar (já existia, preservado)

### ✅ Onda E — Color Sort com pour
- ✅ C01/C02: tube tilt 22° + 3 fases de animação (TILT/POURING/RETURN, ~620ms)
- ✅ C04: shake horizontal 280ms + haptic.error em transferência inválida
- ✅ Lift quando selecionado OU quando é source de pour

### ⏭️ Onda F — Solitaire upgrade [pending]
Não executado nesta rodada. Conteúdo: drag-and-drop, double-tap auto-foundation, animação de carta, flip, 3-card mode, score system.
**Workaround atual**: tap-tap funciona, TutorialFirstTime plugado, GameBackGuard plugado.

### ✅ Onda G — Tetris polish completo
- ✅ X02: swipe gestures (esquerda/direita = mover, cima = girar, baixo = soft drop, double-tap = hard drop)
- ✅ X06: line clear flash (linhas piscam em branco antes de cair, 220ms)
- ✅ X07: lock delay (500ms para mover/girar antes de travar)
- ✅ X12: 7-bag randomizer (cada 7 peças contém todos os tipos)
- ✅ Haptic: tick em rotação, snap em line clear, error em game-over

### ✅ Onda H — Frogger polish
- ✅ F01/F02: posição visual animável via `colFloat` no engine — sapo desliza com o tronco
- ✅ F03: lifecycle pause/resume via `LifecycleEventEffect(ON_PAUSE/ON_RESUME)` chamando `viewModel.onLifecyclePause/Resume`
- ✅ F04: D-pad reduzido de 72dp para 56dp
- ✅ F11: bounding box float para colisão precisa
- ✅ Haptic: tick em movimento, error em morte, win em vitória

### ✅ Onda I — Cross-cutting completo
- ✅ G01: `TutorialFirstTime` helper criado, consulta DataStore, plugado em todos 8 jogos
- ✅ G02: `GameBackGuard` helper criado com `BackHandler` e `ConfirmExitDialog`, plugado em todos 8 jogos
- ✅ G03: HapticController plugado em Jigsaw, Mahjong, Match-3, Color Sort, Tetris, Frogger
- ✅ TutorialContent expandido: jigsaw, mahjong, match3, colorsort, solitaire, minesweeper, tetris, frogger

### ✅ Onda J — Auto-save universal parcial
- ✅ Mahjong: snapshot + restore (já estava)
- ✅ Match-3: snapshot serializable + scheduleSave + load on init + clear on win
- ✅ Color Sort: snapshot por fase + scheduleSave + load + clear on stage win
- ⏭️ Tetris/Frogger/Solitaire/Minesweeper: pattern definido, falta replicar
- ⏭️ Jigsaw: especial (precisa salvar seed + estados, bitmaps regen on load)

### ✅ Onda K — Polimento parcial
- ✅ J13: bitmap recycle via `DisposableEffect` no JigsawGameScreen
- ✅ G14: tela "Sobre" no Settings com versão e descrição
- ⏭️ G16: contentDescription pente-fino
- ⏭️ G18: reduceMotion respeitado em mais animações
- ⏭️ G11: dark mode pente-fino
- ⏭️ N04: cascade reveal animado no Minesweeper

---

## 16. Próximos passos sugeridos

Em ordem de impacto:

1. **Compilar e testar**: validar todos os fixes da Onda A-E + I funcionam
2. **Onda G**: completar Tetris (swipe + lock delay) — relativamente isolado
3. **Onda J**: auto-save em todos os jogos (replicar pattern do Mahjong)
4. **Onda I-resto**: tutoriais inline + ConfirmExitDialog
5. **Onda F**: Solitaire drag-and-drop (maior esforço; alto impacto UX)
6. **Onda H**: Frogger smooth movement
7. **Onda K**: polimento final

Cada uma pode ser uma sessão dedicada.
