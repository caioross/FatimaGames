# Quebra-cabeça (Jigsaw) — Spec técnica

**Versão**: 1.0 · Documento normativo. Inclui o algoritmo canônico de recorte de peças com formato clássico de "tab e slot".

---

## 1. Visão do jogo

Quebra-cabeça clássico onde a usuária:

1. Escolhe uma imagem (galeria, câmera, biblioteca pessoal salva)
2. Define a dificuldade (12, 24, 48 ou 100 peças no MVP)
3. Recebe um conjunto de peças com **formato realista** (bordas com abas e encaixes), espalhadas sobre uma "mesa"
4. Arrasta peças até encaixá-las umas nas outras e na posição-alvo
5. Conclui quando todas as peças estão encaixadas

O diferencial visual é o realismo das peças: contornos clássicos com abas curvas, sombras suaves, fricção sutil ao deslizar — sensação de um quebra-cabeça físico.

## 2. Referências de implementação

Não usamos biblioteca externa para o recorte (decisão ADR-006). Implementamos nativamente em Kotlin + Compose Canvas, inspirados em:

- **Headbreaker** (JS) — modelo conceitual `Tab/Slot/Blank` por aresta ([github.com/flbulgarelli/headbreaker](https://github.com/flbulgarelli/headbreaker))
- **Shamim Akhtar Unity Tutorial** — matemática de cubic Bézier para o "knob" canônico ([github.com/shamim-akhtar/jigsaw-puzzle](https://github.com/shamim-akhtar/jigsaw-puzzle))
- **piecemaker** (Python) — referência de proporções padrão de aba ([github.com/jkenlooper/piecemaker](https://github.com/jkenlooper/piecemaker))

A lib `worldsproject/android-jigsaw-puzzle-library` foi consultada mas descartada (sem manutenção desde 2013, API datada).

## 3. Modelo de domínio

```kotlin
package com.fatimagames.app.feature.games.jigsaw.domain.model

/** Como uma aresta da peça se comporta. */
enum class EdgeType { FLAT, TAB, SLOT }

/** Lado da peça (numa malha 2D). */
enum class Side { TOP, RIGHT, BOTTOM, LEFT }

/** Definição de uma única peça antes de virar bitmap. */
data class PieceDefinition(
  val id: Int,                          // 0..N-1, linha-major
  val gridRow: Int,
  val gridCol: Int,
  val edges: Map<Side, EdgeType>,       // 4 lados
  val baseCellSizePx: Int,              // tamanho do "miolo" quadrado (sem abas)
  val knobInsetPx: Int                  // ~20% do cell size
)

/** Estado em tempo real durante o jogo. */
data class PieceState(
  val pieceId: Int,
  val xPx: Float,
  val yPx: Float,
  val rotationDeg: Float = 0f,          // futuro: rotação. MVP fixa em 0.
  val groupId: Int                      // peças encaixadas compartilham id
)

data class JigsawBoard(
  val rows: Int,
  val cols: Int,
  val imageWidthPx: Int,
  val imageHeightPx: Int,
  val cellSizePx: Int,
  val knobInsetPx: Int,
  val pieces: List<PieceDefinition>,
  val pieceBitmaps: Map<Int, ImageBitmap>,
  val initialStates: List<PieceState>
)
```

## 4. Algoritmo de recorte — passo a passo

### Passo 1 · Calcular a grade

Dado um número-alvo de peças `N` e uma imagem `W × H`, escolher (`cols`, `rows`) que:
- Maximiza `cols × rows == N` (preferencialmente exato; aceita ±5% no MVP)
- Mantém células aproximadamente quadradas (razão `cellW / cellH` entre 0.85 e 1.15)

Algoritmo:
```kotlin
fun chooseGrid(targetPieces: Int, imageAspect: Float): Pair<Int, Int> {
  val candidates = mutableListOf<Triple<Int, Int, Float>>()
  for (cols in 1..targetPieces) {
    val rows = (targetPieces / cols)
    if (rows == 0) continue
    val piecesActual = cols * rows
    val aspectError = abs(((cols.toFloat() / rows) / imageAspect) - 1f)
    val countError = abs(piecesActual - targetPieces).toFloat() / targetPieces
    val score = aspectError * 2f + countError
    candidates += Triple(cols, rows, score)
  }
  val best = candidates.minBy { it.third }
  return best.first to best.second
}
```

### Passo 2 · Tamanho de célula e inset do knob

```
cellW = imageWidth / cols
cellH = imageHeight / rows
cellSize = min(cellW, cellH)        // forçamos célula quadrada visualmente
knobInset = (cellSize * 0.20).roundToInt()   // 20% do lado, padrão da indústria
```

A imagem pode ser recortada (center-crop) para que `imageWidth / cellSize == cols` exatamente.

### Passo 3 · Atribuir tipos de aresta

Cada aresta interna do tabuleiro deve ter um `TAB` de um lado e um `SLOT` do outro (espelhado). Bordas externas são `FLAT`.

```kotlin
fun assignEdges(rows: Int, cols: Int, seed: Long): Array<Array<Map<Side, EdgeType>>> {
  val rng = Random(seed)
  val grid = Array(rows) { Array(cols) { mutableMapOf<Side, EdgeType>() } }

  for (r in 0 until rows) for (c in 0 until cols) {
    grid[r][c][Side.TOP] = when {
      r == 0 -> EdgeType.FLAT
      else -> opposite(grid[r-1][c][Side.BOTTOM]!!)
    }
    grid[r][c][Side.LEFT] = when {
      c == 0 -> EdgeType.FLAT
      else -> opposite(grid[r][c-1][Side.RIGHT]!!)
    }
    grid[r][c][Side.BOTTOM] = if (r == rows-1) EdgeType.FLAT else if (rng.nextBoolean()) EdgeType.TAB else EdgeType.SLOT
    grid[r][c][Side.RIGHT] = if (c == cols-1) EdgeType.FLAT else if (rng.nextBoolean()) EdgeType.TAB else EdgeType.SLOT
  }
  return grid.map { it.map { m -> m.toMap() }.toTypedArray() }.toTypedArray()
}

private fun opposite(e: EdgeType) = when(e) {
  EdgeType.TAB -> EdgeType.SLOT
  EdgeType.SLOT -> EdgeType.TAB
  EdgeType.FLAT -> EdgeType.FLAT
}
```

A `seed` permite reproduzir o mesmo corte (importante para retomar partida salva).

### Passo 4 · Path da peça (formato canônico)

O knob clássico de jigsaw é uma curva composta de **dois cubic Béziers** simétricos formando um "bulbo" que sai (TAB) ou entra (SLOT) do lado da célula.

Para uma aresta horizontal indo do ponto `A` ao ponto `B` (distância `L`), com direção de inflação `dir` (=`+1` para TAB saindo para baixo, `-1` para SLOT entrando para cima, `0` para FLAT):

```
P0 = A
P1 = A + (L*0.35, 0)
P2 = A + (L*0.35,  dir * knobInset * 0.6)   ; primeiro pescoço

P3 = A + (L*0.5,   dir * knobInset * 1.0)   ; topo do knob (esquerda)
P4 = A + (L*0.5,   dir * knobInset * 1.0)   ; topo do knob (direita)

P5 = A + (L*0.65,  dir * knobInset * 0.6)
P6 = A + (L*0.65, 0)
P7 = B
```

Sequência no `Path` (Compose):

```kotlin
fun appendEdge(path: Path, from: Offset, to: Offset, type: EdgeType, knobInset: Float, dir: Float) {
  if (type == EdgeType.FLAT) {
    path.lineTo(to.x, to.y)
    return
  }
  val sign = if (type == EdgeType.TAB) dir else -dir
  val dx = (to.x - from.x); val dy = (to.y - from.y)
  // vetor normal ao segmento
  val nx = -dy; val ny = dx
  val L = hypot(dx, dy)
  val nUnit = Offset(nx / L, ny / L)

  fun pt(t: Float, off: Float) =
    Offset(from.x + dx * t + nUnit.x * off * sign,
           from.y + dy * t + nUnit.y * off * sign)

  // entrada
  path.cubicTo(
    pt(0.30f, 0f).x, pt(0.30f, 0f).y,
    pt(0.35f, knobInset * 0.4f).x, pt(0.35f, knobInset * 0.4f).y,
    pt(0.40f, knobInset * 0.8f).x, pt(0.40f, knobInset * 0.8f).y
  )
  // topo
  path.cubicTo(
    pt(0.45f, knobInset * 1.15f).x, pt(0.45f, knobInset * 1.15f).y,
    pt(0.55f, knobInset * 1.15f).x, pt(0.55f, knobInset * 1.15f).y,
    pt(0.60f, knobInset * 0.8f).x,  pt(0.60f, knobInset * 0.8f).y
  )
  // saída
  path.cubicTo(
    pt(0.65f, knobInset * 0.4f).x, pt(0.65f, knobInset * 0.4f).y,
    pt(0.70f, 0f).x, pt(0.70f, 0f).y,
    to.x, to.y
  )
}
```

A constante `0.4 → 1.15 → 0.4` é o "shape" canônico de jigsaw clássico (validado contra fotos de peças reais e contra o formato do headbreaker). Pequena variação randomizada (±5%) por aresta dá variedade visual sem perder reconhecibilidade do encaixe.

### Passo 5 · Recortar bitmap da peça

```kotlin
fun renderPieceBitmap(
  sourceBitmap: Bitmap,
  piece: PieceDefinition,
  cellSizePx: Int,
  knobInsetPx: Int
): ImageBitmap {
  // tamanho do canvas: cellSize + 2*knobInset em cada eixo (knobs podem sair pra fora)
  val w = cellSizePx + 2 * knobInsetPx
  val h = cellSizePx + 2 * knobInsetPx
  val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(out)

  // construir Path do contorno da peça (em coordenadas locais do bitmap)
  val path = buildPiecePath(piece, cellSizePx, knobInsetPx)

  // clip pelo path
  canvas.save()
  canvas.clipPath(path)

  // desenhar a região da imagem original correspondente
  val srcX = piece.gridCol * cellSizePx - knobInsetPx
  val srcY = piece.gridRow * cellSizePx - knobInsetPx
  val srcRect = Rect(srcX, srcY, srcX + w, srcY + h)
  val dstRect = Rect(0, 0, w, h)
  canvas.drawBitmap(sourceBitmap, srcRect, dstRect, null)
  canvas.restore()

  // sombra/contorno: traço escuro 1.5dp + leve sombra interna
  val strokePaint = Paint().apply {
    style = Paint.Style.STROKE
    strokeWidth = 2f
    color = 0x55000000          // 33% black
    isAntiAlias = true
  }
  canvas.drawPath(path.asAndroidPath(), strokePaint)

  return out.asImageBitmap()
}
```

Áreas de `srcRect` que ficam fora da imagem original (em peças de borda) são preenchidas com transparente — o `clipPath` cuida disso.

### Passo 6 · Detecção de encaixe

Duas peças vizinhas estão "encaixadas" quando:
- São vizinhas na grade original (diferença de linha/coluna = 1)
- A distância entre suas posições atuais (referenciadas no canto superior-esquerdo do "miolo") difere da distância correta por **menos que `snapTolerancePx`**

```kotlin
const val SNAP_TOLERANCE_DP = 18   // ~25px em mdpi

fun checkSnap(a: PieceState, b: PieceState, defA: PieceDefinition, defB: PieceDefinition, cell: Int, tolPx: Float): Boolean {
  val dx = (defB.gridCol - defA.gridCol) * cell
  val dy = (defB.gridRow - defA.gridRow) * cell
  return abs((b.xPx - a.xPx) - dx) < tolPx && abs((b.yPx - a.yPx) - dy) < tolPx
}
```

Quando encaixa, ambas as peças passam a fazer parte do mesmo `groupId` (union-find). Arrastar uma peça do grupo move todas.

### Passo 7 · Detecção de vitória

Vitória quando: existe **um único `groupId`** que contém todas as peças.

## 5. Renderização e gestos

### 5.1 Compose Canvas custom

A área de jogo é um `Canvas` custom que:
- Aplica `translate` + `scale` do gesto de pan/zoom global
- Desenha cada peça `drawImage(piece.bitmap, piece.offset)`
- Desenha indicador de "guia" (silhueta da imagem) atrás, com alpha 0.15, se ligado

### 5.2 Gestos

| Gesto | Efeito |
|---|---|
| Tap em peça | Levanta a peça/grupo (elevação) |
| Drag | Move peça/grupo seguindo o dedo |
| Pinch | Zoom global (0.5× a 3.0×) |
| Two-finger drag | Pan global do tabuleiro |
| Long press | (futuro: rotacionar — fora do MVP) |

Hit-testing: como peças têm formato complexo, primeiro filtramos por bounding box; depois fazemos hit-test exato no `Path` da peça (`region.contains`).

### 5.3 Z-order

Peça arrastada vai para o topo da `drawOrder`. Outras peças/grupos mantêm a ordem de "última interação".

## 6. Persistência e retomada

Estado serializado conforme `JigsawSnapshot` em `architecture.md §3.4`. A `seed` no snapshot é o que permite **regerar o corte** identicamente ao retomar (o bitmap das peças não é persistido — é regerado on-demand).

Fluxo de retomada:
1. Ler `JigsawSnapshot` do Room
2. Carregar foto original do filesystem
3. Re-executar slicer com a `seed` salva
4. Reposicionar peças conforme `pieces[]`
5. Reagrupar via union-find a partir das peças que estão encaixadas (detectadas pela distância)

## 7. Configurações específicas do jigsaw

Acessíveis pelo menu kebab da tela do jogo:

- "Mostrar guia" (silhueta da imagem completa atrás) — toggle
- "Embaralhar peças soltas" — reorganiza peças não-encaixadas para não cobrirem o tabuleiro
- "Tamanho de exibição" — slider que escala todo o tabuleiro
- "Esconder peças de borda primeiro" (futuro)

## 8. Edge cases e tratamento

| Situação | Comportamento |
|---|---|
| Foto vertical (retrato) com puzzle horizontal | Crop guiado antes de iniciar; usuário ajusta o frame |
| Foto pequena (<1024 px no maior lado) | Aviso + sugestão de mínimo; permite seguir com aviso de qualidade |
| Foto muito grande (>8000 px) | Downscale automático para 4000 px no maior lado |
| Foto com EXIF rotation | Aplicar rotation antes do slicer |
| Memória insuficiente para 100 peças | Reduzir resolução das peças automaticamente; avisar se ficou em <medium quality |
| Usuário toca peça que está embaixo de outras | Toque "passa" para a peça do topo na pilha |

## 9. Acessibilidade no jigsaw

- "Mostrar guia" é a principal alavanca: facilita drasticamente a localização
- Modo "snap generoso": tolerância aumentada (36 dp em vez de 18 dp), configurável
- TalkBack: a tela como um todo é ferramenta visual; o TalkBack anuncia operações ("Peça encaixada", "Faltam 47 peças")

## 10. Métricas técnicas (alvo)

| Métrica | Alvo (device classe média) |
|---|---|
| Tempo de geração — 12 peças | < 200 ms |
| Tempo de geração — 48 peças | < 800 ms |
| Tempo de geração — 100 peças | < 2 s |
| Memória de bitmaps — 100 peças | < 80 MB |
| Frame time durante drag de grupo de 20 peças | < 16 ms |

## 11. Casos de teste fundamentais

| # | Cenário | Esperado |
|---|---|---|
| T1 | 12 peças, foto quadrada | Grade 4×3, peças geradas |
| T2 | 48 peças, foto 4:3 | Grade 8×6 ou similar |
| T3 | 100 peças, foto 3:4 vertical | Grade ~7×14, células ainda quadradas |
| T4 | Retomar partida com 30 peças encaixadas | Grupos reformados, posições preservadas |
| T5 | Foto com EXIF rotation 90° | Imagem aparece em pé |
| T6 | Tentar encaixe entre duas peças não-vizinhas | Não encaixa, peça volta à mão se soltar |
| T7 | Vitória com 12 peças | Animação dispara, record gravado |
| T8 | Memória sob pressão | Sem crash, downscale aplicado |

## 12. Out of scope no MVP

- Rotação de peças
- Peças com formas não-clássicas (triangulares, hexagonais, formato de fluxo)
- Multiplayer cooperativo
- Salvar puzzle parcialmente concluído como "álbum"
- Compartilhar imagem final
