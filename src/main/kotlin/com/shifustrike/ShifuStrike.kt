package com.shifustrike

import com.shifustrike.arena.ArenaStorage
import com.shifustrike.command.StrikeCommands
import com.shifustrike.config.ConfigManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ShifuStrike : ModInitializer {
    const val MOD_ID = "shifustrike"
    val LOGGER: Logger = LoggerFactory.getLogger("ShifuStrike")

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
        ConfigManager.load()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> StrikeCommands.register(dispatcher) }

        ServerLifecycleEvents.SERVER_STARTED.register { srv -> ArenaStorage.load(srv) }

        LOGGER.info("ShifuStrike carregado")
    }
}
