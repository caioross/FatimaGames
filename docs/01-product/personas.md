# Personas e contexto de uso

---

## Persona primária — Fátima

| | |
|---|---|
| **Idade** | 60+ |
| **Dispositivo** | Celular Android (referência: tela 6" classe média) |
| **Tempo de uso típico** | 30–90 min/dia, em sessões |
| **Quando joga** | Manhã com café, tardes lentas, antes de dormir |
| **Postura** | Sentada em poltrona / deitada na cama / na mesa |
| **Acessibilidade** | Óculos de leitura; prefere contraste alto; dedos com mobilidade normal |
| **Familiaridade com tecnologia** | Confortável com WhatsApp e fotos; instalar app novo é tarefa que delega |
| **Sentimento desejado** | Calma, prazer estético, sensação de evolução pessoal |

### O que ela ama nos jogos atuais

- Quebra-cabeças com imagens bonitas (paisagens, animais, arte)
- Mahjong porque é "limpinho, sem barulho, dá pra raciocinar"
- A possibilidade de "passar de fase"

### O que a irrita

- Anúncios em vídeo de 30 s, principalmente quando aparecem no meio
- Mensagens piscando "compre vidas!"
- Texto pequeno demais nas instruções
- Não conseguir achar o botão de voltar
- "Daqueles jogos que de repente vira outro jogo"

### Como ela descobriu o que gosta

Boca-a-boca da família, sobrinha que mostrou um app uma vez, marketplace da Play Store (mas instala pouco — desconfia).

---

## Persona secundária — Caio (curador)

| | |
|---|---|
| **Papel** | Filho que instala, configura, mantém |
| **Frequência de toque no app** | Mensal (adicionar fotos, ver se está tudo certo) |
| **Habilidades** | Técnico, instala APK, configura |
| **O que precisa do app** | Confiabilidade, não dar trabalho, "deixar ela feliz" |

### Necessidades específicas

- Adicionar fotos sem precisar mexer no celular dela ("dá pra colocar via algum caminho?" — fora do MVP, mas é uma ideia)
- Reset/limpeza de records se precisar (configurações)
- Ter certeza de que não vai dar tela azul nem comportamento estranho

---

## Cenários de uso

### Cenário 1 — Sessão da manhã

Fátima acorda, faz café, senta na poltrona. Abre o app. Espera ver imediatamente o que ela jogou ontem (continuar partida) ou poder escolher rapidamente um jogo novo.

**Implicação**: a home precisa ter um "Continuar" proeminente quando há partida em andamento.

### Cenário 2 — Foto nova da família

Caio compartilhou no WhatsApp uma foto da neta no aniversário. Fátima salva a foto na galeria. Quer fazer um quebra-cabeça dessa foto.

**Implicação**: o caminho "galeria → quebra-cabeça" precisa ser direto. Idealmente de qualquer momento, ela toca "Quebra-cabeça" → "Foto nova" → galeria → escolhe → escolhe dificuldade → joga.

### Cenário 3 — Cansaço visual à noite

22h, luz baixa, ela quer jogar um pouco. Tela branca clara dói.

**Implicação**: modo escuro real, ativável manualmente OU automático por hora do dia (configuração).

### Cenário 4 — Interrupção (telefone tocou)

Está no meio de uma partida de Mahjong. Telefone toca, ela atende. Volta 10 min depois.

**Implicação**: estado de jogo preservado integralmente; ao abrir, está exatamente como deixou.

### Cenário 5 — "Quero mostrar pra alguém"

Visita em casa. Quer mostrar como tá indo bem nos quebra-cabeças.

**Implicação**: tela de records orgulhosa, com curva de melhoria visível.

---

## Anti-personas (para quem NÃO estamos otimizando)

- Jogador competitivo que quer ranking online
- Jogador casual de comutação ("3 min na fila do banco")
- Adolescente buscando jogos virais
- Power-user que quer estatísticas detalhadas em planilha

Saber para quem NÃO estamos projetando ajuda a recusar features tentadoras que não servem ao público real.
