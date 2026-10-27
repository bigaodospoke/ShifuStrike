package com.shifustrike.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.shifustrike.arena.ArenaData
import com.shifustrike.arena.ArenaStorage
import com.shifustrike.arena.Ponto
import com.shifustrike.config.ConfigManager
import com.shifustrike.core.Box
import com.shifustrike.core.EntryKind
import com.shifustrike.core.Team
import com.shifustrike.match.Match
import com.shifustrike.match.MatchManager
import com.shifustrike.match.Msg
import com.shifustrike.pointblank.EventItems
import com.shifustrike.pointblank.PointBlankItems
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

object StrikeCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val root = Commands.literal("cs")
            .then(staff("criar").executes { ctx -> result(ctx, MatchManager.create(ctx.source.server), "Partida criada! Os jogadores entram com /cs entrar.", announce = true) })
            .then(Commands.literal("entrar").executes { ctx ->
                val p = ctx.source.playerOrException
                result(ctx, MatchManager.join(p), "Você entrou no evento. Aguarde a staff iniciar.")
            })
            .then(Commands.literal("sair").executes { ctx ->
                val p = ctx.source.playerOrException
                result(ctx, MatchManager.leave(p), "Você saiu do evento.")
            })
            .then(staff("iniciar").executes { ctx -> result(ctx, MatchManager.start(ctx.source.server), "Partida iniciada.") })
            .then(staff("cancelar").executes { ctx -> result(ctx, MatchManager.cancel(ctx.source.server), "Partida cancelada.") })
            .then(Commands.literal("status").executes { ctx -> status(ctx) })
            .then(staff("mapa")
                .then(Commands.literal("spawn")
                    .then(Commands.literal("ct").executes { addSpawn(it, ct = true) })
                    .then(Commands.literal("tr").executes { addSpawn(it, ct = false) }))
                .then(Commands.literal("bomba")
                    .then(Commands.literal("a").then(boxArgs { ctx, box -> editArena(ctx, "Bomba A marcada.") { it.bombaA = box } }))
                    .then(Commands.literal("b").then(boxArgs { ctx, box -> editArena(ctx, "Bomba B marcada.") { it.bombaB = box } })))
                .then(Commands.literal("area").then(boxArgs { ctx, box -> editArena(ctx, "Área do mapa marcada.") { it.area = box } }))
                .then(Commands.literal("saida").executes { ctx ->
                    val p = here(ctx)
                    editArena(ctx, "Saída marcada.") { it.saida = p }
                })
                .then(Commands.literal("ver").executes { ctx -> show(ctx) })
                .then(Commands.literal("limpar")
                    .then(Commands.literal("spawns")
                        .then(Commands.literal("ct").executes { editArena(it, "Spawns CT apagados.", false) { a -> a.spawnsCt.clear() } })
                        .then(Commands.literal("tr").executes { editArena(it, "Spawns TR apagados.", false) { a -> a.spawnsTr.clear() } }))
                    .then(Commands.literal("bomba")
                        .then(Commands.literal("a").executes { editArena(it, "Bomba A apagada.", false) { a -> a.bombaA = null } })
                        .then(Commands.literal("b").executes { editArena(it, "Bomba B apagada.", false) { a -> a.bombaB = null } }))
                    .then(Commands.literal("area").executes { editArena(it, "Área apagada.", false) { a -> a.area = null } })
                    .then(Commands.literal("saida").executes { editArena(it, "Saída apagada.", false) { a -> a.saida = null } })
                    .then(Commands.literal("tudo").executes { ctx ->
                        editArena(ctx, "Mapa apagado.", false) { a ->
                            a.spawnsCt.clear(); a.spawnsTr.clear(); a.bombaA = null; a.bombaB = null; a.area = null; a.saida = null
                        }
                    })))
            .then(staff("diagnostico").executes { ctx -> diagnose(ctx) })
            .then(staff("teste").then(Commands.literal("arma").then(
                Commands.argument("chave", StringArgumentType.word())
                    .suggests { _, b -> SharedSuggestionProvider.suggest(ConfigManager.shop.filter { it.item != null }.map { it.chave }.distinct(), b) }
                    .executes { ctx -> giveTestWeapon(ctx, StringArgumentType.getString(ctx, "chave")) },
            )))
            .then(staff("recarregar").executes { ctx ->
                if (MatchManager.match != null) {
                    ctx.source.sendFailure(Msg.error("Há uma partida aberta; recarregue depois que ela acabar."))
                    return@executes 0
                }
                ConfigManager.load()
                ok(ctx, "Configuração e loja recarregadas.")
            })
        dispatcher.register(root)
    }

    private fun staff(name: String): LiteralArgumentBuilder<CommandSourceStack> =
        Commands.literal(name).requires { it.hasPermission(2) }

    private fun boxArgs(apply: (CommandContext<CommandSourceStack>, Box) -> Int) =
        Commands.argument("de", BlockPosArgument.blockPos()).then(Commands.argument("ate", BlockPosArgument.blockPos()).executes { ctx ->
            val a = BlockPosArgument.getBlockPos(ctx, "de")
            val b = BlockPosArgument.getBlockPos(ctx, "ate")
            apply(ctx, Box.of(a.x, a.y, a.z, b.x, b.y, b.z))
        })

    private fun here(ctx: CommandContext<CommandSourceStack>): Ponto {
        val pos = ctx.source.position
        val rot = ctx.source.rotation
        return Ponto(pos.x, pos.y, pos.z, rot.y, rot.x)
    }

    private fun addSpawn(ctx: CommandContext<CommandSourceStack>, ct: Boolean): Int {
        val p = here(ctx)
        return editArena(ctx, "Spawn ${if (ct) "CT" else "TR"} adicionado.") { a -> (if (ct) a.spawnsCt else a.spawnsTr) += p }
    }

    /** Muda o mapa e salva. Ao marcar algo (`marks`), o mapa passa a ser da dimensão de quem marcou. */
    private fun editArena(ctx: CommandContext<CommandSourceStack>, done: String, marks: Boolean = true, change: (ArenaData) -> Unit): Int {
        // a partida usa este mesmo mapa: mudar no meio (ex.: apagar os spawns) quebraria a rodada
        if (MatchManager.match != null) {
            ctx.source.sendFailure(Msg.error("Há uma partida aberta; edite o mapa depois que ela acabar (/cs cancelar)."))
            return 0
        }
        val arena = ArenaStorage.arena
        if (marks) {
            val dim = ctx.source.level.dimension().location().toString()
            if (!arena.empty && arena.dimensao != dim) {
                ctx.source.sendFailure(Msg.error("O mapa está em ${arena.dimensao}. Use /cs mapa limpar tudo antes de marcar em outra dimensão."))
                return 0
            }
            arena.dimensao = dim
        }
        change(arena)
        ArenaStorage.save()
        return ok(ctx, done)
    }

    private fun fmt(b: Box?) = b?.let { "${it.minX} ${it.minY} ${it.minZ} → ${it.maxX} ${it.maxY} ${it.maxZ}" } ?: "—"
    private fun fmt(p: Ponto) = "${"%.1f".format(p.x)} ${"%.1f".format(p.y)} ${"%.1f".format(p.z)}"

    private fun show(ctx: CommandContext<CommandSourceStack>): Int {
        val a = ArenaStorage.arena
        val text = Msg.prefix().append(Component.literal("Mapa (${a.dimensao}):"))
        text.append("\nSpawns CT: ${a.spawnsCt.joinToString(" | ") { fmt(it) }.ifEmpty { "—" }}")
        text.append("\nSpawns TR: ${a.spawnsTr.joinToString(" | ") { fmt(it) }.ifEmpty { "—" }}")
        text.append("\nBomba A: ${fmt(a.bombaA)}\nBomba B: ${fmt(a.bombaB)}\nÁrea: ${fmt(a.area)}")
        text.append("\nSaída: ${a.saida?.let { fmt(it) } ?: "—"}")
        ctx.source.sendSuccess({ text }, false)
        return 1
    }

    private fun diagnose(ctx: CommandContext<CommandSourceStack>): Int {
        val lines = mutableListOf<String>()
        val problems = ArenaStorage.arena.problemas()
        if (problems.isEmpty()) lines += "mapa completo" else problems.forEach { lines += "PROBLEMA: $it" }
        val ak = ResourceLocation.parse("pointblank:ak47")
        lines += if (BuiltInRegistries.ITEM.containsKey(ak)) "pointblank:ak47 ok" else "PROBLEMA: PointBlank não está carregado"
        ConfigManager.activeShop()
        ConfigManager.removedFromShop.forEach { lines += "PROBLEMA: fora da loja: $it" }
        val text = Msg.prefix().append(Component.literal("Diagnóstico:"))
        lines.forEach { text.append(Component.literal("\n$it")) }
        ctx.source.sendSuccess({ text }, false)
        return if (lines.any { it.startsWith("PROBLEMA") }) 0 else 1
    }

    private fun result(ctx: CommandContext<CommandSourceStack>, error: String?, done: String, announce: Boolean = false): Int {
        if (error != null) {
            ctx.source.sendFailure(Msg.error(error))
            return 0
        }
        if (announce) ctx.source.server.playerList.broadcastSystemMessage(Msg.info(done), false) else ok(ctx, done)
        return 1
    }

    private fun status(ctx: CommandContext<CommandSourceStack>): Int {
        val m = MatchManager.match
        val text = when {
            m == null -> "Nenhuma partida aberta."
            m.state == Match.State.LOBBY -> "Inscrições abertas: ${m.lobby.size} jogador(es)."
            else -> "Rodada ${m.round} — CT ${m.score[Team.CT]} × ${m.score[Team.TR]} TR — " +
                "vivos: CT ${m.players.values.count { it.team == Team.CT && it.alive }}, TR ${m.players.values.count { it.team == Team.TR && it.alive }}"
        }
        return ok(ctx, text)
    }

    /** Só para testar: dá a arma da loja carregada e com a reserva, marcada como item do evento. */
    private fun giveTestWeapon(ctx: CommandContext<CommandSourceStack>, chave: String): Int {
        val player = ctx.source.playerOrException
        val e = ConfigManager.activeShop().firstOrNull { it.chave == chave && it.item != null }
        if (e == null) {
            ctx.source.sendFailure(Msg.error("Não há arma '$chave' na loja."))
            return 0
        }
        val stacks = if (e.tipo == EntryKind.GRANADA) {
            listOfNotNull(PointBlankItems.grenade())
        } else {
            listOfNotNull(PointBlankItems.loadedGun(player, e.item!!, e.pente)) + PointBlankItems.ammo(e.municao!!, e.reserva)
        }
        stacks.forEach { player.inventory.add(EventItems.mark(it)) }
        return ok(ctx, "${e.nome} entregue.")
    }

    private fun ok(ctx: CommandContext<CommandSourceStack>, text: String): Int {
        ctx.source.sendSuccess({ Msg.info(text) }, false)
        return 1
    }
}
