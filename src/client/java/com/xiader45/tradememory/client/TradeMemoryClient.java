package com.xiader45.tradememory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class TradeMemoryClient implements ClientModInitializer {

    private static final KeyMapping openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.tradememory.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.MISC
    ));
    @Override
    public void onInitializeClient() {
        // Слушаем каждый тик клиента для проверки нажатия кнопки
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                // Если мы уже в игре и экран не открыт
                if (client.player != null && client.screen == null) {
                    client.setScreen(new TradeMemoryScreen());
                }
            }
        });
    }
}
