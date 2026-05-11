# Design System — Fatima Games

**Versão**: 1.0 · Documento normativo. Toda interface do app deve aderir a estes tokens e componentes.

---

## 1. Filosofia

Três palavras: **calmo, claro, coeso**.

- **Calmo** — sem ruído visual. Whitespace é elemento de design. Animações servem para informar, nunca para impressionar.
- **Claro** — hierarquia tipográfica forte, contraste alto, alvos generosos, ações principais sempre evidentes.
- **Coeso** — um sistema, uma família tipográfica de display, uma de UI, uma paleta. Sem exceções "ah, mas só nessa tela".

## 2. Design tokens

Tokens são a fonte única da verdade. Em código Kotlin, são expostos em `theme/Tokens.kt` como objetos `data object` imutáveis, agrupados por categoria.

### 2.1 Cores

| Token | Light | Dark | Uso |
|---|---|---|---|
| `color.bg.canvas` | `#FAF6F0` | `#1C1A17` | Fundo de tela |
| `color.bg.surface` | `#FFFFFF` | `#26231F` | Cards, superfícies elevadas |
| `color.bg.surfaceMuted` | `#F2EBE0` | `#2A2722` | Linhas de lista, chips |
| `color.bg.tubeWell` | `#E9E1D3` | `#1F1C19` | Fundo de áreas de jogo (mesa) |
| `color.border.subtle` | `#E6DAC6` | `#3A3631` | Bordas decorativas |
| `color.border.strong` | `#C7B89F` | `#4D4842` | Bordas funcionais |
| `color.primary` | `#7A9B7E` | `#9DBFA1` | Botões primários, links |
| `color.primary.pressed` | `#4F6A53` | `#6E8A72` | Estado pressed |
| `color.accent.terracotta` | `#C97B5C` | `#D89578` | Ações secundárias, calor |
| `color.accent.plum` | `#7B5E8C` | `#9B7BAA` | Acentos especiais |
| `color.accent.gold` | `#D4A24A` | `#E5B970` | Records, conquistas |
| `color.text.primary` | `#2C2A26` | `#F2EBE0` | Texto principal |
| `color.text.secondary` | `#6B665E` | `#B5AEA1` | Texto secundário |
| `color.text.disabled` | `#A39E94` | `#6B665E` | Texto desabilitado |
| `color.text.inverse` | `#FAF6F0` | `#2C2A26` | Texto sobre primário |
| `color.feedback.success` | `#5E8C5B` | `#8AB489` | Confirmações, encaixe certo |
| `color.feedback.warning` | `#C9924A` | `#E0AC65` | Avisos |
| `color.feedback.error` | `#B8523A` | `#D17357` | Erros (raros neste produto) |

**Regras de contraste** (verificadas com WCAG):
- Texto principal sobre `bg.canvas` e `bg.surface`: contraste ≥ 7:1 (AAA)
- Texto secundário sobre `bg.canvas`: contraste ≥ 4.5:1 (AA)
- Texto sobre `primary`: usar `text.inverse`, contraste ≥ 4.5:1
- Foco visual (outline) sempre 2 dp com `primary` em ambos os modos

### 2.2 Tipografia

Duas famílias, ambas com licença SIL Open Font, empacotadas no APK (sem fetch em runtime).

| Família | Uso | Pesos incluídos |
|---|---|---|
| **Fraunces** (serif moderna) | Logo, títulos display, telas de vitória | 400, 500, 600 |
| **Inter** (sans-serif) | UI, texto corrido, números | 400, 500, 600, tabular |

Escala tipográfica (com `fontScale` multiplicador respeitado, e fator extra do app: Normal `1.0`, Grande `1.15`, Maior `1.30`):

| Token | Family | Size (sp) | Weight | Line height | Uso |
|---|---|---|---|---|---|
| `text.display.lg` | Fraunces | 36 | 600 | 44 | Tela de vitória |
| `text.display.md` | Fraunces | 28 | 500 | 36 | Header de tela principal |
| `text.title.lg` | Inter | 22 | 600 | 28 | Título de card grande |
| `text.title.md` | Inter | 20 | 600 | 26 | Título de card |
| `text.title.sm` | Inter | 18 | 600 | 24 | Título de seção |
| `text.body.lg` | Inter | 18 | 400 | 26 | Corpo principal |
| `text.body.md` | Inter | 16 | 400 | 24 | Corpo secundário |
| `text.label.lg` | Inter | 16 | 500 | 20 | Labels de botão grande |
| `text.label.md` | Inter | 14 | 500 | 18 | Chips, captions |
| `text.numeric.lg` | Inter (tabular) | 28 | 600 | 32 | Timer, score |
| `text.numeric.md` | Inter (tabular) | 20 | 600 | 24 | Stats de records |

### 2.3 Espaçamento

Base 4 dp. Tokens nomeados:

| Token | dp |
|---|---|
| `space.xxs` | 4 |
| `space.xs` | 8 |
| `space.sm` | 12 |
| `space.md` | 16 |
| `space.lg` | 24 |
| `space.xl` | 32 |
| `space.xxl` | 48 |
| `space.xxxl` | 64 |

Padding padrão de tela: `space.lg` (24 dp) lateral.
Gap padrão entre cards na home: `space.md` (16 dp).

### 2.4 Raios e elevação

| Token | dp | Uso |
|---|---|---|
| `radius.sm` | 8 | Chips, pills |
| `radius.md` | 12 | Botões, inputs |
| `radius.lg` | 20 | Cards |
| `radius.xl` | 28 | Modais, sheet |
| `radius.full` | 9999 | Ícones circulares, avatares |

Elevação (sombras) — apenas três níveis:

| Token | Spec | Uso |
|---|---|---|
| `elevation.0` | nenhuma | Tela base |
| `elevation.1` | `0 2 8 rgba(44,42,38,0.08)` | Cards repousando |
| `elevation.2` | `0 6 16 rgba(44,42,38,0.12)` | Card sendo arrastado, modal, FAB |

### 2.5 Motion (animações)

Curvas e durações padronizadas. Definidas como objetos Compose `tween`/`spring`.

| Token | Tipo | Duração | Easing | Uso |
|---|---|---|---|---|
| `motion.micro` | `tween` | 120 ms | `FastOutSlowInEasing` | Estados de botão (pressed) |
| `motion.standard` | `tween` | 240 ms | `FastOutSlowInEasing` | Transições de tela, swap |
| `motion.expressive` | `spring` | — | `dampingRatio=0.7, stiffness=300` | Encaixe de peça, snap |
| `motion.slow` | `tween` | 400 ms | `LinearOutSlowInEasing` | Líquido caindo no Color Sort |
| `motion.celebration` | `spring` + `tween` 600 ms | — | — | Tela de vitória |

Regra de redução: quando `Settings.Global.ANIMATOR_DURATION_SCALE` == 0 OU configuração do app "reduzir animações" ON, todas as durações são multiplicadas por `0.3` e `spring` vira `tween` linear.

### 2.6 Sombras de toque (haptics)

| Token | API | Quando |
|---|---|---|
| `haptic.tick` | `HapticFeedbackConstants.KEYBOARD_TAP` | Toque em peça selecionável |
| `haptic.snap` | `HapticFeedbackConstants.LONG_PRESS` | Encaixe de peça |
| `haptic.error` | `VibrationEffect.createOneShot(50, 80)` | Tentativa inválida |
| `haptic.win` | Sequência custom 3-pulse | Vitória |

## 3. Componentes

Componentes Compose vivem em `ui/components/`. Cada um tem preview, parâmetros documentados em KDoc e teste de snapshot.

### 3.1 Botões

| Variante | Altura | Padding H | Type token | Quando usar |
|---|---|---|---|---|
| `PrimaryButton` | 72 dp | 32 | `text.label.lg` | Ação principal da tela (1 por tela) |
| `SecondaryButton` | 64 dp | 24 | `text.label.lg` | Ação alternativa |
| `TertiaryButton` (texto) | 56 dp | 16 | `text.label.md` | Cancelar, voltar |
| `IconButton` | 56 dp | — | — | Toolbar, controles de jogo |

Estados: `default`, `pressed`, `disabled`. `pressed` reduz brightness em 10% + escala 0.98 em 120 ms.

### 3.2 Cards

| Variante | Quando |
|---|---|
| `GameCard` | Card de jogo na home (180 dp altura, ilustração + título + sub + record) |
| `StatCard` | Card de records (label + valor numérico em tabular) |
| `PhotoCard` | Item da biblioteca de fotos (thumbnail quadrado + label) |
| `EmptyStateCard` | Estado vazio com ilustração + texto + CTA |

### 3.3 Top bar

Sempre 72 dp de altura. Três variantes:
- `HomeTopBar` — saudação à esquerda, ícones (records, settings) à direita
- `GameTopBar` — voltar à esquerda, título centralizado, menu kebab à direita
- `SimpleTopBar` — voltar à esquerda, título centralizado, sem ação à direita (usada em settings, records)

### 3.4 Bottom action bar (em jogos)

64 dp de altura, fundo `bg.surface`, separador de 1 dp em `border.subtle` no topo. Contém ações contextuais do jogo (ex.: Desfazer, Dica, Embaralhar). Cada ação é um `IconButton` com label abaixo (12 sp).

### 3.5 Modais e sheets

- `Modal` (centralizado) para confirmações: Sair, Reiniciar. Backdrop em rgba(0,0,0,0.4). Cantos `radius.xl`. Padding `space.lg`.
- `Sheet` (bottom) para pausa em jogo, configurações rápidas. Drag handle de 4 dp × 32 dp no topo.

### 3.6 Estados de jogo

- `GamePauseOverlay` — semi-opaco, dois botões grandes empilhados ("Continuar", "Sair")
- `GameWinOverlay` — full-screen, animação de confete sutil, card central com stats e dois CTAs
- `GameNoMovesOverlay` — quando não há mais jogadas válidas; "Reiniciar" e "Sair"

### 3.7 Inputs e seletores

- `SegmentControl` — usado para escolher dificuldade no jigsaw (12/24/48/100). Altura 56 dp, fonte `text.label.lg`.
- `StepperLarge` — para tamanho de fonte. ±, label centralizado, alvos de 64 dp.
- `Toggle` — switch padrão Material 3, 64 dp tall row.

## 4. Iconografia

- Família única: **Phosphor Icons** (versão Regular ou Bold conforme o caso), licença MIT
- Peso fixo (2 dp stroke) em toda a UI
- Tamanho padrão 24 dp em barras, 20 dp inline, 32 dp em estados vazios
- Cor herda do contexto (texto primário ou primary)
- Lista controlada de ícones usados; expansão exige PR no design system

## 5. Ilustração e arte de jogo

### 5.1 Cards da home

Cada jogo tem uma ilustração própria, 1:1, estilizada com a paleta. Estilo: formas geométricas com leve textura, sombras chapadas. Renderizadas como `VectorDrawable` (XML) — escaláveis, sem aliasing.

### 5.2 Estados vazios

Quatro ilustrações leves para estados vazios (sem fotos, sem records, etc.). Mesmas regras de estilo.

### 5.3 Peças de jogo

Cada jogo tem seu sistema próprio de arte (ver specs específicas):
- Jigsaw: fotos do usuário recortadas dinamicamente
- Mahjong: SVGs derivados de `FluffyStuff/riichi-mahjong-tiles` + corpo 3D renderizado em Canvas
- Match-3: gemas como `VectorDrawable` próprias (6 tipos)
- Color Sort: tubos e líquido renderizados via Canvas com gradiente sutil

## 6. Acessibilidade — regras vinculantes

Itens com força normativa para qualquer tela:

1. **Alvo mínimo de toque**: 56 dp × 56 dp (regra do produto), salvo peças de jogo onde o tamanho é determinado pelo gameplay
2. **Espaçamento entre alvos**: ≥ 8 dp
3. **Texto mínimo**: `text.body.md` (16 sp) — abaixo disso, somente captions decorativas
4. **Contraste**: AA mínimo para qualquer texto significativo; AAA para texto principal
5. **`contentDescription`** em todo `Image` decorativo é `null`; em toda imagem informativa é descritivo
6. **Foco navegável**: ordem visual = ordem de foco; testar com tab/D-pad
7. **TalkBack** lê labels, valores, e estados (ex.: "Bambu três, livre, posição 2-4")
8. **Sem cor como único veículo de informação** — sempre acompanhar de forma, ícone ou texto

## 7. Microcopy

Tom de voz: caloroso, direto, sem infantilização, sem técnico.

Exemplos canônicos:

| Contexto | Errado | Certo |
|---|---|---|
| Tela vazia (sem fotos) | "Nenhuma foto encontrada" | "Suas fotos vão aparecer aqui. Toque em Adicionar para começar." |
| Erro ao carregar foto | "Erro 500" | "Não conseguimos abrir essa foto. Tenta outra?" |
| Confirmação de sair | "Tem certeza?" | "Sair da partida? Seu progresso fica salvo." |
| Vitória | "Você venceu!" | "Bom trabalho." |
| Sem movimentos no Color Sort | "Game over" | "Sem mais movimentos. Que tal recomeçar?" |

## 8. Tema escuro

- Não é um simples "inverter cores". Tokens dark são definidos manualmente acima.
- Imagens (fotos do usuário) não recebem tratamento — aparecem como são.
- Peças de Mahjong em dark mode têm fundo levemente off-white (`#E8DCC0`) para manter o caráter tradicional.

## 9. Implementação em Compose

Hierarquia de tema:

```
FatimaGamesTheme
├── LightColors / DarkColors (auto pelo sistema, configurável)
├── Typography (Fraunces + Inter, escalas)
├── Shapes (radii)
├── Spacing (custom CompositionLocal)
├── Motion (custom CompositionLocal)
└── Haptics (custom CompositionLocal)
```

Tokens são consumidos via `LocalAppTheme.current.color.primary` etc. — encapsulação clara, sem `Color(0xFF...)` espalhado por componentes.
