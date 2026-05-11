# Match-3 — Spec técnica

**Versão**: 1.0

---

## 1. Visão

Match-3 estilo Bejeweled clássico. Trocar duas gemas adjacentes; se a troca forma um match de ≥3 iguais em linha ou coluna, as gemas são removidas, gemas acima caem, novas entram pelo topo. Cascatas geram combos.

## 2. Estética das gemas

Em vez das clássicas "joias coloridas genéricas", adotamos **temática natural** — mais coerente com o resto do app:

| ID | Nome | Forma | Cor primária | Forma secundária (acessibilidade) |
|---|---|---|---|---|
| `RAIN` | Gota | Gota d'água | `#4A8AB8` (azul) | gota verticalizada |
| `LEAF` | Folha | Folha estilizada | `#5E8C5B` (verde) | nervura central |
| `BLOOM` | Flor | 4 pétalas | `#C95B85` (rosa) | pétalas simétricas |
| `SUN` | Sol | Círculo com raios | `#E5B23A` (amarelo) | raios em 8 direções |
| `MOON` | Lua | Crescente | `#7B5E8C` (roxo) | curva côncava |
| `EMBER` | Brasa | Chama estilizada | `#C97B5C` (laranja) | bico pontiagudo |

Cada gema é um `VectorDrawable` próprio, com versão "normal", "selecionada" (leve brilho) e "explodindo" (mid-animação).

Forma secundária garante distinguibilidade para daltonismo.

## 3. Tabuleiro

- **Grade**: 8 colunas × 8 linhas no MVP
- **Tamanho da célula**: calculado para preencher a largura disponível menos `space.md` de cada lado
- **Sem buracos** (gradiente futuro: tiles bloqueados em fases) — fora do MVP

## 4. Mecânica core

### 4.1 Geração inicial

Gerar 8×8 sem que existam matches no estado inicial.

```kotlin
fun generateInitialBoard(rng: Random): Array<Array<GemType>> {
  val board = Array(8) { arrayOfNulls<GemType>(8) }
  for (r in 0 until 8) for (c in 0 until 8) {
    val forbidden = mutableSetOf<GemType>()
    if (c >= 2 && board[r][c-1] == board[r][c-2]) forbidden += board[r][c-1]!!
    if (r >= 2 && board[r-1][c] == board[r-2][c]) forbidden += board[r-1][c]!!
    val available = GemType.values().filter { it !in forbidden }
    board[r][c] = available.random(rng)
  }
  // garantir pelo menos um swap válido; se não houver, gerar de novo
  return if (hasValidMove(board as Array<Array<GemType>>)) board else generateInitialBoard(rng)
}
```

### 4.2 Swap

- Tap em uma gema → seleciona (highlight)
- Tap em uma gema adjacente (orto) → tenta swap
- Swap válido (gera match ≥3) → executa, dispara cascade
- Swap inválido → gemas voltam à posição original com animação curta

Drag também suportado: arrastar uma gema para uma posição adjacente equivale ao tap-tap.

### 4.3 Detecção de match

Após cada swap (e após cada cascade), varrer:
- Horizontal: para cada linha, encontrar runs de 3+ gemas iguais consecutivas
- Vertical: idem por coluna
- Combinar runs que se cruzam em "T" ou "L" → consideradas matches especiais (ver §4.5)

### 4.4 Cascade

1. Marcar todas as gemas em match para remoção
2. Calcular pontos da rodada
3. Animação de explosão (240 ms) — escala 0 + partículas
4. Para cada coluna, fazer gemas acima caírem por gravidade (animação 300 ms)
5. Topo da coluna recebe novas gemas aleatórias
6. Verificar novamente; se novos matches surgirem, repetir (cascade level += 1)
7. Quando não houver mais matches, encerrar a rodada

Multiplicador de cascade: `score *= 1.0 + 0.5 * cascadeLevel`.

### 4.5 Gemas especiais

Geradas como bônus em matches de 4+ ou shapes especiais:

| Trigger | Gema gerada | Efeito ao ser combinada |
|---|---|---|
| Match horizontal de 4 | **Flame horizontal** | Limpa toda a linha |
| Match vertical de 4 | **Flame vertical** | Limpa toda a coluna |
| Match em "T" ou "L" (3+3) | **Bomb** | Limpa 3×3 ao redor |
| Match de 5+ em linha | **Star** | Limpa todas as gemas da mesma cor |

Gemas especiais herdam o tipo da gema base mas têm overlay visual claro (ex.: chama dentro da gota, estrela dentro da folha).

## 5. Modos de jogo

### 5.1 Modo Clássico (MVP)

- Tabuleiro 8×8
- Objetivo: alcançar pontuação por **fase**
- Fases têm objetivos crescentes: 5.000 (1) → 10.000 (2) → 20.000 (3)…
- Sem limite de movimentos no MVP — relaxante por design
- Concluir fase = atingir pontuação alvo → pode continuar ou parar
- Salvar como record o **pico de pontuação** + a **fase mais alta atingida**

### 5.2 Fora do MVP

- Modo "objetivos" (limpar X gemas de certa cor, em N movimentos)
- Modo cronometrado
- Tabuleiros com bloqueios

## 6. Pontuação

| Evento | Pontos base |
|---|---|
| Match 3 | 30 |
| Match 4 | 60 + criação de gema especial |
| Match 5+ | 100 + criação de gema especial |
| Ativação de Flame (linha/coluna) | 200 |
| Ativação de Bomb | 300 |
| Ativação de Star | 500 |
| Combo (cascade nível N) | base × (1 + 0.5 × N) |

Score acumulado pela partida.

## 7. Modelo de domínio

```kotlin
enum class GemType { RAIN, LEAF, BLOOM, SUN, MOON, EMBER }

sealed class CellContent {
  data class Normal(val type: GemType) : CellContent()
  data class FlameH(val type: GemType) : CellContent()
  data class FlameV(val type: GemType) : CellContent()
  data class Bomb(val type: GemType) : CellContent()
  data class Star(val type: GemType) : CellContent()
  object Empty : CellContent()
}

typealias Board = Array<Array<CellContent>>

data class Match3State(
  val board: Board,
  val score: Long,
  val stage: Int,
  val cascadeLevel: Int = 0
)
```

## 8. Persistência

```kotlin
@Serializable
data class Match3Snapshot(
  val cells: List<String>,         // 8x8 serializado linearmente
  val score: Long,
  val stage: Int,
  val elapsedMs: Long
)
```

## 9. Animações

- **Selecionar gema**: scale up to 1.08, brilho overlay, 120 ms
- **Swap válido**: gemas trocam de lugar, 200 ms, easing standard
- **Swap inválido**: gemas vão e voltam, 240 ms total
- **Match explode**: scale 1 → 1.2 → 0, opacity → 0, partículas, 320 ms
- **Queda**: simulação de gravidade com ease-out, 300 ms para 1 célula, escalável
- **Nova gema**: cai do topo com bounce sutil

## 10. Edge cases

| Situação | Comportamento |
|---|---|
| Tap-tap em gemas não-adjacentes | Primeira é "deselecionada", segunda fica selecionada |
| Swap durante animação em curso | Fila de inputs; bloquear input visualmente |
| Sem swaps válidos disponíveis | Auto-shuffle do tabuleiro (mantém score), aviso visual "Embaralhando…" |
| Acumular muito score (overflow) | Long; com `format("%,d", score)` para exibição |

## 11. Acessibilidade

- Daltonismo: formas garantem distinguibilidade (cada gema tem forma única)
- TalkBack: "Gota azul, linha 3 coluna 5. Toque para selecionar."
- Animações: respeitam toggle de reduzir movimento (cascades ficam instantâneas)

## 12. Métricas técnicas

| Métrica | Alvo |
|---|---|
| Frame rate em cascade longa | ≥ 58 fps |
| Latência de input (tap → start swap) | < 50 ms |
| Memória estável (sem leak após 30 min de jogo) | sim |
