package com.shifustrike.mixin;

import com.shifustrike.guard.DamageGuard;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dentro de um claim do Flan, quem decide o dano entre jogadores da partida é a partida (DamageGuard), não o claim.
 * O Flan bloqueia dano em Entity.isInvulnerableTo → preventDamage e ataque corpo a corpo em attackSimple.
 * Seletores só por nome; require = 0: sem Flan (ou se ele mudar), nada quebra.
 */
@Pseudo
@Mixin(targets = "io.github.flemmli97.flan.event.EntityInteractEvents", remap = false)
public abstract class FlanPvpMixin {
    @Inject(method = "attackSimple", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void shifustrike$ataqueDaPartida(Player player, Entity target, boolean message, CallbackInfoReturnable<InteractionResult> cir) {
        if (DamageGuard.INSTANCE.matchDecides(player, target)) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "preventDamage", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void shifustrike$danoDaPartida(Entity target, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (DamageGuard.INSTANCE.matchDecides(source.getEntity(), target)) cir.setReturnValue(false);
    }
}
