# Mahjong Solitaire — Spec técnica

**Versão**: 1.0

---

## 1. Visão

Mahjong Solitaire clássico, layout "tartaruga". Objetivo: remover todas as 144 peças pareando peças iguais e "livres".

## 2. Regras

### 2.1 Peça livre

Uma peça está **livre** quando, simultaneamente:
- (a) Não há peça nenhuma diretamente sobre ela (camada acima vazia naquela posição)
- (b) Ao menos um dos lados (esquerdo OU direito) está sem peça adjacente na mesma camada

Peça travada por cima OU travada nos dois lados é não-jogável até que seja liberada.

### 2.2 Match

Selecionar duas peças livres com o mesmo desenho remove ambas. "Mesmo desenho" considera as equivalências tradicionais:

| Categoria | Quantidade | Variações |
|---|---|---|
| Bambu | 9 desenhos × 4 cópias = 36 | 1–9 |
| Caractere (Wan) | 9 × 4 = 36 | 1–9 |
| Círculo (Pin/Dot) | 9 × 4 = 36 | 1–9 |
| Vento | 4 × 4 = 16 | Leste, Sul, Oeste, Norte |
| Dragão | 3 × 4 = 12 | Vermelho, Verde, Branco |
| Flor | 4 × 1 = 4 | Equivalentes entre si (qualquer flor casa com qualquer flor) |
| Estação | 4 × 1 = 4 | Equivalentes entre si |
| **Total** | **144** | 42 desenhos únicos |

### 2.3 Fim de partida

- **Vitória**: todas as 144 removidas
- **Sem movimentos**: nenhum par válido disponível **e** sem embaralhamentos restantes → "Sem mais movimentos. Reiniciar?"

## 3. Layout — "Turtle"

Layout canônico de 5 camadas, 144 peças. Definido como um array 3D `[layer][row][col]` onde `1` = peça presente, `0` = vazio. As coordenadas usam "half-cell" para representar deslocamentos clássicos.

Layout completo armazenado em `assets/mahjong/layout_turtle.json`. Estrutura:

```json
{
  "name": "Turtle",
  "layers": [
    {
      "z": 0,
      "tiles": [
        {"row": 0, "col": 2}, {"row": 0, "col": 3}, ...
      ]
    },
    {"z": 1, "tiles": [...]},
    {"z": 2, "tiles": [...]},
    {"z": 3, "tiles": [...]},
    {"z": 4, "tiles": [...]}
  ]
}
```

Cada camada superior é deslocada por um offset visual de `(-3 dp, -3 dp)` (direita, cima) para simular pilha 3D.

## 4. Assets das peças

### 4.1 Origem

Base: **`FluffyStuff/riichi-mahjong-tiles`** — vetores SVG em domínio público, conjunto completo de 42 faces tradicionais. Licença: PD.

Cada face é convertida para `VectorDrawable` (XML do Android) na pipeline de assets.

### 4.2 Renderização

A peça final é renderizada em runtime via Canvas custom, compondo:

1. **Topo da peça** (face superior, onde está o desenho)
2. **Laterais** (direita e baixo) — retângulos em cor mais escura simulando profundidade

```
┌─────────────┐
│   [face]    │    ← topo, cor marfim, com desenho vetorial
│   desenho   │
│             │
├──┬──────────┤
│  │          │   ← lateral direita (perspectiva), cor #D8C8AC
└──┴──────────┘
   ↑
   lateral baixo (perspectiva), cor #C0AE91
```

Proporção: peça padrão = 60 dp largura × 80 dp altura × 16 dp profundidade visual.

Cor de fundo do topo:
- Padrão: `#F4ECD6` (marfim claro)
- Selecionada: `#FFE489` (amarelo claro), com leve brilho
- Não-livre (visual sutil): mesma cor mas com `alpha=0.85` e sem brilho de borda

### 4.3 Tamanho responsivo

Tamanho da peça calculado para que o layout completo caiba na tela com margem de `space.md`:

```
availableWidth = screenWidth - 2 * space.md
maxCellW = availableWidth / 15   (layout turtle ocupa ~15 half-cells de largura)
tileW = min(maxCellW * 2, MAX_TILE_W)   // half-cell × 2
tileH = tileW * 1.33
```

## 5. Estados de seleção e match

| Estado | Visual |
|---|---|
| Não-livre | Peça normal, leve dim |
| Livre, não-selecionada | Peça normal, clicável |
| Selecionada (1ª de um par em construção) | Borda dourada 2 dp, scale 1.04, brilho |
| Match em progresso (par fechado) | Animação 250 ms: scale up + fade out + leve "sobe" |
| Inválida (tentou matchar com não-igual) | Shake horizontal 6 dp, 200 ms, peças voltam ao normal |

## 6. Ações disponíveis

Bottom action bar:

| Ação | Limite | Custo na pontuação | Comportamento |
|---|---|---|---|
| **Dica** | 3 por partida | -50 pontos cada | Pisca um par válido por 1 s |
| **Desfazer** | Ilimitado | -10 pontos por uso | Reverte o último match |
| **Embaralhar** | 2 por partida | -100 pontos cada | Reorganiza peças restantes mantendo o layout |

Indicadores no topo:
- Tempo decorrido (timer)
- Peças restantes (ex.: "94 / 144")

## 7. Pontuação

A pontuação não é o foco principal (a usuária se importa mais com tempo e completar), mas é registrada:

```
score = 10000
       - max(0, elapsedSec - 600) * 5     // penalidade após 10 min
       - hintsUsed * 50
       - undosUsed * 10
       - reshufflesUsed * 100
       + (consecutive_matches_streak * 5)  // micro-bônus por matches em sequência
```

Mínimo 0. Resultado gravado em `GameRecord.score`.

## 8. Modelo de domínio

```kotlin
package com.fatimagames.app.feature.games.mahjong.domain.model

data class MahjongTile(
  val id: Int,                      // 0..143
  val face: TileFace,               // tipo do desenho
  val layer: Int,
  val row: Int,                     // half-cell coords
  val col: Int,
  val removed: Boolean = false
)

sealed class TileFace {
  data class Bamboo(val number: Int): TileFace()    // 1..9
  data class Character(val number: Int): TileFace()
  data class Circle(val number: Int): TileFace()
  enum class Wind { EAST, SOUTH, WEST, NORTH }
  data class WindFace(val wind: Wind): TileFace()
  enum class Dragon { RED, GREEN, WHITE }
  data class DragonFace(val dragon: Dragon): TileFace()
  data class Flower(val number: Int): TileFace()    // 1..4, equivalentes
  data class Season(val number: Int): TileFace()    // 1..4, equivalentes
}

fun TileFace.matches(other: TileFace): Boolean = when {
  this is TileFace.Flower && other is TileFace.Flower -> true
  this is TileFace.Season && other is TileFace.Season -> true
  else -> this == other
}
```

## 9. Algoritmo de detecção de "livre"

```kotlin
fun isFree(board: List<MahjongTile>, tile: MahjongTile): Boolean {
  if (tile.removed) return false
  val above = board.any { t ->
    !t.removed &&
    t.layer == tile.layer + 1 &&
    abs(t.row - tile.row) <= 1 &&
    abs(t.col - tile.col) <= 1
  }
  if (above) return false

  val sameLayer = board.filter { it.layer == tile.layer && !it.removed && it.id != tile.id }
  val leftBlocked = sameLayer.any { it.row == tile.row && it.col == tile.col - 2 }
  val rightBlocked = sameLayer.any { it.row == tile.row && it.col == tile.col + 2 }
  return !(leftBlocked && rightBlocked)
}
```

(Coordenadas em half-cells; peça ocupa 2 half-cells de largura.)

## 10. Embaralhamento

Quando o usuário pede embaralhar (ou começa uma partida): coletar todas as peças `face` ainda em jogo, permutar aleatoriamente, e reatribuir cada `face` a uma posição da lista de peças não-removidas. O **layout** não muda; só os desenhos rotacionam.

Solvabilidade não é garantida no Mahjong Solitaire clássico (parte do desafio). Mas registramos um warning interno se a permutação resultou em zero pares válidos no estado atual — sinalizamos "Sem movimentos" cedo.

## 11. Persistência

Snapshot serializado:

```kotlin
@Serializable
data class MahjongSnapshot(
  val layoutName: String,            // "TURTLE" no MVP
  val tiles: List<MahjongTileSnap>,
  val hintsUsed: Int,
  val undosUsed: Int,
  val reshufflesUsed: Int,
  val elapsedMs: Long,
  val undoStack: List<Pair<Int, Int>> // pares removidos em ordem, para undo
)
```

Auto-save após cada match ou após qualquer ação, com debounce 1 s.

## 12. Edge cases

| Situação | Comportamento |
|---|---|
| Usuário fecha o app no meio | Estado preservado |
| Dispositivo gira (não suportado no MVP) | Locked em portrait |
| Toque rápido em duas peças quase simultâneo | Fila de eventos serializa; primeira ganha "selecionada", segunda é o "match attempt" |
| Toque numa peça não-livre | Shake sutil + som tilt |
| Pedir dica quando não há par | Botão "Dica" fica dim/disabled; tap mostra mensagem "Sem pares disponíveis. Embaralhar?" |

## 13. Acessibilidade

- TalkBack: cada peça anuncia "{face}, {livre/não-livre}, linha {r}, coluna {c}"
- Modo "alvo grande": peças aumentadas 15% (reduz quantidade visível, mas facilita seleção)
- Sem dependência de cor: peças são reconhecíveis pelo desenho

## 14. Out of scope no MVP

- Layouts adicionais (Pyramid, Cardinal, etc.) — backlog
- Editor de layout
- Mahjong tradicional 4-jogadores (não é o mesmo jogo)
- Modos de tempo (corrida, contra-relógio)
