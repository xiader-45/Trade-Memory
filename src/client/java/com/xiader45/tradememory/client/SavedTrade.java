package com.xiader45.tradememory.client;

public class SavedTrade {
    public String input1;
    public String input2; // Может быть пустым, если житель просит только один предмет
    public String output;
    public boolean outOfStock; // Красный крестик (товар закончился)

    // Пустой конструктор нужен для правильной работы GSON
    public SavedTrade() {}

    public SavedTrade(String input1, String input2, String output, boolean outOfStock) {
        this.input1 = input1;
        this.input2 = input2;
        this.output = output;
        this.outOfStock = outOfStock;
    }
}

