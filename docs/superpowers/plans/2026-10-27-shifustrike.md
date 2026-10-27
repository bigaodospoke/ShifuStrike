# ShifuStrike: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir o mod Fabric 1.21.1 "ShifuStrike". Ele roda um evento de Counter-Strike (`/cs`) no servidor Eclipse Cobblemon usando as armas do PointBlank, com tela de compra (B) e HUD no cliente.

**Architecture:**
- Regras puras (relógio da rodada, vitória, economia, compra, colete) ficam em `core/`, sem classes do Minecraft, testadas com JUnit.
- A partida (`match/`) usa essas regras e cuida dos jogadores reais: inventário guardado em disco, loadout, morte, C4 e espectador.
- Proteções (`guard/` + mixins Java) bloqueiam dano, blocos, comandos, itens e Cobblemon.
- O cliente recebe o estado por payloads e desenha a tela de compra e o HUD.
- O PointBlank é usado só pelo id do item e pelos componentes de dados; nunca chamamos classes dele.

**Tech Stack:**
- Kotlin 2.2.20, Fabric Loom 1.10.5, mapeamentos Mojang + Parchment 2024.11.17, Fabric API 0.116.17+1.21.1.
- Cobblemon 1.7.3 (compile), PointBlank 2.2.0 + GeckoLib 4.7.7 (só runtime de dev).
- Testes: JUnit 5 (lógica pura) e Fabric GameTest (integração com jogadores falsos).

**Spec:** `docs/superpowers/specs/2026-10-26-shifustrike-design.md`. Leia junto; este plano não repete as regras, só as decisões de implementação.

## Global Constraints

- Minecraft `1.21.1`, Java 21, Kotlin, `splitEnvironmentSourceSets()` (`main` + `client`). Mixins em Java, como no `shifutower`.
- `fabric.mod.json`:
  - depends: `fabricloader >=0.16.0`, `minecraft ~1.21.1`, `java >=21`, `fabric-api >=0.116.0`, `fabric-language-kotlin >=1.13.6+kotlin.2.2.20`, `cobblemon >=1.7.3`;
  - recommends: `pointblank >=2.2.0`. Sem PointBlank o servidor sobe e a loja fica sem armas, mas não trava.
- Id do mod `shifustrike`, pacote `com.shifustrike`, comandos `/cs`.
- **Nunca** colocar nada do PointBlank no nosso jar. O jar dele fica em `libs/`, que é ignorado pelo git, e é usado só no runtime de dev. Ícones de armas são gerados no cliente a partir das texturas do PointBlank.
- **Nunca** usar arquivos da Valve (sons, ícones, texturas). Sons só do Minecraft.
- Todo texto visível em português do Brasil: `pt_br.json` e `en_us.json` com o mesmo texto em português. Mensagens de chat do servidor são `Component.literal` em português, todas no `match/Msg.kt`.
- Estilo do código: nomes de classes e funções em inglês; comentários, campos de config e nomes de testes em português, como em `D:\Mod de corrida`.
- Permissão de staff: `.requires { it.hasPermission(2) }`, como em `shifurace/command/RaceCommands.kt:154`.
- Payloads: padrão de `D:\Mod de corrida\src\main\kotlin\com\shifurace\network\Payloads.kt` (`CustomPacketPayload` + `StreamCodec.of`). Enviar sempre checando `ServerPlayNetworking.canSend(player, TYPE)`.
- Escrita em disco: sempre `SafeFiles.writeAtomic` (copiar `D:\Mod de corrida\src\main\kotlin\com\shifurace\util\SafeFiles.kt`).
- Config em `config/shifustrike/config.json` e `config/shifustrike/loja.json`. Mapa e inventários salvos em `<mundo>/shifustrike/`.
- Tempos internos em ticks (20 por segundo). A config é em segundos.
- Nada sobe para o servidor real nem para o modpack nesta entrega.

## Review Focus

1. **Servidor cai no meio da partida.** Quem entrar de novo tem que receber o inventário, o modo de jogo e a vida originais, no ponto de saída, sem nenhum item do evento. Teste na Tarefa 8 (`restaura_depois_de_reinicio`).
2. **Portador da C4 desconecta ou sai com `/cs sair` no meio da rodada.** A C4 cai no chão e a rodada continua. Quem volta entra como espectador. Teste na Tarefa 12 (`c4_cai_quando_portador_desconecta`).
3. **Granada ou explosão.** Dentro da área do mapa não quebra blocos. Fora da área, e com partida parada, tudo continua como antes. Teste na Tarefa 15 (`explosao_fora_da_area_quebra_normal`).
4. **Comprar duas vezes no mesmo tick** (clique duplo, pacote repetido). O dinheiro nunca fica negativo e não sai arma em dobro. Testes nas Tarefas 5 (`mesma_arma_duas_vezes`) e 13 (`compra_dupla_no_mesmo_tick`).
5. **Morto (espectador) usa `/cs sair` ou a staff cancela a partida.** O jogador volta ao modo de jogo original, com a câmera nele mesmo e sem modificadores de congelamento. Teste na Tarefa 11 (`cancelar_com_jogador_morto_restaura`).

---

## Estrutura de arquivos

```
build.gradle.kts, settings.gradle.kts, gradle.properties, gradlew(.bat), gradle/wrapper/*   (copiados de D:\Mod de corrida)
.gitignore                         (o da corrida + libs/*.jar)
libs/pointblank-fabric-1.21-2.2.0.jar   (cópia local, nunca versionada)
tools/gerar_recursos.js            ícones e texturas próprios (faca, c4, kit, colete, capacete, "em breve", icon.png)
run/selftest.sh                    comandos de console para runServer
src/main/kotlin/com/shifustrike/
  ShifuStrike.kt                   entrypoint, registra tudo
  core/Team.kt Box.kt RoundClock.kt RoundRules.kt Economy.kt TeamBalancer.kt ArmorMath.kt Shop.kt BuyRules.kt
  config/StrikeConfig.kt ShopConfig.kt ConfigManager.kt
  arena/ArenaData.kt ArenaStorage.kt
  util/SafeFiles.kt
  data/PlayerSnapshot.kt SnapshotStorage.kt
  registry/ModItems.kt
  item/C4Item.kt DefuseKitItem.kt UseHints.kt
  pointblank/PointBlankItems.kt HeadshotFlag.kt EventItems.kt
  match/Msg.kt Participant.kt Match.kt MatchManager.kt Loadouts.kt Bomb.kt Deaths.kt Spectators.kt Purchases.kt
  guard/DamageGuard.kt AreaGuard.kt CommandGuard.kt CobblemonGuard.kt InteractionGuard.kt
  command/StrikeCommands.kt
  network/Payloads.kt ServerNetworking.kt
  gametest/*.kt                    testes de integração (entrypoint fabric-gametest)
src/main/java/com/shifustrike/mixin/
  ExplosionMixin.java LevelMixin.java CommandsMixin.java ItemEntityMixin.java LivingEntityMixin.java HurtingItemMixin.java
src/main/resources/fabric.mod.json, shifustrike.mixins.json, assets/shifustrike/...
src/client/kotlin/com/shifustrike/client/
  ShifuStrikeClient.kt ClientMatch.kt
  screen/BuyScreen.kt screen/IconCache.kt
  hud/StrikeHud.kt hud/KillFeed.kt
src/client/java/com/shifustrike/mixin/client/GuiMixin.java
src/client/resources/shifustrike.client.mixins.json
src/test/kotlin/com/shifustrike/core/*Test.kt (+ config/, network/)
```

---

### Task 1: Projeto, build e ambiente de dev com PointBlank

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `.gitignore`, `LICENSE` (MIT), `README.md` (curto, em português)
- Create: `src/main/kotlin/com/shifustrike/ShifuStrike.kt`, `src/client/kotlin/com/shifustrike/client/ShifuStrikeClient.kt`, `src/main/kotlin/com/shifustrike/util/SafeFiles.kt`
- Create: `src/main/resources/fabric.mod.json`, `src/main/resources/shifustrike.mixins.json` (lista vazia), `src/client/resources/shifustrike.client.mixins.json` (lista vazia)
- Create: `src/main/kotlin/com/shifustrike/gametest/SmokeGameTest.kt`
- Test: `src/test/kotlin/com/shifustrike/SmokeTest.kt`

**Interfaces:**
- Produces: `ShifuStrike.MOD_ID = "shifustrike"`, `ShifuStrike.LOGGER`, `ShifuStrike.id(path): ResourceLocation`, `SafeFiles.writeAtomic(path: Path, text: String)`.

- [ ] **Step 1: Criar o repositório git e copiar a base**

```bash
cd "/d/Mod Cs pra minecraft" && git init
cp -r "/d/Mod de corrida/gradle" "/d/Mod de corrida/gradlew" "/d/Mod de corrida/gradlew.bat" "/d/Mod de corrida/.gitignore" .
mkdir -p libs && cp "/e/Site Eclipse Cobblemon/mods recomendados/pointblank-fabric-1.21-2.2.0.jar" libs/
printf 'libs/*.jar\n' >> .gitignore
```

- [ ] **Step 2: Escrever `gradle.properties` e `build.gradle.kts`**

Copie o `build.gradle.kts` de `D:\Mod de corrida` e mude só isto:
- `gradle.properties`: `loader_version=0.18.4` (o PointBlank exige ≥ 0.18.4; se o Loom 1.10.5 recusar, suba o Loom para a 1.11 mais nova e anote), `mod_version=1.0.0`, `maven_group=com.shifustrike`, `archives_base_name=shifustrike`. O resto fica igual (minecraft, parchment, fabric_api `0.116.17+1.21.1`, kotlin, cobblemon `1.7.3+1.21.1`).
- `loom.mods.register("shifustrike")`.
- Repositórios a mais: `maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { name = "GeckoLib" }` e `maven("https://api.modrinth.com/maven") { name = "Modrinth" }`.
- Dependências a mais: `modLocalRuntime("software.bernie.geckolib:geckolib-fabric-1.21.1:4.7.7")` e `modLocalRuntime(files("libs/pointblank-fabric-1.21-2.2.0.jar"))`.
- Run de GameTest:

```kotlin
loom {
    runs {
        create("gametest") {
            server()
            name("GameTest")
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.get()}/gametest/junit.xml")
            runDir("build/gametest")
        }
    }
}
```

- `settings.gradle.kts`: `rootProject.name = "ShifuStrike"`.

- [ ] **Step 3: Escrever o `fabric.mod.json`**

Siga os valores das Global Constraints. Use `"environment": "*"`. Entrypoints: `main` → `com.shifustrike.ShifuStrike` (adapter kotlin), `client` → `com.shifustrike.client.ShifuStrikeClient` (adapter kotlin), `fabric-gametest` → `com.shifustrike.gametest.SmokeGameTest` (adapter default; as próximas tarefas acrescentam classes aqui). Mixins: `shifustrike.mixins.json` e `{config: shifustrike.client.mixins.json, environment: client}`.

- [ ] **Step 4: Escrever o teste unitário e o GameTest de fumaça**

```kotlin
class SmokeTest { @Test fun `id do mod`() = assertEquals("shifustrike:x", ShifuStrike.id("x").toString()) }
```

`SmokeGameTest` é uma classe que implementa `FabricGameTest`. Ela tem `@GameTest(template = FabricGameTest.EMPTY_STRUCTURE) fun pointblank_carregado(h: GameTestHelper)`, que confere `BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("pointblank:ak47"))` e chama `h.succeed()`.

- [ ] **Step 5: Rodar**

Run: `./gradlew build test runGametest`
Expected: `BUILD SUCCESSFUL`. No log do gametest aparece `All 1 required tests passed`, além de `ShifuStrike carregado` (o `LOGGER.info` do `onInitialize`).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "chore: estrutura do ShifuStrike com PointBlank no runtime de dev"
```

---

### Task 2: Relógio da rodada e regras de vitória

**Files:**
- Create: `src/main/kotlin/com/shifustrike/core/Team.kt`, `Box.kt`, `RoundClock.kt`, `RoundRules.kt`
- Test: `src/test/kotlin/com/shifustrike/core/RoundClockTest.kt`, `RoundRulesTest.kt`, `BoxTest.kt`

**Interfaces:**
- Produces:
  - `enum class Team { CT, TR; fun other(): Team }`
  - `data class Box(minX..maxZ: Int)` com `companion fun of(ax, ay, az, bx, by, bz): Box` (ordena os cantos) e `fun contains(x: Double, y: Double, z: Double): Boolean`. Os limites incluem o bloco inteiro: `x in minX.toDouble() until maxX + 1.0`.
  - `enum class Phase { FREEZE, LIVE, PLANTED, ROUND_END, MATCH_END }`
  - `data class RoundTimings(freezeTicks=200, buyTicks=600, roundTicks=2300, bombTicks=800, roundEndTicks=100)`
  - `class RoundClock(t: RoundTimings)`, com:
    - `phase`, `ticks`;
    - `startRound()`, `tick()`, `plantBomb()`, `endRound()`, `endMatch()`;
    - `buyOpen`, `frozen`, `roundTimeLeft`, `roundTimeOver`, `bombTimeLeft`, `bombExploded`, `roundEndDone`.
  - `enum class BombState { NONE, PLANTED, EXPLODED, DEFUSED }`, `enum class WinReason { ELIMINATION, TIME, BOMB_EXPLODED, BOMB_DEFUSED, FORFEIT }`
  - `data class RoundOutcome(winner: Team, reason: WinReason)`, `data class RoundSnapshot(ctAlive, trAlive, ctStarted, trStarted: Int, bomb: BombState, timeOver: Boolean)`
  - `object RoundRules { fun evaluate(s: RoundSnapshot): RoundOutcome?; fun matchWinner(ct: Int, tr: Int, toWin: Int): Team? }`

- [ ] **Step 1: Escrever os testes que falham**

Testes do relógio:
- `congela 10s e depois libera`: depois de `startRound()` e 199 `tick()`, a fase é `FREEZE`. No tick 200 vira `LIVE`.
- `compra aberta ate 30s`: `buyOpen` é true em `ticks == 599` e false em `600`.
- `rodada de 1min55 conta depois do congelamento`: `roundTimeOver` fica true exatamente em `ticks == 200 + 2300`.
- `plantar para o relogio da rodada`: depois de `plantBomb()`, `roundTimeLeft` não muda nos ticks seguintes e `roundTimeOver` continua false.
- `bomba explode 40s depois`: `bombExploded` fica true 800 ticks depois do `plantBomb()`, nem um antes.
- `fim de rodada dura 5s`: `roundEndDone` fica true 100 ticks depois do `endRound()`.

Testes de regras (os valores da seção 7 da spec):
- `bomba explodiu ganha TR`
- `bomba desarmada ganha CT`
- `CT todos mortos ganha TR mesmo com bomba plantada`
- `TR todos mortos sem bomba ganha CT`
- `TR todos mortos com bomba plantada continua` (`evaluate` devolve null)
- `tempo acabou sem bomba ganha CT por TIME`
- `time vazio desde o inicio nao perde por eliminacao`: com `ctStarted=0, ctAlive=0` e TR vivo, `evaluate` devolve null.
- `primeiro a 5 vence`: `matchWinner(5, 3, 5) == CT` e `matchWinner(4, 4, 5) == null`.

Testes de caixa:
- `caixa normaliza cantos e inclui o bloco final`: `Box.of(5,0,5, 0,3,0).contains(5.9, 3.5, 0.0)` é true, e `contains(6.0, 0.0, 0.0)` é false.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew test`
Expected: FAIL de compilação, porque as classes ainda não existem.

- [ ] **Step 3: Implementar**

A ordem de `RoundRules.evaluate` é fixa:
1. `EXPLODED` → TR;
2. `DEFUSED` → CT;
3. `ctStarted > 0 && ctAlive == 0` → TR (ELIMINATION);
4. `trStarted > 0 && trAlive == 0 && bomb == NONE` → CT (ELIMINATION);
5. `timeOver && bomb == NONE` → CT (TIME);
6. senão `null`.

`roundTimeLeft`:
- em FREEZE vale `roundTicks`;
- em LIVE vale `roundTicks - (ticks - freezeTicks)`, com mínimo 0;
- em PLANTED, ROUND_END e MATCH_END fica congelado no último valor.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew test`
Expected: PASS.

- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(core): relógio da rodada e regras de vitória"`

---

### Task 3: Economia

**Files:**
- Create: `src/main/kotlin/com/shifustrike/core/Economy.kt`
- Test: `src/test/kotlin/com/shifustrike/core/EconomyTest.kt`

**Interfaces:**
- Consumes: `Team`, `WinReason`, `RoundOutcome` (Task 2).
- Produces:
  - `enum class KillCategory { PADRAO, SMG, ESCOPETA, AWP, FACA }`
  - `data class EconomyRules(...)` com os padrões da spec: `startMoney=500, maxMoney=16000, winElimination=3250, winBomb=3500, lossBase=1400, lossStep=500, lossMax=3400, plantBonus=800`, e `killRewards = mapOf(PADRAO to 300, SMG to 600, ESCOPETA to 900, AWP to 100, FACA to 1500)`.
  - `object Economy`, com:
    - `fun winReward(r, reason: WinReason): Int`. ELIMINATION, TIME e FORFEIT dão `winElimination`; as duas razões de bomba dão `winBomb`.
    - `fun lossReward(r, consecutiveLosses: Int): Int`. O argumento já conta a derrota atual, então vale ≥ 1.
    - `fun roundPayout(r, outcome: RoundOutcome, loserStreak: Int, bombWasPlanted: Boolean): Map<Team, Int>`. Devolve quanto **cada jogador** de cada time ganha.
    - `fun killReward(r, c: KillCategory): Int`
    - `fun add(r, money: Int, delta: Int): Int`, com o resultado preso entre 0 e `maxMoney`.

- [ ] **Step 1: Escrever os testes que falham**
  - `derrotas seguidas sobem 500 ate 3400`: `(1..6).map { lossReward(r, it) } == listOf(1400, 1900, 2400, 2900, 3400, 3400)`.
  - `vitoria por bomba paga 3500 e por eliminacao 3250`.
  - `TR que perde depois de plantar ganha 800 a mais`: `roundPayout(r, RoundOutcome(CT, BOMB_DEFUSED), 1, true)[TR] == 1400 + 800` e `[CT] == 3500`.
  - `CT que perde nao ganha bonus de plantio`: `roundPayout(r, RoundOutcome(TR, BOMB_EXPLODED), 2, true)[CT] == 1900`.
  - `limite de 16000`: `add(r, 15900, 300) == 16000`.
  - `kill por categoria`: AWP 100, FACA 1500, SMG 600, ESCOPETA 900, PADRAO 300.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew test` → FAIL.
- [ ] **Step 3: Implementar** — `lossReward = min(lossBase + lossStep * (n - 1), lossMax)`.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew test` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(core): economia do CS"`

---

### Task 4: Divisão de times e colete

**Files:**
- Create: `src/main/kotlin/com/shifustrike/core/TeamBalancer.kt`, `ArmorMath.kt`
- Test: `src/test/kotlin/com/shifustrike/core/TeamBalancerTest.kt`, `ArmorMathTest.kt`

**Interfaces:**
- Produces:
  - `object TeamBalancer { fun <T> split(ids: List<T>, random: kotlin.random.Random): Map<T, Team> }`. Embaralha e alterna os times. A diferença entre eles nunca passa de 1.
  - `data class ArmorResult(healthDamage: Float, armorLeft: Int)`
  - `object ArmorMath { fun apply(damage: Float, armor: Int, helmet: Boolean, headshot: Boolean, ratio: Float = 0.5f, bonus: Float = 0.5f): ArmorResult }`. Trabalha em pontos do CS (vida 100).

- [ ] **Step 1: Escrever os testes que falham**
  - `times equilibrados`: para n de 1 a 11, `abs(#CT - #TR) <= 1` e todos os ids aparecem uma vez.
  - `mesma semente mesma divisao`: `split(ids, Random(42))` dá o mesmo resultado duas vezes.
  - `colete corta metade no corpo`: `apply(100f, 100, false, false) == ArmorResult(50f, 75)`.
  - `sem capacete headshot ignora colete`: `apply(100f, 100, false, true) == ArmorResult(100f, 100)`.
  - `com capacete headshot usa colete`: `apply(100f, 100, true, true) == ArmorResult(50f, 75)`.
  - `colete acaba no meio do tiro`: `apply(100f, 10, false, false) == ArmorResult(80f, 0)`.
  - `sem colete nada muda`: `apply(40f, 0, true, false) == ArmorResult(40f, 0)`.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew test` → FAIL.
- [ ] **Step 3: Implementar**

```text
se armor <= 0 ou (headshot e !helmet): (damage, armor)
health = damage * ratio; armorDmg = (damage - health) * bonus
se armorDmg > armor: health = damage - armor / bonus; armorDmg = armor
armorLeft = max(0, round(armor - armorDmg))
```

- [ ] **Step 4: Rodar e ver passar** — `./gradlew test` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(core): divisão de times e cálculo do colete"`

---

### Task 5: Catálogo da loja e regras de compra

**Files:**
- Create: `src/main/kotlin/com/shifustrike/core/Shop.kt`, `BuyRules.kt`
- Test: `src/test/kotlin/com/shifustrike/core/ShopTest.kt`, `BuyRulesTest.kt`

**Interfaces:**
- Consumes: `Team`, `KillCategory`.
- Produces:
  - `enum class EntryKind { PRIMARIA, PISTOLA, GRANADA, COLETE, COLETE_CAPACETE, EM_BREVE }`
  - `data class ShopEntry(chave: String, time: Team?, coluna: Int, posicao: Int, nome: String, preco: Int, tipo: EntryKind, item: String? = null, municao: String? = null, pente: Int = 0, reserva: Int = 0, categoria: KillCategory = PADRAO, icone: String, precoAlt: Int? = null)`.
    - `time = null` quer dizer os dois times.
    - `precoAlt` só serve para COLETE_CAPACETE: é o preço quando o jogador já tem colete cheio (350).
  - `object DefaultShop { val entries: List<ShopEntry>; fun starterKey(team: Team): String }`. Inicial: `"glock18"` para TR e `"usp_s"` para CT.
  - `fun List<ShopEntry>.forTeam(team: Team): List<ShopEntry>`
  - `object ShopValidation { fun problems(entries: List<ShopEntry>): List<String> }`
  - `data class Owned(primaria: String?, pistola: String?, armor: Int, helmet: Boolean, granadas: Int)`
  - `data class BuyContext(team: Team, money: Int, alive: Boolean, buyOpen: Boolean, inZone: Boolean, owned: Owned)`
  - `enum class BuyFail { MORTO, FORA_DO_TEMPO, FORA_DA_ZONA, TIME_ERRADO, EM_BREVE, JA_TEM, LIMITE, SEM_DINHEIRO }`
  - `sealed interface BuyCheck { data class Ok(price: Int, replaces: String?); data class Fail(reason: BuyFail) }`
  - `object BuyRules { fun price(e: ShopEntry, o: Owned): Int; fun check(e: ShopEntry, ctx: BuyContext, maxGrenades: Int = 1): BuyCheck }`

- [ ] **Step 1: Escrever os testes que falham**
  - `cada time tem 23 entradas em posicoes unicas`: para CT e TR, `forTeam(t)` tem 23 entradas (Equipamento tem 3 posições, as outras colunas têm 5) e os pares `(coluna, posicao)` são distintos.
  - `ak47 do TR`: chave `ak47`, item `pointblank:ak47`, municao `pointblank:ammo762`, pente 30, reserva 90, preço 2700, coluna 4, posição 2.
  - `catalogo padrao sem problemas`: `ShopValidation.problems(DefaultShop.entries).isEmpty()`.
  - `validacao acha posicao repetida`: duas entradas CT em (2,1) geram 1 problema.
  - `capacete sozinho custa 350`: `price(coleteCapacete, Owned(null, null, 100, false, 0)) == 350` e `price(..., armor = 60) == 1000`.
  - `ordem das falhas`: morto vem antes de sem dinheiro; fora do tempo vem antes de fora da zona.
  - `mesma arma duas vezes`: com `owned.primaria == "ak47"`, comprar `ak47` dá `Fail(JA_TEM)`.
  - `trocar primaria`: com `owned.primaria == "galil"`, comprar `ak47` dá `Ok(2700, replaces = "galil")`.
  - `uma granada so`: com `granadas == 1`, dá `Fail(LIMITE)`.
  - `em breve nao compra`: `zeus` dá `Fail(EM_BREVE)`.
  - `time errado`: CT comprando `ak47` dá `Fail(TIME_ERRADO)`.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew test` → FAIL.

- [ ] **Step 3: Implementar `DefaultShop.entries`**

O catálogo segue a tabela da seção 9 da spec. A tabela abaixo diz chave, item, munição, pente e reserva. Os ícones das armas são `pointblank:textures/item/<arma>.icon.png`. Os ícones próprios ficam em `shifustrike:textures/gui/icone/<chave>.png`.

| chave | time | col,pos | item | municao | pente | reserva | categoria |
|---|---|---|---|---|---|---|---|
| colete | ambos | 1,1 | — | — | | | |
| colete_capacete | ambos | 1,2 | — | — | | | precoAlt 350 |
| zeus | ambos | 1,3 | EM_BREVE | | | | |
| usp_s / glock18 | CT / TR | 2,1 | usp45 / glock18 | ammo45acp / ammo9mm | 12 / 20 | 24 / 120 | |
| berettas | ambos | 2,2 | m9 | ammo9mm | 15 | 120 | |
| p250 | ambos | 2,3 | p30l | ammo9mm | 15 | 26 | |
| five_seven / tec9 | CT / TR | 2,4 | tti_viper / m1911a1 | ammo45acp | 21 / 7 | 100 / 90 | |
| deagle | ambos | 2,5 | deserteagle | ammo50ae | 8 | 35 | |
| mag7 / nova | CT / TR | 3,1 | m590 / m870 | ammo12gauge | 7 / 6 | 32 | ESCOPETA |
| xm1014 | ambos | 3,2 | m1014 | ammo12gauge | 8 | 32 | ESCOPETA |
| mp7 / mp5sd | CT / TR | 3,3 | mp7 / mp5 | ammo46 / ammo9mm | 40 / 30 | 120 | SMG |
| p90 | ambos | 3,4 | p90 | ammo57 | 50 | 100 | SMG |
| mp9 / mac10 | CT / TR | 3,5 | tmp / vector | ammo9mm / ammo45acp | 30 / 25 | 120 / 100 | SMG |
| ssg08 | CT (4,1) e TR (4,3) | | c14 | ammo338lapua | 5 | 90 | |
| m4a1s / galil | CT / TR | 4,2 / 4,1 | hk416 / ak74 | ammo556 / ammo545 | 30 | 80 / 90 | |
| m4a4 / ak47 | CT / TR | 4,3 / 4,2 | m4a1 / ak47 | ammo556 / ammo762 | 30 | 90 | |
| famas / sg553 | CT / TR | 4,4 | g36c / aug | ammo556 | 30 | 90 | |
| awp | ambos | 4,5 | l96a1 | ammo338lapua | 5 | 30 | AWP |
| flash | ambos | 5,1 | EM_BREVE | | | | |
| fumaca | ambos | 5,2 | EM_BREVE | | | | |
| he | ambos | 5,3 | grenade (GRANADA) | — | | | |
| incendiaria / molotov | CT / TR | 5,4 | EM_BREVE | | | | |
| decoy | ambos | 5,5 | EM_BREVE | | | | |

Notas:
- Todos os ids da tabela têm prefixo `pointblank:`.
- Nomes e preços vêm da seção 9 da spec, com os nomes em português da foto do CT para os dois times: "Granada de Luz", "Fumaça", "Granada Explosiva", "Incendiária"/"Molotov", "Distração"/"Decoy".
- Validação: chave única por time, `(coluna, posicao)` único por time, preço ≥ 0, `coluna` e `posicao` entre 1 e 5, PRIMARIA/PISTOLA/GRANADA com `item`, PRIMARIA/PISTOLA com `municao` e `pente` > 0.

- [ ] **Step 4: Implementar `BuyRules.check`**

Ordem de checagem:
1. MORTO
2. FORA_DO_TEMPO
3. FORA_DA_ZONA
4. TIME_ERRADO
5. EM_BREVE
6. JA_TEM: mesma chave em `primaria` ou `pistola`; COLETE com `armor >= 100`; COLETE_CAPACETE com `armor >= 100 && helmet`.
7. LIMITE
8. SEM_DINHEIRO

`replaces` é a primária atual (para PRIMARIA) ou a pistola atual (para PISTOLA).

- [ ] **Step 5: Rodar e ver passar** — `./gradlew test` → PASS.
- [ ] **Step 6: Commit** — `git add -A && git commit -m "feat(core): catálogo da loja e regras de compra"`

---

### Task 6: Config, mapa e comandos de montagem

**Files:**
- Create: `src/main/kotlin/com/shifustrike/config/StrikeConfig.kt`, `ShopConfig.kt`, `ConfigManager.kt`
- Create: `src/main/kotlin/com/shifustrike/arena/ArenaData.kt`, `ArenaStorage.kt`
- Create: `src/main/kotlin/com/shifustrike/command/StrikeCommands.kt`, `src/main/kotlin/com/shifustrike/match/Msg.kt`
- Create: `run/selftest.sh`
- Modify: `ShifuStrike.kt` (carregar a config, registrar comandos e carregar o mapa no `SERVER_STARTED`)
- Test: `src/test/kotlin/com/shifustrike/config/ConfigTest.kt`

**Interfaces:**
- Consumes: `Box`, `RoundTimings`, `EconomyRules`, `ShopEntry`, `DefaultShop`, `ShopValidation`.
- Produces:
  - `class StrikeConfig`, no estilo de `RaceConfig` (vars em português + `sanitize()`), com estes campos:
    - tempos: `segundosCongelado=10`, `segundosCompra=30`, `segundosRodada=115`, `segundosBomba=40`, `segundosPlantar=3`, `segundosDesarmar=5`, `segundosFimRodada=5`;
    - partida: `vitoriasParaVencer=5`, `minimoJogadores=2`;
    - dinheiro: `dinheiroInicial=500`, `dinheiroMaximo=16000`, `vitoriaEliminacao=3250`, `vitoriaBomba=3500`, `derrotaBase=1400`, `derrotaPasso=500`, `derrotaMaxima=3400`, `bonusPlantio=800`;
    - kills: `killPadrao=300`, `killSmg=600`, `killEscopeta=900`, `killAwp=100`, `killFaca=1500`;
    - raios e dano: `raioZonaCompra=15.0`, `raioDesarme=2.0`, `raioExplosaoC4=12.0`, `danoMaximoC4=500f`, `proporcaoColete=0.5f`, `bonusColete=0.5f`;
    - `comandosPermitidos=listOf("cs","msg","tell","w","r")`.
  - Métodos de `StrikeConfig`: `fun timings(): RoundTimings` e `fun economy(): EconomyRules`.
  - `object ShopConfig { fun load(dir: Path): List<ShopEntry>; fun save(dir: Path, e: List<ShopEntry>) }`. Se `loja.json` não existir, grava o `DefaultShop`. Se a validação falhar, avisa no log e usa o `DefaultShop`.
  - `object ConfigManager { var config: StrikeConfig; var shop: List<ShopEntry>; fun load(); fun activeShop(): List<ShopEntry> }`. `activeShop` tira as entradas cujo `item` não existe em `BuiltInRegistries.ITEM` e avisa no log.
  - `data class Ponto(x: Double, y: Double, z: Double, yaw: Float, pitch: Float)`
  - `class ArenaData { var dimensao: String; spawnsCt, spawnsTr: MutableList<Ponto>; var bombaA, bombaB, area: Box?; var saida: Ponto?; fun problemas(): List<String> }`
  - `object ArenaStorage { fun load(server); fun save(); val arena: ArenaData }`, gravando em `<mundo>/shifustrike/mapa.json`.
  - `object Msg`, com helpers `info(player, texto)`, `erro(...)` e `broadcast(players, texto)`. Todos usam o prefixo `"[CS] "` dourado.

- [ ] **Step 1: Escrever os testes que falham**
  - `config padrao vira tempos em ticks`: `StrikeConfig().timings() == RoundTimings(200, 600, 2300, 800, 100)`.
  - `sanitize corrige valores ruins`: `vitoriasParaVencer = 0` vira 1 e `segundosCompra < segundosCongelado` vira `segundosCongelado`.
  - `loja vai e volta do json igual`: `ShopConfig.save` seguido de `load` em `@TempDir` devolve uma lista igual ao `DefaultShop.entries`.
  - `loja invalida cai no padrao`: um `loja.json` com posição repetida carrega o `DefaultShop.entries`.
  - `mapa incompleto lista o que falta`: `ArenaData().problemas()` contém "spawn CT", "spawn TR", "bomba A", "bomba B", "área" e "saída".
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew test` → FAIL.

- [ ] **Step 3: Implementar config, storage e comandos**

Comandos de staff (`hasPermission(2)`), sob `/cs`:
- `mapa spawn ct|tr` adiciona a posição e a rotação atuais.
- `mapa bomba a|b <de> <ate>` e `mapa area <de> <ate>` usam `BlockPosArgument`.
- `mapa saida`, `mapa ver` e `mapa limpar (spawns ct|spawns tr|bomba a|bomba b|tudo)`.
- `diagnostico` lista os problemas do mapa, as entradas tiradas da loja e se `pointblank:ak47` existe.
- `recarregar` recarrega config e loja; recusa se houver partida.

Todo comando `mapa` grava `dimensao` com a dimensão atual. Se o mapa já tiver pontos em outra dimensão, ele recusa com "use /cs mapa limpar tudo".

- [ ] **Step 4: Rodar e ver passar** — `./gradlew test` → PASS.

- [ ] **Step 5: Autoteste no console**

Escreva `run/selftest.sh` no modelo de `D:\Mod de corrida\run\selftest.sh`:
- `cs mapa spawn` não serve no console, então use `execute positioned 0 -60 0 rotated 90 0 run cs mapa spawn ct`, e o mesmo para tr;
- `cs mapa bomba a 10 -61 10 14 -58 14`, depois `bomba b`, `area -30 -64 -30 30 -40 30` e `saida`, todos com `execute positioned ... run`;
- `cs mapa ver`, `cs diagnostico` e `stop`.

Run: `bash run/selftest.sh run/logs/latest.log | ./gradlew runServer`
Expected: o log mostra cada ponto gravado, e `cs diagnostico` termina com "mapa completo" e "pointblank:ak47 ok". O arquivo `run/world/shifustrike/mapa.json` existe.

- [ ] **Step 6: Commit** — `git add -A && git commit -m "feat: config, loja em json, mapa e comandos de montagem"`

---

### Task 7: Itens do evento e ponte com o PointBlank

**Files:**
- Create: `src/main/kotlin/com/shifustrike/pointblank/PointBlankItems.kt`, `EventItems.kt`, `HeadshotFlag.kt`
- Create: `src/main/kotlin/com/shifustrike/registry/ModItems.kt`, `src/main/kotlin/com/shifustrike/item/C4Item.kt` (por enquanto só a classe com use duration, sem lógica), `DefuseKitItem.kt` (idem)
- Create: `tools/gerar_recursos.js`, `src/main/resources/assets/shifustrike/{lang/pt_br.json, lang/en_us.json, models/item/*.json, textures/item/*.png, textures/gui/icone/*.png, icon.png}`
- Modify: `StrikeCommands.kt` (acrescentar `/cs teste arma <chave>`, de staff)
- Test: `src/main/kotlin/com/shifustrike/gametest/ItemsGameTest.kt` (e registrar no `fabric-gametest`)

**Interfaces:**
- Consumes: `ShopEntry`.
- Produces:
  - `ModItems.FACA`: `SwordItem` com dano de ataque total 8 (40 do CS) e velocidade padrão de espada. `ModItems.C4` (`C4Item`) e `ModItems.KIT` (`DefuseKitItem`) têm stack 1.
  - `object EventItems`:
    - `fun mark(stack: ItemStack, purchaseId: UUID? = null): ItemStack` grava `CUSTOM_DATA {shifustrike:{evento:1b, compra:"<uuid>"}}`, juntando com o que já existir;
    - `fun isEvent(stack): Boolean` é true para itens marcados ou para FACA/C4/KIT;
    - `fun purchaseId(stack): UUID?`;
    - `fun purge(player: ServerPlayer): Int`.
  - `object PointBlankItems`:
    - `fun exists(id: String): Boolean`
    - `fun loadedGun(player: ServerPlayer, itemId: String, rounds: Int): ItemStack?`
    - `fun ammo(itemId: String, rounds: Int): List<ItemStack>` (divide pelo tamanho máximo do stack)
    - `fun grenade(): ItemStack?`
  - `object HeadshotFlag { fun set(v: Boolean); fun consume(): Boolean; fun clear() }`, num `ThreadLocal`. O mixin que preenche fica na Task 14.

- [ ] **Step 1: Escrever o GameTest que falha**

`ItemsGameTest` usa `h.makeMockServerPlayerInLevel()`:
- `ak47_vem_carregada`: `loadedGun(p, "pointblank:ak47", 30)` não é nulo. O componente `pointblank:custom_tag` (achado pelo id em `BuiltInRegistries.DATA_COMPONENT_TYPE`) tem `getInt("ammo") == 30` e `mid != 0L`, e o componente `pointblank:ts` existe.
- `municao_divide_em_stacks`: `ammo("pointblank:ammo762", 90)` soma 90 itens.
- `marca_de_evento`: `isEvent(mark(ItemStack(Items.STONE)))` é true, `isEvent(ItemStack(Items.STONE))` é false e `purchaseId` volta o uuid gravado.
- `purge_tira_so_itens_do_evento`: um jogador com 1 pedra comum + 1 faca + 1 ak47 marcada fica só com a pedra, e `purge` devolve 2.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar `loadedGun`** (achado da pesquisa no PointBlank 2.2.0)
  1. Crie o `ItemStack` pelo id.
  2. Chame `stack.item.inventoryTick(stack, player.serverLevel(), player, 0, false)`. Isso faz o próprio PointBlank gerar `mid`/`lid`, os acessórios padrão (supressor da USP, luneta da L96A1 e da C14) e o id do GeckoLib. **Não escreva `mid`/`lid` à mão**, senão os acessórios somem.
  3. Copie o `CompoundTag` de `pointblank:custom_tag`, faça `putInt("ammo", rounds)`, grave de volta e grave `pointblank:ts = System.currentTimeMillis()`. Sem o `ts`, o PointBlank dá NullPointerException.
  4. Use casts `@Suppress("UNCHECKED_CAST")` para `DataComponentType<CompoundTag>` e `DataComponentType<Long>`.

Se o id não existir, devolva `null` e avise no log uma vez por id.

- [ ] **Step 4: Gerar recursos próprios**

`tools/gerar_recursos.js` desenha os PNGs sem dependências, no mesmo estilo do da corrida:
- texturas de item 16×16 para faca, c4 (bloco com fios e visor vermelho) e kit (alicate);
- ícones de GUI 64×64 **brancos com alfa**, que são tingidos na tela de compra: colete, colete_capacete, zeus, flash, fumaca, incendiaria, molotov, decoy;
- `icon.png`.

Modelos `item/generated`. Lang: `item.shifustrike.faca = "Faca"`, `c4 = "C4"`, `kit_desarme = "Kit de desarme"`, `key.shifustrike.comprar = "Abrir loja (CS)"`, `key.categories.shifustrike = "CS"`.

Run: `node tools/gerar_recursos.js`
Expected: os arquivos aparecem em `src/main/resources/assets/shifustrike/`.

- [ ] **Step 5: Rodar e ver passar** — `./gradlew runGametest` → `All N required tests passed`.

- [ ] **Step 6: Conferir no jogo**

Run: `./gradlew runClient`. Abra um mundo em modo aventura e rode `/cs teste arma ak47` (o comando dá a arma carregada e a reserva).
Expected:
- a AK atira 30 vezes, recarrega gastando a munição da reserva e funciona em modo aventura;
- a faca aparece com textura e nome.

Anote no commit se algo falhou em modo aventura. Esse é um risco aberto da pesquisa.

- [ ] **Step 7: Commit** — `git add -A && git commit -m "feat: itens do evento e armas do PointBlank carregadas"`

---

### Task 8: Inventário guardado em disco

**Files:**
- Create: `src/main/kotlin/com/shifustrike/data/PlayerSnapshot.kt`, `SnapshotStorage.kt`
- Modify: `ShifuStrike.kt` (`SnapshotStorage.init(server)` no `SERVER_STARTED`)
- Test: `src/main/kotlin/com/shifustrike/gametest/SnapshotGameTest.kt`

**Interfaces:**
- Consumes: `Ponto`, `EventItems.purge`.
- Produces:
  - `class PlayerSnapshot(val tag: CompoundTag) { companion object { fun capture(p: ServerPlayer): PlayerSnapshot }; fun restore(p: ServerPlayer, exit: Ponto?, exitLevel: ServerLevel?) }`
  - `object SnapshotStorage { fun init(server: MinecraftServer); fun save(id: UUID, s: PlayerSnapshot); fun load(id: UUID): PlayerSnapshot?; fun delete(id: UUID); fun has(id: UUID): Boolean; fun restoreOnJoin(p: ServerPlayer, exit: Ponto?) }`, gravando em `<mundo>/shifustrike/salvos/<uuid>.dat` com `NbtIo.writeCompressed` num `.tmp` e depois `ATOMIC_MOVE`.
  - `const val FREEZE_MODIFIER_ID` (`shifustrike:congelado`), usado na Task 10. O `restore` sempre tira esse modificador de velocidade e de pulo.

- [ ] **Step 1: Escrever os GameTests que falham**
  - `guarda_e_devolve_tudo`: o jogador tem 3 diamantes no slot 7, capacete de ferro, modo SURVIVAL, vida 13, fome 9 e efeito de velocidade.
    1. Rode `capture` e `save`.
    2. Limpe o inventário, coloque em ADVENTURE, vida 20 e sem efeitos.
    3. Rode `load` e `restore(p, null, null)`.
    Expected: tudo volta como estava, e o arquivo é apagado depois do `delete`.
  - `restaura_depois_de_reinicio` (Review Focus 1):
    1. `capture` e `save`, depois dê ao jogador uma faca e uma ak47 marcada.
    2. Simule o reinício: `SnapshotStorage.init(server)` de novo.
    3. Chame `SnapshotStorage.restoreOnJoin(p, exit)`, a mesma função que o `MatchManager.onJoin` usa a partir da Task 9.
    Expected: o inventário fica igual ao original, sem faca nem ak47, e o arquivo some.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**

O snapshot guarda:
- `Inventory.save(ListTag())` (todos os 41 slots) e o slot selecionado;
- o `GameType`;
- vida, fome e saturação;
- efeitos (`MobEffectInstance.save()`);
- dimensão, posição e rotação.

O `restore` faz, nesta ordem:
1. limpa o inventário e carrega o salvo;
2. tira os modificadores de congelamento e devolve a câmera ao próprio jogador (`setCamera(p)`);
3. aplica modo de jogo, vida, fome e efeitos;
4. teleporta para `exit` (se não for nulo) ou para a posição salva.

`restoreOnJoin(p, exit)`: se existe snapshot, faz o restore com saída e apaga o arquivo. Se não existe, roda `EventItems.purge(p)`.

- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: inventário dos jogadores guardado em disco e devolvido"`

---

### Task 9: Ciclo da partida (criar, entrar, iniciar, cancelar)

**Files:**
- Create: `src/main/kotlin/com/shifustrike/match/Participant.kt`, `Match.kt`, `MatchManager.kt`
- Create: `src/main/kotlin/com/shifustrike/guard/CobblemonGuard.kt`
- Modify: `StrikeCommands.kt` (`criar`, `entrar`, `sair`, `iniciar`, `cancelar`, `status`) e `ShifuStrike.kt` (tick, JOIN, DISCONNECT, `SERVER_STOPPING`)
- Test: `src/main/kotlin/com/shifustrike/gametest/MatchGameTest.kt`

**Interfaces:**
- Consumes: `ArenaData`, `StrikeConfig`, `TeamBalancer`, `PlayerSnapshot`, `SnapshotStorage`, `EventItems`, `Msg`.
- Produces:
  - `class Participant(val id: UUID, var name: String, var team: Team)`, com `money`, `alive`, `armor`, `helmet`, `kills`, `deaths`, `connected` e `purchases: MutableList<Purchase>`.
  - `data class Purchase(chave: String, price: Int, stackId: UUID?)`.
  - `class Match(val server: MinecraftServer, val arena: ArenaData, val cfg: StrikeConfig, val shop: List<ShopEntry>, val random: Random)`, com:
    - `state: State` (`LOBBY`, `RUNNING`, `ENDED`), `lobby: LinkedHashSet<UUID>`, `players: Map<UUID, Participant>`;
    - `score: MutableMap<Team, Int>`, `round: Int`, `clock: RoundClock`;
    - `startedTeamSizes: Map<Team, Int>`, que é o tamanho de cada time no início da partida e é usado na regra de desistência;
    - `fun begin(): Result<Unit>`, `fun startRound()` (nesta tarefa só teleporta e deixa em modo aventura), `fun end(winner: Team?)`.
  - `object MatchManager`, com:
    - `var match: Match?`;
    - `fun create(server): String?`, `fun join(p): String?`, `fun leave(p): String?`, `fun start(server): String?`, `fun cancel(server): String?`. Todos devolvem a mensagem de erro, ou null se deu certo.
    - `fun participant(p: ServerPlayer): Participant?` (só com a partida em RUNNING) e `fun isPlaying(p): Boolean`;
    - `fun tick(server)`, `fun onJoin(p)`, `fun onDisconnect(p)`, `fun shutdown(server)`;
    - `fun createForTest(server, arena, cfg): Match`, a porta dos GameTests, que não lê `ArenaStorage`.

- [ ] **Step 1: Escrever os GameTests que falham**

Monte um `ArenaData` relativo ao `h.absolutePos` (spawns CT e TR a 6 blocos um do outro, sites pequenos, área 16×16 e saída fora da área) e use `createForTest`.
- `inicia_divide_e_teleporta`: com 2 jogadores falsos em SURVIVAL e um diamante cada, depois de `join` + `start`:
  - um jogador é CT e o outro TR;
  - os dois estão em ADVENTURE, dentro de 1 bloco do spawn do seu time;
  - o inventário está sem diamante;
  - existe um arquivo de snapshot para cada um;
  - cada um tem `money == 500`.
- `cancelar_devolve_tudo`: depois de `cancel`, os dois estão em SURVIVAL, com o diamante, na saída, sem arquivo de snapshot, e `MatchManager.match == null`.
- `minimo_de_jogadores`: com `minimoJogadores = 2` e 1 jogador, `start` devolve uma mensagem contendo "mínimo".
- `nao_entra_depois_de_iniciar`: `join` com a partida em RUNNING devolve um erro.
- `sair_no_meio_devolve`: `leave` em RUNNING devolve o inventário e tira o jogador de `players`.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**
  - O `start` recusa se:
    - `arena.problemas()` não estiver vazio;
    - houver menos de `minimoJogadores` online no lobby;
    - algum jogador estiver em batalha do Cobblemon (`player.isInBattle()`; esse jogador sai do lobby com aviso).
  - Para cada jogador: `capture` → `save` → limpar inventário → recolher Pokémon (`player.party().forEach { if (it.entity != null) it.recall() }`).
  - Depois: `TeamBalancer.split`, `money = cfg.dinheiroInicial` e `startRound()`.
  - `CobblemonGuard.register()` cancela, para quem está em `MatchManager.isPlaying`:
    - `CobblemonEvents.POKEMON_SENT_PRE` (dono via `event.pokemon.getOwnerPlayer()`);
    - `BATTLE_STARTED_PRE` (`event.battle.players.any { isPlaying(it) }`);
    - `RIDE_EVENT_PRE` (`event.player`);
    - `SHOULDER_MOUNT`.
    O modelo é `D:\Mod de corrida\src\main\kotlin\com\shifurace\race\RaceEvents.kt:64-98`. O inventário já está vazio, então não sobra Poké Bola para jogar.
  - `end(winner)`: restaura cada participante online na saída. Quem estiver offline é restaurado no `onJoin`.
  - `onJoin(p)`:
    - se o jogador é participante de uma partida em RUNNING, vira `connected = true` e espectador (a Task 11 completa isso);
    - senão, `SnapshotStorage.restoreOnJoin(p, arena.saida)`.
  - `shutdown` (`SERVER_STOPPING`): `end(null)`.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: criar, entrar, iniciar e cancelar a partida"`

---

### Task 10: Rodadas, loadout e economia aplicada

**Files:**
- Create: `src/main/kotlin/com/shifustrike/match/Loadouts.kt`
- Modify: `Match.kt` (`startRound` completo, `tick`, `endRound`, dinheiro, sequência de derrotas, fim da partida)
- Test: `src/main/kotlin/com/shifustrike/gametest/RoundGameTest.kt`

**Interfaces:**
- Consumes: `RoundClock`, `RoundRules`, `Economy`, `PointBlankItems`, `EventItems`, `ModItems`, `DefaultShop.starterKey`.
- Produces:
  - `object Loadouts`, com:
    - constantes de slot: `SLOT_PRIMARIA=0`, `SLOT_PISTOLA=1`, `SLOT_FACA=2`, `SLOT_GRANADA=3`, `SLOT_EXTRA=4` (C4 no TR, kit no CT);
    - `fun giveWeapon(p, e: ShopEntry, purchaseId: UUID?): Boolean` (arma no slot certo + reserva; marca tudo);
    - `fun giveRoundBasics(p, team, shop, fresh: Boolean)`. Com `fresh`, limpa e dá faca + pistola inicial. Sempre garante faca, e kit para o CT.
    - `fun slotOf(stack, shop): Int?`, `fun normalize(p, shop)`, `fun bestWeapon(p): ItemStack?`, `fun ownedOf(p, part, shop): Owned`.
  - `Match.endRound(o: RoundOutcome)`, `Match.lossStreak: MutableMap<Team, Int>`, `Match.bombWasPlanted: Boolean` (preenchido na Task 12).

- [ ] **Step 1: Escrever os GameTests que falham**

Use `cfg` de teste com `segundosCongelado=1, segundosCompra=2, segundosRodada=3, segundosFimRodada=1` e 2 jogadores.
- `comeca_com_faca_e_pistola`: depois do `start`, o TR tem `glock18` no slot 1, a faca no slot 2 e munição `ammo9mm` somando 120. O CT tem `usp45`, a faca e o kit no slot 4.
- `congela_no_inicio`: durante o FREEZE, o atributo `MOVEMENT_SPEED` do jogador é 0. Depois de 1s, volta ao normal.
- `tempo_acaba_ct_ganha`: depois de `1 + 3` segundos mais 1 tick, `score[CT] == 1`. O CT tem `500 + 3250` e o TR tem `500 + 1400`.
- `sobrevivente_mantem_arma`: dê `ak47` ao TR com `giveWeapon`. Depois do fim da rodada e do início da próxima, o TR ainda tem a `ak47` no slot 0.
- `cinco_vitorias_acaba`: com `vitoriasParaVencer=2`, depois de duas rodadas por tempo, `match.state == ENDED` e todos foram restaurados.
- `sem_regeneracao`: um jogador vivo com vida 10 continua com vida 10 depois de 200 ticks.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**
  - `startRound()`:
    1. desistência: se algum time teve ≥ 1 jogador no início da partida e agora tem 0 conectados, `end(outro)` com FORFEIT, e para aqui;
    2. limpa os itens de evento no chão da área;
    3. `clock.startRound()`;
    4. para cada participante conectado: teleporta para o spawn do time (embaralhado e em rodízio), ADVENTURE, câmera nele mesmo, vida cheia, limpa efeitos e fogo, modificadores de congelamento e `giveRoundBasics(fresh = !alive)`. Quem estava morto zera colete e capacete;
    5. `alive = true` e `purchases.clear()`;
    6. guarda os vivos de cada time como `ctStarted`/`trStarted`.
  - `tick()`:
    - `clock.tick()`;
    - ao sair do FREEZE, tira os modificadores;
    - a cada tick, para os vivos: `foodData.setFoodLevel(17)` e `setSaturation(0f)`. Com fome abaixo de 18 o Minecraft não regenera, e com mais de 6 o jogador ainda corre;
    - a cada 10 ticks, `Loadouts.normalize`;
    - se `phase` for LIVE ou PLANTED, monta o `RoundSnapshot` (vivos conectados por time) e chama `RoundRules.evaluate`. Se houver resultado, `endRound`;
    - em ROUND_END, quando `roundEndDone`: se `matchWinner` não for nulo, `end(vencedor)`; senão, `startRound()`.
  - `endRound(o)`:
    1. soma `score`;
    2. a sequência de derrotas do perdedor sobe 1 e a do vencedor zera;
    3. aplica `Economy.roundPayout` a cada participante;
    4. `clock.endRound()`;
    5. mostra o título "Antiterroristas venceram" ou "Terroristas venceram" para todos.
  - Modificadores de congelamento: `AttributeModifier(FREEZE_MODIFIER_ID, -1.0, ADD_MULTIPLIED_TOTAL)` em `MOVEMENT_SPEED` e `JUMP_STRENGTH`.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: rodadas com congelamento, loadout e economia"`

---

### Task 11: Morte, kills, espectador e desconexão

**Files:**
- Create: `src/main/kotlin/com/shifustrike/match/Deaths.kt`, `Spectators.kt`
- Modify: `Match.kt` (chamar `Spectators.tick`), `MatchManager.kt` (`onDisconnect`, `onJoin` de participante), `ShifuStrike.kt` (`ServerLivingEntityEvents.ALLOW_DEATH`)
- Test: `src/main/kotlin/com/shifustrike/gametest/DeathGameTest.kt`

**Interfaces:**
- Consumes: `Economy.killReward`, `Loadouts.bestWeapon`, `HeadshotFlag`, `ShopEntry.categoria`.
- Produces:
  - `object Deaths`, com:
    - `fun onAllowDeath(victim: ServerPlayer, source: DamageSource): Boolean`, que devolve false para participantes e cancela a morte do Minecraft;
    - `fun killCategory(killer: ServerPlayer, source: DamageSource, shop): KillCategory`;
    - `fun dropOnDeath(p)`.
  - `Participant.lastHitHeadshot: Boolean`, preenchido pela Task 14.
  - Callback de feed de kill: `Match.onKill: (KillInfo) -> Unit`, com `data class KillInfo(killer: String?, killerTeam: Team?, victim: String, victimTeam: Team, weapon: String, headshot: Boolean)`. A Task 16 liga isso à rede.
  - `object Spectators { fun makeSpectator(m: Match, p: ServerPlayer); fun tick(m: Match); fun cycle(m: Match, p: ServerPlayer) }`

- [ ] **Step 1: Escrever os GameTests que falham**

Todos os testes desta tarefa que causam dano usam `cfg` de teste com `segundosCongelado=1` e só batem depois de `h.runAfterDelay(21)`. Assim continuam válidos quando a Task 14 bloquear dano durante o congelamento.
  - `kill_de_rifle_paga_300`: o CT segura `ak47` e causa `p.hurt(ct.damageSources().playerAttack(ct), 1000f)` no TR.
    - TR: `alive == false`, modo SPECTATOR, `deaths == 1`, câmera em um colega vivo ou em qualquer vivo.
    - CT: `kills == 1` e `money == 800`.
    - A rodada termina com CT vencendo por eliminação.
  - `kill_de_faca_paga_1500` e `kill_de_awp_paga_100`.
  - `morto_larga_a_melhor_arma`: o TR com `ak47` e `glock18` morre. No chão fica 1 `ItemEntity` com a ak47 marcada e nenhuma glock.
  - `queda_nao_paga_ninguem`: com `damageSources().fall()`, ninguém ganha dinheiro.
  - `desconectar_conta_como_morte`: `MatchManager.onDisconnect(tr)` deixa `alive == false` e `connected == false`. Ao voltar com `onJoin`, fica espectador e `alive == false` até a próxima rodada.
  - `cancelar_com_jogador_morto_restaura` (Review Focus 5): com o TR morto (espectador) e `cancel`, o TR fica em SURVIVAL, com a câmera nele mesmo e a velocidade normal.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**

`onAllowDeath` para participante com a partida em RUNNING:
1. `dropOnDeath`: larga a melhor arma (slot 0, ou slot 1 se o 0 estiver vazio) e a C4 se tiver, como `ItemEntity` sem atraso de pegar; limpa o resto do inventário.
2. Vida cheia, `alive = false`, `deaths++`, colete 0 e sem capacete.
3. Se o atacante (`source.entity`) for participante do outro time: `kills++`, dinheiro de `killCategory` e `onKill`.
4. `makeSpectator`.
5. Devolve false.

`killCategory`:
- se `source.directEntity != killer` (granada), é PADRAO;
- senão, olha o item na mão: FACA dá FACA; uma entrada da loja com aquele `item` dá a `categoria` dela; se não achar, PADRAO.

Nome da arma no feed:
- o `nome` da entrada da loja, "Faca" ou "Granada Explosiva";
- vazio se não houver atacante.

`Spectators`:
- o alvo é um colega vivo; se não houver, qualquer vivo;
- `tick` refaz `setCamera` se a câmera não estiver num vivo válido;
- `cycle` passa para o próximo da lista.

`onDisconnect` de quem estava vivo: `dropOnDeath`, `alive = false` e `connected = false`. O inventário com itens do evento fica com ele; o snapshot em disco resolve na volta.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: mortes, recompensas por kill, espectador e desconexão"`

---

### Task 12: Bomba C4 (plantar, explodir, desarmar)

**Files:**
- Create: `src/main/kotlin/com/shifustrike/match/Bomb.kt`, `src/main/kotlin/com/shifustrike/item/UseHints.kt`
- Modify: `item/C4Item.kt`, `item/DefuseKitItem.kt`, `Match.kt` (sortear a C4 no `startRound`, `bomb.tick`, ligar `BombState` ao `RoundSnapshot`, `bombWasPlanted`)
- Test: `src/main/kotlin/com/shifustrike/gametest/BombGameTest.kt`

**Interfaces:**
- Consumes: `RoundClock.plantBomb/bombExploded`, `Box`, `StrikeConfig` (`segundosPlantar`, `segundosDesarmar`, `raioDesarme`, `raioExplosaoC4`, `danoMaximoC4`).
- Produces:
  - `class Bomb(val match: Match)`, com:
    - estado: `var state: BombState`, `var plantedPos: Vec3?`, `var site: Char?`;
    - funções: `fun giveToRandomTr()`, `fun canPlant(p): Boolean`, `fun plant(p)`, `fun canDefuse(p): Boolean`, `fun defuse(p)`, `fun tick()`, `fun reset()`.
  - `object UseHints { @Volatile var sites: List<Box>; @Volatile var bombPos: Vec3?; @Volatile var team: Team? }`. É código comum, preenchido pelo cliente na Task 16. Serve para o `use()` do lado do cliente não começar a barra quando o servidor vai recusar.
  - `C4Item`: `getUseDuration = segundosPlantar * 20`, `UseAnim.BOW`.
    - `use()` começa a usar só se `canPlant` (no cliente, via `UseHints`).
    - `onUseTick` para se o jogador andar mais de 0,2 bloco na horizontal.
    - `finishUsingItem` chama `plant`.
  - `DefuseKitItem`: igual, com `segundosDesarmar` e `canDefuse`/`defuse`.

- [ ] **Step 1: Escrever os GameTests que falham**
  - `c4_vai_para_um_tr`: depois do `start`, exatamente 1 TR tem `ModItems.C4` no slot 4.
  - `plantar_fora_do_site_nao_deixa`: `canPlant` é false fora da caixa e true dentro, no chão, em LIVE.
  - `plantada_explode_e_tr_ganha`: depois de `plant(tr)` e `segundosBomba` (use 2 no teste), `score[TR] == 1`, a razão é `BOMB_EXPLODED` e o CT ao lado da bomba morreu.
  - `desarmada_ct_ganha`: depois de `plant(tr)` e `defuse(ct)`, `score[CT] == 1` com `BOMB_DEFUSED`, e o CT recebe 3500.
  - `tr_mortos_com_bomba_continua`: com a bomba plantada e o TR morto, a rodada não acaba antes da explosão.
  - `tr_perde_depois_de_plantar_ganha_bonus`: depois de plantar e desarmar, o TR recebe `1400 + 800`.
  - `ct_nao_pega_c4`: a C4 no chão encostando num CT continua no chão (o mixin é da Task 15). **Deixe este teste escrito, com `@GameTest(required = false)`, até a Task 15**.
  - `c4_cai_quando_portador_desconecta` (Review Focus 2): `onDisconnect` do portador deixa 1 `ItemEntity` de C4 no chão e a rodada continua.
  - `c4_perdida_volta_para_um_tr`: se a C4 sumir (o `ItemEntity` é descartado) e ninguém estiver com ela, em até 20 ticks ela volta para um TR vivo.

- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**

`plant`:
1. tira a C4 do inventário;
2. cria um `Display.ItemDisplay` com a C4, com tag `shifustrike_bomba`, na posição do bloco aos pés;
3. guarda `plantedPos` e `site`;
4. `state = PLANTED`, `clock.plantBomb()`, `bombWasPlanted = true`;
5. mostra o título "Bomba plantada" para todos.

`tick` com a bomba plantada:
- toca `SoundEvents.NOTE_BLOCK_BIT` na bomba. O intervalo cai linearmente de 20 ticks para 2 ticks conforme o tempo acaba;
- quando `clock.bombExploded`, chama `explode`.

`explode`:
- partículas `EXPLOSION_EMITTER` e som de explosão;
- para cada participante vivo a até `raioExplosaoC4`, dano `danoMaximoC4 * (1 - d / raio) / 5`, com `damageSources().explosion(null, null)`;
- `state = EXPLODED`.

`defuse`: `state = DEFUSED` e remove o display.

`reset`: remove o display e qualquer C4 no chão, e zera o estado.

Fora da bomba plantada, a cada 20 ticks: se nenhum TR tem a C4 e não há `ItemEntity` de C4 na área, chama `giveToRandomTr`.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS (o teste `ct_nao_pega_c4` é opcional por enquanto).
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: bomba C4 com plantio, explosão e desarme"`

---

### Task 13: Compra e reembolso no servidor

**Files:**
- Create: `src/main/kotlin/com/shifustrike/match/Purchases.kt`
- Test: `src/main/kotlin/com/shifustrike/gametest/PurchaseGameTest.kt`

**Interfaces:**
- Consumes: `BuyRules`, `Loadouts.giveWeapon`, `Loadouts.ownedOf`, `Economy`, `EventItems.purchaseId`, `ArenaData` (spawns).
- Produces:
  - `object Purchases`, com:
    - `fun buy(m: Match, p: ServerPlayer, chave: String): BuyCheck`
    - `fun refund(m: Match, p: ServerPlayer, chave: String): Boolean`
    - `fun inBuyZone(m: Match, p: ServerPlayer): Boolean`. É true se o jogador estiver a até `raioZonaCompra` de algum spawn do seu time.

- [ ] **Step 1: Escrever os GameTests que falham**
  - `compra_ak_desconta_e_entrega`: com o TR em FREEZE no spawn e `money = 3000`, `buy(m, tr, "ak47")` é Ok. O TR tem a ak47 carregada (ammo 30) no slot 0, 90 de `ammo762` e `money == 300`.
  - `compra_dupla_no_mesmo_tick` (Review Focus 4): duas chamadas `buy(...,"ak47")` seguidas com `money = 3000`. A segunda é `Fail(JA_TEM)`, `money == 300` e só existe 1 ak47.
  - `trocar_primaria_larga_a_antiga`: com galil e depois ak47, a galil fica no chão como `ItemEntity`.
  - `colete_vira_estado`: comprar `colete` deixa `armor == 100`. Comprar `colete_capacete` em seguida custa 350 e deixa `helmet == true`.
  - `fora_do_tempo_e_da_zona`: depois de `segundosCompra`, dá `Fail(FORA_DO_TEMPO)`. Com `raioZonaCompra=3` no `cfg` de teste e o jogador a 5 blocos do spawn (ainda dentro da área), dá `Fail(FORA_DA_ZONA)`.
  - `reembolso_devolve`: compre a ak47 e chame `refund(...,"ak47")`. O dinheiro volta, a ak47 some e `purchases` fica vazio. Depois de `segundosCompra`, o `refund` devolve false.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**

`buy`:
1. Procura a entrada na `shop` do time do jogador.
2. Monta o `BuyContext` (o `buyOpen` vem do relógio) e chama `BuyRules.check`.
3. Se for Ok:
   - desconta o dinheiro;
   - se houver `replaces`, larga a arma antiga do slot no chão;
   - entrega a arma com `purchaseId` novo, ou aplica colete/capacete no `Participant`, ou entrega a granada no slot 3;
   - grava `Purchase(chave, preco, purchaseId)`.
4. Avisa o jogador pelo `Msg` em caso de falha, com um texto por `BuyFail`.

`refund`:
- só com `buyOpen`;
- precisa de uma `Purchase` daquela chave;
- arma: acha o stack pelo `purchaseId` e remove, junto com a munição de reserva do mesmo tipo até a quantidade entregue;
- colete: só se `armor == 100`;
- granada: só se o jogador ainda tiver a granada.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: compra e reembolso validados no servidor"`

---

### Task 14: Dano, fogo amigo, colete e headshot

**Files:**
- Create: `src/main/kotlin/com/shifustrike/guard/DamageGuard.kt`
- Create: `src/main/java/com/shifustrike/mixin/LivingEntityMixin.java`, `HurtingItemMixin.java`
- Modify: `shifustrike.mixins.json`, `ShifuStrike.kt` (`ServerLivingEntityEvents.ALLOW_DAMAGE` e `HeadshotFlag.clear()` no início de cada tick)
- Test: `src/main/kotlin/com/shifustrike/gametest/DamageGameTest.kt`

**Interfaces:**
- Consumes: `MatchManager.participant`, `ArmorMath`, `HeadshotFlag`, `RoundClock.frozen`.
- Produces:
  - `object DamageGuard`, com:
    - `fun allowDamage(victim: LivingEntity, source: DamageSource): Boolean`
    - `fun afterArmor(victim: LivingEntity, source: DamageSource, amount: Float): Float`. Chamada pelo mixin, aplica o colete do CS e devolve o dano em vida do Minecraft.
  - `LivingEntityMixin`: `@Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)` → `cir.setReturnValue(DamageGuard.afterArmor(...))`.
  - `HurtingItemMixin`:
    - declaração: `@Pseudo @Mixin(targets = "<pacote>.HurtingItem")`, com `@WrapOperation(method = "hurtEntity", at = @At(value = "INVOKE", target = "L<pacote>/HitScan;isHeadshot", remap = false), remap = false, require = 0)`;
    - o handler chama o original, grava `HeadshotFlag.set(resultado)` e devolve o resultado;
    - o pacote certo está nos fontes descompilados em `C:\Users\bigao\AppData\Local\Temp\claude\d--Mod-Cs-pra-minecraft\d06cb4a5-9de5-4761-a533-3f5614940d77\scratchpad\pbresearch\src\vicmatskiv\pointblank\`. Se a pasta não existir mais, descompile `libs/pointblank-*.jar` com Vineflower (está no cache do Gradle);
    - seletores **só por nome, sem descritor**, assim não dependem de remapeamento;
    - `require = 0`: se não aplicar, headshot é sempre false e nada quebra.

- [ ] **Step 1: Escrever os GameTests que falham**
  - `sem_fogo_amigo`: dois CT. `hurt(playerAttack(ct1), 5f)` em `ct2` não muda a vida.
  - `de_fora_nao_machuca`: um jogador que não está na partida atirando num participante não causa dano, e vice-versa.
  - `congelado_nao_toma_dano`: em FREEZE, CT contra TR não causa dano.
  - `colete_corta_metade`: TR com `armor=100` toma `hurt(playerAttack(ct), 10f)`, que são 50 pontos do CS. A vida cai 5 (25 do CS) e `armor == 88` (perdeu 12,5, arredondado).
  - `headshot_sem_capacete_ignora_colete`: com `HeadshotFlag.set(true)` antes do `hurt`, a vida cai 10 e o colete fica igual.
  - `queda_ignora_colete`: com `fall()`, o dano é cheio e o colete fica igual.
  - `granada_propria_machuca`: o atacante é a própria vítima (dano de explosão com dono = vítima) e o dano passa.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew runGametest` → FAIL.

- [ ] **Step 3: Implementar**

`allowDamage`, na ordem:
1. sem partida em RUNNING → true;
2. vítima e atacante fora da partida → true;
3. só um dos dois na partida → false;
4. vítima participante sem atacante jogador → true;
5. algum dos dois morto → false;
6. atacante igual à vítima → true;
7. mesmo time → false;
8. `clock.frozen` → false;
9. senão → true.

`afterArmor`:
- só para participantes e quando `source.entity is Player` ou `source.is(DamageTypeTags.IS_EXPLOSION)`;
- `ArmorMath.apply(amount * 5, armor, helmet, HeadshotFlag.consume())`;
- grava `armor` e `lastHitHeadshot`;
- devolve `healthDamage / 5`.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew runGametest` → PASS.
- [ ] **Step 5: Conferir o headshot no jogo** — `./gradlew runClient`, verifique no log que o `HurtingItemMixin` aplicou (sem o aviso de mixin ignorado). Isso fica confirmado de vez no teste a dois da Task 20.
- [ ] **Step 6: Commit** — `git add -A && git commit -m "feat: dano do CS com colete, sem fogo amigo e headshot"`

---

### Task 15: Proteções da área, itens, comandos e baús

**Files:**
- Create: `src/main/kotlin/com/shifustrike/guard/AreaGuard.kt`, `CommandGuard.kt`, `InteractionGuard.kt`
- Create: `src/main/java/com/shifustrike/mixin/ExplosionMixin.java`, `LevelMixin.java`, `CommandsMixin.java`, `ItemEntityMixin.java`
- Modify: `shifustrike.mixins.json`, `Match.kt` (chamar `AreaGuard.tick` a cada 10 ticks)
- Test: `src/test/kotlin/com/shifustrike/guard/CommandGuardTest.kt` (JUnit) e `src/main/kotlin/com/shifustrike/gametest/GuardGameTest.kt`

**Interfaces:**
- Consumes: `MatchManager.match`, `ArenaData.area/saida/dimensao`, `EventItems`, `Loadouts.slotOf`.
- Produces:
  - `object AreaGuard { fun isProtected(level: Level, x: Double, y: Double, z: Double): Boolean; fun tick(m: Match) }`
  - `object CommandGuard { fun isAllowed(commandLine: String, allowed: List<String>): Boolean }`. Tira a `/`, pega a primeira palavra em minúsculas e tira o namespace (`minecraft:tell` vira `tell`).
  - `object InteractionGuard { fun register(); fun allowPickup(p: Player, stack: ItemStack): Boolean }`
  - Mixins (todos de classes vanilla, sem depender do PointBlank):
    - `ExplosionMixin`: `@Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)`. Devolve false se `AreaGuard.isProtected(level, center())`. Precisa de `@Shadow` em `level` e `center()`.
    - `LevelMixin`: `@Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)`. Só do lado do servidor; devolve false se a posição estiver protegida.
    - `CommandsMixin`: `@Inject(method = "performPrefixedCommand", at = @At("HEAD"), cancellable = true)`. Se `source.getPlayer()` está jogando e `!isAllowed`, manda `Msg.erro` e cancela.
    - `ItemEntityMixin`: `@Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)` → cancela se `!allowPickup`.

- [ ] **Step 1: Escrever os testes que falham**

JUnit `CommandGuardTest`: `isAllowed("/cs sair")`, `isAllowed("msg Fulano oi")` e `isAllowed("/minecraft:tell a b")` são true. `isAllowed("/home")`, `isAllowed("/tpa x")`, `isAllowed("spawn")` e `isAllowed("")` são false.

GameTests:
- `explosao_na_area_nao_quebra`: com a partida em RUNNING, uma `level.explode(..., ExplosionInteraction.TNT)` no meio de blocos de pedra dentro da área deixa todos os blocos no lugar.
- `explosao_fora_da_area_quebra_normal` (Review Focus 3): a mesma explosão fora da área quebra blocos. Com a partida parada, dentro da área também quebra.
- `bala_nao_quebra_vidro`: `level.destroyBlock(vidro, true, ct)` dentro da área devolve false e o vidro fica.
- `de_fora_e_tirado_da_area`: um jogador falso fora da partida, em SURVIVAL dentro da área, vai para a saída em até 10 ticks. Em SPECTATOR, ele fica.
- `vivo_que_sai_volta_ao_spawn`: um participante teleportado para fora da área volta ao spawn do time.
- `ct_nao_pega_c4` (o mesmo da Task 12): agora com `required = true`.
- `nao_pega_segunda_primaria`: com ak47 no slot 0, uma galil no chão encostando continua no chão.
- `nao_abre_bau`: `UseBlockCallback` num baú, para participante, dá FAIL.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew test runGametest` → FAIL.

- [ ] **Step 3: Implementar**

`isProtected`: partida em RUNNING, mesma dimensão do mapa e `area.contains`.

`AreaGuard.tick`, para todos os jogadores online na dimensão do mapa:
- quem não está na partida, não é espectador do Minecraft e está dentro da área vai para a saída com aviso;
- participante vivo fora da área volta ao spawn do time.

`allowPickup`:
- quem não está jogando não pega item de evento;
- quem está jogando não pega item que não é de evento;
- C4 só pega TR;
- primária ou pistola só se o slot estiver vazio.

`InteractionGuard.register`:
- `UseBlockCallback` dá FAIL para participante quando `state.getMenuProvider(level, pos) != null` ou quando o block entity é um `Container`;
- `AttackEntityCallback` dá FAIL quando um participante bate em entidade que não é jogador.

Lembre de ligar o `@GameTest(required = true)` no `ct_nao_pega_c4`.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew test runGametest` → PASS.
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: proteção do mapa, comandos bloqueados, itens e baús"`

---

### Task 16: Rede (estado da partida para o cliente e pedidos de compra)

**Files:**
- Create: `src/main/kotlin/com/shifustrike/network/Payloads.kt`, `ServerNetworking.kt`
- Create: `src/client/kotlin/com/shifustrike/client/ClientMatch.kt`
- Modify: `Match.kt` (enviar o estado a cada 10 ticks e eventos), `ShifuStrikeClient.kt` (receivers), `ShifuStrike.kt` (`ServerNetworking.register()`)
- Test: `src/test/kotlin/com/shifustrike/network/PayloadCodecTest.kt`

**Interfaces:**
- Consumes: `Phase`, `Team`, `WinReason`, `Box`, `ShopEntry`, `KillInfo`, `Purchases`, `Spectators.cycle`.
- Produces (todos os payloads com `TYPE` e `CODEC`):
  - S2C `MatchInfoPayload(team: Team, shop: List<ShopEntryDto>, siteA: Box, siteB: Box)`. É enviado no início da partida e quando um participante volta. `data class ShopEntryDto(chave, coluna, posicao, nome, preco, precoAlt: Int?, tipo: EntryKind, icone: String)`.
  - S2C `MatchStatePayload`, enviado a cada 10 ticks para cada participante, com:
    - `phase`, `round`, `ctScore`, `trScore`, `timeLeftTicks`, `buyTicksLeft`;
    - `ctAlive`, `trAlive`;
    - `money`, `armor`, `helmet`, `alive`, `inBuyZone`;
    - `bombPlanted`, `bombPos: BlockPos?`, `carryingC4`.
  - S2C `KillFeedPayload(info: KillInfo)`, `RoundEndPayload(winner: Team, reason: WinReason, ctScore: Int, trScore: Int)`, `MatchEndPayload(winner: Team?, ctScore: Int, trScore: Int, lines: List<ScoreLine>)` com `ScoreLine(name, team, kills, deaths)`, e `MatchClearPayload` (enviado a quem é restaurado).
  - C2S `BuyPayload(chave: String)`, `RefundPayload(chave: String)`, `SpectateCyclePayload`.
  - `object ClientMatch`, com:
    - estado: `var info: MatchInfoPayload?`, `var state: MatchStatePayload?`, `val feed: ArrayDeque<Pair<KillInfo, Long>>`, `var banner: Banner?`;
    - `val active: Boolean`;
    - preenche `UseHints` (sites, bombPos, team).

- [ ] **Step 1: Escrever o teste que falha**

`PayloadCodecTest`: para cada payload, `CODEC.encode` num `RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)` e `decode` dá um objeto igual ao original. Inclua um `MatchStatePayload` com `bombPos = null` e outro com `BlockPos(1, 2, 3)`, e um `MatchInfoPayload` com 23 entradas do `DefaultShop`.
- [ ] **Step 2: Rodar e ver falhar** — `./gradlew test` → FAIL.
- [ ] **Step 3: Implementar**
  - Servidor:
    - registra os tipos com `PayloadTypeRegistry.playS2C()`/`playC2S()`;
    - receivers C2S chamam `Purchases.buy`/`refund`/`Spectators.cycle` no thread do servidor (`context.server().execute`), sempre conferindo `MatchManager.participant`;
    - o `Match` liga `onKill`, `endRound`, `end` e `startRound` aos envios.
  - Cliente: `ClientPlayNetworking.registerGlobalReceiver` atualiza o `ClientMatch`. `MatchClearPayload` e a desconexão (`ClientPlayConnectionEvents.DISCONNECT`) zeram tudo.
- [ ] **Step 4: Rodar e ver passar** — `./gradlew test runGametest` → PASS (os GameTests anteriores continuam passando, porque `canSend` é false para jogadores falsos).
- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat: rede entre servidor e cliente da partida"`

---

### Task 17: Tela de compra (tecla B)

**Files:**
- Create: `src/client/kotlin/com/shifustrike/client/screen/BuyScreen.kt`, `IconCache.kt`
- Modify: `ShifuStrikeClient.kt` (`KeyMapping("key.shifustrike.comprar", GLFW.GLFW_KEY_B, "key.categories.shifustrike")` via `KeyBindingHelper`)

**Interfaces:**
- Consumes: `ClientMatch`, `ShopEntryDto`, `BuyPayload`, `RefundPayload`.
- Produces:
  - `class BuyScreen : Screen(Component.literal("Comprar"))`
  - `object IconCache { fun icon(path: String): ResourceLocation? }`. Lê o PNG pelo `ResourceManager`, troca o RGB por branco mantendo o alfa, registra como `DynamicTexture` em `shifustrike:icone_gerado/<hash>` e guarda em cache. Se o arquivo não existir, devolve null e o quadrado mostra só o nome.

- [ ] **Step 1: Implementar a abertura**

B abre a tela só se `ClientMatch.state` diz `alive && buyTicksLeft > 0 && inBuyZone`. Senão, mostra na barra de ação "Fora do tempo de compra" ou "Longe da zona de compra". Com a tela aberta, B ou ESC fecham. Se o `buyTicksLeft` zerar, a tela fecha sozinha.

- [ ] **Step 2: Implementar o layout do mockup**

Referência: `.superpowers/brainstorm/901-1793068773/content/tela-compra.html`.
- Painel escuro translúcido (`0xDD15181B`) ocupando cerca de 60% da largura à esquerda.
- Topo: dinheiro em verde, "Tempo de compra restante: m:ss" e "Antiterroristas · Rodada N".
- 5 colunas com cabeçalho numerado: Equipamento, Pistolas, Intermediárias, Rifles, Granadas.
- Quadrados (`0xFF262A2F`, borda clara no hover):
  - número no canto superior esquerdo, nome no superior direito, preço no inferior direito;
  - ícone no centro, tingido de `0x8EC5FF` (CT) ou `0xE8B64C` (TR) com `RenderSystem.setShaderColor`;
  - "em breve" com alfa 0,35; preço acima do dinheiro com o quadrado escurecido.
  - COLETE_CAPACETE mostra o preço calculado: `precoAlt` se `armor >= 100 && !helmet`.
- À direita, o modelo do jogador com `InventoryScreen.renderEntityInInventoryFollowsMouse`.
- Rodapé: dinheiro e "[B] FECHAR · [1-5] CATEGORIA / ITEM · [CLIQUE DIREITO] REEMBOLSAR".
- Tamanhos calculados a partir de `width`/`height`, para caber de 854×480 para cima.

- [ ] **Step 3: Implementar a entrada**
  - Clique esquerdo manda `BuyPayload`; clique direito manda `RefundPayload`.
  - Teclas 1-5: a primeira escolhe a coluna (destacada), a segunda compra o item daquela posição. ESC dentro da coluna volta ao nível de colunas.

- [ ] **Step 4: Conferir no jogo**

Run: `./gradlew runServer`, com o mapa do selftest, `online-mode=false` em `run/server.properties` e `minimoJogadores=1` em `run/config/shifustrike/config.json`. Depois `./gradlew runClient`, conecte em `localhost`, `/cs criar`, `/cs entrar`, `/cs iniciar`.
Expected:
- B abre uma tela com a mesma disposição do mockup e as silhuetas das armas na cor do time;
- comprar a ak47 (TR) desconta $2700 quando houver dinheiro;
- clique direito reembolsa;
- depois de 30s, B mostra "Fora do tempo de compra".

Tire um print e compare com o mockup. **Confira também se o B abre alguma coisa do Xaero's Minimap ao mesmo tempo.** Se abrir, anote para a Task 20.

- [ ] **Step 5: Commit** — `git add -A && git commit -m "feat(cliente): tela de compra no layout aprovado"`

---

### Task 18: HUD do CS

**Files:**
- Create: `src/client/kotlin/com/shifustrike/client/hud/StrikeHud.kt`, `KillFeed.kt`
- Create: `src/client/java/com/shifustrike/mixin/client/GuiMixin.java`
- Modify: `shifustrike.client.mixins.json`, `ShifuStrikeClient.kt` (`HudRenderCallback.EVENT.register { g, delta -> StrikeHud.render(g, delta) }`, como em `shifurace/client/ShifuRaceClient.kt:60`; ataque em espectador manda `SpectateCyclePayload`)

**Interfaces:**
- Consumes: `ClientMatch`, `ModItems.C4`, `ModItems.KIT`.
- Produces:
  - `object StrikeHud { fun render(g: GuiGraphics, delta: DeltaTracker) }`
  - `object KillFeed { fun render(g: GuiGraphics, now: Long) }`. Mostra no máximo 5 linhas, cada uma por 6 s.
  - `GuiMixin`: `@Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)`, cancelando quando `ClientMatch.active`.

- [ ] **Step 1: Implementar** (seção 10 da spec)
  - Topo central: "CT n × n TR", com o tempo `m:ss` ou o ícone da C4 piscando quando a bomba estiver plantada (sem cronômetro), e os vivos de cada time.
  - Inferior esquerdo: `$dinheiro`, vida (`player.health * 5`) e colete (com "+ capacete" se tiver).
  - Superior direito: o `KillFeed` no formato "Matador [arma] Vítima", com cor do time e "(HS)" para headshot.
  - Centro: faixa do fim de rodada por 4 s e do fim da partida por 8 s, com o placar final.
  - Barra de progresso: se `player.isUsingItem` com C4 ou kit, mostra `ticksUsingItem / useDuration` com "Plantando..." ou "Desarmando...".
  - Ícone da C4 ao lado do dinheiro para quem estiver carregando a bomba.
  - Espectador da partida: "Assistindo: <nome>" e "[Clique] próximo".
- [ ] **Step 2: Conferir no jogo**

Use o mesmo roteiro da Task 17, com 1 jogador TR (`minimoJogadores=1`): plante a C4 no site e espere a explosão.
Expected:
- o placar e o tempo descem, e a vida e a fome do Minecraft somem;
- a barra "Plantando..." enche em 3 s e o ícone de bomba plantada aparece;
- a faixa "Terroristas venceram" aparece, o placar vai a 1 × 0 e o dinheiro sobe 3500.
- [ ] **Step 3: Commit** — `git add -A && git commit -m "feat(cliente): HUD do CS, feed de kills e barras de progresso"`

---

### Task 19: Flan (PvP dentro de claim)

**Files:**
- Modify: `build.gradle.kts` (`modLocalRuntime("maven.modrinth:flan:1.21.1-1.12.8-fabric")`)
- Possível Create: `src/main/java/com/shifustrike/mixin/FlanPvpMixin.java` (`@Pseudo`, `require = 0`)

**Interfaces:**
- Consumes: `MatchManager.participant`, para saber se os dois são adversários na mesma partida.

- [ ] **Step 1: Montar o cenário**

Com `runServer`, crie um claim de admin do Flan cobrindo a área do selftest e deixe a permissão de PvP do claim **desligada**. Inicie uma partida com dois clientes:
- `./gradlew runClient`;
- em outro terminal, `./gradlew runClient --args="--username Jogador2"`.

- [ ] **Step 2: Testar**

Expected: o tiro da ak47 do CT no TR causa dano e a kill conta. Se causar, anote "Flan não bloqueia dano de arma em claim", tire o Flan do `build.gradle.kts`, faça commit e pule para a Task 20.

- [ ] **Step 3: Se bloquear, contornar**

1. Descompile o jar do Flan, que está no cache do Gradle, com Vineflower.
2. Ache a checagem de PvP. A pesquisa acha que ela está em `AttackEntityCallback` ou num mixin de `Player.hurt`/`ServerPlayer.hurt`.
3. Escreva `FlanPvpMixin` (`@Pseudo`, seletores só por nome, `remap = false` nos alvos do Flan, `require = 0`). Ele faz a checagem do Flan permitir quando atacante e vítima são adversários vivos da mesma partida.
4. Repita o Step 2 até passar.

- [ ] **Step 4: Commit** — `git add -A && git commit -m "fix: PvP da partida funciona dentro de claim do Flan"` (ou o commit de anotação do Step 2)

---

### Task 20: Partida completa a dois e jar final

**Files:**
- Modify: `README.md` (comandos, montagem do mapa, ordem de subida do PointBlank e do ShifuStrike: primeiro para os jogadores, depois para o servidor)

- [ ] **Step 1: Rodar tudo**

Run: `./gradlew clean build test runGametest`
Expected: `BUILD SUCCESSFUL`, todos os testes JUnit passando e "All N required tests passed".

- [ ] **Step 2: Jogar uma partida completa com dois clientes**

Use `minimoJogadores=2`, `vitoriasParaVencer=2` e os dois `runClient` da Task 19. Marque cada item:
- [ ] `/cs criar`, os dois `/cs entrar`, `/cs iniciar`: os dois são teleportados para spawns de times diferentes e o inventário some.
- [ ] Congelamento de 10 s; a loja abre com B e fecha sozinha aos 30 s.
- [ ] Compra de pistola, colete e ak47/m4a4: o dinheiro desconta e as armas atiram e recarregam.
- [ ] Headshot aparece com "(HS)" no feed (confirma o `HurtingItemMixin`). Se nunca aparecer, anote como limitação conhecida.
- [ ] Kill de faca: +$1500.
- [ ] Granada dentro do mapa não quebra blocos nem vidro.
- [ ] `/home` durante a partida é bloqueado; `/msg` funciona.
- [ ] Rodada de bomba plantada e desarmada; rodada por tempo; fim da partida com placar.
- [ ] No fim, os dois voltam à saída com o inventário original e o modo de jogo de antes.
- [ ] Derrubar o servidor no meio (fechar o `runServer`), subir de novo e entrar: o inventário original volta.
- [ ] B não abre o Xaero junto. Se abrir, anote no README que os jogadores podem trocar a tecla nas opções.

- [ ] **Step 3: Conferir o jar**

Run: `unzip -l build/libs/shifustrike-1.0.0.jar | grep -i pointblank`
Expected: nenhuma linha. Nada do PointBlank pode estar dentro do nosso jar.

- [ ] **Step 4: Commit** — `git add -A && git commit -m "docs: README com comandos, montagem e ordem de subida"`
