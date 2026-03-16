package com.xiader45.tradememory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeMemoryManager {

    // Используем встроенный в игру Gson для работы с JSON
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static UUID lastInteractedVillagerUuid = null;
    public static net.minecraft.world.phys.Vec3 lastInteractedVillagerPos = null;
    public static UUID highlightedVillagerUuid = null;

    // Наша база данных, которая хранится в оперативной памяти во время игры
    private static final Map<UUID, VillagerTradeRecord> records = new HashMap<>();

    // Метод для получения названия файла в зависимости от того, где находится игрок
    private static Path getSaveFile() {
        Minecraft client = Minecraft.getInstance();
        String fileName = "unknown_world";

        if (client.getSingleplayerServer() != null) {
            // Если мы в одиночной игре или открыли мир для сети
            fileName = "local_" + client.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            // Если мы играем на внешнем сервере
            ServerData serverInfo = client.getCurrentServer();
            if (serverInfo != null) {
                fileName = "server_" + serverInfo.ip;
            }
        }

        // Убираем запрещенные символы из названия (точки, двоеточия)
        fileName = fileName.replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";

        // Возвращаем путь: .minecraft/config/tradememory/<имя_файла>
        return FabricLoader.getInstance().getConfigDir().resolve("tradememory").resolve(fileName);
    }

    public static void load() {
        records.clear();
        Path file = getSaveFile();

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Type type = new TypeToken<Map<UUID, VillagerTradeRecord>>(){}.getType();
                Map<UUID, VillagerTradeRecord> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    records.putAll(loaded);
                }
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке торгов жителей: " + e.getMessage());
            }
        }
    }

    public static void save() {
        Path file = getSaveFile();

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(records, writer);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении торгов жителей: " + e.getMessage());
        }
    }

    // Методы для работы с базой
    public static Map<UUID, VillagerTradeRecord> getRecords() {
        return records;
    }

    public static void addOrUpdateRecord(VillagerTradeRecord record) {
        records.put(record.getVillagerUuid(), record);
        save(); // Сразу сохраняем изменения на диск
    }

    public static void removeRecord(UUID uuid) {
        if (records.remove(uuid) != null) {
            save(); // Если удалили, перезаписываем файл
        }
    }
}
