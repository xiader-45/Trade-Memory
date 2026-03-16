package com.xiader45.tradememory.client.mixin;

import com.xiader45.tradememory.client.TradeMemoryManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        // Если мы выбрали кого-то в GUI, проверяем, совпадает ли UUID
        if (TradeMemoryManager.highlightedVillagerUuid != null) {
            Entity entity = (Entity) (Object) this;
            if (entity.getUUID().equals(TradeMemoryManager.highlightedVillagerUuid)) {
                cir.setReturnValue(true); // Включаем ванильную обводку!
            }
        }
    }
}
