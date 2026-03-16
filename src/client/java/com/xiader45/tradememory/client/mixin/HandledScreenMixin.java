package com.xiader45.tradememory.client.mixin;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.xiader45.tradememory.client.SavedTrade;
import com.xiader45.tradememory.client.TradeMemoryManager;
import com.xiader45.tradememory.client.VillagerTradeRecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    // В Yarn 1.21 метод закрытия экрана называется "close"
    @Inject(method = "onClose", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {

        // Проверяем, что сейчас закрывается именно экран торговли с жителем
        if (((Object) this) instanceof MerchantScreen merchantScreen) {

            if (TradeMemoryManager.lastInteractedVillagerUuid != null && TradeMemoryManager.lastInteractedVillagerPos != null) {

                MerchantMenu handler = merchantScreen.getMenu();
                boolean isLocked = handler.getTraderXp() > 0;

                // ПОДСКАЗКА: Если getRecipes() горит красным, поменяйте на getOffers()
                for (MerchantOffer offer : handler.getOffers()) {
                    if (offer.getUses() > 0) {
                        isLocked = true;
                        break;
                    }
                }

                if (isLocked) {
                    VillagerTradeRecord record = new VillagerTradeRecord(
                            TradeMemoryManager.lastInteractedVillagerUuid,
                            TradeMemoryManager.lastInteractedVillagerPos.x,
                            TradeMemoryManager.lastInteractedVillagerPos.y,
                            TradeMemoryManager.lastInteractedVillagerPos.z
                    );

                    Minecraft client = Minecraft.getInstance();
                    if (client.level != null) {
                        RegistryOps<JsonElement> ops = client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
                        List<SavedTrade> savedTrades = new ArrayList<>();

                        for (MerchantOffer offer : handler.getOffers()) {
                            ItemStack in1Stack = offer.getCostA();
                            Optional<ItemCost> secondItemOpt = offer.getItemCostB();
                            ItemStack in2Stack = secondItemOpt.isEmpty() ? ItemStack.EMPTY : secondItemOpt.get().itemStack();
                            ItemStack outStack = offer.getResult();

                            String in1 = in1Stack.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops, in1Stack).getOrThrow().toString();
                            String in2 = in2Stack.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops, in2Stack).getOrThrow().toString();
                            String out = outStack.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops, outStack).getOrThrow().toString();

                            savedTrades.add(new SavedTrade(in1, in2, out, offer.isOutOfStock()));
                        }
                        record.setTrades(savedTrades);
                    }

                    TradeMemoryManager.addOrUpdateRecord(record);
                } else {
                    TradeMemoryManager.removeRecord(TradeMemoryManager.lastInteractedVillagerUuid);
                }

                TradeMemoryManager.lastInteractedVillagerUuid = null;
                TradeMemoryManager.lastInteractedVillagerPos = null;
            }
        }
    }
}
