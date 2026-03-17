package com.xiader45.tradememory.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryOps;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class TradeMemoryScreen extends Screen {

    private static final Identifier CUSTOM_ARROW = Identifier.of("tradememory", "arrow");

    private UUID selectedVillagerUuid = null;

    private final int gridWidth = 203;
    private final int guiHeight = 203;
    private final int tradeWidth = 145;

    private float targetScrollPosition = 0.0f;
    private float scrollPosition = 0.0f;
    private boolean isScrolling = false;
    private boolean wasMouseDown = false;
    private int maxScrollRow = 0;

    private String searchQuery = "";

    public enum SortMode { DEFAULT, PRICE, DISTANCE }
    private SortMode currentSortMode = SortMode.DEFAULT;

    private final List<VillagerCardWidget> cards = new ArrayList<>();
    private final Map<UUID, List<TradeDisplay>> decodedTrades = new HashMap<>();
    private List<VillagerEntity> knownVillagers = new ArrayList<>();
    private List<VillagerEntity> filteredVillagers = new ArrayList<>();
    private final List<TradeDisplay> currentTrades = new ArrayList<>();

    public TradeMemoryScreen() {
        super(Text.literal("Trade Memory Screen"));
    }

    public int getGridX() {
        int totalWidth = (selectedVillagerUuid != null) ? (gridWidth + tradeWidth + 8) : gridWidth;
        return (this.width - totalWidth) / 2 + 12;
    }

    public int getGridY() {
        return (this.height - guiHeight) / 2 + 12;
    }

    @Override
    protected void init() {
        super.init();
        TradeMemoryManager.load();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        RegistryOps<JsonElement> ops = client.world.getRegistryManager().getOps(JsonOps.INSTANCE);
        decodedTrades.clear();
        for (var entry : TradeMemoryManager.getRecords().entrySet()) {
            List<TradeDisplay> trades = new ArrayList<>();
            for (SavedTrade st : entry.getValue().getTrades()) {
                trades.add(new TradeDisplay(parseItem(st.input1, ops), parseItem(st.input2, ops), parseItem(st.output, ops), st.outOfStock));
            }
            decodedTrades.put(entry.getKey(), trades);
        }

        Box searchBox = client.player.getBoundingBox().expand(128.0);
        List<VillagerEntity> allVillagers = client.world.getEntitiesByClass(VillagerEntity.class, searchBox, entity -> true);
        knownVillagers = allVillagers.stream()
                .filter(v -> TradeMemoryManager.getRecords().containsKey(v.getUuid()))
                .collect(Collectors.toList());

        int totalWidth = (selectedVillagerUuid != null) ? (gridWidth + tradeWidth + 8) : gridWidth;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - guiHeight) / 2;

        int searchW = 151;

        TextFieldWidget searchBoxWidget = new TextFieldWidget(client.textRenderer, startX + 2, startY - 26, searchW, 20, Text.literal("Поиск"));
        searchBoxWidget.setText(searchQuery);
        searchBoxWidget.setChangedListener(text -> {
            searchQuery = text.toLowerCase(Locale.ROOT);
            targetScrollPosition = 0.0f;
            updateGrid();
        });
        this.addDrawableChild(searchBoxWidget);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("$"), btn -> {
            currentSortMode = SortMode.PRICE;
            targetScrollPosition = 0.0f;
            updateGrid();
        }).dimensions(startX + 157, startY - 26, 20, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("D"), btn -> {
            currentSortMode = SortMode.DISTANCE;
            targetScrollPosition = 0.0f;
            updateGrid();
        }).dimensions(startX + 181, startY - 26, 20, 20).build());

        cards.clear();
        for (int i = 0; i < 24; i++) {
            VillagerCardWidget card = new VillagerCardWidget(0, 0, null, this, false);
            card.visible = false;
            cards.add(card);
            this.addDrawableChild(card);
        }

        if (selectedVillagerUuid != null) {
            int rightX = startX + gridWidth + 8;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Подсветить"), btn -> {
                if (selectedVillagerUuid.equals(TradeMemoryManager.highlightedVillagerUuid)) {
                    TradeMemoryManager.highlightedVillagerUuid = null;
                } else {
                    TradeMemoryManager.highlightedVillagerUuid = selectedVillagerUuid;
                }
                // ИСПРАВЛЕНИЕ: Закрываем графический интерфейс при нажатии на кнопку
                this.close();
            }).dimensions(rightX + 10, startY + guiHeight - 26, tradeWidth - 20, 20).build());

            currentTrades.clear();
            List<TradeDisplay> tr = decodedTrades.get(selectedVillagerUuid);
            if (tr != null) {
                currentTrades.addAll(tr);
            }
        }

        updateGrid();
    }

    private void updateGrid() {
        filteredVillagers = knownVillagers.stream()
                .filter(v -> matchesSearch(v.getUuid(), searchQuery))
                .collect(Collectors.toList());

        if (currentSortMode == SortMode.PRICE) {
            filteredVillagers.sort(Comparator.comparingInt(v -> getBestPrice(v.getUuid())));
        } else if (currentSortMode == SortMode.DISTANCE) {
            filteredVillagers.sort(Comparator.comparingDouble(v -> v.squaredDistanceTo(MinecraftClient.getInstance().player)));
        } else {
            filteredVillagers.sort(Comparator.comparing(net.minecraft.entity.Entity::getUuid));
        }

        int totalRows = (int) Math.ceil(filteredVillagers.size() / 4.0);
        maxScrollRow = Math.max(0, totalRows - 4);

        if (maxScrollRow == 0) {
            targetScrollPosition = 0.0f;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScrollRow > 0) {
            float scrollStep = 1.0f / maxScrollRow;
            if (verticalAmount > 0) {
                this.targetScrollPosition -= scrollStep;
            } else if (verticalAmount < 0) {
                this.targetScrollPosition += scrollStep;
            }
            this.targetScrollPosition = Math.max(0.0f, Math.min(1.0f, this.targetScrollPosition));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean matchesSearch(UUID uuid, String query) {
        if (query.isEmpty()) return true;
        List<TradeDisplay> trades = decodedTrades.get(uuid);
        if (trades == null) return false;
        for (TradeDisplay trade : trades) {
            if (matches(trade.out, query)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(ItemStack stack, String query) {
        if (stack.isEmpty()) return false;
        try {
            for (Text text : stack.getTooltip(net.minecraft.item.Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, net.minecraft.item.tooltip.TooltipType.BASIC)) {
                if (text.getString().toLowerCase(Locale.ROOT).contains(query)) return true;
            }
        } catch (Exception | Error e) {
            if (stack.getName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private int getBestPrice(UUID uuid) {
        List<TradeDisplay> trades = decodedTrades.get(uuid);
        if (trades == null) return 999999;

        int bestPrice = 999999;
        for (TradeDisplay trade : trades) {
            if (searchQuery.isEmpty() || matches(trade.out, searchQuery)) {
                int price = trade.in1.getCount() + trade.in2.getCount();
                if (price > 0 && price < bestPrice) {
                    bestPrice = price;
                }
            }
        }
        return bestPrice;
    }

    public void selectVillager(UUID uuid) {
        this.selectedVillagerUuid = uuid;
        this.clearChildren();
        this.init();
    }

    private ItemStack parseItem(String json, RegistryOps<JsonElement> ops) {
        if (json == null || json.isEmpty()) return ItemStack.EMPTY;
        try {
            return ItemStack.CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private void drawWindow(DrawContext context, int x, int y, int width) {
        context.fill(x - 1, y - 1, x + width + 1, y, 0xFF000000);
        context.fill(x - 1, y + guiHeight, x + width + 1, y + guiHeight + 1, 0xFF000000);
        context.fill(x - 1, y, x, y + guiHeight, 0xFF000000);
        context.fill(x + width, y, x + width + 1, y + guiHeight, 0xFF000000);

        context.fill(x, y, x + width, y + guiHeight, 0x40000000);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) { }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isMouseDown = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        int totalWidth = (selectedVillagerUuid != null) ? (gridWidth + tradeWidth + 8) : gridWidth;
        int startX = (this.width - totalWidth) / 2;
        int startY = (this.height - guiHeight) / 2;

        if (maxScrollRow > 0) {
            int scrollbarX = startX + 194;
            int scrollbarY = startY + 12;
            int scrollbarHeight = guiHeight - 24;
            int totalRows = maxScrollRow + 4;
            int thumbHeight = Math.max(10, scrollbarHeight * 4 / totalRows);
            int trackHeight = scrollbarHeight - thumbHeight;

            if (isMouseDown) {
                if (!wasMouseDown) {
                    if (mouseX >= scrollbarX && mouseX <= scrollbarX + 5 && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
                        isScrolling = true;
                    }
                }
                if (isScrolling) {
                    float newScroll = ((float) mouseY - (float) scrollbarY - (float) thumbHeight / 2.0f) / trackHeight;
                    this.targetScrollPosition = Math.max(0.0f, Math.min(1.0f, newScroll));
                    this.scrollPosition = this.targetScrollPosition;
                }
            } else {
                isScrolling = false;
            }
        } else {
            isScrolling = false;
        }
        wasMouseDown = isMouseDown;

        scrollPosition += (targetScrollPosition - scrollPosition) * 0.3f;
        if (Math.abs(targetScrollPosition - scrollPosition) < 0.001f) {
            scrollPosition = targetScrollPosition;
        }

        float maxScrollPixels = maxScrollRow * 45.0f;
        float currentScrollPixels = scrollPosition * maxScrollPixels;
        int firstRow = (int) (currentScrollPixels / 45);
        float offset = currentScrollPixels - (firstRow * 45);

        int startGridX = getGridX();
        int startGridY = getGridY();

        for (int i = 0; i < 24; i++) {
            VillagerCardWidget card = cards.get(i);
            int index = firstRow * 4 + i;
            if (index < filteredVillagers.size()) {
                VillagerEntity v = filteredVillagers.get(index);
                card.setVillager(v, v.getUuid().equals(selectedVillagerUuid));
                card.visible = true;
                int r = i / 4;
                int c = i % 4;
                card.setX(startGridX + c * 45);
                card.setY(startGridY + r * 45 - (int) offset);
            } else {
                card.visible = false;
                card.setVillager(null, false);
            }
        }

        drawWindow(context, startX, startY, gridWidth);

        if (maxScrollRow > 0) {
            int scrollbarX = startX + 194;
            int scrollbarY = startY + 12;
            int scrollbarHeight = guiHeight - 24;
            int totalRows = maxScrollRow + 4;
            int thumbHeight = Math.max(10, scrollbarHeight * 4 / totalRows);
            int trackHeight = scrollbarHeight - thumbHeight;

            int thumbY = scrollbarY + (int) (this.scrollPosition * trackHeight);
            int color = this.isScrolling ? 0xFFAAAAAA : 0xFF888888;
            context.fill(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, color);
        }

        if (selectedVillagerUuid != null) {
            int rightX = startX + gridWidth + 8;
            drawWindow(context, rightX, startY, tradeWidth);

            context.drawText(MinecraftClient.getInstance().textRenderer, "Торги:", rightX + 10, startY + 10, 0xFFFFFFFF, false);

            super.render(context, mouseX, mouseY, delta);

            int tradeY = startY + 25;
            for (TradeDisplay trade : currentTrades) {
                if (tradeY > startY + guiHeight - 35) break;

                int bgX = rightX + 8;
                int bgY = tradeY;
                int bgHeight = 18;
                boolean hasItem2 = !trade.in2.isEmpty();

                int bgWidth = hasItem2 ? 83 : 58;

                context.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0x40000000);

                drawItemWithTooltip(context, trade.in1, bgX + 3, bgY + 1, mouseX, mouseY);

                if (hasItem2) {
                    context.drawText(MinecraftClient.getInstance().textRenderer, "+", bgX + 21, bgY + 5, 0xFFFFFF, false);
                    drawItemWithTooltip(context, trade.in2, bgX + 28, bgY + 1, mouseX, mouseY);

                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, CUSTOM_ARROW, bgX + 47, bgY + 2, 14, 14);

                    drawItemWithTooltip(context, trade.out, bgX + 64, bgY + 1, mouseX, mouseY);
                } else {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, CUSTOM_ARROW, bgX + 22, bgY + 2, 14, 14);

                    drawItemWithTooltip(context, trade.out, bgX + 39, bgY + 1, mouseX, mouseY);
                }

                if (trade.disabled) {
                    context.drawText(MinecraftClient.getInstance().textRenderer, "X", bgX + bgWidth + 4, bgY + 5, 0xFF0000, false);
                }

                tradeY += 22;
            }
        } else {
            super.render(context, mouseX, mouseY, delta);
        }
    }

    private void drawItemWithTooltip(DrawContext context, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        if (stack.isEmpty()) return;

        context.drawItem(stack, x, y);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, x, y);

        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, stack, mouseX, mouseY);
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    private static class TradeDisplay {
        ItemStack in1, in2, out;
        boolean disabled;

        TradeDisplay(ItemStack in1, ItemStack in2, ItemStack out, boolean disabled) {
            this.in1 = in1; this.in2 = in2; this.out = out; this.disabled = disabled;
        }
    }
}
