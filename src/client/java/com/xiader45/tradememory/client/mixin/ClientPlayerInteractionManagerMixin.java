package com.xiader45.tradememory.client.mixin;

import com.xiader45.tradememory.client.TradeMemoryManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactEntity", at = @At("HEAD"))
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (entity instanceof VillagerEntity) {
            // Запоминаем жителя для сохранения торгов при закрытии окна
            TradeMemoryManager.lastInteractedVillagerUuid = entity.getUuid();
            TradeMemoryManager.lastInteractedVillagerPos = entity.getEntityPos();

            // ИСПРАВЛЕНИЕ: Автоматически выключаем подсветку, как только мы нашли жителя и кликнули по нему!
            TradeMemoryManager.highlightedVillagerUuid = null;
        } else {
            // Если кликнули по другому мобу, очищаем память сохранения
            TradeMemoryManager.lastInteractedVillagerUuid = null;
            TradeMemoryManager.lastInteractedVillagerPos = null;
        }
    }
}
