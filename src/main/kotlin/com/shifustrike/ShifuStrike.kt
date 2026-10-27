package com.shifustrike

import com.shifustrike.arena.ArenaStorage
import com.shifustrike.command.StrikeCommands
import com.shifustrike.config.ConfigManager
import com.shifustrike.data.SnapshotStorage
import com.shifustrike.guard.CobblemonGuard
import com.shifustrike.match.Deaths
import com.shifustrike.match.MatchManager
import com.shifustrike.registry.ModItems
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.minecraft.server.level.ServerPlayer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ShifuStrike : ModInitializer {
    const val MOD_ID = "shifustrike"
    val LOGGER: Logger = LoggerFactory.getLogger("ShifuStrike")

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
        ModItems.register()
        ConfigManager.load()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> StrikeCommands.register(dispatcher) }

        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            ArenaStorage.load(srv)
            SnapshotStorage.init(srv)
        }
        ServerLifecycleEvents.SERVER_STOPPING.register { srv -> MatchManager.shutdown(srv) }
        ServerTickEvents.END_SERVER_TICK.register { srv -> MatchManager.tick(srv) }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ -> MatchManager.onJoin(handler.player) }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> MatchManager.onDisconnect(handler.player) }

        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, _ ->
            if (entity is ServerPlayer) Deaths.onAllowDeath(entity, source) else true
        }

        CobblemonGuard.register()

        LOGGER.info("ShifuStrike carregado")
    }
}
