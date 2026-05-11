# PRD — Fatima Games

**Versão**: 1.0 · **Última revisão**: 2026-05-11 · **Status**: Aprovação pendente

---

## 1. Sumário executivo

Fatima Games é um app Android nativo que reúne quatro jogos casuais (quebra-cabeça, Mahjong Solitaire, Match-3, Color Sort) numa experiência deliberadamente sem-ansiedade, otimizada para acessibilidade visual e motora. O produto se diferencia de concorrentes do gênero por (a) ausência total de monetização intrusiva, (b) qualidade visual elevada das peças e (c) personalização real (uso de fotos próprias no quebra-cabeça, escalonamento de fonte e contraste).

O MVP é distribuído inicialmente via APK direto, com publicação na Google Play Store planejada para uma fase posterior.

## 2. Problema e oportunidade

### 2.1 Problema observado

Usuárias de 60+ que jogam quebra-cabeças e Mahjong em apps gratuitos enfrentam:

| Fricção | Impacto |
|---|---|
| Anúncios em vídeo no meio de partidas | Quebra do estado de fluxo, irritação |
| Pop-ups de IAP, "vidas", energia | Confusão sobre o que é jogo vs. cobrança |
| Texto pequeno e botões apertados | Dificuldade motora, erro de toque, abandono |
| Estética genérica, peças "low-poly" | Sensação de produto descartável |
| Quebra-cabeças com biblioteca pré-fixada | Falta de conexão emocional com a imagem |

### 2.2 Oportunidade

Construir um app pessoal/familiar que resolva todos os pontos acima, com qualidade gráfica acima da média da categoria gratuita, e que sirva como base extensível para incluir novos jogos ao longo do tempo. Custo de não-monetização é absorvido (uso pessoal/familiar), o que é uma vantagem competitiva genuína.

## 3. Público-alvo

### 3.1 Persona primária

**Fátima · 60+ · usuária Android**
- Joga 30–90 min por dia em momentos de descanso
- Usa óculos de leitura, contraste alto é preferência
- Tem fotos pessoais e da família que adoraria usar como quebra-cabeça
- Não baixa app novo com facilidade; quando encontra um bom, fica anos
- Não joga online, não compete em ranking, não compartilha em rede social

### 3.2 Persona secundária (familiar/curador)

**Caio · responsável técnico**
- Instala o app, configura preferências iniciais
- Adiciona fotos novas remotamente quando faz sentido (visita, comemoração)
- Recebe feedback informal de bugs/desejos

Detalhe em [personas.md](personas.md).

## 4. Escopo

### 4.1 Dentro do MVP (v1.0)

| # | Feature | Por quê |
|---|---|---|
| F1 | Tela inicial com galeria dos 4 jogos | Navegação principal |
| F2 | Quebra-cabeça com upload de foto da galeria/câmera | Diferencial-chave |
| F3 | Quebra-cabeça com 12 / 24 / 48 / 100 peças | Progressão de desafio |
| F4 | Mahjong Solitaire — layout tartaruga clássico | Pedido explícito da usuária |
| F5 | Match-3 estilo Bejeweled — grid 8×8, modo clássico | Variedade |
| F6 | Color Sort — 50 fases pré-desenhadas | Variedade |
| F7 | Records pessoais por jogo (tempo, pontos, partidas) | Acompanhamento de progresso |
| F8 | Configurações (tema, fonte, sons, vibração) | Acessibilidade real |
| F9 | Modo escuro | Conforto noturno |
| F10 | Auto-save de partida em andamento | Não perder trabalho |
| F11 | Biblioteca pessoal de fotos do usuário | Suporte ao F2 |

### 4.2 Fora do MVP (backlog)

- Modos de Mahjong adicionais (Pyramid, Cardinal, etc.)
- Match-3 com modos especiais (objetivos, limite de movimentos, fases)
- Gerador procedural de fases de Color Sort
- Conquistas e selos
- Múltiplos perfis no mesmo dispositivo
- Sincronização em nuvem
- Suporte a tablet com layout otimizado
- Suporte a orientação paisagem
- Trilha sonora ambiente
- Notificações de lembrete
- Internacionalização além de pt-BR

### 4.3 Explicitamente não-objetivos

- Multiplayer, ranking online, social
- Monetização (ads, IAP, assinatura, energia, moeda)
- Coleta de dados além do mínimo técnico (sem analytics de terceiros no MVP)

## 5. Métricas de sucesso

Como é um produto pessoal, métricas são qualitativas + de qualidade técnica.

### 5.1 Qualitativas (usuária)

- Sessão diária ≥ 20 min, sustentada por 4+ semanas após instalação
- Pelo menos 3 dos 4 jogos jogados regularmente (não só o favorito)
- Quebra-cabeça com foto pessoal é a feature mais usada (verificável por records)

### 5.2 Técnicas

| Métrica | Alvo |
|---|---|
| Crash-free sessions | ≥ 99,5% |
| Cold start | < 1500 ms no device-alvo |
| Frame rate sustentado em jogo | ≥ 58 fps |
| ANR | 0 em uso normal |
| Tamanho do APK | < 30 MB |
| Tempo de geração das peças (jigsaw 100 peças) | < 2 s |

### 5.3 Métrica de qualidade de experiência (heurística)

A cada release, validar manualmente em sessão com a usuária:
- Conseguiu navegar entre os jogos sem ajuda? (sim/não)
- Conseguiu iniciar um quebra-cabeça com foto sua? (sim/não)
- Houve alguma ação onde ela ficou parada ≥ 5 s? (sim/não — se sim, anotar)
- Houve toque acidental num botão errado? (contar)

## 6. Requisitos não-funcionais

| Categoria | Requisito |
|---|---|
| Plataforma | Android 8.0+ (API 26+), telas 5"–7" portrait |
| Performance | 60 fps target, 30 fps mínimo aceitável em jogo |
| Acessibilidade | WCAG 2.1 AA mínimo, AAA para texto principal |
| Privacidade | Nenhum dado sai do device sem ação explícita |
| Offline-first | Funciona 100% sem internet |
| Tamanho | < 30 MB APK; < 100 MB com dados de usuário típicos |
| Bateria | Sessão de 30 min consome < 8% em device médio |
| Recuperação | App nunca abre num estado inválido; estado de partida sempre íntegro |
| Internacionalização | pt-BR no MVP, estrutura preparada para outros locales |

## 7. Riscos

| # | Risco | Severidade | Mitigação |
|---|---|---|---|
| R1 | Performance do jigsaw com 200+ peças em device antigo | Alta | Cap inicial em 100 peças; perfilar antes de liberar 200 |
| R2 | Renderização das peças de Mahjong com look pobre | Média | Usar SVGs do `riichi-mahjong-tiles` (PD), renderizar 3D-falso em Canvas |
| R3 | Curva de aprendizado do Color Sort para usuária | Média | Tutorial interativo inline na primeira partida |
| R4 | Esforço subestimado do jigsaw | Alta | Tratar como o maior épico; começa primeiro |
| R5 | Foto vertical do celular distorcendo no jigsaw | Média | Crop guiado antes de iniciar a partida |
| R6 | Persistência corrompida em crash | Média | Auto-save com debounce + transações; testes de força |

## 8. Decisões registradas (ADRs resumidas)

| ADR | Decisão | Status |
|---|---|---|
| ADR-001 | Kotlin + Jetpack Compose como stack de UI | Aceito |
| ADR-002 | Min SDK 26 (Android 8.0) | Aceito |
| ADR-003 | Layout portrait apenas no MVP | Aceito |
| ADR-004 | Room para persistência local | Aceito |
| ADR-005 | Hilt para DI | Aceito |
| ADR-006 | Implementação nativa do recorte de jigsaw em Kotlin (sem lib externa) | Aceito |
| ADR-007 | Assets de Mahjong baseados em `FluffyStuff/riichi-mahjong-tiles` (domínio público) | Aceito |
| ADR-008 | Sem analytics de terceiros no MVP | Aceito |
| ADR-009 | Sem trilha sonora ambiente no MVP | Aceito |

Detalhes em [docs/05-engineering/standards.md](../05-engineering/standards.md#adrs).

## 9. Critérios de aceitação do MVP

O MVP é considerado pronto quando:

1. Os 4 jogos são jogáveis do início ao fim sem crashes
2. Records são gravados e exibidos corretamente
3. O quebra-cabeça aceita foto da galeria E da câmera
4. Toggle de modo escuro funciona em todas as telas
5. App roda offline 100%
6. Build de release assinado é gerado e instalável em device real
7. Sessão de teste de 60 min com a usuária-alvo é completada sem bloqueio
8. Métricas técnicas da seção 5.2 são atingidas em medição real
