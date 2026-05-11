# Como receber o APK pronto sem precisar fazer nada local

Você tem dois caminhos. Use o que for mais confortável.

---

## Caminho A — Android Studio (você já tem, 5 minutos)

1. Android Studio → File → Open → `E:\FatimaGames`
2. Esperar o Gradle Sync (1–3 min na primeira vez)
3. Se aparecer arquivo em vermelho, abrir, `Alt+Enter` em cada erro → "Import" / "Quick Fix"
4. Clicar no ▶ verde com o celular conectado, OU
5. Menu Build → Build Bundle(s) / APK(s) → Build APK(s) → "locate" → arquivo em `app/build/outputs/apk/debug/app-debug.apk`

**É o caminho mais rápido.** Você já tem o IDE, o SDK provavelmente também já está baixado. O APK sai em ~5 minutos contando o sync inicial.

---

## Caminho B — GitHub Actions (zero ferramenta local, 10 minutos)

Para isso eu já deixei um workflow pronto em `.github/workflows/build-apk.yml`. O fluxo é:

### 1. Criar repositório no GitHub
- Acessa https://github.com/new
- Nome: `fatima-games` (ou o que quiser)
- Visibilidade: **Private** (recomendado, é app pessoal)
- NÃO marcar "Initialize with README"
- Criar

### 2. Subir o código

No terminal, dentro de `E:\FatimaGames`:

```bash
git init
git add .
git commit -m "Initial commit: Fatima Games MVP"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/fatima-games.git
git push -u origin main
```

(Quando pedir senha, use um Personal Access Token: github.com/settings/tokens → Generate new token → marca "repo" → cola no lugar da senha.)

### 3. O build roda sozinho

Assim que você dá push, o GitHub Actions ativa. Aba "Actions" do repositório → "Build Fatima Games APK" → aguarda ficar verde (~5–8 min).

### 4. Baixar o APK

- Clicar no run que terminou
- Rolar até "Artifacts" → `fatima-games-debug-apk` → Download
- Descompactar o zip → tem o `app-debug.apk` dentro
- Transferir para o celular (Drive, WhatsApp, USB) e tocar pra instalar
- Vai pedir para autorizar "Fontes desconhecidas" → autorizar

### 5. Atualizações futuras

Toda vez que você der `git push`, o APK novo é gerado automaticamente. Sem reabrir IDE, sem rodar comando.

---

## Por que eu não consegui te entregar o APK direto desta sessão

O ambiente em que eu rodo (sandbox Linux para experimentos) tem um proxy HTTP com **allowlist restrita**. Eu testei:

| Host | Resultado |
|---|---|
| `github.com` (páginas web e `git clone`) | ✅ Funciona |
| `pypi.org` (pacotes Python) | ✅ Funciona |
| `registry.npmjs.org` (pacotes Node) | ✅ Funciona |
| `dl.google.com` (Android SDK) | ❌ `X-Proxy-Error: blocked-by-allowlist` |
| `services.gradle.org` (Gradle binário) | ❌ Bloqueado |
| `repo1.maven.org` (Maven Central) | ❌ Bloqueado |
| `release-assets.githubusercontent.com` (binários de releases GitHub) | ❌ Bloqueado |
| `corretto.aws`, `cdn.azul.com`, `aka.ms` (JDKs alternativas) | ❌ Todas bloqueadas |

O proxy intercepta cada conexão HTTPS e devolve 403 quando o host não está na allowlist. Para construir um app Android nativo eu preciso baixar:

- JDK 17 (~200MB) — bloqueado
- Gradle 8.10 (~150MB) — bloqueado
- Android SDK platforms;android-34 (~70MB) — bloqueado
- Android SDK build-tools;34.0.0 (~50MB) — bloqueado
- ~200 dependências Maven (Compose, Hilt, Room, etc., totalizando ~300MB) — bloqueado

**Achei JDK 25 via PyPI (`pip install jdk4py`), mas sozinho não resolve.** Sem Gradle e sem os jars do Maven Central, o `gradle assembleDebug` não tem como rodar.

A solução real é deixar o ambiente que TEM acesso à internet rodar o build — daí o GitHub Actions, ou seu próprio Android Studio. Ambos os caminhos estão preparados acima.
