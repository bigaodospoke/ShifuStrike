package com.shifustrike.client.screen

import com.mojang.blaze3d.platform.NativeImage
import com.shifustrike.ShifuStrike
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation

/**
 * Silhuetas brancas dos ícones (armas do PointBlank e ícones próprios), geradas no cliente a partir das texturas
 * que ele já tem — nada do PointBlank vai dentro do nosso jar. A tela pinta na cor do time.
 */
object IconCache {
    private val cache = HashMap<String, ResourceLocation?>()

    fun icon(path: String): ResourceLocation? = cache.getOrPut(path) { load(path) }

    private fun load(path: String): ResourceLocation? {
        val rl = ResourceLocation.tryParse(path) ?: return null
        val mc = Minecraft.getInstance()
        val res = mc.resourceManager.getResource(rl).orElse(null) ?: return null
        return try {
            val img = res.open().use { NativeImage.read(it) }
            for (y in 0 until img.height) for (x in 0 until img.width) {
                val alpha = (img.getPixelRGBA(x, y) ushr 24) and 0xFF
                img.setPixelRGBA(x, y, (alpha shl 24) or 0xFFFFFF)
            }
            val id = ShifuStrike.id("icone_gerado/${Integer.toHexString(path.hashCode())}")
            mc.textureManager.register(id, DynamicTexture(img))
            id
        } catch (e: Exception) {
            ShifuStrike.LOGGER.warn("Ícone {} não pôde ser lido: {}", path, e.message)
            null
        }
    }
}
