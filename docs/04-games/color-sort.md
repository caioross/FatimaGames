# Color Sort — Spec técnica

**Versão**: 1.0

---

## 1. Visão

Jogo de organização: vários tubos contêm camadas de líquido colorido em ordem aleatória. O jogador transfere líquidos entre tubos até que cada tubo contenha uma única cor (ou esteja vazio).

## 2. Componentes do tabuleiro

| Elemento | Descrição |
|---|---|
| Tubo | Recipiente cilíndrico transparente, capacidade fixa de 4 unidades de líquido |
| Unidade de líquido | Faixa horizontal colorida, cada tubo cabe 4 |
| Cor | Identidade visual + identidade lógica; cada fase tem N cores distintas |

## 3. Regras

### 3.1 Transferência

Tocar em um tubo "origem" → tubo se eleva ~12 dp visualmente, fica em estado "armado".
Tocar em outro tubo "destino" → tenta transferir.

Transferência válida se:
1. Tubo origem não está vazio
2. Tubo destino tem espaço (não está cheio)
3. **Tubo destino está vazio** OU **cor no topo do destino é igual à cor no topo da origem**

A transferência move **todas as unidades contíguas da mesma cor no topo da origem**, até o limite do espaço do destino.

Exemplo:
- Origem (de baixo pra cima): [azul, vermelho, vermelho, vermelho]
- Destino (de baixo pra cima): [vermelho]
- Resultado: origem = [azul]; destino = [vermelho, vermelho, vermelho, vermelho]
- (As 3 vermelhas contíguas do topo da origem foram movidas)

### 3.2 Cancelar seleção

- Tocar novamente no mesmo tubo origem → "abaixa" e desarma
- Tocar em destino inválido → animação de "não" (shake) + permanece armado o origem

### 3.3 Vitória

Cada tubo está vazio OU contém uma única cor preenchendo as 4 unidades.

### 3.4 Sem movimentos

Quando nenhuma transferência válida está disponível e o objetivo não foi atingido. Mostrar overlay com:
- "Sem mais movimentos"
- Botões: "Desfazer", "Reiniciar fase", "Sair"

## 4. Catálogo de fases — MVP

50 fases pré-desenhadas, dificuldade progressiva:

| Fases | Cores | Tubos preenchidos | Tubos vazios | Capacidade |
|---|---|---|---|---|
| 1–5 | 3 | 3 | 2 | 4 |
| 6–10 | 4 | 4 | 2 | 4 |
| 11–20 | 5 | 5 | 2 | 4 |
| 21–30 | 6 | 6 | 2 | 4 |
| 31–40 | 7 | 7 | 2 | 4 |
| 41–50 | 8 | 8 | 2 | 4 |

As 5 primeiras fases servem como **tutorial implícito** — solucionáveis em 2–4 movimentos.

Cada fase é armazenada em `assets/colorsort/stages.json`:

```json
{
  "stages": [
    {
      "id": 1,
      "tubes": [
        ["RED", "BLUE", "RED", "BLUE"],
        ["BLUE", "RED", "BLUE", "RED"],
        ["RED", "BLUE", "RED", "BLUE"],
        [],
        []
      ]
    },
    ...
  ]
}
```

**Validação obrigatória**: toda fase pré-desenhada deve ter solução verificada por busca (BFS limitado). Pipeline de assets executa essa validação no build.

### 4.1 Solver para validação

```kotlin
fun isSolvable(tubes: List<List<Color>>, maxNodes: Int = 200_000): Boolean {
  val visited = HashSet<String>()
  val queue: ArrayDeque<List<List<Color>>> = ArrayDeque()
  queue.add(tubes)
  visited.add(stateKey(tubes))
  while (queue.isNotEmpty() && visited.size < maxNodes) {
    val state = queue.removeFirst()
    if (isWin(state)) return true
    for (next in legalMoves(state)) {
      val key = stateKey(next)
      if (key !in visited) {
        visited.add(key); queue.addLast(next)
      }
    }
  }
  return false
}
```

## 5. Ações disponíveis (bottom bar)

| Ação | Limite | Comportamento |
|---|---|---|
| **Desfazer** | Ilimitado | Reverte uma transferência |
| **Adicionar tubo** | 1 por fase | Cria um tubo vazio extra |
| **Reiniciar** | Ilimitado | Volta ao estado inicial da fase |

## 6. Pontuação e progresso

- Não há score numérico no Color Sort.
- Métricas registradas: **tempo**, **movimentos totais**, **uso de ajudas (desfazer/adicionar tubo)**, **fases concluídas**.
- Records mostram: fase mais alta, melhor tempo por fase 1–50, total de movimentos médio.

## 7. Modelo de domínio

```kotlin
enum class LiquidColor(val hex: Long) {
  RED(0xFFE24B4A), BLUE(0xFF378ADD), GREEN(0xFF639922),
  YELLOW(0xFFEFB12A), ORANGE(0xFFD85A30), PURPLE(0xFF7B5E8C),
  PINK(0xFFD4537E), CYAN(0xFF4FB3B3),
  TEAL(0xFF1D9E75), LIME(0xFF97C459)
}

/** Cada tubo: lista from-bottom-to-top. */
data class Tube(val units: List<LiquidColor>, val capacity: Int = 4) {
  val isEmpty get() = units.isEmpty()
  val isFull get() = units.size == capacity
  val top: LiquidColor? get() = units.lastOrNull()
  /** Quantas unidades da mesma cor estão contíguas no topo. */
  val topRunSize: Int get() {
    val t = top ?: return 0
    var n = 0; for (i in units.indices.reversed()) if (units[i] == t) n++ else break
    return n
  }
}

data class ColorSortState(
  val stage: Int,
  val tubes: List<Tube>,
  val movesMade: Int,
  val elapsedMs: Long,
  val extraTubeUsed: Boolean,
  val undoStack: List<Move>
)

data class Move(val fromIdx: Int, val toIdx: Int, val units: List<LiquidColor>)
```

## 8. Renderização

- Tubos desenhados em Canvas custom
  - Contorno: linha 1.5 dp cinza escuro, com base arredondada
  - Vidro: leve highlight branco transparente na lateral esquerda
- Líquido: retângulos preenchidos, cores conforme `LiquidColor`
- Transferência animada: gota colorida atravessa do bico do tubo origem para o bico do destino em arco parabólico (`motion.slow`, 400 ms); o líquido "cai" no destino com pequeno splash

### 8.1 Layout

- Até 6 tubos: linha única
- 7–10 tubos: duas linhas
- 11–14 tubos: duas linhas com ajuste de tamanho

Tamanho do tubo é responsivo (largura entre 36 dp e 48 dp).

## 9. Persistência

```kotlin
@Serializable
data class ColorSortSnapshot(
  val stage: Int,
  val tubes: List<List<String>>,     // cores serializadas
  val movesMade: Int,
  val elapsedMs: Long,
  val extraTubeUsed: Boolean,
  val undoStack: List<MoveSnap>
)
```

Como cada fase é pequena, snapshot é leve (< 1 KB tipicamente).

## 10. Acessibilidade

- **Cores + número de identificação**: no modo "acessibilidade alta", cada cor tem um numeral pequeno sobreposto no centro do bloco (1, 2, 3…). Toggle nas configurações.
- TalkBack: "Tubo 3, de baixo para cima: azul, azul, vermelho, amarelo. Toque para selecionar."
- Alvos: cada tubo tem touch area mínima de 60 dp largura
- Animação de líquido respeita "reduzir movimento" (vira fade)

## 11. Edge cases

| Situação | Comportamento |
|---|---|
| Tubo "armado" e o jogador toca em outro lugar (fora) | Tubo desarma |
| Múltiplos toques rápidos | Fila serial; bloquear input durante animação de transferência |
| Tentar transferir cor diferente para o topo | Animação shake + som tilt; tubo origem permanece armado |
| Estado salvo de uma fase removida do catálogo (futuro update) | Migration: recriar do stage atual; warn discreto |

## 12. Out of scope no MVP

- Gerador procedural de fases (ainda que o solver já esteja pronto para validação)
- Modos com tempo limitado
- Tubos com capacidades diferentes (3, 5, 6)
- Cores especiais (coringa, divisor)
