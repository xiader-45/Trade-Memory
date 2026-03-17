package com.xiader45.tradememory.client.mixin;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.xiader45.tradememory.client.SavedTrade;
import com.xiader45.tradememory.client.TradeMemoryManager;
import com.xiader45.tradememory.client.VillagerTradeRecord;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    // В Yarn 1.21 метод закрытия экрана называется "close"
    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {

        // Проверяем, что сейчас закрывается именно экран торговли с жителем
        if (((Object) this) instanceof MerchantScreen merchantScreen) {

            if (TradeMemoryManager.lastInteractedVillagerUuid != null && TradeMemoryManager.lastInteractedVillagerPos != null) {

                MerchantScreenHandler handler = merchantScreen.getScreenHandler();
                boolean isLocked = handler.getExperience() > 0;

                // ПОДСКАЗКА: Если getRecipes() горит красным, поменяйте на getOffers()
                for (TradeOffer offer : handler.getRecipes()) {
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

                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.world != null) {
                        RegistryOps<JsonElement> ops = client.world.getRegistryManager().getOps(JsonOps.INSTANCE);
                        List<SavedTrade> savedTrades = new ArrayList<>();

                        for (TradeOffer offer : handler.getRecipes()) {
                            ItemStack in1Stack = offer.getDisplayedFirstBuyItem();
                            Optional<TradedItem> secondItemOpt = offer.getSecondBuyItem();
                            ItemStack in2Stack = secondItemOpt.isEmpty() ? ItemStack.EMPTY : secondItemOpt.get().itemStack();
                            ItemStack outStack = offer.getSellItem();

                            String in1 = in1Stack.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops, in1Stack).getOrThrow().toString();
                            String in2 = in2Stack.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops, in2Stack).getOrThrow().toString();
                            String out = outStack.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops, outStack).getOrThrow().toString();

                            savedTrades.add(new SavedTrade(in1, in2, out, offer.isDisabled()));
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
