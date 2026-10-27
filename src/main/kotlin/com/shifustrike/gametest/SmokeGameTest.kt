package com.shifustrike.gametest

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation

class SmokeGameTest : FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    fun pointblank_carregado(h: GameTestHelper) {
        h.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("pointblank:ak47")), "pointblank:ak47 não existe")
        h.succeed()
    }
}
