package com.xiader45.tradememory.client.mixin;

import com.xiader45.tradememory.client.TradeMemoryManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interact", at = @At("HEAD"))
    private void onInteractEntity(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof Villager) {
            // Запоминаем жителя для сохранения торгов при закрытии окна
            TradeMemoryManager.lastInteractedVillagerUuid = entity.getUUID();
            TradeMemoryManager.lastInteractedVillagerPos = entity.position();

            // ИСПРАВЛЕНИЕ: Автоматически выключаем подсветку, как только мы нашли жителя и кликнули по нему!
            TradeMemoryManager.highlightedVillagerUuid = null;
        } else {
            // Если кликнули по другому мобу, очищаем память сохранения
            TradeMemoryManager.lastInteractedVillagerUuid = null;
            TradeMemoryManager.lastInteractedVillagerPos = null;
        }
    }
}
