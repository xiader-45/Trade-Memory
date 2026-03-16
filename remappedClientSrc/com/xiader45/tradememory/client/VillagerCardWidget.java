package com.xiader45.tradememory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

public class VillagerCardWidget extends AbstractWidget {
    private final TradeMemoryScreen screen;
    private Villager villager;
    private boolean isSelected;

    public VillagerCardWidget(int x, int y, Villager villager, TradeMemoryScreen screen, boolean isSelected) {
        // Теперь размер виджета можно менять здесь, и житель сам подстроится под него!
        super(x, y, 44, 44, Component.empty());
        this.villager = villager;
        this.screen = screen;
        this.isSelected = isSelected;
    }

    public void setVillager(Villager villager, boolean isSelected) {
        this.villager = villager;
        this.isSelected = isSelected;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (this.villager == null || !this.visible) return;

        context.enableScissor(screen.getGridX(), screen.getGridY(), screen.getGridX() + 179, screen.getGridY() + 179);

        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x40000000);

        if (this.isHovered() && isMouseOver(mouseX, mouseY)) {
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x33FFFFFF);
        }

        if (isSelected) {
            int x = this.getX();
            int y = this.getY();
            int w = this.width;
            int h = this.height;
            int color = 0xFFFFFFFF;
            context.fill(x, y, x + w, y + 1, color);
            context.fill(x, y + h - 1, x + w, y + h, color);
            context.fill(x, y, x + 1, y + h, color);
            context.fill(x + w - 1, y, x + w, y + h, color);
        }

        try {
            // ДИНАМИЧЕСКИЕ ВЫЧИСЛЕНИЯ РАЗМЕРОВ
            int topY = this.getY() - (this.height / 3); // Запас под голову (1/3 от высоты виджета)
            int bottomY = this.getY() + this.height;    // Ноги упираются ровно в нижнюю границу квадрата
            int entitySize = this.height / 2;           // Масштаб моба - всегда ровно половина высоты виджета

            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                    context,
                    this.getX(),
                    topY,
                    this.getX() + this.width,
                    bottomY,
                    entitySize,
                    1.0f,
                    (float) mouseX,
                    (float) mouseY,
                    this.villager
            );
        } catch (Exception | Error e) {
            // Если модель не смогла отрендериться, буква "V" тоже рисуется точно по центру любого квадрата
            context.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    "V", this.getX() + (this.width / 2) - 3, this.getY() + (this.height / 2) - 4, 0xFFFFFFFF, false);
        }

        context.disableScissor();
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (this.villager != null && this.visible) {
            screen.selectVillager(villager.getUUID());
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (mouseY < screen.getGridY() || mouseY > screen.getGridY() + 179) return false;
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}