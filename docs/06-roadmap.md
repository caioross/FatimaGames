# Roadmap — Fatima Games

**Versão**: 1.0 · Plano em fases. Cada fase termina com um APK instalável e testável.

---

## Resumo das fases

| Fase | Nome | Entregável |
|---|---|---|
| 0 | Fundação | Projeto Android compilando; tema e Home navegável |
| 1 | Quebra-cabeça | Jogo completo do jigsaw com fotos próprias |
| 2 | Mahjong | Mahjong Solitaire turtle jogável |
| 3 | Match-3 | Match-3 com cascade e gemas especiais |
| 4 | Color Sort | 50 fases de Color Sort + ações |
| 5 | Polimento | Records, configurações, modo escuro, acessibilidade |
| 6 | Release | Build assinado, APK instalado no device da usuária |
| 7+ | Pós-MVP | Backlog (Play Store, conquistas, novos jogos, etc.) |

## Fase 0 — Fundação

**Objetivos**
- Projeto Android com Gradle Kotlin DSL, Version Catalog, Hilt, Room, Compose, Material 3
- Tema completo aplicado (cores light/dark, tipografia, espaçamento, motion)
- Tela Splash + Home com os 4 cards estáticos navegáveis (telas-destino vazias)
- Stubs de Room funcionando (DAOs + 1 query de exemplo)
- Lint e CI básico passando

**Entregável**
APK debug que abre, mostra Home com cards bonitos, navega para 4 telas vazias com top bar.

**Critérios de aceite**
- App abre em < 1500 ms cold start
- Home está pixel-coerente com o mockup aprovado
- Toggle de modo escuro funciona na Home
- Lint zero erros, zero warnings críticos

## Fase 1 — Quebra-cabeça

**Objetivos**
- Tela de seleção: galeria, câmera, biblioteca pessoal
- Biblioteca pessoal de fotos (CRUD básico)
- Tela de escolha de dificuldade (12 / 24 / 48 / 100 peças)
- Geração de peças (slicer com Bézier conforme spec)
- Drag-and-drop, snap, agrupamento por groupId
- Pan/zoom da câmera
- Vitória + record salvo
- Auto-save de estado

**Entregável**
Jogador pode importar foto, jogar até completar, vitória dispara, record salvo.

**Critérios de aceite**
- Slicer gera 100 peças em < 2 s no device-alvo
- Snap funciona, peças encaixam, grupos arrastam corretos
- Retomar partida funciona após fechar/abrir o app
- Mín 5 dos 8 casos de teste do spec passando

## Fase 2 — Mahjong

**Objetivos**
- Pipeline de assets: SVG → VectorDrawable, 42 faces
- Layout turtle (definição + renderização 3D-falsa)
- Detecção de peça livre
- Lógica de match com equivalências (Flores, Estações)
- Dica, Desfazer, Embaralhar
- Vitória + record

**Entregável**
Mahjong jogável do início ao fim.

**Critérios de aceite**
- Renderização da pilha sem artefatos visuais
- isFree retorna correto em 100% dos casos de teste
- Animação de match suave 60 fps

## Fase 3 — Match-3

**Objetivos**
- Tabuleiro 8×8 com 6 gemas (VectorDrawable)
- Swap com validação
- Detecção de matches (3, 4, 5, T/L)
- Cascade com gravidade animada
- Gemas especiais (Flame, Bomb, Star)
- Pontuação + fases
- Vitória/continuação

**Entregável**
Match-3 jogável; possibilidade de jogar por horas com pontuação acumulando.

**Critérios de aceite**
- Frame rate ≥ 58 fps em cascade longa
- Sem inputs perdidos durante animações
- Gemas especiais funcionam conforme matrix do spec

## Fase 4 — Color Sort

**Objetivos**
- Renderização de tubos com líquido (Canvas)
- Lógica de transferência (regras §3.1 do spec)
- Banco de 50 fases pré-validadas (com solver)
- Animação de líquido caindo
- Desfazer, Adicionar tubo, Reiniciar
- Vitória + record

**Entregável**
50 fases de Color Sort jogáveis.

**Critérios de aceite**
- Todas as 50 fases são solucionáveis (verificado por solver no build)
- Estado salva e retoma corretamente
- Animação de líquido fluida

## Fase 5 — Polimento

**Objetivos**
- Tela de records completa (por jogo, com mini gráficos)
- Tela de configurações com todos os toggles do spec
- Modo escuro 100% funcional em todas as telas
- Escala de fonte (Normal/Grande/Maior) aplicada globalmente
- Toggle de sons e háptica
- Toggle de "reduzir animações"
- Microcopy revisado em todas as telas
- Tutoriais inline na primeira partida de cada jogo
- Estados vazios em todas as telas

**Entregável**
App polido, pronto para ser usado por horas sem fricção.

**Critérios de aceite**
- WCAG AA validado por ferramenta + checklist manual
- Sessão de teste de 60 min com a usuária-alvo sem bloqueios
- Crash-free ≥ 99,5% em uso simulado

## Fase 6 — Release

**Objetivos**
- Gerar keystore de release
- Build assinado (`assembleRelease`)
- Verificar tamanho < 30 MB
- Testar em device real do dia-a-dia da usuária
- Instalar APK no celular
- Sessão de uso inicial supervisionada

**Entregável**
Fatima Games rodando no celular da Fátima.

**Critérios de aceite**
- App instala via APK direto sem erro
- Funciona 100% offline
- Métricas técnicas batem alvos do PRD §5.2

## Fase 7+ — Pós-MVP (backlog priorizado)

Em ordem indicativa, não fixa:

1. **Trilha sonora ambiente** (com toggle e seleção de tema musical)
2. **Conquistas/selos** (gamificação leve, opt-in)
3. **Mahjong: layouts adicionais** (Pyramid, Cardinal, etc.)
4. **Match-3: modo objetivos**
5. **Color Sort: gerador procedural infinito**
6. **Internacionalização** (en, es)
7. **Google Play Store** (conta de dev, política de privacidade, screenshots, descrição)
8. **Paciência (jogo de cartas)** — novo jogo
9. **Sudoku** — novo jogo
10. **Sincronização opcional em nuvem** (Drive/iCloud) — exige análise de privacidade

---

## Riscos consolidados

(replicado do PRD §7, mantido aqui para visibilidade no roadmap)

| # | Risco | Severidade | Mitigação | Status |
|---|---|---|---|---|
| R1 | Jigsaw com 200 peças em device antigo | Alta | Cap em 100 no MVP | Aceito |
| R2 | Mahjong com look pobre | Média | Assets PD + Canvas 3D-falso | Mitigado |
| R3 | Color Sort com curva difícil | Média | Primeiras 5 fases ultra-fáceis | Mitigado |
| R4 | Esforço do jigsaw subestimado | Alta | Tratar como épico maior; primeira fase | Aceito |
| R5 | Foto vertical distorcendo | Média | Crop guiado | Mitigado |
| R6 | Persistência corrompida | Média | Auto-save com debounce + transações | Mitigado |

## Estimativa de esforço (indicativo, não compromisso)

Como não é um projeto comercial com sprint planning, as estimativas servem para detectar fases que estão "esticando". Em sessões de codificação focadas:

| Fase | Esforço relativo |
|---|---|
| 0 | 1 unidade |
| 1 (Jigsaw) | 3–4 unidades |
| 2 (Mahjong) | 2 unidades |
| 3 (Match-3) | 2 unidades |
| 4 (Color Sort) | 1,5 unidade |
| 5 (Polimento) | 1,5 unidade |
| 6 (Release) | 0,5 unidade |

Onde "1 unidade" ≈ uma sessão de codificação dedicada de algumas horas. Total estimado: ~12 unidades.

## Definition of Done (DoD) geral por feature

Toda feature/PR antes de ser declarada concluída:

- [ ] Implementação completa conforme spec
- [ ] Testes unitários para lógica de domínio (≥ alvo de cobertura)
- [ ] Pelo menos 1 teste de integração ou UI cobrindo o happy path
- [ ] Sem regressões no app (smoke test manual: abre, navega, joga 1 partida)
- [ ] Sem warnings novos de lint
- [ ] Tema light + dark verificados
- [ ] Acessibilidade: TalkBack lê os elementos novos, contraste OK
- [ ] Documentação do spec atualizada se houve mudança
- [ ] APK debug builda
