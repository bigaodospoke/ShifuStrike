package com.shifustrike.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Grava os dados de um jogador no disco na hora (o Minecraft só grava no autosave ou ao sair). */
@Mixin(PlayerList.class)
public interface PlayerListInvoker {
    @Invoker("save")
    void shifustrike$save(ServerPlayer player);
}
