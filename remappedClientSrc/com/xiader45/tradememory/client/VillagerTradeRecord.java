package com.xiader45.tradememory.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VillagerTradeRecord {
    private UUID villagerUuid;
    private double x, y, z;

    // НОВОЕ ПОЛЕ: Список торгов
    private List<SavedTrade> trades = new ArrayList<>();

    public VillagerTradeRecord(UUID villagerUuid, double x, double y, double z) {
        this.villagerUuid = villagerUuid;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getVillagerUuid() { return villagerUuid; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public void updatePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // НОВЫЕ МЕТОДЫ:
    public List<SavedTrade> getTrades() { return trades; }
    public void setTrades(List<SavedTrade> trades) { this.trades = trades; }
}

