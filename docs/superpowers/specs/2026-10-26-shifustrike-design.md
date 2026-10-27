# ShifuStrike (CS no Minecraft): design

Data: 2026-10-26
Autor do pedido: dono do servidor Eclipse Cobblemon
Status: aprovado pelo dono em 2026-10-27

## 1. Intenção

O servidor **Eclipse Cobblemon** (Fabric 1.21.1) quer um **evento de Counter-Strike dentro do Minecraft**: duas equipes, Terroristas (TR) e Antiterroristas (CT), em rodadas com compra de armas, bomba C4, desarme e economia, até um time chegar a **5 vitórias**. As armas são as do mod **PointBlank**, que já é usado pelo servidor.

É um **evento conduzido pela staff**. A staff cria a partida (`/cs criar`), os jogadores se inscrevem (`/cs entrar`) e, com `/cs iniciar`, todos são teleportados para o mapa que o servidor já construiu, com os spawns de CT e TR. A partir daí o mod controla tudo sozinho: times, dinheiro, compras, bomba, tempos, vitórias, recompensas e a próxima rodada.

O que importa para o dono:
- **parecer CS**: tela de compra igual às fotos do CS2 que ele mandou, tecla **B**, preços do CS e as regras de rodada e economia do CS;
- **funcionar sozinho** depois do `/cs iniciar`, sem a staff ter que intervir rodada a rodada;
- usar as **armas do PointBlank**.

Nome técnico do mod: **ShifuStrike** (id `shifustrike`), seguindo o padrão dos outros mods do servidor (`shifutower`, `shifurace`). *Escolha do Claude.* Os comandos continuam `/cs ...`.

## 2. Objetivos

1. Staff configura o mapa dentro do jogo: spawns de CT e TR, as áreas dos bombsites A e B, a área do mapa e o ponto de saída.
2. Fluxo do evento por comando: criar, inscrever, iniciar, jogar as rodadas sozinho, encerrar e devolver tudo.
3. Rodadas com as regras do CS (seção 7) e economia do CS (seção 8), começando com **$500** por jogador.
4. Tela de compra própria, aberta com **B**, no layout aprovado (seção 9).
5. Armas do PointBlank entregues já carregadas e com munição de reserva; granada de fragmentação do PointBlank.
6. Itens próprios do mod: **C4**, **faca**, **colete de Kevlar** e **capacete**. Todo CT recebe o **kit de desarme** de graça.
7. HUD de CS: placar, tempo, dinheiro, feed de kills, aviso de bomba plantada e barras de plantar e desarmar.
8. O jogador **nunca perde nada** por participar (inventário, Pokémon, posição e modo de jogo voltam ao normal).

## 3. Não-objetivos (primeira versão)

- **Flashbang, fumaça, molotov ou incendiária, decoy e Zeus x27** ficam para a próxima versão. Aparecem na tela de compra **apagados, como "em breve"** (aprovado assim).
- Sem troca de lado no meio da partida.
- Sem prêmio automático para o time vencedor. Se houver prêmio, a staff entrega por fora.
- Sem vários mapas: há um mapa só, já construído.
- Sem modo competitivo ranqueado, estatísticas guardadas entre eventos, skins ou mercado.
- Sem versão "só servidor" e sem menu de baú como reserva (abordagem B e C descartadas, seção 6).
- Sem mexer nos outros mods do servidor nem na config global do PointBlank.
- **Voz (Simple Voice Chat) fica como está**, por proximidade (decisão do dono). Voz por time pode vir depois.
- Subir para o servidor e para o modpack dos jogadores não entra nesta entrega (decisão do dono).

## 4. Anti-objetivos (falharia mesmo "funcionando")

- **Jogador perder algo**: itens do inventário, armadura, Pokémon do time, modo de jogo ou ficar preso no mapa depois do evento, inclusive se o servidor cair no meio da partida.
- **Itens do evento vazarem para fora**: arma, munição, C4, faca, colete ou dinheiro do CS ficando com o jogador depois do evento, sendo jogados fora do mapa ou guardados em baús.
- **Mapa destruído**: granada ou bala quebrando blocos do mapa. No servidor, o PointBlank está com `explosionDestroyBlocksEnabled = true` e `bulletsBreakGlassEnabled = true`.
- **Partida injusta**: fogo amigo, comprar fora do tempo, CT pegando a C4, morto passando informação pelo modo espectador livre, jogador escapando do mapa com `/home`, `/tpa`, `/spawn` ou `/warp`.
- **Copiar arquivos da Valve** (sons, ícones, texturas do CS). Nomes de armas e regras podem ser usados; arquivos de arte e som, não.

## 5. Restrições

- **Plataforma**: Fabric, Minecraft 1.21.1, Java 21, Kotlin. Usa a mesma base de build dos mods de corrida e da torre (`D:\Mod de corrida`): Fabric Loom 1.10.5, Kotlin 2.2.20, mapeamentos Mojang + Parchment `2024.11.17`, `fabric-language-kotlin 1.13.6+kotlin.2.2.20`, `splitEnvironmentSourceSets()` (código `main` + `client`), testes JUnit 5.
- **Servidor** (painel Tick): loader **0.20.0**, `fabric-api-0.116.16+1.21.1`. Cobblemon 1.7.3 vem dentro do `Eclipse-Cobblemon-1.2.0.jar`. Há também Flan (claims), Essential Commands (`/home`, `/tpa`, `/back`...), LuckPerms, TAB, Simple Voice Chat e EasyAuth.
- **PointBlank** `pointblank-fabric-1.21-2.2.0.jar` (cópia em `E:\Site Eclipse Cobblemon\mods recomendados`). Ele exige GeckoLib ≥ 4.5.8 e fabricloader ≥ 0.18.4. As configs dele (`config/pointblank-*.toml`) existem no servidor, mas o jar não aparece solto em `/mods`. **Decisão do dono:** primeiro o mod é feito e testado localmente (com PointBlank e GeckoLib no ambiente de desenvolvimento). Depois o PointBlank e o ShifuStrike sobem **juntos** para o servidor e para os jogadores. Essa subida não faz parte desta entrega. Licença: All Rights Reserved, então **não redistribuir nada do PointBlank** dentro do nosso jar (texturas, modelos, código).
- **Sem dependência de código do PointBlank** (decisão da abordagem A): o mod se refere às armas só pelo id do item (`pointblank:ak47`). Se for preciso ler ou gravar dados internos do item (por exemplo, a munição carregada), isso acontece pelos componentes de dados do item, sem compilar contra as classes do PointBlank. Se uma arma configurada não existir, o mod avisa no log e tira o item da loja, sem travar.
- **Cliente obrigatório**: o mod registra itens próprios (C4, faca, colete, capacete). Com o Fabric, isso faz o cliente **sem o mod não conseguir entrar no servidor**. O jar tem que ir para o launcher dos jogadores **junto** com o servidor.
- **Distribuição (depois, fora desta entrega)**: script pronto `node "E:/Site Eclipse Cobblemon/ferramentas/tick/subir-mod.mjs" <jar> [--trocar]`. Subir para produção e reiniciar o servidor só com o ok do dono. Ordem obrigatória: os jogadores recebem os jars **antes** do servidor, senão ninguém entra.
- **Idioma**: todo texto visível em português do Brasil (`pt_br` e `en_us` com o mesmo texto em português, como a torre e a corrida).
- Pasta do projeto: `D:\Mod Cs pra minecraft` (ainda vazia, não é repositório git).

## 6. Abordagens consideradas

| | Abordagem | A favor | Contra |
|---|---|---|---|
| **A (escolhida pelo dono)** | **Mod no servidor e no cliente.** Tela de compra própria, tecla B e HUD. Armas do PointBlank tratadas só pelo id do item | Única que entrega a tela das fotos e a tecla B; o modpack do servidor já resolve a instalação; uma atualização do PointBlank dificilmente quebra o nosso mod | Todo jogador precisa do jar no cliente |
| B | Só no servidor: menu de baú, aberto por item ou comando, e placar na barra de chefe e no scoreboard | Ninguém instala nada | Sem tecla B, sem tela parecida com as fotos |
| C | A e B juntos (tela para quem tem o mod, baú como reserva) | Funciona com e sem o mod | Quase o dobro de trabalho na interface; só compensa se alguém jogar sem o modpack, e isso não acontece |

## 7. Regras da partida (aprovadas)

### Montagem do mapa (uma vez, comandos de staff)
- `/cs mapa spawn ct|tr` adiciona um ponto de spawn (posição e direção) onde a staff está. Pode haver vários por time; os jogadores são distribuídos entre eles.
- `/cs mapa bomba a|b` marca a área do bombsite com duas posições (dois cantos).
- `/cs mapa saida` define para onde todos voltam no fim.
- *Escolha do Claude:* `/cs mapa area` marca a área do mapa inteiro (dois cantos). Ela serve para proteger os blocos, limpar itens no chão e devolver ao spawn quem sair dela. Também *escolha do Claude:* comandos para listar e limpar pontos (`/cs mapa ver`, `/cs mapa limpar ...`) e um aviso no `/cs criar` se faltar algo.
- A config do mapa fica salva em arquivo e inclui a dimensão.

### Evento
- `/cs criar` abre as inscrições. `/cs entrar` e `/cs sair` estão abertos a todos. `/cs iniciar` e `/cs cancelar` são só para a staff.
- *Escolha do Claude:* só dá para entrar antes do `/cs iniciar`. São precisos pelo menos 2 jogadores, um por time.
- No `/cs iniciar`, os times são divididos automaticamente e equilibrados (diferença máxima de 1).
- Ao começar, o mod guarda e limpa o inventário do jogador (incluindo armadura e mão secundária), guarda posição, modo de jogo, vida, fome e efeitos. Os Pokémon dele são recolhidos e ficam bloqueados (não pode soltar, montar, batalhar nem jogar Poké Bola) até o fim. O jogador fica em **modo aventura**, que impede quebrar e colocar blocos.
- Os dados guardados vão **para o disco na hora**. Se o servidor cair, o jogador recebe tudo de volta quando entrar de novo.
- *Escolha do Claude:* durante a partida, os participantes só podem usar `/cs` e comandos de conversa (`/msg`, `/tell`, `/r`). Os outros comandos ficam bloqueados.
- **Fim**: todos voltam ao ponto de saída com tudo restaurado. Itens do evento e dinheiro do CS somem.

### Rodada
- Início: todos são teleportados para os spawns do seu time, com vida cheia.
- **30s de compra** com B. Nos **primeiros 10s** o jogador fica parado no spawn (freeze time). *Escolha do Claude:* só dá para comprar perto do spawn do próprio time (raio configurável, padrão 15 blocos), como a zona de compra do CS.
- **Rodada de 1:55**. A C4 vai para um TR sorteado.
- **Plantar**: o TR com a C4 segura o clique direito por **3s** dentro de um bombsite. Se ele se mexer ou soltar, a contagem zera.
- **Bomba plantada**: o tempo da rodada para e começa a contagem de **40s**, com bip acelerando como no CS. O HUD mostra um ícone de bomba plantada, sem cronômetro (como no CS).
- **Desarmar**: o CT segura o clique direito na bomba por **5s**. Todo CT tem o kit, então o tempo é sempre 5s. Se ele se mexer ou soltar, a contagem zera.
- **Explosão**: mata quem estiver perto (raio e dano configuráveis) e não quebra blocos.
- Todos recebem **faca**. A pistola inicial é **Glock-18 (TR)** e **USP-S (CT)**.
- **Morte**: o jogador vira espectador até a próxima rodada, **preso à câmera de colegas vivos**. *Escolha do Claude:* ele troca de colega clicando. Ao morrer, ele larga no chão só a melhor arma (primária, ou a pistola se não tiver primária) e a C4, se estiver com ela. O resto some.
- **Sobreviventes** mantêm armas, colete e granadas para a próxima rodada. Os mortos voltam só com faca e pistola inicial.
- Armas no chão podem ser pegas por qualquer um. **A C4 só pode ser pega por TR.** O que estiver no chão some no fim da rodada.
- **Sem fogo amigo**, nem com granada.
- *Escolha do Claude:* sem regeneração natural de vida durante a rodada, como no CS. A vida cheia volta só no começo da rodada.
- **Fim de rodada** (cerca de 5s mostrando o vencedor, depois a próxima rodada):
  - todos os TR morrem **sem** bomba plantada: vitória CT;
  - todos os TR morrem **com** bomba plantada: a rodada continua, e o CT precisa desarmar;
  - todos os CT morrem: vitória TR;
  - bomba explode: vitória TR;
  - bomba desarmada: vitória CT;
  - tempo acaba sem bomba plantada: vitória CT.
- **Fim da partida**: primeiro time a **5 vitórias** (no máximo 9 rodadas, sem empate). *Escolha do Claude:* se um time ficar sem nenhum jogador (todos saíram ou desconectaram), o outro vence a partida.
- *Escolha do Claude:* quem desconecta no meio da partida conta como morto naquela rodada. Se voltar enquanto a partida durar, volta como espectador e joga a partir da próxima rodada. Se a partida já tiver acabado, recebe o inventário de volta.

## 8. Economia (aprovada; valores do CS, exceto o início)

| Evento | Valor |
|---|---|
| Dinheiro inicial | **$500** |
| Limite | $16000 |
| Vitória por eliminação ou tempo | $3250 para cada jogador do time |
| Vitória por bomba explodida ou desarmada | $3500 |
| Derrota | $1400, mais $500 a cada derrota seguida, até $3400 |
| TR perde depois de plantar | +$800 para cada TR |
| Kill com a maioria das armas (pistola, rifle, granada) | $300 |
| Kill com SMG | $600 |
| Kill com escopeta | $900 |
| Kill com AWP | $100 |
| Kill com faca | $1500 |

- O dinheiro é individual. Todos os valores ficam no arquivo de config.
- *Escolha do Claude:* a arma da kill é a que o atacante tinha na mão no momento do dano fatal, ou o tipo da granada se o dano veio de uma granada. Mortes sem atacante (queda, explosão da C4) não dão dinheiro a ninguém.

## 9. Tela de compra (aprovada)

Mockup aprovado: `.superpowers/brainstorm/901-1793068773/content/tela-compra.html` (os ícones estão na mesma pasta).

- O mesmo layout para os dois times, seguindo a foto do CT: painel escuro translúcido no lado esquerdo da tela, o modelo do jogador visível à direita e **5 colunas numeradas**: 1 Equipamento, 2 Pistolas, 3 Intermediárias, 4 Rifles, 5 Granadas.
- Cada quadrado mostra o número (canto superior esquerdo), o nome do CS (canto superior direito), o ícone da arma como silhueta na cor do time (azul CT, dourado TR) e o preço (canto inferior direito).
- Barra do topo com dinheiro e "Tempo de compra restante". Rodapé com dinheiro e atalhos.
- Itens apagados = "em breve". Item sem dinheiro suficiente aparece escurecido.
- Atalhos: **B** abre e fecha. **1-5** escolhe a categoria e depois o item (como no CS). Clique compra. *Do mockup:* clique direito **reembolsa** um item comprado nesta rodada durante o tempo de compra, se ele ainda estiver com o jogador.
- **Ícones**: as silhuetas são geradas **em tempo de execução** a partir das texturas `pointblank:textures/item/<arma>.icon.png` que o cliente já tem. Assim nada do PointBlank é copiado para o nosso jar. Itens próprios (colete, capacete, C4, faca) ganham ícones feitos para o mod.
- **A compra é validada no servidor**: tempo de compra, zona de compra, dinheiro, time, item ativo e limite por slot.
- **Regras de slot (CS):** 1 arma primária (intermediária ou rifle) e 1 pistola. Comprar outra primária larga a atual no chão. Granada explosiva: máximo 1 na v1. Colete: não compra se já tem colete cheio. "Colete + Capacete" custa $1000, ou $350 se já tem colete.
- Hotbar fixa *(escolha do Claude)*: 1 primária, 2 pistola, 3 faca, 4 granada, 5 C4.

### Tabela de armas (aprovada; editável na config)

`≈` = o PointBlank não tem essa arma, então foi usada a mais parecida.

| Coluna | # | CT | TR | Preço CT / TR | Item PointBlank |
|---|---|---|---|---|---|
| Equip. | 1 | Colete de Kevlar | igual | $650 | próprio |
| Equip. | 2 | Colete + Capacete | igual | $1000 ($350 só capacete) | próprio |
| Equip. | 3 | Zeus x27 (em breve) | igual | $200 | — |
| Pistolas | 1 | USP-S (inicial) | Glock-18 (inicial) | $200 | `usp45` / `glock18` |
| Pistolas | 2 | Berettas Duplas | igual | $300 | ≈ `m9` |
| Pistolas | 3 | P250 | igual | $300 | ≈ `p30l` |
| Pistolas | 4 | Five-SeveN | Tec-9 | $500 | ≈ `tti_viper` / ≈ `m1911a1` |
| Pistolas | 5 | Desert Eagle | igual | $700 | `deserteagle` |
| Interm. | 1 | MAG-7 | Nova | $1300 / $1050 | ≈ `m590` / ≈ `m870` |
| Interm. | 2 | XM1014 | igual | $2000 | `m1014` |
| Interm. | 3 | MP7 | MP5-SD | $1400 / $1500 | `mp7` / `mp5` |
| Interm. | 4 | P90 | igual | $2350 | `p90` |
| Interm. | 5 | MP9 | MAC-10 | $1250 / $1050 | ≈ `tmp` / ≈ `vector` |
| Rifles | 1 | SSG 08 | Galil AR | $1700 / $1800 | ≈ `c14` / ≈ `ak74` |
| Rifles | 2 | M4A1-S | AK-47 | $2900 / $2700 | ≈ `hk416` / `ak47` |
| Rifles | 3 | M4A4 | SSG 08 | $2900 / $1700 | ≈ `m4a1` / ≈ `c14` |
| Rifles | 4 | FAMAS | SG 553 | $1950 / $3000 | ≈ `g36c` / ≈ `aug` |
| Rifles | 5 | AWP | igual | $4750 | `l96a1` |
| Granadas | 1 | Granada de Luz (em breve) | Flashbang (em breve) | $200 | — |
| Granadas | 2 | Fumaça (em breve) | igual | $300 | — |
| Granadas | 3 | Granada Explosiva | igual | $300 | `grenade` |
| Granadas | 4 | Incendiária (em breve) | Molotov (em breve) | $500 / $400 | — |
| Granadas | 5 | Distração (em breve) | Decoy (em breve) | $50 | — |

Categoria de recompensa por kill: SMG = MP7, MP5-SD, P90, MP9, MAC-10. Escopeta = MAG-7, Nova, XM1014. AWP = AWP. O resto conta como "padrão".

Munição: cada arma vem **carregada** e com a **reserva do CS** em itens de munição do PointBlank (por exemplo, AK-47 com 30 no pente e 90 de reserva). A quantidade fica por arma na config. A recarga funciona do jeito normal do PointBlank, consumindo os itens de munição do inventário.

## 10. HUD (cliente)

- Topo central: placar CT × TR, tempo da rodada (ou ícone de bomba plantada) e quantos jogadores vivos em cada time.
- Canto inferior esquerdo: dinheiro, vida (0-100, que é a vida do Minecraft vezes 5) e colete.
- Canto superior direito: **feed de kills** (quem matou quem, com que arma, e marca de headshot se der para saber).
- Faixa central no fim da rodada ("Antiterroristas venceram") e no fim da partida.
- Barra de progresso ao plantar e ao desarmar.
- Mensagens e sons usam sons do Minecraft, nada da Valve.
- *Escolha do Claude:* a barra de vida e a fome do Minecraft ficam escondidas durante a partida, porque o HUD do CS mostra a vida.

## 11. Colete e capacete

- O colete tem **100 pontos** de armadura e o capacete protege a cabeça.
- Inspiração CS: o colete corta mais ou menos **metade do dano de bala** no corpo e gasta pontos de armadura. O capacete corta o dano extra de tiro na cabeça.
- O PointBlank já tem multiplicador de headshot (`headshotDamageModifier = 3.0`).
- Os números exatos e o jeito de detectar headshot ficam para quem construir (seção 13), com valores na config.
- O colete e o capacete não dão armadura de Minecraft (não podem empilhar com armadura de verdade, que de qualquer forma foi guardada).

## 12. Proteções obrigatórias

- **Blocos do mapa**: nada quebra blocos dentro de `/cs mapa area` enquanto houver uma partida ativa, nem explosões, nem balas no vidro, nem qualquer outra coisa. A config global do PointBlank não é alterada.
- **PvP**: o dano entre participantes de times diferentes tem que funcionar mesmo que o mapa esteja dentro de um claim do Flan ou de uma área com PvP desligado. O dano de quem não está na partida contra quem está (e o contrário) é bloqueado.
- **Itens do evento** levam uma marca. Eles não podem ser guardados em baús nem sair da área. Se aparecerem com alguém fora da partida (bug, crash), são apagados ao entrar no servidor.
- **Sair da área**: o jogador volta para o spawn do time (vivo) ou é preso de novo à câmera (morto).
- **Quem não está jogando** *(escolha do Claude)*: durante a partida, ninguém de fora entra na área do mapa. Quem estiver dentro no `/cs iniciar` ou tentar entrar é levado para o ponto de saída. A exceção é a staff em modo espectador do Minecraft, que pode assistir livremente sem ver o HUD nem a tela de compra. Ela não interage com nada.

## 13. Fica para quem construir

- Como o PointBlank guarda a munição carregada no item (componentes de dados) e como entregar a arma já carregada sem depender das classes dele. Também como saber, pelo dano, se foi headshot e de qual arma ou granada veio.
- Números do colete e do capacete, raio e dano da explosão da C4, raio da zona de compra (todos na config).
- Visual exato do HUD, dos ícones próprios e do modelo da C4 plantada, desde que siga o estilo do mockup.
- Formato dos arquivos de config e de dados. Permissão de staff no mesmo esquema dos outros mods (`shifurace`), compatível com LuckPerms.
- Divisão do código, protocolo de rede cliente-servidor e testes (lógica de rodada, economia e vitória em testes de unidade, como na corrida).
- Mensagens no chat e textos da interface, em português.
