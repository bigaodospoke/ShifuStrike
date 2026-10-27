package com.shifustrike

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ShifuStrike : ModInitializer {
    const val MOD_ID = "shifustrike"
    val LOGGER: Logger = LoggerFactory.getLogger("ShifuStrike")

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
        LOGGER.info("ShifuStrike carregado")
    }
}
