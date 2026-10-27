package com.shifustrike.mixin.client;

import com.shifustrike.client.ClientMatch;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Na partida, a vida, a armadura e a fome do Minecraft somem: o HUD do CS mostra a vida. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void shifustrike$escondeVida(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ClientMatch.INSTANCE.getActive()) ci.cancel();
    }
}
