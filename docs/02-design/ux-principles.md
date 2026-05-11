# Princípios de UX e Acessibilidade

Documento normativo. Cada princípio é uma regra de design, não um ideal.

---

## 1. Os cinco princípios fundamentais

### P1 · Alvo generoso, espaçamento generoso

Touch targets mínimos de 56 dp; ações principais 72 dp+. Espaçamento mínimo entre alvos clicáveis = 8 dp. Razão: pessoas com tremor leve, dedos menos ágeis, ou usando o app na cama deitada não devem errar.

**Como verificar**: passar o "teste do polegar" — qualquer ação principal deve ser tocável com o polegar de uma mão segurando o aparelho na vertical.

### P2 · Hierarquia visual antes de texto

Antes de adicionar instrução textual, perguntar: "dá pra resolver com hierarquia (tamanho, cor, posição) ou ícone?" Texto é o último recurso. Quando inevitável, é grande (≥ 18 sp) e tem peso forte.

### P3 · Feedback imediato e proporcional

Toda ação tem retorno em ≤ 100 ms. Tipo de retorno proporcional à importância:
- Toque em peça selecionável: pulse visual + tick háptico
- Encaixe certo: animação + som + haptic snap
- Tentativa inválida: shake suave + som "tilt" + haptic erro
- Vitória: animação celebração + som + haptic win

### P4 · Estado nunca se perde, e o app sabe onde estava

Auto-save a cada jogada (debounce 1 s). Ao abrir o app, ele oferece "Continuar partida" se houver estado salvo. Voltar do background reapresenta o jogo no estado exato.

### P5 · Caminho de volta sempre visível

Ícone de voltar em toda tela que não é a home. Toque no ícone confirma se houver progresso a perder; caso contrário, volta direto. Botão de "Home" acessível em ≤ 2 toques de qualquer ponto.

---

## 2. Padrões de interação

### 2.1 Confirmações

Confirmação só para ações **destrutivas e irreversíveis**:
- Sair de partida em andamento (perda de progresso? Não, mas o usuário pode achar que sim — confirmar mesmo assim)
- Deletar foto da biblioteca
- Reiniciar partida

Não pedir confirmação para:
- Voltar para a home (auto-save cuida)
- Pausar
- Trocar configuração

Confirmação usa modal com dois botões grandes empilhados. Botão destrutivo é o **secundário** (não primário) para reduzir cliques acidentais.

### 2.2 Carregamento

- < 200 ms: nenhum indicador
- 200 ms – 1 s: skeleton ou progress sutil
- ≥ 1 s: indicador claro com texto ("Preparando peças…")
- Operações que sempre demoram (gerar peças do jigsaw) têm progresso determinístico

### 2.3 Erros

Erros são raríssimos neste produto (offline-first, sem rede). Quando acontecem:
- Linguagem amigável (não-técnica)
- Sempre oferecer um caminho de saída ("Tentar de novo" / "Escolher outra foto")
- Nunca culpar o usuário

### 2.4 Tutoriais

- **Jigsaw**: ao iniciar a primeira partida, um overlay com 3 passos curtos (arrastar, encaixar, zoom). Pode pular.
- **Mahjong**: explicação visual de "peça livre" em uma frase + animação curta na primeira partida.
- **Match-3**: animação demonstrativa antes da primeira partida (2 segundos de demo de swap + match).
- **Color Sort**: primeira fase é um tutorial implícito (tabuleiro mínimo, solução em 3 movimentos).

Cada jogo armazena se o tutorial já foi visto. Acessível depois via "Como jogar" no menu kebab.

---

## 3. Acessibilidade — checklist por tela

Toda PR de UI deve passar nesta checklist antes do merge:

| # | Checagem | Critério |
|---|---|---|
| A1 | Touch targets | Mínimo 56 dp × 56 dp (verificar com inspector) |
| A2 | Espaçamento entre alvos | ≥ 8 dp |
| A3 | Contraste de texto | AA mínimo (4.5:1), AAA preferido para texto primário |
| A4 | Fonte mínima | 16 sp, escalável com `fontScale` |
| A5 | `contentDescription` | Toda imagem/ícone informativo |
| A6 | Foco TalkBack | Ordem visual coerente |
| A7 | Cor não-única | Estado não depende só de cor |
| A8 | Animações | Respeitam "reduzir animações" |
| A9 | Háptica | Configurável (ligar/desligar) |
| A10 | Modo escuro | Sem texto invisível, sem imagens com fundo branco "estourado" |

---

## 4. Acessibilidade — implementação concreta

### 4.1 Fontes escaláveis

App expõe três níveis (`Normal 1.0`, `Grande 1.15`, `Maior 1.30`) que se **multiplicam** ao `fontScale` do sistema. Resultado é capado em `2.0` para evitar quebra de layout.

### 4.2 Modo daltônico

Match-3 e Color Sort: cada cor tem também uma **forma** secundária associada (ver specs). O Mahjong já é diferenciado por desenho.

### 4.3 TalkBack — labels esperadas

| Elemento | Label esperada |
|---|---|
| Card "Quebra-cabeça" na home | "Quebra-cabeça com suas fotos. Toque para abrir." |
| Peça de Mahjong | "Bambu três, livre, linha 2 coluna 4. Toque para selecionar." |
| Tubo de Color Sort | "Tubo 3, contém de baixo para cima: vermelho, vermelho, azul." |
| Botão Desfazer | "Desfazer última jogada." |
| Score | "Pontuação: mil e quinhentos pontos." |

### 4.4 Redução de movimento

Quando o usuário ativa "reduzir animações" no app (ou no sistema):
- Transições de tela ficam em 60 ms linear
- Animações de "respiração" e brilho são removidas
- Encaixe de peça vira instantâneo (sem spring)
- Confete da vitória é substituído por fade-in simples

---

## 5. Tom e linguagem

Princípios de microcopy:

- **Curto e direto**. Frases de até 8 palavras quando possível.
- **Caloroso, nunca infantil**. "Bom trabalho" sim, "Yupiiiii!" não.
- **Verbo no imperativo** em CTAs ("Adicionar foto", "Continuar").
- **Pessoal sem ser invasivo**. "Olá!" sim, "Olá, Fátima!" só se ela configurar o nome.
- **Sem jargão técnico**. "Falha ao decodificar bitmap" → "Não conseguimos abrir essa foto."
- **Sem gameficação artificial**. Sem "níveis épicos", sem "conquistas lendárias".

## 6. Critérios "uso pela usuária-alvo"

Heurísticas que aplicaremos em sessões de teste reais com a usuária:

1. **Tempo até o primeiro toque útil**: ao abrir o app, ela consegue iniciar um jogo em ≤ 15 s?
2. **Auto-explicação**: ela entende o que cada card faz sem perguntar?
3. **Reversibilidade**: quando errou, ela achou o caminho de volta?
4. **Conforto físico**: ela conseguiu jogar 20 min sem reclamar de cansaço visual?
5. **Memória**: na segunda sessão (24h depois), ela ainda lembra onde está cada coisa?

Cada uma dessas perguntas é registrada após cada sessão de teste em um log simples (data, jogo, observação). Esse log é uma das entradas para priorizar refinamentos pós-MVP.
