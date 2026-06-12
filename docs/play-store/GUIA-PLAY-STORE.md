# 📲 Guia de publicação na Google Play — Fatima Games

Tudo o que você precisa para subir o **Fatima Games** na Google Play Store: textos
prontos para copiar/colar (PT e EN), respostas dos formulários obrigatórios, e os
arquivos de imagem nos tamanhos exatos exigidos.

> **Status técnico:** o app já está pronto para envio — `targetSdk 35` (exigência da
> Play desde ago/2025), AAB assinado com keystore de upload, sem permissões
> sensíveis e 100% offline. Veja [§7 Checklist](#7-checklist-final).

---

## 1. O pacote (AAB)

- **Arquivo:** `app/build/outputs/bundle/release/app-release.aab`
- **Assinatura:** chave de upload `fatimagames-upload-key.jks` (alias `fatimagames-upload`).
  Ative o **Play App Signing** ao criar o app (recomendado e padrão) — você envia o AAB
  com a chave de upload e o Google gerencia a chave de assinatura final.
- **applicationId:** `com.fatimagames.app` &nbsp;·&nbsp; **versionName:** `1.0.0` &nbsp;·&nbsp; **versionCode:** `1`
- **Min Android:** 8.0 (API 26) &nbsp;·&nbsp; **Target:** Android 15 (API 35)

> ⚠️ **Guarde o keystore.** Faça backup do `fatimagames-upload-key.jks` e das senhas
> (em `keystore.properties`). Com o Play App Signing, perder a chave de upload é
> recuperável pela Play Console, mas é um transtorno — guarde em local seguro.

---

## 2. Ficha da loja (Main store listing)

### 2.1 Nome do app  *(máx. 30 caracteres)*

```
Fatima Games
```

### 2.2 Descrição curta  *(máx. 80 caracteres)*

**Português**
```
Jogos casuais calmos, sem anúncios e offline. Quebra-cabeça, Mahjong e mais.
```

**English**
```
Calm, ad-free, offline casual games. Jigsaw, Mahjong, Solitaire and more.
```

### 2.3 Descrição completa  *(máx. 4000 caracteres)*

**Português**
```
Fatima Games é uma coleção de jogos casuais feita para relaxar — não para viciar.
Sem anúncios, sem compras, sem pressa. Só você, oito clássicos e um tempo tranquilo
para a mente descansar.

🎮 OITO JOGOS, UM SÓ LUGAR CALMO
• Quebra-cabeça — monte suas próprias fotos em peças, de 12 a 100.
• Mahjong — o solitário de peças no layout tartaruga, com dica, desfazer e embaralhar.
• Combinar gemas — alinhe três ou mais e veja a cascata de cores acontecer.
• Organizar cores — transfira líquidos entre tubos; as fases são sempre solucionáveis.
• Paciência — o Klondike de cartas de sempre, limpo e sem pressa.
• Campo Minado — pura dedução tranquila, em três níveis de dificuldade.
• Tetris — encaixe as peças e complete linhas no seu ritmo.
• Sapo aventureiro — ajude o sapinho a atravessar, um pulo de cada vez.

🍃 FEITO PARA ACOLHER
• Sem anúncios em vídeo, sem pop-ups, sem energia que acaba.
• Funciona 100% offline — sem internet e sem cadastro.
• Não coletamos nada: suas fotos e recordes ficam só no seu aparelho.

♿ ACESSIBILIDADE EM PRIMEIRO LUGAR
• Letras grandes e ajustáveis, contraste alto e modo escuro completo.
• Botões generosos e bem espaçados — fácil de tocar, até com uma mão só.
• Suas partidas são salvas automaticamente: volte exatamente de onde parou.

💚 Fatima Games nasceu de um desejo simples: dar à minha mãe — e a quem mais quiser —
jogos bonitos e tranquilos, sem as armadilhas dos apps gratuitos de sempre.
Baixe, jogue e relaxe.
```

**English**
```
Fatima Games is a collection of casual games made to help you relax — not to hook you.
No ads, no purchases, no rush. Just you, eight classics and a calm moment for your mind.

🎮 EIGHT GAMES, ONE PEACEFUL PLACE
• Jigsaw — piece together your own photos, from 12 to 100 pieces.
• Mahjong — the turtle-layout tile solitaire, with hint, undo and shuffle.
• Match Gems — line up three or more and watch the colorful cascade.
• Color Sort — pour liquids between tubes; every level is always solvable.
• Solitaire — the classic Klondike card game, clean and unhurried.
• Minesweeper — calm, pure deduction across three difficulty levels.
• Tetris — fit the blocks and clear lines at your own pace.
• Frog Crossing — help the little frog cross, one hop at a time.

🍃 MADE TO WELCOME YOU
• No video ads, no pop-ups, no energy that runs out.
• Works 100% offline — no internet, no sign-up.
• We collect nothing: your photos and records stay on your device.

♿ ACCESSIBILITY FIRST
• Large, adjustable text, high contrast and a full dark mode.
• Generous, well-spaced buttons — easy to tap, even with one hand.
• Your games are saved automatically: pick up right where you left off.

💚 Fatima Games started from a simple wish: to give my mother — and anyone else —
beautiful, peaceful games without the traps of the usual free apps.
Download, play and relax.
```

---

## 3. Recursos gráficos (arquivos prontos nesta pasta)

| Recurso | Tamanho / formato exigido | Arquivo | Status |
|---|---|---|---|
| **Ícone do app** | 512 × 512 px · PNG 32-bit · ≤ 1 MB · sem transparência | [`icon-512.png`](icon-512.png) | ✅ pronto |
| **Gráfico de destaque** (feature graphic) | 1024 × 500 px · PNG/JPG · ≤ 15 MB | [`feature-graphic-1024x500.png`](feature-graphic-1024x500.png) | ✅ pronto |
| **Screenshots de celular** | 2 a 8 imagens · PNG/JPG · lado de 320–3840 px · proporção máx. 2:1 | — | ⚠️ capturar (veja §4) |
| Screenshots de tablet 7" e 10" | opcional · mesmas regras | — | opcional |
| Vídeo promocional | opcional · link do YouTube | — | opcional |

> `icon-512-original.png` é o redimensionamento direto do `icon.png` (com o fundo
> escuro da arte), incluído só para comparação. **Use o `icon-512.png`** (recortado e
> full-bleed). Os ativos podem ser regerados com `python docs/play-store/_generate_assets.py`.

---

## 4. Screenshots — como capturar  *(obrigatório: mínimo 2)*

A Play exige screenshots **reais** do app (não mockups). O app é retrato (portrait),
então capture em **9:16** — por exemplo **1080 × 1920** ou **1080 × 2400**.

**Telas sugeridas (capture 4 a 8):**
1. **Início** — a grade dos 8 jogos com a saudação
2. **Quebra-cabeça** — uma foto em peças
3. **Mahjong** — o layout tartaruga
4. **Organizar cores** — os tubos coloridos
5. **Combinar gemas** ou **Tetris** — tabuleiro colorido
6. **Tela de vitória** — confete + estatísticas
7. **Configurações** — mostrando tema/escala de fonte (acessibilidade)

**Capturando via emulador ou aparelho (com depuração USB):**
```bash
# 1. Instale o app de debug (ou rode pelo Android Studio) e abra a tela desejada
# 2. Capture direto para o PC:
adb exec-out screencap -p > docs/play-store/screenshots/01-inicio.png
# repita navegando por cada tela (02-jigsaw.png, 03-mahjong.png, ...)
```

> Dica: no emulador, use um perfil **Pixel** (1080 × 2400). Tire as fotos com o
> **modo claro** e algumas no **modo escuro** para mostrar o recurso.

---

## 5. Configurações da loja (Store settings)

| Campo | Valor sugerido |
|---|---|
| **Tipo de app** | Jogo (Game) |
| **Categoria** | Casual *(alternativas: Quebra-cabeça / Puzzle)* |
| **Tags** | quebra-cabeça, casual, relaxante, offline, sem anúncios |
| **E-mail de contato** | *(seu e-mail de suporte — obrigatório e público)* |
| **Site** | `https://fatimagames.vercel.app` |
| **Política de privacidade** | `https://fatimagames.vercel.app/privacidade` *(veja §6)* |

---

## 6. Conteúdo do app (App content) — respostas dos formulários

### 6.1 Política de privacidade  *(obrigatória)*

Hospede a política e informe a URL na Play Console. **Já incluímos uma página pronta no
site:** publique o site e use `https://fatimagames.vercel.app/privacidade`. O texto também
está em [`POLITICA-DE-PRIVACIDADE.md`](POLITICA-DE-PRIVACIDADE.md) (PT + EN).

### 6.2 Anúncios

> **O app contém anúncios?** → **Não.**

### 6.3 Acesso ao app (App access)

> **Todas as funcionalidades estão disponíveis sem restrições** (sem login, sem
> credenciais). Selecione *"All functionality is available without special access".*

### 6.4 Classificação de conteúdo (questionário IARC)

Categoria: **Jogo**. Respostas:

| Pergunta | Resposta |
|---|---|
| Violência | Nenhuma |
| Conteúdo sexual / nudez | Nenhum |
| Linguagem imprópria | Nenhuma |
| Substâncias controladas | Nenhuma |
| **Jogos de azar** (simulado ou real) | **Não** *(Paciência/Mahjong são jogos de carta/peça, sem aposta)* |
| Medo / terror | Nenhum |
| Compartilha localização | Não |
| Permite compras digitais | Não |
| Interação entre usuários / conteúdo gerado | Não |

➡️ **Resultado esperado:** **Livre / Everyone (L)**.

### 6.5 Público-alvo e conteúdo (Target audience)

| Campo | Valor sugerido |
|---|---|
| Faixas etárias-alvo | **18 e mais** *(ou 13+)* — o app é familiar e seguro, mas **não** é direcionado a crianças; isso evita as exigências extras do programa *Designed for Families*. |
| O app é atraente para crianças? | Não direcionado a crianças |

### 6.6 Segurança de dados (Data safety)  *(o ponto forte do app)*

O app **não tem permissão de internet** e roda totalmente offline — nada é transmitido.

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha dados do usuário? | **Não** |
| Coleta algum tipo de dado obrigatório? | **Não** — as fotos do quebra-cabeça são escolhidas pelo **seletor de fotos do Android** e ficam **só no aparelho**; nada sai do dispositivo. |
| Os dados são criptografados em trânsito? | N/A (nenhum dado trafega) |
| Há como solicitar exclusão de dados? | Os dados ficam no aparelho; desinstalar o app remove tudo. |

➡️ **Resultado:** *"No data collected"* e *"No data shared"*.

### 6.7 Outras declarações

- **App do governo?** Não · **Recursos financeiros?** Não · **Saúde?** Não
- **Apenas para uso interno/testes?** Não (é um app público)

---

## 7. Checklist final

**Técnico (já resolvido neste projeto):**
- [x] `targetSdk 35` (Android 15) — exigência da Play desde 31/ago/2025
- [x] AAB assinado com keystore de **upload** (não debug)
- [x] Permissões mínimas — só `VIBRATE`; **sem CAMERA / mídia / internet**
- [x] App 64-bit (Kotlin/Compose, sem libs nativas) — AAB cobre todas as ABIs
- [x] Minify/R8 com regras de ProGuard validadas
- [x] Edge-to-edge do Android 15 tratado (opt-out para preservar o layout)

**Na Play Console (você preenche):**
- [ ] Criar o app e ativar **Play App Signing**
- [ ] Ficha da loja: nome, descrições, ícone, feature graphic (§2 e §3)
- [ ] Subir **≥ 2 screenshots** de celular (§4)
- [ ] Política de privacidade publicada e URL informada (§6.1)
- [ ] Classificação de conteúdo respondida (§6.4)
- [ ] Público-alvo (§6.5) e Segurança de dados (§6.6)
- [ ] Preço: **Grátis** · Países/regiões: a definir
- [ ] Subir o `app-release.aab` em **Testes internos** primeiro → depois Produção

> 💡 **Recomendado:** comece por **Teste interno** (libera em minutos, sem revisão
> completa) para instalar no celular da sua mãe e validar. Depois promova para Produção.

---

## 8. Como regerar o AAB

```bash
# Pré-requisitos: JDK 17 + Android SDK (platform 35), keystore.properties na raiz.
./gradlew :app:bundleRelease
# Saída: app/build/outputs/bundle/release/app-release.aab
```

Para gerar também um APK universal de teste a partir do AAB (opcional), use o
[bundletool](https://github.com/google/bundletool):
```bash
bundletool build-apks --bundle=app-release.aab --output=app.apks \
  --ks=fatimagames-upload-key.jks --ks-key-alias=fatimagames-upload --mode=universal
```
